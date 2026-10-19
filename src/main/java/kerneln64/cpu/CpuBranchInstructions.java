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


import kerneln64.memory.Memory.MemoryException;
import kerneln64.cpu.Cop0.TlbException;
import static kerneln64.cpu.Cpu.*;

/** Jumps and branches, including their delay-slot execution. */
final class CpuBranchInstructions {
    private CpuBranchInstructions() {}

    static void r4300i_J(Cpu cpu) {
        cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, (cpu.pc&0xF0000000) + ((cpu.inst&0x3FFFFFF) << 2), 0, 0);
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_JAL(Cpu cpu) {
        cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, (cpu.pc&0xF0000000) + ((cpu.inst&0x3FFFFFF) << 2), 0, 0);
        cpu.GPR[31] = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_BEQ(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] == cpu.GPR[(cpu.inst>>RT)&0x1F])
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, (cpu.inst>>RT)&0x1F);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_BNE(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] != cpu.GPR[(cpu.inst>>RT)&0x1F])
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, (cpu.inst>>RT)&0x1F);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_BLEZ(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] <= 0L)
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_BGTZ(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] > 0L)
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_BEQL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] == cpu.GPR[(cpu.inst>>RT)&0x1F]) {
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, (cpu.inst>>RT)&0x1F);
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_BNEL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] != cpu.GPR[(cpu.inst>>RT)&0x1F]) {
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, (cpu.inst>>RT)&0x1F);
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_BLEZL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] <= 0L) {
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_BGTZL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] > 0L) {
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_SPECIAL_JR(Cpu cpu) {
        cpu.jumpToLocation = (int)cpu.GPR[(cpu.inst>>RS)&0x1F];
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_SPECIAL_JALR(Cpu cpu) {
        cpu.jumpToLocation = (int)cpu.GPR[(cpu.inst>>RS)&0x1F];
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_REGIMM_BLTZ(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] < 0L)
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_REGIMM_BGEZ(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] >= 0L)
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_REGIMM_BLTZL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] < 0L) {
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_REGIMM_BGEZL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] >= 0L) {
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
            cpu.pc += 4;
            cpu.inDelaySlot = true;
            if (!cpu.executeInstruction()) return;
            cpu.jumpTo(cpu.jumpToLocation);
        } else {
            cpu.pc += 4;
        }
    }

    static void r4300i_REGIMM_BLTZAL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] < 0L)
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.GPR[31] = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
    }

    static void r4300i_REGIMM_BGEZAL(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] >= 0L)
            cpu.jumpToLocation = cpu.testInterpreterJump(cpu.pc, cpu.pc + (((short)cpu.inst) << 2) + 4, (cpu.inst>>RS)&0x1F, 0);
        else
            cpu.jumpToLocation = cpu.pc + 8;
        cpu.GPR[31] = cpu.pc + 8;
        cpu.pc += 4;
        cpu.inDelaySlot = true;
        if (!cpu.executeInstruction()) return;
        cpu.jumpTo(cpu.jumpToLocation);
        }

    static boolean delaySlotEffectsCompare(Cpu cpu, int pc, int reg1, int reg2) {
        try {
            int instr = cpu.mem.loadWord(cpu.cop0.translateVaddr(pc + 4));
            cpu.tmpOp = (instr>>26)&0x3F;
            cpu.tmpRs = (instr>>21)&0x1F;
            cpu.tmpRt = (instr>>16)&0x1F;
            cpu.tmpRd = (instr>>11)&0x1F;
            cpu.tmpFunct = (instr)&0x3F;
        } catch (TlbException e) {
            return true;
        } catch (MemoryException e) {
            e.printStackTrace();
            return true;
        }
         
        switch (cpu.tmpOp) {
            case R4300i_SPECIAL:
                switch (cpu.tmpFunct) {
                    case R4300i_SPECIAL_SLL:
                    case R4300i_SPECIAL_SRL:
                    case R4300i_SPECIAL_SRA:
                    case R4300i_SPECIAL_SLLV:
                    case R4300i_SPECIAL_SRLV:
                    case R4300i_SPECIAL_SRAV:
                    case R4300i_SPECIAL_MFHI:
                    case R4300i_SPECIAL_MTHI:
                    case R4300i_SPECIAL_MFLO:
                    case R4300i_SPECIAL_MTLO:
                    case R4300i_SPECIAL_DSLLV:
                    case R4300i_SPECIAL_DSRLV:
                    case R4300i_SPECIAL_DSRAV:
                    case R4300i_SPECIAL_ADD:
                    case R4300i_SPECIAL_ADDU:
                    case R4300i_SPECIAL_SUB:
                    case R4300i_SPECIAL_SUBU:
                    case R4300i_SPECIAL_AND:
                    case R4300i_SPECIAL_OR:
                    case R4300i_SPECIAL_XOR:
                    case R4300i_SPECIAL_NOR:
                    case R4300i_SPECIAL_SLT:
                    case R4300i_SPECIAL_SLTU:
                    case R4300i_SPECIAL_DADD:
                    case R4300i_SPECIAL_DADDU:
                    case R4300i_SPECIAL_DSUB:
                    case R4300i_SPECIAL_DSUBU:
                    case R4300i_SPECIAL_DSLL:
                    case R4300i_SPECIAL_DSRL:
                    case R4300i_SPECIAL_DSRA:
                    case R4300i_SPECIAL_DSLL32:
                    case R4300i_SPECIAL_DSRL32:
                    case R4300i_SPECIAL_DSRA32:
                        if (cpu.tmpRd == 0) { return false; }
                        if (cpu.tmpRd == reg1) { return true; }
                        if (cpu.tmpRd == reg2) { return true; }
                        break;
                    case R4300i_SPECIAL_MULT:
                    case R4300i_SPECIAL_MULTU:
                    case R4300i_SPECIAL_DIV:
                    case R4300i_SPECIAL_DIVU:
                    case R4300i_SPECIAL_DMULT:
                    case R4300i_SPECIAL_DMULTU:
                    case R4300i_SPECIAL_DDIV:
                    case R4300i_SPECIAL_DDIVU:
                        break;
                    default:
                        return true;
                }
                break;
            case R4300i_CP0:
                switch (cpu.tmpRs) {
                    case R4300i_COP0_MT: break;
                    case R4300i_COP0_MF:
                        if (cpu.tmpRt == 0) { return false; }
                        if (cpu.tmpRt == reg1) { return true; }
                        if (cpu.tmpRt == reg2) { return true; }
                        break;
                    default:
                        if ( (cpu.tmpRs & 0x10 ) != 0 ) {
                            switch((cpu.inst&0x3F)) {
                                case R4300i_COP0_CO_TLBR: break;
                                case R4300i_COP0_CO_TLBWI: break;
                                case R4300i_COP0_CO_TLBWR: break;
                                case R4300i_COP0_CO_TLBP: break;
                                default:
                                    return true;
                            }
                        } else {
                            return true;
                        }
                }
                break;
            case R4300i_CP1:
                switch (cpu.tmpRs) {
                    case R4300i_COP1_MF:
                        if (cpu.tmpRt == 0) { return false; }
                        if (cpu.tmpRt == reg1) { return true; }
                        if (cpu.tmpRt == reg2) { return true; }
                        break;
                    case R4300i_COP1_CF: break;
                    case R4300i_COP1_MT: break;
                    case R4300i_COP1_CT: break;
                    case R4300i_COP1_S: break;
                    case R4300i_COP1_D: break;
                    case R4300i_COP1_W: break;
                    case R4300i_COP1_L: break;
                    default:
                        return true;
                }
                break;
            case R4300i_ANDI:
            case R4300i_ORI:
            case R4300i_XORI:
            case R4300i_LUI:
            case R4300i_ADDI:
            case R4300i_ADDIU:
            case R4300i_SLTI:
            case R4300i_SLTIU:
            case R4300i_DADDI:
            case R4300i_DADDIU:
            case R4300i_LB:
            case R4300i_LH:
            case R4300i_LW:
            case R4300i_LWL:
            case R4300i_LWR:
            case R4300i_LDL:
            case R4300i_LDR:
            case R4300i_LBU:
            case R4300i_LHU:
            case R4300i_LD:
            case R4300i_LWC1:
            case R4300i_LDC1:
                if (cpu.tmpRt == 0) { return false; }
                if (cpu.tmpRt == reg1) { return true; }
                if (cpu.tmpRt == reg2) { return true; }
                break;
            case R4300i_CACHE: break;
            case R4300i_SB: break;
            case R4300i_SH: break;
            case R4300i_SW: break;
            case R4300i_SWR: break;
            case R4300i_SWL: break;
            case R4300i_SWC1: break;
            case R4300i_SDC1: break;
            case R4300i_SD: break;
            default:
                return true;
        }
        return false;
    }
}
