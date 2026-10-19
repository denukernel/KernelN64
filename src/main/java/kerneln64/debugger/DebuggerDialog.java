package kerneln64.debugger;
import kerneln64.ui.CoreOptions;
import kerneln64.cpu.Cpu;
import kerneln64.ui.HostMessages;
import kerneln64.memory.Memory;
import kerneln64.cpu.Registers;
import kerneln64.ui.SettingsDialog;
import kerneln64.Main;


import java.awt.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Modeless CPU debugger; all guest access is queued on its original CPU. */
public final class DebuggerDialog extends JDialog {
    private static DebuggerDialog current;
    private final Cpu cpu;
    private final JLabel status = new JLabel("Connecting...");
    private final JTextField codeAddress = new JTextField("80000000", 10), memoryAddress = new JTextField("80000000", 10);
    private final JCheckBox follow = new JCheckBox("Follow PC", true), tracing = new JCheckBox("Record trace (last 1024 instructions)");
    private final DefaultTableModel code = readOnly("", "Address", "Opcode", "Instruction");
    private final DefaultTableModel registers = new DefaultTableModel(new String[]{"Register", "64-bit hexadecimal"}, 0) {
        public boolean isCellEditable(int row, int col) { return col == 1 && row > 0 && row < 32 && cpu.debugger.paused; }
        public void setValueAt(Object value, int row, int col) {
            if (updating) { super.setValueAt(value,row,col); return; }
            action(() -> { cpu.debugger.requirePaused(); cpu.GPR[row] = Long.parseUnsignedLong(clean(value.toString()),16); });
        }
    };
    private final DefaultTableModel cp0 = readOnly("Register", "Hex value"), fpr = readOnly("FPR", "Raw 64-bit value", "Double interpretation");
    private final DefaultTableModel memory;
    private final DefaultListModel<String> breaks = new DefaultListModel<>();
    private final JList<String> breakpointList = new JList<>(breaks);
    private final JTextArea trace = new JTextArea();
    private final JTable codeTable = new JTable(code), registerTable = new JTable(registers), memoryTable;
    private final JTabbedPane tabs = new JTabbedPane();
    private final javax.swing.Timer refresh;
    private boolean busy, updating;
    private int displayedMemory;

