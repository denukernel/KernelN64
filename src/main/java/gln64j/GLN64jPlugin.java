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
 * Originally based on Project64 code.
 *
 */


package gln64j;
import gln64j.config.GLN64jConfig;
import gln64j.config.GLN64jPluginSettings;
import gln64j.core.Registers;
import gln64j.core.Rsp;
import gln64j.opengl.OpenGl;
import gln64j.opengl.OpenGlGdp;

import gln64j.debug.Debug;
import gln64j.rsp.Gsp;
import gln64j.rdp.Gdp;
import java.nio.ByteBuffer;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import plugin.GfxPlugin;


public class GLN64jPlugin implements GfxPlugin {
    
    public static final boolean DEBUG = Debug.DEBUG_GLN64;
    
    private static final int NUM_FRAMES = 7;
    
//    public static GL gl;
    
    // used by OpenGl
    public static JFrame hWnd;
    
    // used by Gdp
    public static Runnable CheckInterrupts;
    
    public static Registers REG = new Registers();
    public static ByteBuffer DMEM;
    public static ByteBuffer IMEM;
    public static ByteBuffer TMEM = ByteBuffer.allocate(8 * 512);
    public static ByteBuffer RDRAM;
    public static int RDRAMSize;
    
    // used by OpenGl, Gdp
//    public static int screenWidth;
//    public static int screenHeight;
    
//    private long lastOrigin;
    private String name;
    
//    private long lastFrame;
//    private long[] frames = new long[NUM_FRAMES];
//    private int currentFrame;
//    private long frequency;
//    private int oldViVsyncReg = 0;
//    private int viIntrTime = 500000;
//    private int viFieldNumber;
    
    /** Creates a new instance of JglN64 */
    public GLN64jPlugin() {

        
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
    
    @Override
    public void pluginConfig(JFrame parent) {
        new GLN64jConfig(parent).setVisible(true);
    }

    
    public void pluginTest(JFrame hParent) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") pluginTest.");
    }
    
    public void drawScreen() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") drawScreen.");
    }
    
    public void getPluginInfo(PluginInfo pluginInfo) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") getPluginInfo.");
    }
    
    public boolean initiateGfx(GfxInfo gfxInfo) {
    	gln64j.config.GLN64jPluginSettings.load();
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") initiateGfx.");
        
        hWnd = gfxInfo.hWnd;

        DMEM = gfxInfo.dmem;
        IMEM = gfxInfo.imem;
        RDRAM = gfxInfo.rdram;
        
        REG.MI_INTR = gfxInfo.MI_INTR_REG;
        REG.MI_Registers = gfxInfo.miRegisters;
        
        REG.DPC_START = gfxInfo.DPC_START_REG;
        REG.DPC_END = gfxInfo.DPC_END_REG;
        REG.DPC_CURRENT = gfxInfo.DPC_CURRENT_REG;
        REG.DPC_STATUS = gfxInfo.DPC_STATUS_REG;
        REG.DPC_CLOCK = gfxInfo.DPC_CLOCK_REG;
        REG.DPC_BUFBUSY = gfxInfo.DPC_BUFBUSY_REG;
        REG.DPC_PIPEBUSY = gfxInfo.DPC_PIPEBUSY_REG;
        REG.DPC_TMEM = gfxInfo.DPC_TMEM_REG;
        REG.DPC_Registers = gfxInfo.dpcRegisters;
        
        REG.VI_STATUS = VI_STATUS_REG;
        REG.VI_ORIGIN = VI_ORIGIN_REG;
        REG.VI_WIDTH = VI_WIDTH_REG;
        REG.VI_INTR = VI_INTR_REG;
        REG.VI_V_CURRENT_LINE = VI_CURRENT_REG;
        REG.VI_TIMING = VI_BURST_REG;
        REG.VI_V_SYNC = VI_V_SYNC_REG;
        REG.VI_H_SYNC = VI_H_SYNC_REG;
        REG.VI_LEAP = VI_LEAP_REG;
        REG.VI_H_START = VI_H_START_REG;
        REG.VI_V_START = VI_V_START_REG;
        REG.VI_V_BURST = VI_V_BURST_REG;
        REG.VI_X_SCALE = VI_X_SCALE_REG;
        REG.VI_Y_SCALE = VI_Y_SCALE_REG;
        REG.VI_Registers = gfxInfo.viRegisters;
        
        CheckInterrupts = gfxInfo.checkInterrupts;
        
        return true;
    }
    
    public void moveScreen(int xpos, int ypos) {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") moveScreen.");
    }
    
    public void processDList() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") processDList.");
        OpenGlGdp.hDC.display();
    }
    
    public void processRDPList() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") processRDPList.");
    }
    
    public void romClosed() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") romClosed.");
        OpenGl.OGL_Stop();
        if (DEBUG) Debug.CloseDebugDlg();
    }
    
    public void romOpen() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") romOpen.");
        OpenGl.uc_start = OpenGl.uc_dstart = 0;
        RDRAMSize = RDRAM.capacity();

        OpenGlGdp.combiners = new gln64j.rdp.combiners.Combiners();
        OpenGlGdp.cache = new gln64j.rdp.textures.TextureCache();
        Rsp.gsp = new Gsp(RDRAM, DMEM);
        Rsp.gdp = new Gdp(CheckInterrupts, REG);
        // Configure the new cache, not the previous backend's cache from the
        // constructor. I8/IA16 need automatic format selection for alpha textures.
        OpenGl.config();

        OpenGl.OGL_Start();
        
        OpenGlGdp.OGL_ResizeWindow();
        if (DEBUG) Debug.OpenDebugDlg();
    }
    
    public void showCFB() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") showCFB.");
    }
    
    public void updateScreen() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") updateScreen.");


        OpenGlGdp.viUpdateScreen();
        

    }
    
    public void viStatusChanged() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") viStatusChanged.");
    }
    
    public void viWidthChanged() {
        if (DEBUG) System.out.println("GFX Plugin (" + name + ") viWidthChanged.");
    }


    // Private Methods /////////////////////////////////////////////////////////


}
