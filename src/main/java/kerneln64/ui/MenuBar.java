package kerneln64.ui;
import kerneln64.hardware.Audio;
import kerneln64.cpu.Cpu;
import kerneln64.debugger.trace.Debug;
import kerneln64.debugger.DebuggerDialog;
import kerneln64.debugger.KernelThreadDialog;
import kerneln64.memory.Memory;
import kerneln64.debugger.MemorySearchDialog;
import kerneln64.cpu.Registers;
import kerneln64.state.Rewind;
import kerneln64.state.StateSlots;
import kerneln64.Main;


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.*;


import java.awt.*;

public class MenuBar extends JMenuBar {
    private StateSlotMenu stateSlots;

    public MenuBar() {
        setBorderPainted(false);
        add(createFileMenu());
        add(createSettingsMenu());
        add(createDebugMenu());
        add(createToolsMenu());
        add(createPluginMenu());
        add(createHelpMenu());

        add(Box.createHorizontalGlue()); 
    }

    private JMenu createHelpMenu() {
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About KernelN64...");
        about.addActionListener(e -> JOptionPane.showMessageDialog(Main.hMainWindow,
                "KernelN64\nJava Nintendo 64 emulator\nDevelopment preview\n\n"
                + "Third-party credits and licenses are included in\nTHIRD-PARTY-NOTICES.md and the licenses folder.",
                "About KernelN64", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        return help;
    }
    private JMenu createPluginMenu() {
        JMenu plugins = new JMenu("Plugins");

        String[] labels = { "Configure Graphics Plugin...", "Configure Audio Plugin...", "Configure Controller Plugin..." };
        int[] keys = { KeyEvent.VK_V, KeyEvent.VK_U, KeyEvent.VK_X };
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            JMenuItem item = new JMenuItem(labels[i]);
            item.setAccelerator(KeyStroke.getKeyStroke(keys[i], ActionEvent.CTRL_MASK));
            item.addActionListener(e -> PluginSettings.configure(index));
            plugins.add(item);
        }
        plugins.addSeparator();
        JMenuItem select = new JMenuItem("Select Plugins...");
        select.addActionListener(e -> SettingsDialog.show("Plugins"));
        plugins.add(select);

        return plugins;
    }

    private JMenu createFileMenu() {
        JMenu file = new JMenu("File");

        JMenuItem openRom = new JMenuItem("Open ROM...");
        openRom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openRom.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(Main.lastRomDir);
            chooser.setDialogTitle("Open ROM");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Nintendo 64 ROMs (*.z64, *.n64, *.v64)", "z64", "n64", "v64"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) openRom(chooser.getSelectedFile());
        });

