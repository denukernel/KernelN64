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

/** Integer arithmetic, shifts, comparisons, and special-register instructions. */
final class CpuIntegerInstructions {
    private CpuIntegerInstructions() {}

    static void r4300i_ADDI(Cpu cpu) {
        if (((cpu.inst>>RT)&0x1F) == 0) return;
        cpu.GPR[(cpu.inst>>RT)&0x1F] = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
    }

    static void r4300i_ADDIU(Cpu cpu) {
        int result=(int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (short)cpu.inst;
        if(Main.cheats.musicBurstShift!=0)result=Main.cheats.musicCountdown(cpu,cpu.inst,result);
        cpu.GPR[(cpu.inst>>RT)&0x1F]=result;
    }

    static void r4300i_SLTI(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] < (long)((short)cpu.inst))
            cpu.GPR[(cpu.inst>>RT)&0x1F] = 1L;
        else
            cpu.GPR[(cpu.inst>>RT)&0x1F] = 0L;
    }

    static void r4300i_SLTIU(Cpu cpu) {
        if (cpu.compareUnsignedLongs(cpu.GPR[(cpu.inst>>RS)&0x1F], (long)((short)cpu.inst)) < 0)
            cpu.GPR[(cpu.inst>>RT)&0x1F] = 1L;
        else
            cpu.GPR[(cpu.inst>>RT)&0x1F] = 0L;
    }

    static void r4300i_ANDI(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] & (cpu.inst&0xFFFF);
    }

    static void r4300i_ORI(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] | (cpu.inst&0xFFFF);
    }

    static void r4300i_XORI(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] ^ (cpu.inst&0xFFFF);
    }

    static void r4300i_LUI(Cpu cpu) {
        if (((cpu.inst>>RT)&0x1F) == 0) return;
        cpu.GPR[(cpu.inst>>RT)&0x1F] = (cpu.inst&0xFFFF) << 16;
    }

    static void r4300i_DADDIU(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RT)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] + (long)((short)cpu.inst);
    }

    static void r4300i_SPECIAL_SLL(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RT)&0x1F] << ((cpu.inst>>SA)&0x1F);
    }

    static void r4300i_SPECIAL_SRL(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RT)&0x1F] >>> ((cpu.inst>>SA)&0x1F);
    }

    static void r4300i_SPECIAL_SRA(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RT)&0x1F] >> ((cpu.inst>>SA)&0x1F);
    }

    static void r4300i_SPECIAL_SLLV(Cpu cpu) {
        if (((cpu.inst>>RD)&0x1F) == 0) return;
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RT)&0x1F] << (cpu.GPR[(cpu.inst>>RS)&0x1F]&0x1F);
    }

    static void r4300i_SPECIAL_SRLV(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RT)&0x1F] >>> (cpu.GPR[(cpu.inst>>RS)&0x1F]&0x1F);
    }

    static void r4300i_SPECIAL_SRAV(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RT)&0x1F] >> (cpu.GPR[(cpu.inst>>RS)&0x1F]&0x1F);
    }

    static void r4300i_SPECIAL_SYSCALL(Cpu cpu) {
        if (cpu.inDelaySlot)
            cpu.jumpToLocation = cpu.cop0.doSysCallException(true, cpu.pc) + 4;
        else
            cpu.jumpTo(cpu.cop0.doSysCallException(false, cpu.pc));
    }

    static void r4300i_SPECIAL_BREAK(Cpu cpu) {
        if (cpu.inDelaySlot)
            cpu.jumpToLocation = cpu.cop0.doBreakException(true, cpu.pc) + 4;
        else
            cpu.jumpTo(cpu.cop0.doBreakException(false, cpu.pc));
    }

    static void r4300i_SPECIAL_SYNC(Cpu cpu) {
    }

    static void r4300i_SPECIAL_MFHI(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.HI;
    }

    static void r4300i_SPECIAL_MTHI(Cpu cpu) {
        cpu.HI = cpu.GPR[(cpu.inst>>RS)&0x1F];
    }

    static void r4300i_SPECIAL_MFLO(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.LO;
    }

    static void r4300i_SPECIAL_MTLO(Cpu cpu) {
        cpu.LO = cpu.GPR[(cpu.inst>>RS)&0x1F];
    }

    static void r4300i_SPECIAL_DSLLV(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] << (cpu.GPR[(cpu.inst>>RS)&0x1F]&0x3F);
    }

    static void r4300i_SPECIAL_DSRLV(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] >>> (cpu.GPR[(cpu.inst>>RS)&0x1F]&0x3F);
    }

    static void r4300i_SPECIAL_DSRAV(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] >> (cpu.GPR[(cpu.inst>>RS)&0x1F]&0x3F);
    }

    static void r4300i_SPECIAL_MULT(Cpu cpu) {
        cpu.HI = (long)((int)cpu.GPR[(cpu.inst>>RS)&0x1F]) * (long)((int)cpu.GPR[(cpu.inst>>RT)&0x1F]);
        cpu.LO = (int)cpu.HI;
        cpu.HI = (int)(cpu.HI>>32);
    }

    static void r4300i_SPECIAL_MULTU(Cpu cpu) {
        cpu.HI = (cpu.GPR[(cpu.inst>>RS)&0x1F]&0xFFFFFFFFL) * (cpu.GPR[(cpu.inst>>RT)&0x1F]&0xFFFFFFFFL);
        cpu.LO = (int)cpu.HI;
        cpu.HI = (int)(cpu.HI>>32);
    }

    static void r4300i_SPECIAL_DIV(Cpu cpu) {
        cpu.LO = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] / (int)cpu.GPR[(cpu.inst>>RT)&0x1F];
        cpu.HI = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] % (int)cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_DIVU(Cpu cpu) {
        cpu.LO = (cpu.GPR[(cpu.inst>>RS)&0x1F]&0xFFFFFFFFL) / (cpu.GPR[(cpu.inst>>RT)&0x1F]&0xFFFFFFFFL);
        cpu.HI = (cpu.GPR[(cpu.inst>>RS)&0x1F]&0xFFFFFFFFL) % (cpu.GPR[(cpu.inst>>RT)&0x1F]&0xFFFFFFFFL);
    }

    static void r4300i_SPECIAL_DMULT(Cpu cpu) {
    }

    static void r4300i_SPECIAL_DMULTU(Cpu cpu) {
        cpu.LO = (cpu.GPR[(cpu.inst>>RS)&0x1F]&0xFFFFFFFFL) * (cpu.GPR[(cpu.inst>>RT)&0x1F]&0xFFFFFFFFL);
        cpu.tmpDmultu[0] = (cpu.GPR[(cpu.inst>>RS)&0x1F]>>>32) * (cpu.GPR[(cpu.inst>>RT)&0x1F]&0xFFFFFFFFL);
        cpu.tmpDmultu[1] = (cpu.GPR[(cpu.inst>>RS)&0x1F]&0xFFFFFFFFL) * (cpu.GPR[(cpu.inst>>RT)&0x1F]>>>32);
        cpu.HI = (cpu.GPR[(cpu.inst>>RS)&0x1F]>>>32) * (cpu.GPR[(cpu.inst>>RT)&0x1F]>>>32);
        cpu.tmpDmultu[2] = (cpu.LO>>>32) + (cpu.tmpDmultu[0]&0xFFFFFFFFL) + (cpu.tmpDmultu[1]&0xFFFFFFFFL);
        cpu.LO += ((cpu.tmpDmultu[0]&0xFFFFFFFFL) + (cpu.tmpDmultu[1]&0xFFFFFFFFL))<<32;
        cpu.HI += (cpu.tmpDmultu[0]>>>32) + (cpu.tmpDmultu[1]>>>32) + (cpu.tmpDmultu[2]>>>32);
    }

    static void r4300i_SPECIAL_DDIV(Cpu cpu) {
        cpu.LO = cpu.GPR[(cpu.inst>>RS)&0x1F] / cpu.GPR[(cpu.inst>>RT)&0x1F];
        cpu.HI = cpu.GPR[(cpu.inst>>RS)&0x1F] % cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_DDIVU(Cpu cpu) {
        cpu.LO = cpu.GPR[(cpu.inst>>RS)&0x1F] / cpu.GPR[(cpu.inst>>RT)&0x1F];
        cpu.HI = cpu.GPR[(cpu.inst>>RS)&0x1F] % cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_ADD(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (int)cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_ADDU(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] + (int)cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_SUB(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] - (int)cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_SUBU(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = (int)cpu.GPR[(cpu.inst>>RS)&0x1F] - (int)cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_AND(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] & cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_OR(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] | cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_XOR(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] ^ cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_NOR(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = ~(cpu.GPR[(cpu.inst>>RS)&0x1F] | cpu.GPR[(cpu.inst>>RT)&0x1F]);
    }

    static void r4300i_SPECIAL_SLT(Cpu cpu) {
        if (cpu.GPR[(cpu.inst>>RS)&0x1F] < cpu.GPR[(cpu.inst>>RT)&0x1F]) // signed comparison
            cpu.GPR[(cpu.inst>>RD)&0x1F] = 1L;
        else
            cpu.GPR[(cpu.inst>>RD)&0x1F] = 0L;
    }

    static void r4300i_SPECIAL_SLTU(Cpu cpu) {
        if (cpu.compareUnsignedLongs(cpu.GPR[(cpu.inst>>RS)&0x1F], cpu.GPR[(cpu.inst>>RT)&0x1F]) < 0)
            cpu.GPR[(cpu.inst>>RD)&0x1F] = 1L;
        else
            cpu.GPR[(cpu.inst>>RD)&0x1F] = 0L;
    }

    static void r4300i_SPECIAL_DADD(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] + cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_DADDU(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] + cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_DSUB(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] - cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_DSUBU(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RS)&0x1F] - cpu.GPR[(cpu.inst>>RT)&0x1F];
    }

    static void r4300i_SPECIAL_TEQ(Cpu cpu) {


    }

    static void r4300i_SPECIAL_DSLL(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] << ((cpu.inst>>SA)&0x1F);
    }

    static void r4300i_SPECIAL_DSRL(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] >>> ((cpu.inst>>SA)&0x1F);
    }

    static void r4300i_SPECIAL_DSRA(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] >> ((cpu.inst>>SA)&0x1F);
    }

    static void r4300i_SPECIAL_DSLL32(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] << (((cpu.inst>>SA)&0x1F) + 32);
    }

    static void r4300i_SPECIAL_DSRL32(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] >>> (((cpu.inst>>SA)&0x1F) + 32);
    }

    static void r4300i_SPECIAL_DSRA32(Cpu cpu) {
        cpu.GPR[(cpu.inst>>RD)&0x1F] = cpu.GPR[(cpu.inst>>RT)&0x1F] >> (((cpu.inst>>SA)&0x1F) + 32);
    }
}
