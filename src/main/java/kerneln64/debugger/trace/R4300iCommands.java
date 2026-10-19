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


/*
 * Project 64 - A Nintendo 64 emulator.
 *
 * (c) Copyright 2001 zilmar (zilmar@emulation64.com) and
 * Jabo (jabo@emulation64.com).
 *
 * pj64 homepage: www.pj64.net
 *
 * Permission to use, copy, modify and distribute Project64 in both binary and
 * source form, for non-commercial purposes, is hereby granted without fee,
 * providing that this license information and copyright notice appear with
 * all copies and any derived work.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event shall the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Project64 is freeware for PERSONAL USE only. Commercial users should
 * seek permission of the copyright holders first. Commercial use includes
 * charging money for Project64 or software derived from Project64.
 *
 * The copyright holders request that bug fixes and improvements to the code
 * should be forwarded to them so if they want them.
 *
 */

package kerneln64.debugger.trace;
import kerneln64.cpu.Cop1;
import kerneln64.Main;


import kerneln64.cpu.Cpu;


public class R4300iCommands {


//        //R4300i OpCodes
//        public static final int R4300i_SPECIAL=                     0;
//        public static final int R4300i_REGIMM=                      1;
//        public static final int R4300i_J=				2;
//        public static final int R4300i_JAL=				3;
//        public static final int R4300i_BEQ=				4;
//        public static final int R4300i_BNE=				5;
//        public static final int R4300i_BLEZ=			6;
//        public static final int R4300i_BGTZ=			7;
//        public static final int R4300i_ADDI=			8;
//        public static final int R4300i_ADDIU=                       9;
//        public static final int R4300i_SLTI=			10;
//        public static final int R4300i_SLTIU=                       11;
//        public static final int R4300i_ANDI=			12;
//        public static final int R4300i_ORI=				13;
//        public static final int R4300i_XORI=			14;
//        public static final int R4300i_LUI=				15;
//        public static final int R4300i_CP0=				16;
//        public static final int R4300i_CP1=				17;
//        public static final int R4300i_BEQL=			20;
//        public static final int R4300i_BNEL=			21;
//        public static final int R4300i_BLEZL=                       22;
//        public static final int R4300i_BGTZL=                       23;
//        public static final int R4300i_DADDI=                       24;
//        public static final int R4300i_DADDIU=                      25;
//        public static final int R4300i_LDL=				26;
//        public static final int R4300i_LDR=				27;
//        public static final int R4300i_LB=				32;
//        public static final int R4300i_LH=				33;
//        public static final int R4300i_LWL=				34;
//        public static final int R4300i_LW=				35;
//        public static final int R4300i_LBU=				36;
//        public static final int R4300i_LHU=				37;
//        public static final int R4300i_LWR=				38;
//        public static final int R4300i_LWU=				39;
//        public static final int R4300i_SB=				40;
//        public static final int R4300i_SH=				41;
//        public static final int R4300i_SWL=				42;
//        public static final int R4300i_SW=				43;
//        public static final int R4300i_SDL=				44;
//        public static final int R4300i_SDR=				45;
//        public static final int R4300i_SWR=				46;
//        public static final int R4300i_CACHE=                       47;
//        public static final int R4300i_LL=				48;
//        public static final int R4300i_LWC1=                        49;
//        public static final int R4300i_LWC2=			0x32;
//        public static final int R4300i_LLD=				0x34;
//        public static final int R4300i_LDC1=			53;
//        public static final int R4300i_LDC2=			0x36;
//        public static final int R4300i_LD=				55;
//        public static final int R4300i_SC=				0x38;
//        public static final int R4300i_SWC1=			57;
//        public static final int R4300i_SWC2=			0x3A;
//        public static final int R4300i_SCD=				0x3C;
//        public static final int R4300i_SDC1=			61;
//        public static final int R4300i_SDC2=			62;
//        public static final int R4300i_SD=				63;
//        /* R4300i Special opcodes */
//        public static final int R4300i_SPECIAL_SLL=			0;
//        public static final int R4300i_SPECIAL_SRL=			2;
//        public static final int R4300i_SPECIAL_SRA=			3;
//        public static final int R4300i_SPECIAL_SLLV=                4;
//        public static final int R4300i_SPECIAL_SRLV=                6;
//        public static final int R4300i_SPECIAL_SRAV=                7;
//        public static final int R4300i_SPECIAL_JR=			8;
//        public static final int R4300i_SPECIAL_JALR=		9;
//        public static final int R4300i_SPECIAL_SYSCALL=             12;
//        public static final int R4300i_SPECIAL_BREAK=               13;
//        public static final int R4300i_SPECIAL_SYNC=		15;
//        public static final int R4300i_SPECIAL_MFHI=		16;
//        public static final int R4300i_SPECIAL_MTHI=		17;
//        public static final int R4300i_SPECIAL_MFLO=		18;
//        public static final int R4300i_SPECIAL_MTLO=		19;
//        public static final int R4300i_SPECIAL_DSLLV=               20;
//        public static final int R4300i_SPECIAL_DSRLV=               22;
//        public static final int R4300i_SPECIAL_DSRAV=               23;
//        public static final int R4300i_SPECIAL_MULT=		24;
//        public static final int R4300i_SPECIAL_MULTU=               25;
//        public static final int R4300i_SPECIAL_DIV=			26;
//        public static final int R4300i_SPECIAL_DIVU=		27;
//        public static final int R4300i_SPECIAL_DMULT=               28;
//        public static final int R4300i_SPECIAL_DMULTU=              29;
//        public static final int R4300i_SPECIAL_DDIV=                30;
//        public static final int R4300i_SPECIAL_DDIVU=               31;
//        public static final int R4300i_SPECIAL_ADD=			32;
//        public static final int R4300i_SPECIAL_ADDU=                33;
//        public static final int R4300i_SPECIAL_SUB=			34;
//        public static final int R4300i_SPECIAL_SUBU=		35;
//        public static final int R4300i_SPECIAL_AND=			36;
//        public static final int R4300i_SPECIAL_OR=			37;
//        public static final int R4300i_SPECIAL_XOR=			38;
//        public static final int R4300i_SPECIAL_NOR=			39;
//        public static final int R4300i_SPECIAL_SLT=			42;
//        public static final int R4300i_SPECIAL_SLTU=		43;
//        public static final int R4300i_SPECIAL_DADD=		44;
//        public static final int R4300i_SPECIAL_DADDU=               45;
//        public static final int R4300i_SPECIAL_DSUB=		46;
//        public static final int R4300i_SPECIAL_DSUBU=               47;
//        public static final int R4300i_SPECIAL_TGE=			48;
//        public static final int R4300i_SPECIAL_TGEU=		49;
//        public static final int R4300i_SPECIAL_TLT=			50;
//        public static final int R4300i_SPECIAL_TLTU=		51;
//        public static final int R4300i_SPECIAL_TEQ=			52;
//        public static final int R4300i_SPECIAL_TNE=			54;
//        public static final int R4300i_SPECIAL_DSLL=		56;
//        public static final int R4300i_SPECIAL_DSRL=		58;
//        public static final int R4300i_SPECIAL_DSRA=		59;
//        public static final int R4300i_SPECIAL_DSLL32=              60;
//        public static final int R4300i_SPECIAL_DSRL32=              62;
//        public static final int R4300i_SPECIAL_DSRA32=              63;
//        /* R4300i RegImm opcodes */
//        public static final int R4300i_REGIMM_BLTZ=			0;
//        public static final int R4300i_REGIMM_BGEZ=			1;
//        public static final int R4300i_REGIMM_BLTZL=		2;
//        public static final int R4300i_REGIMM_BGEZL=		3;
//        public static final int R4300i_REGIMM_TGEI=			0x08;
//        public static final int R4300i_REGIMM_TGEIU=		0x09;
//        public static final int R4300i_REGIMM_TLTI=			0x0A;
//        public static final int R4300i_REGIMM_TLTIU=		0x0B;
//        public static final int R4300i_REGIMM_TEQI=			0x0C;
//        public static final int R4300i_REGIMM_TNEI=			0x0E;
//        public static final int R4300i_REGIMM_BLTZAL=               0x10;
//        public static final int R4300i_REGIMM_BGEZAL=               17;
//        public static final int R4300i_REGIMM_BLTZALL=              0x12;
//        public static final int R4300i_REGIMM_BGEZALL=              0x13;
//        /* R4300i COP0 opcodes */
//        public static final int R4300i_COP0_MF=			0;
//        public static final int R4300i_COP0_MT=			4;
//        /* R4300i COP0 CO opcodes */
//        public static final int R4300i_COP0_CO_TLBR=		1;
//        public static final int R4300i_COP0_CO_TLBWI=               2;
//        public static final int R4300i_COP0_CO_TLBWR=               6;
//        public static final int R4300i_COP0_CO_TLBP=		8;
//        public static final int R4300i_COP0_CO_ERET=		24;
//        /* R4300i COP1 opcodes */
//        public static final int R4300i_COP1_MF=			0;
//        public static final int R4300i_COP1_DMF=			1;
//        public static final int R4300i_COP1_CF=			2;
//        public static final int R4300i_COP1_MT=			4;
//        public static final int R4300i_COP1_DMT=			5;
//        public static final int R4300i_COP1_CT=			6;
//        public static final int R4300i_COP1_BC=			8;
//        public static final int R4300i_COP1_S=			16;
//        public static final int R4300i_COP1_D=			17;
//        public static final int R4300i_COP1_W=			20;
//        public static final int R4300i_COP1_L=			21;
//        /* R4300i COP1 BC opcodes */
//        public static final int R4300i_COP1_BC_BCF=			0;
//        public static final int R4300i_COP1_BC_BCT=			1;
//        public static final int R4300i_COP1_BC_BCFL=		2;
//        public static final int R4300i_COP1_BC_BCTL=		3;
//        public static final int R4300i_COP1_FUNCT_ADD=              0;
//        public static final int R4300i_COP1_FUNCT_SUB=              1;
//        public static final int R4300i_COP1_FUNCT_MUL=              2;
//        public static final int R4300i_COP1_FUNCT_DIV=              3;
//        public static final int R4300i_COP1_FUNCT_SQRT=             4;
//        public static final int R4300i_COP1_FUNCT_ABS=              5;
//        public static final int R4300i_COP1_FUNCT_MOV=              6;
//        public static final int R4300i_COP1_FUNCT_NEG=              7;
//        public static final int R4300i_COP1_FUNCT_ROUND_L=          8;
//        public static final int R4300i_COP1_FUNCT_TRUNC_L=          9;
//        public static final int R4300i_COP1_FUNCT_CEIL_L=           10;
//        public static final int R4300i_COP1_FUNCT_FLOOR_L=          11;
//        public static final int R4300i_COP1_FUNCT_ROUND_W=          12;
//        public static final int R4300i_COP1_FUNCT_TRUNC_W=          13;
//        public static final int R4300i_COP1_FUNCT_CEIL_W=           14;
//        public static final int R4300i_COP1_FUNCT_FLOOR_W=          15;
//        public static final int R4300i_COP1_FUNCT_CVT_S=		32;
//        public static final int R4300i_COP1_FUNCT_CVT_D=		33;
//        public static final int R4300i_COP1_FUNCT_CVT_W=		36;
//        public static final int R4300i_COP1_FUNCT_CVT_L=		37;
//        public static final int R4300i_COP1_FUNCT_C_F=              48;
//        public static final int R4300i_COP1_FUNCT_C_UN=             49;
//        public static final int R4300i_COP1_FUNCT_C_EQ=             50;
//        public static final int R4300i_COP1_FUNCT_C_UEQ=		51;
//        public static final int R4300i_COP1_FUNCT_C_OLT=		52;
//        public static final int R4300i_COP1_FUNCT_C_ULT=		53;
//        public static final int R4300i_COP1_FUNCT_C_OLE=		54;
//        public static final int R4300i_COP1_FUNCT_C_ULE=		55;
//        public static final int R4300i_COP1_FUNCT_C_SF=             56;
//        public static final int R4300i_COP1_FUNCT_C_NGLE=           57;
//        public static final int R4300i_COP1_FUNCT_C_SEQ=		58;
//        public static final int R4300i_COP1_FUNCT_C_NGL=		59;
//        public static final int R4300i_COP1_FUNCT_C_LT=             60;
//        public static final int R4300i_COP1_FUNCT_C_NGE=		61;
//        public static final int R4300i_COP1_FUNCT_C_LE=             62;
//        public static final int R4300i_COP1_FUNCT_C_NGT=		63;
        
//        public int op; // Cpu
//        public int target; // Cpu
//        public int rs; // Cpu
//        public int base; // Cpu
//        public int fmt; // Cop1
//        public int rt; // Cpu
//        public int ft; // Cop1
//        public int immediate; // Cpu
//        public int offset; // Cpu
//        public int rd; // Cpu
//        public int fs; // Cop1
//        public int sa; // Cpu
//        public int fd; // Cop1
//        public int funct; // Cpu, Cop1


