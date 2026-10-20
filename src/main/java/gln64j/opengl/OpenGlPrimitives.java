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

package gln64j.opengl;
import gln64j.config.GLN64jPluginSettings;
import gln64j.core.Gbi;
import gln64j.core.Rsp;

import gln64j.debug.Debug;
import gln64j.rdp.combiners.Combiners;
import gln64j.rdp.Gdp;
import javax.media.opengl.GL;
import static gln64j.opengl.OpenGlGdp.*;

/** Rectangle, line, and triangle drawing and batching. */
final class OpenGlPrimitives {
    private OpenGlPrimitives() {}

	static void OGL_DrawRect(int ulx, int uly, int lrx, int lry, float[] color, int depthSource)  {
        OpenGl.OGL_GspUpdateStates();
        if (GLN64jPluginSettings.wireframe)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        OGL_GdpUpdateStates();
        
        boolean culling = gl.glIsEnabled(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, screenWidth, screenHeight, 0, 1.0f, -1.0f);
        gl.glViewport(0, heightOffset, width, height);
        if (Debug.WIREFRAME)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE); //TMP
        gl.glDepthRange(0.0f, 1.0f);
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex4f(ulx, uly, (depthSource == Gbi.G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
        gl.glVertex4f(lrx, uly, (depthSource == Gbi.G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
        gl.glVertex4f(lrx, lry, (depthSource == Gbi.G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
        gl.glVertex4f(ulx, lry, (depthSource == Gbi.G_ZS_PRIM) ? zDepth : nearZ, 1.0f);
        gl.glEnd();
        
        gl.glLoadIdentity();
        

        if (culling)
            gl.glEnable(GL.GL_CULL_FACE);
        

        gl.glEnable(GL.GL_SCISSOR_TEST);
        updateViewport();
    }

	static void OGL_DrawTexturedRect(float ulx, float uly, float lrx, float lry, float uls, float ult, float lrs, float lrt, boolean flip, int depthSource, int cycleType)  {
        OpenGl.OGL_GspUpdateStates();
        
        OGL_GdpUpdateStates();
        if (GLN64jPluginSettings.wireframe)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        rect0.x = ulx;
        rect0.y = uly;
        rect0.z = depthSource == Gbi.G_ZS_PRIM ? zDepth : nearZ;
        rect0.w = 1.0f;
        rect0.color[0] = 1.0f;
        rect0.color[1] = 1.0f;
        rect0.color[2] = 1.0f;
        rect0.color[3] = 0.0f;
        rect0.secondaryColor[0] = 1.0f;
        rect0.secondaryColor[1] = 1.0f;
        rect0.secondaryColor[2] = 1.0f;
        rect0.secondaryColor[3] = 1.0f;
        rect0.s0 = uls;
        rect0.t0 = ult;
        rect0.s1 = uls;
        rect0.t1 = ult;
        rect0.fog = 0.0f;
        
        rect1.x = lrx;
        rect1.y = lry;
        rect1.z = depthSource == Gbi.G_ZS_PRIM ? zDepth : nearZ;
        rect1.w = 1.0f;
        rect1.color[0] = 1.0f;
        rect1.color[1] = 1.0f;
        rect1.color[2] = 1.0f;
        rect1.color[3] = 0.0f;
        rect1.secondaryColor[0] = 1.0f;
        rect1.secondaryColor[1] = 1.0f;
        rect1.secondaryColor[2] = 1.0f;
        rect1.secondaryColor[3] = 1.0f;
        rect1.s0 = lrs;
        rect1.t0 = lrt;
        rect1.s1 = lrs;
        rect1.t1 = lrt;
        rect1.fog = 0.0f;
        
        boolean culling = gl.glIsEnabled(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0, screenWidth, screenHeight, 0, 1.0f, -1.0f); // left, right, bottom, top, near, far
        gl.glViewport(0, heightOffset, width, height); // x, y, width, height
        if (Debug.WIREFRAME)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE); //TMP
        
        if (combiners.usesT0) {
            rect0.s0 = rect0.s0 * cache.current[0].shiftScaleS - cache.textureTile[0].fuls;
            rect0.t0 = rect0.t0 * cache.current[0].shiftScaleT - cache.textureTile[0].fult;
            rect1.s0 = (rect1.s0 + 1.0f) * cache.current[0].shiftScaleS - cache.textureTile[0].fuls;
            rect1.t0 = (rect1.t0 + 1.0f) * cache.current[0].shiftScaleT - cache.textureTile[0].fult;
            
            if ((cache.current[0].maskS != 0) && (StrictMath.IEEEremainder(rect0.s0, cache.current[0].width) == 0.0f) && (cache.current[0].mirrorS == 0)) {
                rect1.s0 -= rect0.s0;
                rect0.s0 = 0.0f;
            }
            if ((cache.current[0].maskT != 0) && (StrictMath.IEEEremainder(rect0.t0, cache.current[0].height) == 0.0f) && (cache.current[0].mirrorT == 0)) {
                rect1.t0 -= rect0.t0;
                rect0.t0 = 0.0f;
            }
            
            if (Combiners.ARB_multitexture)
                gl.glActiveTexture(GL.GL_TEXTURE0);
            
            if ((rect0.s0 >= 0.0f) && (rect1.s0 <= cache.current[0].width)) {
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            }
            if ((rect0.t0 >= 0.0f) && (rect1.t0 <= cache.current[0].height)) {
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            }
            
            rect0.s0 *= cache.current[0].scaleS;
            rect0.t0 *= cache.current[0].scaleT;
            rect1.s0 *= cache.current[0].scaleS;
            rect1.t0 *= cache.current[0].scaleT;
        }
        
        if (combiners.usesT1 && Combiners.ARB_multitexture) {
            rect0.s1 = rect0.s1 * cache.current[1].shiftScaleS - cache.textureTile[1].fuls;
            rect0.t1 = rect0.t1 * cache.current[1].shiftScaleT - cache.textureTile[1].fult;
            rect1.s1 = (rect1.s1 + 1.0f) * cache.current[1].shiftScaleS - cache.textureTile[1].fuls;
            rect1.t1 = (rect1.t1 + 1.0f) * cache.current[1].shiftScaleT - cache.textureTile[1].fult;
            
            if ((cache.current[1].maskS != 0) && (StrictMath.IEEEremainder(rect0.s1, cache.current[1].width) == 0.0f) && (cache.current[1].mirrorS == 0)) {
                rect1.s1 -= rect0.s1;
                rect0.s1 = 0.0f;
            }
            if ((cache.current[1].maskT != 0) && (StrictMath.IEEEremainder(rect0.t1, cache.current[1].height) == 0.0f) && (cache.current[1].mirrorT == 0)) {
                rect1.t1 -= rect0.t1;
                rect0.t1 = 0.0f;
            }
            
            gl.glActiveTexture(GL.GL_TEXTURE1);
            
            if ((rect0.s1 == 0.0f) && (rect1.s1 <= cache.current[1].width)) {
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            }
            if ((rect0.t1 == 0.0f) && (rect1.t1 <= cache.current[1].height)) {
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            }
            
            rect0.s1 *= cache.current[1].scaleS;
            rect0.t1 *= cache.current[1].scaleT;
            rect1.s1 *= cache.current[1].scaleS;
            rect1.t1 *= cache.current[1].scaleT;
        }
        

        if (cycleType == Gbi.G_CYC_COPY) {
            if (Combiners.ARB_multitexture)
                gl.glActiveTexture(GL.GL_TEXTURE0);
            
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        }
        
        combiners.setConstant(rect0.color, combiners.vertex.color, combiners.vertex.alpha);
        
        if (Combiners.EXT_secondary_color)
            combiners.setConstant(rect0.secondaryColor, combiners.vertex.secondaryColor, combiners.vertex.alpha);
        
        gl.glBegin(GL.GL_QUADS);

        gl.glColor4f(rect0.color[0], rect0.color[1], rect0.color[2], rect0.color[3]);
        if (Combiners.EXT_secondary_color)

            gl.glSecondaryColor3fEXT(rect0.secondaryColor[0], rect0.secondaryColor[1], rect0.secondaryColor[2]);
        
        if (Combiners.ARB_multitexture) {
            gl.glMultiTexCoord2f(GL.GL_TEXTURE0, rect0.s0, rect0.t0);
            gl.glMultiTexCoord2f(GL.GL_TEXTURE1, rect0.s1, rect0.t1);
            gl.glVertex4f(rect0.x, rect0.y, rect0.z, 1.0f);
            
            gl.glMultiTexCoord2f(GL.GL_TEXTURE0, rect1.s0, rect0.t0);
            gl.glMultiTexCoord2f(GL.GL_TEXTURE1, rect1.s1, rect0.t1);
            gl.glVertex4f(rect1.x, rect0.y, rect0.z, 1.0f);
            
            gl.glMultiTexCoord2f(GL.GL_TEXTURE0, rect1.s0, rect1.t0);
            gl.glMultiTexCoord2f(GL.GL_TEXTURE1, rect1.s1, rect1.t1);
            gl.glVertex4f(rect1.x, rect1.y, rect0.z, 1.0f);
            
            gl.glMultiTexCoord2f(GL.GL_TEXTURE0, rect0.s0, rect1.t0);
            gl.glMultiTexCoord2f(GL.GL_TEXTURE1, rect0.s1, rect1.t1);
            gl.glVertex4f(rect0.x, rect1.y, rect0.z, 1.0f);
        } else {
            gl.glTexCoord2f(rect0.s0, rect0.t0);
            gl.glVertex4f(rect0.x, rect0.y, rect0.z, 1.0f);
            
            if (flip)
                gl.glTexCoord2f(rect0.s0, rect1.t0);
            else
                gl.glTexCoord2f(rect1.s0, rect0.t0);
            gl.glVertex4f(rect1.x, rect0.y, rect0.z, 1.0f);
            
            gl.glTexCoord2f(rect1.s0, rect1.t0);
            gl.glVertex4f(rect1.x, rect1.y, rect0.z, 1.0f);
            
            if (flip)
                gl.glTexCoord2f(rect1.s0, rect0.t0);
            else
                gl.glTexCoord2f(rect0.s0, rect1.t0);
            gl.glVertex4f(rect0.x, rect1.y, rect0.z, 1.0f);
        }
        gl.glEnd();
        
        gl.glLoadIdentity();
        

        if (culling)
            gl.glEnable(GL.GL_CULL_FACE);
        updateViewport();
    }

	static void OGL_DrawLine(float[] vtx1, float[] c1, float[] vtx2, float[] c2, float width)  {
        if (Rsp.gsp.changed != 0 || Rsp.gdp.changed != 0) {
            OpenGl.OGL_GspUpdateStates();
            OGL_GdpUpdateStates();
        }
        
        final int VTX = 0;
        final int CLR = 1;
        float[][][] v = {{vtx1, c1}, {vtx2, c2}};
        
        float[] color = new float[4];


        gl.glLineWidth(width * scaleX);
        
        gl.glBegin(GL.GL_LINES);
        for (int i = 0; i < 2; i++) {
            float[][] spvert = v[i];
            color[0] = spvert[CLR][0];
            color[1] = spvert[CLR][1];
            color[2] = spvert[CLR][2];
            color[3] = spvert[CLR][3];
            combiners.setConstant(color, combiners.vertex.color, combiners.vertex.alpha);
            gl.glColor4fv(color, 0);
            
            if (Combiners.EXT_secondary_color) {
                color[0] = spvert[CLR][0];
                color[1] = spvert[CLR][1];
                color[2] = spvert[CLR][2];
                color[3] = spvert[CLR][3];
                combiners.setConstant(color, combiners.vertex.secondaryColor, combiners.vertex.alpha);
                gl.glSecondaryColor3fvEXT(color, 0);
            }
            
            gl.glVertex4f(spvert[VTX][0], spvert[VTX][1], spvert[VTX][2], spvert[VTX][3]);
        }
        gl.glEnd();
    }

	static void OGL_AddTriangle(float[] vtx1, float[] c1, float[] tex1, float[] vtx2, float[] c2, float[] tex2, float[] vtx3, float[] c3, float[] tex3)  {
        if (Rsp.gsp.changed != 0 || Rsp.gdp.changed != 0) {
            OpenGl.OGL_GspUpdateStates();
            OGL_GdpUpdateStates();
        }
        if (GLN64jPluginSettings.wireframe)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);

        // Select the existing inputs directly; avoid four arrays per triangle.
        for (int i = 0; i < 3; i++) {
            float[] position = i == 0 ? vtx1 : i == 1 ? vtx2 : vtx3;
            float[] shade = i == 0 ? c1 : i == 1 ? c2 : c3;
            float[] texture = i == 0 ? tex1 : i == 1 ? tex2 : tex3;
            GLVertex vertex = vertices[numVertices];
            vertex.vtx.put(0, position[0]);
            vertex.vtx.put(1, position[1]);

            vertex.vtx.put(2, Gdp.RDP_GETOM_Z_SOURCE_SEL(Rsp.gdp.otherMode) == Gbi.G_ZS_PRIM ? zDepth * position[3] : position[2]);
            vertex.vtx.put(3, position[3]);
            
            vertex.color.put(0, shade[0]);
            vertex.color.put(1, shade[1]);
            vertex.color.put(2, shade[2]);
            vertex.color.put(3, shade[3]);
            float w = position[3];
            // Vertices behind the near plane participate in clipped triangles.
            // Use near-plane depth rather than maximum fog for their interpolation.
            float fogZ = position[2] < -w || w == 0 ? -1.0f : position[2] / w;
            float fog = fogZ * Rsp.gsp.fog.multiplier + Rsp.gsp.fog.offset;
            vertex.fog.put(0, Math.max(0.0f, Math.min(255.0f, fog)));
            combiners.setConstant(vertex.color, combiners.vertex.color, combiners.vertex.alpha);
            
            if (Combiners.EXT_secondary_color) {
                vertex.secondaryColor.put(0, 0.0f);
                vertex.secondaryColor.put(1, 0.0f);
                vertex.secondaryColor.put(2, 0.0f);
                vertex.secondaryColor.put(3, 1.0f);
                combiners.setConstant(vertex.secondaryColor, combiners.vertex.secondaryColor, Combiners.ONE);
            }
            
            if (combiners.usesT0) {
                vertex.tex0.put(0, cache.getTexS(0, texture[0]));
                vertex.tex0.put(1, cache.getTexT(0, texture[1]));
            }
            if (combiners.usesT1) {
                vertex.tex1.put(0, cache.getTexS(1, texture[0]));
                vertex.tex1.put(1, cache.getTexT(1, texture[1]));
            }
            numVertices++;
        }
        
        numTriangles++;
        
        if (numVertices >= 255)
            OGL_DrawTriangles();
    }

	static void OGL_DrawTriangles()  {
        if (numTriangles < 1)
            return;
        stipple();
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, numVertices);
        numTriangles = numVertices = 0;
    }
}
