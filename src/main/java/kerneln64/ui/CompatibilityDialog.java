package kerneln64.ui;
import kerneln64.Main;


import java.awt.*;
import java.util.List;
import javax.swing.*;

final class CompatibilityDialog extends JDialog {
    CompatibilityDialog() {
        super(Main.hMainWindow, "Local Compatibility Database", true);
        List<String> hashes = CompatibilityDatabase.hashes();
        DefaultListModel<String> names = new DefaultListModel<>();
        for (String hash : hashes) names.addElement(CompatibilityDatabase.get(hash, "name", hash) + " — " + hash.substring(0, 8));
        JList<String> list = new JList<>(names); list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JComboBox<String> status = new JComboBox<>(new String[] {"Unknown", "Boots", "Playable", "Issues", "Broken"});
        JTextArea notes = new JTextArea(10, 40); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        JLabel tested = new JLabel("Select a ROM to view or edit your observations.");
        JTextField graphics = new JTextField(), audio = new JTextField(), input = new JTextField();
        graphics.setEditable(false); audio.setEditable(false); input.setEditable(false);
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 6));
        fields.add(new JLabel("Your compatibility rating:")); fields.add(status);
        fields.add(new JLabel("Saved graphics plugin:")); fields.add(graphics);
        fields.add(new JLabel("Saved audio plugin:")); fields.add(audio);
        fields.add(new JLabel("Saved controller plugin:")); fields.add(input);
        JPanel right = new JPanel(new BorderLayout(8, 8)); right.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        right.add(fields, BorderLayout.NORTH); right.add(new JScrollPane(notes), BorderLayout.CENTER); right.add(tested, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), right); split.setDividerLocation(255);
        add(new JLabel("  Local notes for each exact ROM revision; ratings are entered by you."), BorderLayout.NORTH); add(split, BorderLayout.CENTER);
        JButton save = new JButton("Save Notes"), capture = new JButton("Store Current Plugins"), use = new JButton("Use Saved Plugins"), close = new JButton("Close");
        list.addListSelectionListener(e -> {
            int index = list.getSelectedIndex(); if (index < 0 || e.getValueIsAdjusting()) return;
            String hash = hashes.get(index);
            status.setSelectedItem(CompatibilityDatabase.get(hash, "status", "Unknown")); notes.setText(CompatibilityDatabase.get(hash, "notes", ""));
            graphics.setText(CompatibilityDatabase.get(hash, "graphics", "")); audio.setText(CompatibilityDatabase.get(hash, "audio", "")); input.setText(CompatibilityDatabase.get(hash, "input", ""));
            tested.setText("Last noted: " + CompatibilityDatabase.get(hash, "tested", "not yet rated"));
        });
        capture.addActionListener(e -> {
            graphics.setText(Main.video != null && Main.video.gfxPlugin != null ? Main.video.gfxPlugin.getClass().getName() : PluginSettings.CLASSES[0][PluginSettings.selected(0)]);
            audio.setText(Main.audio != null && Main.audio.audioPlugin != null ? Main.audio.audioPlugin.getClass().getName() : PluginSettings.CLASSES[1][PluginSettings.selected(1)]);
            input.setText(Main.pif != null && Main.pif.controllerPlugin != null ? Main.pif.controllerPlugin.getClass().getName() : PluginSettings.CLASSES[2][PluginSettings.selected(2)]);
        });
        save.addActionListener(e -> {
            int index = list.getSelectedIndex(); if (index < 0) return;
            try { CompatibilityDatabase.update(hashes.get(index), (String)status.getSelectedItem(), notes.getText(), graphics.getText(), audio.getText(), input.getText()); tested.setText("Notes saved."); }
            catch (Exception error) { HostMessages.error("Compatibility Notes", error); }
        });
        use.addActionListener(e -> {
            String[] values = {graphics.getText(), audio.getText(), input.getText()};
            for (int i = 0; i < 3; i++) {
                if (values[i].isEmpty()) continue;
                if (!java.util.Arrays.asList(PluginSettings.CLASSES[i]).contains(values[i])) {
                    HostMessages.error("Plugin Profile", new IllegalArgumentException("Unavailable plugin: " + values[i])); return;
                }
            }
            for (int i = 0; i < 3; i++) if (!values[i].isEmpty()) Main.setPluginSetting(PluginSettings.KEYS[i], values[i]);
            Main.saveConfig(); tested.setText("Plugin selections saved. Restart or open a ROM to apply.");
        });
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(capture); buttons.add(use); buttons.add(save); buttons.add(close); add(buttons, BorderLayout.SOUTH);
        boolean any = !hashes.isEmpty(); save.setEnabled(any); capture.setEnabled(any); use.setEnabled(any);
        if (any) list.setSelectedIndex(Math.max(0, hashes.indexOf(Main.currentRomHash)));
        setSize(930, 470); setLocationRelativeTo(Main.hMainWindow); setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