    private static int op; // Cpu
    private static int target; // Cpu
    private static int rs; // Cpu
    private static int base; // Cpu
    private static int fmt; // Cop1
    private static int rt; // Cpu
    private static int ft; // Cop1
    private static int immediate; // Cpu
    private static int offset; // Cpu
    private static int rd; // Cpu
    private static int fs; // Cop1
    private static int sa; // Cpu
    private static int fd; // Cop1
    private static int funct; // Cpu, Cop1
    
    /** Creates a new instance of R4300iCommands */
    public R4300iCommands() {
    }
    
    // called by Main
    public static String R4300iOpcodeName(int opCode, int pc) {

        setHex(opCode);
        
        switch (op) {
            case Cpu.R4300i_SPECIAL:
                return R4300iSpecialName( opCode, pc );
            case Cpu.R4300i_REGIMM:
                return R4300iRegImmName( opCode, pc );
            case Cpu.R4300i_J:
                return "J";
            case Cpu.R4300i_JAL:
                return "JAL";
            case Cpu.R4300i_BEQ:
                return "BEQ";
            case Cpu.R4300i_BNE:
                return "BNE";
            case Cpu.R4300i_BLEZ:
                return "BLEZ";
            case Cpu.R4300i_BGTZ:
                return "BGTZ";
            case Cpu.R4300i_ADDI:
                return "ADDI";
            case Cpu.R4300i_ADDIU:
                return "ADDIU";
            case Cpu.R4300i_SLTI:
                return "SLTI";
            case Cpu.R4300i_SLTIU:
                return "SLTIU";
            case Cpu.R4300i_ANDI:
                return "ANDI";
            case Cpu.R4300i_ORI:
                return "ORI";
            case Cpu.R4300i_XORI:
                return "XORI";
            case Cpu.R4300i_LUI:
                return "LUI";
            case Cpu.R4300i_CP0:
                switch (rs) {
                    case Cpu.R4300i_COP0_MF:
                        return "COP0_MF";
                    case Cpu.R4300i_COP0_MT:
                        return "COP0_MT";
                    default:
                        if ( (rs & 0x10 ) != 0 ) {
                            switch( funct ) {
                                case Cpu.R4300i_COP0_CO_TLBR:
                                    return "COP0_C0_TLBR";
                                case Cpu.R4300i_COP0_CO_TLBWI:
                                    return "COP0_C0_TLBWI";
                                case Cpu.R4300i_COP0_CO_TLBWR:
                                    return "COP0_C0_TLBWR";
                                case Cpu.R4300i_COP0_CO_TLBP:
                                    return "COP0_C0_TLBP";
                                case Cpu.R4300i_COP0_CO_ERET:
                                    return "COP0_C0_ERET";
                                default:
                                    return "Unknown COP0_C0: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
                            }
                        } else {
                            return "Unknown COP0: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
                        }
                }
            case Cpu.R4300i_CP1:
                return R4300iCop1Name( opCode, pc );
            case Cpu.R4300i_BEQL:
                return "BEQL";
            case Cpu.R4300i_BNEL:
                return "BNEL";
            case Cpu.R4300i_BLEZL:
                return "BLEZL";
            case Cpu.R4300i_BGTZL:
                return "BGTZL";
            case Cpu.R4300i_DADDI:
                return "DADDI";
            case Cpu.R4300i_DADDIU:
                return "DADDIU";
            case Cpu.R4300i_LDL:
                return "LDL";
            case Cpu.R4300i_LDR:
                return "LDR";
            case Cpu.R4300i_LB:
                return "LB";
            case Cpu.R4300i_LH:
                return "LH";
            case Cpu.R4300i_LWL:
                return "LWL";
            case Cpu.R4300i_LW:
                return "LW";
            case Cpu.R4300i_LBU:
                return "LBU";
            case Cpu.R4300i_LHU:
                return "LHU";
            case Cpu.R4300i_LWR:
                return "LWR";
            case Cpu.R4300i_LWU:
                return "LWU";
            case Cpu.R4300i_SB:
                return "SB";
            case Cpu.R4300i_SH:
                return "SH";
            case Cpu.R4300i_SWL:
                return "SWL";
            case Cpu.R4300i_SW:
                return "SW";
            case Cpu.R4300i_SDL:
                return "SDL";
            case Cpu.R4300i_SDR:
                return "SDR";
            case Cpu.R4300i_SWR:
                return "SWR";
            case Cpu.R4300i_CACHE:
                return "CACHE";
            case Cpu.R4300i_LL:
                return "LL";
            case Cpu.R4300i_LWC1:
                return "LWC1";
            case Cpu.R4300i_LDC1:
                return "LDC1";
            case Cpu.R4300i_LD:
                return "LD";
            case Cpu.R4300i_SC:
                return "SC";
            case Cpu.R4300i_SWC1:
                return "SWC1";
            case Cpu.R4300i_SDC1:
                return "SDC1";
            case Cpu.R4300i_SD:
                return "SD";
            default:
                return "Unknown: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
        }
    }
    
