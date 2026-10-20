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

import kernel64gl.engine.rsp.Gsp;
import kernel64gl.engine.rdp.combiners.Combiners;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.FloatBuffer;
import javax.swing.JFrame;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;


public class OpenGl {

//    private static final int SIZEOF_FLOAT = 4;
//    private static final int SIZEOF_GLVERTEX = 17 * SIZEOF_FLOAT;

	private static class GLVertex {
		public FloatBuffer vtx; // 4
		public FloatBuffer color; // 4
		public FloatBuffer secondaryColor; // 4
		public FloatBuffer tex0; // 2
		public FloatBuffer tex1; // 2
		public FloatBuffer fog; // 1

		public GLVertex() {
		}

		public GLVertex(float x, float y, float z, float w, float r1, float g1, float b1, float a1, float r2, float g2,
				float b2, float a2, float s0, float t0, float s1, float t1, float f) {
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

	private static class OGL_EventListener implements GLEventListener {

		public void init(GLAutoDrawable gLDrawable) {
			gl = gLDrawable.getGL();
            polygonMode=-1;depthTest=-1;
            if(kerneln64.Main.video!=null && kerneln64.Main.video.gfxPlugin instanceof HlePlugin plugin)
                plugin.configureContext(gl,true);


			OpenGlGdp.init(gl);

			Rsp.gsp.changed = Rsp.gdp.changed = 0xFFFFFFFF;

		}

		public void display(GLAutoDrawable gLDrawable) {
			try {
                if(kerneln64.Main.video!=null && kerneln64.Main.video.gfxPlugin instanceof HlePlugin plugin)
                    plugin.configureContext(gLDrawable.getGL(),false);
				// 1. CLEAR the buffers immediately to stop the "ghosting" effect

                renderDisplayList();

				// -------------------------------------
				// ⭐ ADD OSD TEXT RENDERING HERE ⭐
				// -------------------------------------
				if (OpenGlGdp.showCpuUsage) {
					OpenGlGdp.osdText.beginRendering(OpenGlGdp.screenWidth, OpenGlGdp.screenHeight);
					OpenGlGdp.osdText.setColor(1f, 1f, 1f, 1f);

					OpenGlGdp.osdText.draw(String.format("CPU:   %.1f%%", OpenGlGdp.cpuUsage), 10,
							OpenGlGdp.screenHeight - 20);
					OpenGlGdp.osdText.draw(String.format("GFX:   %.1f%%", OpenGlGdp.gfxUsage), 10,
							OpenGlGdp.screenHeight - 40);
					OpenGlGdp.osdText.draw(String.format("RSP:   %.1f%%", OpenGlGdp.rspUsage), 10,
							OpenGlGdp.screenHeight - 60);
					OpenGlGdp.osdText.draw(String.format("Audio: %.1f%%", OpenGlGdp.audioUsage), 10,
							OpenGlGdp.screenHeight - 80);

					OpenGlGdp.osdText.endRendering();
				}
				// -------------------------------------

			} catch (Exception e) {
                throw new IllegalStateException("Graphics rendering failed", e);
			}
		}

		public void reshape(GLAutoDrawable gLDrawable, int i, int i0, int i1, int i2) {
            OpenGlGdp.setDrawableSize(i1, i2);
		}

		public void displayChanged(GLAutoDrawable gLDrawable, boolean b, boolean b0) {
		}

	};

    /** Shared HLE display-list execution; the drawing implementation supplies GL. */
    public static void renderDisplayList() {
				OpenGlGdp.VI_UpdateSize();
				OpenGlGdp.OGL_UpdateScale();

				Rsp.gdp.gDPSetAlphaCompare(Gbi.G_AC_NONE);
				Rsp.gdp.gDPSetDepthSource(Gbi.G_ZS_PIXEL);
				Rsp.gdp.gDPSetRenderMode(0, 0);

				Rsp.gdp.gDPSetAlphaDither(Gbi.G_AD_DISABLE);
				Rsp.gdp.gDPSetColorDither(Gbi.G_CD_DISABLE);
				Rsp.gdp.gDPSetCombineKey(Gbi.G_CK_NONE);
				Rsp.gdp.gDPSetTextureConvert(Gbi.G_TC_FILT);
				Rsp.gdp.gDPSetTextureFilter(Gbi.G_TF_POINT);
				Rsp.gdp.gDPSetTextureLUT(Gbi.G_TT_NONE);
				Rsp.gdp.gDPSetTextureLOD(Gbi.G_TL_TILE);
				Rsp.gdp.gDPSetTextureDetail(Gbi.G_TD_CLAMP);
				Rsp.gdp.gDPSetTexturePersp(Gbi.G_TP_PERSP);
				Rsp.gdp.gDPSetCycleType(Gbi.G_CYC_1CYCLE);
				Rsp.gdp.gDPPipelineMode(Gbi.G_PM_NPRIMITIVE);

				Rsp.gsp.loadUcode(uc_start, uc_dstart);
				Rsp.gsp.RSP_ProcessDList();

    }

	public static GL gl;
    private static int polygonMode=-1,depthTest=-1;
    private static void setDepthTest(boolean enabled) {
        enabled &= RendererSettings.depthTest;
        int requested=enabled?1:0;
        if(requested!=depthTest){if(enabled)gl.glEnable(GL.GL_DEPTH_TEST);else gl.glDisable(GL.GL_DEPTH_TEST);depthTest=requested;}
    }
    public static void polygonMode(int mode) {
        if(polygonMode!=mode){gl.glPolygonMode(GL.GL_FRONT_AND_BACK,mode);polygonMode=mode;}
    }

	public static int uc_start;
	public static int uc_dstart;

//    public static boolean	EXT_fog_coord;			// TNT, GeForce, Rage 128, Radeon

	// used by Gsp
//    public static int numTriangles;
//    private static GLVertex[] vertices = new GLVertex[256];
//    private static int numVertices;
	private static boolean fog;
//    private static FloatBuffer bigArray;

	/** Creates a new instance of OpenGl */
	private OpenGl() {
	}

	// called by GLN64Plugin
	public static void config() {
		fog = false; // true;
		OpenGlGdp.config();
	}

	// called by GLN64Plugin
    public static void OGL_Stop() {
        GLAutoDrawable drawable=OpenGlGdp.hDC;
        if(drawable==null)return;
        OpenGlGdp.hDC=null;
        onEdt(()->{
            if(drawable instanceof GLCanvas canvas){if(canvas.getParent()!=null)canvas.getParent().remove(canvas);else canvas.getContext().destroy();}
        });
    }
    private static void onEdt(Runnable action) {
        if(javax.swing.SwingUtilities.isEventDispatchThread()){action.run();return;}
        try{javax.swing.SwingUtilities.invokeAndWait(action);}catch(Exception error){throw new IllegalStateException("Graphics window operation failed",error);}
    }

	// called by HlePlugin
	public static boolean OGL_Start() {
        NativeLibraries.install();
        onEdt(OpenGl::createWindow);return true;
    }
    private static void createWindow() {
		int displayWidth = RendererSettings.windowWidth;
		int displayHeight = RendererSettings.windowHeight;

		JFrame window = HlePlugin.hWnd;
		GLCanvas canvas = new GLCanvas();

        kerneln64.ui.KeyboardRouting.attach(canvas);
        canvas.setFocusable(true);
		canvas.requestFocusInWindow();

		canvas.setIgnoreRepaint(true);
		canvas.setAutoSwapBufferMode(false);
		canvas.addGLEventListener(new OGL_EventListener());

		canvas.setBackground(java.awt.Color.BLACK);
		window.getContentPane().setBackground(java.awt.Color.BLACK);
		window.getContentPane().removeAll();
		window.getContentPane().add(canvas);
        if (kerneln64.Main.emulationStatus != null)
            window.getContentPane().add(kerneln64.Main.emulationStatus, BorderLayout.SOUTH);
		canvas.setSize(displayWidth, displayHeight);
        canvas.setPreferredSize(new java.awt.Dimension(displayWidth, displayHeight));
        window.setResizable(!RendererSettings.lockWindow);
        window.setAlwaysOnTop(RendererSettings.alwaysOnTop);

		window.pack();

		while (!canvas.isDisplayable()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}

		gl = canvas.getGL();
		OpenGlGdp.hDC = canvas;
		OpenGlGdp.hDC.swapBuffers(); // TMP
	}


	// Private Methods /////////////////////////////////////////////////////////

	private static void OGL_UpdateCullFace() {
		if (RendererSettings.culling && (Rsp.gsp.geometryMode & Gbi.G_CULL_BOTH) != 0) {
			gl.glEnable(GL.GL_CULL_FACE);

			if ((Rsp.gsp.geometryMode & Gbi.G_CULL_BOTH) == Gbi.G_CULL_BOTH)
                gl.glCullFace(GL.GL_FRONT_AND_BACK);
			else if ((Rsp.gsp.geometryMode & Gbi.G_CULL_BACK) != 0)
				gl.glCullFace(GL.GL_BACK);
			else
				gl.glCullFace(GL.GL_FRONT);
		} else
			gl.glDisable(GL.GL_CULL_FACE);
	}

	public static void OGL_GspUpdateStates() {
        // Fog is evaluated by the fragment shader.
		// Wireframe toggle
		if (RendererSettings.wireframe)
			polygonMode(GL.GL_LINE);
		else
			polygonMode(GL.GL_FILL);

		// Geometry mode (same as before except fog removed)
		if ((Rsp.gsp.changed & Gsp.CHANGED_GEOMETRYMODE) != 0) {
            OGL_UpdateCullFace();
			if ((Rsp.gsp.geometryMode & Gbi.G_ZBUFFER) != 0)
				setDepthTest(true);
			else
				setDepthTest(false);

			Rsp.gsp.changed &= Gsp.CHANGED_MATRIX;
		}

		if ((Rsp.gsp.geometryMode & Gbi.G_ZBUFFER) != 0)
			setDepthTest(true);
		else
			setDepthTest(false);

		Rsp.gsp.changed &= Gsp.CHANGED_MATRIX;
	}

}
