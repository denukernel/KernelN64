package kerneln64.ui;
import kerneln64.cpu.Cpu;
import kerneln64.state.SaveStates;
import kerneln64.Main;


import java.awt.Component;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** File dialogs and CPU-boundary state operations share the same ROM identity checks. */
final class StateFileActions {
    record Session(Cpu cpu,File rom,String hash) {
        void verify() throws IOException {
            if(Main.cpu!=cpu || !cpu.running || !rom.equals(Main.currentRomFile)
                    || !hash.equals(Main.currentRomHash))throw new IOException("The active ROM changed.");
        }
    }
    static Path savePath(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".j64state")
                ?path:path.resolveSibling(path.getFileName()+".j64state");
    }
    static void choose(Component parent,boolean load,boolean toFile,Runnable refreshed) {
        String title=load?"Load State":"Save State";
        if(Main.currentRomFile==null || Main.cpu==null || !Main.cpu.running) {
            JOptionPane.showMessageDialog(parent,"Open a ROM first.");return;
        }
        Session session=new Session(Main.cpu,Main.currentRomFile,Main.currentRomHash);
        try {
            Path path=Main.stateFile().toPath();
            if(toFile) {
                JFileChooser chooser=new JFileChooser(new File(Main.AUTO_SAVE_DIR));
                chooser.setDialogTitle(load?"Load State From File":"Save State As File");
                chooser.setFileFilter(new FileNameExtensionFilter("KernelN64 savestates (*.j64state)","j64state"));
                if(!load)chooser.setSelectedFile(new File(session.rom().getName()+".j64state"));
                int answer=load?chooser.showOpenDialog(parent):chooser.showSaveDialog(parent);
                if(answer!=JFileChooser.APPROVE_OPTION)return;
                session.verify();path=chooser.getSelectedFile().toPath().toAbsolutePath();
                if(!load) {
                    path=savePath(path);
                    if(Files.exists(path) && JOptionPane.showConfirmDialog(parent,"Replace this savestate?\n"+path,
                            title,JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
                }
            }
            execute(session,path,load).whenComplete((unused,error)->SwingUtilities.invokeLater(()->{
                if(error!=null)HostMessages.error(title,error);else refreshed.run();
            }));
        }catch(Exception error){HostMessages.error(title,error);}
    }
    static CompletableFuture<Void> execute(Session session,Path path,boolean load) {
        var pending=session.cpu().submitCommand(()->{
            try {
                session.verify();
                if(load && !Files.isRegularFile(path))throw new IOException("Savestate file does not exist: "+path);
                if(load)SaveStates.load(path);else SaveStates.save(path);
            }catch(IOException error){throw new UncheckedIOException(error);}
        });
        return CompletableFuture.runAsync(()->{
            try {pending.get();}
            catch(InterruptedException error){Thread.currentThread().interrupt();throw new java.util.concurrent.CompletionException(error);}
            catch(java.util.concurrent.ExecutionException error){throw new java.util.concurrent.CompletionException(error.getCause());}
        });
    }
}
