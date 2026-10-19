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


package kerneln64.debugger.trace;
import kerneln64.cpu.CpuDebugBranches;
import kerneln64.cpu.CpuDebugCoprocessors;
import kerneln64.cpu.CpuDebugInteger;
import kerneln64.cpu.CpuDebugMemory;
import kerneln64.cpu.CpuDebugOpcodeTable;
import kerneln64.Main;


import kerneln64.cpu.Cpu;
import kerneln64.Main;
import kerneln64.memory.Memory;
import kerneln64.cpu.Registers;
import static kerneln64.cpu.Cop1.FSTATUS_REGISTER;


public class DebugCpu extends Cpu {
    
    public String getITypeDebug(String name, long rs, long rt, int immediate) {
        return String.format("%X:%s:%d:%d:%X %X:%X:%X ", pc, name, rs, rt, immediate, rs, rt, immediate);
    }
    
    public String getRTypeDebug(String name, long rs, long rt, long rd) {
        return String.format("%X:%s:%d:%d:%d:%d %X:%X:%X ", pc, name, rs, rt, rd, sa, rs, rt, rd);
    }
    
    public String getJTypeDebug(String name, int target) {
        return String.format("%X:%s:%X %X ", pc, name, target, target);
    }
    
    /** Creates a new instance of InterpreterOps */
    public DebugCpu(Registers regs, Memory mem) {
        super(regs, mem);
    }
    
    public void printCpuState() {
        System.out.printf("CPU State:\n");
        System.out.printf("PC=%x\n", pc);
        for (int j=0; j<16; j++)
            System.out.printf("reg[%2d]:%8x%8x        reg[%d]:%8x%8x\n",
                    j,
                    (int)(GPR[j] >> 32),
                    (int)(GPR[j]),
                    j+16,
                    (int)(GPR[j+16] >> 32),
                    (int)(GPR[j+16]));
        System.out.printf("hi:%8x%8x        lo:%8x%8x\n",
                (int)(HI >> 32),
                (int)(HI),
                (int)(LO >> 32),
                (int)(LO));
    }
    
    /************************* OpCode functions *************************/
    public Runnable r4300i_J_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_J_Debug(this);
    
    public Runnable r4300i_JAL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_JAL_Debug(this);
    
    public Runnable r4300i_BEQ_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BEQ_Debug(this);
    
    public Runnable r4300i_BNE_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BNE_Debug(this);
    
    public Runnable r4300i_BLEZ_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BLEZ_Debug(this);
    
    public Runnable r4300i_BGTZ_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BGTZ_Debug(this);
    
    public Runnable r4300i_ADDI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_ADDI_Debug(this);
    
    public Runnable r4300i_ADDIU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_ADDIU_Debug(this);
    
    public Runnable r4300i_SLTI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SLTI_Debug(this);
    
    public Runnable r4300i_SLTIU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SLTIU_Debug(this);
    
    public Runnable r4300i_ANDI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_ANDI_Debug(this);
    
    public Runnable r4300i_ORI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_ORI_Debug(this);
    
    public Runnable r4300i_XORI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_XORI_Debug(this);
    
    public Runnable r4300i_LUI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_LUI_Debug(this);
    
    public Runnable r4300i_BEQL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BEQL_Debug(this);
    
    public Runnable r4300i_BNEL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BNEL_Debug(this);
    
    public Runnable r4300i_BLEZL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BLEZL_Debug(this);
    
    public Runnable r4300i_BGTZL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_BGTZL_Debug(this);
    
    public Runnable r4300i_DADDIU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_DADDIU_Debug(this);
    
    public Runnable r4300i_LDL_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LDL_Debug(this);
    
    public Runnable r4300i_LDR_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LDR_Debug(this);
    
    public Runnable r4300i_LB_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LB_Debug(this);
    
    public Runnable r4300i_LH_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LH_Debug(this);
    
    public Runnable r4300i_LWL_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LWL_Debug(this);
    
    public Runnable r4300i_LW_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LW_Debug(this);
    
