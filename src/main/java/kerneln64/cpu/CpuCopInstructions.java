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
import kerneln64.Main;


import static kerneln64.cpu.Cpu.*;

/** Coprocessor register transfers, floating-point branches, and exception return. */
final class CpuCopInstructions {
    private CpuCopInstructions() {}

    static void r4300i_COP1_MF(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.cop1.mfC1((cpu.inst>>RD)&0x1F);
    }

    static void r4300i_COP1_DMF(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.cop1.dmfC1((cpu.inst>>RD)&0x1F);
    }

    static void r4300i_COP1_CF(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.cop1.cfC1((cpu.inst>>RD)&0x1F);
    }

    static void r4300i_COP1_MT(Cpu cpu) {
         cpu.cop1.mtC1((cpu.inst>>RD)&0x1F, (int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
    }

    static void r4300i_COP1_DMT(Cpu cpu) {
        cpu.cop1.dmtC1((cpu.inst>>RD)&0x1F, cpu.GPR[(cpu.inst>>RT)&0x1F]);
    }

    static void r4300i_COP1_CT(Cpu cpu) {
        cpu.cop1.ctC1((cpu.inst>>RD)&0x1F, (int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
    }

    static void r4300i_COP1_BCF(Cpu cpu) {
        if ((cpu.cop1.FPCR[Cop1.FSTATUS_REGISTER] & Cop1.FPCSR_C) == 0)
            cpu.jumpToLocation = cpu.pc + (((short)cpu.inst) << 2) + 4;
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_COP1_BCT(Cpu cpu) {
        if ((cpu.cop1.FPCR[Cop1.FSTATUS_REGISTER] & Cop1.FPCSR_C) != 0)
            cpu.jumpToLocation = cpu.pc + (((short)cpu.inst) << 2) + 4;
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_COP1_BCFL(Cpu cpu) {
        if ((cpu.cop1.FPCR[Cop1.FSTATUS_REGISTER] & Cop1.FPCSR_C) == 0) {
            cpu.jumpToLocation = cpu.pc + (((short)cpu.inst) << 2) + 4;
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_COP1_BCTL(Cpu cpu) {
        if ((cpu.cop1.FPCR[Cop1.FSTATUS_REGISTER] & Cop1.FPCSR_C) != 0) {
            cpu.jumpToLocation = cpu.pc + (((short)cpu.inst) << 2) + 4;
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_COP0_MF(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.cop0.mfC0((cpu.inst>>RD)&0x1F);
    }

    static void r4300i_COP0_MT(Cpu cpu) {
        cpu.cop0.mtC0((cpu.inst>>RD)&0x1F, (int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
        if (((cpu.inst>>RD)&0x1F) == Cop0.STATUS_REGISTER) {
            cpu.cop1.setMode32((cpu.cop0.CP0[Cop0.STATUS_REGISTER] & Cop0.STATUS_FR) == 0);
            cpu.CheckInterrupts.run();
        }
    }

    static void r4300i_COP0_CO_ERET(Cpu cpu) {
        if ((cpu.cop0.CP0[Cop0.STATUS_REGISTER] & Cop0.STATUS_ERL) != 0) {
            cpu.jumpToLocation = cpu.cop0.CP0[Cop0.ERROREPC_REGISTER];
            cpu.cop0.CP0[Cop0.STATUS_REGISTER] &= ~Cop0.STATUS_ERL;
        } else {
            cpu.jumpToLocation = cpu.cop0.CP0[Cop0.EPC_REGISTER];
            cpu.cop0.CP0[Cop0.STATUS_REGISTER] &= ~Cop0.STATUS_EXL;
        }
        cpu.llBit = 0;
        cpu.CheckInterrupts.run();
        cpu.jumpTo(cpu.jumpToLocation);
    }
}
