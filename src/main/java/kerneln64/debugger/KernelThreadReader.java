package kerneln64.debugger;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.*;

/** Read-only libultra OSThread/OSMesgQueue decoding. All calls run on the CPU thread. */
public final class KernelThreadReader {
    public record ThreadInfo(int address, int id, int priority, int state, int flags, int pc, int sp, String queue) {}
    public record Scheduler(int address, int running, int faulted, List<ThreadInfo> threads) {}
    static int physical(ByteBuffer ram, int address, int length) {
        if ((address & 0xE0000000) != 0x80000000 && (address & 0xE0000000) != 0xA0000000) return -1;
        int p = address & 0x1FFFFFFF;
        return (address & 3) == 0 && length >= 0 && p <= ram.capacity() - length ? p : -1;
    }
    public static Scheduler discover(ByteBuffer ram, boolean sm64) {
        Scheduler found = null;
        for (int p = 0; p <= ram.capacity() - 24; p += 4) {
            if (ram.getInt(p) != 0 || ram.getInt(p + 4) != -1) continue;
            Scheduler candidate = read(ram, p | 0x80000000);
            if (candidate == null) continue;
            Set<Integer> ids = new HashSet<>();
            for (ThreadInfo t : candidate.threads()) ids.add(t.id());
            if (sm64 && !ids.containsAll(Set.of(1, 3, 4, 5))) continue;
            if (found != null) return null; // Ambiguous layouts must not be presented as verified.
            found = candidate;
        }
        return found;
    }
    static Scheduler read(ByteBuffer ram, int address) {
        int p = physical(ram, address, 24);
        if (p < 0 || ram.getInt(p) != 0 || ram.getInt(p + 4) != -1) return null;
        int running = ram.getInt(p + 16), faulted = ram.getInt(p + 20), next = ram.getInt(p + 12);
        List<ThreadInfo> threads = new ArrayList<>(); Set<Integer> visited = new HashSet<>();
        while (next != address) {
            if (threads.size() >= 64 || !visited.add(next)) return null;
            int t = physical(ram, next, 0x1B0);
            if (t < 0) return null;
            int priority = ram.getInt(t + 4), state = ram.getShort(t + 0x10) & 65535;
            int id = ram.getInt(t + 0x14), fp = ram.getInt(t + 0x18);
            int pc = ram.getInt(t + 0x11C), sp = ram.getInt(t + 0xF4);
            if (priority < 0 || priority > 255 || id < 0 || id > 65535 || fp < 0 || fp > 1
                    || (state != 1 && state != 2 && state != 4 && state != 8)
                    || (next != faulted && (ram.getShort(t + 0x12) & 2) == 0
                        && (physical(ram, pc, 4) < 0 || physical(ram, sp, 0) < 0))) return null;
            threads.add(new ThreadInfo(next, id, priority, state, ram.getShort(t + 0x12) & 65535,
                    pc, sp, state == 8 ? queue(ram, ram.getInt(t + 8), next) : "—"));
            next = ram.getInt(t + 12);
        }
        if (threads.size() < 3 || !visited.contains(running) || (faulted != 0 && !visited.contains(faulted))) return null;
        threads.sort(Comparator.comparingInt(ThreadInfo::id).thenComparingInt(ThreadInfo::priority));
        return new Scheduler(address, running, faulted, List.copyOf(threads));
    }
    private static String queue(ByteBuffer ram, int pointer, int thread) {
        // OSThread.queue points to either OSMesgQueue.mtqueue or .fullqueue.
        for (int offset : new int[]{0, 4}) {
            int address = pointer - offset, q = physical(ram, address, 24);
            if (q < 0) continue;
            int count = ram.getInt(q + 8), first = ram.getInt(q + 12), capacity = ram.getInt(q + 16);
            if (capacity < 1 || capacity > 4096 || count < 0 || count > capacity || first < 0 || first >= capacity
                    || physical(ram, ram.getInt(q + 20), capacity * 4) < 0) continue;
            int link = ram.getInt(q + offset); boolean member = false;
            for (int i = 0; i < 64; i++) {
                if (link == thread) { member = true; break; }
                int l = physical(ram, link, 8);
                if (l < 0 || ram.getInt(l + 4) == -1) break;
                link = ram.getInt(l);
            }
            if (member) return String.format(Locale.ROOT, "%s %d/%d @%08X", offset == 0 ? "RECV" : "SEND", count, capacity, address);
        }
        return String.format("Queue unknown @%08X", pointer);
    }
    static String name(int id, boolean sm64) {
        if (!sm64) return "Guest thread";
        return switch (id) { case 1 -> "IDLE LOOP"; case 2 -> "FAULT HANDLER"; case 3 -> "MASTER SCHED";
            case 4 -> "SOUND DRIVER"; case 5 -> "MAIN GAME LOOP"; default -> "OS / OTHER"; };
    }
    static String state(int state) {
        return switch (state) { case 1 -> "STOPPED"; case 2 -> "RUNNABLE"; case 4 -> "RUNNING"; case 8 -> "WAITING"; default -> "UNKNOWN"; };
    }
}

