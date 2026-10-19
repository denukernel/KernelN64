package kerneln64.ui;
import kerneln64.state.StateSlots;
import kerneln64.Main;


import java.awt.Component;
import java.nio.file.*;
import javax.swing.*;

/** Slot UI lives outside the main menu and never renames the actual state files. */
final class StateSlotMenu {
    final JMenu menu=new JMenu("State Slot");
    private final JMenuItem save,load;
    private final Component parent;
    StateSlotMenu(Component parent,JMenuItem save,JMenuItem load) {
        this.parent=parent;this.save=save;this.load=load;
        menu.addMenuListener(new javax.swing.event.MenuListener(){
            public void menuSelected(javax.swing.event.MenuEvent e){refresh();}
            public void menuDeselected(javax.swing.event.MenuEvent e){}
            public void menuCanceled(javax.swing.event.MenuEvent e){}
        });
    }
    void refresh() {
        menu.removeAll();boolean ready=Main.currentRomFile!=null && Main.cpu!=null && Main.cpu.running;
        menu.setEnabled(ready);save.setEnabled(ready);load.setEnabled(ready);
        if(!ready){save.setText("Save State");load.setText("Load State");return;}
        try {
            StateSlots slots=StateSlots.current();ButtonGroup group=new ButtonGroup();
            save.setText("Save State — "+slots.label(slots.selected()));
            load.setText("Load State — "+slots.label(slots.selected()));
            for(int i=1;i<=StateSlots.COUNT;i++)add(slots,group,i);
            menu.addSeparator();add(slots,group,0);menu.addSeparator();
            JMenuItem rename=new JMenuItem("Rename Selected Slot...");
            rename.addActionListener(e->{
                if(!sameRom(slots))return;
                int selected=slots.selected();
                String text=(String)JOptionPane.showInputDialog(parent,"Name for "+slots.label(selected)+" (blank resets it):",
                    "Rename State Slot",JOptionPane.PLAIN_MESSAGE,null,null,slots.name(selected));
                if(text==null)return;
                try {if(sameRom(slots)){slots.rename(selected,text);refresh();}}
                catch(Exception error){HostMessages.error("State Slot",error);}
            });
            menu.add(rename);
        }catch(Exception error){
            save.setEnabled(false);load.setEnabled(false);
            JMenuItem failed=new JMenuItem("Cannot read slot settings: "+error.getMessage());failed.setEnabled(false);menu.add(failed);
        }
    }
    private void add(StateSlots slots,ButtonGroup group,int index) {
        Path path=slots.path(index);boolean exists=Files.isRegularFile(path);
        JRadioButtonMenuItem item=new JRadioButtonMenuItem(slots.label(index)+(exists?" [Saved]":" [Empty]"),slots.selected()==index);
        item.setToolTipText(path+(index>=1 && index<=9?" | Select with "+index+" while playing":""));group.add(item);menu.add(item);
        item.addActionListener(e->{
            try {if(sameRom(slots)){slots.select(index);refresh();}}
            catch(Exception error){HostMessages.error("State Slot",error);}
        });
    }
    private boolean sameRom(StateSlots slots) {
        try{return slots==StateSlots.current();}catch(Exception error){HostMessages.error("State Slot",error);return false;}
    }
}
