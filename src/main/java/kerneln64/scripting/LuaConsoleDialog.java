package kerneln64.scripting;

import kerneln64.Main;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Modeless script editor. All script execution is queued to the emulation thread. */
public final class LuaConsoleDialog extends JDialog {
    private static LuaConsoleDialog current;
    private final JTextArea editor=new JTextArea(18,85),output=new JTextArea(10,85);
    private final JLabel state=new JLabel("no script running");
    private Path file;
    private final Timer timer;
    public static void showConsole(){if(current==null||!current.isDisplayable())current=new LuaConsoleDialog();current.setVisible(true);current.toFront();}
    private LuaConsoleDialog(){
        super(Main.hMainWindow,"KernelN64 lua scripting",false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        editor.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));output.setFont(editor.getFont());output.setEditable(false);
        editor.setText("-- callbacks run once per observed video interrupt, on the cpu thread\nprint(emu.romname(), emu.romhash())\nevent.onframe(function(frame)\n    if frame % 60 == 0 then print(\"frame\", frame) end\nend)\n");
        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton open=new JButton("open..."),save=new JButton("save as..."),run=new JButton("run / restart"),stop=new JButton("stop"),clear=new JButton("clear log"),help=new JButton("api help");
        open.addActionListener(e->choose(false));save.addActionListener(e->choose(true));
        run.addActionListener(e->{try{var task=LuaScripts.run(editor.getText(),file==null?"editor.lua":file.getFileName().toString());CompletableFuture.runAsync(()->{try{task.get();}catch(Exception error){LuaScripts.log("could not start script: "+error.getMessage());}});}catch(Exception error){LuaScripts.log(error.getMessage());}});
        stop.addActionListener(e->LuaScripts.requestStop());clear.addActionListener(e->LuaScripts.clearLog());
        help.addActionListener(e->JOptionPane.showMessageDialog(this,"event.onframe(fn), event.onloadstate(fn), event.onstop(fn)\n"
            +"emu.framecount(), romname(), romhash(), ram_size(), getregister(name)\n"
            +"emu.pause(), resume(), stop()\nmemory.read_u8/u16/u32, read_s8/s16/s32, read_float, read_bytes\n"
            +"memory.write_u8/u16/u32, write_float(address, value)\njoypad.get(port), set(table, port), clear(port)\nsavestate.save(slot), load(slot)\n\n"
            +"ports 1-4; slots 1-10. memory uses physical/KSEG0/KSEG1 RDRAM.\n"
            +"run scripts explicitly; ROM changes stop them.\nsee docs/Lua-scripting.md and lua/examples for details.","lua api",JOptionPane.INFORMATION_MESSAGE));
        for(JButton button:new JButton[]{open,save,run,stop,clear,help})toolbar.add(button);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(editor),new JScrollPane(output));split.setResizeWeight(.65);
        add(toolbar,BorderLayout.NORTH);add(split,BorderLayout.CENTER);add(state,BorderLayout.SOUTH);
        timer=new Timer(200,e->{var snapshot=LuaScripts.snapshot();state.setText(snapshot.status());if(!output.getText().equals(snapshot.output())){output.setText(snapshot.output());output.setCaretPosition(output.getDocument().getLength());}});timer.start();
        pack();setLocationRelativeTo(Main.hMainWindow);
    }
    private void choose(boolean saving){
        JFileChooser chooser=new JFileChooser(file==null?Path.of("lua/examples").toFile():file.getParent().toFile());chooser.setFileFilter(new FileNameExtensionFilter("lua scripts (*.lua)","lua"));
        if((saving?chooser.showSaveDialog(this):chooser.showOpenDialog(this))!=JFileChooser.APPROVE_OPTION)return;
        Path selected=chooser.getSelectedFile().toPath().toAbsolutePath();
        try{
            if(saving){
                if(!selected.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".lua"))selected=selected.resolveSibling(selected.getFileName()+".lua");
                if(Files.exists(selected)&&JOptionPane.showConfirmDialog(this,"replace "+selected.getFileName()+"?","save script",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                Files.writeString(selected,editor.getText(),java.nio.charset.StandardCharsets.UTF_8);
            }else{if(Files.size(selected)>1048576)throw new java.io.IOException("script file is too large");String text=Files.readString(selected);if(text.length()>LuaScripts.MAX_SOURCE)throw new java.io.IOException("script is too large");editor.setText(text);}
            file=selected;setTitle("KernelN64 lua scripting - "+file.getFileName());
        }catch(Exception error){JOptionPane.showMessageDialog(this,error.getMessage(),"lua script",JOptionPane.ERROR_MESSAGE);}
    }
    @Override public void dispose(){timer.stop();super.dispose();}
}
