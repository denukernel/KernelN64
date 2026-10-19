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

package kerneln64.cpu;
import kerneln64.debugger.trace.DebugCpu;
import kerneln64.Main;


import kerneln64.cpu.Cpu;
import static kerneln64.cpu.Cop1.FSTATUS_REGISTER;
import kerneln64.debugger.trace.Debug;
import static kerneln64.cpu.Cpu.*;

/** Legacy instruction tracing; uses the same Cpu state as execution. */
public final class CpuDebugMemory {
    private CpuDebugMemory() {}

    public static void r4300i_LDL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_LDL.run();
    }

    public static void r4300i_LDR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_LDR.run();
    }

    public static void r4300i_LB_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LB", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LB.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)

            System.out.printf("rt=%X VA=%X\n", cpu.GPR[cpu.rt], (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset));
    }

    public static void r4300i_LH_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LH", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LH.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)

            System.out.printf("rt=%X VA=%X\n", cpu.GPR[cpu.rt], (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset));
    }

    public static void r4300i_LWL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LWL", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LWL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_LW_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LW", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LW.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)

            System.out.printf("rt=%X VA=%X\n", cpu.GPR[cpu.rt], (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset));
    }

    public static void r4300i_LBU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LBU", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LBU.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)

            System.out.printf("rt=%X VA=%X\n", cpu.GPR[cpu.rt], (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset));
    }

    public static void r4300i_LHU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LHU", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LHU.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)

            System.out.printf("rt=%X VA=%X\n", cpu.GPR[cpu.rt], (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset));
    }

    public static void r4300i_LWR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LWR", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LWR.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_LWU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_LWU.run();
    }

    public static void r4300i_SB_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)
            System.out.printf(cpu.getITypeDebug("SB", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SB.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset), (byte) (short)(((byte)(cpu.GPR[cpu.rt]&0xFF))&0xFF));
    }

    public static void r4300i_SH_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)
            System.out.printf(cpu.getITypeDebug("SH", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SH.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset), (short) (((int)((short)(cpu.GPR[cpu.rt]&0xFFFF)))&0xFFFF));
    }

    public static void r4300i_SWL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)
            System.out.printf(cpu.getITypeDebug("SWL", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SWL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset) & ~0x03, cpu.tmpWord);
    }

    public static void r4300i_SW_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)
            System.out.printf(cpu.getITypeDebug("SW", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SW.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset), (int)(cpu.GPR[cpu.rt]&0x00000000FFFFFFFFL));
    }

    public static void r4300i_SDL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SDL.run();
    }

    public static void r4300i_SDR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SDR.run();
    }

    public static void r4300i_SWR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)
            System.out.printf(cpu.getITypeDebug("SWR", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SWR.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset) & ~0x03, cpu.tmpWord);
    }

    public static void r4300i_CACHE_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_CACHE.run();
    }

    public static void r4300i_LL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_LL.run();
    }

    public static void r4300i_LWC1_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("LWC1", cpu.GPR[cpu.base], cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW, (int)cpu.offset));
        
        cpu.r4300i_LWC1.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES || Debug.DEBUG_CP_OPCODES)

            System.out.printf("ft=%X VA=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW, (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + (int)cpu.offset));
    }

    public static void r4300i_SC_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SC.run();
    }

    public static void r4300i_LD_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)
            System.out.printf(cpu.getITypeDebug("LD", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_LD.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES)

            System.out.printf("rt=%X VA=%X\n", cpu.GPR[cpu.rt], (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset));
    }

    public static void r4300i_LDC1_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("LDC1", cpu.GPR[cpu.base], cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW, (int)cpu.offset));
        
        cpu.r4300i_LDC1.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_LD_OPCODES || Debug.DEBUG_CP_OPCODES)

            System.out.printf("ft=%X VA=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW, (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + (int)cpu.offset));
    }

    public static void r4300i_SWC1_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("SWC1", cpu.GPR[cpu.base], cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW, (int)cpu.offset));
        
        cpu.r4300i_SWC1.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES || Debug.DEBUG_CP_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset), cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW);
    }

    public static void r4300i_SDC1_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("SDC1", cpu.GPR[cpu.base], cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW, (int)cpu.offset));
        
        cpu.r4300i_SDC1.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES || Debug.DEBUG_CP_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset), cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rt>>1 : cpu.rt].DW);
    }

    public static void r4300i_SD_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)
            System.out.printf(cpu.getITypeDebug("SD", cpu.GPR[cpu.base], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SD.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_ST_OPCODES)

            System.out.printf("VA=%X *=%X\n", (int)((cpu.GPR[cpu.base]&0x00000000FFFFFFFFL) + cpu.offset), cpu.GPR[cpu.rt]);
    }
}
