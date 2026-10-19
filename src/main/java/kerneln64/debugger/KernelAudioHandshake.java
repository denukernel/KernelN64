package kerneln64.debugger;
import kerneln64.hardware.Audio;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.Locale;

/** Read-only SM64 audio handshake evidence. Addresses are derived from validated code. */
final class KernelAudioHandshake {
    // US compiler output for wait_for_audio_frames and the counter producer.
    // Only relocated address operands are masked; changed logic is unsupported.
    private static final int[] WAIT = {0x3C020000, 0x24420000, 0xAC400000, 0x8C4E0000,
        0x01C4082A, 0x10200005, 0, 0x8C4F0000, 0x01E4082A, 0x5420FFFE, 0x8C4F0000, 0x03E00008, 0};
    private static final int[] PRODUCER = {0x3C040000, 0x24840000, 0x8C820000, 0x27BDFFB8,
        0x3C0F0000, 0x244E0001, 0xAFBF0014, 0xAC8E0000};
    record Profile(int waitPc, int producerPc, int counter) {
        boolean inWait(int pc) { return Integer.compareUnsigned(pc - waitPc, 44) < 0; }
        boolean valid(ByteBuffer ram) {
            return matches(ram, waitPc, WAIT, false) && matches(ram, producerPc, PRODUCER, true)
                && address(ram, waitPc) == counter && address(ram, producerPc) == counter;
        }
    }
    private Profile profile;
    private boolean initialized;
    private int previousFrame, previousCounter, stagnantVis, unsampledVis;
    private long previousNanos, stagnantNanos, unsampledNanos;
    private long waitSamples;
    private int targetFrames;
    private boolean freezeSuspected;
    boolean freezeSuspected() { return freezeSuspected; }
    private String pauseReason;
    String pauseReason() { return pauseReason; }

