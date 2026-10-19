package kerneln64.ui;
import kerneln64.Main;


import java.io.File;

public record LaunchArguments(File rom, File state, boolean help) {
    public static final String HELP = "KernelN64.exe [ROM.z64]\nKernelN64.exe --rom ROM.z64 [--state FILE.j64state]\nQuote paths that contain spaces. ROM formats: z64, n64, v64.\nSettings and saves are stored beside the launcher.";
    public static LaunchArguments parse(String[] args) {
        File rom = null, state = null; boolean help = false, positional = false;
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (!positional && argument.equals("--")) { positional = true; continue; }
            if (!positional && (argument.equals("--help") || argument.equals("-h"))) { help = true; continue; }
            if (!positional && (argument.equals("--rom") || argument.equals("--state"))) {
                if (++i >= args.length) throw new IllegalArgumentException("Missing path after " + argument);
                if (argument.equals("--rom")) {
                    if (rom != null) throw new IllegalArgumentException("Specify one ROM path."); rom = new File(args[i]).getAbsoluteFile();
                } else {
                    if (state != null) throw new IllegalArgumentException("Specify one save-state path."); state = new File(args[i]).getAbsoluteFile();
                }
            } else {
                if (!positional && argument.startsWith("-")) throw new IllegalArgumentException("Unknown option: " + argument);
                if (rom != null) throw new IllegalArgumentException("Specify one ROM path; quote paths containing spaces.");
                rom = new File(argument).getAbsoluteFile();
            }
        }
        if (!help) {
            if (state != null && rom == null) throw new IllegalArgumentException("--state requires a ROM path.");
            if (rom != null && !rom.isFile()) throw new IllegalArgumentException("ROM file not found: " + rom);
            if (state != null && !state.isFile()) throw new IllegalArgumentException("Save state not found: " + state);
        }
        return new LaunchArguments(rom, state, help);
    }
}
