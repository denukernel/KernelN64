package kerneln64.ui;
import kerneln64.hardware.Audio;
import kerneln64.cpu.Cpu;
import kerneln64.state.Rewind;
import kerneln64.hardware.Video;
import kerneln64.Main;


import java.awt.*;
import javax.swing.*;

/** Project64-style category navigation with staged edits and Apply/OK/Cancel. */
public final class SettingsDialog extends JDialog {
    private final JCheckBox audio = new JCheckBox("Enable audio", Main.UseAudio);
    private final JCheckBox tlb = new JCheckBox("Use TLB", Main.UseTlb);
    private final JCheckBox cache = new JCheckBox("Cache instructions", Main.CacheInstructions);
    private final JCheckBox hideAdvanced = new JCheckBox("Hide advanced settings", CoreOptions.hideAdvanced);
    private final JCheckBox debugger = new JCheckBox("Enable debugger", CoreOptions.debuggerEnabled);
    private final JCheckBox pauseOnStart = new JCheckBox("Start emulation paused (debugger)", CoreOptions.pauseOnStart);
    private final JCheckBox memoryErrors = new JCheckBox("Report unhandled memory accesses", Main.ShowUnhandledMemory);
    private final JCheckBox pifErrors = new JCheckBox("Report PIF RAM errors", Main.ShowPifRamErrors);
    private final JCheckBox tlbErrors = new JCheckBox("Report TLB misses", Main.ShowTLBMisses);
    private final JCheckBox rewind = new JCheckBox("Enable rewind history", Rewind.enabled);
    private final JComboBox<Integer> ram = new JComboBox<>(CoreOptions.RAM_MB);
    private final JSpinner fastModeVi = new JSpinner(new SpinnerNumberModel(
            FastForwardOptions.targetVi(),FastForwardOptions.MIN_VI,FastForwardOptions.MAX_VI,60));
    private final JComboBox<String>[] plugins;
    private final JLabel pluginNote = new JLabel("Plugin changes apply to the running game without restarting.");

    public static void show(String category) { new SettingsDialog(category).setVisible(true); }