    public Runnable r4300i_LBU_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LBU_Debug(this);
    
    public Runnable r4300i_LHU_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LHU_Debug(this);
    
    public Runnable r4300i_LWR_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LWR_Debug(this);
    
    public Runnable r4300i_LWU_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LWU_Debug(this);
    
    public Runnable r4300i_SB_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SB_Debug(this);
    
    public Runnable r4300i_SH_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SH_Debug(this);
    
    public Runnable r4300i_SWL_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SWL_Debug(this);
    
    public Runnable r4300i_SW_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SW_Debug(this);
    
    public Runnable r4300i_SDL_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SDL_Debug(this);
    
    public Runnable r4300i_SDR_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SDR_Debug(this);
    
    public Runnable r4300i_SWR_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SWR_Debug(this);
    
    public Runnable r4300i_CACHE_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_CACHE_Debug(this);
    
    public Runnable r4300i_LL_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LL_Debug(this);
    
    public Runnable r4300i_LWC1_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LWC1_Debug(this);
    
    public Runnable r4300i_SC_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SC_Debug(this);
    
    public Runnable r4300i_LD_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LD_Debug(this);
    
    public Runnable r4300i_LDC1_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_LDC1_Debug(this);
    
    public Runnable r4300i_SWC1_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SWC1_Debug(this);
    
    public Runnable r4300i_SDC1_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SDC1_Debug(this);
    
    public Runnable r4300i_SD_Debug = () -> kerneln64.cpu.CpuDebugMemory.r4300i_SD_Debug(this);
    
    /********************** R4300i OpCodes: Special **********************/
    public Runnable r4300i_SPECIAL_SLL_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SLL_Debug(this);
    
    public Runnable r4300i_SPECIAL_SRL_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SRL_Debug(this);
    
    public Runnable r4300i_SPECIAL_SRA_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SRA_Debug(this);
    
    public Runnable r4300i_SPECIAL_SLLV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SLLV_Debug(this);
    
    public Runnable r4300i_SPECIAL_SRLV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SRLV_Debug(this);
    
    public Runnable r4300i_SPECIAL_SRAV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SRAV_Debug(this);
    
    public Runnable r4300i_SPECIAL_JR_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_SPECIAL_JR_Debug(this);
    
    public Runnable r4300i_SPECIAL_JALR_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_SPECIAL_JALR_Debug(this);
    
    public Runnable r4300i_SPECIAL_SYSCALL_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SYSCALL_Debug(this);
    
    public Runnable r4300i_SPECIAL_BREAK_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_BREAK_Debug(this);
    
    public Runnable r4300i_SPECIAL_SYNC_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SYNC_Debug(this);
    
    public Runnable r4300i_SPECIAL_MFHI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_MFHI_Debug(this);
    
    public Runnable r4300i_SPECIAL_MTHI_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_MTHI_Debug(this);
    
    public Runnable r4300i_SPECIAL_MFLO_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_MFLO_Debug(this);
    
    public Runnable r4300i_SPECIAL_MTLO_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_MTLO_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSLLV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSLLV_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSRLV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSRLV_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSRAV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSRAV_Debug(this);
    
    public Runnable r4300i_SPECIAL_MULT_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_MULT_Debug(this);
    
    public Runnable r4300i_SPECIAL_MULTU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_MULTU_Debug(this);
    
    public Runnable r4300i_SPECIAL_DIV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DIV_Debug(this);
    
    public Runnable r4300i_SPECIAL_DIVU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DIVU_Debug(this);
    
    public Runnable r4300i_SPECIAL_DMULT_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DMULT_Debug(this);
    
    public Runnable r4300i_SPECIAL_DMULTU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DMULTU_Debug(this);
    
    public Runnable r4300i_SPECIAL_DDIV_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DDIV_Debug(this);
    
    public Runnable r4300i_SPECIAL_DDIVU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DDIVU_Debug(this);
    
    public Runnable r4300i_SPECIAL_ADD_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_ADD_Debug(this);
    
    public Runnable r4300i_SPECIAL_ADDU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_ADDU_Debug(this);
    
