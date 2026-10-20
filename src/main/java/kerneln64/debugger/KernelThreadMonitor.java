package kerneln64.debugger;
import kerneln64.hardware.Audio;
import kerneln64.ui.CoreOptions;
import kerneln64.cpu.Cpu;
import kerneln64.memory.Memory;
import kerneln64.cheats.Sm64Compatibility;
import kerneln64.Main;


import java.util.*;

/** Host-only sampling session, deliberately excluded from guest savestates. */
public final class KernelThreadMonitor {
    public static volatile KernelThreadMonitor active;
    /** Called only at outer CPU boundaries, including while paused. */
    public static void service(Cpu cpu) {
        KernelThreadMonitor monitor = active;
        if (!KernelThreadDialog.monitorOpen && !CoreOptions.pauseOnAudioHandshake && !CoreOptions.pauseOnKernelFault) {
            if (monitor != null && monitor.cpu == cpu) active = null;
            return;
        }
        if (monitor == null || monitor.cpu != cpu) {
            synchronized (KernelThreadMonitor.class) {
                monitor = active;
                if (monitor == null || monitor.cpu != cpu)
                    active = monitor = new KernelThreadMonitor(cpu, Main.mem, false);
            }
        }
        monitor.setPauseOnFreeze(CoreOptions.pauseOnAudioHandshake);
        monitor.setPauseOnFault(CoreOptions.pauseOnKernelFault);
        if (!cpu.isPaused()) monitor.sample();
    }
    public static synchronized void stopped(Cpu cpu) {
        if (active != null && active.cpu == cpu) active = null;
    }
    final Cpu cpu;
    private final Memory memory;
    private final boolean sm64;
    final boolean generic;
    private KernelThreadReader.Scheduler scheduler;
    private final Map<Integer, Long> samples = new HashMap<>();
    private int countdown = 1024, previousFrame;
    private long total, started, lastDiscovery, previousAudio;
    private final KernelAudioHandshake handshake = new KernelAudioHandshake();
    private int gameThread;
    private String audioStatus = "";
    private boolean pauseOnFreeze, pauseTriggered;
    private boolean pauseOnFault;
    private final Set<KernelThreadHealth.Fault> handledFaults = new HashSet<>();
    private String capturedFreeze;
    private long nextBackgroundSnapshot;
    void setPauseOnFreeze(boolean enabled) {
        if (enabled != pauseOnFreeze) pauseTriggered = false;
        pauseOnFreeze = enabled;
    }
    void setPauseOnFault(boolean enabled) {
        if (enabled != pauseOnFault) handledFaults.clear();
        pauseOnFault = enabled;
    }
    record View(String text, Map<Integer, Integer> rows, String inspection, String diagnosis) {}
    View view(boolean rescan, int selected) {
        String text = snapshot(rescan);
        Map<Integer, Integer> rows = new HashMap<>();
        if (scheduler != null) {
            for (int i = 0; i < scheduler.threads().size(); i++) rows.put(4 + i, scheduler.threads().get(i).address());
        }
        return new View(text, Map.copyOf(rows), scheduler == null ? "No validated scheduler available."
            : KernelThreadInspection.read(memory.RDRAM, scheduler, selected, cpu), scheduler == null ? "Waiting for a validated scheduler." : audioStatus);
    }
    KernelThreadMonitor(Cpu cpu, Memory memory, boolean generic) {
        this.cpu = cpu; this.memory = memory; this.generic = generic;
        sm64 = Sm64Compatibility.detect(memory.rom).family();
        reset(System.nanoTime());
    }
    void sample() {
        if (--countdown != 0) return;
        countdown = 1024;
        if (scheduler != null) {
            int p = KernelThreadReader.physical(memory.RDRAM, scheduler.address(), 24);
            if (p >= 0) {
                int running = memory.RDRAM.getInt(p + 16);
                samples.merge(running, 1L, Long::sum); total++;
                if (running == gameThread) handshake.sample(cpu.pc, (int) cpu.GPR[4]);
            }
        }
        if (!KernelThreadDialog.monitorOpen && (pauseOnFreeze || pauseOnFault)) {
            long now = System.nanoTime();
            if (now >= nextBackgroundSnapshot) {
                nextBackgroundSnapshot = now + 250_000_000L;
                snapshot(false);
            }
        }
    }
    String snapshot(boolean rescan) {
        long now = System.nanoTime();
        if (!sm64 && !generic) return "No SM64 profile detected.\n\nEnable Generic libultra to try structural discovery for another game.\nNo guest data is modified.";
        if (rescan) { scheduler = null; lastDiscovery = 0; pauseTriggered = false; handledFaults.clear(); capturedFreeze = null; reset(now); }
        if (scheduler != null) scheduler = KernelThreadReader.read(memory.RDRAM, scheduler.address());
        if (scheduler == null && now - lastDiscovery >= 2_000_000_000L) {
            lastDiscovery = now; scheduler = KernelThreadReader.discover(memory.RDRAM, sm64); reset(now);
            if (scheduler != null) handshake.reset(memory.RDRAM, sm64);
        }
        if (scheduler == null) return "Waiting for a validated " + (sm64 ? "SM64" : "libultra") + " scheduler...\n\nThe game may still be booting, use a different kernel layout, or have invalid/ambiguous thread links.\nThread status and load are unknown until discovery succeeds.";
        int frame = Main.video.currentFrame;
        gameThread = scheduler.threads().stream().filter(t -> t.id() == 5).mapToInt(t -> t.address()).findFirst().orElse(0);
        if (frame < previousFrame) { handledFaults.clear(); pauseTriggered = false; reset(now); }
        double seconds = (now - started) / 1_000_000_000.0;
        StringBuilder out = new StringBuilder("KERNELN64 KERNEL THREAD DIAGNOSTIC MONITOR\n");
        out.append("=".repeat(112)).append('\n');
        long audioDelta = Main.rsp.millisInAlist - previousAudio;
        String audio = seconds > 0 && audioDelta >= 0 ? String.format(Locale.ROOT, "%.2f%%", audioDelta / (seconds * 10)) : "—";
        out.append(String.format(Locale.ROOT, "%s | Scheduler: %08X | VI/s: %.1f | Host AList: %s | %s\n", sm64 ? "SM64 profile" : "Generic libultra (experimental)", scheduler.address(), seconds > 0 ? (frame - previousFrame) / seconds : 0, audio, cpu.isPaused() ? "EMULATION PAUSED" : "LIVE"));
        out.append("ID    ROLE              PRIO  STATE       SAMPLED CPU%  PC        THREAD    FLAGS  MESSAGE QUEUE\n");
        Set<Integer> known = new HashSet<>();
        for (var t : scheduler.threads()) {
            known.add(t.address());
            String share = total == 0 ? "—" : String.format(Locale.ROOT, "%.2f", samples.getOrDefault(t.address(), 0L) * 100.0 / total);
            int pc = scheduler.running() == t.address() ? cpu.pc : t.pc();
            out.append(String.format(Locale.ROOT, "%s%-4d %-17s %4d  %-10s %11s  %08X  %08X  %04X   %s\n", t.address() == scheduler.running() ? ">" : " ", t.id(), KernelThreadReader.name(t.id(), sm64), t.priority(), KernelThreadReader.state(t.state()), share, pc, t.address(), t.flags(), t.queue()));
        }
        out.append("=".repeat(112)).append('\n');
        out.append(scheduler.faulted() == 0 ? "Faulted-thread pointer: none" : String.format("Faulted-thread pointer: %08X", scheduler.faulted()));
        long unknown = samples.entrySet().stream().filter(e -> !known.contains(e.getKey())).mapToLong(Map.Entry::getValue).sum();
        out.append(" | Samples: ").append(total).append(" | Unattributed: ").append(unknown).append('\n');
        out.append("Observed guest priorities/states; > marks the scheduler's current thread.\n");
        out.append("Load is sampled every 1024 outer CPU instructions during this refresh interval, not host CPU usage.\n");
        out.append("PC is live for the current thread; other PCs are saved contexts. Queue values are occupancy/capacity.\n");
        out.append("A waiting thread or empty queue alone is not an error. Stall warnings below are evidence-based suspicions.\n");
        long soundSamples = scheduler.threads().stream().filter(t -> t.id() == 4)
            .mapToLong(t -> samples.getOrDefault(t.address(), 0L)).sum();
        var health = KernelThreadHealth.inspect(memory.RDRAM, scheduler, sm64);
        handledFaults.retainAll(health.faults());
        audioStatus = health.text() + "\n" + (sm64 ? handshake.describe(memory.RDRAM, scheduler, frame, cpu.isPaused(),
            soundSamples, total, cpu.pc, (int) cpu.GPR[4]) : "Audio handshake diagnosis requires an SM64 profile.\n");
        var freshFault = health.faults().stream().filter(f -> !handledFaults.contains(f)).findFirst().orElse(null);
        boolean acknowledgedAudioFault = pauseOnFault && health.faults().stream()
            .anyMatch(f -> f.id() == 4 && handledFaults.contains(f));
        String pauseReason = pauseOnFault && freshFault != null ? freshFault.reason()
            : sm64 && pauseOnFreeze && !pauseTriggered && !acknowledgedAudioFault ? handshake.pauseReason() : null;
        if (!cpu.isPaused() && pauseReason != null) {
            if (pauseOnFault && freshFault != null) handledFaults.addAll(health.faults());
            else pauseTriggered = true;
            capturedFreeze = "AUTO-PAUSED: " + pauseReason + ". Captured detection evidence:\n" + audioStatus;
            cpu.debugger.pauseForMonitor(pauseReason + " - inspect saved thread context");
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (Main.cpu == cpu && active == this && cpu.running && cpu.isPaused())
                    KernelThreadDialog.showMonitor();
            });
            int live = out.indexOf("| LIVE\n");
            if (live >= 0) out.replace(live, live + 7, "| EMULATION PAUSED\n");
        }
        if (capturedFreeze != null && cpu.isPaused()) audioStatus = capturedFreeze;
        else if (!cpu.isPaused()) capturedFreeze = null;
        if (pauseOnFault) audioStatus += "Fault auto-pause armed for all validated guest threads. Resume acknowledges the captured fault; a new fault can pause again.\n";
        if (pauseOnFreeze) audioStatus += pauseTriggered
            ? "Auto-pause already triggered. Resume to continue; Rescan or toggle the option to re-arm.\n"
            : "Auto-pause armed: TH4 fault or verified-counter stall; does not wait for TH5.\n";
        out.append('\n').append(audioStatus);
        reset(now); return out.toString();
    }
    private void reset(long now) {
        samples.clear(); total = 0; started = now;
        previousFrame = Main.video.currentFrame; previousAudio = Main.rsp.millisInAlist;
    }
}
