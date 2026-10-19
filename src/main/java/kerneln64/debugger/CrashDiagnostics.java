package kerneln64.debugger;
import kerneln64.hardware.Audio;
import kerneln64.cpu.Cop0;
import kerneln64.ui.HostMessages;
import kerneln64.Main;


import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class CrashDiagnostics {
    private static final int[] addresses = new int[128], instructions = new int[128];
    private static int cursor, count;
    public static void reset() { cursor = count = 0; }
    public static void instruction(int pc, int opcode) {
        addresses[cursor] = pc; instructions[cursor] = opcode;
        cursor = (cursor + 1) & 127; if (count < 128) count++;
    }
    public static Path write(Throwable error) throws IOException {
        Path folder = Path.of("CrashReports"); Files.createDirectories(folder);
        Path file = folder.resolve("crash-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + ".txt");
        StringWriter text = new StringWriter(); PrintWriter out = new PrintWriter(text);
        out.println("KernelN64 crash report — " + Instant.now());
        out.println("ROM: " + Main.currentRomFile); out.println("SHA-256: " + Main.currentRomHash);
        out.println("Java: " + System.getProperty("java.version") + " / " + System.getProperty("os.name"));
        out.println("Graphics: " + (Main.video == null ? null : Main.video.gfxPlugin));
        out.println("Audio: " + (Main.audio == null ? null : Main.audio.audioPlugin));
        out.println("Speed: " + Main.SpeedMultiplier + "  cache: " + Main.CacheInstructions + "  TLB: " + Main.UseTlb);
        if (Main.cpu != null) {
            out.printf("PC: %08X%n", Main.cpu.pc);
            for (int i = 0; i < Main.cpu.GPR.length; i++) out.printf("R%02d: %016X%s", i, Main.cpu.GPR[i], i % 4 == 3 ? "\n" : "  ");
            out.printf("HI: %016X LO: %016X%n", Main.cpu.HI, Main.cpu.LO);
            if (Main.cpu.cop0 != null) {
                out.printf("CP0 Status: %08X Cause: %08X EPC: %08X%n", Main.cpu.cop0.CP0[Cop0.STATUS_REGISTER], Main.cpu.cop0.CP0[Cop0.CAUSE_REGISTER], Main.cpu.cop0.CP0[Cop0.EPC_REGISTER]);
            }
        }
        out.println("Active cheat programs:"); out.print(Main.cheats.diagnosticCodes());
        out.println("Recent fetched instructions (PC / raw opcode, oldest first):");
        for (int i = 0; i < count; i++) {
            int index = (cursor - count + i + 128) & 127;
            out.printf("%08X  %08X%n", addresses[index], instructions[index]);
        }
        error.printStackTrace(out); out.flush(); Files.writeString(file, text.toString()); return file;
    }
    public static void stopped(Throwable error) {
        try {
            Path file = write(error);
            System.err.println("Emulation stopped; report: " + file.toAbsolutePath());
            HostMessages.error("Emulation stopped", new IOException("Emulation stopped: " + error.getMessage() + "\nReport: " + file.toAbsolutePath()));
        } catch (IOException failure) { HostMessages.error("Crash report could not be saved", failure); }
    }
}