    void reset(ByteBuffer ram, boolean sm64) {
        profile = sm64 ? discover(ram) : null;
        initialized = false; stagnantVis = unsampledVis = 0; waitSamples = 0;
        stagnantNanos = unsampledNanos = 0;
    }
    void sample(int pc, int target) {
        if (profile != null && profile.inWait(pc)) { waitSamples++; targetFrames = target; }
    }
    static Profile discover(ByteBuffer ram) {
        Profile found = null;
        for (int p = 0; p <= ram.capacity() - WAIT.length * 4; p += 4) {
            if (ram.getInt(p) >>> 16 != 0x3C02) continue;
            int pc = p | 0x80000000;
            if (!matches(ram, pc, WAIT, false)) continue;
            int counter = address(ram, pc);
            if (KernelThreadReader.physical(ram, counter, 4) < 0) continue;
            int producer = 0;
            for (int q = 0; q <= ram.capacity() - PRODUCER.length * 4; q += 4) {
                if (ram.getInt(q) >>> 16 != 0x3C04) continue;
                int at = q | 0x80000000;
                if (matches(ram, at, PRODUCER, true) && address(ram, at) == counter) {
                    if (producer != 0) return null;
                    producer = at;
                }
            }
            if (producer == 0) continue;
            if (found != null) return null;
            found = new Profile(pc, producer, counter);
        }
        return found;
    }
    private static int address(ByteBuffer ram, int pc) {
        int p = KernelThreadReader.physical(ram, pc, 8);
        return (ram.getInt(p) << 16) + (short) ram.getInt(p + 4);
    }
    private static boolean matches(ByteBuffer ram, int pc, int[] words, boolean producer) {
        int p = KernelThreadReader.physical(ram, pc, words.length * 4);
        if (p < 0) return false;
        for (int i = 0; i < words.length; i++) {
            int mask = i < 2 || (producer && i == 4) ? 0xFFFF0000 : -1;
            if ((ram.getInt(p + i * 4) & mask) != words[i]) return false;
        }
        return true;
    }
    String describe(ByteBuffer ram, KernelThreadReader.Scheduler scheduler, int frame, boolean paused,
            long soundSamples, long totalSamples, int livePc, int liveA0) {
        return describeAt(ram, scheduler, frame, paused, soundSamples, totalSamples, livePc, liveA0, System.nanoTime());
    }
    String describeAt(ByteBuffer ram, KernelThreadReader.Scheduler scheduler, int frame, boolean paused,
            long soundSamples, long totalSamples, int livePc, int liveA0, long now) {
        freezeSuspected = false;
        pauseReason = null;
        var sound = scheduler.threads().stream().filter(t -> t.id() == 4).findFirst().orElse(null);
        var game = scheduler.threads().stream().filter(t -> t.id() == 5).findFirst().orElse(null);
        if (sound == null || game == null) { waitSamples = 0; initialized = false; return "Audio handshake: threads 4/5 unavailable.\n"; }
        if (profile != null && !profile.valid(ram)) { profile = null; initialized = false; }
        int counter = profile == null ? 0 : ram.getInt(KernelThreadReader.physical(ram, profile.counter(), 4));
        int delta = frame - previousFrame;
        boolean baseline = !initialized || paused || delta < 0 || delta > 600;
        if (baseline) { stagnantVis = unsampledVis = 0; stagnantNanos = unsampledNanos = 0; waitSamples = 0; }
        else if (totalSamples > 0) {
            stagnantVis = counter == previousCounter ? Math.min(1000000, stagnantVis + delta) : 0;
            unsampledVis = soundSamples == 0 ? Math.min(1000000, unsampledVis + delta) : 0;
            long elapsed = Math.max(0, Math.min(10_000_000_000L, now - previousNanos));
            stagnantNanos = counter == previousCounter ? stagnantNanos + elapsed : 0;
            unsampledNanos = soundSamples == 0 ? unsampledNanos + elapsed : 0;
        } else {
            stagnantVis = unsampledVis = 0; stagnantNanos = unsampledNanos = 0;
        }
        initialized = !paused; previousFrame = frame; previousCounter = counter; previousNanos = now;
        boolean stalled = stagnantVis >= 120 || stagnantNanos >= 2_000_000_000L;
        boolean fault = scheduler.faulted() == sound.address() || (sound.flags() & 2) != 0;
        if (!paused) {
            if (fault) pauseReason = "Thread 4 audio fault detected";
            else if (profile != null && stalled) pauseReason = "Thread 4 audio stall suspected";
        }
        StringBuilder out = new StringBuilder("AUDIO HANDSHAKE\n");
        if (fault) out.append("TH4 FAULT: libultra fault pointer/flag identifies the sound thread.\n");
        else if (paused) out.append("TH4: emulation paused; stall detection suspended.\n");
        else if (profile != null && stalled)
            out.append(String.format(Locale.ROOT, "TH4 SUSPECTED STALL: counter unchanged for %d VI / %.1f s while guest code executes.\n", stagnantVis, stagnantNanos / 1e9));
        else if (profile == null && (unsampledVis >= 120 || unsampledNanos >= 2_000_000_000L))
            out.append(String.format(Locale.ROOT, "TH4 SUSPECTED INACTIVITY: no samples for %d VI / %.1f s; handshake profile unavailable.\n", unsampledVis, unsampledNanos / 1e9));
        else out.append("TH4: no sustained stall detected in this observation window.\n");
        out.append("TH4 state: ").append(KernelThreadReader.state(sound.state())).append(" | execution samples this interval: ").append(soundSamples).append("\n");
        if (profile == null) out.append("Audio counter/handshake code unverified; use Rescan after boot. No fixed hack addresses assumed.\n");
        else out.append(String.format(Locale.ROOT, "Audio frame counter @%08X = %d | unchanged: %d VI / %.1f s\nwait_for_audio_frames @%08X\n",
            profile.counter(), counter, stagnantVis, stagnantNanos / 1e9, profile.waitPc()));
        int pc = scheduler.running() == game.address() ? livePc : game.pc();
        int target = scheduler.running() == game.address() ? liveA0 : ram.getInt(KernelThreadReader.physical(ram, game.address(), 0x1B0) + 0x3C);
        if (game.state() == 8) out.append("TH5 waiting on message queue: ").append(game.queue()).append(" (producer identity unknown).\n");
        else if (profile != null && (profile.inWait(pc) || waitSamples > 0)) {
            if (!profile.inWait(pc)) target = targetFrames;
            out.append(String.format(Locale.ROOT, "TH5 %s wait_for_audio_frames: needs audio counter >= %d; observed %d.\n",
                profile.inWait(pc) ? "inside" : "sampled inside", target, counter));
            out.append("Dependency: thread 4 must create audio frames; this is a busy-wait, not a message-queue receive.\n");
            if ((fault || stalled) && counter < target && !paused) {
                freezeSuspected = true;
                out.append("AUDIO HANDSHAKE FREEZE SUSPECTED: TH5 needs counter progress and TH4 is faulted or not advancing it.\n");
            }
        } else out.append(String.format(Locale.ROOT, "TH5: no verified audio wait observed this interval; PC %08X (%s).\n", pc, KernelThreadReader.state(game.state())));
        waitSamples = 0;
        out.append("Detection threshold: 120 VI or 2 seconds with guest execution; pause/rewind resets the observation.\n");
        return out.toString();
    }
}
