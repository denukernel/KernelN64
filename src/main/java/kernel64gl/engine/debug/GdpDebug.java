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


package kernel64gl.engine.debug;

import kernel64gl.engine.core.Gbi;
import kernel64gl.engine.opengl.OpenGl;
import kernel64gl.engine.opengl.OpenGlGdp;
import kernel64gl.engine.core.Registers;
import kernel64gl.engine.rdp.Gdp;


public class GdpDebug extends Gdp{
    
    public static final boolean DEBUG = Debug.DEBUG_GDP;
    
    /** Creates a new instance of GDP */
    public GdpDebug(Runnable checkInterrupts, Registers regs) {
        super(checkInterrupts, regs);
    }
    
    public void gDPTexture(float sc, float tc, int level, int tile, int on) {
        super.gDPTexture(sc, tc, level, tile, on);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TEXTURE,
                    "gdPTexture( %f, %f, %d, %d, %d );\n", sc, tc, level, tile, on);
        }
    }


    public void gDPSetRenderMode(int mode1, int mode2) { 
        super.gDPSetRenderMode(mode1, mode2);

    }
    
    public void gDPSetOtherMode(int mode0, int mode1) {
        super.gDPSetOtherMode(mode0, mode1);

    }
    
    public void gDPSetPrimDepth(int z, int dz) {
        super.gDPSetPrimDepth(z, dz);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPSetPrimDepth( %f, %f );\n",
                    primDepth.z,
                    primDepth.deltaZ);
        }
    }
    
    public void gDPSetCombine(int muxs0, int muxs1) {
        super.gDPSetCombine(muxs0, muxs1);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_COMBINE,
                    "gDPSetCombine( %s, %s, %s, %s, %s, %s, %s, %s,\n",
                    Gbi.saRGBText[OpenGlGdp.combiners.combine.saRGB0],
                    Gbi.sbRGBText[OpenGlGdp.combiners.combine.sbRGB0],
                    Gbi.mRGBText[OpenGlGdp.combiners.combine.mRGB0],
                    Gbi.aRGBText[OpenGlGdp.combiners.combine.aRGB0],
                    Gbi.saAText[OpenGlGdp.combiners.combine.saA0],
                    Gbi.sbAText[OpenGlGdp.combiners.combine.sbA0],
                    Gbi.mAText[OpenGlGdp.combiners.combine.mA0],
                    Gbi.aAText[OpenGlGdp.combiners.combine.aA0] );
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_COMBINE,
                    "               %s, %s, %s, %s, %s, %s, %s, %s );\n",
                    Gbi.saRGBText[OpenGlGdp.combiners.combine.saRGB1],
                    Gbi.sbRGBText[OpenGlGdp.combiners.combine.sbRGB1],
                    Gbi.mRGBText[OpenGlGdp.combiners.combine.mRGB1],
                    Gbi.aRGBText[OpenGlGdp.combiners.combine.aRGB1],
                    Gbi.saAText[OpenGlGdp.combiners.combine.saA1],
                    Gbi.sbAText[OpenGlGdp.combiners.combine.sbA1],
                    Gbi.mAText[OpenGlGdp.combiners.combine.mA1],
                    Gbi.aAText[OpenGlGdp.combiners.combine.aA1] );
        }
    }
    
    public void gDPSetColorImage(int format, int size, int width, int address) {
        super.gDPSetColorImage(format, size, width, address);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPSetColorImage( %s, %s, %d, 0x%08X );\n",
                    Gbi.ImageFormatText[colorImage.format],
                    Gbi.ImageSizeText[colorImage.size],
                    colorImage.width,
                    colorImage.address );
        }
    }
    
    public void gDPSetTextureImage(int format, int size, int width, int address) {
        super.gDPSetTextureImage(format, size, width, address);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TEXTURE,
                    "gDPSetTextureImage( %s, %s, %d, 0x%08X );\n",
                    Gbi.ImageFormatText[format],
                    Gbi.ImageSizeText[size],
                    width,
                    address );
        }
    }
    
    public void gDPSetDepthImage(int address) { 
        super.gDPSetDepthImage(address);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPSetDepthImage( 0x%08X );\n", depthImageAddress);
        }
    }
    
    public void gDPSetEnvColor(int r, int g, int b, int a) { 
        super.gDPSetEnvColor(r, g, b, a);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_COMBINE,
                    "gDPSetEnvColor( %d, %d, %d, %d );\n", r, g, b, a);
        }
    }
    
    public void gDPSetBlendColor(int r, int g, int b, int a) { 
        super.gDPSetBlendColor(r, g, b, a);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPSetBlendColor( %d, %d, %d, %d );\n", r, g, b, a);
        }
    }
    
    public void gDPSetFogColor(int r, int g, int b, int a) {  
        super.gDPSetFogColor(r, g, b, a);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPSetFogColor( %d, %d, %d, %d );\n", r, g, b, a);
        }
    }
    
    public void gDPSetFillColor(int c) {   
        super.gDPSetFillColor(c);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPSetFillColor( 0x%08X );\n", c);
        }
    }
    
    public void gDPSetPrimColor(int m, int l, int r, int g, int b, int a) { 
        super.gDPSetPrimColor(m, l, r, g, b, a);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_COMBINE,
                    "gDPSetPrimColor( %d, %d, %d, %d, %d, %d );\n", m, l, r, g, b, a);
        }
    }
    
    public void gDPSetTile(int format, int size, int line, int tmem, int tile, int palette, int cmt, int cms, int maskt, int masks, int shiftt, int shifts) {
        super.gDPSetTile(format, size, line, tmem, tile, palette, cmt, cms, maskt, masks, shiftt, shifts);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TEXTURE,
                    "gDPSetTile( %s, %s, %d, %d, %d, %d, %s%s, %s%s, %d, %d, %d, %d );\n",
                    Gbi.ImageFormatText[format],
                    Gbi.ImageSizeText[size],
                    line,
                    tmem,
                    tile,
                    palette,
                    (cmt & Gbi.G_TX_MIRROR)!=0 ? "G_TX_MIRROR" : "G_TX_NOMIRROR",
                    (cmt & Gbi.G_TX_CLAMP)!=0 ? " | G_TX_CLAMP" : "",
                    (cms & Gbi.G_TX_MIRROR)!=0 ? "G_TX_MIRROR" : "G_TX_NOMIRROR",
                    (cms & Gbi.G_TX_CLAMP)!=0 ? " | G_TX_CLAMP" : "",
                    maskt,
                    masks,
                    shiftt,
                    shifts );
        }
    }
    
    public void gDPSetTileSize(int tile, int uls, int ult, int lrs, int lrt) { 
        super.gDPSetTileSize(tile, uls, ult, lrs, lrt);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TEXTURE,
                    "gDPSetTileSize( %d, %.2f, %.2f, %.2f, %.2f );\n",
                    tile, uls, ult, lrs, lrt);
        }
    }
    
    public void gDPLoadTile(int tile, int uls, int ult, int lrs, int lrt) { 
        super.gDPLoadTile(tile, uls, ult, lrs, lrt);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TEXTURE,
                    "gDPLoadTile( %d, %d, %d, %d, %d );\n",
                    tile, uls, ult, lrs, lrt);
        }
    }
    
    public void gDPLoadBlock(int tile, int uls, int ult, int lrs, int dxt) { 
        super.gDPLoadBlock(tile, uls, ult, lrs, dxt);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TEXTURE,
                    "gDPLoadBlock( %d, %d, %d, %d, %d );\n", tile, uls, ult, lrs, dxt);
        }
    }
    
    public void gDPLoadTLUT(int tile, int uls, int ult, int lrs, int lrt) { 
        super.gDPLoadTLUT(tile, uls, ult, lrs, lrt);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TEXTURE,
                    "gDPLoadTLUT( %d, %d, %d, %d, %d );\n",
                    tile, uls, ult, lrs, lrt);
        }
    }
    
    public void gDPSetScissor(int mode, float ulx, float uly, float lrx, float lry) {  
        super.gDPSetScissor(mode, ulx, uly, lrx, lry);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_IGNORED,
                    "gDPSetScissor( %s, %.2f, %.2f, %.2f, %.2f );\n",
                    Gbi.ScissorModeText[scissor.mode],
                    scissor.ulx,
                    scissor.uly,
                    scissor.lrx,
                    scissor.lry);
        }
    }
    
    public void gDPFillRectangle(int ulx, int uly, int lrx, int lry) {  
        super.gDPFillRectangle(ulx, uly, lrx, lry);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPFillRectangle( %d, %d, %d, %d );\n", ulx, uly, lrx, lry);
        }
    }
    
    public void gDPSetConvert(int k0, int k1, int k2, int k3, int k4, int k5) {
        super.gDPSetConvert(k0, k1, k2, k3, k4, k5);
    }
    
    public void gDPSetKeyR(int cR, int sR, int wR) {
        super.gDPSetKeyR(cR, sR, wR);
    }
    
    public void gDPSetKeyGB(int cG, int sG, int wG, int cB, int sB, int wB) {
        super.gDPSetKeyGB(cG, sG, wG, cB, sB, wB);
    }
    
    public void gDPTextureRectangle(float ulx, float uly, float lrx, float lry, int tile, float s, float t, float dsdx, float dtdy, boolean flip) { 
        super.gDPTextureRectangle(ulx, uly, lrx, lry, tile, s, t, dsdx, dtdy, flip);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPTextureRectangle( %f, %f, %f, %f, %d, %f, %f, %f, %f );\n",
                    ulx, uly, lrx, lry, tile, s, t, dsdx, dtdy);
        }
    }
    
    public void gDPTextureRectangleFlip(float ulx, float uly, float lrx, float lry, int tile, float s, float t, float dsdx, float dtdy) { 
        super.gDPTextureRectangleFlip(ulx, uly, lrx, lry, tile, s, t, dsdx, dtdy);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPTextureRectangleFlip( %f, %f, %f, %f, %d, %f, %f, %f, %f );\n",
                    ulx, uly, lrx, lry, tile, s, t, dsdx, dtdy);
        }
    }
    
    public void gDPFullSync() {   
        super.gDPFullSync();
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gDPFullSync();\n");
        }
    }
    
    public void gDPTileSync() {
        super.gDPTileSync();
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_IGNORED | Debug.DEBUG_TEXTURE,
                    "gDPTileSync();\n");
        }
    }
    
    public void gDPPipeSync() {
        super.gDPPipeSync();
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_IGNORED,
                    "gDPPipeSync();\n");
        }
    }
    
    public void gDPLoadSync() {
        super.gDPLoadSync();
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_IGNORED,
                    "gDPLoadSync();\n");
        }
    }
    
    public void gDPNoOp() {
        super.gDPNoOp();
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_IGNORED,
                    "gDPNoOp();\n");
        }
    }
    
}
