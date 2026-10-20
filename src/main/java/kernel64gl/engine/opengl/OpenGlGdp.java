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


package kernel64gl.engine.opengl;
import kernel64gl.engine.HlePlugin;
import kernel64gl.engine.config.RendererSettings;
import kernel64gl.engine.core.Gbi;
import kernel64gl.engine.core.Rsp;

import com.sun.opengl.util.BufferUtil;
import kernel64gl.engine.debug.Debug;
import kernel64gl.engine.rdp.combiners.Combiners;
import kernel64gl.engine.rdp.Gdp;
import kernel64gl.engine.rdp.textures.TextureCache;
import java.nio.FloatBuffer;
import java.util.Random;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import com.sun.opengl.util.j2d.TextRenderer;


public class OpenGlGdp {
    
    public class hDC {

	}

	private static final int SIZEOF_FLOAT = 4;
    private static final int SIZEOF_GLVERTEX = 13 * SIZEOF_FLOAT;
    
    static class GLSimpleVertex {
        public float x, y, z, w;
        

        public float[] color = new float[4]; // r,g,b,a
        public float[] secondaryColor = new float[4]; // r,g,b,a
        
        public float s0, t0, s1, t1;
        
        public float fog;
    };
    static TextRenderer osdText;

    static class GLVertex {
        public FloatBuffer vtx; // 4
        public FloatBuffer color; // 4
        public FloatBuffer secondaryColor; // 4
        public FloatBuffer tex0; // 2
        public FloatBuffer tex1; // 2
        public FloatBuffer fog; // 1
        
        public GLVertex() { }
        
        public GLVertex(float x, float y, float z, float w,
                float r1, float g1, float b1, float a1,
                float r2, float g2, float b2, float a2,
                float s0, float t0, float s1, float t1, float f) {
            vtx = FloatBuffer.allocate(4);
            vtx.put(x);
            vtx.put(y);
            vtx.put(z);
            vtx.put(w);
            color = FloatBuffer.allocate(4);
            color.put(r1);
            color.put(g1);
            color.put(b1);
            color.put(a1);
            secondaryColor = FloatBuffer.allocate(4);
            secondaryColor.put(r2);
            secondaryColor.put(g2);
            secondaryColor.put(b2);
            secondaryColor.put(a2);
            tex0 = FloatBuffer.allocate(2);
            tex0.put(s0);
            tex0.put(t0);
            tex1 = FloatBuffer.allocate(2);
            tex1.put(s1);
            tex1.put(t1);
            fog = FloatBuffer.allocate(1);
            fog.put(f);
        }
    };


    public static GL gl;
 
    // used by Gdp
    public static TextureCache cache = new TextureCache();
    public static Combiners combiners = new Combiners();
    public static int screenWidth;
    public static int screenHeight;
    
    // used by GLN64Plugin.processDlist, OpenGl.OGLStart
    public static GLAutoDrawable hDC;
    
    static int numTriangles;
    static GLVertex[] vertices = new GLVertex[1536];
    private static FloatBuffer bigArray;
    static float scaleX;
    static float scaleY;
    static float zDepth;
    static int numVertices;
    static float nearZ;
    private static float vTrans;
    private static float vScale;
    static int width;
    static int height;
    static int heightOffset;
    static boolean usePolygonStipple;
    static byte[][][] stipplePattern = new byte[32][8][128];
    static int lastStipple;
    static final GLSimpleVertex rect0 = new GLSimpleVertex();
    static final GLSimpleVertex rect1 = new GLSimpleVertex();
    private static int windowedWidth;
    private static int windowedHeight;
 // --- CPU/GFX usage overlay ---
    public static boolean showCpuUsage = false;
    public static float cpuUsage = 0f;
    public static float gfxUsage = 0f;
    public static float rspUsage = 0f;
    public static float audioUsage = 0f;

    public static void init(GL agl) {
        gl = agl;
        osdText = new TextRenderer(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14), true, true);

        for (int i = 0; i < 1536; i++)
            vertices[i] = new GLVertex();
        bigArray = BufferUtil.newFloatBuffer(1536 * 13);
        for (int i = 0; i < 1536; i++) {
            bigArray.position(i * 13);
            vertices[i].vtx = bigArray.slice();
            bigArray.position(i * 13 + 4);
            vertices[i].color = bigArray.slice();
            bigArray.position(i * 13 + 8);
            vertices[i].secondaryColor = FloatBuffer.allocate(4);
            bigArray.position(i * 13 + 8);
            vertices[i].tex0 = bigArray.slice();
            bigArray.position(i * 13 + 10);
            vertices[i].tex1 = bigArray.slice();
            bigArray.position(i * 13 + 12);
            vertices[i].fog = bigArray.slice();
        }

