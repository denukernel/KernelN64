package gln64j.config;
import gln64j.GLN64jPlugin;

import java.awt.*;
import javax.swing.*;

public final class GLN64jConfig extends JDialog {
    public GLN64jConfig(JFrame parent) {
        super(parent, "glN64j Settings", true);
        JPanel page = new JPanel(new GridLayout(0, 2, 10, 10));
        page.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JSpinner width = new JSpinner(new SpinnerNumberModel(GLN64jPluginSettings.windowWidth, 320, 3840, 80));
        JSpinner height = new JSpinner(new SpinnerNumberModel(GLN64jPluginSettings.windowHeight, 240, 2160, 60));
        JComboBox<String> presets = new JComboBox<>(new String[] { "Custom", "640 × 480", "800 × 600", "960 × 720", "1280 × 960", "1600 × 1200" });
        presets.addActionListener(e -> {
            int i = presets.getSelectedIndex();
            int[] widths = {0, 640, 800, 960, 1280, 1600};
            if (i > 0) { width.setValue(widths[i]); height.setValue(widths[i] * 3 / 4); }
        });
        JCheckBox lock = new JCheckBox("Lock window size", GLN64jPluginSettings.lockWindow);
        JCheckBox top = new JCheckBox("Always on top", GLN64jPluginSettings.alwaysOnTop);
        JCheckBox fog = new JCheckBox("Enable fog", GLN64jPluginSettings.enableFog);
        JCheckBox filter = new JCheckBox("Bilinear filtering", GLN64jPluginSettings.bilinear);
        JCheckBox wire = new JCheckBox("Wireframe mode", GLN64jPluginSettings.wireframe);
        page.add(new JLabel("Render area preset:")); page.add(presets);
        page.add(new JLabel("Width (pixels):")); page.add(width);
        page.add(new JLabel("Height (pixels):")); page.add(height);
        page.add(lock); page.add(top); page.add(fog); page.add(filter); page.add(wire); page.add(new JLabel());
        java.util.function.BooleanSupplier apply = () -> {
            try { width.commitEdit(); height.commitEdit(); }
            catch (java.text.ParseException error) { JOptionPane.showMessageDialog(this, "Enter a valid width and height."); return false; }
            GLN64jPluginSettings.windowWidth = ((Number) width.getValue()).intValue();
            GLN64jPluginSettings.windowHeight = ((Number) height.getValue()).intValue();
            GLN64jPluginSettings.lockWindow = lock.isSelected(); GLN64jPluginSettings.alwaysOnTop = top.isSelected();
            GLN64jPluginSettings.enableFog = fog.isSelected(); GLN64jPluginSettings.bilinear = filter.isSelected();
            GLN64jPluginSettings.wireframe = wire.isSelected();
            GLN64jPluginSettings.save();
            if (GLN64jPlugin.hWnd == parent) GLN64jPluginSettings.applyWindow(parent);
            return true;
        };
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK"), cancel = new JButton("Cancel"), applyButton = new JButton("Apply");
        ok.addActionListener(e -> { if (apply.getAsBoolean()) dispose(); }); cancel.addActionListener(e -> dispose()); applyButton.addActionListener(e -> apply.getAsBoolean());
        buttons.add(ok); buttons.add(cancel); buttons.add(applyButton);
        add(page, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok); setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack(); setResizable(false); setLocationRelativeTo(parent);
    }
}
