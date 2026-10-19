package aziaudio.config;
import aziaudio.AziAudioPlugin;

import java.awt.*;
import javax.swing.*;

public class AziAudioConfig extends JDialog {
    private final JSlider volume = slider(0, 100, AziAudioSettings.volume, 20);
    private final JSlider balance = slider(-100, 100, AziAudioSettings.balance, 100);
    private final JCheckBox mute = new JCheckBox("Mute", AziAudioSettings.mute);
    private final JCheckBox mono = new JCheckBox("Mono output", AziAudioSettings.mono);
    private final JCheckBox swap = new JCheckBox("Swap left and right channels", AziAudioSettings.swapChannels);
    private final JCheckBox fastMute = new JCheckBox("Mute during fast-forward", AziAudioSettings.muteFastForward);
    private final JCheckBox sync = new JCheckBox("Sync audio to game", AziAudioSettings.syncAudio);
    private final JCheckBox force = new JCheckBox("Force audio sync (compatibility)", AziAudioSettings.forceOldSync);
    private final JCheckBox ai = new JCheckBox("Emulate AI (Audio Interface)", AziAudioSettings.aiEmulation);
    private final JCheckBox hle = new JCheckBox("High Level Emulation (HLE)", AziAudioSettings.hle);
    private final JComboBox<String> latency = new JComboBox<>(new String[] {
        "Very low (about 50 ms)", "Low (about 100 ms)", "Medium (about 200 ms)", "High (about 400 ms)" });
    private final JComboBox<String> quality = new JComboBox<>(new String[] {
        "Cubic (recommended)", "Linear", "Nearest (no smoothing)" });
    private static final int[] DIVIDERS = {40, 20, 10, 5};

    public AziAudioConfig(JFrame parent) {
        super(parent, "Azimer Audio Settings", true);
        JTabbedPane tabs = new JTabbedPane();
        JPanel playback = page();
        playback.add(new JLabel("Volume:")); playback.add(volume); playback.add(mute);
        playback.add(Box.createVerticalStrut(10));
        playback.add(new JLabel("Stereo balance (left / center / right):")); playback.add(balance);
        playback.add(mono); playback.add(swap); playback.add(fastMute);
        playback.add(Box.createVerticalStrut(10));
        playback.add(new JLabel("Audio buffer target:")); playback.add(latency);
        playback.add(new JLabel("Actual latency also depends on the audio device."));
        latency.setSelectedIndex(2);
        for (int i = 0; i < DIVIDERS.length; i++) if (AziAudioSettings.bufferDivider == DIVIDERS[i]) latency.setSelectedIndex(i);
        mute.addActionListener(e -> volume.setEnabled(!mute.isSelected()));
        volume.setEnabled(!mute.isSelected());
        JPanel advanced = page();
        advanced.add(new JLabel("Resampling quality (Java audio backend):")); advanced.add(quality);
        quality.setSelectedIndex(AziAudioSettings.interpolation);
        quality.setEnabled(!(Boolean.getBoolean("kerneln64.nativeAudio") || Boolean.getBoolean("jario64.nativeAudio")));
        advanced.add(Box.createVerticalStrut(15));
        advanced.add(sync); advanced.add(force); advanced.add(ai); advanced.add(hle);
        tidy(playback); tidy(advanced);
        tabs.addTab("Playback", playback); tabs.addTab("Advanced", advanced);
        add(tabs, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton defaults = new JButton("Restore Defaults"), ok = new JButton("OK"), cancel = new JButton("Cancel"), apply = new JButton("Apply");
        defaults.addActionListener(e -> {
            volume.setValue(100); volume.setEnabled(true); balance.setValue(0);
            mute.setSelected(false); mono.setSelected(false); swap.setSelected(false); fastMute.setSelected(false);
            sync.setSelected(true); force.setSelected(false); ai.setSelected(true); hle.setSelected(true);
            latency.setSelectedIndex(2); quality.setSelectedIndex(0);
        });
        ok.addActionListener(e -> { apply(); dispose(); }); cancel.addActionListener(e -> dispose()); apply.addActionListener(e -> apply());
        buttons.add(defaults); buttons.add(ok); buttons.add(cancel); buttons.add(apply);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok); setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack(); setResizable(false); setLocationRelativeTo(parent);
    }

    private void apply() {
        AziAudioSettings.volume = volume.getValue(); AziAudioSettings.balance = balance.getValue();
        AziAudioSettings.mute = mute.isSelected(); AziAudioSettings.mono = mono.isSelected();
        AziAudioSettings.swapChannels = swap.isSelected(); AziAudioSettings.muteFastForward = fastMute.isSelected();
        AziAudioSettings.syncAudio = sync.isSelected(); AziAudioSettings.forceOldSync = force.isSelected();
        AziAudioSettings.aiEmulation = ai.isSelected(); AziAudioSettings.hle = hle.isSelected();
        AziAudioSettings.bufferDivider = DIVIDERS[latency.getSelectedIndex()];
        AziAudioSettings.interpolation = quality.getSelectedIndex();
        AziAudioSettings.save(); AziAudioPlugin.refreshSettings();
    }

    private static JPanel page() {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        return panel;
    }
    private static void tidy(JPanel panel) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JComponent child) {
                child.setAlignmentX(Component.LEFT_ALIGNMENT);
                child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getPreferredSize().height));
            }
        }
    }
    private static JSlider slider(int min, int max, int value, int ticks) {
        JSlider slider = new JSlider(min, max, value); slider.setMajorTickSpacing(ticks);
        slider.setPaintLabels(true); slider.setPaintTicks(true); return slider;
    }
}
