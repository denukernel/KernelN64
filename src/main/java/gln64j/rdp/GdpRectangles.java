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
import gln64j.core.Gbi;
import gln64j.rdp.combiners.Combiners;
import static gln64j.rdp.Gdp.*;

/** RDP fill and texture rectangle commands. */
final class GdpRectangles {
    private GdpRectangles() {}

	static void gDPFillRectangle(Gdp state, int ulx, int uly, int lrx, int lry)  {
        // ulx rect.xh (w2 >> 12) & 0xfff
        // uly rect.yh (w2 >> 0) & 0xfff
        // lrx rect.xl (w1 >> 12) & 0xfff
        // lry rect.yl (w1 >> 0) & 0xfff
        if(d3d11.framebuffer.D3DFramebuffers.enabled()) {
            OpenGlGdp.OGL_DrawTriangles();
            boolean fill=state.RDP_GETOM_CYCLE_TYPE(state.otherMode)==Gbi.G_CYC_FILL;
            if(fill){lrx++;lry++;}
            ulx=Math.max(ulx,(int)Math.ceil(state.scissor.ulx));uly=Math.max(uly,(int)Math.ceil(state.scissor.uly));
            lrx=Math.min(lrx,(int)state.scissor.lrx);lry=Math.min(lry,(int)state.scissor.lry);
            if(lrx<=ulx || lry<=uly)return;
            if(state.depthImageAddress==state.colorImage.address) {
                d3d11.framebuffer.D3DFramebuffers.clearDepth(ulx,uly,lrx,lry);return;
            }
            float[] color=fill?state.fillColor.color:new float[]{Combiners.primColor.r,Combiners.primColor.g,Combiners.primColor.b,Combiners.primColor.a};
            OpenGlGdp.OGL_DrawRect(ulx,uly,lrx,lry,color,state.RDP_GETOM_Z_SOURCE_SEL(state.otherMode));
            state.colorImage.changed=true;state.colorImage.height=Math.max(state.colorImage.height,lry);return;
        }
        DepthBufferStack.DepthBuffer buffer = state.depthBuffers.findBuffer(state.colorImage.address);
     // 1. Clamp corrupted coordinates to visible screen bounds
        ulx = state.clampX(ulx); uly = state.clampY(uly);
        lrx = state.clampX(lrx); lry = state.clampY(lry);

        // 2. Identify the active color
        float[] activeColorTemp = (state.RDP_GETOM_CYCLE_TYPE(state.otherMode) == Gbi.G_CYC_FILL) ? state.fillColor.color : state.blendColor;
        float[] activeColor = new float[] { activeColorTemp[0], activeColorTemp[1], activeColorTemp[2], 1.0f };

        // 3. WHITE FLASH SUPPRESSOR: 
        // Detect if this covers >95% of the screen and is pure white
        if (lrx - ulx >= OpenGlGdp.screenWidth - 10 && lry - uly >= OpenGlGdp.screenHeight - 10) {
            if (activeColor[0] > 0.95f && activeColor[1] > 0.95f && activeColor[2] > 0.95f) {
                
                return; // Skip drawing the blinding rectangle
            }
            
            // If it's a full-screen fill but NOT pure white, treat as clear
            OpenGlGdp.OGL_ClearColorBuffer(activeColor);
            return;
        }

        OpenGlGdp.OGL_DrawRect(ulx, uly, lrx, lry, activeColor, state.RDP_GETOM_Z_SOURCE_SEL(state.otherMode));
        if (buffer != null)
            buffer.cleared = true;
        
        if (state.depthImageAddress == state.colorImage.address) {

            OpenGlGdp.OGL_ClearDepthBuffer(state.RDP_GETOM_Z_UPDATE_EN(state.otherMode) != 0);
            return;
        }
        

        if (state.RDP_GETOM_CYCLE_TYPE(state.otherMode) == Gbi.G_CYC_FILL) {
            lrx++;
            lry++;
            
            // ⭐ IMPROVED DETECTION: 
            // Star Select background often fills 320x240 or slightly less.
            if (lrx - ulx >= OpenGlGdp.screenWidth - 5 && lry - uly >= OpenGlGdp.screenHeight - 5) {
                OpenGlGdp.OGL_ClearColorBuffer(activeColor);
                return;
            }
        }
        

        OpenGlGdp.OGL_DrawRect(ulx, uly, lrx, lry, activeColor, state.RDP_GETOM_Z_SOURCE_SEL(state.otherMode));
        
        // RDP function
        if (state.current != null)
            state.current.cleared = false;
        state.colorImage.changed = true;
        state.colorImage.height = StrictMath.max(state.colorImage.height, lry);
    }

