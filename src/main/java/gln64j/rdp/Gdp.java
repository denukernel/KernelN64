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


package gln64j.rdp;

import gln64j.opengl.OpenGlGdp;
import gln64j.core.Registers;
import gln64j.core.Gbi;
import gln64j.rdp.combiners.Combiners;


public class Gdp {
    
    public static final int CHANGED_RENDERMODE=		0x001;
    public static final int CHANGED_CYCLETYPE=		0x002;
    public static final int CHANGED_SCISSOR=		0x004;
    public static final int CHANGED_COLORBUFFER =	0x008;
    public static final int CHANGED_TILE=		0x010;
    public static final int CHANGED_COMBINE_COLORS=     0x020;
    public static final int CHANGED_COMBINE=		0x040;
    public static final int CHANGED_ALPHACOMPARE=       0x080;
    public static final int CHANGED_FOGCOLOR=		0x100;
    public static final int CHANGED_TEXTURE=            0x200;
    public static final int CHANGED_VIEWPORT =		0x400;
    
    public static class OtherMode {
        public int w0; // high
        public int w1; // low
        

    };
    public void reset() {
        changed = 0;
        otherMode.w0 = 0;
        otherMode.w1 = 0;

        fogColor[0] = 0f;
        fogColor[1] = 0f;
        fogColor[2] = 0f;
        fogColor[3] = 0f;

        blendColor[0] = 0f;
        blendColor[1] = 0f;
        blendColor[2] = 0f;
        blendColor[3] = 0f;

        scissor.mode = 0;
        scissor.ulx = 0f;
        scissor.uly = 0f;
        scissor.lrx = 0f;
        scissor.lry = 0f;

        primDepth.z = 0f;
        primDepth.deltaZ = 0f;

        colorImage.format = 0;
        colorImage.size = 0;
        colorImage.width = 0;
        colorImage.height = 0;
        colorImage.bpl = 0;
        colorImage.address = 0;
        colorImage.changed = false;
        colorImage.depthImage = 0;

        depthImageAddress = 0;
        current = null;
        depthBuffers.init();

        fillColor.color[0] = 0f;
        fillColor.color[1] = 0f;
        fillColor.color[2] = 0f;
        fillColor.color[3] = 0f;
        fillColor.z = 0f;
        fillColor.dz = 0f;
    }

    public static final int PIPELINE_MODE = 0x800000;
    public static final int CYCLE_TYPE = 0x300000;
    public static final int TEX_PERSP = 0x80000;
    public static final int TEX_DETAIL = 0x60000;
    public static final int TEX_LOD = 0x10000;
    public static final int TEX_LUT = 0xC000;
    public static final int TEX_FILTER = 0x3000;
    public static final int TEX_CONVERT = 0xE00;
    public static final int COMBINE_KEY = 0x100;
    public static final int COLOR_DITHER = 0xC0;
    public static final int ALPHA_DITHER = 0x30;
    public static final int DEPTH_SOURCE = 0x4;
    public static final int ALPHA_COMPARE = 0x3;
    
public static final int RDP_GETOM_CYCLE_TYPE(OtherMode om) {return(((om).w0 >> 20) & 0x3);}

public static final int RDP_GETOM_TLUT_TYPE(OtherMode om) {return(((om).w0 >> 14) & 0x3);}//& 0x04000)!=0 ? 1 : 0);}
public static final int RDP_GETOM_SAMPLE_TYPE(OtherMode om) {return(((om).w0 >> 12) & 0x3);}//& 0x02000)!=0 ? 1 : 0);}

public static final int RDP_GETOM_FORCE_BLEND(OtherMode om) {return(((om).w1 & 0x4000)!=0 ? 1 : 0);}
public static final int RDP_GETOM_ALPHA_CVG_SELECT(OtherMode om) {return(((om).w1 & 0x2000)!=0 ? 1 : 0);}
public static final int RDP_GETOM_CVG_TIMES_ALPHA(OtherMode om) {return(((om).w1 & 0x1000)!=0 ? 1 : 0);}
public static final int RDP_GETOM_Z_MODE(OtherMode om) {return(((om).w1 >> 10) & 0x3);}

public static final int RDP_GETOM_Z_UPDATE_EN(OtherMode om) {return(((om).w1 & 0x20)!=0 ? 1 : 0);}
public static final int RDP_GETOM_Z_COMPARE_EN(OtherMode om) {return(((om).w1 & 0x10)!=0 ? 1 : 0);}

public static final int RDP_GETOM_Z_SOURCE_SEL(OtherMode om) {return(((om).w1 & 0x04)!=0 ? 1 : 0);}

public static final int RDP_GETOM_ALPHA_COMPARE_EN(OtherMode om) {return(((om).w1 >> 0) & 0x3);}//& 0x01)!=0 ? 1 : 0);}

    
    public static class FillColor {
        public float[] color = new float[4];
        public float z;
        public float dz;
    };
    
