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
import kerneln64.memory.Dma;
import kerneln64.ui.EmulationStatus;
import kerneln64.memory.Memory;
import kerneln64.cpu.Registers;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.Properties;
import javax.swing.JFrame;

import gln64j.rdp.Gdp;
import gln64j.rsp.Gsp;
import plugin.AudioPlugin;
import plugin.GfxPlugin;


public class Rsp {
    
    // regSP SP Registers (RSP)
    public static final int SP_MEM_ADDR_REG=                    0;
    public static final int SP_DRAM_ADDR_REG=                   1;
    public static final int SP_RD_LEN_REG=			2;
    public static final int SP_WR_LEN_REG=			3;
    public static final int SP_STATUS_REG=			4;
    public static final int SP_DMA_FULL_REG=                    5;
    public static final int SP_DMA_BUSY_REG=                    6;
    public static final int SP_SEMAPHORE_REG=                   7;
    public static final int SP_PC_REG=                          8;
    public static final int SP_IBIST_REG=			9;
    
    public static final int SP_CLR_HALT=			0x00001;	/* Bit  0: clear halt */
    public static final int SP_SET_HALT=			0x00002;	/* Bit  1: set halt */
    public static final int SP_CLR_BROKE=                       0x00004;	/* Bit  2: clear broke */
    public static final int SP_CLR_INTR=			0x00008;	/* Bit  3: clear intr */
    public static final int SP_SET_INTR=			0x00010;	/* Bit  4: set intr */
    public static final int SP_CLR_SSTEP=                       0x00020;	/* Bit  5: clear sstep */
    public static final int SP_SET_SSTEP=                       0x00040;	/* Bit  6: set sstep */
    public static final int SP_CLR_INTR_BREAK=                  0x00080;	/* Bit  7: clear intr on break */
    public static final int SP_SET_INTR_BREAK=                  0x00100;	/* Bit  8: set intr on break */
    public static final int SP_CLR_SIG0=			0x00200;	/* Bit  9: clear signal 0 */
    public static final int SP_SET_SIG0=			0x00400;	/* Bit 10: set signal 0 */
    public static final int SP_CLR_SIG1=			0x00800;	/* Bit 11: clear signal 1 */
    public static final int SP_SET_SIG1=			0x01000;	/* Bit 12: set signal 1 */
    public static final int SP_CLR_SIG2=			0x02000;	/* Bit 13: clear signal 2 */
    public static final int SP_SET_SIG2=			0x04000;	/* Bit 14: set signal 2 */
    public static final int SP_CLR_SIG3=			0x08000;	/* Bit 15: clear signal 3 */
    public static final int SP_SET_SIG3=			0x10000;	/* Bit 16: set signal 3 */
    public static final int SP_CLR_SIG4=			0x20000;	/* Bit 17: clear signal 4 */
    public static final int SP_SET_SIG4=			0x40000;	/* Bit 18: set signal 4 */
    public static final int SP_CLR_SIG5=			0x80000;	/* Bit 19: clear signal 5 */
    public static final int SP_SET_SIG5=			0x100000;       /* Bit 20: set signal 5 */
    public static final int SP_CLR_SIG6=			0x200000;       /* Bit 21: clear signal 6 */
    public static final int SP_SET_SIG6=			0x400000;       /* Bit 22: set signal 6 */
    public static final int SP_CLR_SIG7=			0x800000;       /* Bit 23: clear signal 7 */
    public static final int SP_SET_SIG7=			0x1000000;      /* Bit 24: set signal 7 */
    
    public static final int SP_STATUS_HALT=                     0x001;		/* Bit  0: halt */
    public static final int SP_STATUS_BROKE=			0x002;		/* Bit  1: broke */
    public static final int SP_STATUS_DMA_BUSY=                 0x004;		/* Bit  2: dma busy */
    public static final int SP_STATUS_DMA_FULL=                 0x008;		/* Bit  3: dma full */
    public static final int SP_STATUS_IO_FULL=                  0x010;		/* Bit  4: io full */
    public static final int SP_STATUS_SSTEP=			0x020;		/* Bit  5: single step */
    public static final int SP_STATUS_INTR_BREAK=               0x040;		/* Bit  6: interrupt on break */
    public static final int SP_STATUS_SIG0=                     0x080;		/* Bit  7: signal 0 set */
    public static final int SP_STATUS_SIG1=                     0x100;		/* Bit  8: signal 1 set */
    public static final int SP_STATUS_SIG2=                     0x200;		/* Bit  9: signal 2 set */
    public static final int SP_STATUS_SIG3=                     0x400;		/* Bit 10: signal 3 set */
    public static final int SP_STATUS_SIG4=                     0x800;		/* Bit 11: signal 4 set */
    public static final int SP_STATUS_SIG5=                     0x1000;		/* Bit 12: signal 5 set */
    public static final int SP_STATUS_SIG6=                     0x2000;		/* Bit 13: signal 6 set */
    public static final int SP_STATUS_SIG7=                     0x4000;		/* Bit 14: signal 7 set */
    
