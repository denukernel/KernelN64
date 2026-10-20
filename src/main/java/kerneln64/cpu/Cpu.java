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
import kerneln64.hardware.Audio;
import kerneln64.ui.CoreOptions;
import kerneln64.debugger.CrashDiagnostics;
import kerneln64.debugger.DebuggerCore;
import kerneln64.memory.Dma;
import kerneln64.debugger.KernelThreadDialog;
import kerneln64.debugger.KernelThreadMonitor;
import kerneln64.memory.Memory;
import kerneln64.hardware.Pif;
import kerneln64.state.Rewind;
import kerneln64.hardware.Rsp;
import kerneln64.hardware.Video;
import kerneln64.Main;


import kerneln64.memory.Memory.MemoryException;
import kerneln64.cpu.Cop0.TlbException;
import static kerneln64.Main.ShowTLBMisses;


/** CPU state, instruction dispatch, interrupts, and execution lifecycle.
 * Instruction implementations live in the package-private Cpu*Instructions helpers.
 * State fields stay here to preserve the save-state schema. */
public class Cpu {
    
    public static boolean dump_inst;
    public Thread emuThread;
    public volatile boolean running = false;
    private final java.util.concurrent.ConcurrentLinkedQueue<java.util.concurrent.FutureTask<Void>> commands = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public final DebuggerCore debugger = new DebuggerCore(this);

    public java.util.concurrent.Future<Void> submitCommand(Runnable command) {
        java.util.concurrent.FutureTask<Void> task = new java.util.concurrent.FutureTask<>(command, null);
        if (!running || emuThread == null || !emuThread.isAlive()) {
            task.cancel(false);
            return task;
        }
        commands.add(task);
        return task;
    }

    public void invalidateCode(int address) {
        int index = address >>> 2;
        if (cachedOpcodes != null && index < cachedOpcodes.length) cachedOpcodes[index].cached = false;
    }

    public void invalidateAllCode() {
        if (cachedOpcodes != null) for (CachedOpcode opcode : cachedOpcodes) opcode.cached = false;
    }


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
    
    public class CPU_ACTION {
        public boolean DoSomething;
        //public boolean CloseCPU;
        public boolean CheckInterrupts;
        public boolean DoInterrupt;
    };
    
    public class CachedOpcode {
        public int inst;
        public boolean cached;
        public Runnable code;
    }
    
    private static final int OP = 26;
    static final int RS = 21;
    static final int RT = 16;
    static final int RD = 11;
    static final int SA = 06;
    
    // used by Main(init), DebugOps
    public int pc;
    public long[] GPR = new long[32];
    
    // used by DebugOps
    public Cop0 cop0;
    public Cop1 cop1;
    protected int jumpToLocation;
    public long HI;
    public long LO;
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
    
    public int addr;
    public int llBit;
    public int llAddr;
    private int[] regMI; // CheckInterrupts
    private int[] regCop0;
    CachedOpcode[] cachedOpcodes;
    private CachedOpcode cachedOp;
    private Runnable cachedCode;
    public boolean inDelaySlot;
    private CPU_ACTION cpuAction = new CPU_ACTION();
    Memory mem;
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
    long[] tmpDmultu = new long[3];
    int tmpOp;
    int tmpRs;
    int tmpRt;
    int tmpRd;
    int tmpFunct;
    boolean cacheInstructions;
    /** Host pause reasons stay outside guest savestates. */
    public final PauseState pauseState = new PauseState();
    public static final class PauseState {
        private volatile boolean manual, inactive;
        public boolean manual() { return manual; }
        public boolean blocked() { return manual || inactive; }
        public void manual(boolean value) { manual = value; }
        public void inactive(boolean value) { inactive = value; }
        public void clear() { manual = inactive = false; }
    }
    public boolean isPaused() { return pauseState.blocked() || debugger.paused; }
    
    /** Creates a new instance of Cpu */
    public Cpu(Registers regs, Memory mem) {
        this.regMI = regs.regMI;
        this.mem = mem;
        mem.debugger = debugger;
        LO = 0x0;
        HI = 0x0;
    }
    
    public void cacheInstructions(boolean cache) {
        cacheInstructions = cache;
        if (cacheInstructions) {
            cachedOpcodes = new CachedOpcode[0x802000>>>2]; // 1050624b (1Mb)
            for (int i=0; i<(0x802000>>>2); i++)
                cachedOpcodes[i] = new CachedOpcode();
        }
        else {
            cachedOpcodes = null;
        }
    }
    