    @SuppressWarnings("unchecked")
    private SettingsDialog(String category) {
        super(Main.hMainWindow, "KernelN64 Settings", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(content);
        DefaultListModel<String> categories = new DefaultListModel<>();
        categories.addElement("General"); categories.addElement("Plugins");
        if (!hideAdvanced.isSelected()) categories.addElement("Advanced");
        JList<String> navigation = new JList<>(categories);
        hideAdvanced.addActionListener(e -> {
            if (hideAdvanced.isSelected()) { navigation.setSelectedValue("General", true); categories.removeElement("Advanced"); }
            else if (!categories.contains("Advanced")) categories.addElement("Advanced");
        });
        navigation.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navigation.setFixedCellWidth(120);
        navigation.setFixedCellHeight(30);
        content.add(new JScrollPane(navigation), BorderLayout.WEST);
        CardLayout cards = new CardLayout();
        JPanel pages = new JPanel(cards);
        JPanel general = page("General Settings");
        general.add(audio);
        general.add(hideAdvanced);
        general.add(Box.createVerticalStrut(12));
        JPanel speedRow=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        speedRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedRow.add(new JLabel("Fast Mode (F4) target: "));speedRow.add(fastModeVi);speedRow.add(new JLabel(" VI/s"));
        speedRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));general.add(speedRow);
        fastModeVi.setToolTipText("120–1200 VI/s (2×–20× normal speed). Higher targets depend on your computer.");
        general.add(new JLabel("F4 toggles normal / fast mode. Apply updates the running speed."));
        general.add(new JLabel("1–9: select slot    F5: save    F7: load    Ctrl+R: restart"));
        general.add(Box.createVerticalStrut(12));
        general.add(new JLabel("Use Settings > Emulation Info to choose status-bar readings."));
        pages.add(general, "General");
        JPanel pluginPage = page("Plugin Selection");
        plugins = new JComboBox[3];
        String[] labels = { "Video (graphics) plugin", "Audio (sound) plugin", "Input (controller) plugin" };
        for (int i = 0; i < 3; i++) {
            pluginPage.add(new JLabel(labels[i]));
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            plugins[i] = new JComboBox<>(PluginSettings.NAMES[i]);
            plugins[i].setSelectedIndex(PluginSettings.selected(i));
            row.add(plugins[i], BorderLayout.CENTER);
            final int type = i;
            JButton configure = new JButton("Configure...");
            configure.addActionListener(e -> PluginSettings.configure(type));
            configure.setToolTipText("Configure the active plugin; Apply activates the selected plugin");
            row.add(configure, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            pluginPage.add(row);
            pluginPage.add(Box.createVerticalStrut(10));
        }
        pluginPage.add(new JLabel("RSP: built-in HLE"));
        pluginPage.add(Box.createVerticalStrut(12));
        pluginPage.add(pluginNote);
        pages.add(pluginPage, "Plugins");
        JPanel advanced = page("Advanced Emulation Settings");
        ram.setSelectedItem(CoreOptions.ramMb);
        JPanel memoryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        memoryRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        memoryRow.add(new JLabel("RDRAM size (MB): ")); memoryRow.add(ram);
        memoryRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); advanced.add(memoryRow);
        advanced.add(new JLabel("8 MB: standard expansion RAM. 16–64 MB: experimental hacks."));
        advanced.add(tlb);
        advanced.add(cache);
        advanced.add(debugger);
        advanced.add(pauseOnStart);
        advanced.add(memoryErrors);
        advanced.add(pifErrors);
        advanced.add(tlbErrors);
        advanced.add(rewind);
        advanced.add(Box.createVerticalStrut(12));
        advanced.add(new JLabel("Core changes take effect when you restart or open a ROM."));
        pages.add(advanced, "Advanced");
        content.add(pages, BorderLayout.CENTER);
        navigation.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cards.show(pages, navigation.getSelectedValue());
        });
        navigation.setSelectedValue(categories.contains(category) ? category : "General", true);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton ok = new JButton("OK"), cancel = new JButton("Cancel"), apply = new JButton("Apply");
        ok.addActionListener(e -> apply(true));
        cancel.addActionListener(e -> dispose());
        apply.addActionListener(e -> apply(false));
        buttons.add(ok); buttons.add(cancel); buttons.add(apply);
        content.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(Main.hMainWindow);
    }

    private static JPanel page(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setPreferredSize(new Dimension(475, 340));
        return panel;
    }

    private void apply(boolean close) {
        try{fastModeVi.commitEdit();}
        catch(java.text.ParseException error){JOptionPane.showMessageDialog(this,"Choose 120–1200 VI/s.","Fast Mode",JOptionPane.ERROR_MESSAGE);return;}
        FastForwardOptions.setTargetVi((Integer)fastModeVi.getValue());
        Main.UseAudio = audio.isSelected();
        Main.UseTlb = tlb.isSelected();
        Main.CacheInstructions = cache.isSelected();
        CoreOptions.ramMb = (Integer) ram.getSelectedItem();
        CoreOptions.hideAdvanced = hideAdvanced.isSelected();
        CoreOptions.debuggerEnabled = debugger.isSelected();
        CoreOptions.pauseOnStart = pauseOnStart.isSelected();
        Main.ShowUnhandledMemory = memoryErrors.isSelected();
        Main.ShowPifRamErrors = pifErrors.isSelected();
        Main.ShowTLBMisses = tlbErrors.isSelected();
        Rewind.setEnabled(rewind.isSelected());
        if (Main.dma != null) Main.dma.showUnhandledMemory(Main.ShowUnhandledMemory);
        if (Main.pif != null) Main.pif.showPifRamErrors(Main.ShowPifRamErrors);
        if (!CoreOptions.debuggerEnabled && Main.cpu != null && Main.cpu.running) {
            Cpu current = Main.cpu; current.submitCommand(current.debugger::resume);
        }
        String[] choices=new String[3];
        for(int i=0;i<3;i++)choices[i]=PluginSettings.CLASSES[i][plugins[i].getSelectedIndex()];
        pluginNote.setText("Applying plugin changes...");setEnabled(false);
        LivePlugins.apply(choices).whenComplete((unused,error)->SwingUtilities.invokeLater(()->{
            setEnabled(true);
            if(error!=null){pluginNote.setText("Plugin change failed.");JOptionPane.showMessageDialog(this,error.getMessage(),"Plugin Change",JOptionPane.ERROR_MESSAGE);}
            else {pluginNote.setText("Plugins applied to the running game.");if(close)dispose();}
        }));
    }
}
