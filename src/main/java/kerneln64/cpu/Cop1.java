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


import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;


public class Cop1 {
    
    // FPCR
    public static final int REVISION_REGISTER=                  0;
    public static final int FSTATUS_REGISTER=                   31;
    
    public static final int FPCSR_FS=				0x01000000;	/* flush denorm to zero */
    public static final int FPCSR_C=				0x00800000;	/* condition bit */
    public static final int FPCSR_CE=				0x00020000;	/* cause: unimplemented operation */
    public static final int FPCSR_CV=				0x00010000;	/* cause: invalid operation */
    public static final int FPCSR_CZ=				0x00008000;	/* cause: division by zero */
    public static final int FPCSR_CO=				0x00004000;	/* cause: overflow */
    public static final int FPCSR_CU=				0x00002000;	/* cause: underflow */
    public static final int FPCSR_CI=				0x00001000;	/* cause: inexact operation */
    public static final int FPCSR_EV=				0x00000800;	/* enable: invalid operation */
    public static final int FPCSR_EZ=				0x00000400;	/* enable: division by zero */
    public static final int FPCSR_EO=				0x00000200;	/* enable: overflow */
    public static final int FPCSR_EU=				0x00000100;	/* enable: underflow */
    public static final int FPCSR_EI=				0x00000080;	/* enable: inexact operation */
    public static final int FPCSR_FV=				0x00000040;	/* flag: invalid operation */
    public static final int FPCSR_FZ=				0x00000020;	/* flag: division by zero */
    public static final int FPCSR_FO=				0x00000010;	/* flag: overflow */
    public static final int FPCSR_FU=				0x00000008;	/* flag: underflow */
    public static final int FPCSR_FI=				0x00000004;	/* flag: inexact operation */
    public static final int FPCSR_RM_MASK=                      0x00000003;	/* rounding mode mask */
    public static final int FPCSR_RM_RN=			0x00000000;	/* round to nearest */
    public static final int FPCSR_RM_RZ=			0x00000001;	/* round to zero */
    public static final int FPCSR_RM_RP=			0x00000002;	/* round to positive infinity */
    public static final int FPCSR_RM_RM=			0x00000003;	/* round to negative infinity */
    
    // used by DebugCpu
    public class MipsDword {
        
        public long DW;
        
        public int getW(int index) {
            switch(mode32?index&1:0) {
                case 0:
                    return (int)DW;
                case 1:
                    return (int)(DW>>32);
                default:
                    return 0;
            }
        }
        
        public void setW(int index, int w) {
            switch(mode32?index&1:0) {
                case 0:
                    DW = ((((long) w)) & 0x00000000FFFFFFFFL) | (DW&0xFFFFFFFF00000000L);
                    break;
                case 1:
                    DW = ((((long) w) << 32) & 0xFFFFFFFF00000000L) | (DW&0x00000000FFFFFFFFL);
                    break;
                default:
                    return;
            }
        }
        
        public float getF(int index) {
            return Float.intBitsToFloat((int)DW);
        }
        
        public void setF(int index, float f) {

            DW = (((long)Float.floatToIntBits(f)))&0x00000000FFFFFFFFL;
        }
        
        public double getD() {
            return Double.longBitsToDouble(DW);
        }
        
        public void setD(double d) {
            DW = Double.doubleToLongBits(d);
        }
        
    }
    
    // used by Cpu, DebugOps
    public int[] FPCR = new int[32];
    
    // used by Cpu, DebugOps
    public MipsDword[] FPR = new MipsDword[32];
    
    private MipsDword[] tmpFPR = new MipsDword[16];
    
    // used by Cpu, DebugOps
    public boolean mode32;
    
    // initialized by Cpu
    public int fmt;
    public int ft;
    public int fs;
    public int fd;
    public int funct;
    
    private static final int RC_NEAR = 0;
    private static final int RC_CHOP = 1;
    private static final int RC_UP =   2;
    private static final int RC_DOWN = 3;
    
    private int roundingModel = RC_NEAR;
    private RoundingMode[] roundingMode = {
        RoundingMode.HALF_EVEN,
        RoundingMode.DOWN,
        RoundingMode.CEILING,
        RoundingMode.FLOOR
    };
    private MathContext[] floatContext = {
        MathContext.DECIMAL32,
        new MathContext(7, RoundingMode.DOWN),
        new MathContext(7, RoundingMode.CEILING),
        new MathContext(7, RoundingMode.FLOOR)
    };
    