    // called by Main
    public void setOps(Runnable[][] ops) {
        r4300i_Opcode = ops[0];
        r4300i_Special = ops[1];
        r4300i_Regimm = ops[2];
        r4300i_CoP0 = ops[3];
        r4300i_CoP0_Function = ops[4];
        r4300i_CoP1 = ops[5];
        r4300i_CoP1_BC = ops[6];
        r4300i_CoP1_S = ops[7];
        r4300i_CoP1_D = ops[8];
        r4300i_CoP1_W = ops[9];
        r4300i_CoP1_L = ops[10];
    }
    
    // called by Main
    public void connect(Cop0 cop0, Cop1 cop1) {

        this.cop0 = cop0;
        this.cop1 = cop1;

        
        this.regCop0 = cop0.CP0;

    }
    
    // Misc ////////////////////////////////////////////////////////////////////
    
    // used by Main [Audio]
    public final Runnable AiCheckInterrupts = new Runnable() {
        public void run() {
            cpuAction.CheckInterrupts = true;
            cpuAction.DoSomething = true;
        }
    };
    
    // used by Main [Memory, Dma, Cop0, Rsp, Pif, Video]
    public final Runnable CheckInterrupts = new Runnable() {
        public void run() {
            regMI[Registers.MI_INTR_REG] &= ~Registers.MI_INTR_AI;
            regMI[Registers.MI_INTR_REG] |= (mem.audioIntrReg[0] & Registers.MI_INTR_AI);
            
            if ((regMI[Registers.MI_INTR_MASK_REG] & regMI[Registers.MI_INTR_REG]) != 0) {
                cop0.CP0[Cop0.FAKE_CAUSE_REGISTER] |= Cop0.CAUSE_IP2;
            } else  {
                cop0.CP0[Cop0.FAKE_CAUSE_REGISTER] &= ~Cop0.CAUSE_IP2;
            }
            if ((cop0.CP0[Cop0.STATUS_REGISTER] & Cop0.STATUS_IE) == 0) { return; }
            if ((cop0.CP0[Cop0.STATUS_REGISTER] & Cop0.STATUS_EXL) != 0) { return; }
            if ((cop0.CP0[Cop0.STATUS_REGISTER] & Cop0.STATUS_ERL) != 0) { return; }
            
            if ((cop0.CP0[Cop0.STATUS_REGISTER] & cop0.CP0[Cop0.FAKE_CAUSE_REGISTER] & 0xFF00) != 0) {
                if (!cpuAction.DoInterrupt) {
                    cpuAction.DoSomething = true;
                    cpuAction.DoInterrupt = true;
                }
            }
        }
    };
    
    ////////////////////////////////////////////////////////////////////////////
    
