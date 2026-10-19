package kerneln64.debugger;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.Locale;

/** Explains the saved guest exception without translating addresses or touching devices. */
final class KernelThreadFault {
    static String describe(ByteBuffer ram, KernelThreadReader.Scheduler scheduler, int threadAddress) {
        var t = scheduler.threads().stream().filter(x -> x.address() == threadAddress).findFirst().orElse(null);
        if (t == null || (scheduler.faulted() != threadAddress && (t.flags() & 2) == 0)) return "";
        int p = KernelThreadReader.physical(ram, threadAddress, 0x1B0);
        if (p < 0) return "Saved fault context is unmapped.\n";
        int epc = ram.getInt(p + 0x11C), cause = ram.getInt(p + 0x120), bad = ram.getInt(p + 0x124);
        int code = (cause >>> 2) & 31;
        boolean delay = (cause & 0x80000000) != 0;
        int faultPc = epc + (delay ? 4 : 0);
        StringBuilder out = new StringBuilder("\nSAVED FAULT — thread " + t.id() + "\n");
        out.append(String.format(Locale.ROOT, "Cause %08X | ExcCode %d: %s\nSaved EPC %08X | fault instruction PC %08X | BD=%s\n",
            cause, code, explanation(code), epc, faultPc, delay));
        if (delay) {
            out.append("The exception occurred in a branch delay slot: EPC names the branch; EPC+4 names the faulting instruction.\n");
            instruction(out, ram, epc, "Branch");
        }
        int at = KernelThreadReader.physical(ram, faultPc, 4);
        if (at < 0) out.append("Instruction unavailable: PC is unaligned or outside directly mapped RDRAM. No opcode guessed.\n");
        else {
            int word = ram.getInt(at);
            instruction(out, ram, faultPc, "Fault instruction");
            int op = word >>> 26, rs = (word >>> 21) & 31;
            int width = switch(op) {
                case 32,36,40 -> 1;
                case 33,37,41 -> 2;
                case 35,39,43,48,49,56,57 -> 4;
                case 52,53,55,60,61,63 -> 8;
                default -> 0;
            };
            if (width > 0) {
                if (rs == 26 || rs == 27) out.append("Effective address unavailable: libultra does not save k0/k1.\n");
                else {
                    long base = rs == 0 ? 0 : ram.getLong(p + (rs < 26 ? 0x20 + (rs - 1) * 8 : 0xE8 + (rs - 28) * 8));
                    int effective = (int) base + (short) word;
                    out.append(String.format(Locale.ROOT, "Saved base r%d=%016X, signed offset=%d -> address %08X (%d-byte access).\n",
                        rs, base, (short) word, effective, width));
                    if ((code == 4 || code == 5) && effective == bad && (effective & (width - 1)) != 0)
                        out.append("This address is misaligned for the access width and matches BadVAddr.\n");
                    else if (code >= 1 && code <= 5 && effective != bad)
                        out.append("Calculated address differs from BadVAddr; the saved context/current opcode does not fully explain this fault.\n");
                }
            }
            if (code == 9 && (word & 0xFC00003F) == 13)
                out.append(String.format(Locale.ROOT, "BREAK code: 0x%05X (an explicit software trap; its meaning depends on the game).\n", (word >>> 6) & 0xFFFFF));
        }
        if (code >= 1 && code <= 5) out.append(String.format(Locale.ROOT, "BadVAddr %08X: virtual address recorded by the exception.\n", bad));
        else out.append("BadVAddr is not diagnostic for this exception type and may contain an older address.\n");
        if (code == 11) out.append("Unavailable coprocessor: ").append((cause >>> 28) & 3).append('\n');
        if (code == 15) out.append(String.format(Locale.ROOT, "Saved floating-point control/status: %08X\n", ram.getInt(p + 0x12C)));
        out.append("Opcode bytes are from RAM at capture time; self-modifying code may differ from what executed.\n");
        out.append("This explains the CPU exception, not which corrupted sequence byte originally caused it.\n");
        return out.toString();
    }
    private static void instruction(StringBuilder out, ByteBuffer ram, int pc, String label) {
        int p = KernelThreadReader.physical(ram, pc, 4);
        if (p < 0) { out.append(label).append(": unavailable\n"); return; }
        int word = ram.getInt(p);
        out.append(String.format(Locale.ROOT, "%s: %08X  %08X  %s\n", label, pc, word, MipsDisassembler.format(word, pc)));
    }
    private static String explanation(int code) {
        return switch(code) {
            case 0 -> "Interrupt (not itself a synchronous CPU fault; fault context may be stale)";
            case 1 -> "TLB modification: a write targeted a page without write permission";
            case 2 -> "TLB load/fetch exception: the virtual address could not be translated";
            case 3 -> "TLB store exception: the write address could not be translated";
            case 4 -> "Address error on load/instruction fetch: invalid or misaligned address";
            case 5 -> "Address error on store: invalid or misaligned write address";
            case 6 -> "Instruction bus error while fetching code";
            case 7 -> "Data bus error while accessing memory";
            case 8 -> "SYSCALL software exception";
            case 9 -> "BREAK software exception";
            case 10 -> "Reserved/invalid instruction";
            case 11 -> "Coprocessor unusable: the requested coprocessor was unavailable";
            case 12 -> "Signed arithmetic overflow";
            case 13 -> "TRAP instruction condition was met";
            case 15 -> "Floating-point exception";
            case 23 -> "Watch exception";
            default -> "Unknown/reserved exception code";
        };
    }
}