    public Runnable r4300i_SPECIAL_SUB_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SUB_Debug(this);
    
    public Runnable r4300i_SPECIAL_SUBU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SUBU_Debug(this);
    
    public Runnable r4300i_SPECIAL_AND_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_AND_Debug(this);
    
    public Runnable r4300i_SPECIAL_OR_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_OR_Debug(this);
    
    public Runnable r4300i_SPECIAL_XOR_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_XOR_Debug(this);
    
    public Runnable r4300i_SPECIAL_NOR_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_NOR_Debug(this);
    
    public Runnable r4300i_SPECIAL_SLT_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SLT_Debug(this);
    
    public Runnable r4300i_SPECIAL_SLTU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_SLTU_Debug(this);
    
    public Runnable r4300i_SPECIAL_DADD_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DADD_Debug(this);
    
    public Runnable r4300i_SPECIAL_DADDU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DADDU_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSUB_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSUB_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSUBU_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSUBU_Debug(this);
    
    public Runnable r4300i_SPECIAL_TEQ_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_TEQ_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSLL_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSLL_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSRL_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSRL_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSRA_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSRA_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSLL32_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSLL32_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSRL32_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSRL32_Debug(this);
    
    public Runnable r4300i_SPECIAL_DSRA32_Debug = () -> kerneln64.cpu.CpuDebugInteger.r4300i_SPECIAL_DSRA32_Debug(this);
    
    /********************** R4300i OpCodes: RegImm **********************/
    public Runnable r4300i_REGIMM_BLTZ_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_REGIMM_BLTZ_Debug(this);
    
    public Runnable r4300i_REGIMM_BGEZ_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_REGIMM_BGEZ_Debug(this);
    
    public Runnable r4300i_REGIMM_BLTZL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_REGIMM_BLTZL_Debug(this);
    
    public Runnable r4300i_REGIMM_BGEZL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_REGIMM_BGEZL_Debug(this);
    
    public Runnable r4300i_REGIMM_BLTZAL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_REGIMM_BLTZAL_Debug(this);
    
    public Runnable r4300i_REGIMM_BGEZAL_Debug = () -> kerneln64.cpu.CpuDebugBranches.r4300i_REGIMM_BGEZAL_Debug(this);
    
    /************************** COP0 functions **************************/
    public Runnable r4300i_COP0_MF_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP0_MF_Debug(this);
    
    public Runnable r4300i_COP0_MT_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP0_MT_Debug(this);
    
    /************************** COP0 CO functions ***********************/
    public Runnable r4300i_COP0_CO_TLBR_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP0_CO_TLBR_Debug(this);
    
    public Runnable r4300i_COP0_CO_TLBWI_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP0_CO_TLBWI_Debug(this);
    
    public Runnable r4300i_COP0_CO_TLBWR_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP0_CO_TLBWR_Debug(this);
    
    public Runnable r4300i_COP0_CO_TLBP_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP0_CO_TLBP_Debug(this);
    
    public Runnable r4300i_COP0_CO_ERET_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP0_CO_ERET_Debug(this);
    
    /************************** COP1 functions **************************/
    public Runnable r4300i_COP1_MF_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_MF_Debug(this);
    
    public Runnable r4300i_COP1_DMF_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_DMF_Debug(this);
    
    public Runnable r4300i_COP1_CF_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_CF_Debug(this);
    
    public Runnable r4300i_COP1_MT_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_MT_Debug(this);
    
    public Runnable r4300i_COP1_DMT_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_DMT_Debug(this);
    
    public Runnable r4300i_COP1_CT_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_CT_Debug(this);
    
    /************************* COP1: BC1 functions ***********************/
    public Runnable r4300i_COP1_BCF_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_BCF_Debug(this);
    
    public Runnable r4300i_COP1_BCT_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_BCT_Debug(this);
    
    public Runnable r4300i_COP1_BCFL_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_BCFL_Debug(this);
    
    public Runnable r4300i_COP1_BCTL_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_BCTL_Debug(this);
    
