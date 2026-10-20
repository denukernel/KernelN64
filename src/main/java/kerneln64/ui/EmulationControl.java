package kerneln64.ui;

import kerneln64.Main;
import kerneln64.cpu.Cpu;
import java.awt.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;

/** Host controls serviced without changing guest state or blocking Swing on shutdown. */
public final class EmulationControl {
    private static boolean installed;
    private EmulationControl() {}

    public static boolean paused() {
        Cpu cpu = Main.cpu;
        return cpu != null && cpu.running && cpu.isPaused();
    }
    public static Future<?> togglePause() {
        Cpu cpu = Main.cpu;
        if (cpu == null || !cpu.running) return CompletableFuture.completedFuture(null);
        return cpu.submitCommand(() -> {
            if (Main.cpu != cpu || !cpu.running) return;
            if (cpu.isPaused()) {
                cpu.debugger.resume();
                EmulationStatus.reset();
            } else {
                cpu.pauseState.manual(true);
                KeyboardRouting.release();
            }
            refreshTitle();
        });
    }
    public static CompletableFuture<Void> end() {
        Cpu expected = Main.cpu;
        return CompletableFuture.runAsync(() -> {
            synchronized (Main.class) {
                if (Main.cpu != expected) return;
                try { Main.endEmulation(); }
                catch (java.io.IOException error) { throw new CompletionException(error); }
            }
        });
    }
    static boolean belongsToApplication(Window window) {
        for (Window next = window; next != null; next = next.getOwner())
            if (next == Main.hMainWindow) return true;
        return false;
    }
    static void install() {
        if (installed) return;
        installed = true;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow",
            event -> windowActive(belongsToApplication((Window) event.getNewValue())));
    }
    public static void applyFocusPolicy() {
        windowActive(belongsToApplication(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow()));
    }
    static void windowActive(boolean active) {
        Cpu cpu = Main.cpu;
        if (cpu == null) return;
        boolean wasPaused = cpu.pauseState.blocked();
        cpu.pauseState.inactive(CoreOptions.pauseWhenInactive && !active);
        if (CoreOptions.pauseWhenInactive && !active) KeyboardRouting.release();
        if (wasPaused && !cpu.pauseState.blocked() && Main.video != null) {
            Main.video.resetTimer();
            EmulationStatus.reset();
        }
        refreshTitle();
    }
    static void started() {
        Main.cpu.pauseState.clear();
        applyFocusPolicy();
    }
    static void refreshTitle() {
        SwingUtilities.invokeLater(() -> {
            if (Main.hMainWindow != null) {
                Main.hMainWindow.setTitle(Main.currentRomFile == null ? "KernelN64"
                    : EmulationStatus.windowTitle(Main.currentRomFile) + (paused() ? " [paused]" : ""));
                if (Main.hMainWindow.getJMenuBar() instanceof MenuBar menu) menu.refreshEmulationControls();
            }
        });
    }
}