    public static final boolean DLIST = true;
    public static final boolean ALIST = true;
    public static final int R4300i_SP_INTR = 0x1;
	public static Gsp gsp;
	public static Gdp gdp;
    
    // SP Registers (RSP)
    // used by Memory, Dma, Main(init)
    public int[] regSP = new int[10];
    
    // used by Main
    public long millisInDlist;
    public long millisInAlist;
    
    private Runnable checkInterrupts;
    private ByteBuffer rDram;
    private ByteBuffer dMem;
    private ByteBuffer iMem;
    private int[] regMI;
    private int[] regDPC;
//    private Runnable processDlistList;
//    private Runnable processAlistList;
    public GfxPlugin gfx;
    private AudioPlugin audio;
    private int taskType;
    
    /** Creates a new instance of Rsp */
    public Rsp(Runnable checkInterrupts, Registers regs, ByteBuffer rDram, ByteBuffer dMem, ByteBuffer iMem) {
        this.checkInterrupts = checkInterrupts;
        this.rDram = rDram;
        this.dMem = dMem;
        this.iMem = iMem;
        this.regMI = regs.regMI;
        this.regDPC = regs.regDPC;
        regSP[SP_STATUS_REG] = 0x00000001;
    }
    
//    private plugin.RSP_Plugin rspPlugin;
//    private plugin.RSP_Plugin.RSP_INFO rspInfo;
    
    public boolean setupPlugin(JFrame hWnd, Properties cfg, GfxPlugin gfx, AudioPlugin audio) {


        this.gfx = gfx;
        this.audio = audio;
        return true;
    }
    

    public long lastValidAudioTaskTime = System.currentTimeMillis();
    public boolean audioInitialized = false; // Prevents boot-up false positives
    // Retained for reflection-based savestate compatibility; the old warning popup was removed.
    public boolean audioHaltedAlertShown = false;
    // called by Memory
    public void runRsp(int mode) {
        if ((regSP[SP_STATUS_REG] & SP_STATUS_HALT) == 0) {
            if ((regSP[SP_STATUS_REG] & SP_STATUS_BROKE) == 0) {


                taskType = dMem.getInt(0xFC0);
                if (DLIST) {
                    if (taskType == 1) {
                        long profileStart = System.nanoTime();
                        long time = System.currentTimeMillis();
                        if (gfx != null)
                            gfx.processDList();
                        regSP[SP_STATUS_REG] |= 0x0203;
                        if ((regSP[SP_STATUS_REG] & SP_STATUS_INTR_BREAK) != 0) {
                            regMI[Registers.MI_INTR_REG] |= R4300i_SP_INTR;
                            checkInterrupts.run();
                        }
                        regDPC[Registers.DPC_STATUS_REG] &= ~2;
                        millisInDlist += (System.currentTimeMillis() - time);
                        EmulationStatus.graphicsNanos += System.nanoTime() - profileStart;
                        return;
                    }
                }
                if (ALIST) {
                    if (taskType == 2) {
                    	int aListLen = dMem.getInt(0xFF4); // Get actual length in bytes

                        // Only count this as a heartbeat if the length is > 0
                        if (aListLen > 0) {
                            lastValidAudioTaskTime = System.currentTimeMillis();
                            audioInitialized = true; // Audio has successfully started at least once
                        }

                        long profileStart = System.nanoTime();
                        long time = System.currentTimeMillis();
                        if (audio != null)
                            audio.processAList();
                        regSP[SP_STATUS_REG] |= 0x0203;
                        if ((regSP[SP_STATUS_REG] & SP_STATUS_INTR_BREAK) != 0) {
                            regMI[Registers.MI_INTR_REG] |= R4300i_SP_INTR;
                            checkInterrupts.run();
                        }
                        millisInAlist += (System.currentTimeMillis() - time);
                        EmulationStatus.audioNanos += System.nanoTime() - profileStart;
                        return;
                    }
                }
                if (DLIST && ALIST) {
                    regSP[SP_STATUS_REG] |= 0x0203;
                    if ((regSP[SP_STATUS_REG] & SP_STATUS_INTR_BREAK) != 0 ) {
                        regMI[Registers.MI_INTR_REG] |= R4300i_SP_INTR;
                        checkInterrupts.run();
                    }
                    return;
                }
                return;
            }
        }
    }
    public void clear() {
        for (int i = 0; i < dMem.capacity(); i++) dMem.put(i, (byte) 0);
        for (int i = 0; i < iMem.capacity(); i++) iMem.put(i, (byte) 0);
        for (int i = 0; i < regSP.length; i++) regSP[i] = 0;
        regSP[SP_STATUS_REG] = SP_STATUS_HALT;
        taskType = 0;
    }

    // called by Main
    public void shutdown() {

    }
    
}