    // called by Main
    public void startEmulation() {
        if (running && emuThread != null && emuThread.isAlive()) {
             // Signal stop
             running = false;
             try {
                 emuThread.join(1000); // Wait up to 1s for it to die
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
        }
        
        running = true;
        cpuAction = new CPU_ACTION();
        cop0.initTimers();

        emuThread = new Thread(() -> {
            try { startInterpreterCPU(); }
            catch (Throwable error) {
                if (running || !isStopInterruption(error)) CrashDiagnostics.stopped(error);
            }
            finally {
                running = false;
                kerneln64.scripting.LuaScripts.stopped(this);
                KernelThreadMonitor.stopped(this);
                java.util.concurrent.FutureTask<Void> pending;
                while ((pending = commands.poll()) != null) pending.cancel(false);
            }
        }, "N64 CPU");
        emuThread.setPriority(Thread.MAX_PRIORITY);
        emuThread.start();
    }

    public static boolean isStopInterruption(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            if (cause instanceof InterruptedException) return true;
        return false;
    }


    void addressErrorException(int address, boolean fromRead) {
        if (inDelaySlot)
            jumpToLocation = cop0.doAddressError(true, address, fromRead, pc) + 4;
        else
            jumpTo(cop0.doAddressError(false, address, fromRead, pc));
    }
    
    boolean testCop1UsableException() {
        if ((cop0.CP0[Cop0.STATUS_REGISTER] & Cop0.STATUS_CU1) != 0)
            return false;
        if (inDelaySlot) {
            jumpToLocation = cop0.doCopUnusableException(true, 1, pc) + 4;
            return false;
        } else {
            jumpTo(cop0.doCopUnusableException(false, 1, pc));
            return true;
        }
    }
    
    // Helper to be called from Runnables when TlbException occurs
    void handleTlbException(int address, boolean store) {
         if (inDelaySlot)
            jumpToLocation = cop0.doTlbMiss(true, address, pc) + 4;
        else
            jumpTo(cop0.doTlbMiss(false, address, pc));
    }

    void tlbReadException(int address) {
        handleTlbException(address, false);
    }

    // The main emulation loop
    private void startInterpreterCPU() {
        while (running) {
            java.util.concurrent.FutureTask<Void> command;
            while ((command = commands.poll()) != null) command.run();
            if (!running) break;
            if (kerneln64.scripting.LuaScripts.running()) kerneln64.scripting.LuaScripts.service(this);
            if (CoreOptions.pauseOnKernelFault || CoreOptions.pauseOnAudioHandshake || KernelThreadMonitor.active != null || KernelThreadDialog.monitorOpen)
                KernelThreadMonitor.service(this);
            if (pauseState.blocked() || !debugger.beforeInstruction()) { java.util.concurrent.locks.LockSupport.parkNanos(2_000_000); continue; }
            if (Rewind.service()) continue;
            try {
                fetchInstruction();
            } catch (TlbException e) {
                pc = cop0.doTlbMiss(inDelaySlot, pc, pc);
                continue;
            } catch (MemoryException e) {
                throw new IllegalStateException(String.format("Invalid instruction fetch at %08X", pc), e);
            }

            // 2. Increment Timers
            cop0.tick++;

            // 4. Instruction Execution
            // apply speed multiplier to CPU timing (Placeholder for timing logic)
            if (cacheInstructions && cachedCode != null) {
                cachedCode.run();
            } else {
                r4300i_Opcode[((inst >> OP) & 0x3F)].run();
            }
            
            // Check if jumpTo updated PC (meaning an exception occurred during execution)
            // If so, we skip the standard pc += 4
            // Wait, jumpTo sets pc to target. But loop does pc += 4.
            // So executing an exception means we must adjust.
            // Actually, jumpTo sets pc to target - 4. So pc += 4 makes it target.
            // CORRECT.

            // 5. Update Program Counter
            pc += 4;
            GPR[0] = 0;

            // 6. Speed & Sync Handling
            if (Main.FrameLimit) {

            }
        }
    }
    private void fetchInstruction() throws TlbException, MemoryException {
        int physical = cop0.translateVaddr(pc);
        debugger.fetching = true;
        try { inst = mem.loadWord(physical); } finally { debugger.fetching = false; }
        debugger.record(pc, inst);
        CrashDiagnostics.instruction(pc, inst);
        if (!cacheInstructions) return;
        int index = physical < 0x800000 ? physical >>> 2
                : physical >= 0x04000000 && physical < 0x04002000 ? (physical - 0x03800000) >>> 2 : -1;
        if (index >= 0 && index < cachedOpcodes.length) {
            CachedOpcode entry = cachedOpcodes[index];
            // DMA, self-modifying code and GameShark can all replace an instruction.
            if (!entry.cached || entry.inst != inst) {
                entry.inst = inst;
                entry.code = (inst >>> OP) == 0 ? r4300i_Special[inst & 63] : r4300i_Opcode[inst >>> OP];
                entry.cached = true;
            }
            cachedCode = entry.code;
        } else cachedCode = (inst >>> OP) == 0 ? r4300i_Special[inst & 63] : r4300i_Opcode[inst >>> OP];
    }

    boolean executeInstruction() {
        try {
            fetchInstruction();
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
        GPR[0] = 0;
        return true;
    }
    
    int testInterpreterJump(int pc, int targetPC, int reg1, int reg2) {
        if (pc != targetPC) return targetPC;
        
        if (delaySlotEffectsCompare(pc, reg1, reg2)) return targetPC;
        
        if (cpuAction.DoInterrupt) return targetPC;
        
        if (cop0.inPermLoop()) {

            throw new IllegalStateException(String.format("Unrecoverable CPU loop at %08X", pc));
        }
        
        return targetPC;
    }
    
    private boolean delaySlotEffectsCompare(int pc, int reg1, int reg2) {
        return CpuBranchInstructions.delaySlotEffectsCompare(this, pc, reg1, reg2);
    }
    
    void jumpTo(int address) {
        inDelaySlot = false;
        pc = address;
        cop0.timerDone();
        if (cpuAction.DoSomething) {


            if (cpuAction.CheckInterrupts) {
                cpuAction.CheckInterrupts = false;
                CheckInterrupts.run();
            }
            if (cpuAction.DoInterrupt) {
                cpuAction.DoInterrupt = false;
                pc = cop0.doIntrException(false, pc);
            }
            
            cpuAction.DoSomething = false;
            
            if (cpuAction.DoInterrupt) {
                cpuAction.DoSomething = true;
            }
        }
        pc -= 4;
    }
    
    int compareUnsignedLongs(long a, long b) {
        if (a == b) return 0;
        if (((a>>32)&0xFFFFFFFFL) < ((b>>32)&0xFFFFFFFFL)) return -1;
        if (((a>>32)&0xFFFFFFFFL) > ((b>>32)&0xFFFFFFFFL)) return 1;
        if (((a>>32)&0xFFFFFFFFL) == ((b>>32)&0xFFFFFFFFL)
        && (a&0xFFFFFFFFL) < (b&0xFFFFFFFFL)) return -1;
        return 1;
    }
    
    public Runnable R4300i_opcode_SPECIAL = new Runnable() {
        public void run() {
            r4300i_Special[inst&0x3F].run();
        }
    };
    
    public Runnable R4300i_opcode_REGIMM = new Runnable() {
        public void run() {
            r4300i_Regimm[(inst>>RT)&0x1F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP0 = new Runnable() {
        public void run() {
            r4300i_CoP0[(inst>>21)&0x1F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP0_CO = new Runnable() {
        public void run() {
            r4300i_CoP0_Function[inst&0x3F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP1 = new Runnable() {
        public void run() {
            if (testCop1UsableException())
                return;
            cop1.ft = (inst>>RT)&0x1F;
            cop1.fs = (inst>>RD)&0x1F;
            cop1.fd = (inst>>SA)&0x1F;
            cop1.funct = inst&0x3F;
            r4300i_CoP1[cop1.fmt=(inst>>RS)&0x1F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP1_BC = new Runnable() {
        public void run() {
            r4300i_CoP1_BC[(inst>>RT)&0x1F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP1_S = new Runnable() {
        public void run() {
            r4300i_CoP1_S[inst&0x3F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP1_D = new Runnable() {
        public void run() {
            r4300i_CoP1_D[inst&0x3F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP1_W = new Runnable() {
        public void run() {
            r4300i_CoP1_W[inst&0x3F].run();
        }
    };
    
    public Runnable R4300i_opcode_COP1_L = new Runnable() {
        public void run() {
            r4300i_CoP1_L[inst&0x3F].run();
        }
    };
    
    /************************* OpCode functions *************************/
    public Runnable r4300i_J = () -> CpuBranchInstructions.r4300i_J(this);
    
    public Runnable r4300i_JAL = () -> CpuBranchInstructions.r4300i_JAL(this);
    
    public Runnable r4300i_BEQ = () -> CpuBranchInstructions.r4300i_BEQ(this);
    
    public Runnable r4300i_BNE = () -> CpuBranchInstructions.r4300i_BNE(this);
    
    public Runnable r4300i_BLEZ = () -> CpuBranchInstructions.r4300i_BLEZ(this);
    
    public Runnable r4300i_BGTZ = () -> CpuBranchInstructions.r4300i_BGTZ(this);
    
    public Runnable r4300i_ADDI = () -> CpuIntegerInstructions.r4300i_ADDI(this);
    
    public Runnable r4300i_ADDIU = () -> CpuIntegerInstructions.r4300i_ADDIU(this);
    
    public Runnable r4300i_SLTI = () -> CpuIntegerInstructions.r4300i_SLTI(this);
    
    public Runnable r4300i_SLTIU = () -> CpuIntegerInstructions.r4300i_SLTIU(this);
    
    public Runnable r4300i_ANDI = () -> CpuIntegerInstructions.r4300i_ANDI(this);
    
    public Runnable r4300i_ORI = () -> CpuIntegerInstructions.r4300i_ORI(this);
    
    public Runnable r4300i_XORI = () -> CpuIntegerInstructions.r4300i_XORI(this);
    
    public Runnable r4300i_LUI = () -> CpuIntegerInstructions.r4300i_LUI(this);
    
    public Runnable r4300i_BEQL = () -> CpuBranchInstructions.r4300i_BEQL(this);
    
    public Runnable r4300i_BNEL = () -> CpuBranchInstructions.r4300i_BNEL(this);
    
    public Runnable r4300i_BLEZL = () -> CpuBranchInstructions.r4300i_BLEZL(this);
    
    public Runnable r4300i_BGTZL = () -> CpuBranchInstructions.r4300i_BGTZL(this);
    
    public Runnable r4300i_DADDIU = () -> CpuIntegerInstructions.r4300i_DADDIU(this);
    
    
    public Runnable r4300i_LDL = () -> CpuMemoryInstructions.r4300i_LDL(this);
    
    
    public Runnable r4300i_LDR = () -> CpuMemoryInstructions.r4300i_LDR(this);
    
    public Runnable r4300i_LB = () -> CpuMemoryInstructions.r4300i_LB(this);
    
    public Runnable r4300i_LH = () -> CpuMemoryInstructions.r4300i_LH(this);
    
    
    public Runnable r4300i_LWL = () -> CpuMemoryInstructions.r4300i_LWL(this);
    
    public Runnable r4300i_LW = () -> CpuMemoryInstructions.r4300i_LW(this);
    
    public Runnable r4300i_LBU = () -> CpuMemoryInstructions.r4300i_LBU(this);
    
    public Runnable r4300i_LHU = () -> CpuMemoryInstructions.r4300i_LHU(this);
    
    
    public Runnable r4300i_LWR = () -> CpuMemoryInstructions.r4300i_LWR(this);
    
    public Runnable r4300i_LWU = () -> CpuMemoryInstructions.r4300i_LWU(this);
    
    public Runnable r4300i_SB = () -> CpuMemoryInstructions.r4300i_SB(this);
    
    public Runnable r4300i_SH = () -> CpuMemoryInstructions.r4300i_SH(this);
    
    
    public Runnable r4300i_SWL = () -> CpuMemoryInstructions.r4300i_SWL(this);
    
    public Runnable r4300i_SW = () -> CpuMemoryInstructions.r4300i_SW(this);
    
    
    public Runnable r4300i_SDL = () -> CpuMemoryInstructions.r4300i_SDL(this);
    
    
    public Runnable r4300i_SDR = () -> CpuMemoryInstructions.r4300i_SDR(this);
    
    
    public Runnable r4300i_SWR = () -> CpuMemoryInstructions.r4300i_SWR(this);
    
    public Runnable r4300i_CACHE = () -> CpuMemoryInstructions.r4300i_CACHE(this);
    
    public Runnable r4300i_LL = () -> CpuMemoryInstructions.r4300i_LL(this);
    
    public Runnable r4300i_LWC1 = () -> CpuMemoryInstructions.r4300i_LWC1(this);
    
    public Runnable r4300i_SC = () -> CpuMemoryInstructions.r4300i_SC(this);
    
    public Runnable r4300i_LD = () -> CpuMemoryInstructions.r4300i_LD(this);
    
    public Runnable r4300i_LDC1 = () -> CpuMemoryInstructions.r4300i_LDC1(this);
    
    public Runnable r4300i_SWC1 = () -> CpuMemoryInstructions.r4300i_SWC1(this);
    
    public Runnable r4300i_SDC1 = () -> CpuMemoryInstructions.r4300i_SDC1(this);
    
    public Runnable r4300i_SD = () -> CpuMemoryInstructions.r4300i_SD(this);
    
    /********************** R4300i OpCodes: Special **********************/
    public Runnable r4300i_SPECIAL_SLL = () -> CpuIntegerInstructions.r4300i_SPECIAL_SLL(this);
    
    public Runnable r4300i_SPECIAL_SRL = () -> CpuIntegerInstructions.r4300i_SPECIAL_SRL(this);
    
    public Runnable r4300i_SPECIAL_SRA = () -> CpuIntegerInstructions.r4300i_SPECIAL_SRA(this);
    
    public Runnable r4300i_SPECIAL_SLLV = () -> CpuIntegerInstructions.r4300i_SPECIAL_SLLV(this);
    
    public Runnable r4300i_SPECIAL_SRLV = () -> CpuIntegerInstructions.r4300i_SPECIAL_SRLV(this);
    
    public Runnable r4300i_SPECIAL_SRAV = () -> CpuIntegerInstructions.r4300i_SPECIAL_SRAV(this);
    
    public Runnable r4300i_SPECIAL_JR = () -> CpuBranchInstructions.r4300i_SPECIAL_JR(this);
    
    public Runnable r4300i_SPECIAL_JALR = () -> CpuBranchInstructions.r4300i_SPECIAL_JALR(this);
    
    public Runnable r4300i_SPECIAL_SYSCALL = () -> CpuIntegerInstructions.r4300i_SPECIAL_SYSCALL(this);
    
    public Runnable r4300i_SPECIAL_BREAK = () -> CpuIntegerInstructions.r4300i_SPECIAL_BREAK(this);
    
    public Runnable r4300i_SPECIAL_SYNC = () -> CpuIntegerInstructions.r4300i_SPECIAL_SYNC(this);
    
    public Runnable r4300i_SPECIAL_MFHI = () -> CpuIntegerInstructions.r4300i_SPECIAL_MFHI(this);
    
    public Runnable r4300i_SPECIAL_MTHI = () -> CpuIntegerInstructions.r4300i_SPECIAL_MTHI(this);
    
    public Runnable r4300i_SPECIAL_MFLO = () -> CpuIntegerInstructions.r4300i_SPECIAL_MFLO(this);
    
    public Runnable r4300i_SPECIAL_MTLO = () -> CpuIntegerInstructions.r4300i_SPECIAL_MTLO(this);
    
    public Runnable r4300i_SPECIAL_DSLLV = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSLLV(this);
    
    public Runnable r4300i_SPECIAL_DSRLV = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSRLV(this);
    
    public Runnable r4300i_SPECIAL_DSRAV = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSRAV(this);
    
    public Runnable r4300i_SPECIAL_MULT = () -> CpuIntegerInstructions.r4300i_SPECIAL_MULT(this);
    
    public Runnable r4300i_SPECIAL_MULTU = () -> CpuIntegerInstructions.r4300i_SPECIAL_MULTU(this);
    
    public Runnable r4300i_SPECIAL_DIV = () -> CpuIntegerInstructions.r4300i_SPECIAL_DIV(this);
    
    public Runnable r4300i_SPECIAL_DIVU = () -> CpuIntegerInstructions.r4300i_SPECIAL_DIVU(this);
    
    public Runnable r4300i_SPECIAL_DMULT = () -> CpuIntegerInstructions.r4300i_SPECIAL_DMULT(this);
    
    public Runnable r4300i_SPECIAL_DMULTU = () -> CpuIntegerInstructions.r4300i_SPECIAL_DMULTU(this);
    
    public Runnable r4300i_SPECIAL_DDIV = () -> CpuIntegerInstructions.r4300i_SPECIAL_DDIV(this);
    
    public Runnable r4300i_SPECIAL_DDIVU = () -> CpuIntegerInstructions.r4300i_SPECIAL_DDIVU(this);
    
    public Runnable r4300i_SPECIAL_ADD = () -> CpuIntegerInstructions.r4300i_SPECIAL_ADD(this);
    
    public Runnable r4300i_SPECIAL_ADDU = () -> CpuIntegerInstructions.r4300i_SPECIAL_ADDU(this);
    
    public Runnable r4300i_SPECIAL_SUB = () -> CpuIntegerInstructions.r4300i_SPECIAL_SUB(this);
    
    public Runnable r4300i_SPECIAL_SUBU = () -> CpuIntegerInstructions.r4300i_SPECIAL_SUBU(this);
    
    public Runnable r4300i_SPECIAL_AND = () -> CpuIntegerInstructions.r4300i_SPECIAL_AND(this);
    
    public Runnable r4300i_SPECIAL_OR = () -> CpuIntegerInstructions.r4300i_SPECIAL_OR(this);
    
    public Runnable r4300i_SPECIAL_XOR = () -> CpuIntegerInstructions.r4300i_SPECIAL_XOR(this);
    
    public Runnable r4300i_SPECIAL_NOR = () -> CpuIntegerInstructions.r4300i_SPECIAL_NOR(this);
    
    public Runnable r4300i_SPECIAL_SLT = () -> CpuIntegerInstructions.r4300i_SPECIAL_SLT(this);
    
    public Runnable r4300i_SPECIAL_SLTU = () -> CpuIntegerInstructions.r4300i_SPECIAL_SLTU(this);
    
    public Runnable r4300i_SPECIAL_DADD = () -> CpuIntegerInstructions.r4300i_SPECIAL_DADD(this);
    
    public Runnable r4300i_SPECIAL_DADDU = () -> CpuIntegerInstructions.r4300i_SPECIAL_DADDU(this);
    
    public Runnable r4300i_SPECIAL_DSUB = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSUB(this);
    
    public Runnable r4300i_SPECIAL_DSUBU = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSUBU(this);
    
    public Runnable r4300i_SPECIAL_TEQ = () -> CpuIntegerInstructions.r4300i_SPECIAL_TEQ(this);
    
    public Runnable r4300i_SPECIAL_DSLL = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSLL(this);
    
    public Runnable r4300i_SPECIAL_DSRL = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSRL(this);
    
    public Runnable r4300i_SPECIAL_DSRA = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSRA(this);
    
    public Runnable r4300i_SPECIAL_DSLL32 = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSLL32(this);
    
    public Runnable r4300i_SPECIAL_DSRL32 = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSRL32(this);
    
    public Runnable r4300i_SPECIAL_DSRA32 = () -> CpuIntegerInstructions.r4300i_SPECIAL_DSRA32(this);
    
    /********************** R4300i OpCodes: RegImm **********************/
    public Runnable r4300i_REGIMM_BLTZ = () -> CpuBranchInstructions.r4300i_REGIMM_BLTZ(this);
    
    public Runnable r4300i_REGIMM_BGEZ = () -> CpuBranchInstructions.r4300i_REGIMM_BGEZ(this);
    
    public Runnable r4300i_REGIMM_BLTZL = () -> CpuBranchInstructions.r4300i_REGIMM_BLTZL(this);
    
    public Runnable r4300i_REGIMM_BGEZL = () -> CpuBranchInstructions.r4300i_REGIMM_BGEZL(this);
    
    public Runnable r4300i_REGIMM_BLTZAL = () -> CpuBranchInstructions.r4300i_REGIMM_BLTZAL(this);
    
    public Runnable r4300i_REGIMM_BGEZAL = () -> CpuBranchInstructions.r4300i_REGIMM_BGEZAL(this);
    
    /************************** COP1 functions **************************/
    public Runnable r4300i_COP1_MF = () -> CpuCopInstructions.r4300i_COP1_MF(this);
    
    public Runnable r4300i_COP1_DMF = () -> CpuCopInstructions.r4300i_COP1_DMF(this);
    
    public Runnable r4300i_COP1_CF = () -> CpuCopInstructions.r4300i_COP1_CF(this);
    
    public Runnable r4300i_COP1_MT = () -> CpuCopInstructions.r4300i_COP1_MT(this);
    
    public Runnable r4300i_COP1_DMT = () -> CpuCopInstructions.r4300i_COP1_DMT(this);
    
    public Runnable r4300i_COP1_CT = () -> CpuCopInstructions.r4300i_COP1_CT(this);
    
    /************************* COP1: BC1 functions ***********************/
    public Runnable r4300i_COP1_BCF = () -> CpuCopInstructions.r4300i_COP1_BCF(this);
    
    public Runnable r4300i_COP1_BCT = () -> CpuCopInstructions.r4300i_COP1_BCT(this);
    
    public Runnable r4300i_COP1_BCFL = () -> CpuCopInstructions.r4300i_COP1_BCFL(this);
    
    public Runnable r4300i_COP1_BCTL = () -> CpuCopInstructions.r4300i_COP1_BCTL(this);
    
    /************************** COP0 functions **************************/
    public Runnable r4300i_COP0_MF = () -> CpuCopInstructions.r4300i_COP0_MF(this);
    
    public Runnable r4300i_COP0_MT = () -> CpuCopInstructions.r4300i_COP0_MT(this);
    
    public Runnable r4300i_COP0_CO_ERET = () -> CpuCopInstructions.r4300i_COP0_CO_ERET(this);
    
    /************************** Other functions **************************/
    public Runnable R4300i_UnknownOpcode = new Runnable() {
        public void run() {
            throw new IllegalStateException(String.format("Unhandled opcode %08X at %08X", inst, pc));
        }
    };
    
}

