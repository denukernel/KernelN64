package kerneln64.ui;
import kerneln64.hardware.Audio;
import kerneln64.cheats.AudioCodeSelection;
import kerneln64.cheats.CheatEngine;
import kerneln64.cheats.CheatRepository;
import kerneln64.cpu.Cpu;
import kerneln64.cheats.Sm64SampleCodes;
import kerneln64.Main;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/** Project64-style grouped checkboxes, notes and a named custom-code editor. */
public final class CheatDialog extends JDialog {
    private static CheatDialog active;
    public static void open()throws IOException {
        if(active!=null && active.isDisplayable() && active.rom.equals(Main.currentRomFile)) {
            active.setVisible(true);active.toFront();active.requestFocus();return;
        }
        if(active!=null)active.dispose();
        CheatDialog dialog=new CheatDialog();active=dialog;
        dialog.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent event){if(active==dialog)active=null;}});
        dialog.setVisible(true);
    }
    private final File rom=Main.currentRomFile;
    private final Path path=Main.cheatFile().toPath();
    private final List<CheatRepository.Entry> entries;
    private final JTree tree=new JTree();
    private final JTextArea notes=new JTextArea(5,35);
    private final JTextField search=new JTextField();
    private final JLabel status=new JLabel("Audio: up to 4 compatible effects. F9: GameShark button.");
    public CheatDialog()throws IOException {
        super(Main.hMainWindow,"Cheats - "+Main.currentRomFile.getName(),false);
        entries=CheatRepository.load(path,Main.sm64UsCheats);
        status.setToolTipText("Cheat file: "+path.toAbsolutePath().normalize()+" | "+AudioCodeSelection.HELP);
        JPanel content=new JPanel(new BorderLayout(8,8));content.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));setContentPane(content);
        JPanel filter=new JPanel(new BorderLayout(8,0));
        JCheckBox compatibility=new JCheckBox("SM64 US / legacy codes",Main.sm64UsCheats);
        compatibility.setEnabled(!Main.decompCheatsRejected);
        compatibility.setToolTipText(Main.decompCheats==null?"For US-based SM64 hacks. For a decomp build, keep its matching .elf beside the ROM.":"Presets and pasted US codes use "+Main.decompCheats.elfPath+". The editor displays native build addresses.");
        JPanel identity=new JPanel(new BorderLayout());identity.add(new JLabel(Main.cheatProfile.label()));identity.add(compatibility,BorderLayout.EAST);filter.add(identity,BorderLayout.NORTH);
        compatibility.addActionListener(event->{
            if(!rom.equals(Main.currentRomFile)){compatibility.setSelected(Main.sm64UsCheats);status.setText("The ROM changed; reopen Cheats.");return;}
            boolean enabled=compatibility.isSelected();CheatEngine validation=new CheatEngine();validation.setSm64Compatibility(enabled);
            try {
                validation.setCodes(CheatRepository.serialize(entries));Main.sm64UsCheats=enabled;Main.cheats.setSm64Compatibility(enabled);
                Main.setPluginSetting("cheats.sm64."+Main.currentRomHash,Boolean.toString(enabled));Main.saveConfig();
                if(enabled)for(var preset:CheatRepository.presets())if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);
                publish();rebuild();
            }catch(IllegalArgumentException error){compatibility.setSelected(Main.sm64UsCheats);JOptionPane.showMessageDialog(this,error.getMessage(),"SM64 Compatibility",JOptionPane.ERROR_MESSAGE);}
        });
        filter.add(new JLabel("Find:"),BorderLayout.WEST);filter.add(search);content.add(filter,BorderLayout.NORTH);
        tree.setRootVisible(false);tree.setShowsRootHandles(true);tree.setRowHeight(23);tree.setCellRenderer(new CheckRenderer());
        tree.addTreeSelectionListener(e->selection());
        tree.addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){TreePath p=tree.getPathForLocation(e.getX(),e.getY());if(p==null)return;tree.setSelectionPath(p);Rectangle bounds=tree.getPathBounds(p);if(bounds!=null && e.getX()<bounds.x+22)toggle(p);else if(e.getClickCount()==2 && ((DefaultMutableTreeNode)p.getLastPathComponent()).getUserObject() instanceof CheatRepository.Entry)edit();}});
        tree.getInputMap().put(KeyStroke.getKeyStroke("SPACE"),"toggle");tree.getActionMap().put("toggle",new AbstractAction(){public void actionPerformed(ActionEvent e){toggle(tree.getSelectionPath());}});
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){rebuild();}public void removeUpdate(DocumentEvent e){rebuild();}public void changedUpdate(DocumentEvent e){rebuild();}});
        JPanel lower=new JPanel(new BorderLayout(4,4));JPanel tools=new JPanel(new GridLayout(1,0,4,0));
        button(tools,"Add...",()->edit(null));button(tools,"Edit...",this::edit);button(tools,"Delete",this::remove);button(tools,"Import...",this::importCodes);
        if(Main.sm64UsCheats)button(tools,"Presets",()->{for(var preset:CheatRepository.presets())if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);publish();rebuild();});button(tools,"Unmark All",()->{for(var entry:entries)entry.enabled=false;publish();tree.repaint();});
        JPanel toolRows=new JPanel(new GridLayout(0,1));toolRows.add(tools);
        if(Main.sm64UsCheats){JPanel sampleTools=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));button(sampleTools,"Build Sample Codes",this::generateSamples);toolRows.add(sampleTools);}
        lower.add(toolRows,BorderLayout.NORTH);notes.setEditable(false);notes.setLineWrap(true);notes.setWrapStyleWord(true);JScrollPane notePane=new JScrollPane(notes);notePane.setBorder(BorderFactory.createTitledBorder("Notes"));lower.add(notePane);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(tree),lower);split.setResizeWeight(0.75);content.add(split);
        JPanel bottom=new JPanel(new BorderLayout());bottom.add(status);JButton close=new JButton("Close");close.addActionListener(e->dispose());bottom.add(close,BorderLayout.EAST);content.add(bottom,BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(e->dispose(),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);
        rebuild();setSize(590,650);setMinimumSize(new Dimension(490,420));setLocationRelativeTo(Main.hMainWindow);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if(Main.sm64UsCheats)SwingUtilities.invokeLater(this::generateSamples);
    }
    private boolean buildingSamples;
    private void generateSamples() {
        if(buildingSamples || !isDisplayable())return;
        Cpu cpu=Main.cpu;String hash=Main.currentRomHash;
        if(cpu==null || !cpu.running || !rom.equals(Main.currentRomFile))return;
        if(entries.stream().anyMatch(e->e.enabled && e.name.startsWith("Audio\\Sample distortion\\"))) {
            status.setText("Disable sample distortion before building another sample code.");return;
        }
        buildingSamples=true;status.setText("Inspecting the current music sample bank...");
        new SwingWorker<List<CheatRepository.Entry>,Void>() {
            protected List<CheatRepository.Entry> doInBackground()throws Exception {
                List<CheatRepository.Entry> result=new ArrayList<>();
                cpu.submitCommand(()->{
                    if(cpu!=Main.cpu || !hash.equals(Main.currentRomHash))throw new IllegalStateException("The ROM changed.");
                    if(Main.cheats.hasSamplePrograms())throw new IllegalStateException("Disable sample distortion before rebuilding its codes.");
                    result.addAll(Sm64SampleCodes.generate(Main.mem.RDRAM));
                }).get(10,java.util.concurrent.TimeUnit.SECONDS);return result;
            }
            protected void done() {
                buildingSamples=false;if(!isDisplayable() || cpu!=Main.cpu || !hash.equals(Main.currentRomHash))return;
                try {
                    var result=get();int added=0;
                    for(var preset:result)if(entries.stream().noneMatch(e->e.name.equals(preset.name))){entries.add(preset);added++;}
                    if(added>0 && !CheatDialog.this.publish()){rebuild();return;}
                    rebuild();status.setText(added+" sample codes added under Audio > Sample distortion (disabled, saved).");
                }catch(Exception error){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();status.setText(cause.getMessage());}
            }
        }.execute();
    }
    private static void button(JPanel panel,String label,Runnable action){JButton b=new JButton(label);b.addActionListener(e->action.run());panel.add(b);}
    private void rebuild() {
        DefaultMutableTreeNode root=new DefaultMutableTreeNode("Cheats");String query=search.getText().strip().toLowerCase(Locale.ROOT);
        for(var entry:entries) {
            if(!query.isEmpty() && !(entry.name+" "+entry.notes).toLowerCase(Locale.ROOT).contains(query))continue;
            String[] parts=entry.name.split("\\\\");DefaultMutableTreeNode parent=root;
            for(int i=0;i<parts.length-1;i++) {
                DefaultMutableTreeNode found=null;for(int j=0;j<parent.getChildCount();j++){var child=(DefaultMutableTreeNode)parent.getChildAt(j);if(parts[i].equals(child.getUserObject())){found=child;break;}}
                if(found==null){found=new DefaultMutableTreeNode(parts[i]);parent.add(found);}parent=found;
            }
            parent.add(new DefaultMutableTreeNode(entry));
        }
        tree.setModel(new DefaultTreeModel(root));
        if(!query.isEmpty())for(int row=0;row<tree.getRowCount();row++)tree.expandRow(row);
    }
    private List<CheatRepository.Entry> descendants(DefaultMutableTreeNode node) {
        List<CheatRepository.Entry> found=new ArrayList<>();var nodes=node.depthFirstEnumeration();
        while(nodes.hasMoreElements()){var n=(DefaultMutableTreeNode)nodes.nextElement();if(n.getUserObject() instanceof CheatRepository.Entry entry)found.add(entry);}return found;
    }
    private void toggle(TreePath path) {
        if(path==null)return;var found=descendants((DefaultMutableTreeNode)path.getLastPathComponent());boolean enabled=found.stream().anyMatch(e->!e.enabled);
        try{AudioCodeSelection.toggle(entries,found,enabled);publish();}
        catch(IllegalArgumentException error){status.setText(error.getMessage());status.setToolTipText(error.getMessage());}
        tree.repaint();selection();
    }
    private CheatRepository.Entry selected(){var p=tree.getSelectionPath();return p!=null && ((DefaultMutableTreeNode)p.getLastPathComponent()).getUserObject() instanceof CheatRepository.Entry entry?entry:null;}
    private void selection(){var entry=selected();notes.setText(entry==null?"Expand a category and check a code to enable it. Space also toggles the selection.":entry.notes+(AudioCodeSelection.audio(entry)?"\n\n"+AudioCodeSelection.HELP:"")+"\n\n"+entry.codes);notes.setCaretPosition(0);}
    private boolean publish() {
        try {
            if(!rom.equals(Main.currentRomFile))throw new IOException("The ROM changed. Reopen Cheats for the active game.");
            String text=CheatRepository.serialize(entries);CheatRepository.validator().setCodes(text);CheatRepository.save(path,entries);Main.cheats.setCodes(text);
            if(Main.cpu!=null && Main.cpu.running)Main.cpu.submitCommand(()->{if(rom.equals(Main.currentRomFile))Main.cheats.synchronize(Main.mem.RDRAM,Main.cpu);});
            status.setText(entries.stream().filter(e->e.enabled).count()+" cheats enabled. Audio: "+AudioCodeSelection.count(entries)+"/"+AudioCodeSelection.MAX_EFFECTS+". Saved.");
            return true;
        } catch(Exception error){JOptionPane.showMessageDialog(this,error.getMessage(),"Cheats",JOptionPane.ERROR_MESSAGE);return false;}
    }
    private void edit(){var entry=selected();if(entry!=null)edit(entry);}
    private void edit(CheatRepository.Entry entry) {
        JTextField name=new JTextField(entry==null?"Custom\\New Cheat":entry.name);
        JTextArea codes=new JTextArea(entry==null?"":entry.codes,10,38),description=new JTextArea(entry==null?"":entry.notes,4,38);codes.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));description.setLineWrap(true);description.setWrapStyleWord(true);
        JPanel panel=new JPanel(new BorderLayout(6,6));JPanel top=new JPanel(new BorderLayout());top.add(new JLabel("Name (use \\ for categories):"),BorderLayout.NORTH);top.add(name);panel.add(top,BorderLayout.NORTH);
        JScrollPane codePane=new JScrollPane(codes);codePane.setBorder(BorderFactory.createTitledBorder("Codes: XXXXXXXX YYYY, one per line"));panel.add(codePane);JScrollPane notePane=new JScrollPane(description);notePane.setBorder(BorderFactory.createTitledBorder("Notes"));panel.add(notePane,BorderLayout.SOUTH);
        if(Main.decompCheats!=null) {
            CheatCodeInput paste=new CheatCodeInput(codes,Main.decompCheats);
            JPanel heading=new JPanel(new BorderLayout(4,4));heading.add(top);heading.add(paste,BorderLayout.SOUTH);panel.add(heading,BorderLayout.NORTH);
        }
        while(JOptionPane.showConfirmDialog(this,panel,entry==null?"Add Cheat":"Edit Cheat",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                if(name.getText().isBlank() || codes.getText().isBlank())throw new IllegalArgumentException("Enter a name and at least one code.");
                CheatRepository.validator().setCodes(codes.getText());
                CheatRepository.Entry edited=new CheatRepository.Entry(name.getText().strip(),description.getText(),codes.getText().strip(),entry!=null && entry.enabled,entry==null?"":entry.group);
                if(entry!=null)edited.addressing=entry.addressing;
                if(Main.decompCheats!=null)edited.addressing=Main.decompCheats.hash;
                List<CheatRepository.Entry> previous=new ArrayList<>(entries),staged=new ArrayList<>(entries);
                if(entry==null)staged.add(edited);else staged.set(staged.indexOf(entry),edited);
                AudioCodeSelection.validate(staged);entries.clear();entries.addAll(staged);
                if(!publish()){entries.clear();entries.addAll(previous);rebuild();continue;}
                rebuild();return;
            }catch(IllegalArgumentException error){JOptionPane.showMessageDialog(this,error.getMessage(),"Invalid Code",JOptionPane.ERROR_MESSAGE);}
        }
    }
    private void remove(){var entry=selected();if(entry!=null){entries.remove(entry);publish();rebuild();notes.setText("");}}
    private void importCodes(){
        JFileChooser chooser=new JFileChooser(".");JCheckBox nativeAddresses=new JCheckBox("File already uses this build's addresses");
        if(Main.decompCheats!=null)chooser.setAccessory(nativeAddresses);
        if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)try{
            var imported=CheatRepository.importCodes(Files.readString(chooser.getSelectedFile().toPath()),nativeAddresses.isSelected());
            entries.addAll(imported);if(!publish())entries.removeAll(imported);rebuild();
        }catch(Exception error){JOptionPane.showMessageDialog(this,error.getMessage(),"Import Cheats",JOptionPane.ERROR_MESSAGE);}
    }
    private final class CheckRenderer extends JCheckBox implements TreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean focus) {
            var node=(DefaultMutableTreeNode)value;Object item=node.getUserObject();
            String label=item instanceof CheatRepository.Entry entry?entry.name.substring(entry.name.lastIndexOf('\\')+1):item.toString();
            var found=descendants(node);long enabled=found.stream().filter(e->e.enabled).count();
            setText(label+(enabled>0 && enabled<found.size()?" (some)":""));setSelected(!found.isEmpty() && enabled==found.size());
            setOpaque(true);setBackground(selected?UIManager.getColor("Tree.selectionBackground"):tree.getBackground());setForeground(selected?UIManager.getColor("Tree.selectionForeground"):tree.getForeground());setFont(tree.getFont());return this;
        }
    }
}