    public static void show(String tab) {
        if (!CoreOptions.debuggerEnabled) { SettingsDialog.show("Advanced"); return; }
        if (Main.cpu == null || !Main.cpu.running) { HostMessages.error("Debugger",new IllegalStateException("Open a ROM first.")); return; }
        if (current == null || !current.isDisplayable() || current.cpu != Main.cpu) {
            if(current!=null)current.dispose(); current = new DebuggerDialog(Main.cpu);
        }
        for(int i=0;i<current.tabs.getTabCount();i++)if(current.tabs.getTitleAt(i).equals(tab))current.tabs.setSelectedIndex(i);
        current.setVisible(true); current.toFront(); current.update();
    }
    private DebuggerDialog(Cpu cpu) {
        super(Main.hMainWindow,"KernelN64 Debugger",false); this.cpu = cpu;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(toolbar,"Break",()->action(()->cpu.debugger.pause("Paused by debugger")));
        button(toolbar,"Run",()->action(cpu.debugger::resume));
        button(toolbar,"Step",()->action(cpu.debugger::step));
        button(toolbar,"Step Over",()->action(()->{
            cpu.debugger.requirePaused(); int op=ByteBuffer.wrap(cpu.debugger.read(cpu.pc,4)).getInt();
            boolean call=(op>>>26)==3 || ((op>>>26)==0 && (op&63)==9) || ((op>>>26)==1 && ((op>>>16)&31)>=16);
            if(call)cpu.debugger.runTo(cpu.pc+8);else cpu.debugger.step();
        }));
        button(toolbar,"Run to RA",()->action(()->cpu.debugger.runTo((int)cpu.GPR[31])));
        button(toolbar,"Refresh",this::update);
        add(toolbar,BorderLayout.NORTH);
        JPanel commands = new JPanel(new BorderLayout());
        JPanel addresses = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addresses.add(new JLabel("Address (hex):")); addresses.add(codeAddress); addresses.add(follow);
        button(addresses,"Go",()->{follow.setSelected(false);update();});
        button(addresses,"Toggle Breakpoint",()->{try{int address=selectedCode();action(()->{if(!cpu.debugger.execute.remove(address))cpu.debugger.execute.add(address);});}catch(Exception e){status.setText(e.getMessage());}});
        button(addresses,"Run Here",()->codeAction(false));
        button(addresses,"Set PC",()->codeAction(true));
        commands.add(addresses,BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(codeTable),new JScrollPane(registerTable));
        split.setResizeWeight(.7);split.setDividerLocation(700);
        codeTable.getColumnModel().getColumn(0).setPreferredWidth(22);
        codeTable.getColumnModel().getColumn(1).setPreferredWidth(85);
        codeTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        codeTable.getColumnModel().getColumn(3).setPreferredWidth(360);commands.add(split,BorderLayout.CENTER);tabs.addTab("Commands",commands);
        tabs.addTab("CPU Registers",new JScrollPane(new JTable(cp0))); tabs.addTab("FPU Registers",new JScrollPane(new JTable(fpr)));
        String[] columns=new String[18];columns[0]="Address";for(int i=0;i<16;i++)columns[i+1]=String.format("%X",i);columns[17]="ASCII";
        memory=new DefaultTableModel(columns,0) {
            public boolean isCellEditable(int row,int col){return col>0 && col<17 && cpu.debugger.paused;}
            public void setValueAt(Object value,int row,int col) {
                if(updating){super.setValueAt(value,row,col);return;}
                int address=displayedMemory+row*16+col-1;
                action(()->{int number=Integer.parseInt(clean(value.toString()),16);if(number<0||number>255)throw new IllegalArgumentException("Enter one byte: 00–FF");cpu.debugger.write(address,new byte[]{(byte)number});});
            }
        };
        memoryTable=new JTable(memory);memoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        memoryTable.getColumnModel().getColumn(0).setPreferredWidth(90);for(int i=1;i<17;i++)memoryTable.getColumnModel().getColumn(i).setPreferredWidth(33);memoryTable.getColumnModel().getColumn(17).setPreferredWidth(180);
        JPanel memoryPanel=new JPanel(new BorderLayout()), memoryControls=new JPanel(new FlowLayout(FlowLayout.LEFT));
        memoryControls.add(new JLabel("Address (hex):"));memoryControls.add(memoryAddress);
        button(memoryControls,"Go",this::update);
        button(memoryControls,"Previous",()->moveMemory(-512));button(memoryControls,"Next",()->moveMemory(512));
        button(memoryControls,"Dump...",this::dump);memoryPanel.add(memoryControls,BorderLayout.NORTH);memoryPanel.add(new JScrollPane(memoryTable),BorderLayout.CENTER);
        tabs.addTab("Memory",memoryPanel);
        JPanel breakpointPanel=new JPanel(new BorderLayout()), breakpointControls=new JPanel(new FlowLayout(FlowLayout.LEFT));
        button(breakpointControls,"Add Execution...",()->addBreakpoint(0));
        button(breakpointControls,"Add Read...",()->addBreakpoint(1));button(breakpointControls,"Add Write...",()->addBreakpoint(2));
        button(breakpointControls,"Remove",()->{int selected=breakpointList.getSelectedIndex();action(()->{int count=cpu.debugger.execute.size();if(selected>=0 && selected<count)cpu.debugger.execute.remove(new ArrayList<>(cpu.debugger.execute).get(selected));else if(selected>=count && selected-count<cpu.debugger.watches.size())cpu.debugger.watches.remove(selected-count);});});
        button(breakpointControls,"Clear",()->action(()->{cpu.debugger.execute.clear();cpu.debugger.watches.clear();}));
        breakpointPanel.add(breakpointControls,BorderLayout.NORTH);breakpointPanel.add(new JScrollPane(breakpointList),BorderLayout.CENTER);
        breakpointPanel.add(new JLabel("Memory watchpoints: CPU accesses, physical/KSEG addresses; stop after the instruction. DMA is not watched."),BorderLayout.SOUTH);
        tabs.addTab("Breakpoints",breakpointPanel);
        trace.setEditable(false);trace.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        JPanel tracePanel=new JPanel(new BorderLayout());tracePanel.add(tracing,BorderLayout.NORTH);tracePanel.add(new JScrollPane(trace),BorderLayout.CENTER);
        tracing.addActionListener(e->{boolean selected=tracing.isSelected();action(()->cpu.debugger.tracing=selected);});tabs.addTab("Trace",tracePanel);
        for(JTable table:new JTable[]{codeTable,memoryTable}){table.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));table.setRowHeight(20);}
        add(tabs,BorderLayout.CENTER);
        JPanel footer=new JPanel(new GridLayout(2,1));footer.add(status);footer.add(new JLabel("F2: Step    F3: Step Over    F5: Run    F6: Break. Branch steps include their delay slot."));add(footer,BorderLayout.SOUTH);
        bind("F2",()->action(cpu.debugger::step));bind("F3",()->((JButton)toolbar.getComponent(3)).doClick());bind("F5",()->action(cpu.debugger::resume));bind("F6",()->action(()->cpu.debugger.pause("Paused by debugger")));
        setSize(1080,650);setMinimumSize(new Dimension(900,480));setLocationRelativeTo(Main.hMainWindow);
        refresh=new javax.swing.Timer(300,e->update());refresh.start();
        addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){refresh.stop();}});
    }
    private static DefaultTableModel readOnly(String... names){return new DefaultTableModel(names,0){public boolean isCellEditable(int r,int c){return false;}};}
    private static String clean(String value){return value.trim().replaceFirst("^(0[xX]|\\$)","");}
    private static int hex(String value){return Integer.parseUnsignedInt(clean(value),16);}
    private void guard(){if(Main.cpu!=cpu || !cpu.running)throw new IllegalStateException("ROM changed or stopped. Reopen the debugger.");}
    private static void button(JPanel panel,String text,Runnable action){JButton b=new JButton(text);b.addActionListener(e->action.run());panel.add(b);}
    private void bind(String key,Runnable action){getRootPane().registerKeyboardAction(e->action.run(),KeyStroke.getKeyStroke(key),JComponent.WHEN_IN_FOCUSED_WINDOW);}
    private void codeAction(boolean setPc){try{int address=selectedCode();action(()->{if(setPc)cpu.debugger.setPc(address);else cpu.debugger.runTo(address);});}catch(Exception e){status.setText(e.getMessage());}}
    private int selectedCode(){int row=codeTable.getSelectedRow();return row>=0?hex(code.getValueAt(row,1).toString()):hex(codeAddress.getText());}
    private void moveMemory(int delta){try{memoryAddress.setText(String.format("%08X",hex(memoryAddress.getText())+delta));update();}catch(Exception e){status.setText(e.getMessage());}}
    private void action(Runnable operation){
        try{guard();}catch(Exception e){status.setText(e.getMessage());return;}
        CompletableFuture.runAsync(()->{try{cpu.submitCommand(()->{guard();operation.run();}).get(10,TimeUnit.SECONDS);}catch(Exception e){throw new CompletionException(e);}})
            .whenComplete((v,e)->SwingUtilities.invokeLater(()->{if(e!=null)status.setText(message(e));else update();}));
    }
    private static String message(Throwable e){while(e.getCause()!=null)e=e.getCause();return e.getMessage();}
    private void addBreakpoint(int type){
        JTextField address=new JTextField(codeAddress.getText()),length=new JTextField("1");JPanel fields=new JPanel(new GridLayout(0,2));fields.add(new JLabel("Address (hex)"));fields.add(address);
        if(type!=0){fields.add(new JLabel("Length (decimal)"));fields.add(length);}
        if(JOptionPane.showConfirmDialog(this,fields,"Add breakpoint",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{int a=hex(address.getText()),n=Integer.parseInt(length.getText());if(n<1||n>0x4000000)throw new IllegalArgumentException("Invalid length");
            action(()->{if(type==0){if((a&3)!=0)throw new IllegalArgumentException("Execution address must be word aligned");cpu.debugger.execute.add(a);}else cpu.debugger.watches.add(new DebuggerCore.Watch(DebuggerCore.physical(a),n,type==1,type==2));});
        }catch(Exception e){status.setText(e.getMessage());}
    }
    private void dump(){
        JFileChooser chooser=new JFileChooser();if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        String length=JOptionPane.showInputDialog(this,"Length in bytes (1–65536)","512");if(length==null)return;
        try{int address=hex(memoryAddress.getText()),count=Integer.parseInt(length);Path path=chooser.getSelectedFile().toPath();
            if(count<1||count>65536)throw new IllegalArgumentException("Dump 1–65536 bytes");
            if(Files.exists(path)&&JOptionPane.showConfirmDialog(this,"Replace "+path.getFileName()+"?","Memory dump",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            CompletableFuture.runAsync(()->{try{byte[][] bytes=new byte[1][];cpu.submitCommand(()->{guard();bytes[0]=cpu.debugger.read(address,count);}).get(10,TimeUnit.SECONDS);Files.write(path,bytes[0]);}catch(Exception e){throw new CompletionException(e);}}).whenComplete((v,e)->SwingUtilities.invokeLater(()->status.setText(e==null?"Memory dump saved: "+path:message(e))));
        }catch(Exception e){status.setText(e.getMessage());}
    }
    private void update(){
        if(busy || !isDisplayable())return;
        try{guard();}catch(Exception e){status.setText(e.getMessage());refresh.stop();return;}
        int requestedCode,requestedMemory;
        try{requestedCode=hex(codeAddress.getText());requestedMemory=hex(memoryAddress.getText())&~15;}catch(Exception e){status.setText("Enter an eight-digit hexadecimal address");return;}
        boolean followPc=follow.isSelected();busy=true;
        CompletableFuture.supplyAsync(()->{try{Object[][] result=new Object[1][];
            cpu.submitCommand(()->{guard();int start=followPc?cpu.pc-32:requestedCode;
                ArrayList<Object[]> instructions=new ArrayList<>();for(int i=0;i<48;i++){int a=start+i*4;try{int op=ByteBuffer.wrap(cpu.debugger.read(a,4)).getInt();instructions.add(new Object[]{a==cpu.pc?">":cpu.debugger.execute.contains(a)?"B":"",String.format("%08X",a),String.format("%08X",op),MipsDisassembler.format(op,a)});}catch(Exception e){instructions.add(new Object[]{"",String.format("%08X",a),"????????","Unmapped"});}}
                ArrayList<Object[]> general=new ArrayList<>();String[] names={"zero","at","v0","v1","a0","a1","a2","a3","t0","t1","t2","t3","t4","t5","t6","t7","s0","s1","s2","s3","s4","s5","s6","s7","t8","t9","k0","k1","gp","sp","fp","ra"};
                for(int i=0;i<32;i++)general.add(new Object[]{"r"+i+" "+names[i],String.format("%016X",cpu.GPR[i])});general.add(new Object[]{"HI",String.format("%016X",cpu.HI)});general.add(new Object[]{"LO",String.format("%016X",cpu.LO)});
                ArrayList<Object[]> controls=new ArrayList<>(),floats=new ArrayList<>();for(int i=0;i<32;i++){controls.add(new Object[]{"CP0 "+i,String.format("%08X",cpu.cop0.CP0[i])});long raw=cpu.cop1.FPR[i].DW;floats.add(new Object[]{"F"+i,String.format("%016X",raw),Double.longBitsToDouble(raw)});}
                controls.add(new Object[]{"FPCSR",String.format("%08X",cpu.cop1.FPCR[31])});
                ArrayList<Object[]> rows=new ArrayList<>();for(int y=0;y<32;y++){Object[] row=new Object[18];row[0]=String.format("%08X",requestedMemory+y*16);StringBuilder ascii=new StringBuilder();for(int x=0;x<16;x++){try{int b=cpu.debugger.read(requestedMemory+y*16+x,1)[0]&255;row[x+1]=String.format("%02X",b);ascii.append(b>=32&&b<127?(char)b:'.');}catch(Exception e){row[x+1]="??";ascii.append('?');}}row[17]=ascii.toString();rows.add(row);}
                ArrayList<String> breakRows=new ArrayList<>();for(int a:cpu.debugger.execute)breakRows.add(String.format("Execute %08X",a));for(var w:cpu.debugger.watches)breakRows.add(String.format("%s %08X + %d bytes",w.write()?"Write":"Read",w.address(),w.length()));
                result[0]=new Object[]{instructions,general,controls,floats,rows,breakRows,String.join("\n",cpu.debugger.trace),cpu.pc,(cpu.debugger.paused?"Paused — ":"Running — ")+cpu.debugger.reason+" | RDRAM "+Main.mem.RDRAM.capacity()/1048576+" MB"};
            }).get(10,TimeUnit.SECONDS);return result[0];
        }catch(Exception e){throw new CompletionException(e);}}).whenComplete((data,error)->SwingUtilities.invokeLater(()->{
            busy=false;if(!isDisplayable())return;if(error!=null){status.setText(message(error));return;}
            int selectedRow=codeTable.getSelectedRow();String selectedAddress=selectedRow<0?null:code.getValueAt(selectedRow,1).toString();
            updating=true;try{fill(code,data[0]);if(selectedAddress!=null)for(int row=0;row<code.getRowCount();row++)if(selectedAddress.equals(code.getValueAt(row,1)))codeTable.setRowSelectionInterval(row,row);
                if(!registerTable.isEditing())fill(registers,data[1]);fill(cp0,data[2]);fill(fpr,data[3]);if(!memoryTable.isEditing()){fill(memory,data[4]);displayedMemory=requestedMemory;}
                int selected=breakpointList.getSelectedIndex();breaks.clear();for(Object row:(java.util.List<?>)data[5])breaks.addElement(row.toString());if(selected<breaks.size())breakpointList.setSelectedIndex(selected);
                trace.setText((String)data[6]);if(followPc)codeAddress.setText(String.format("%08X",(Integer)data[7]));status.setText((String)data[8]);
            }finally{updating=false;}
        }));
    }
    private static void fill(DefaultTableModel model,Object rows){model.setRowCount(0);for(Object row:(java.util.List<?>)rows)model.addRow((Object[])row);}
}
