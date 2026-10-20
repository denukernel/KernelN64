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
import kernel64gl.engine.config.RendererSettings;
import kernel64gl.engine.core.Gbi;
import kernel64gl.engine.core.Rsp;

import kernel64gl.engine.rdp.combiners.Combiners;
import kernel64gl.engine.rdp.Gdp;
import kernel64gl.engine.rdp.textures.TextureCache;
import javax.media.opengl.GL;
import static kernel64gl.engine.opengl.OpenGlGdp.*;

/** RDP render-state synchronization and stipple setup. */
final class OpenGlRenderState {
    private OpenGlRenderState() {}

	static void OGL_GdpUpdateStates()  {
        if ((Rsp.gdp.changed & Gdp.CHANGED_VIEWPORT) != 0) updateViewport();
        if ((Rsp.gdp.changed & Gdp.CHANGED_FOGCOLOR) != 0)
            gl.glFogfv(GL.GL_FOG_COLOR, Rsp.gdp.fogColor, 0);
        if ((Rsp.gdp.changed & Gdp.CHANGED_RENDERMODE) != 0) {

            if (Gdp.RDP_GETOM_Z_COMPARE_EN(Rsp.gdp.otherMode) != 0)
                gl.glDepthFunc(GL.GL_LEQUAL);
            else
                gl.glDepthFunc(GL.GL_ALWAYS);
            

            if (Gdp.RDP_GETOM_Z_UPDATE_EN(Rsp.gdp.otherMode) != 0)
                gl.glDepthMask(true);
            else
                gl.glDepthMask(false);
            

            if (Gdp.RDP_GETOM_Z_MODE(Rsp.gdp.otherMode) == Gbi.ZMODE_DEC)
                gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            else {
                gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
            }
        }

        if ((Rsp.gdp.changed & Gdp.CHANGED_ALPHACOMPARE) != 0 || (Rsp.gdp.changed & Gdp.CHANGED_RENDERMODE) != 0) {

            if ((Gdp.RDP_GETOM_ALPHA_COMPARE_EN(Rsp.gdp.otherMode) == Gbi.G_AC_THRESHOLD) && (Gdp.RDP_GETOM_ALPHA_CVG_SELECT(Rsp.gdp.otherMode) == 0)) {
                gl.glEnable(GL.GL_ALPHA_TEST);
                
                gl.glAlphaFunc((Rsp.gdp.blendColor[3] > 0.0f) ? GL.GL_GEQUAL : GL.GL_GREATER, Rsp.gdp.blendColor[3]);
            }
            // Used in TEX_EDGE and similar render modes
//            else if (Rsp.gdp.otherMode.cvgXAlpha != 0) {
            else if (Gdp.RDP_GETOM_CVG_TIMES_ALPHA(Rsp.gdp.otherMode) != 0) {
                gl.glEnable(GL.GL_ALPHA_TEST);
                
                // Arbitrary number -- gives nice results though
                gl.glAlphaFunc(GL.GL_GEQUAL, 0.5f);
            } else {
                gl.glDisable(GL.GL_ALPHA_TEST);
            }
            

            if (usePolygonStipple && (Gdp.RDP_GETOM_ALPHA_COMPARE_EN(Rsp.gdp.otherMode) == Gbi.G_AC_DITHER) && (Gdp.RDP_GETOM_ALPHA_CVG_SELECT(Rsp.gdp.otherMode) == 0))
                gl.glEnable(GL.GL_POLYGON_STIPPLE);
            else
                gl.glDisable(GL.GL_POLYGON_STIPPLE);
        }
        
        if ((Rsp.gdp.changed & Gdp.CHANGED_SCISSOR) != 0) {
            gl.glScissor((int)(Rsp.gdp.scissor.ulx * scaleX), (int)((screenHeight - Rsp.gdp.scissor.lry) * scaleY + heightOffset),
                    (int)((Rsp.gdp.scissor.lrx - Rsp.gdp.scissor.ulx) * scaleX), (int)((Rsp.gdp.scissor.lry - Rsp.gdp.scissor.uly) * scaleY));
        }


        if ((Rsp.gdp.changed & Gdp.CHANGED_COMBINE) != 0 || (Rsp.gdp.changed & Gdp.CHANGED_CYCLETYPE) != 0) {

            if (Gdp.RDP_GETOM_CYCLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_CYC_COPY) {
//                combiners.setCombine(gl, Rsp.gdp.otherMode.cycleType == Gbi.G_CYC_2CYCLE, combiners.encodeCombineMode(
                combiners.setCombine(gl, false, combiners.encodeCombineMode(
                        Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_TEXEL0,
                        Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_TEXEL0,
                        Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_TEXEL0,
                        Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_TEXEL0));
//            } else if (Rsp.gdp.otherMode.cycleType == Gbi.G_CYC_FILL) {
                } else if (Gdp.RDP_GETOM_CYCLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_CYC_FILL) {
//                combiners.setCombine(gl, Rsp.gdp.otherMode.cycleType == Gbi.G_CYC_2CYCLE, combiners.encodeCombineMode(
                combiners.setCombine(gl, false, combiners.encodeCombineMode(
                        Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_SHADE,
                        Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_1,
                        Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_0, Combiners.G_CCMUX_SHADE,
                        Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_0, Combiners.G_ACMUX_1));
            } else {

                combiners.setCombine(gl, Gdp.RDP_GETOM_CYCLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_CYC_2CYCLE, combiners.combine.getMux());
            }
            // A new combiner can start using a texture unit that was previously dummy-bound.
            Rsp.gdp.changed |= Gdp.CHANGED_COMBINE_COLORS | Gdp.CHANGED_TEXTURE;
        }
        
        if ((Rsp.gdp.changed & Gdp.CHANGED_COMBINE_COLORS) != 0) {
            combiners.updateCombineColors(gl);
            Rsp.gdp.changed &= ~Gdp.CHANGED_COMBINE_COLORS;
        }
        
        if ((Rsp.gdp.changed & Gdp.CHANGED_TEXTURE) != 0 || (Rsp.gdp.changed & Gdp.CHANGED_TILE) != 0 || (cache.changed & TextureCache.CHANGED_TMEM) != 0) {
            combiners.beginTextureUpdate(gl);
            
            if (combiners.usesT0) {

                cache.update(Gdp.RDP_GETOM_TLUT_TYPE(Rsp.gdp.otherMode) == Gbi.G_TT_IA16, 0, scaleX, scaleY, (Gdp.RDP_GETOM_SAMPLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_TF_BILERP) || (Gdp.RDP_GETOM_SAMPLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_TF_AVERAGE));
                
                Rsp.gdp.changed &= ~Gdp.CHANGED_TEXTURE;
                Rsp.gdp.changed &= ~Gdp.CHANGED_TILE;
                cache.changed &= ~TextureCache.CHANGED_TMEM;
            } else {
                cache.activateDummy(0);
            }
            
            //NOTE: The following block does not appear to work if
            // combiner uses T0 but not T1
            if (combiners.usesT1) {

                cache.update(Gdp.RDP_GETOM_TLUT_TYPE(Rsp.gdp.otherMode) == Gbi.G_TT_IA16, 1, scaleX, scaleY, (Gdp.RDP_GETOM_SAMPLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_TF_BILERP) || (Gdp.RDP_GETOM_SAMPLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_TF_AVERAGE));
                
                Rsp.gdp.changed &= ~Gdp.CHANGED_TEXTURE;
                Rsp.gdp.changed &= ~Gdp.CHANGED_TILE;
                cache.changed &= ~TextureCache.CHANGED_TMEM;
            } else {
                cache.activateDummy(1);
            }
            
            combiners.endTextureUpdate(gl);
        }


        if ((Rsp.gdp.changed & Gdp.CHANGED_RENDERMODE) != 0 || (Rsp.gdp.changed & Gdp.CHANGED_CYCLETYPE) != 0) {

            if ((Gdp.RDP_GETOM_FORCE_BLEND(Rsp.gdp.otherMode) != 0) &&
                    (Gdp.RDP_GETOM_CYCLE_TYPE(Rsp.gdp.otherMode) != Gbi.G_CYC_COPY) &&
                    (Gdp.RDP_GETOM_CYCLE_TYPE(Rsp.gdp.otherMode) != Gbi.G_CYC_FILL) &&
                    (Gdp.RDP_GETOM_ALPHA_CVG_SELECT(Rsp.gdp.otherMode) == 0)) {
                gl.glEnable(GL.GL_BLEND);
                

                switch (Rsp.gdp.otherMode.w1 >>> 16) {
                    case 0x0448: // Add
                    case 0x055A:
                        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
                        break;
                    case 0x0C08: // 1080 Sky
                    case 0x0F0A: // Used LOTS of places
                        gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO);
                        break;
                    case 0xC810: // Blends fog
                    case 0xC811: // Blends fog
                    case 0x0C18: // Standard interpolated blend
                    case 0x0C19: // Used for antialiasing
                    case 0x0050: // Standard interpolated blend
                    case 0x0055: // Used for antialiasing
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case 0x0FA5: // Seems to be doing just blend color - maybe combiner can be used for this?
                    case 0x5055: // Used in Paper Mario intro, I'm not sure if this is right...
                        gl.glBlendFunc(GL.GL_ZERO, GL.GL_ONE);
                        break;
                    default:
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                }
            } else {
                gl.glDisable(GL.GL_BLEND);
            }
            

            if (Gdp.RDP_GETOM_CYCLE_TYPE(Rsp.gdp.otherMode) == Gbi.G_CYC_FILL) {
                // Fill writes the packed color directly, including colors with zero alpha.
                gl.glDisable(GL.GL_BLEND);
            }
        }
        
        cache.changed &= TextureCache.CHANGED_TMEM;
        Rsp.gdp.changed &= Gdp.CHANGED_TILE;
        Rsp.gdp.changed &= Gdp.CHANGED_TEXTURE;
    }

	static void stipple()  {

        if (usePolygonStipple && (Gdp.RDP_GETOM_ALPHA_COMPARE_EN(Rsp.gdp.otherMode) == Gbi.G_AC_DITHER) && (Gdp.RDP_GETOM_ALPHA_CVG_SELECT(Rsp.gdp.otherMode) == 0)) {
            lastStipple = (lastStipple + 1) & 0x7;
            gl.glPolygonStipple(stipplePattern[(int)(Combiners.envColor[3] * 255.0f) >> 3][lastStipple], 0);
        }
    }
}
