package kerneln64.cpu;
import kerneln64.Main;


import kerneln64.cpu.Cop0;
import kerneln64.cpu.Cop1;
import kerneln64.memory.Memory;
import kerneln64.cpu.Cop0.TlbException;
import kerneln64.cpu.Cpu.CPU_ACTION;
import kerneln64.cpu.Cpu.CachedOpcode;
import kerneln64.memory.Memory.MemoryException;

public class CpuExecuteInstruction {
    public static boolean dump_inst;
    
    //R4300i OpCodes
    public static final int R4300i_SPECIAL=                     0;
    public static final int R4300i_REGIMM=                      1;
    public static final int R4300i_J=				2;
    public static final int R4300i_JAL=				3;
    public static final int R4300i_BEQ=				4;
    public static final int R4300i_BNE=				5;
    public static final int R4300i_BLEZ=			6;
    public static final int R4300i_BGTZ=			7;
    public static final int R4300i_ADDI=			8;
    public static final int R4300i_ADDIU=                       9;
    public static final int R4300i_SLTI=			10;
    public static final int R4300i_SLTIU=                       11;
    public static final int R4300i_ANDI=			12;
    public static final int R4300i_ORI=				13;
    public static final int R4300i_XORI=			14;
    public static final int R4300i_LUI=				15;
    public static final int R4300i_CP0=				16;
    public static final int R4300i_CP1=				17;
    public static final int R4300i_BEQL=			20;
    public static final int R4300i_BNEL=			21;
    public static final int R4300i_BLEZL=                       22;
    public static final int R4300i_BGTZL=                       23;
    public static final int R4300i_DADDI=                       24;
    public static final int R4300i_DADDIU=                      25;
    public static final int R4300i_LDL=				26;
    public static final int R4300i_LDR=				27;
    public static final int R4300i_LB=				32;
    public static final int R4300i_LH=				33;
    public static final int R4300i_LWL=				34;
    public static final int R4300i_LW=				35;
    public static final int R4300i_LBU=				36;
    public static final int R4300i_LHU=				37;
    public static final int R4300i_LWR=				38;
    public static final int R4300i_LWU=				39;
    public static final int R4300i_SB=				40;
    public static final int R4300i_SH=				41;
    public static final int R4300i_SWL=				42;
    public static final int R4300i_SW=				43;
    public static final int R4300i_SDL=				44;
    public static final int R4300i_SDR=				45;
    public static final int R4300i_SWR=				46;
    public static final int R4300i_CACHE=                       47;
    public static final int R4300i_LL=				48;
    public static final int R4300i_LWC1=                        49;
    public static final int R4300i_LWC2=			0x32;
    public static final int R4300i_LLD=				0x34;
    public static final int R4300i_LDC1=			53;
    public static final int R4300i_LDC2=			0x36;
    public static final int R4300i_LD=				55;
    public static final int R4300i_SC=				0x38;
    public static final int R4300i_SWC1=			57;
    public static final int R4300i_SWC2=			0x3A;
    public static final int R4300i_SCD=				0x3C;
    public static final int R4300i_SDC1=			61;
    public static final int R4300i_SDC2=			62;
    public static final int R4300i_SD=				63;
    
    /* R4300i Special opcodes */
    public static final int R4300i_SPECIAL_SLL=			0;
    public static final int R4300i_SPECIAL_SRL=			2;
    public static final int R4300i_SPECIAL_SRA=			3;
    public static final int R4300i_SPECIAL_SLLV=                4;
    public static final int R4300i_SPECIAL_SRLV=                6;
    public static final int R4300i_SPECIAL_SRAV=                7;
    public static final int R4300i_SPECIAL_JR=			8;
    public static final int R4300i_SPECIAL_JALR=		9;
    public static final int R4300i_SPECIAL_SYSCALL=             12;
    public static final int R4300i_SPECIAL_BREAK=               13;
    public static final int R4300i_SPECIAL_SYNC=		15;
    public static final int R4300i_SPECIAL_MFHI=		16;
    public static final int R4300i_SPECIAL_MTHI=		17;
    public static final int R4300i_SPECIAL_MFLO=		18;
    public static final int R4300i_SPECIAL_MTLO=		19;
    public static final int R4300i_SPECIAL_DSLLV=               20;
    public static final int R4300i_SPECIAL_DSRLV=               22;
    public static final int R4300i_SPECIAL_DSRAV=               23;
    public static final int R4300i_SPECIAL_MULT=		24;
    public static final int R4300i_SPECIAL_MULTU=               25;
    public static final int R4300i_SPECIAL_DIV=			26;
    public static final int R4300i_SPECIAL_DIVU=		27;
    public static final int R4300i_SPECIAL_DMULT=               28;
    public static final int R4300i_SPECIAL_DMULTU=              29;
    public static final int R4300i_SPECIAL_DDIV=                30;
    public static final int R4300i_SPECIAL_DDIVU=               31;
    public static final int R4300i_SPECIAL_ADD=			32;
    public static final int R4300i_SPECIAL_ADDU=                33;
    public static final int R4300i_SPECIAL_SUB=			34;
    public static final int R4300i_SPECIAL_SUBU=		35;
    public static final int R4300i_SPECIAL_AND=			36;
    public static final int R4300i_SPECIAL_OR=			37;
    public static final int R4300i_SPECIAL_XOR=			38;
    public static final int R4300i_SPECIAL_NOR=			39;
    public static final int R4300i_SPECIAL_SLT=			42;
    public static final int R4300i_SPECIAL_SLTU=		43;
    public static final int R4300i_SPECIAL_DADD=		44;
    public static final int R4300i_SPECIAL_DADDU=               45;
    public static final int R4300i_SPECIAL_DSUB=		46;
    public static final int R4300i_SPECIAL_DSUBU=               47;
    public static final int R4300i_SPECIAL_TGE=			48;
    public static final int R4300i_SPECIAL_TGEU=		49;
    public static final int R4300i_SPECIAL_TLT=			50;
    public static final int R4300i_SPECIAL_TLTU=		51;
    public static final int R4300i_SPECIAL_TEQ=			52;
    public static final int R4300i_SPECIAL_TNE=			54;
    public static final int R4300i_SPECIAL_DSLL=		56;
    public static final int R4300i_SPECIAL_DSRL=		58;
    public static final int R4300i_SPECIAL_DSRA=		59;
    public static final int R4300i_SPECIAL_DSLL32=              60;
    public static final int R4300i_SPECIAL_DSRL32=              62;
    public static final int R4300i_SPECIAL_DSRA32=              63;
    
