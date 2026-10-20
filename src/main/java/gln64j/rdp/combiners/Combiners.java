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


package gln64j.rdp.combiners;

import static gln64j.rdp.combiners.CombinerMux.*;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;


public class Combiners {
    
    public static final int TEXTURE_ENV=		0;
    public static final int TEXTURE_ENV_COMBINE=	1;
    public static final int NV_REGISTER_COMBINERS=	2;
    public static final int NV_TEXTURE_ENV_COMBINE4=	3;
    
    /*
     * G_SETCOMBINE: color combine modes
     */
    /* Color combiner constants: */
    public static final int G_CCMUX_COMBINED=           0;
    public static final int G_CCMUX_TEXEL0=             1;
    public static final int G_CCMUX_TEXEL1=             2;
    public static final int G_CCMUX_PRIMITIVE=          3;
    public static final int G_CCMUX_SHADE=              4;
    public static final int G_CCMUX_ENVIRONMENT=        5;
    public static final int G_CCMUX_CENTER=             6;
    public static final int G_CCMUX_SCALE=              6;
    public static final int G_CCMUX_COMBINED_ALPHA=     7;
    public static final int G_CCMUX_TEXEL0_ALPHA=       8;
    public static final int G_CCMUX_TEXEL1_ALPHA=       9;
    public static final int G_CCMUX_PRIMITIVE_ALPHA=	10;
    public static final int G_CCMUX_SHADE_ALPHA=        11;
    public static final int G_CCMUX_ENV_ALPHA=          12;
    public static final int G_CCMUX_LOD_FRACTION=       13;
    public static final int G_CCMUX_PRIM_LOD_FRAC=      14;
    public static final int G_CCMUX_NOISE=              7;
    public static final int G_CCMUX_K4=                 7;
    public static final int G_CCMUX_K5=                 15;
    public static final int G_CCMUX_1=                  6;
    public static final int G_CCMUX_0=                  31;
    
    /* Alpha combiner constants: */
    public static final int G_ACMUX_COMBINED=           0;
    public static final int G_ACMUX_TEXEL0=             1;
    public static final int G_ACMUX_TEXEL1=             2;
    public static final int G_ACMUX_PRIMITIVE=          3;
    public static final int G_ACMUX_SHADE=              4;
    public static final int G_ACMUX_ENVIRONMENT=        5;
    public static final int G_ACMUX_LOD_FRACTION=       0;
    public static final int G_ACMUX_PRIM_LOD_FRAC=      6;
    public static final int G_ACMUX_1=                  6;
    public static final int G_ACMUX_0=                  7;
    
    public static final int SR_MASK_0 = (0x01 << 0) - 1;
    public static final int SR_MASK_1 = (0x01 << 1) - 1;
    public static final int SR_MASK_2 = (0x01 << 2) - 1;
    public static final int SR_MASK_3 = (0x01 << 3) - 1;
    public static final int SR_MASK_4 = (0x01 << 4) - 1;
    public static final int SR_MASK_5 = (0x01 << 5) - 1;
    public static final int SR_MASK_6 = (0x01 << 6) - 1;
    public static final int SR_MASK_7 = (0x01 << 7) - 1; // 0x7F
    public static final int SR_MASK_8 = (0x01 << 8) - 1; // 0xFF
    public static final int SR_MASK_9 = (0x01 << 9) - 1;
    public static final int SR_MASK_10 = (0x01 << 10) - 1;
    public static final int SR_MASK_11 = (0x01 << 11) - 1;
    public static final int SR_MASK_12 = (0x01 << 12) - 1;
    public static final int SR_MASK_13 = (0x01 << 13) - 1;
    public static final int SR_MASK_14 = (0x01 << 14) - 1;
    public static final int SR_MASK_15 = (0x01 << 15) - 1;
    public static final int SR_MASK_16 = (0x01 << 16) - 1;
    public static final int SR_MASK_17 = (0x01 << 17) - 1;
    public static final int SR_MASK_18 = (0x01 << 18) - 1;
    public static final int SR_MASK_19 = (0x01 << 19) - 1;
    public static final int SR_MASK_20 = (0x01 << 20) - 1;
    public static final int SR_MASK_21 = (0x01 << 21) - 1;
    public static final int SR_MASK_22 = (0x01 << 22) - 1;
    public static final int SR_MASK_23 = (0x01 << 23) - 1;
    public static final int SR_MASK_24 = (0x01 << 24) - 1;
    public static final int SR_MASK_25 = (0x01 << 25) - 1;
    public static final int SR_MASK_26 = (0x01 << 26) - 1;
    public static final int SR_MASK_27 = (0x01 << 27) - 1;
    public static final int SR_MASK_28 = (0x01 << 28) - 1;
    public static final int SR_MASK_29 = (0x01 << 29) - 1;
    public static final int SR_MASK_30 = (0x01 << 30) - 1;
    public static final int SR_MASK_31 = (0x01 << 31) - 1;
    
