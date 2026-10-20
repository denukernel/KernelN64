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


package kerneln64.hardware;
import kerneln64.cpu.Cop0;
import kerneln64.ui.EmulationStatus;
import kerneln64.rom.GameSharkFramebuffer;
import kerneln64.memory.Memory;
import kerneln64.cpu.Registers;
import kerneln64.state.Rewind;
import kerneln64.ui.Screenshots;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.Properties;
import javax.swing.JFrame;
import plugin.GfxPlugin;


public class Video {
    
    public static final String GFX_PLUGIN = "GFX_PLUGIN";
    
//    // regVI Video Interface (VI) Registers
//    public static final int VI_STATUS_REG=			0;
//    public static final int VI_ORIGIN_REG= 			1;
//    public static final int VI_WIDTH_REG= 			2;
//    public static final int VI_INTR_REG=  			3;
//    public static final int VI_CURRENT_REG=                     4;
//    public static final int VI_BURST_REG=  			5;
//    public static final int VI_V_SYNC_REG= 			6;
//    public static final int VI_H_SYNC_REG= 			7;
//    public static final int VI_LEAP_REG=  			8;
//    public static final int VI_H_START_REG=                     9;
//    public static final int VI_V_START_REG=                     10;
//    public static final int VI_V_BURST_REG=			11;
//    public static final int VI_X_SCALE_REG=			12;
//    public static final int VI_Y_SCALE_REG=			13;
    
    private static final int NUM_FRAMES = 7;
    
    // used by Main
    public long millisInGfx;
    
    // Video Interface (VI) Registers
    // used by Memory
    public int[] regVI = new int[14];
    
    // used by Main, Memory
    public GfxPlugin gfxPlugin;
    
    private Runnable checkInterrupts;
    private ByteBuffer rDram;
    private ByteBuffer dMem;
    private ByteBuffer iMem;
    private int[] regMI;
    private int[] regDPC;
    private JFrame hWnd;
    long lastFrame;
    private volatile long lastTimeNano;
    private long[] frames = new long[NUM_FRAMES];
    public int currentFrame;
    private long frequency;
    int oldViVsyncReg = 0;
    private int viIntrTime = 500000;
    int viFieldNumber;
    private int halfLine;
    private Cop0 cop0; // for timer
    
    public Runnable timerInterrupt = new Runnable() {
        public void run() {
            Main.cheats.applyRuntime(Main.mem.RDRAM, Main.cpu);
            regMI[Registers.MI_INTR_REG] |= Registers.MI_INTR_VI;
            checkInterrupts.run();
            if (gfxPlugin != null) {
                cop0.update();
                long time = System.currentTimeMillis();

                cop0.changeTimer(Cop0.VI_TIMER, refreshScreen());
                millisInGfx += (System.currentTimeMillis() - time);
            }
        }
    };
    
    /** Creates a new instance of Video */
    public Video(Runnable checkInterrupts, Registers regs, ByteBuffer rDram, ByteBuffer dMem, ByteBuffer iMem) {
        this.checkInterrupts = checkInterrupts;
        this.rDram = rDram;
        this.dMem = dMem;
        this.iMem = iMem;
        this.regMI = regs.regMI;
        this.regDPC = regs.regDPC;
        frequency = 1000;
        currentFrame = 0;
        viFieldNumber = 0;
        lastTimeNano = System.nanoTime();
    }
    
    // called by Main
    public void setTimer(Cop0 cop0) {
        this.cop0 = cop0;
    }

    // called by Main
    public void setFrameLimit(boolean frameLimit) {
        // Redundant with Main.FrameLimit but kept for API compatibility if needed
    }

    public void resetTimer() {
        lastTimeNano = System.nanoTime();
    }
    
