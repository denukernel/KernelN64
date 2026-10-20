package kerneln64.ui;

import kerneln64.Main;
import kerneln64.cpu.Cpu;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** A repaintable still image while native renderer surfaces are not producing frames. */
public final class PausedFrame {
    private static volatile Cpu owner;
    private static volatile BufferedImage frame;
    // Component ownership is confined to the EDT. Native canvases stay attached,
    // so pausing does not destroy their graphics contexts.
    private static Container content;
    private static Component renderer;
    private static FrameView preview;
    private static final class FrameView extends JComponent {
        BufferedImage image;
        FrameView(BufferedImage image) { this.image = image; }
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        }
    }
    private PausedFrame() {}

    /** Called once at a CPU pause boundary, with no concurrent guest rendering. */
    public static void show(Cpu cpu) {
        if (Main.cpu != cpu || Main.hMainWindow == null) return;
        BufferedImage image;
        try { image = Screenshots.currentFrame(); }
        catch (RuntimeException error) {
            System.err.println("Cannot preserve paused frame: " + error.getMessage());
            return;
        }
        if (image == null) return;
        owner = cpu; frame = image;
        SwingUtilities.invokeLater(() -> {
            if (owner != cpu || Main.cpu != cpu || !cpu.running || !cpu.isPaused()) return;
            removePreview();
            Container parent = Main.hMainWindow.getContentPane();
            if (!(parent.getLayout() instanceof BorderLayout layout)) return;
            Component surface = layout.getLayoutComponent(BorderLayout.CENTER);
            if (surface == null) return;
            content = parent; renderer = surface;
            preview = new FrameView(frame);
            preview.setPreferredSize(surface.getPreferredSize());
            renderer.setVisible(false);
            content.add(preview, BorderLayout.CENTER);
            content.validate(); content.repaint();
        });
    }

    public static BufferedImage current() {
        Cpu cpu = owner;
        return cpu != null && Main.cpu == cpu && cpu.running && cpu.isPaused() ? frame : null;
    }

    /** Savestate and rewind previews can change while guest execution is paused. */
    public static boolean present(BufferedImage image) {
        Cpu cpu = owner;
        if (current() == null) return false;
        frame = image;
        SwingUtilities.invokeLater(() -> {
            if (owner == cpu && preview != null) { preview.image = image; preview.repaint(); }
        });
        return true;
    }

    public static void hide(Cpu cpu) {
        if (owner != cpu) return;
        owner = null; frame = null;
        SwingUtilities.invokeLater(PausedFrame::removePreview);
    }

    private static void removePreview() {
        if (content != null && preview != null) {
            // A ROM change may already have replaced the content. Never reinsert
            // a detached renderer from an earlier session.
            if (preview.getParent() == content) content.remove(preview);
            if (renderer.getParent() == content) {
                content.add(renderer, BorderLayout.CENTER);
                renderer.setVisible(true);
                if (Main.hMainWindow != null && Main.hMainWindow.isActive()) renderer.requestFocusInWindow();
            }
            content.validate(); content.repaint();
        }
        content = null; renderer = null; preview = null;
    }
}