    // Internal combiner commands
    public static final int LOAD=               0;
    public static final int SUB=                1;
    public static final int MUL=		2;
    public static final int ADD=		3;
    public static final int INTER=              4;
    
    // Internal generalized combiner inputs
    public static final int COMBINED=		0;
    public static final int TEXEL0=		1;
    public static final int TEXEL1=		2;
    public static final int PRIMITIVE=		3;
    public static final int SHADE=		4;
    public static final int ENVIRONMENT=	5;
    public static final int CENTER=		6;
    public static final int SCALE=		7;
    public static final int COMBINED_ALPHA=     8;
    public static final int TEXEL0_ALPHA=       9;
    public static final int TEXEL1_ALPHA=       10;
    public static final int PRIMITIVE_ALPHA=	11;
    public static final int SHADE_ALPHA=	12;
    public static final int ENV_ALPHA=		13;
    public static final int LOD_FRACTION=       14;
    public static final int PRIM_LOD_FRAC=      15;
    public static final int NOISE=              16;
    public static final int K4=			17;
    public static final int K5=			18;
    public static final int ONE=		19;
    public static final int ZERO=		20;
    
    public static boolean EXT_fog_coord;		// TNT, GeForce, Rage 128, Radeon
    public static boolean ATI_texture_env_combine3;	// Radeon
    public static boolean ATIX_texture_env_route;	// Radeon
    public static boolean ARB_multitexture;		// TNT, GeForce, Rage 128, Radeon
    public static boolean ARB_texture_env_combine;	// GeForce, Rage 128, Radeon
    public static boolean ARB_texture_env_crossbar;	// Radeon (GeForce supports it, but doesn't report it)
    public static boolean EXT_texture_env_combine;	// TNT, GeForce, Rage 128, Radeon
    public static boolean EXT_secondary_color;		// GeForce, Radeon
    public static boolean NV_texture_env_combine4;	// TNT, GeForce
    public static boolean NV_register_combiners;	// GeForce
    public static int maxTextureUnits;                  // TNT = 2, GeForce = 2-4, Rage 128 = 2, Radeon = 3-6
    
    public static interface CompiledCombiner {
        public void set(GL gl, Combiners combiner);
        public void updateColors(GL gl);
    }
    
    public static class CombinerOp {
        int op;
        int param1;
        int param2;
        int param3;
    };
    
    public static class CombinerStage {
        
        int numOps;
        CombinerOp[] op = new CombinerOp[6];
        
        public CombinerStage() {
            for (int i=0; i<6; i++)
                op[i] = new CombinerOp();
        }
        
        public void simplifyCycle(CombineCycle cc) { CombinerStages.simplify(this, cc); }
        
    };
    
    public static class Combiner {
        
        public int numStages;
        public CombinerStage[] stage = new CombinerStage[2];
        
        public void mergeStages() { CombinerStages.merge(this); }
        
    };
    
    public static class CachedCombiner {
        
        public gDPCombine combine = new gDPCombine();
        public boolean twoCycle;
        public CompiledCombiner compiled;
        public CachedCombiner left;
        public CachedCombiner right;
        
    };
    
    public static class Vertex {
            public int color;
            public int secondaryColor;
            public int alpha;
        };
    
    public static class CombineCycle {
        int sa, sb, m, a;
    };
    
    public static class PrimColor {
        public int m;
        public float l, r, g, b, a;
    };
    
    public static class gDPCombine {
        // muxs1                // 32
        public int	aA1;	//: 3;
        public int	sbA1;	//: 3;
        public int	aRGB1;	//: 3;
        public int	aA0;	//: 3;
        public int	sbA0;	//: 3;
        public int	aRGB0;	//: 3;
        public int	mA1;	//: 3;
        public int	saA1;	//: 3;
        public int	sbRGB1;	//: 4;
        public int	sbRGB0;	//: 4;
        
        // muxs0                // 24
        public int	mRGB1;	//: 5;
        public int	saRGB1;	//: 4;
        public int	mA0;	//: 3;
        public int	saA0;	//: 3;
        public int	mRGB0;	//: 5;
        public int	saRGB0;	//: 4;
        