    // called by Main
    public boolean setupPlugin(JFrame hWnd, Properties cfg) {

        this.hWnd = hWnd;
        String gfx_plugin = cfg.getProperty(GFX_PLUGIN, "DEFAULT_GFX_PLUGIN");
        try {
            Class c = Class.forName(gfx_plugin);
            gfxPlugin = (GfxPlugin)c.newInstance();
        } catch (Exception ex) {
            System.err.println("No gfx plugin loaded.");
            ex.printStackTrace();
            return false;
        }
        
        GfxPlugin.GfxInfo gfxInfo = new GfxPlugin.GfxInfo();
        
        gfxInfo.memoryBswaped = true;
        gfxInfo.checkInterrupts = checkInterrupts;
        gfxInfo.hWnd = hWnd;
        gfxInfo.rdram = rDram;
        gfxInfo.dmem = dMem;
        gfxInfo.imem = iMem;
        
        gfxInfo.MI_INTR_REG = Registers.MI_INTR_REG;
        gfxInfo.miRegisters = regMI;
        gfxInfo.DPC_START_REG = Registers.DPC_START_REG;
        gfxInfo.DPC_END_REG = Registers.DPC_END_REG;
        gfxInfo.DPC_CURRENT_REG = Registers.DPC_CURRENT_REG;
        gfxInfo.DPC_STATUS_REG = Registers.DPC_STATUS_REG;
        gfxInfo.DPC_CLOCK_REG = Registers.DPC_CLOCK_REG;
        gfxInfo.DPC_BUFBUSY_REG = Registers.DPC_BUFBUSY_REG;
        gfxInfo.DPC_PIPEBUSY_REG = Registers.DPC_PIPEBUSY_REG;
        gfxInfo.DPC_TMEM_REG = Registers.DPC_TMEM_REG;
        gfxInfo.dpcRegisters = regDPC;

        gfxInfo.viRegisters = regVI;
        
        if (!gfxPlugin.initiateGfx(gfxInfo) ) {
            System.err.println("Failed to Initilize Graphics!");
            return false;
        }
        
        return true;
    }
    
    private int refreshScreen(){
        //long time = System.currentTimeMillis();
        
    	if (oldViVsyncReg != regVI[gfxPlugin.VI_V_SYNC_REG]) {
    	    if (regVI[gfxPlugin.VI_V_SYNC_REG] == 0) {
    	        viIntrTime = 500000;
    	    } else {
    	        viIntrTime = (regVI[gfxPlugin.VI_V_SYNC_REG] + 1) * 1500;
    	        if ((regVI[gfxPlugin.VI_V_SYNC_REG] % 1) != 0) {
    	            viIntrTime -= 38;
    	        }
    	    }

    	    // FIX: update old value
    	    oldViVsyncReg = regVI[gfxPlugin.VI_V_SYNC_REG];
    	}

        if ((regVI[gfxPlugin.VI_STATUS_REG] & 0x10) != 0) {
            if (viFieldNumber == 0) {
                viFieldNumber = 1;
            } else {
                viFieldNumber = 0;
            }
        } else {
            viFieldNumber = 0;
        }
        currentFrame += 1;
        
        long graphicsStart = System.nanoTime();
        if (gfxPlugin != null) {
            var firmwareFrame=GameSharkFramebuffer.read();
            if(firmwareFrame!=null)Screenshots.present(firmwareFrame);
            else gfxPlugin.updateScreen();
        }
        Screenshots.service();
        EmulationStatus.graphicsNanos += System.nanoTime() - graphicsStart;
        
        // High-precision speed-aware frame limiter
        if (Main.FrameLimit) {
            long targetIntervalNano = (long)(16666667L / Main.SpeedMultiplier);
            long nextFrameTime = lastTimeNano + targetIntervalNano;
            
            long idleStart = System.nanoTime();
            while (Main.FrameLimit && System.nanoTime() < nextFrameTime) {
                long remainingNano = nextFrameTime - System.nanoTime();
                if (remainingNano > 200000L)
                    java.util.concurrent.locks.LockSupport.parkNanos(remainingNano - 200000L);
                else
                    Thread.onSpinWait();
            }
            long now = System.nanoTime();
            // Scheduler overshoot must not accumulate into a slower VI rate.
            // Limit catch-up to one VI after a slow frame or a pause.
            lastTimeNano = Math.max(nextFrameTime, now - targetIntervalNano);
            EmulationStatus.idleNanos += now - idleStart;
        } else lastTimeNano = System.nanoTime();
        
        EmulationStatus.vi();
        Rewind.onVi();

        // Fast-forward changes wall-clock pacing, never the emulated VI period.
        return viIntrTime;
    }


    public int updateCurrentHalfLine(int timer) {
        if (timer < 0)
            return 0;
        int halfLine = (timer / 1500);
        halfLine &= ~1;
        halfLine += viFieldNumber;
        return halfLine;
    }


}