    private static String R4300iRegImmName(int opCode, int PC) {

        
        switch (rt) {
            case Cpu.R4300i_REGIMM_BLTZ:
                return "REGIMM_BLTZ";
            case Cpu.R4300i_REGIMM_BGEZ:
                return "REGIMM_BGEZ";
            case Cpu.R4300i_REGIMM_BLTZL:
                return "REGIMM_BLTZL";
            case Cpu.R4300i_REGIMM_BGEZL:
                return "REGIMM_BGEZL";
            case Cpu.R4300i_REGIMM_TGEI:
                return "REGIMM_TGEI";
            case Cpu.R4300i_REGIMM_TGEIU:
                return "REGIMM_TGEIU";
            case Cpu.R4300i_REGIMM_TLTI:
                return "REGIMM_TLTI";
            case Cpu.R4300i_REGIMM_TLTIU:
                return "REGIMM_TLTIU";
            case Cpu.R4300i_REGIMM_TEQI:
                return "REGIMM_TEQI";
            case Cpu.R4300i_REGIMM_TNEI:
                return "REGIMM_TNEI";
            case Cpu.R4300i_REGIMM_BLTZAL:
                return "REGIMM_BLTZAL";
            case Cpu.R4300i_REGIMM_BGEZAL:
                return "REGIMM_ BGEZAL";
            case Cpu.R4300i_REGIMM_BLTZALL:
                return "REGIMM_BLTZALL";
            case Cpu.R4300i_REGIMM_BGEZALL:
                return "REGIMM_BGEZALL";
            default:
                return "Unknown REGIMM: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
        }
    }
    