        public int pad8;
        
        public void setMux(long value) {
            setMuxs0((int)(value>>32));
            setMuxs1((int)value);
        }
        
        public long getMux() {
            return (((long)getMuxs0())<<32) | (getMuxs1()&0xFFFFFFFFL);
        }
        
        public void setMuxs1(int value) {
            sbRGB0 = (value>>28)&15;
            sbRGB1 = (value>>24)&15;
            saA1 = (value>>21)&7;
            mA1 = (value>>18)&7;
            aRGB0 = (value>>15)&7;
            sbA0 = (value>>12)&7;
            aA0 = (value>>9)&7;
            aRGB1 = (value>>6)&7;
            sbA1 = (value>>3)&7;
            aA1 = (value)&7;
        }
        
        public int getMuxs1() {
            return ((sbRGB0&15)<<28)
            | ((sbRGB1&15)<<24) | ((saA1&7)<<21) | ((mA1&7)<<18) | ((aRGB0&7)<<15)
            | ((sbA0&7)<<12) | ((aA0&7)<<9) | ((aRGB1&7)<<6) | ((sbA1&7)<<3)
            | (aA1&7);
        }
        
        public void setMuxs0(int value) {
            pad8 = (value>>24)&0xFF;
            saRGB0 = (value>>20)&15;
            mRGB0 = (value>>15)&31;
            saA0 = (value>>12)&7;
            mA0 = (value>>9)&7;
            saRGB1 = (value>>5)&15;
            mRGB1 = (value)&31;
        }
        
        public int getMuxs0() {
            return ((pad8&0xFF)<<24) | ((saRGB0&15)<<20)
            | ((mRGB0&31)<<15) | ((saA0&7)<<12) | ((mA0&7)<<9)
            | ((saRGB1&15)<<5) | (mRGB1&31);
        }
    };
    
    public Vertex vertex = new Vertex();
    public CachedCombiner root;
    public CachedCombiner current;
    private int compiler;
    public boolean usesT0;
    public boolean usesT1;
    public boolean usesNoise;
    
    // used by TextureEnvCombiner, OpenGl, Combiners
    public static float[] envColor = new float[4];
    // used by TextureEnvCombiner, Combiners
    public static PrimColor primColor = new PrimColor();
    
    public gDPCombine combine = new gDPCombine();

    // called by OpenGl
    /** Generalized RDP operands, shared with programmable rendering backends. */
    public static int[] expandMux(long mux) {
        gDPCombine mode = new gDPCombine(); mode.setMux(mux);
        return new int[] {
            saRGBExpanded[mode.saRGB0], sbRGBExpanded[mode.sbRGB0], mRGBExpanded[mode.mRGB0], aRGBExpanded[mode.aRGB0],
            saAExpanded[mode.saA0], sbAExpanded[mode.sbA0], mAExpanded[mode.mA0], aAExpanded[mode.aA0],
            saRGBExpanded[mode.saRGB1], sbRGBExpanded[mode.sbRGB1], mRGBExpanded[mode.mRGB1], aRGBExpanded[mode.aRGB1],
            saAExpanded[mode.saA1], sbAExpanded[mode.sbA1], mAExpanded[mode.mA1], aAExpanded[mode.aA1]
        };
    }

    public void init() {
        if (NV_register_combiners) {
            compiler = NV_REGISTER_COMBINERS;
            NVRegisterCombiner.init();
        }
        else if (EXT_texture_env_combine || ARB_texture_env_combine) {
            compiler = TEXTURE_ENV_COMBINE;
            TextureEnvCombiner.init();
        }
        else {
            compiler = TEXTURE_ENV;
            TextureEnv.init();
        }
        root = null;
    }

    public void updateCombineColors(GL gl) {
        current.compiled.updateColors(gl);
//        Gdp.changed &= ~Gdp.CHANGED_COMBINE_COLORS;
    }

    public void endTextureUpdate(GL gl) {
        current.compiled.set(gl, this);
    }

    public void setCombine(GL gl, boolean twocycle, long mux) {
        selectCombine(twocycle, mux);
        setCombineStates(gl);
    }

    public void beginTextureUpdate(GL gl) {
        switch (compiler) {
            case TEXTURE_ENV_COMBINE:
                TextureEnvCombiner.BeginTextureUpdate_texture_env_combine(gl);
                break;
        }
    }

