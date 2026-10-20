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


package kernel64gl.engine.opcodes;

import kernel64gl.engine.rdp.Gdp;
import kernel64gl.engine.rsp.Gsp;
import static kernel64gl.engine.debug.Debug.*;
import kernel64gl.engine.rsp.GBIFunc;
import static kernel64gl.engine.core.Gbi.*;


public class F3dexbg extends F3dex2 {
    
    /**
     * Creates a new instance of F3dexbg
     */
    public F3dexbg() {
    }
    
    public static GBIFunc F3DEXBG_Vtx = new GBIFunc() {
        public void exec(int w0, int w1) {
            gsp.gSPVertexBg(gsp.RSP_SegmentToPhysical(w1), (w0>>12)&0xFF, ((w0>>1)&0x7F));
        }
    };
    
    public static GBIFunc F3DEXBG_MoveWord = new GBIFunc() {
        public void exec(int w0, int w1) {
            int index = (w0 >> 16) & 0xFF;
            int offset = w0 & 0xFFFF;
            int data = w1;
            
            switch (index) {
                case 0x02: //G_MW_NUMLIGHT:
                    // CHANGED
                    gsp.gSPNumLights(data / 48 /*24*/);
                    break;
                case 0x04: //G_MW_CLIP:
                    break;
                case 0x06: //G_MW_SEGMENT:
                    gsp.gSPSegment(offset >> 2, data & 0x00FFFFFF);
                    break;
                case 0x08: //G_MW_FOG:
                    gsp.gSPFogFactor((short)((data>>16)&0xFFFF), (short)(data&0xFFFF));
                    break;
                case 0x0C: //G_MW_FORCEMTX:
                    // Handled in movemem
                    break;
                case 0x0E: //G_MW_PERSPNORM:
                    break;
                case 0x10: { // moveword coord mod
                    // CHANGED
                    int n = offset >> 2;
                    
                    if ((w0&8) != 0)
                        return;
//                    int idx = (w0 >> 1) & 3;
//                    int pos = w0 & 0x30;
                    gsp.gSPCoordMod(w1, (w0 >> 1) & 3, w0 & 0x30);

                    break;
                }
                default:
                    System.err.printf("moveword unknown (index: 0x%08X, offset 0x%08X)\n", index, offset);
            }
        }
    };
    
    public static GBIFunc F3DEXBG_MoveMem = new GBIFunc() {
        public void exec(int w0, int w1) {
            int idx = w0 & 0xFF;
//            int addr = Rsp.RSP_SegmentToPhysical(w1);
//            int ofs = (w0 >> 5) & 0x3FFF;
            
            switch (idx) {
                case 8: //F3DEX2_MV_VIEWPORT:
                    gsp.gSPViewport(gsp.RSP_SegmentToPhysical(w1));
                    break;
                case 10: //G_MV_LIGHT:
                    // CHANGED
//                    int n = (ofs / 48);
                    gsp.gSPLightBg(gsp.RSP_SegmentToPhysical(w1), ((w0 >> 5) & 0x3FFF) / 48);

                    break;
                case 14: // Normales

                    gsp.gSPNormals(gsp.RSP_SegmentToPhysical(w1));
                    break;
                default:
                    System.err.printf("movemem unknown (%d)\n", idx);
            }
        }
    };
    
    public static GBIFunc F3DEXBG_Tri4 = new GBIFunc() {
        public void exec(int w0, int w1) {
            gsp.gSP4Triangles(
                    (w0>>23)&0x1F, (w0>>18)&0x1F, ((((w0>>15)&0x7)<<2)|((w1>>30)&0x3)),
                    (w0>>10)&0x1F, (w0>>5)&0x1F, (w0>>0)&0x1F,
                    (w1>>25)&0x1F, (w1>>20)&0x1F, (w1>>15)&0x1F,
                    (w1>>10)&0x1F, (w1>>5)&0x1F, (w1>>0)&0x1F);
        }
    };
    
