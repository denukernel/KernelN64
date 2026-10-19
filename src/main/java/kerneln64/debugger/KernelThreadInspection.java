package kerneln64.debugger;
import kerneln64.cpu.Cpu;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.Locale;

/** Copies a guest context at a CPU command boundary; never executes guest reads. */
final class KernelThreadInspection {
    private static final String[] NAMES = {"zero", "at", "v0", "v1", "a0", "a1", "a2", "a3",
        "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "s0", "s1", "s2", "s3",
        "s4", "s5", "s6", "s7", "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"};
    static String read(ByteBuffer ram, KernelThreadReader.Scheduler scheduler, int address, Cpu cpu) {
        var thread = scheduler.threads().stream().filter(t -> t.address() == address).findFirst().orElse(null);
        if (thread == null) return "Select a thread row to inspect its registers and stack.";
        int p = KernelThreadReader.physical(ram, address, 0x1B0);
        if (p < 0) return "Thread context is no longer mapped.";
        boolean fault = scheduler.faulted() == address || (thread.flags() & 2) != 0;
        boolean live = scheduler.running() == address && !fault;
        StringBuilder out = new StringBuilder(String.format(Locale.ROOT, "Thread %d @%08X | %s\n",
                thread.id(), address, live ? "LIVE CPU (may be inside kernel/interrupt code)" : "SAVED OSThread context"));
        out.append(fault ? "FAULT EVIDENCE: libultra fault pointer/flag identifies this thread.\n" : "No libultra fault evidence for this thread; this does not rule out a hang.\n");
        if (fault) out.append(KernelThreadFault.describe(ram, scheduler, address)).append('\n');
        for (int r = 0; r < 32; r++) {
            String value;
            if (live) value = String.format(Locale.ROOT, "%016X", cpu.GPR[r]);
            else if (r == 0) value = "0000000000000000";
            else if (r == 26 || r == 27) value = "not saved by OS";
            else value = String.format(Locale.ROOT, "%016X", ram.getLong(p + (r < 26 ? 0x20 + (r - 1) * 8 : 0xE8 + (r - 28) * 8)));
            out.append(String.format(Locale.ROOT, "r%-2d %-4s %-18s%s", r, NAMES[r], value, r % 2 == 1 ? "\n" : "    "));
        }
        out.append(String.format(Locale.ROOT, "HI %016X    LO %016X\n", live ? cpu.HI : ram.getLong(p + 0x110), live ? cpu.LO : ram.getLong(p + 0x108)));
        int pc = live ? cpu.pc : ram.getInt(p + 0x11C);
        int status = live ? cpu.cop0.CP0[12] : ram.getInt(p + 0x118);
        int cause = live ? cpu.cop0.CP0[13] : ram.getInt(p + 0x120);
        int bad = live ? cpu.cop0.CP0[8] : ram.getInt(p + 0x124);
        out.append(String.format(Locale.ROOT, "PC %08X  Status %08X  Cause %08X  BadVAddr %08X\n", pc, status, cause, bad));
        out.append(String.format(Locale.ROOT, "Cause ExcCode=%d  BD=%s (Cause alone is not proof of a new fault)\n", (cause >>> 2) & 31, (cause & 0x80000000) != 0));
        out.append("\nSTACK MEMORY — up to 256 bytes from SP; allocation bounds unknown, not an unwound call stack\n");
        int sp = live ? (int) cpu.GPR[29] : thread.sp();
        int base = KernelThreadReader.physical(ram, sp, 4);
        if (base < 0) return out.append(String.format("SP %08X is unaligned or outside directly mapped RDRAM.\n", sp)).toString();
        int bytes = Math.min(256, ram.capacity() - base);
        for (int offset = 0; offset + 4 <= bytes; offset += 4) {
            if (offset % 16 == 0) out.append(String.format(Locale.ROOT, "%08X:", sp + offset));
            out.append(String.format(Locale.ROOT, " %08X", ram.getInt(base + offset)));
            if (offset % 16 == 12 || offset + 8 > bytes) out.append('\n');
        }
        return out.toString();
    }
}