	static int clampX(Gdp state, int x)  {
        return Math.max(0, Math.min(x, OpenGlGdp.screenWidth));
    }

	static int clampY(Gdp state, int y)  {
        return Math.max(0, Math.min(y, OpenGlGdp.screenHeight));
    }

	static void gDPTextureRectangle(Gdp state, float ulx, float uly, float lrx, float lry, int tilenum, float s, float t, float dsdx, float dtdy, boolean flip)  {

        if (state.RDP_GETOM_CYCLE_TYPE(state.otherMode) == Gbi.G_CYC_COPY) {
            dsdx = 1.0f;
            lrx += 1.0f;
            lry += 1.0f;
        }
        
        float lrs = s + (lrx - ulx - 1) * dsdx;
        float lrt = t + (lry - uly - 1) * dtdy;
        
        OpenGlGdp.cache.setTextureTile(lrs, lrt, tilenum, s, t, dsdx, dtdy);
     // 1. HARD CLAMP coordinates to prevent out-of-bounds crashes
        ulx = Math.max(-100, Math.min(ulx, OpenGlGdp.screenWidth + 100));
        uly = Math.max(-100, Math.min(uly, OpenGlGdp.screenHeight + 100));
        
        // 2. Prevent infinite texture loops from corrupted dsdx/dtdy
        if (Math.abs(dsdx) > 100.0f || Math.abs(dtdy) > 100.0f) return;
        if (lrs > s) {
            if (lrt > t)

                OpenGlGdp.OGL_DrawTexturedRect(ulx, uly, lrx, lry, s, t, lrs, lrt, flip, state.RDP_GETOM_Z_SOURCE_SEL(state.otherMode), state.RDP_GETOM_CYCLE_TYPE(state.otherMode));
            else

                OpenGlGdp.OGL_DrawTexturedRect(ulx, lry, lrx, uly, s, lrt, lrs, t, flip, state.RDP_GETOM_Z_SOURCE_SEL(state.otherMode), state.RDP_GETOM_CYCLE_TYPE(state.otherMode));
        } else {
            if (lrt > t)

                OpenGlGdp.OGL_DrawTexturedRect(lrx, uly, ulx, lry, lrs, t, s, lrt, flip, state.RDP_GETOM_Z_SOURCE_SEL(state.otherMode), state.RDP_GETOM_CYCLE_TYPE(state.otherMode));
            else

                OpenGlGdp.OGL_DrawTexturedRect(lrx, lry, ulx, uly, lrs, lrt, s, t, flip, state.RDP_GETOM_Z_SOURCE_SEL(state.otherMode), state.RDP_GETOM_CYCLE_TYPE(state.otherMode));
        }
        
        OpenGlGdp.cache.resetTextureTile();
        
        if (state.current != null)
            state.current.cleared = false;
        state.colorImage.changed = true;
        state.colorImage.height = (int)StrictMath.max(state.colorImage.height, state.scissor.lry);
    }

	static void gDPTextureRectangleFlip(Gdp state, float ulx, float uly, float lrx, float lry, int tilenum, float s, float t, float dsdx, float dtdy)  {
        // ulx rect.xh * 4
        // uly rect.yh * 4
        // lrx rect.xl * 4
        // lry rect.yl * 4
        // rect.tilenum
        // s rect.t * 32
        // t rect.s * 32
        // dsdx rect.dtdy * 1024
        // dsdy rect.dsdx * 1024
        state.gDPTextureRectangle(ulx, uly, lrx, lry, tilenum, s + (lrx - ulx) * dsdx, t + (lry - uly) * dtdy, -dsdx, -dtdy, true);
    }
}
