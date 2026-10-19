package kerneln64.ui;
import gln64j.GLN64jPlugin;
import kerneln64.rom.GameSharkCartridge;
import kerneln64.rom.GameSharkFramebuffer;
import kerneln64.Main;


import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.opengl.*;

public final class Screenshots {
    private static final AtomicBoolean requested = new AtomicBoolean();
    private static final java.util.concurrent.ExecutorService writer = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Screenshot writer"); t.setDaemon(true); return t;
    });
    public static void request() {
        if (Main.cpu == null || !Main.cpu.running) { HostMessages.error("Screenshot", new IllegalStateException("Open a ROM first.")); return; }
        requested.set(true);
    }
    public static BufferedImage readDrawable(GLAutoDrawable drawable, int readBuffer) {
        GLContext context = drawable.getContext();
        boolean acquired = GLContext.getCurrent() != context;
        if (acquired && context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) throw new IllegalStateException("Cannot access the rendered frame.");
        try {
            GL gl = drawable.getGL(); int[] old = new int[1]; gl.glGetIntegerv(GL.GL_READ_BUFFER, old, 0);
            try {
                int w = drawable.getWidth(), h = drawable.getHeight();
                if (w <= 0 || h <= 0 || (long) w * h > 16_777_216) throw new IllegalStateException("Invalid screenshot dimensions.");
                ByteBuffer pixels = ByteBuffer.allocateDirect(w * h * 4);
                gl.glReadBuffer(readBuffer); gl.glReadPixels(0, 0, w, h, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixels);
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int i = (y * w + x) * 4;
                    image.setRGB(x, h - 1 - y, ((pixels.get(i) & 255) << 16) | ((pixels.get(i + 1) & 255) << 8) | (pixels.get(i + 2) & 255));
                }
                return image;
            } finally { gl.glReadBuffer(old[0]); }
        } finally { if (acquired) context.release(); }
    }
    public static void service() {
        if (!requested.getAndSet(false)) return;
        try {
            BufferedImage image = currentFrame();
            if (image == null) throw new IllegalStateException("No rendered frame is available yet.");
            String name = Main.currentRomFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path output = Path.of("Screenshots", name + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "-" + Long.toUnsignedString(System.nanoTime()) + ".png");
            writer.submit(() -> {
                try { Files.createDirectories(output.getParent()); javax.imageio.ImageIO.write(image, "png", output.toFile()); System.out.println("Screenshot: " + output.toAbsolutePath()); }
                catch (Exception error) { HostMessages.error("Screenshot", error); }
            });
        } catch (Exception error) { HostMessages.error("Screenshot", error); }
    }
    public static BufferedImage currentFrame() {
        if(Main.mem!=null && GameSharkCartridge.isFirmware(Main.mem.rom))return GameSharkFramebuffer.read();
        if (Main.video.gfxPlugin instanceof d3d11.D3D11Plugin d3d) return d3d.screenshot();
        if (Main.video.gfxPlugin instanceof gln64j.GLN64jPlugin && gln64j.opengl.OpenGlGdp.hDC != null)
            return readDrawable(gln64j.opengl.OpenGlGdp.hDC, GL.GL_FRONT);
        if (Main.video.gfxPlugin instanceof cfb.CFBPlugin cfb) return cfb.screenshot();
        return null;
    }
    public static void present(BufferedImage image) {
        if (image == null) return;
        if (Main.video.gfxPlugin instanceof d3d11.D3D11Plugin d3d) { d3d.preview(image); return; }
        if (Main.video.gfxPlugin instanceof gln64j.GLN64jPlugin && gln64j.opengl.OpenGlGdp.hDC != null)
            presentDrawable(gln64j.opengl.OpenGlGdp.hDC, image, GL.GL_FRONT);
        else if (Main.video.gfxPlugin instanceof cfb.CFBPlugin cfb) cfb.presentScreenshot(image);
    }
    /** Present a rewind preview without replaying any guest graphics commands. */
    public static void presentDrawable(GLAutoDrawable drawable, BufferedImage image, int buffer) {
        GLContext context = drawable.getContext();
        boolean acquired = GLContext.getCurrent() != context;
        if (acquired && context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) throw new IllegalStateException("Cannot present rewind frame.");
        try {
            GL gl = drawable.getGL(); int[] mode = new int[1]; gl.glGetIntegerv(GL.GL_MATRIX_MODE, mode, 0);
            gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);
            gl.glMatrixMode(GL.GL_PROJECTION); gl.glPushMatrix(); gl.glLoadIdentity(); gl.glOrtho(0, drawable.getWidth(), 0, drawable.getHeight(), -1, 1);
            gl.glMatrixMode(GL.GL_MODELVIEW); gl.glPushMatrix(); gl.glLoadIdentity();
            try {
                gl.glDisable(GL.GL_DEPTH_TEST); gl.glDisable(GL.GL_STENCIL_TEST); gl.glDisable(GL.GL_SCISSOR_TEST);
                gl.glDisable(GL.GL_ALPHA_TEST); gl.glDisable(GL.GL_BLEND); gl.glDisable(GL.GL_FOG);
                gl.glColorMask(true, true, true, true); gl.glDrawBuffer(buffer); gl.glViewport(0, 0, drawable.getWidth(), drawable.getHeight());
                gl.glRasterPos2i(0, 0); gl.glPixelZoom(drawable.getWidth() / (float)image.getWidth(), drawable.getHeight() / (float)image.getHeight());
                ByteBuffer pixels = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
                for (int y = image.getHeight() - 1; y >= 0; y--) for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y); pixels.put((byte)(rgb >> 16)).put((byte)(rgb >> 8)).put((byte)rgb).put((byte)255);
                }
                pixels.flip(); gl.glDrawPixels(image.getWidth(), image.getHeight(), GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixels); gl.glFlush();
            } finally {
                gl.glMatrixMode(GL.GL_MODELVIEW); gl.glPopMatrix(); gl.glMatrixMode(GL.GL_PROJECTION); gl.glPopMatrix();
                gl.glPopAttrib(); gl.glMatrixMode(mode[0]);
            }
        } finally { if (acquired) context.release(); }
    }
}
