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
public final class CpuDebugBranches {
    private CpuDebugBranches() {}

    public static void r4300i_J_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_J_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getJTypeDebug("J", cpu.target << 2));
        
        cpu.r4300i_J.run();
        
        if (Debug.DEBUG_J_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation, cpu.GPR[31]);
    }

    public static void r4300i_JAL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_J_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getJTypeDebug("JAL", cpu.target << 2));
        
        cpu.r4300i_JAL.run();
        
        if (Debug.DEBUG_J_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X 31=%X\n", cpu.jumpToLocation, cpu.GPR[31]);
    }

    public static void r4300i_BEQ_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BEQ", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BEQ.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_BNE_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BNE", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BNE.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_BLEZ_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BLEZ", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BLEZ.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_BGTZ_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BGTZ", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BGTZ.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_BEQL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BEQL", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BEQL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_BNEL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BNEL", cpu.GPR[cpu.rs], cpu.GPR[cpu.rt], (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BNEL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_BLEZL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BLEZL", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BLEZL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_BGTZL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BGTZL", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_BGTZL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_SPECIAL_JR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getRTypeDebug("JR", cpu.GPR[cpu.rs], 0, 0));
        
        cpu.r4300i_SPECIAL_JR.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_SPECIAL_JALR_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getRTypeDebug("JALR", cpu.GPR[cpu.rs], 0, cpu.GPR[cpu.rd]));
        
        cpu.r4300i_SPECIAL_JALR.run();
        
        if (Debug.DEBUG_R_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X rd=%X\n", cpu.jumpToLocation, cpu.GPR[cpu.rd]);
    }

    public static void r4300i_REGIMM_BLTZ_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BLTZ", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_REGIMM_BLTZ.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_REGIMM_BGEZ_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BGEZ", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_REGIMM_BGEZ.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_REGIMM_BLTZL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BLTZL", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_REGIMM_BLTZL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_REGIMM_BGEZL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BGEZL", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_REGIMM_BGEZL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X\n", cpu.jumpToLocation);
    }

    public static void r4300i_REGIMM_BLTZAL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        cpu.r4300i_REGIMM_BLTZAL.run();
    }

    public static void r4300i_REGIMM_BGEZAL_Debug(kerneln64.debugger.trace.DebugCpu cpu) {
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf(cpu.getITypeDebug("BGEZAL", cpu.GPR[cpu.rs], 0, (int)((cpu.offset << 2) + 4)));
        
        cpu.r4300i_REGIMM_BGEZAL.run();
        
        if (Debug.DEBUG_I_OPCODES || Debug.DEBUG_BR_OPCODES)
            System.out.printf("JP=%X 31=%X\n", cpu.jumpToLocation, cpu.GPR[31]);
    }
}