    public static class PrimDepth {
        public float z;
        public float deltaZ;
    };
    
    public static class ColorImage {
        public int format;
        public int size;
        public int width;
        public int height;
        public int bpl;
        public int address;
        public boolean changed;
        public int depthImage;
    };
    
    public static class Scissor {
        public int mode;
        public float ulx;
        public float uly;
        public float lrx;
        public float lry;
    };
    
    public static class Convert {
        public int k0;
        public int k1;
        public int k2;
        public int k3;
        public int k4;
        public int k5;
    };
    
    public static class Key {
        public float[] center = new float[4];
        public float[] scale = new float[4];
        public float[] width = new float[4];
    };
    
    // used by Gsp.processDlist
    public int DList;
    // used by OpenGl.offsetFill/addTriangle, OpenGlGdp, Opcodes
    public OtherMode otherMode = new OtherMode();
    // used by OpenGL.init/addTriangle/drawLine, OpenGlGdp, Opcodes
    public int changed;
    // used by OpenGlGdp.updateStates
    public float[] fogColor = new float[4];
    public float[] blendColor = new float[4];
    public Scissor scissor = new Scissor();
    // used by OpenGlGdp.viUpdateScreen
    public ColorImage colorImage = new ColorImage();
    
    protected PrimDepth primDepth = new PrimDepth();
    protected DepthBufferStack depthBuffers = new DepthBufferStack();
    protected DepthBufferStack.DepthBuffer current;
    protected int depthImageAddress;

    protected FillColor fillColor = new FillColor();
    protected Runnable checkInterrupts;
    protected Registers reg;
    
    /** Creates a new instance of GDP */
    public Gdp(Runnable checkInterrupts, Registers regs) {
        this.checkInterrupts = checkInterrupts;
        this.reg = regs;
        
        OpenGlGdp.cache.construct();
        
        current = null;
        depthBuffers.init();
    }
    
 // ADDED: constructor that accepts kerneln64.cpu.Registers
    public Gdp(Runnable checkInterrupts, kerneln64.cpu.Registers jRegs) {
        this.checkInterrupts = checkInterrupts;

        // Create a new gln64j.core.Registers instance
        this.reg = new gln64j.core.Registers();

        // --- COPY MI REGISTERS (size = 4) ---
        this.reg.MI_Registers = new int[4];
        for (int i = 0; i < 4; i++) {
            this.reg.MI_Registers[i] = jRegs.regMI[i];
        }

        // --- COPY DPC REGISTERS (size = 10) ---
        this.reg.DPC_Registers = new int[10];
        for (int i = 0; i < 10; i++) {
            this.reg.DPC_Registers[i] = jRegs.regDPC[i];
        }

        // VI registers: gln64j.core.Registers only allocates the array,
        // so we initialize it to a safe default (all zeros)
        this.reg.VI_Registers = new int[14];

        // FINISH INITIALIZATION SAME AS DEFAULT CONSTRUCTOR
        OpenGlGdp.cache.construct();
        current = null;
        depthBuffers.init();
    }