    /** Creates a new instance of Cop1 */
    public Cop1() {
        for (int i=0; i<FPR.length; i++)
            FPR[i] = new MipsDword();
        FPCR[REVISION_REGISTER] = 0x00000511;
    }
    
    public void setMode32(boolean mode32) {
        if (!this.mode32 && mode32) {
            this.mode32 = mode32;
            for (int i=0; i<16; i++) {
                tmpFPR[i] = FPR[(i<<1)+1];
                FPR[(i<<1)+1] = FPR[i<<1];
            }
        }
        else if (this.mode32 && !mode32) {
            this.mode32 = mode32;
            for (int i=0; i<16; i++) {
                 FPR[(i<<1)+1] = tmpFPR[i];
            }
        }
    }
    
    // called by Cpu
    private void setRoundingModel(int fs) {
        switch(FPCR[fs] & 3) {
            case 0: roundingModel = RC_NEAR; break;
            case 1: roundingModel = RC_CHOP; break;
            case 2: roundingModel = RC_UP; break;
            case 3: roundingModel = RC_DOWN; break;
        }
    }
    
    public void lwC1(int ft, int value) {
        FPR[ft].setW(ft, value);
    }
    
    public int swC1(int ft) {
        return FPR[ft].getW(ft);
    }
    
    public void ldC1(int ft, long value) {
        FPR[ft].DW = value;
    }
    
    public long sdC1(int ft) {
        return FPR[ft].DW;
    }
    
    public int mfC1(int fs) {
        return FPR[fs].getW(fs);
    }
    
    public void mtC1(int fs, int value) {
        FPR[fs].setW(fs, value);
    }
    
    public long dmfC1(int fs) {
        return FPR[fs].DW;
    }
    
    public void dmtC1(int fs, long value) {
        FPR[fs].DW = value;
    }
    
    public void ctC1(int fs, int value) {
        if (fs != 31) {
            System.err.printf("CTC1 what register are you writing to ?\n");
            return;
        }
        FPCR[fs] = value;
        setRoundingModel(fs);
    }
    
    public int cfC1(int fs) {
        if (fs != 31 && fs != 0) {
            System.err.printf("CFC1 what register are you reading from ?\n");
            return 0;
        }
        return (int)FPCR[fs];
    }
    