    private static String R4300iSpecialName(int opCode, int PC) {

        
        switch (funct) {
            case Cpu.R4300i_SPECIAL_SLL:
                if (opCode != 0) {
                    return "SPECIAL_SLL";
                } else {
                    return "SPECIAL_NOP";
                }
            case Cpu.R4300i_SPECIAL_SRL:
                return "SPECIAL_SRL";
            case Cpu.R4300i_SPECIAL_SRA:
                return "SPECIAL_SRA";
            case Cpu.R4300i_SPECIAL_SLLV:
                return "SPECIAL_SLLV";
            case Cpu.R4300i_SPECIAL_SRLV:
                return "SPECIAL_SRLV";
            case Cpu.R4300i_SPECIAL_SRAV:
                return "SPECIAL_SRAV";
            case Cpu.R4300i_SPECIAL_JR:
                return "SPECIAL_JR";
            case Cpu.R4300i_SPECIAL_JALR:
                return "SPECIAL_JALR";
            case Cpu.R4300i_SPECIAL_SYSCALL:
                return "SPECIAL_SYSCALL";
            case Cpu.R4300i_SPECIAL_BREAK:
                return "SPECIAL_BREAK";
            case Cpu.R4300i_SPECIAL_SYNC:
                return "SPECIAL_SYNC";
            case Cpu.R4300i_SPECIAL_MFHI:
                return "SPECIAL_MFHI";
            case Cpu.R4300i_SPECIAL_MTHI:
                return "SPECIAL_MTHI";
            case Cpu.R4300i_SPECIAL_MFLO:
                return "SPECIAL_MFLO";
            case Cpu.R4300i_SPECIAL_MTLO:
                return "SPECIAL_MTLO";
            case Cpu.R4300i_SPECIAL_DSLLV:
                return "SPECIAL_DSLLV";
            case Cpu.R4300i_SPECIAL_DSRLV:
                return "SPECIAL_DSRLV";
            case Cpu.R4300i_SPECIAL_DSRAV:
                return "SPECIAL_DSRAV";
            case Cpu.R4300i_SPECIAL_MULT:
                return "SPECIAL_MULT";
            case Cpu.R4300i_SPECIAL_MULTU:
                return "SPECIAL_MULTU";
            case Cpu.R4300i_SPECIAL_DIV:
                return "SPECIAL_DIV";
            case Cpu.R4300i_SPECIAL_DIVU:
                return "SPECIAL_DIVU";
            case Cpu.R4300i_SPECIAL_DMULT:
                return "SPECIAL_DMULT";
            case Cpu.R4300i_SPECIAL_DMULTU:
                return "SPECIAL_DMULTU";
            case Cpu.R4300i_SPECIAL_DDIV:
                return "SPECIAL_DDIV";
            case Cpu.R4300i_SPECIAL_DDIVU:
                return "SPECIAL_DDIVU";
            case Cpu.R4300i_SPECIAL_ADD:
                return "SPECIAL_ADD";
            case Cpu.R4300i_SPECIAL_ADDU:
                return "SPECIAL_ADDU";
            case Cpu.R4300i_SPECIAL_SUB:
                return "SPECIAL_SUB";
            case Cpu.R4300i_SPECIAL_SUBU:
                return "SPECIAL_SUBU";
            case Cpu.R4300i_SPECIAL_AND:
                return "SPECIAL_AND";
            case Cpu.R4300i_SPECIAL_OR:
                return "SPECIAL_OR";
            case Cpu.R4300i_SPECIAL_XOR:
                return "SPECIAL_XOR";
            case Cpu.R4300i_SPECIAL_NOR:
                return "SPECIAL_NOR";
            case Cpu.R4300i_SPECIAL_SLT:
                return "SPECIAL_SLT";
            case Cpu.R4300i_SPECIAL_SLTU:
                return "SPECIAL_SLTU";
            case Cpu.R4300i_SPECIAL_DADD:
                return "SPECIAL_DADD";
            case Cpu.R4300i_SPECIAL_DADDU:
                return "SPECIAL_DADDU";
            case Cpu.R4300i_SPECIAL_DSUB:
                return "SPECIAL_DSUB";
            case Cpu.R4300i_SPECIAL_DSUBU:
                return "SPECIAL_DSUBU";
            case Cpu.R4300i_SPECIAL_TGE:
                return "SPECIAL_TGE";
            case Cpu.R4300i_SPECIAL_TGEU:
                return "SPECIAL_TGEU";
            case Cpu.R4300i_SPECIAL_TLT:
                return "SPECIAL_TLT";
            case Cpu.R4300i_SPECIAL_TLTU:
                return "SPECIAL_TLTU";
            case Cpu.R4300i_SPECIAL_TEQ:
                return "SPECIAL_TEQ";
            case Cpu.R4300i_SPECIAL_TNE:
                return "SPECIAL_TNE";
            case Cpu.R4300i_SPECIAL_DSLL:
                return "SPECIAL_DSLL";
            case Cpu.R4300i_SPECIAL_DSRL:
                return "SPECIAL_DSRL";
            case Cpu.R4300i_SPECIAL_DSRA:
                return "SPECIAL_DSRA";
            case Cpu.R4300i_SPECIAL_DSLL32:
                return "SPECIAL_DSLL32";
            case Cpu.R4300i_SPECIAL_DSRL32:
                return "SPECIAL_DSRL32";
            case Cpu.R4300i_SPECIAL_DSRA32:
                return "SPECIAL_DSRA32";
            default:
                return "Unknown SPECIAL: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
        }
    }
    