	// called by F3d, F3dex2
    public void gDPTexture(float sc, float tc, int level, int tile, int on) {
        OpenGlGdp.cache.setTexture(sc, tc, level, tile, on);
        changed |= CHANGED_TEXTURE;
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPPipelineMode(int mode) {

        otherMode.w0 &= ~PIPELINE_MODE;
        otherMode.w0 |= ((mode & 0x1) << 23);
        
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetCycleType(int type) {

        otherMode.w0 &= ~CYCLE_TYPE;
        otherMode.w0 |= ((type & 0x3) << 20);
        changed |= CHANGED_CYCLETYPE;
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetTexturePersp(int enable) {

        otherMode.w0 &= ~TEX_PERSP;
        otherMode.w0 |= ((enable & 0x1) << 19);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetTextureDetail(int type) {

        otherMode.w0 &= ~TEX_DETAIL;
        otherMode.w0 |= ((type & 0x3) << 17);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetTextureLOD(int mode) {

        otherMode.w0 &= ~TEX_LOD;
        otherMode.w0 |= ((mode & 0x1) << 16);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetTextureLUT(int mode) {

        otherMode.w0 &= ~TEX_LUT;
        otherMode.w0 |= ((mode & 0x3) << 14);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetTextureFilter(int type) {

        otherMode.w0 &= ~TEX_FILTER;
        otherMode.w0 |= ((type & 0x3) << 12);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetTextureConvert(int type) {

        otherMode.w0 &= ~TEX_CONVERT;
        otherMode.w0 |= ((type & 0x7) << 9);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetCombineKey(int type) {

        otherMode.w0 &= ~COMBINE_KEY;
        otherMode.w0 |= ((type & 0x1) << 8);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetColorDither(int type) {

        otherMode.w0 &= ~COLOR_DITHER;
        otherMode.w0 |= ((type & 0x3) << 6);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetAlphaDither(int type) {

        otherMode.w0 &= ~ALPHA_DITHER;
        otherMode.w0 |= ((type & 0x3) << 4);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetAlphaCompare(int mode) {

        otherMode.w1 &= ~ALPHA_COMPARE;
        otherMode.w1 |= ((mode & 0x3) << 0);
        changed |= CHANGED_ALPHACOMPARE;
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetDepthSource(int source) {

        otherMode.w1 &= ~DEPTH_SOURCE;
        otherMode.w1 |= ((source & 0x1) << 2);
    }
    
    // called by Rsp, F3d, F3dex2
    public void gDPSetRenderMode(int mode1, int mode2) {

        otherMode.w1 = (otherMode.w1 & 0x00000007);
        otherMode.w1 = (otherMode.w1 | mode1 | mode2);
        changed |= CHANGED_RENDERMODE;
    }
    
    //////////////////////////////////////////////////////////////
    
    // called by Rdp
    public void gDPSetOtherMode(int mode0, int mode1) {

        otherMode.w0 = mode0;
        otherMode.w1 = mode1;
        changed |= CHANGED_RENDERMODE | CHANGED_CYCLETYPE | CHANGED_ALPHACOMPARE;
    }
    
    // called by Rdp
    public void gDPSetPrimDepth(int z, int dz) {
        // z rdpState.primitiveZ (0xfff)
        // dz rdpState.primitiveDeltaZ (w1 & 0xfff)
        primDepth.z = z;
        primDepth.deltaZ = dz;
        OpenGlGdp.setZDepth(z);
    }
    
    // called by Rdp
    public void gDPSetCombine(int muxs0, int muxs1) {
        // muxs0 rdpState.combineModes.w1
        // muxs1 rdpState.combineModes.w2
        OpenGlGdp.combiners.combine.setMuxs0(muxs0);
        OpenGlGdp.combiners.combine.setMuxs1(muxs1);
        changed |= CHANGED_COMBINE;
    }
    
    // called by Rdp
    public void gDPSetColorImage(int format, int size, int width, int address) {
        if(d3d11.framebuffer.D3DFramebuffers.enabled())OpenGlGdp.OGL_DrawTriangles();
        // rdpFbFormat
        // rdpFbSize
        // rdpFbWidth (0x3ff)
        // rdpFbAddress (w2 & 0x01ffffff)
        if (colorImage.address != address) {
            colorImage.changed = false;
            
            if (width == OpenGlGdp.screenWidth)
                colorImage.height = OpenGlGdp.screenHeight;
            else
                colorImage.height = 1;
        }
        
        colorImage.format = format;
        colorImage.size = size;
        colorImage.width = width;
        colorImage.address = address;
    }
    
    // called by Rdp
    public void gDPSetTextureImage(int format, int size, int width, int address) {
        // rdpTiFormat
        // rdpTiSize
        // rdpTiWidth (0x3ff)
        // rdpTiAddress (w2 & 0x01ffffff)
        OpenGlGdp.cache.setTextureImage(format, size, width, address);
    }
    
    // called by Rdp
    public int depthImageAddress() { return depthImageAddress; }

    public void gDPSetDepthImage(int address) {
        if(d3d11.framebuffer.D3DFramebuffers.enabled()){OpenGlGdp.OGL_DrawTriangles();d3d11.framebuffer.D3DFramebuffers.depthImage(address);}
        depthImageAddress=address;

        DepthBufferStack.DepthBuffer buffer = depthBuffers.top;
        // Search through saved depth buffers
        while (buffer != null) {
            if (buffer.address == address) {
                depthBuffers.moveToTop(buffer);
                current = buffer;
                return;
            }
            buffer = buffer.lower;
        }
        buffer = new DepthBufferStack.DepthBuffer();
        depthBuffers.addTop(buffer);
        buffer.address = address;
        buffer.cleared = true;
        current = buffer;
        if (current.cleared)

            OpenGlGdp.OGL_ClearDepthBuffer(RDP_GETOM_Z_UPDATE_EN(otherMode) != 0);
        depthImageAddress = address;
    }


    // called by Rdp
    public void gDPSetEnvColor(int r, int g, int b, int a) {

        Combiners.envColor[0] = r * 0.0039215689f; // r / 255
        Combiners.envColor[1] = g * 0.0039215689f; // g / 255
        Combiners.envColor[2] = b * 0.0039215689f; // b / 255
        Combiners.envColor[3] = a * 0.0039215689f; // a / 255
        changed |= CHANGED_COMBINE_COLORS;
    }
    
    // called by Rdp
    public void gDPSetBlendColor(int r, int g, int b, int a) {

        blendColor[0] = r * 0.0039215689f; // r / 255
        blendColor[1] = g * 0.0039215689f; // g / 255
        blendColor[2] = b * 0.0039215689f; // b / 255
        blendColor[3] = a * 0.0039215689f; // a / 255
    }
    
    // called by Rdp
    public void gDPSetFogColor(int r, int g, int b, int a) {

        fogColor[0] = r * 0.0039215689f; // r / 255
        fogColor[1] = g * 0.0039215689f; // g / 255
        fogColor[2] = b * 0.0039215689f; // b / 255
        fogColor[3] = a * 0.0039215689f; // a / 255
        changed |= CHANGED_FOGCOLOR;
    }
    
    // called by Rdp
    public void gDPSetFillColor(int c) {
        // rdpState.fillColor
        fillColor.color[0] = ((c >> 11) & 0x1f) * 0.032258064f;
        fillColor.color[1] = ((c >> 6) & 0x1f) * 0.032258064f;
        fillColor.color[2] = ((c >> 1) & 0x1f) * 0.032258064f;
        fillColor.color[3] = c & 0x1;
        fillColor.z = (c >> 2) & 0x3fff;
        fillColor.dz = c & 0x3;
    }
    
    // called by Rdp
    public void gDPSetPrimColor(int m, int l, int r, int g, int b, int a) {

        Combiners.primColor.m = m;
        Combiners.primColor.l = l * 0.0039215689f; // l / 255
        Combiners.primColor.r = r * 0.0039215689f; // r / 255
        Combiners.primColor.g = g * 0.0039215689f; // g / 255
        Combiners.primColor.b = b * 0.0039215689f; // b / 255
        Combiners.primColor.a = a * 0.0039215689f; // a / 255
        changed |= CHANGED_COMBINE_COLORS;
    }
    
    // called by Rdp
    public void gDPSetTile(int format, int size, int line, int tmem, int tilenum, int palette, int cmt, int cms, int maskt, int masks, int shiftt, int shifts) {
        // rdpTiles[tilenum]
        // format
        // size
        // line / 8
        // tmem / 8
        // tilenum
        // palette
        // mt/ct ((w2 >> 18) & 0x1) | ((w2 >> 19) & 0x1)
        // ms/cs ((w2 >> 8) & 0x1) | ((w2 >> 9) & 0x1)
        // mask_t
        // mask_s
        // shift_t
        // shift_s
        OpenGlGdp.cache.setTile(format, size, line, tmem, tilenum, palette, cmt, cms, maskt, masks, shiftt, shifts);
    }
    
    // called by Rdp
    public void gDPSetTileSize(int tilenum, int uls, int ult, int lrs, int lrt) {
        // rdpTiles[tilenum]
        // tilenum
        // uls sl
        // ult tl
        // lrs sh
        // lrt th
        OpenGlGdp.cache.setTileSize(tilenum,
                (uls >> 2) & 0x3ff,
                (ult >> 2) & 0x3ff,
                (lrs >> 2) & 0x3ff,
                (lrt >> 2) & 0x3ff,
                uls * 0.25f,  // uls / 4
                ult * 0.25f,  // ult / 4
                lrs * 0.25f,  // lrs / 4
                lrt * 0.25f); // lrt / 4
        changed |= CHANGED_TILE;
    }
    
    // called by Rdp
    public void gDPLoadTile(int tilenum, int uls, int ult, int lrs, int lrt) {
        // tilenum
        // uls sl * 4
        // ult tl * 4
        // lrs sh * 4
        // lrt th * 4
        gDPSetTileSize(tilenum, uls, ult, lrs, lrt);
        OpenGlGdp.cache.loadTile(tilenum);
    }
    
    // called by Rdp
    public void gDPLoadBlock(int tilenum, int uls, int ult, int lrs, int dxt) {
        // tilenum
        // uls sl
        // ult tl >> 11
        // lrs sh
        // dxt
        gDPSetTileSize(tilenum, uls, ult, lrs, dxt);
        OpenGlGdp.cache.loadBlock(tilenum, uls, ult, lrs, dxt);
    }
    
    // called by Rdp
    public void gDPLoadTLUT(int tilenum, int uls, int ult, int lrs, int lrt) {
        // rdpTiles[tilenum]
        // tilenum
        // uls sl
        // ult tl
        // lrs sh
        // lrt th
        gDPSetTileSize(tilenum, uls, ult, lrs, lrt);
        OpenGlGdp.cache.loadLUT(tilenum);
    }
    
    // called by Rdp
    public void gDPSetScissor(int mode, float ulx, float uly, float lrx, float lry) {
        // rdpState.clipMode
        // ulx rdpState.clip.xh * 4
        // uly rdpState.clip.yh * 4
        // lrx rdpState.clip.xl * 4
        // lry rdpState.clip.yl * 4
        scissor.mode = mode;
        scissor.ulx = ulx;
        scissor.uly = uly;
        scissor.lrx = lrx;
        scissor.lry = lry;
        changed |= CHANGED_SCISSOR;
    }
    
    // called by Rdp
	public void gDPFillRectangle(int ulx, int uly, int lrx, int lry) {
		GdpRectangles.gDPFillRectangle(this, ulx, uly, lrx, lry);
	}
 // Inside Gdp.java
	int clampX(int x) {
		return GdpRectangles.clampX(this, x);
	}
	int clampY(int y) {
		return GdpRectangles.clampY(this, y);
	}
    // called by Rdp
    public void gDPSetConvert(int k0, int k1, int k2, int k3, int k4, int k5) {

    }
    
    // called by Rdp
    public void gDPSetKeyR(int cR, int sR, int wR) {

    }
    
    // called by Rdp
    public void gDPSetKeyGB(int cG, int sG, int wG, int cB, int sB, int wB) {

    }
    
    public void update() {
        if (current != null)
            current.cleared = false;
        colorImage.changed = true;
        colorImage.height = (int)StrictMath.max(colorImage.height, scissor.lry);
    }
    
    // called by Rdp
	public void gDPTextureRectangle(float ulx, float uly, float lrx, float lry, int tilenum, float s, float t, float dsdx, float dtdy, boolean flip) {
		GdpRectangles.gDPTextureRectangle(this, ulx, uly, lrx, lry, tilenum, s, t, dsdx, dtdy, flip);
	}
    
    // called by Rdp
	public void gDPTextureRectangleFlip(float ulx, float uly, float lrx, float lry, int tilenum, float s, float t, float dsdx, float dtdy) {
		GdpRectangles.gDPTextureRectangleFlip(this, ulx, uly, lrx, lry, tilenum, s, t, dsdx, dtdy);
	}
    
    // called by Rdp
    public void gDPFullSync() {
        reg.MI_Registers[reg.MI_INTR] |= Registers.MI_INTR_DP;
        checkInterrupts.run();
    }
    
    // called by Rdp
    public void gDPTileSync() {

    }
    
    // called by Rdp
    public void gDPPipeSync() {

    }
    
    // called by Rdp
    public void gDPLoadSync() {

    }
    
    // called by Rdp
    public void gDPNoOp() {

    }
    
}