    /************************** COP1: S functions ************************/
    public Runnable r4300i_COP1_S_ADD = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, FPR[fs].getF(fs) + FPR[ft].getF(ft));
        }
    };
    
    public Runnable r4300i_COP1_S_SUB = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, FPR[fs].getF(fs) - FPR[ft].getF(ft));
        }
    };
    
    public Runnable r4300i_COP1_S_MUL = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, FPR[fs].getF(fs) * FPR[ft].getF(ft));
        }
    };
    
    public Runnable r4300i_COP1_S_DIV = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, FPR[fs].getF(fs) / FPR[ft].getF(ft));
        }
    };
    
    public Runnable r4300i_COP1_S_SQRT = new Runnable() {
        public void run() {
            float val = FPR[fs].getF(fs);
            double result = StrictMath.sqrt(val);
            // FIX: Guard against NaN (negative input) and Infinity
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                FPR[fd].setF(fd, (float)result);
                return;
            }
            FPR[fd].setF(fd, BigDecimal.valueOf(result).round(floatContext[roundingModel]).floatValue());
        }
    };
    
    public Runnable r4300i_COP1_S_ABS = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, StrictMath.abs(FPR[fs].getF(fs)));
        }
    };
    
    public Runnable r4300i_COP1_S_MOV = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, FPR[fs].getF(fs));
        }
    };
    
    public Runnable r4300i_COP1_S_NEG = new Runnable() {
        public void run() {
            float val = FPR[fs].getF(fs);
            // FIX: Guard against NaN and Infinity before BigDecimal conversion
            if (Float.isNaN(val) || Float.isInfinite(val)) {
                FPR[fd].setF(fd, -val); // Handle negation directly
                return;
            }
            FPR[fd].setF(fd, BigDecimal.valueOf(val).negate(floatContext[roundingModel]).floatValue());
        }
    };
    
    public Runnable r4300i_COP1_S_TRUNC_L = new Runnable() {
        public void run() {
            FPR[fd].DW = (long)(double)FPR[fs].getF(fs);
        }
    };
    
    public Runnable r4300i_COP1_S_ROUND_W = new Runnable() {
        public void run() {
        }
    };
    
    public Runnable r4300i_COP1_S_TRUNC_W = new Runnable() {
        public void run() {
            FPR[fd].setW(fd, (int)FPR[fs].getF(fs));
        }
    };
    
    public Runnable r4300i_COP1_S_FLOOR_W = new Runnable() {
        public void run() {
            FPR[fd].setW(fd, (int)StrictMath.floor(FPR[fs].getF(fs)));
        }
    };
    
    public Runnable r4300i_COP1_S_CVT_D = new Runnable() {
        public void run() {
            FPR[fd].setD((double)FPR[fs].getF(fs));
        }
    };
    
    public Runnable r4300i_COP1_S_CVT_W = new Runnable() {
        public void run() {
            float val = FPR[fs].getF(fs);
            if (Float.isNaN(val) || Float.isInfinite(val)) {
                FPR[fd].setW(fd, 0); // Default to 0 or handle as needed
                return;
            }
            FPR[fd].setW(fd, BigDecimal.valueOf(val).setScale(0, roundingMode[roundingModel]).intValue());
        }
    };
    
    public Runnable r4300i_COP1_S_CVT_L = new Runnable() {
        public void run() {
            FPR[fd].DW = BigDecimal.valueOf(FPR[fs].getF(fs)).setScale(0, roundingMode[roundingModel]).longValue();
        }
    };
    
    public Runnable r4300i_COP1_S_CMP = new Runnable() {
        public void run() {
            boolean less;
            boolean equal;
            boolean unorded;
            float temp0 = FPR[fs].getF(fs);
            float temp1 = FPR[ft].getF(ft);
            
            if (Float.isNaN(temp0) || Float.isNaN(temp1)) {
                System.err.printf("Nan ?\n");
                less = false;
                equal = false;
                unorded = true;
                if ((funct & 8) != 0) {
                    System.err.printf("Signal InvalidOperationException\nin r4300i_COP1_S_CMP\n%X  %ff\n%X  %ff\n", temp0, temp0, temp1, temp1);
                }
            } else {
                less = temp0 < temp1;
                equal = temp0 == temp1;
                unorded = false;
            }
            
            boolean condition = (((funct&4)==0?false:true) && less)
            | (((funct&2)==0?false:true) && equal)
            | (((funct&1)==0?false:true) && unorded);
            
            if (condition) {
                FPCR[FSTATUS_REGISTER] |= FPCSR_C;
            } else {
                FPCR[FSTATUS_REGISTER] &= ~FPCSR_C;
            }
        }
    };
    
    /************************** COP1: D functions ************************/
    public Runnable r4300i_COP1_D_ADD = new Runnable() {
        public void run() {
//            FPR[fd].setD(FPR[fs].getD() + FPR[ft].getD());
            FPR[fd].setD(FPR[fs].getD() + FPR[ft].getD());
        }
    };
    
    public Runnable r4300i_COP1_D_SUB = new Runnable() {
        public void run() {
//            FPR[fd].setD(FPR[fs].getD() - FPR[ft].getD());
            FPR[fd].setD(FPR[fs].getD() - FPR[ft].getD());
        }
    };
    
    public Runnable r4300i_COP1_D_MUL = new Runnable() {
        public void run() {
//            FPR[fd].setD(FPR[fs].getD() * FPR[ft].getD());
            FPR[fd].setD(FPR[fs].getD() * FPR[ft].getD());
        }
    };
    
    public Runnable r4300i_COP1_D_DIV = new Runnable() {
        public void run() {
//            FPR[fd].setD(FPR[fs].getD() / FPR[ft].getD());
            FPR[fd].setD(FPR[fs].getD() / FPR[ft].getD());
        }
    };
    
    public Runnable r4300i_COP1_D_SQRT = new Runnable() {
        public void run() {
//            FPR[fd].setD(StrictMath.sqrt(FPR[fs].getD()));
            FPR[fd].setD(StrictMath.sqrt(FPR[fs].getD()));
        }
    };
    
    public Runnable r4300i_COP1_D_ABS = new Runnable() {
        public void run() {
//            FPR[fd].setD(StrictMath.abs(FPR[fs].getD()));
            FPR[fd].setD(StrictMath.abs(FPR[fs].getD()));
        }
    };
    
    public Runnable r4300i_COP1_D_MOV = new Runnable() {
        public void run() {
//            FPR[fd].setD(FPR[fs].getD());
            FPR[fd].setD(FPR[fs].getD());
        }
    };
    
    public Runnable r4300i_COP1_D_NEG = new Runnable() {
        public void run() {
//            FPR[fd].setD(FPR[fs].getD() * -1.0);
            FPR[fd].setD(FPR[fs].getD() * -1.0);
        }
    };
    
    public Runnable r4300i_COP1_D_ROUND_W = new Runnable() {
        public void run() {
        }
    };
    
    public Runnable r4300i_COP1_D_TRUNC_W = new Runnable() {
        public void run() {
//            FPR[fd].setW(fd, (int)FPR[fs].getD());
            FPR[fd].setW(fd, (int)FPR[fs].getD());
        }
    };
    
    public Runnable r4300i_COP1_D_CVT_S = new Runnable() {
        public void run() {
//            FPR[fd].setF(fd, (float)FPR[fs].getD());
            FPR[fd].setF(fd, (float)FPR[fs].getD());
        }
    };
    
    public Runnable r4300i_COP1_D_CVT_W = new Runnable() {
        public void run() {
//            FPR[fd].setW(fd, BigDecimal.valueOf(FPR[fs].getD()).setScale(0, roundingMode[roundingModel]).intValue());
            FPR[fd].setW(fd, BigDecimal.valueOf(FPR[fs].getD()).setScale(0, roundingMode[roundingModel]).intValue());
        }
    };
    
    public Runnable r4300i_COP1_D_CVT_L = new Runnable() {
        public void run() {
        }
    };
    
    public Runnable r4300i_COP1_D_CMP = new Runnable() {
        public void run() {
            boolean less;
            boolean equal;
            boolean unorded;
//            double temp0 = FPR[fs].getD();
//            double temp1 = FPR[ft].getD();
            double temp0 = FPR[fs].getD();
            double temp1 = FPR[ft].getD();
            
            if (Double.isNaN(temp0) || Double.isNaN(temp1)) {
                System.err.printf("Nan ?\n");
                less = false;
                equal = false;
                unorded = true;
                if ((funct & 8) != 0) {
                    System.err.printf("Signal InvalidOperationException\nin r4300i_COP1_D_CMP\n");
                }
            } else {
                less = temp0 < temp1;
                equal = temp0 == temp1;
                unorded = false;
            }
            
            boolean condition = (((funct & 4)!=0) && less)
            | (((funct & 2)!=0) && equal)
            | (((funct & 1)!=0) && unorded);
            
            if (condition) {
                FPCR[FSTATUS_REGISTER] |= FPCSR_C;
            } else {
                FPCR[FSTATUS_REGISTER] &= ~FPCSR_C;
            }
        }
    };
    
    /************************** COP1: W functions ************************/
    public Runnable r4300i_COP1_W_CVT_S = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, (float)FPR[fs].getW(fs));
        }
    };
    
    public Runnable r4300i_COP1_W_CVT_D = new Runnable() {
        public void run() {
            FPR[fd].setD((double)FPR[fs].getW(fs));
        }
    };
    
    /************************** COP1: L functions ************************/
    public Runnable r4300i_COP1_L_CVT_S = new Runnable() {
        public void run() {
            FPR[fd].setF(fd, BigDecimal.valueOf(FPR[fs].DW).round(floatContext[roundingModel]).floatValue());
        }
    };
    
    public Runnable r4300i_COP1_L_CVT_D = new Runnable() {
        public void run() {
        }
    };


//        float a = 1.2f;
//        float b = 1.6f;
//        float c = -1.2f;
//        float d = -1.6f;
//        float e = 234.65369f;
//        float g = 1.253645744568959399898456838845878984845656456f;


//        BigDecimal d1 = new BigDecimal(14.050001);

//        BigDecimal d2 = d1.setScale(0, BigDecimal.ROUND_HALF_EVEN);

//        double fs = Double.longBitsToDouble(0x412E84803810346CL);

//        BigDecimal bd = BigDecimal.valueOf(fs).round(MathContext.DECIMAL32);
//        float fv = bd.floatValue();


}