    /************************** COP1: S functions ************************/
    public Runnable r4300i_COP1_S_ADD_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_ADD_Debug(this);
    
    public Runnable r4300i_COP1_S_SUB_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_SUB_Debug(this);
    
    public Runnable r4300i_COP1_S_MUL_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_MUL_Debug(this);
    
    public Runnable r4300i_COP1_S_DIV_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_DIV_Debug(this);
    
    public Runnable r4300i_COP1_S_SQRT_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_SQRT_Debug(this);
    
    public Runnable r4300i_COP1_S_ABS_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_ABS_Debug(this);
    
    public Runnable r4300i_COP1_S_MOV_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_MOV_Debug(this);
    
    public Runnable r4300i_COP1_S_NEG_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_NEG_Debug(this);
    
    public Runnable r4300i_COP1_S_TRUNC_L_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_TRUNC_L_Debug(this);
    
    public Runnable r4300i_COP1_S_ROUND_W_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_ROUND_W_Debug(this);
    
    public Runnable r4300i_COP1_S_TRUNC_W_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_TRUNC_W_Debug(this);
    
    public Runnable r4300i_COP1_S_FLOOR_W_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_FLOOR_W_Debug(this);
    
    public Runnable r4300i_COP1_S_CVT_D_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_CVT_D_Debug(this);
    
    public Runnable r4300i_COP1_S_CVT_W_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_CVT_W_Debug(this);
    
    public Runnable r4300i_COP1_S_CVT_L_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_CVT_L_Debug(this);
    
    public Runnable r4300i_COP1_S_CMP_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_S_CMP_Debug(this);
    
    /************************** COP1: D functions ************************/
    public Runnable r4300i_COP1_D_ADD_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_ADD_Debug(this);
    
    public Runnable r4300i_COP1_D_SUB_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_SUB_Debug(this);
    
    public Runnable r4300i_COP1_D_MUL_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_MUL_Debug(this);
    
    public Runnable r4300i_COP1_D_DIV_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_DIV_Debug(this);
    
    public Runnable r4300i_COP1_D_SQRT_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_SQRT_Debug(this);
    
    public Runnable r4300i_COP1_D_ABS_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_ABS_Debug(this);
    
    public Runnable r4300i_COP1_D_MOV_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_MOV_Debug(this);
    
    public Runnable r4300i_COP1_D_NEG_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_NEG_Debug(this);
    
    public Runnable r4300i_COP1_D_ROUND_W_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_ROUND_W_Debug(this);
    
    public Runnable r4300i_COP1_D_TRUNC_W_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_TRUNC_W_Debug(this);
    
    public Runnable r4300i_COP1_D_CVT_S_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_CVT_S_Debug(this);
    
    public Runnable r4300i_COP1_D_CVT_W_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_CVT_W_Debug(this);
    
    public Runnable r4300i_COP1_D_CVT_L_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_CVT_L_Debug(this);
    
    public Runnable r4300i_COP1_D_CMP_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_D_CMP_Debug(this);
    
    /************************** COP1: W functions ************************/
    public Runnable r4300i_COP1_W_CVT_S_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_W_CVT_S_Debug(this);
    
    public Runnable r4300i_COP1_W_CVT_D_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_W_CVT_D_Debug(this);
    
    /************************** COP1: L functions ************************/
    public Runnable r4300i_COP1_L_CVT_S_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_L_CVT_S_Debug(this);
    
    public Runnable r4300i_COP1_L_CVT_D_Debug = () -> kerneln64.cpu.CpuDebugCoprocessors.r4300i_COP1_L_CVT_D_Debug(this);
    
    /************************** Other functions **************************/
    public Runnable R4300i_UnknownOpcode_Debug = () -> kerneln64.cpu.CpuDebugInteger.R4300i_UnknownOpcode_Debug(this);
    
    public static Runnable[][] buildDebugCpuOps(DebugCpu cpu) {
        return kerneln64.cpu.CpuDebugOpcodeTable.buildDebugCpuOps(cpu);
    }
    
}
