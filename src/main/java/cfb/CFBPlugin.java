/**
 * Copyright 2005 Jason LaDere
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Originally based on Zilmar's Basic CFB Plugin.
 *
 */

package cfb;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.swing.JFrame;
import plugin.GfxPlugin;


public class CFBPlugin implements GfxPlugin {
    
    public static final boolean DEBUG = false;
    
    private Graphics2D ddPrimary;
    private java.awt.Canvas canvas;
    private BufferedImage ddCFB;
    private boolean fullScreen;
    private int[] convet16to32 = new int[65536];
    private int viStatus;
    private int viWidth;
    private String name;
    
    private JFrame hWnd;
    private int[] viReg;
    private ByteBuffer rdram;
    private boolean memoryBswaped;
    private Runnable checkInterrupts;
    private int[] miRegisters;
    private int miInterrupt;
    
    public CFBPlugin() {
        name = this.getClass().getName();
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") created.");
    }

    public void captureScreen(String directory) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") captureScreen.");
    }

    public void changeWindow() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") changeWindow.");
    }

    public void closePlugin() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") closePlugin.");
    }

    public void pluginAbout(JFrame hParent) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") pluginAbout.");
    }

    public void pluginConfig(JFrame hParent) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") pluginConfig.");
    }

    public void pluginTest(JFrame hParent) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") pluginTest.");
    }

    public void drawScreen() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") drawScreen.");
        if (ddCFB == null)
            return;
        java.awt.Graphics output=canvas==null?null:canvas.getGraphics();
        if(output!=null)try{output.drawImage(ddCFB,0,0,canvas.getWidth(),canvas.getHeight(),null);}finally{output.dispose();}
    }

    public void getPluginInfo(PluginInfo pluginInfo) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") getPluginInfo.");
        pluginInfo.version = 0x0102;
        pluginInfo.type = PLUGIN_TYPE_GFX;
        pluginInfo.name = "Basic CFB plugin by zilmar";
        pluginInfo.normalMemory = true;
        pluginInfo.memoryBswaped = true;
    }

    public boolean initiateGfx(GfxInfo gfxInfo) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") initiateGFX.");
        this.hWnd = gfxInfo.hWnd;this.checkInterrupts=gfxInfo.checkInterrupts;this.miRegisters=gfxInfo.miRegisters;this.miInterrupt=gfxInfo.MI_INTR_REG;
        this.viReg = gfxInfo.viRegisters;
        this.rdram = gfxInfo.rdram;
        this.memoryBswaped = gfxInfo.memoryBswaped;
        
        int red, green, blue, alpha;
        int color16;
        int color32;
        
        fullScreen = false;
        ddPrimary = null;
        
        for (int count = 0; count < 65536; count ++) {
            color16 = count;
            red   = (color16 & 0xF800) >> 11;
            green = (color16 & 0x07C0) >> 6;
            blue  = (color16 & 0x003E) >> 1;
            alpha = (color16 & 0x0001);
            color32 = (alpha<<27) | (red<<19) | (green<<11) | (blue<<3);
            convet16to32[count] = color32;
        }
        
        ddCFB  = null;
        return true;
    }

    public void moveScreen(int xpos, int ypos) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") moveScreen.");
    }
    
    public void processDList() {
        // Even a framebuffer-only renderer must complete the guest's DP task.
        // Otherwise the game waits forever and cannot resume after a live switch.
        miRegisters[miInterrupt]|=0x20;checkInterrupts.run();
    }

    public void processRDPList() {
        miRegisters[miInterrupt]|=0x20;checkInterrupts.run();
    }

    public void romClosed() { onEdt(()->{if(canvas!=null && canvas.getParent()!=null)canvas.getParent().remove(canvas);canvas=null;}); }
    private static void onEdt(Runnable action){if(javax.swing.SwingUtilities.isEventDispatchThread()){action.run();return;}try{javax.swing.SwingUtilities.invokeAndWait(action);}catch(Exception e){throw new IllegalStateException(e);}}

    public void romOpen() {
        onEdt(()->{
            canvas=new java.awt.Canvas();canvas.setBackground(java.awt.Color.BLACK);canvas.setPreferredSize(new Dimension(640,480));
            kerneln64.ui.KeyboardRouting.attach(canvas);
            hWnd.getContentPane().removeAll();hWnd.add(canvas,java.awt.BorderLayout.CENTER);
            if(kerneln64.Main.emulationStatus!=null)hWnd.add(kerneln64.Main.emulationStatus,java.awt.BorderLayout.SOUTH);
            hWnd.pack();canvas.requestFocusInWindow();
        });setupCFB();
    }

    public void showCFB() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") showCFB.");
    }

    public BufferedImage screenshot() {
        if (ddCFB == null) return null;
        BufferedImage copy = new BufferedImage(ddCFB.getWidth(), ddCFB.getHeight(), BufferedImage.TYPE_INT_RGB);
        copy.setData(ddCFB.getData()); return copy;
    }
    public void presentScreenshot(BufferedImage image) {
        BufferedImage previous = ddCFB;
        try { ddCFB = image; drawScreen(); }
        finally { ddCFB = previous; }
    }

    public void updateScreen() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") updateScreen.");
        BufferedImage surfBuf;
        IntBuffer buffer;
        if (ddCFB == null)
            return;
        
        int w=viReg[VI_WIDTH_REG],h=(w>>2)*3,origin=viReg[VI_ORIGIN_REG]&0xFFFFFF;
        int type=viReg[VI_STATUS_REG]&3,bytes=type==3?4:2;
        if(w<1 || h<1 || w>2048 || (long)origin+(long)w*h*bytes>rdram.capacity())return;
        if(ddCFB.getWidth()!=w || ddCFB.getHeight()!=h)setupCFB();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int offset=origin+(y*w+x)*bytes,color;
            if(bytes==4)color=rdram.getInt(offset)>>>8;
            else {int v=rdram.getShort(offset)&65535;color=(((v>>>11)&31)*255/31<<16)|(((v>>>6)&31)*255/31<<8)|(((v>>>1)&31)*255/31);}
            ddCFB.setRGB(x,y,color);
        }
        drawScreen();
        
        return;
    }

    public void viStatusChanged() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") viStatusChanged.");
        if (viReg[VI_STATUS_REG] != viStatus) {
            setupCFB();
            viStatus = viReg[VI_STATUS_REG];
        }
    }

    public void viWidthChanged() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") viWidthChanged.");
        if (viReg[VI_WIDTH_REG] != viWidth) {
            setupCFB();
            viWidth = viReg[VI_WIDTH_REG];
            changeWinSize(hWnd, viWidth);
        }
    }
    
    public int updateCurrentHalfLine(int timer) {
        return 0;
    }
    
    // Private Methods /////////////////////////////////////////////////////////
    
    private void setupCFB() {
        if (viReg[VI_STATUS_REG] == 0)
            return;
        if (viReg[VI_WIDTH_REG] == 0)
            return;
        
        int w = viReg[VI_WIDTH_REG];  
        int h = ((viReg[VI_WIDTH_REG] >> 2) * 3);
        ddCFB = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }
    
    private void changeWinSize(JFrame hWnd, int width) {
        if (fullScreen)
            return;
        if(canvas!=null && width>0 && width<=2048)onEdt(()->{canvas.setPreferredSize(new Dimension(width,(width>>2)*3));hWnd.pack();});
    }
    
}