    /* R4300i RegImm opcodes */
    public static final int R4300i_REGIMM_BLTZ=			0;
    public static final int R4300i_REGIMM_BGEZ=			1;
    public static final int R4300i_REGIMM_BLTZL=		2;
    public static final int R4300i_REGIMM_BGEZL=		3;
    public static final int R4300i_REGIMM_TGEI=			0x08;
    public static final int R4300i_REGIMM_TGEIU=		0x09;
    public static final int R4300i_REGIMM_TLTI=			0x0A;
    public static final int R4300i_REGIMM_TLTIU=		0x0B;
    public static final int R4300i_REGIMM_TEQI=			0x0C;
    public static final int R4300i_REGIMM_TNEI=			0x0E;
    public static final int R4300i_REGIMM_BLTZAL=               0x10;
    public static final int R4300i_REGIMM_BGEZAL=               17;
    public static final int R4300i_REGIMM_BLTZALL=              0x12;
    public static final int R4300i_REGIMM_BGEZALL=              0x13;
    
    /* R4300i COP0 opcodes */
    public static final int R4300i_COP0_MF=			0;
    public static final int R4300i_COP0_MT=			4;
    
    /* R4300i COP0 CO opcodes */
    public static final int R4300i_COP0_CO_TLBR=		1;
    public static final int R4300i_COP0_CO_TLBWI=               2;
    public static final int R4300i_COP0_CO_TLBWR=               6;
    public static final int R4300i_COP0_CO_TLBP=		8;
    public static final int R4300i_COP0_CO_ERET=		24;
    
    /* R4300i COP1 opcodes */
    public static final int R4300i_COP1_MF=			0;
    public static final int R4300i_COP1_DMF=			1;
    public static final int R4300i_COP1_CF=			2;
    public static final int R4300i_COP1_MT=			4;
    public static final int R4300i_COP1_DMT=			5;
    public static final int R4300i_COP1_CT=			6;
    public static final int R4300i_COP1_BC=			8;
    public static final int R4300i_COP1_S=			16;
    public static final int R4300i_COP1_D=			17;
    public static final int R4300i_COP1_W=			20;
    public static final int R4300i_COP1_L=			21;
    
    /* R4300i COP1 BC opcodes */
    public static final int R4300i_COP1_BC_BCF=			0;
    public static final int R4300i_COP1_BC_BCT=			1;
    public static final int R4300i_COP1_BC_BCFL=		2;
    public static final int R4300i_COP1_BC_BCTL=		3;
    
