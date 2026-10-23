package kerneln64.scripting;

import kerneln64.Main;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Modeless multi-script workspace. Runtime execution stays on the CPU thread. */
public final class LuaConsoleDialog extends JDialog {
    private static LuaConsoleDialog current;
    private static List<Document> remembered=new ArrayList<>();
    private static final class Document {
        final String id;
        String name,source;
        Path file;
        Document(String name,String source){this(UUID.randomUUID().toString(),name,source);}
        Document(String id,String name,String source){this.id=id;this.name=name;this.source=source;}
        LuaScripts.Script script(){return new LuaScripts.Script(id,name,source);}
    }
    private final DefaultListModel<Document> documents=new DefaultListModel<>();
    private final JList<Document> list=new JList<>(documents);
    private final JTextArea editor=new JTextArea(18,70),output=new JTextArea(8,70),errors=new JTextArea(8,70);
    private final JLabel state=new JLabel("no script selected");
    private Map<String,LuaScripts.ScriptStatus> snapshots=Map.of();
    private Document selected;
    private Path groupFile;
    private boolean loading;
    private final javax.swing.Timer timer;
    public static void showConsole(){if(current==null||!current.isDisplayable())current=new LuaConsoleDialog();current.setVisible(true);current.toFront();}
    private LuaConsoleDialog(){
        super(Main.hMainWindow,"KernelN64 lua scripting",false);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        editor.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));output.setFont(editor.getFont());errors.setFont(editor.getFont());output.setEditable(false);errors.setEditable(false);
        for(Document doc:remembered)documents.addElement(doc);
        for(var script:LuaScripts.definitions())if(!contains(script.id()))documents.addElement(new Document(script.id(),script.name(),script.source()));
        if(documents.isEmpty())documents.addElement(new Document("editor.lua","-- each script has its own globals and callbacks\nprint(emu.romname())\nevent.onframe(function(frame)\n    if frame % 60 == 0 then print(\"frame\", frame) end\nend)\n"));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object value,int index,boolean chosen,boolean focus){
                JLabel label=(JLabel)super.getListCellRendererComponent(l,value,index,chosen,focus);Document doc=(Document)value;
                var snapshot=snapshots.get(doc.id);label.setText(doc.name+" - "+(snapshot==null?"ready":snapshot.status()));return label;
            }
        });
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())select();});
        editor.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){saveEdit();}public void removeUpdate(DocumentEvent e){saveEdit();}public void changedUpdate(DocumentEvent e){saveEdit();}
        });
        JPanel actions=new JPanel(new GridLayout(0,1));
        JPanel files=new JPanel(new FlowLayout(FlowLayout.LEFT)),runs=new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(files,"new script",()->add(new Document("script-"+(documents.size()+1)+".lua","")));
        button(files,"add files...",this::openScripts);button(files,"save script as...",this::saveScript);button(files,"remove",this::remove);
        button(files,"api help",this::help);
        button(runs,"run all / restart",this::runAll);button(runs,"stop all",LuaScripts::requestStop);
        button(runs,"open group...",()->openGroup(false));button(runs,"save group...",this::saveGroup);button(runs,"run saved group...",()->openGroup(true));
        button(runs,"clear selected log",()->{if(selected!=null)LuaScripts.clearLog(selected.id);refresh();});
        actions.add(files);actions.add(runs);add(actions,BorderLayout.NORTH);
        JTabbedPane logs=new JTabbedPane();logs.addTab("output",new JScrollPane(output));logs.addTab("errors",new JScrollPane(errors));
        JSplitPane editSplit=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(editor),logs);editSplit.setResizeWeight(.68);
        JScrollPane sidebar=new JScrollPane(list);sidebar.setPreferredSize(new Dimension(250,450));
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,sidebar,editSplit);split.setResizeWeight(.23);
        add(split,BorderLayout.CENTER);add(state,BorderLayout.SOUTH);
        timer=new javax.swing.Timer(200,e->refresh());timer.start();list.setSelectedIndex(0);refresh();
        setSize(1080,690);setLocationRelativeTo(Main.hMainWindow);
    }
    private void button(JPanel row,String text,Runnable action){JButton b=new JButton(text);b.addActionListener(e->{try{action.run();}catch(Exception error){failure(error);}});row.add(b);}
    private void saveEdit(){if(!loading&&selected!=null)selected.source=editor.getText();}
    private boolean contains(String id){for(int i=0;i<documents.size();i++)if(documents.get(i).id.equals(id))return true;return false;}
    private void select(){selected=list.getSelectedValue();loading=true;editor.setText(selected==null?"":selected.source);editor.setEditable(selected!=null);editor.setCaretPosition(0);loading=false;refresh();}
    private void add(Document doc){if(documents.size()>=LuaScripts.MAX_SCRIPTS)throw new IllegalStateException("use at most 32 scripts");documents.addElement(doc);list.setSelectedValue(doc,true);}
    private void remove(){
        if(selected==null)return;var snapshot=snapshots.get(selected.id);
        if(snapshot!=null&&snapshot.status().equals("running"))throw new IllegalStateException("stop all scripts before removing a running script");
        int index=list.getSelectedIndex();documents.remove(index);if(!documents.isEmpty())list.setSelectedIndex(Math.min(index,documents.size()-1));else select();
    }
    private List<LuaScripts.Script> scripts(){saveEdit();List<LuaScripts.Script> group=new ArrayList<>();for(int i=0;i<documents.size();i++)group.add(documents.get(i).script());return group;}
    private void runAll(){
        Future<?> future=LuaScripts.runGroup(scripts());
        CompletableFuture.runAsync(()->{try{future.get();}catch(Exception error){SwingUtilities.invokeLater(()->failure(error));}});
    }
    private void refresh(){
        for(var script:LuaScripts.definitions())if(!contains(script.id()))documents.addElement(new Document(script.id(),script.name(),script.source()));
        Map<String,LuaScripts.ScriptStatus> next=new HashMap<>();for(var snapshot:LuaScripts.statuses())next.put(snapshot.id(),snapshot);snapshots=next;list.repaint();
        var snapshot=selected==null?null:LuaScripts.script(selected.id);
        state.setText(LuaScripts.snapshot().status()+" | "+(selected==null?"no script selected":selected.name+" - "+(snapshot==null?"ready":snapshot.status())));
        text(output,snapshot==null?"":snapshot.output());text(errors,snapshot==null?"":snapshot.errors());
    }
    private static void text(JTextArea area,String value){if(!area.getText().equals(value)){area.setText(value);area.setCaretPosition(area.getDocument().getLength());}}
    private JFileChooser chooser(Path path,String description,String extension){JFileChooser chooser=new JFileChooser(path==null?Path.of("lua/examples").toFile():path.toAbsolutePath().getParent().toFile());chooser.setFileFilter(new FileNameExtensionFilter(description,extension));return chooser;}
    private static String readScript(Path path)throws java.io.IOException{
        if(Files.size(path)>1048576)throw new java.io.IOException("script file is too large");String text=Files.readString(path);if(text.startsWith("\uFEFF"))text=text.substring(1);
        if(text.length()>LuaScripts.MAX_SOURCE)throw new java.io.IOException("script is too large");return text;
    }
    private void openScripts(){
        JFileChooser chooser=chooser(selected==null?null:selected.file,"lua scripts (*.lua)","lua");chooser.setMultiSelectionEnabled(true);
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try{
            List<Document> additions=new ArrayList<>();for(var file:chooser.getSelectedFiles()){Path path=file.toPath().toAbsolutePath();Document doc=new Document(path.getFileName().toString(),readScript(path));doc.file=path;additions.add(doc);}
            if(documents.size()+additions.size()>LuaScripts.MAX_SCRIPTS)throw new IllegalArgumentException("use at most 32 scripts");
            for(Document doc:additions)add(doc);
        }catch(Exception error){failure(error);}
    }
    private boolean replace(Path path){return !Files.exists(path)||JOptionPane.showConfirmDialog(this,"replace "+path.getFileName()+"?","save",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    private static Path suffix(Path path,String extension){return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)?path:path.resolveSibling(path.getFileName()+extension);}
    private void saveScript(){
        if(selected==null)return;JFileChooser chooser=chooser(selected.file,"lua scripts (*.lua)","lua");
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=suffix(chooser.getSelectedFile().toPath().toAbsolutePath(),".lua");if(!replace(path))return;
        try{saveEdit();Files.writeString(path,selected.source);selected.file=path;selected.name=path.getFileName().toString();refresh();}catch(Exception error){failure(error);}
    }
    private void saveGroup(){
        JFileChooser chooser=chooser(groupFile,"lua script groups (*.luagroup)","luagroup");if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=suffix(chooser.getSelectedFile().toPath().toAbsolutePath(),".luagroup");if(!replace(path))return;
        try{LuaScriptGroups.write(path,scripts());groupFile=path;}catch(Exception error){failure(error);}
    }
    private void openGroup(boolean run){
        JFileChooser chooser=chooser(groupFile,"lua script groups (*.luagroup)","luagroup");if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try{
            Path path=chooser.getSelectedFile().toPath().toAbsolutePath();List<LuaScripts.Script> group=LuaScriptGroups.read(path);
            LuaScripts.requestStop();documents.clear();selected=null;
            for(var script:group)documents.addElement(new Document(script.id(),script.name(),script.source()));
            groupFile=path;list.setSelectedIndex(0);if(run)runAll();
        }catch(Exception error){failure(error);}
    }
    private void help(){JOptionPane.showMessageDialog(this,"each script has independent globals, callbacks, output and errors.\nrun all / restart starts the list together; stop all stops every script.\nrun saved group loads and starts a saved group explicitly.\n\nevent.onframe(fn), event.onloadstate(fn), event.onstop(fn)\nemu.pause(), resume(), stop() (stops only the calling script)\njoypad.set(table, port), get(port), clear(port)\n\nlater scripts override the same input fields. memory writes run in list order.\nsee docs/Lua-scripting.md for the full API.","lua scripting",JOptionPane.INFORMATION_MESSAGE);}
    private void failure(Exception error){Throwable cause=error instanceof ExecutionException&&error.getCause()!=null?error.getCause():error;JOptionPane.showMessageDialog(this,cause.getMessage(),"lua scripting",JOptionPane.ERROR_MESSAGE);}
    @Override public void dispose(){saveEdit();remembered=new ArrayList<>();for(int i=0;i<documents.size();i++)remembered.add(documents.get(i));timer.stop();super.dispose();}
}
