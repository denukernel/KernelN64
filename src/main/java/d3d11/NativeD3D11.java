package d3d11;

import java.awt.Canvas;
import java.nio.ByteBuffer;
import java.nio.file.*;

public final class NativeD3D11 {
    private static boolean loaded;
    public static synchronized void load() {
        if (loaded) return;
        Path local = bundledLibrary();
        if (local != null) System.load(local.toString()); else System.loadLibrary("KernelN64D3D11");
        loaded = true;
    }

    private static Path bundledLibrary() {
        try {
            Path location = Path.of(NativeD3D11.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(location)) location = location.getParent();
            for (int depth = 0; location != null && depth < 4; depth++, location = location.getParent()) {
                Path library = libraryUnder(location);
                if (library != null) return library;
            }
        } catch (Exception ignored) {
            // Custom classloaders can omit a code source; retain working-directory and JVM lookup.
        }
        return libraryUnder(Path.of("").toAbsolutePath());
    }

    private static Path libraryUnder(Path root) {
        for (String directory : new String[] { "KernelN64winportable", "lib" }) {
            Path library = root.resolve(directory).resolve(System.mapLibraryName("KernelN64D3D11")).toAbsolutePath().normalize();
            if (Files.isRegularFile(library)) return library;
        }
        return null;
    }
    public static native long create(Canvas canvas, int width, int height, boolean warp);
    public static native int samples(long device);
    public static native void selectDepth(long device, int address);
    public static native void resetTargets(long device);
    public static native void selectTarget(long device, int address);
    public static native void forgetTarget(long device, int address);
    public static native void resize(long device, int width, int height);
    public static native void close(long device);
    public static native String description(long device);
    public static native void clearDepthRect(long device, int x, int y, int width, int height, float depth);
    public static native void clear(long device, int mask, float r, float g, float b, float a);
    public static native void texture(long device, int id, int width, int height, ByteBuffer rgba);
    public static native void deleteTexture(long device, int id);
    public static native void draw(long device, float[] vertices, int primitive, int[] state, float[] constants);
    public static native void present(long device, boolean vsync);
    public static native byte[] read(long device);
    /** Width, height, then RGB pixels of the most recently presented frame. */
    public static native int[] readPresented(long device);
    public static native void image(long device, int width, int height, ByteBuffer rgba);
}