        if (Debug.DEBUG_OGL) System.out.println("GL_NV_register_combiners: " + Combiners.NV_register_combiners);
        Combiners.ARB_multitexture = gl.glGetString(GL.GL_EXTENSIONS).contains("GL_ARB_multitexture");
        if (Debug.DEBUG_OGL) System.out.println("GL_ARB_multitexture: " + Combiners.ARB_multitexture);
        if (Combiners.ARB_multitexture) {
            int[] maxTextureUnits_t = new int[1];
            gl.glGetIntegerv(GL.GL_MAX_TEXTURE_UNITS, maxTextureUnits_t, 0);
            Combiners.maxTextureUnits = StrictMath.min(8, maxTextureUnits_t[0]); // The plugin only supports 8, and 4 is really enough
        }
        Combiners.EXT_fog_coord = gl.glGetString(GL.GL_EXTENSIONS).contains("GL_EXT_fog_coord");
        if (Debug.DEBUG_OGL) System.out.println("GL_EXT_fog_coord: " + Combiners.EXT_fog_coord);
        Combiners.EXT_secondary_color = false; // GLSL uses shade plus uniforms, never secondary color.
        if (Debug.DEBUG_OGL) System.out.println("GL_EXT_secondary_color: " + Combiners.EXT_secondary_color);
        Combiners.ARB_texture_env_combine = gl.glGetString(GL.GL_EXTENSIONS).contains("GL_ARB_texture_env_combine");
        if (Debug.DEBUG_OGL) System.out.println("GL_ARB_texture_env_combine: " + Combiners.ARB_texture_env_combine);
        Combiners.ARB_texture_env_crossbar = gl.glGetString(GL.GL_EXTENSIONS).contains("GL_ARB_texture_env_crossbar");
        if (Debug.DEBUG_OGL) System.out.println("GL_ARB_texture_env_crossbar: " + Combiners.ARB_texture_env_crossbar);
        Combiners.EXT_texture_env_combine = gl.glGetString(GL.GL_EXTENSIONS).contains("GL_EXT_texture_env_combine");
        if (Debug.DEBUG_OGL) System.out.println("GL_EXT_texture_env_combine: " + Combiners.EXT_texture_env_combine);
        Combiners.ATI_texture_env_combine3 = gl.glGetString(GL.GL_EXTENSIONS).contains("GL_ATI_texture_env_combine3");
        if (Debug.DEBUG_OGL) System.out.println("GL_ATI_texture_env_combine3: " + Combiners.ATI_texture_env_combine3);
        if (Debug.DEBUG_OGL) System.out.println("GL_ATIX_texture_env_route: " + Combiners.ATIX_texture_env_route);
        if (Debug.DEBUG_OGL) System.out.println("GL_NV_texture_env_combine4: " + Combiners.NV_texture_env_combine4);

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        bigArray.position(0);
        gl.glVertexPointer(4, GL.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        bigArray.position(4);
        gl.glColorPointer(4, GL.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        if (Combiners.EXT_secondary_color) {
            bigArray.position(8);
            gl.glSecondaryColorPointerEXT(3, GL.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
            gl.glEnableClientState(GL.GL_SECONDARY_COLOR_ARRAY_EXT);
        }
        if (Combiners.ARB_multitexture) {
            gl.glClientActiveTexture(GL.GL_TEXTURE0);
            bigArray.position(8);
            gl.glTexCoordPointer(2, GL.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
            gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
            gl.glClientActiveTexture(GL.GL_TEXTURE1);
            bigArray.position(10);
            gl.glTexCoordPointer(2, GL.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
            gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
        } else {
            bigArray.position(8);
            gl.glTexCoordPointer(2, GL.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
            gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
        }
        if (Combiners.EXT_fog_coord) {
            gl.glFogi(GL.GL_FOG_COORDINATE_SOURCE_EXT, GL.GL_FOG_COORDINATE_EXT);
            gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
            gl.glFogf(GL.GL_FOG_START, 0.0f);
            gl.glFogf(GL.GL_FOG_END, 255.0f);
            bigArray.position(12);
            gl.glFogCoordPointerEXT(GL.GL_FLOAT, SIZEOF_GLVERTEX, bigArray.slice());
            gl.glEnableClientState(GL.GL_FOG_COORDINATE_ARRAY_EXT);
        }
        gl.glPolygonOffset(-3.0f, -3.0f);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        
        Random rand = new Random();
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 8; j++) {
                for (int k = 0; k < 128; k++) {
                    stipplePattern[i][j][k] = (byte) (((i > (rand.nextInt() >>> 10) ? 1 : 0) << 7) |
                            ((i > (rand.nextInt() >>> 10) ? 1 : 0) << 6) |
                            ((i > (rand.nextInt() >>> 10) ? 1 : 0) << 5) |
                            ((i > (rand.nextInt() >>> 10) ? 1 : 0) << 4) |
                            ((i > (rand.nextInt() >>> 10) ? 1 : 0) << 3) |
                            ((i > (rand.nextInt() >>> 10) ? 1 : 0) << 2) |
                            ((i > (rand.nextInt() >>> 10) ? 1 : 0) << 1) |
                            ((i > (rand.nextInt() >>> 10) ? 1 : 0) << 0));
                }
            }
        }
        usePolygonStipple = false;
        cache.init(gl, HlePlugin.RDRAM, HlePlugin.TMEM, Combiners.maxTextureUnits, Combiners.ARB_multitexture);
        combiners.init();
        OGL_UpdateScale();
    }
    
    // called by OpenGl.config
    public static void config() {
        cache.config(RendererSettings.cacheMiB * 1048576, RendererSettings.textureDepth);
        windowedWidth = RendererSettings.windowWidth;
        windowedHeight = RendererSettings.windowHeight;
    }
    
    public static void setDrawableSize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        windowedWidth = width = newWidth;
        windowedHeight = height = newHeight;
        if (Rsp.gdp != null) Rsp.gdp.changed |= Gdp.CHANGED_SCISSOR | Gdp.CHANGED_VIEWPORT;
    }

    // called by GLN64Plugin.romOpen
    public static void OGL_ResizeWindow() {
        width = windowedWidth;
        height = windowedHeight;
    }
    
    // called by OpenGl.display
    public static void OGL_UpdateScale() {
        scaleX = width / (float)screenWidth;
        scaleY = height / (float)screenHeight;
    }
    
    // called by OpenGl.display
    public static void VI_UpdateSize() {
        float xScale = (HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_X_SCALE]&Gbi.SR_MASK_12) * Gbi.FIXED2FLOATRECIP10;
//        float xOffset = ((HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_X_SCALE]>>16)&Gbi.SR_MASK_12) * Gbi.FIXED2FLOATRECIP10;
        
        float yScale = (HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_Y_SCALE]&Gbi.SR_MASK_12) * Gbi.FIXED2FLOATRECIP10;
//        float yOffset = ((HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_X_SCALE]>>16)&Gbi.SR_MASK_12) * Gbi.FIXED2FLOATRECIP10;
        
        int hEnd = HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_H_START]&Gbi.SR_MASK_10;
        int hStart = (HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_H_START]>>16)&Gbi.SR_MASK_10;
        
        // These are in half-lines, so shift an extra bit
        int vEnd = (HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_V_START]>>1)&Gbi.SR_MASK_9;
        int vStart = (HlePlugin.REG.VI_Registers[HlePlugin.REG.VI_V_START]>>17)&Gbi.SR_MASK_9;
        zDepth = 0.0f; 
        
        screenWidth = (int)((hEnd - hStart) * xScale);
        screenHeight = (int)((vEnd - vStart) * yScale * 1.0126582f);
        
        if (screenWidth == 0)
            screenWidth = 320;
        if (screenHeight == 0)
            screenHeight = 240;
    }
    
    // called by HlePlugin.updateScreen
    public static void viUpdateScreen() {

        if ((Rsp.gdp.changed & Gdp.CHANGED_COLORBUFFER) != 0) {
            hDC.swapBuffers();
            Rsp.gdp.changed &= ~Gdp.CHANGED_COLORBUFFER;
        }


    }
    
    // called by Gsp.viewport
    public static void setView(float trans, float scale) {
        vTrans = trans;
        vScale = scale;
        nearZ = trans - scale;
        Rsp.gdp.changed |= Gdp.CHANGED_VIEWPORT;
    }

    /** Restore the RSP viewport after a screen-space rectangle. */
    static void updateViewport() {
        float halfWidth = Math.abs(Rsp.gsp.viewport.vscale[0]);
        float halfHeight = Math.abs(Rsp.gsp.viewport.vscale[1]);
        if (halfWidth == 0 || halfHeight == 0) {
            gl.glViewport(0, heightOffset, width, height);
        } else {
            gl.glViewport((int) ((Rsp.gsp.viewport.vtrans[0] - halfWidth) * scaleX),
                    (int) ((screenHeight - Rsp.gsp.viewport.vtrans[1] - halfHeight) * scaleY + heightOffset),
                    (int) (halfWidth * 2 * scaleX), (int) (halfHeight * 2 * scaleY));
        }
        gl.glDepthRange(0.0, 1.0);
    }
    
    // called by Gdp.setPrimDepth
    public static void setZDepth(int z) {
        zDepth = StrictMath.min(1.0f, StrictMath.max(0.0f, ((z * Gbi.FIXED2FLOATRECIP15) - vTrans) / vScale));
    }
    
    // called by Gdp.setDepthImage
    public static void OGL_ClearDepthBuffer(boolean depthUpdate) {
        OpenGl.OGL_GspUpdateStates();
        
        OGL_GdpUpdateStates();
        gl.glDepthMask(true);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        gl.glDepthMask(depthUpdate);
    }
    
    // called by Gdp.fillRectangle
    public static void OGL_ClearColorBuffer(float[] color) {
        gl.glClearColor(color[0], color[1], color[2], 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }
    
    // called by Gdp.fillRectangle
    // uses screenWidth, screenHeight, width, height, heightOffset, zDepth, nearZ
	public static void OGL_DrawRect(int ulx, int uly, int lrx, int lry, float[] color, int depthSource) {
		OpenGlPrimitives.OGL_DrawRect(ulx, uly, lrx, lry, color, depthSource);
	}
    
    // called by TextureCache.drawTextureRectangle
    // uses screenWidth, screenHeight, width, height, heightOffset, zDepth, nearZ, rect0, rect1
	public static void OGL_DrawTexturedRect(float ulx, float uly, float lrx, float lry, float uls, float ult, float lrs, float lrt, boolean flip, int depthSource, int cycleType) {
		OpenGlPrimitives.OGL_DrawTexturedRect(ulx, uly, lrx, lry, uls, ult, lrs, lrt, flip, depthSource, cycleType);
	}
    
    // called by Gsp.gSPLine3D
    // uses scaleX, combiners
	public static void OGL_DrawLine(float[] vtx1, float[] c1, float[] vtx2, float[] c2, float width) {
		OpenGlPrimitives.OGL_DrawLine(vtx1, c1, vtx2, c2, width);
	}
    
    // called by Gsp.gSPTriangle
    // uses vertices, numVertices, numTriangles, combiners, cache
	public static void OGL_AddTriangle(float[] vtx1, float[] c1, float[] tex1, float[] vtx2, float[] c2, float[] tex2, float[] vtx3, float[] c3, float[] tex3) {
		OpenGlPrimitives.OGL_AddTriangle(vtx1, c1, tex1, vtx2, c2, tex2, vtx3, c3, tex3);
	}
    
    // called by Gsp.gSPFlushTriangles
	public static void OGL_DrawTriangles() {
		OpenGlPrimitives.OGL_DrawTriangles();
	}
    
    // Private Methods /////////////////////////////////////////////////////////
    
	static void stipple() {
		OpenGlRenderState.stipple();
	}


    public static void resetRDP() {
        cache.reset();
        combiners.reset();
        numTriangles = 0;
        numVertices = 0;

        for (int i = 0; i < 1536; i++) {
            GLVertex v = vertices[i];
            v.vtx.put(0, 0f);
            v.vtx.put(1, 0f);
            v.vtx.put(2, 0f);
            v.vtx.put(3, 1f);
            v.color.put(0, 0f);
            v.color.put(1, 0f);
            v.color.put(2, 0f);
            v.color.put(3, 0f);
            v.secondaryColor.put(0, 0f);
            v.secondaryColor.put(1, 0f);
            v.secondaryColor.put(2, 0f);
            v.secondaryColor.put(3, 1f);
            v.tex0.put(0, 0f);
            v.tex0.put(1, 0f);
            v.tex1.put(0, 0f);
            v.tex1.put(1, 0f);
            v.fog.put(0, 0f);
        }
    }
    public static void resetGFX() {
        numTriangles = 0;
        numVertices = 0;
        zDepth = 0f;
        vTrans = 0f;
        vScale = 1f;

        screenWidth = 320;
        screenHeight = 240;
        width = windowedWidth;
        height = windowedHeight;
        heightOffset = 0;

        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
        gl.glDisable(GL.GL_FOG);

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glViewport(0, 0, width, height);
    }

	static void OGL_GdpUpdateStates() {
		OpenGlRenderState.OGL_GdpUpdateStates();
	}

}