    public static void F3DEXBG_Init(Gsp rsp, Gdp rdp) {
        gsp = rsp;
        gdp = rdp;
        F3dex2.GBI_InitFlags();
        G_RDPHALF_2 = F3dex2.F3DEX2_RDPHALF_2;
        G_SETOTHERMODE_H = F3dex2.F3DEX2_SETOTHERMODE_H;
        G_SETOTHERMODE_L = F3dex2.F3DEX2_SETOTHERMODE_L;
        G_RDPHALF_1 = F3dex2.F3DEX2_RDPHALF_1;
        G_SPNOOP = F3dex2.F3DEX2_SPNOOP;
        G_ENDDL = F3dex2.F3DEX2_ENDDL;
        G_DL = F3dex2.F3DEX2_DL;
        G_LOAD_UCODE = F3dex2.F3DEX2_LOAD_UCODE;
        G_MOVEMEM = F3dex2.F3DEX2_MOVEMEM;
        G_MOVEWORD = F3dex2.F3DEX2_MOVEWORD;
        G_MTX = F3dex2.F3DEX2_MTX;
        G_GEOMETRYMODE = F3dex2.F3DEX2_GEOMETRYMODE;
        G_POPMTX = F3dex2.F3DEX2_POPMTX;
        G_TEXTURE = F3dex2.F3DEX2_TEXTURE;
        G_DMA_IO = F3dex2.F3DEX2_DMA_IO;
        G_SPECIAL_1 = F3dex2.F3DEX2_SPECIAL_1;
        G_SPECIAL_2 = F3dex2.F3DEX2_SPECIAL_2;
        G_SPECIAL_3 = F3dex2.F3DEX2_SPECIAL_3;
        G_VTX = F3dex2.F3DEX2_VTX;
        G_MODIFYVTX =F3dex2.F3DEX2_MODIFYVTX;
        G_CULLDL = F3dex2.F3DEX2_CULLDL;
        G_BRANCH_Z = F3dex2.F3DEX2_BRANCH_Z;
        G_TRI1 = F3dex2.F3DEX2_TRI1;
        G_TRI2 = F3dex2.F3DEX2_TRI2;
        G_QUAD = F3dex2.F3DEX2_QUAD;
        
        gsp.pcStackSize = 18;
        
        gsp.setUcode(G_RDPHALF_2, F3d.F3D_RDPHalf_2);
        gsp.setUcode(G_SETOTHERMODE_H, F3dex2.F3DEX2_SetOtherMode_H);
        gsp.setUcode(G_SETOTHERMODE_L, F3dex2.F3DEX2_SetOtherMode_L);
        gsp.setUcode(G_RDPHALF_1, F3d.F3D_RDPHalf_1);
        gsp.setUcode(G_SPNOOP, F3d.F3D_SPNoOp);
        gsp.setUcode(G_ENDDL, F3d.F3D_EndDL);
        gsp.setUcode(G_DL, F3d.F3D_DList);
        gsp.setUcode(G_LOAD_UCODE, F3dex.F3DEX_Load_uCode);
        gsp.setUcode(G_MOVEMEM, F3DEXBG_MoveMem);
        gsp.setUcode(G_MOVEWORD, F3DEXBG_MoveWord);
        gsp.setUcode(G_MTX, F3dex2.F3DEX2_Mtx);
        gsp.setUcode(G_GEOMETRYMODE, F3dex2.F3DEX2_GeometryMode);
        gsp.setUcode(G_POPMTX, F3dex2.F3DEX2_PopMtx);
        gsp.setUcode(G_TEXTURE, F3dex2.F3DEX2_Texture);
        gsp.setUcode(G_DMA_IO, F3dex2.F3DEX2_DMAIO);
        gsp.setUcode(G_SPECIAL_1, F3dex2.F3DEX2_Special_1);
        gsp.setUcode(G_SPECIAL_2, F3dex2.F3DEX2_Special_2);
        gsp.setUcode(G_SPECIAL_3, F3dex2.F3DEX2_Special_3);
        
        gsp.setUcode(G_VTX, F3DEXBG_Vtx);
        gsp.setUcode(G_MODIFYVTX, F3dex.F3DEX_ModifyVtx);
        gsp.setUcode(G_CULLDL, F3dex.F3DEX_CullDL);
        gsp.setUcode(G_BRANCH_Z, F3dex.F3DEX_Branch_Z);
        gsp.setUcode(G_TRI1, F3dex2.F3DEX2_Tri1);
        gsp.setUcode(G_TRI2, F3dex.F3DEX_Tri2);
        gsp.setUcode(G_QUAD, F3dex2.F3DEX2_Quad);
        
        for (int i=0x10; i<0x20; i++)
            gsp.setUcode(i, F3DEXBG_Tri4);
        
        if (DEBUG_MICROCODE) System.out.println("Initialized F3DEXBG opcodes");
    }
    
}