    private void selectCombine(boolean twocycle, long mux) {
        CachedCombiner comb = root;
        CachedCombiner parent = comb;

        while (comb != null) {
            parent = comb;

            int order = Long.compare(mux, comb.combine.getMux());
            if (order == 0) order = Boolean.compare(twocycle, comb.twoCycle);
            if (order == 0)
                break;
            else if (order < 0)
                comb = comb.left;
            else
                comb = comb.right;
        }

        if (comb == null) {
            comb = compile(twocycle, mux);

            if (parent == null)
                root = comb;
            else if (mux < parent.combine.getMux() ||
                    (mux == parent.combine.getMux() && !twocycle && parent.twoCycle))
                parent.left = comb;
            else
                parent.right = comb;
        }

        current = comb;

//        Gdp.changed |= Gdp.CHANGED_COMBINE_COLORS;
    }

    private void setCombineStates(GL gl) {
        current.compiled.set(gl, this);
    }


    private CachedCombiner compile(boolean twocycle, long mux) {
        gDPCombine newcombine = new gDPCombine();

        newcombine.setMux(mux);

        int numCycles;

        Combiner color = new Combiner();
        Combiner alpha = new Combiner();


        if (twocycle) {
            numCycles = 2;
            color.numStages = 2;
            alpha.numStages = 2;
        } else {
            numCycles = 1;
            color.numStages = 1;
            alpha.numStages = 1;
        }

        CombineCycle[] cc = new CombineCycle[2];
        CombineCycle[] ac = new CombineCycle[2];

        // Decode and expand the combine mode into a more general form
        cc[0] = new CombineCycle();
        cc[0].sa = saRGBExpanded[newcombine.saRGB0];
        cc[0].sb = sbRGBExpanded[newcombine.sbRGB0];
        cc[0].m = mRGBExpanded[newcombine.mRGB0];
        cc[0].a = aRGBExpanded[newcombine.aRGB0];
        ac[0] = new CombineCycle();
        ac[0].sa = saAExpanded[newcombine.saA0];
        ac[0].sb = sbAExpanded[newcombine.sbA0];
        ac[0].m = mAExpanded[newcombine.mA0];
        ac[0].a = aAExpanded[newcombine.aA0];

        cc[1] = new CombineCycle();
        cc[1].sa = saRGBExpanded[newcombine.saRGB1];
        cc[1].sb = sbRGBExpanded[newcombine.sbRGB1];
        cc[1].m = mRGBExpanded[newcombine.mRGB1];
        cc[1].a = aRGBExpanded[newcombine.aRGB1];
        ac[1] = new CombineCycle();
        ac[1].sa = saAExpanded[newcombine.saA1];
        ac[1].sb = sbAExpanded[newcombine.sbA1];
        ac[1].m = mAExpanded[newcombine.mA1];
        ac[1].a = aAExpanded[newcombine.aA1];

        for (int i = 0; i < numCycles; i++) {
            color.stage[i] = new CombinerStage();
            alpha.stage[i] = new CombinerStage();
            // Simplify each RDP combiner cycle into a combiner stage
            color.stage[i].simplifyCycle(cc[i]);
            alpha.stage[i].simplifyCycle(ac[i]);
        }

        if (numCycles == 2) {
            // Attempt to merge the two stages into one
            color.mergeStages();
            alpha.mergeStages();
        }

        CachedCombiner cached = new CachedCombiner();

        cached.combine.setMux(newcombine.getMux());
        cached.twoCycle = twocycle;
        cached.left = null;
        cached.right = null;

        // Send the simplified combiner to the hardware-specific compiler
        switch (compiler) {
            case TEXTURE_ENV:
                cached.compiled = new TextureEnv(color, alpha);
                break;

            case TEXTURE_ENV_COMBINE:
                cached.compiled = new TextureEnvCombiner(color, alpha);
                break;

            case NV_REGISTER_COMBINERS:
                cached.compiled = new NVRegisterCombiner(color, alpha);
                break;
        }

        return cached;
    }
    
    // called by OpenGl, FrameBuffers
    public final long encodeCombineMode(
            int ccmux_a0, int ccmux_b0, int ccmux_c0, int ccmux_d0,
            int acmux_a0, int acmux_b0, int acmux_c0, int acmux_d0,
            int ccmux_a1, int ccmux_b1, int ccmux_c1, int ccmux_d1,
            int acmux_a1, int acmux_b1, int acmux_c1, int acmux_d1) {
        return (long)(((long)(((ccmux_a0&SR_MASK_4)<<20) | ((ccmux_c0&SR_MASK_5)<<15) |
                ((acmux_a0&SR_MASK_3)<<12) | ((acmux_c0&SR_MASK_3)<<9) |
                ((ccmux_a1&SR_MASK_4)<<5) | ((ccmux_c1&SR_MASK_5)<<0)) << 32) |
                (long)(((ccmux_b0&SR_MASK_4)<<28) | ((ccmux_d0&SR_MASK_3)<<15) |
                ((acmux_b0&SR_MASK_3)<<12) | ((acmux_d0&SR_MASK_3)<<9) |
                ((ccmux_b1&SR_MASK_4)<<24) | ((acmux_a1&SR_MASK_3)<<21) |
                ((acmux_c1&SR_MASK_3)<<18) | ((ccmux_d1&SR_MASK_3)<<6) |
                ((acmux_b1&SR_MASK_3)<<3) | ((acmux_d1&SR_MASK_3)<<0)));
    }
    
