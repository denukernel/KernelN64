package kerneln64.ui;
import kerneln64.Main;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.Locale;
import java.util.Properties;
import javax.swing.*;

/** Wall-clock breakdown on the emulation thread, presented on Swing's EDT. */
public final class EmulationStatus extends JPanel {
    private static final String[] KEYS = { "cpu", "gfx", "alist", "idle", "vi" };
    private static final boolean[] shown = { true, true, true, true, true };
    private final JLabel[] readings = new JLabel[5];
    // Kept outside guest hardware so loading a state cannot rewind measurements.
    public static long graphicsNanos, audioNanos, idleNanos;
    private static long start, viCount;

    public EmulationStatus() {
        super(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        JPanel usage = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        usage.setOpaque(false);
        String[] names = { "R4300i", "GFX", "AList", "Idle", "VI/s" };
        for (int i = 0; i < readings.length; i++) readings[i] = new JLabel(names[i] + ": —");
        for (int i = 0; i < 4; i++) usage.add(readings[i]);
        add(usage, BorderLayout.CENTER);
        add(readings[4], BorderLayout.EAST);
        readings[0].setToolTipText("Emulation-thread time outside graphics, audio lists, and frame pacing");
        readings[3].setToolTipText("Time waiting for the next video interrupt");
        refreshVisibility();
    }

    static String windowTitle(File rom) {
        String name = rom.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name) + " - KernelN64";
    }

    static void loadSettings(Properties config) {
        for (int i = 0; i < shown.length; i++)
            shown[i] = Boolean.parseBoolean(config.getProperty("info." + KEYS[i], "true"));
    }

    static void saveSettings(Properties config) {
        for (int i = 0; i < shown.length; i++)
            config.setProperty("info." + KEYS[i], Boolean.toString(shown[i]));
    }

    static boolean isShown(int index) { return shown[index]; }

    static void setShown(int index, boolean value) {
        shown[index] = value;
        if (Main.emulationStatus != null) Main.emulationStatus.refreshVisibility();
    }

    private void refreshVisibility() {
        boolean any = false;
        for (int i = 0; i < shown.length; i++) {
            readings[i].setVisible(shown[i]);
            any |= shown[i];
        }
        setVisible(any);
        revalidate();
    }

    static void reset() {
        start = System.nanoTime();
        viCount = graphicsNanos = audioNanos = idleNanos = 0;
        SwingUtilities.invokeLater(() -> {
            if (Main.emulationStatus != null) {
                String[] names = { "R4300i", "GFX", "AList", "Idle", "VI/s" };
                for (int i = 0; i < names.length; i++)
                    Main.emulationStatus.readings[i].setText(names[i] + ": —");
            }
        });
    }

    public static void vi() {
        long now = System.nanoTime();
        if (start == 0) { start = now; graphicsNanos = audioNanos = idleNanos = 0; }
        viCount++;
        long elapsed = now - start;
        if (elapsed < 500_000_000L) return;
        double scale = 100.0 / elapsed;
        String[] values = {
            String.format(Locale.ROOT, "R4300i: %.2f%%", Math.max(0L, elapsed - graphicsNanos - audioNanos - idleNanos) * scale),
            String.format(Locale.ROOT, "GFX: %.2f%%", graphicsNanos * scale),
            String.format(Locale.ROOT, "AList: %.2f%%", audioNanos * scale),
            String.format(Locale.ROOT, "Idle: %.2f%%", idleNanos * scale),
            String.format(Locale.ROOT, "VI/s: %.1f", viCount * 1_000_000_000.0 / elapsed)
        };
        start = now;
        viCount = graphicsNanos = audioNanos = idleNanos = 0;
        if (Main.emulationStatus != null) SwingUtilities.invokeLater(() -> {
            if (Main.emulationStatus != null)
                for (int i = 0; i < values.length; i++) Main.emulationStatus.readings[i].setText(values[i]);
        });
    }
}
