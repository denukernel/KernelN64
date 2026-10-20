package kerneln64.debugger;

import java.nio.ByteBuffer;
import java.util.*;

/** Read-only diagnosis of every validated guest thread, independent of audio. */
final class KernelThreadHealth {
    record Fault(int address, int id, int pc, int cause, int badAddress) {
        String reason() { return "Kernel thread " + id + " fault detected"; }
    }
    record Report(String text, List<Fault> faults) {}

    static Report inspect(ByteBuffer ram, KernelThreadReader.Scheduler scheduler, boolean sm64) {
        StringBuilder text = new StringBuilder("GUEST THREAD HEALTH\n");
        List<Fault> faults = new ArrayList<>();
        for (var thread : scheduler.threads()) {
            boolean faulted = scheduler.faulted() == thread.address() || (thread.flags() & 2) != 0;
            if (faulted) {
                int p = KernelThreadReader.physical(ram, thread.address(), 0x1B0);
                if (p < 0) continue;
                faults.add(new Fault(thread.address(), thread.id(), thread.pc(), ram.getInt(p + 0x120), ram.getInt(p + 0x124)));
                text.append("THREAD FAULT: thread ").append(thread.id()).append(" (")
                    .append(KernelThreadReader.name(thread.id(), sm64)).append(").\n");
                text.append(KernelThreadFault.describe(ram, scheduler, thread.address()));
            } else if (thread.state() == 1) {
                text.append("THREAD STOPPED: thread ").append(thread.id()).append(" (")
                    .append(KernelThreadReader.name(thread.id(), sm64)).append(") at ")
                    .append(String.format(Locale.ROOT, "%08X", thread.pc()))
                    .append("; no kernel fault flag/pointer. Stop alone does not establish a crash.\n");
            }
        }
        if (faults.isEmpty()) text.append("No confirmed kernel thread faults. Waiting and stopped states alone do not trigger fault auto-pause.\n");
        faults.sort(Comparator.comparingInt(f -> f.address() == scheduler.faulted() ? 0 : 1));
        return new Report(text.toString(), List.copyOf(faults));
    }
}
