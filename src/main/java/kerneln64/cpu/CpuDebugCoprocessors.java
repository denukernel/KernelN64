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
public final class CpuDebugCoprocessors {
    private CpuDebugCoprocessors() {}

    public static void r4300i_COP0_MF_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MFC0", 0, cpu.GPR[cpu.rt], cpu.cop0.CP0[cpu.rd]&0xFFFFFFFFL));
        
        cpu.r4300i_COP0_MF.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_COP0_MT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MTC0", 0, cpu.GPR[cpu.rt], cpu.cop0.CP0[cpu.rd]&0xFFFFFFFFL));
        
        cpu.r4300i_COP0_MT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("rd=%X\n", cpu.cop0.CP0[cpu.rd]);
    }

    public static void r4300i_COP0_CO_TLBR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("%s.\n", cpu.getRTypeDebug("TLBR", 0, 0, 0));
        
        cpu.cop0.r4300i_COP0_CO_TLBR.run();
    }

    public static void r4300i_COP0_CO_TLBWI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.cop0.r4300i_COP0_CO_TLBWI.run();
    }

    public static void r4300i_COP0_CO_TLBWR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("%s.\n", cpu.getRTypeDebug("TLBWR", 0, 0, 0));
        
        cpu.cop0.r4300i_COP0_CO_TLBWR.run();
    }

    public static void r4300i_COP0_CO_TLBP_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("%s.\n", cpu.getRTypeDebug("TLBP", 0, 0, 0));
        
        cpu.cop0.r4300i_COP0_CO_TLBP.run();
    }

    public static void r4300i_COP0_CO_ERET_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("ERET", 0, 0, 0));
        
        cpu.r4300i_COP0_CO_ERET.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_COP1_MF_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MFC1", 0, cpu.GPR[cpu.rt], cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rd>>1 : cpu.rd].DW));
        
        cpu.r4300i_COP1_MF.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_COP1_DMF_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DMFC1", 0, cpu.GPR[cpu.rt], cpu.cop1.FPCR[cpu.rd]));
        
        cpu.r4300i_COP1_DMF.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_COP1_CF_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CFC1", 0, cpu.GPR[cpu.rt], cpu.cop1.FPCR[cpu.rd]));
        
        cpu.r4300i_COP1_CF.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_COP1_MT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MTC1", 0, cpu.GPR[cpu.rt], cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rd>>1 : cpu.rd].DW));
        
        cpu.r4300i_COP1_MT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fs=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rd>>1 : cpu.rd].DW);
    }

    public static void r4300i_COP1_DMT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DMTC1", 0, cpu.GPR[cpu.rt], cpu.cop1.FPCR[cpu.rd]));
        
        cpu.r4300i_COP1_DMT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fs=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.rd>>1 : cpu.rd].DW);
    }

    public static void r4300i_COP1_CT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CTC1", 0, cpu.GPR[cpu.rt], cpu.cop1.FPCR[cpu.rd]));
        
        cpu.r4300i_COP1_CT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fs=%X\n", cpu.cop1.FPCR[cpu.rd]);
    }

    public static void r4300i_COP1_BCF_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("BCF", 0, 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_COP1_BCF.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_COP1_BCT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("BCT", 0, 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_COP1_BCT.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_COP1_BCFL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("BCFL", 0, 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_COP1_BCFL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_COP1_BCTL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getITypeDebug("BCTL", 0, 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_COP1_BCTL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_COP1_S_ADD_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("ADD.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_ADD.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_SUB_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SUB.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_SUB.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_MUL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MUL.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_MUL.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_DIV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DIV.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_DIV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_SQRT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SQRT.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_SQRT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_ABS_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("ABS.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_ABS.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_MOV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MOV.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_MOV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_NEG_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("NEG.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_NEG.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_TRUNC_L_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("TRUNC.L.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_TRUNC_L.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_ROUND_W_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.cop1.r4300i_COP1_S_ROUND_W.run();
    }

    public static void r4300i_COP1_S_TRUNC_W_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("TRUNC.W.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_TRUNC_W.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_FLOOR_W_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.cop1.r4300i_COP1_S_FLOOR_W.run();
    }

    public static void r4300i_COP1_S_CVT_D_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.D.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_CVT_D.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_CVT_W_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.W.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_CVT_W.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_CVT_L_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.L.S", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_CVT_L.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_S_CMP_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CMP.S", 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_S_CMP.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("31=%X\n", cpu.cop1.FPCR[FSTATUS_REGISTER]);
    }

    public static void r4300i_COP1_D_ADD_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("ADD.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_ADD.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_SUB_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SUB.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_SUB.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_MUL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MUL.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_MUL.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_DIV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DIV.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_DIV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_SQRT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SQRT.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_SQRT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_ABS_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("ABS.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_ABS.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_MOV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MOV.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_MOV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_NEG_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("NEG.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_NEG.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_ROUND_W_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.cop1.r4300i_COP1_D_ROUND_W.run();
    }

    public static void r4300i_COP1_D_TRUNC_W_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("TRUNC.W.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_TRUNC_W.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_CVT_S_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.S.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_CVT_S.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_CVT_W_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.W.D", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_CVT_W.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_D_CVT_L_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.cop1.r4300i_COP1_D_CVT_L.run();
    }

    public static void r4300i_COP1_D_CMP_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CMP.D", 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.ft>>1 : cpu.cop1.ft].DW, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_D_CMP.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("31=%X\n", cpu.cop1.FPCR[FSTATUS_REGISTER]);
    }

    public static void r4300i_COP1_W_CVT_S_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.S.W", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_W_CVT_S.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_W_CVT_D_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.D.W", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_W_CVT_D.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_L_CVT_S_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("CVT.S.L", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW, 0, cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fs>>1 : cpu.cop1.fs].DW));
        
        cpu.cop1.r4300i_COP1_L_CVT_S.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_CP_OPCODES)
            System.out.printf("fd=%X\n", cpu.cop1.FPR[cpu.cop1.mode32 ? cpu.cop1.fd>>1 : cpu.cop1.fd].DW);
    }

    public static void r4300i_COP1_L_CVT_D_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.cop1.r4300i_COP1_L_CVT_D.run();
    }
}