    // called by OpenGl, TextureEnvCombine
    public final void setConstant(FloatBuffer constant, int color, int alpha) {
        switch (color) {
            case PRIMITIVE:
                constant.put(0, primColor.r);
                constant.put(1, primColor.g);
                constant.put(2, primColor.b);
                break;
            case ENVIRONMENT:
                constant.put(0, envColor[0]);
                constant.put(1, envColor[1]);
                constant.put(2, envColor[2]);
                break;
            case PRIMITIVE_ALPHA:
                constant.put(0, primColor.a);
                constant.put(1, primColor.a);
                constant.put(2, primColor.a);
                break;
            case ENV_ALPHA:
                constant.put(0, envColor[3]);
                constant.put(1, envColor[3]);
                constant.put(2, envColor[3]);
                break;
            case PRIM_LOD_FRAC:
                constant.put(0, primColor.l);
                constant.put(1, primColor.l);
                constant.put(2, primColor.l);
                break;
            case ONE:
                constant.put(0, 1.0f);
                constant.put(1, 1.0f);
                constant.put(2, 1.0f);
                break;
            case ZERO:
                constant.put(0, 0.0f);
                constant.put(1, 0.0f);
                constant.put(2, 0.0f);
                break;
        }
        switch (alpha) {
            case PRIMITIVE_ALPHA:
                constant.put(3, primColor.a);
                break;
            case ENV_ALPHA:
                constant.put(3, envColor[3]);
                break;
            case PRIM_LOD_FRAC:
                constant.put(3, primColor.l);
                break;
            case ONE:
                constant.put(3, 1.0f);
                break;
            case ZERO:
                constant.put(3, 0.0f);
                break;
        }
    }


    // called by OpenGl
    public final void setConstant(float[] constant, int color, int alpha) {
        switch (color) {
            case PRIMITIVE:
                constant[0] = primColor.r;
                constant[1] = primColor.g;
                constant[2] = primColor.b;
                break;
            case ENVIRONMENT:
                constant[0] = envColor[0];
                constant[1] = envColor[1];
                constant[2] = envColor[2];
                break;
            case PRIMITIVE_ALPHA:
                constant[0] = primColor.a;
                constant[1] = primColor.a;
                constant[2] = primColor.a;
                break;
            case ENV_ALPHA:
                constant[0] = envColor[3];
                constant[1] = envColor[3];
                constant[2] = envColor[3];
                break;
            case PRIM_LOD_FRAC:
                constant[0] = primColor.l;
                constant[1] = primColor.l;
                constant[2] = primColor.l;
                break;
            case ONE:
                constant[0] = 1.0f;
                constant[1] = 1.0f;
                constant[2] = 1.0f;
                break;
            case ZERO:
                constant[0] = 0.0f;
                constant[1] = 0.0f;
                constant[2] = 0.0f;
                break;
        }
        switch (alpha) {
            case PRIMITIVE_ALPHA:
                constant[3] = primColor.a;
                break;
            case ENV_ALPHA:
                constant[3] = envColor[3];
                break;
            case PRIM_LOD_FRAC:
                constant[3] = primColor.l;
                break;
            case ONE:
                constant[3] = 1.0f;
                break;
            case ZERO:
                constant[3] = 0.0f;
                break;
        }
    }
    public void reset() {
        root = null;
        current = null;
        usesT0 = false;
        usesT1 = false;
        usesNoise = false;

        primColor.m = 0;
        primColor.l = 0f;
        primColor.r = 0f;
        primColor.g = 0f;
        primColor.b = 0f;
        primColor.a = 0f;

        envColor[0] = 0f;
        envColor[1] = 0f;
        envColor[2] = 0f;
        envColor[3] = 0f;
    }

    /**
     * Creates a new instance of Combiners
     */
    public Combiners() {
    }
    
}
