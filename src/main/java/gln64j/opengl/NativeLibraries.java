package gln64j.opengl;

import com.sun.opengl.impl.NativeLibLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Resolves the bundled JOGL DLLs for plain Eclipse Java launches as well as the EXE. */
final class NativeLibraries {
    private static boolean installed;

    private NativeLibraries() {}

    static synchronized void install() {
        if (installed || !System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) return;
        Path directory = findDirectory();
        if (directory == null) return; // Preserve JOGL's normal loader on other installations.

        NativeLibLoader.setLoadingAction((name, dependencies, preload, ignoreDependencyErrors) -> {
            if (preload && dependencies != null) {
                for (String dependency : dependencies) {
                    try { load(directory, dependency); }
                    catch (UnsatisfiedLinkError error) {
                        if (!ignoreDependencyErrors && !String.valueOf(error.getMessage()).contains("already loaded")) throw error;
                    }
                }
            }
            load(directory, name);
        });
        installed = true;
    }

    private static void load(Path directory, String name) {
        Path library = directory.resolve(System.mapLibraryName(name));
        if (Files.isRegularFile(library)) System.load(library.toString());
        else System.loadLibrary(name); // JDK jawt and optional vendor libraries use their normal search path.
    }

    private static Path findDirectory() {
        Set<Path> roots = new LinkedHashSet<>();
        try {
            Path location = Path.of(NativeLibraries.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(location)) location = location.getParent();
            // target/classes in Eclipse; dist/KernelN64/app.jar in the portable build.
            for (int depth = 0; location != null && depth < 4; depth++, location = location.getParent()) roots.add(location);
        } catch (Exception ignored) {
            // A custom classloader may have no code source; the working directory still works.
        }
        roots.add(Path.of("").toAbsolutePath());
        for (Path root : roots) {
            for (String child : new String[] { "Jario64winportable", "lib" }) {
                Path directory = root.resolve(child).toAbsolutePath().normalize();
                if (Files.isRegularFile(directory.resolve(System.mapLibraryName("jogl")))) return directory;
            }
        }
        return null;
    }
}