    public static final int R4300i_COP1_FUNCT_ADD=              0;
    public static final int R4300i_COP1_FUNCT_SUB=              1;
    public static final int R4300i_COP1_FUNCT_MUL=              2;
    public static final int R4300i_COP1_FUNCT_DIV=              3;
    public static final int R4300i_COP1_FUNCT_SQRT=             4;
    public static final int R4300i_COP1_FUNCT_ABS=              5;
    public static final int R4300i_COP1_FUNCT_MOV=              6;
    public static final int R4300i_COP1_FUNCT_NEG=              7;
    public static final int R4300i_COP1_FUNCT_ROUND_L=          8;
    public static final int R4300i_COP1_FUNCT_TRUNC_L=          9;
    public static final int R4300i_COP1_FUNCT_CEIL_L=           10;
    public static final int R4300i_COP1_FUNCT_FLOOR_L=          11;
    public static final int R4300i_COP1_FUNCT_ROUND_W=          12;
    public static final int R4300i_COP1_FUNCT_TRUNC_W=          13;
    public static final int R4300i_COP1_FUNCT_CEIL_W=           14;
    public static final int R4300i_COP1_FUNCT_FLOOR_W=          15;
    public static final int R4300i_COP1_FUNCT_CVT_S=		32;
    public static final int R4300i_COP1_FUNCT_CVT_D=		33;
    public static final int R4300i_COP1_FUNCT_CVT_W=		36;
    public static final int R4300i_COP1_FUNCT_CVT_L=		37;
    public static final int R4300i_COP1_FUNCT_C_F=              48;
    public static final int R4300i_COP1_FUNCT_C_UN=             49;
    public static final int R4300i_COP1_FUNCT_C_EQ=             50;
    public static final int R4300i_COP1_FUNCT_C_UEQ=		51;
    public static final int R4300i_COP1_FUNCT_C_OLT=		52;
    public static final int R4300i_COP1_FUNCT_C_ULT=		53;
    public static final int R4300i_COP1_FUNCT_C_OLE=		54;
    public static final int R4300i_COP1_FUNCT_C_ULE=		55;
    public static final int R4300i_COP1_FUNCT_C_SF=             56;
    public static final int R4300i_COP1_FUNCT_C_NGLE=           57;
    public static final int R4300i_COP1_FUNCT_C_SEQ=		58;
    public static final int R4300i_COP1_FUNCT_C_NGL=		59;
    public static final int R4300i_COP1_FUNCT_C_LT=             60;
    public static final int R4300i_COP1_FUNCT_C_NGE=		61;
    public static final int R4300i_COP1_FUNCT_C_LE=             62;
    public static final int R4300i_COP1_FUNCT_C_NGT=		63;
    
    private class CPU_ACTION {
        public boolean DoSomething;
        //public boolean CloseCPU;
        public boolean CheckInterrupts;
        public boolean DoInterrupt;
    };
    
    private class CachedOpcode {
        public int inst;
        public boolean cached;
        public Runnable code;
    }
    
    private static final int OP = 26;
    private static final int RS = 21;
    private static final int RT = 16;
    private static final int RD = 11;
    private static final int SA = 06;
    
    // used by Main(init), DebugOps
    public int pc;
    public long[] GPR = new long[32];
    
    // used by DebugOps
    protected Cop0 cop0;
    protected Cop1 cop1;
    protected int jumpToLocation;
    protected long HI;
    protected long LO;
    protected int tmpWord;
    protected long tmpDouble;
    protected int inst;
    protected int target;
    protected int rs;
    protected int base;
    protected int rt;
    protected short offset;
    protected int rd;
    protected int sa;
    protected int funct;
    protected int currentInstr;
    
    private int addr;
    private int llBit;
    private int llAddr;
    private int[] regMI; // CheckInterrupts
    private int[] regCop0;
    private CachedOpcode[] cachedOpcodes;
    private CachedOpcode cachedOp;
    private Runnable cachedCode;
    private boolean inDelaySlot;
    private CPU_ACTION cpuAction;
    private Memory mem;
    private Runnable[] r4300i_Opcode;
    private Runnable[] r4300i_Special;
    private Runnable[] r4300i_Regimm;
    private Runnable[] r4300i_CoP0;
    private Runnable[] r4300i_CoP0_Function;
    private Runnable[] r4300i_CoP1;
    private Runnable[] r4300i_CoP1_BC;
    private Runnable[] r4300i_CoP1_S;
    private Runnable[] r4300i_CoP1_D;
    private Runnable[] r4300i_CoP1_W;
    private Runnable[] r4300i_CoP1_L;
    private long[] tmpDmultu = new long[3];
    private int tmpOp;
    private int tmpRs;
    private int tmpRt;
    private int tmpRd;
    private int tmpFunct;
    private boolean cacheInstructions;
    
    public boolean executeInstruction() {
        try {
            if (cacheInstructions) {
                int pAddr = cop0.translateVaddr(pc);
                cachedOp = cachedOpcodes[(pAddr<0x400000)?(pAddr>>>2):((pAddr-0x3C00000)>>>2)];
                if (cachedOp.cached) {
                    inst = cachedOp.inst;
                    cachedCode = cachedOp.code;
                } else {
                    cachedOp.cached = true;
                    cachedOp.inst = inst = mem.loadWord(pAddr);
                    cachedCode = cachedOp.code = (((inst>>OP)&0x3F)==0)?r4300i_Special[inst&0x3F]:r4300i_Opcode[(inst>>OP)&0x3F];
                }
            } else {
                inst = mem.loadWord(cop0.translateVaddr(pc));
            }
        } catch (TlbException e) {
            pc = cop0.doTlbMiss(inDelaySlot, pc, pc);
            return false;
        } catch (MemoryException e) {
            e.printStackTrace();
            return false;
        }
        
        cop0.tick++;
        
        if (cacheInstructions)
            cachedCode.run();
        else
            r4300i_Opcode[((inst>>OP)&0x3F)].run();
        
        return true;
    }

}
