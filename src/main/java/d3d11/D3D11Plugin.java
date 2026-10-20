package d3d11;
import gln64j.GLN64jPlugin;
import d3d11.bridge.D3DCombiner;
import d3d11.bridge.D3DGlBridge;
import d3d11.config.D3D11Settings;
import d3d11.framebuffer.D3DFramebuffers;
import gln64j.core.Rsp;
import gln64j.opengl.OpenGl;
import gln64j.opengl.OpenGlGdp;

import gln64j.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import javax.swing.*;

/** Native D3D11 rasterization with the shared glN64j N64 HLE decoder. */
public final class D3D11Plugin extends GLN64jPlugin {
    private long device;
    private D3DGlBridge renderer;
    private Canvas canvas;
    private int width,height;
    private long presentedContent = -1;
    private int presentedTarget = -1;
    @Override public void pluginConfig(JFrame parent) { D3D11Settings.show(parent); }
    @Override public void pluginAbout(JFrame parent) { JOptionPane.showMessageDialog(parent,"KernelN64 Direct3D 11\nNative D3D11 rasterizer and RDP shaders; shared N64 HLE decoder.\nFog, multisample coverage and RDRAM framebuffer feedback.\nExperimental HLE renderer; N64 sample patterns and depth are approximated."); }
    @Override public void romOpen() {
        NativeD3D11.load();
        width=D3D11Settings.width;height=D3D11Settings.height;
        runEdt(() -> {
            canvas=new Canvas();canvas.setBackground(Color.BLACK);canvas.setPreferredSize(new Dimension(width,height));canvas.setIgnoreRepaint(true);
            kerneln64.ui.KeyboardRouting.attach(canvas);
            hWnd.getContentPane().removeAll();hWnd.add(canvas,BorderLayout.CENTER);
            if(kerneln64.Main.emulationStatus!=null)hWnd.add(kerneln64.Main.emulationStatus,BorderLayout.SOUTH);
            hWnd.setResizable(!D3D11Settings.locked);hWnd.pack();canvas.requestFocusInWindow();
        });
        device=NativeD3D11.create(canvas,width,height,D3D11Settings.warp);
        initializeDecoder();System.out.println("Graphics: "+NativeD3D11.description(device));
    }
    /** Native off-screen target, used by saved-frame and shader checks. */
    public void openOffscreen(int width,int height,boolean warp) {
        NativeD3D11.load();this.width=width;this.height=height;device=NativeD3D11.create(null,width,height,warp);initializeDecoder();
    }
    private void initializeDecoder() {
        presentedContent=-1;presentedTarget=-1;
        OpenGl.uc_start=OpenGl.uc_dstart=0;RDRAMSize=RDRAM.capacity();
        renderer=new D3DGlBridge(device,width,height);OpenGl.gl=renderer.gl;OpenGlGdp.hDC=null;
        OpenGlGdp.cache=new gln64j.rdp.textures.TextureCache();OpenGlGdp.combiners=new D3DCombiner(renderer);
        gln64j.core.Rsp.gsp=new gln64j.rsp.Gsp(RDRAM,DMEM);gln64j.core.Rsp.gdp=new gln64j.rdp.Gdp(CheckInterrupts,REG);
        OpenGlGdp.config();OpenGlGdp.cache.config(64*1024*1024,1);OpenGlGdp.setDrawableSize(width,height);
        OpenGlGdp.screenWidth=320;OpenGlGdp.screenHeight=240;OpenGlGdp.init(renderer.gl);
        renderer.framebuffers=new D3DFramebuffers(renderer,RDRAM);
        gln64j.core.Rsp.gsp.changed=gln64j.core.Rsp.gdp.changed=-1;
    }
    private void resize() {
        if(canvas!=null&&canvas.getWidth()>0&&canvas.getHeight()>0&&(canvas.getWidth()!=width||canvas.getHeight()!=height)) {
            D3DFramebuffers.capture();renderer.framebuffers.reset();width=canvas.getWidth();height=canvas.getHeight();NativeD3D11.resize(device,width,height);OpenGlGdp.setDrawableSize(width,height);renderer.width=width;renderer.height=height;
        }
    }
    @Override public synchronized void processDList() { if(device==0)return;resize();renderer.framebuffers.beginList();OpenGl.renderDisplayList(); }
    @Override public synchronized void updateScreen() {
        if(device==0)return;
        resize();
        int previous=renderer.framebuffers.selected();
        int display=renderer.framebuffers.display(REG.VI_Registers[REG.VI_ORIGIN] & 0xffffff);
        boolean changed=(Rsp.gdp.changed & gln64j.rdp.Gdp.CHANGED_COLORBUFFER)!=0;
        // Many games render at 30 fps but generate 60 VI events. Avoid resolving
        // and presenting an unchanged target twice, including during fast mode.
        if(changed || display!=presentedTarget || renderer.framebuffers.contentVersion()!=presentedContent) {
            NativeD3D11.selectTarget(device,display);
            NativeD3D11.present(device,D3D11Settings.vsync&&kerneln64.Main.SpeedMultiplier<=1);
            NativeD3D11.selectTarget(device,previous);
            presentedTarget=display;presentedContent=renderer.framebuffers.contentVersion();
        }
        Rsp.gdp.changed&=~gln64j.rdp.Gdp.CHANGED_COLORBUFFER;
    }
    @Override public synchronized void romClosed() {
        if(renderer!=null)renderer.framebuffers.close();
        if(device!=0){NativeD3D11.close(device);device=0;}renderer=null;OpenGlGdp.hDC=null;
        if(canvas!=null){Canvas previous=canvas;canvas=null;runEdt(() -> {if(previous.getParent()!=null)previous.getParent().remove(previous);hWnd.revalidate();hWnd.repaint();});}
    }
    @Override public void closePlugin() { romClosed(); }
    public synchronized BufferedImage screenshot() {
        int previous=renderer.framebuffers.selected();
        NativeD3D11.selectTarget(device,renderer.framebuffers.display(REG.VI_Registers[REG.VI_ORIGIN]&0xffffff));
        byte[] rgba=NativeD3D11.read(device);NativeD3D11.selectTarget(device,previous);BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=(y*width+x)*4;image.setRGB(x,y,((rgba[i]&255)<<16)|((rgba[i+1]&255)<<8)|(rgba[i+2]&255));}return image;
    }
    public synchronized void preview(BufferedImage image) {
        ByteBuffer rgba=ByteBuffer.allocateDirect(image.getWidth()*image.getHeight()*4);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int rgb=image.getRGB(x,y);rgba.put((byte)(rgb>>16)).put((byte)(rgb>>8)).put((byte)rgb).put((byte)255);}rgba.flip();
        NativeD3D11.image(device,image.getWidth(),image.getHeight(),rgba);NativeD3D11.present(device,D3D11Settings.vsync);
    }
    public long drawCalls() { return renderer==null?0:renderer.drawCalls; }
    public long triangles() { return renderer==null?0:renderer.triangles; }
    private static void runEdt(Runnable action) {
        if(SwingUtilities.isEventDispatchThread()){action.run();return;}
        try{SwingUtilities.invokeAndWait(action);}catch(Exception error){throw new IllegalStateException("D3D11 window operation failed",error);}
    }
}