        JMenuItem exit = new JMenuItem("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        exit.addActionListener(e -> Main.exit("Goodbye"));

        file.add(openRom);
        JMenu recent = new JMenu("Recent ROMs");
        recent.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuSelected(javax.swing.event.MenuEvent e) {
                recent.removeAll();
                int index = 1;
                for (java.io.File rom : Main.recentRoms.files()) {
                    JMenuItem item = new JMenuItem(index++ + "  " + rom.getName());
                    item.setToolTipText(rom.getPath());
                    item.addActionListener(event -> openRom(rom));
                    recent.add(item);
                }
                if (Main.recentRoms.files().isEmpty()) {
                    JMenuItem empty = new JMenuItem("No recent ROMs");
                    empty.setEnabled(false);
                    recent.add(empty);
                } else {
                    recent.addSeparator();
                    JMenuItem clear = new JMenuItem("Clear List");
                    clear.addActionListener(event -> { Main.recentRoms.clear(); Main.saveConfig(); });
                    recent.add(clear);
                }
            }
            public void menuDeselected(javax.swing.event.MenuEvent e) {}
            public void menuCanceled(javax.swing.event.MenuEvent e) {}
        });
        file.add(recent);
        file.addSeparator();
        JMenuItem saveState = new JMenuItem("Save State");
        saveState.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        saveState.addActionListener(e -> stateAction(false));
        JMenuItem loadState = new JMenuItem("Load State");
        loadState.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        loadState.addActionListener(e -> stateAction(true));
        JMenuItem screenshot = new JMenuItem("Capture Screenshot");
        screenshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        screenshot.addActionListener(e -> Screenshots.request());
        file.add(screenshot);
        file.add(saveState);
        file.add(loadState);
        stateSlots=new StateSlotMenu(this,saveState,loadState);file.add(stateSlots.menu);
        JMenuItem saveAs=new JMenuItem("Save State As...");
        saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5,ActionEvent.CTRL_MASK));
        saveAs.addActionListener(e->StateFileActions.choose(this,false,true,stateSlots::refresh));
        JMenuItem loadFrom=new JMenuItem("Load State From...");
        loadFrom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7,ActionEvent.CTRL_MASK));
        loadFrom.addActionListener(e->StateFileActions.choose(this,true,true,stateSlots::refresh));
        file.add(saveAs);file.add(loadFrom);
        file.addMenuListener(new javax.swing.event.MenuListener(){
            public void menuSelected(javax.swing.event.MenuEvent e){stateSlots.refresh();}
            public void menuDeselected(javax.swing.event.MenuEvent e){}
            public void menuCanceled(javax.swing.event.MenuEvent e){}
        });
        file.addSeparator();
        file.add(exit);

        return file;
    }

    private JMenu createSettingsMenu() {
        JMenu settings = new JMenu("Settings");

        JMenuItem configuration = new JMenuItem("Settings...");
        configuration.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
        configuration.addActionListener(e -> SettingsDialog.show("General"));
        settings.add(configuration);
        JCheckBoxMenuItem rewind = new JCheckBoxMenuItem("Enable Rewind (hold Backspace)", Rewind.enabled);
        rewind.addActionListener(e -> { Rewind.setEnabled(rewind.isSelected()); Main.saveConfig(); });
        settings.add(rewind);
        settings.addSeparator();
        JMenu info = new JMenu("Emulation Info");
        String[] names = { "R4300i", "GFX", "AList", "Idle", "VI/s" };
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(names[i], EmulationStatus.isShown(i));
            item.addActionListener(e -> {
                EmulationStatus.setShown(index, item.isSelected());
                Main.saveConfig();
            });
            info.add(item);
        }
        settings.add(info);

        return settings;
    }

    private JMenu createDebugMenu() {
        JMenu debug = new JMenu("Debug");
        String[] debuggerPages = {"Commands", "Memory", "Breakpoints", "CPU Registers", "FPU Registers", "Trace"};
        for (String page : debuggerPages) {
            JMenuItem item = new JMenuItem(page + "...");
            item.addActionListener(e -> DebuggerDialog.show(page));
            debug.add(item);
        }
        debug.addSeparator();

        JMenuItem kernel = new JMenuItem("Kernel Thread Monitor...");
        kernel.addActionListener(e -> KernelThreadDialog.showMonitor());
        debug.add(kernel);
        debug.addSeparator();

        JCheckBoxMenuItem unhandled = new JCheckBoxMenuItem("Show Unhandled Memory", Main.ShowUnhandledMemory);
        unhandled.addActionListener(e -> {
            Main.ShowUnhandledMemory = unhandled.isSelected();
            Main.saveConfig();
        });

        JCheckBoxMenuItem piff = new JCheckBoxMenuItem("Show PIF RAM Errors", Main.ShowPifRamErrors);
        piff.addActionListener(e -> {
            Main.ShowPifRamErrors = piff.isSelected();
            Main.saveConfig();
        });
        JCheckBoxMenuItem tlbMiss = new JCheckBoxMenuItem("Show TLB Misses", Main.ShowTLBMisses);
        tlbMiss.addActionListener(e -> {
            Main.ShowTLBMisses = tlbMiss.isSelected();
            Main.saveConfig();
        });

        debug.add(unhandled);
        debug.add(piff);
        debug.add(tlbMiss);

        return debug;
    }

    private JMenu createToolsMenu() {
        JMenu tools = new JMenu("Tools");
        JMenuItem cheats = new JMenuItem("GameShark Codes...");
        cheats.addActionListener(e -> editCheats());
        tools.add(cheats);
        JMenuItem memory = new JMenuItem("Memory Search...");
        memory.addActionListener(e -> {
            if (Main.cpu == null || !Main.cpu.running) { HostMessages.error("Memory Search", new IllegalStateException("Open a ROM first.")); return; }
            new MemorySearchDialog().setVisible(true);
        });
        tools.add(memory);
        JMenuItem compatibility = new JMenuItem("Compatibility Database...");
        compatibility.addActionListener(e -> new CompatibilityDialog().setVisible(true));
        tools.add(compatibility);
        JMenuItem button = new JMenuItem("GameShark Button");
        button.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        button.addActionListener(e -> Main.cheats.pressButton());
        tools.add(button);

        JMenuItem dumpInst = new JCheckBoxMenuItem("Dump Instructions", Cpu.dump_inst);
        dumpInst.addActionListener(e -> {
            Cpu.dump_inst = ((JCheckBoxMenuItem) dumpInst).isSelected();
        });

        JMenuItem reset = new JMenuItem("Restart Emulation");
        reset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        reset.addActionListener(e -> {
            reset.setEnabled(false);
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception {
                    Main.restartEmulation();
                    return null;
                }
                protected void done() {
                    reset.setEnabled(true);
                    try { get(); }
                    catch (Exception error) {
                        Throwable cause = error;
                        while (cause.getCause() != null) cause = cause.getCause();
                        JOptionPane.showMessageDialog(MenuBar.this, cause.getMessage(), "Restart Emulation", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        tools.add(dumpInst);
        tools.addSeparator();
        tools.add(reset);

        return tools;
    }

    private void openRom(java.io.File rom) {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception { Main.loadRom(rom); return null; }
            protected void done() {
                try { get(); }
                catch (Exception error) {
                    Throwable cause = error;
                    while (cause.getCause() != null) cause = cause.getCause();
                    JOptionPane.showMessageDialog(MenuBar.this, cause.getMessage(), "Open ROM", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    void selectStateSlot(int slot) {
        try {
            StateSlots slots=StateSlots.current();
            if(slots.selected()!=slot)slots.select(slot);
            stateSlots.refresh();
        }catch(Exception error){HostMessages.error("State Slot",error);}
    }

    private void stateAction(boolean load) {
        StateFileActions.choose(this,load,false,stateSlots::refresh);
    }

    private void editCheats() {
        if(Main.currentRomFile==null){JOptionPane.showMessageDialog(this,"Open a ROM first.");return;}
        try{CheatDialog.open();}catch(IOException error){JOptionPane.showMessageDialog(this,error.getMessage(),"Cheats",JOptionPane.ERROR_MESSAGE);}
    }
}
