package kerneln64.state;
import kerneln64.ui.HostMessages;
import kerneln64.ui.Screenshots;
import kerneln64.Main;


import java.io.IOException;
import java.util.ArrayDeque;

/** Host-only history. All snapshots and restores run between CPU instructions. */
public final class Rewind {
    private static final long LIMIT = 128L * 1024 * 1024;
    private record Entry(byte[] state, java.awt.image.BufferedImage frame) {
        long weight() { return state.length + (frame == null ? 0 : (long)frame.getWidth() * frame.getHeight() * 4); }
    }
    private static final ArrayDeque<Entry> history = new ArrayDeque<>();
    public static volatile boolean enabled, held;
    private static long bytes, nextStep;
    private static int interrupts;
    private static volatile boolean captureDue;

    public static synchronized void clear() {
        history.clear(); bytes = 0; interrupts = 0; captureDue = false; nextStep = 0;
    }
    public static synchronized void setEnabled(boolean value) {
        enabled = value; held = false; clear();
    }
    public static synchronized int size() { return history.size(); }
    public static synchronized void add(byte[] state) {
        add(state, null);
    }
    private static synchronized void add(byte[] state, java.awt.image.BufferedImage frame) {
        Entry entry = new Entry(state, frame);
        if (entry.weight() > LIMIT) return;
        history.addLast(entry); bytes += entry.weight();
        while (bytes > LIMIT || history.size() > 30) bytes -= history.removeFirst().weight();
    }
    public static synchronized void onVi() {
        if (enabled && !held && ++interrupts >= 60) { interrupts = 0; captureDue = true; }
    }
    /** Returns true while forward emulation should wait. */
    public static boolean service() {
        if (!enabled || (!held && !captureDue)) return false;
        try {
            if (held) {
                Entry entry = null;
                synchronized (Rewind.class) {
                    if (System.nanoTime() >= nextStep && !history.isEmpty()) {
                        entry = history.removeLast(); bytes -= entry.weight();
                        nextStep = System.nanoTime() + 180_000_000L;
                    }
                }
                if (entry != null) {
                    SaveStates.restore(entry.state());
                    captureDue = false; interrupts = 0;
                    Screenshots.present(entry.frame());
                    Main.video.resetTimer();
                }
                java.util.concurrent.locks.LockSupport.parkNanos(5_000_000L);
                return true;
            }
            boolean capture;
            synchronized (Rewind.class) { capture = captureDue; captureDue = false; nextStep = 0; }
            if (capture) {
                byte[] state = SaveStates.capture();
                java.awt.image.BufferedImage frame = Screenshots.currentFrame();
                if (enabled) add(state, frame);
            }
        } catch (Exception error) {
            setEnabled(false);
            HostMessages.error("Rewind disabled", error);
        }
        return false;
    }
}
