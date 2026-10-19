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
import kerneln64.Main;
import static kerneln64.cpu.Cop1.FSTATUS_REGISTER;
import kerneln64.debugger.trace.Debug;
import kerneln64.debugger.trace.R4300iCommands;
import static kerneln64.cpu.Cpu.*;

/** Legacy instruction tracing; uses the same Cpu state as execution. */
public final class CpuDebugInteger {
    private CpuDebugInteger() {}

    public static void r4300i_ADDI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("ADDI", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_ADDI.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_ADDIU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("ADDIU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_ADDIU.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_SLTI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("SLTI", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SLTI.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_SLTIU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("SLTIU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_SLTIU.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_ANDI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("ANDI", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_ANDI.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_ORI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("ORI", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_ORI.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_XORI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("XORI", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_XORI.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_LUI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("LUI", 0, cpu.GPR[cpu.rt], (int)(cpu.offset << 16)));
        
        cpu.r4300i_LUI.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_DADDIU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getITypeDebug("DADDIU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)cpu.offset));
        
        cpu.r4300i_DADDIU.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rt=%X\n", cpu.GPR[cpu.rt]);
    }

    public static void r4300i_SPECIAL_SLL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_SLL.run();
    }

    public static void r4300i_SPECIAL_SRL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SRL", 0, cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SRL.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SRA_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SRA", 0, cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SRA.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SLLV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SLLV", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SLLV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SRLV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SRLV", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SRLV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SRAV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SRAV", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SRAV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SYSCALL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_SYSCALL.run();
    }

    public static void r4300i_SPECIAL_BREAK_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_BREAK.run();
    }

    public static void r4300i_SPECIAL_SYNC_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_SYNC.run();
    }

    public static void r4300i_SPECIAL_MFHI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MFHI", 0, 0, cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_MFHI.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_MTHI_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MTHI", cpu.GPR[cpu.rs], 0, 0));
        
        cpu.r4300i_SPECIAL_MTHI.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("HI=%X\n", cpu.HI);
    }

    public static void r4300i_SPECIAL_MFLO_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MFL0", 0, 0, cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_MFLO.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_MTLO_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MTL0", cpu.GPR[cpu.rs], 0, 0));
        
        cpu.r4300i_SPECIAL_MTLO.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X\n", cpu.LO);
    }

    public static void r4300i_SPECIAL_DSLLV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DSLLV", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DSLLV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DSRLV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DSRLV", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DSRLV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DSRAV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_DSRAV.run();
    }

    public static void r4300i_SPECIAL_MULT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MULT", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], 0));
        
        cpu.r4300i_SPECIAL_MULT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X HI=%X\n", cpu.LO, cpu.HI);
    }

    public static void r4300i_SPECIAL_MULTU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("MULTU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], 0));
        
        cpu.r4300i_SPECIAL_MULTU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X HI=%X\n", cpu.LO, cpu.HI);
    }

    public static void r4300i_SPECIAL_DIV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DIV", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], 0));
        
        cpu.r4300i_SPECIAL_DIV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X HI=%X\n", cpu.LO, cpu.HI);
    }

    public static void r4300i_SPECIAL_DIVU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DIVU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], 0));
        
        cpu.r4300i_SPECIAL_DIVU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X HI=%X\n", cpu.LO, cpu.HI);
    }

    public static void r4300i_SPECIAL_DMULT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_DMULT.run();
    }

    public static void r4300i_SPECIAL_DMULTU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DMULTU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], 0));
        
        cpu.r4300i_SPECIAL_DMULTU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X HI=%X\n", cpu.LO, cpu.HI);
    }

    public static void r4300i_SPECIAL_DDIV_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DDIV", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], 0));
        
        cpu.r4300i_SPECIAL_DDIV.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X HI=%X\n", cpu.LO, cpu.HI);
    }

    public static void r4300i_SPECIAL_DDIVU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DDIVU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], 0));
        
        cpu.r4300i_SPECIAL_DDIVU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("LO=%X HI=%X\n", cpu.LO, cpu.HI);
    }

    public static void r4300i_SPECIAL_ADD_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("ADD", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_ADD.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_ADDU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("ADDU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_ADDU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SUB_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SUB", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SUB.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SUBU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SUBU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SUBU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_AND_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("AND", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_AND.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_OR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("OR", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_OR.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_XOR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("XOR", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_XOR.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_NOR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("NOR", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_NOR.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SLT_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SLT", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SLT.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_SLTU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("SLTU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_SLTU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DADD_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_DADD.run();
    }

    public static void r4300i_SPECIAL_DADDU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DADDU", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DADDU.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DSUB_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_DSUB.run();
    }

    public static void r4300i_SPECIAL_DSUBU_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_DSUBU.run();
    }

    public static void r4300i_SPECIAL_TEQ_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_TEQ.run();
    }

    public static void r4300i_SPECIAL_DSLL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DSLL", 0, cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DSLL.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DSRL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DSRL", 0, cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DSRL.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DSRA_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_SPECIAL_DSRA.run();
    }

    public static void r4300i_SPECIAL_DSLL32_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DSLL32", 0, cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DSLL32.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DSRL32_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DSRL32", 0, cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DSRL32.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void r4300i_SPECIAL_DSRA32_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf(cpu.getRTypeDebug("DSRA32", 0, cpu.GPR[cpu.rt], cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_DSRA32.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_OP_OPCODES)
            System.out.printf("rd=%X\n", cpu.GPR[cpu.rd]);
    }

    public static void R4300i_UnknownOpcode_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        String Message = String.format("Unhandled R4300i OpCode at: %08X\n%s\n%X\n", cpu.pc,
                R4300iCommands.R4300iOpcodeName(cpu.currentInstr, cpu.pc), cpu.currentInstr);
        Message += "Stoping Emulation !";
        System.out.printf("-%X:Unhandled R4300i OpCode:%X\n", cpu.pc, cpu.currentInstr);
        System.err.printf("%s\n", Message);
        Main.exit(Message);
    }
}