    private static String R4300iCop1Name(int opCode, int PC) {

        
        switch (fmt) {
            case Cpu.R4300i_COP1_MF:
                return "COP1_MF";
            case Cpu.R4300i_COP1_DMF:
                return "COP1_DMF";
            case Cpu.R4300i_COP1_CF:
                return "COP1_CF";
            case Cpu.R4300i_COP1_MT:
                return "COP1_MT";
            case Cpu.R4300i_COP1_DMT:
                return "COP1_DMT";
            case Cpu.R4300i_COP1_CT:
                return "COP1_CT";
            case Cpu.R4300i_COP1_BC:
                switch (ft) {
                    case Cpu.R4300i_COP1_BC_BCF:
                        return "COP1_BC_BCF";
                    case Cpu.R4300i_COP1_BC_BCT:
                        return "COP1_BC_BCT";
                    case Cpu.R4300i_COP1_BC_BCFL:
                        return "COP1_BC_BCFL";
                    case Cpu.R4300i_COP1_BC_BCTL:
                        return "COP1_BC_BCTL";
                    default:
                        return "Unknown COP1_BC: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
                }
            case Cpu.R4300i_COP1_S:
            case Cpu.R4300i_COP1_D:
            case Cpu.R4300i_COP1_W:
            case Cpu.R4300i_COP1_L:
                switch (funct) {
                    case Cpu.R4300i_COP1_FUNCT_ADD:
                        return "COP1_FUNCT_ADD." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_SUB:
                        return "COP1_FUNCT_SUB." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_MUL:
                        return "COP1_FUNCT_MUL." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_DIV:
                        return "COP1_FUNCT_DIV." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_SQRT:
                        return "COP1_FUNCT_SQRT." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_ABS:
                        return "COP1_FUNCT_ABS." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_MOV:
                        return "COP1_FUNCT_MOV." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_NEG:
                        return "COP1_FUNCT_NEG." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_ROUND_L:
                        return "COP1_FUNCT_ROUND_L." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_TRUNC_L:
                        return "COP1_FUNCT_TRUNC_L." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_CEIL_L:
                        return "COP1_FUNCT_CEIL_L." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_FLOOR_L:
                        return "COP1_FUNCT_FLOOR_L." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_ROUND_W:
                        return "COP1_FUNCT_ROUND_W." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_TRUNC_W:
                        return "COP1_FUNCT_TRUNC_W." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_CEIL_W:
                        return "COP1_FUNCT_CEIL_W." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_FLOOR_W:
                        return "COP1_FUNCT_FLOOR_W." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_CVT_S:
                        return "COP1_FUNCT_CVT_S." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_CVT_D:
                        return "COP1_FUNCT_CVT_D." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_CVT_W:
                        return "COP1_FUNCT_CVT_W." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_CVT_L:
                        return "COP1_FUNCT_CVT_L." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_F:
                        return "COP1_FUNCT_C_F." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_UN:
                        return "COP1_FUNCT_C_UN." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_EQ:
                        return "COP1_FUNCT_C_EQ." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_UEQ:
                        return "COP1_FUNCT_C_UEQ." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_OLT:
                        return "COP1_FUNCT_C_OLT." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_ULT:
                        return "COP1_FUNCT_C_ULT." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_OLE:
                        return "COP1_FUNCT_C_OLE." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_ULE:
                        return "COP1_FUNCT_C_ULE." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_SF:
                        return "COP1_FUNCT_C_SF." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_NGLE:
                        return "COP1_FUNCT_C_NGLE." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_SEQ:
                        return "COP1_FUNCT_C_SEQ." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_NGL:
                        return "COP1_FUNCT_C_NGL." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_LT:
                        return "COP1_FUNCT_C_LT." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_NGE:
                        return "COP1_FUNCT_C_NGE." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_LE:
                        return "COP1_FUNCT_C_LE." + FPR_Type(fmt);
                    case Cpu.R4300i_COP1_FUNCT_C_NGT:
                        return "COP1_FUNCT_C_NGT." + FPR_Type(fmt);
                    default:
                        return "Unknown COP1_FUNCT: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
                }
            default:
                return "Unknown COP1: " + Integer.toHexString(opCode) + " (" + Integer.toBinaryString(opCode) + ")";
        }
    }
    
    private static char FPR_Type(int Reg) {
        return (Reg==Cpu.R4300i_COP1_S?'S':Reg==Cpu.R4300i_COP1_D?'D':Reg==Cpu.R4300i_COP1_W?'W':'L');
    }
    
    private static void setHex(int hex) {
            op = (hex>>26)&0x3F;
            target = (hex)&0x3FFFFFF;
            rs = base = fmt = (hex>>21)&0x1F;
            rt = ft = (hex>>16)&0x1F;
            immediate = offset = (hex)&0xFFFF;
            rd = fs = (hex>>11)&0x1F;
            sa = fd = (hex>>6)&0x1F;
            funct = (hex)&0x3F;
        }
    
}
