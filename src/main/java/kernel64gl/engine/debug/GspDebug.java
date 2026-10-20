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
import kernel64gl.engine.rsp.Gsp;
import java.nio.ByteBuffer;


public class GspDebug extends Gsp {
    
    public static final boolean DEBUG = Debug.DEBUG_GSP;
    
    /** Creates a new instance of GSP */
    public GspDebug(ByteBuffer rdram, ByteBuffer dmem) {
        super(rdram, dmem);
    }
    
    public void gSPNoOp() {
        super.gSPNoOp();
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_IGNORED,
                    "gSPNoOp();\n");
        }
    }
    
    public void gSPViewport(int address) {
        super.gSPViewport(address);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gdPViewport( 0x%08X );\n", address);
        }
    }
    
    public void gSPEndDisplayList() {
        super.gSPEndDisplayList();
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPEndDisplayList();\n\n");
        }
    }
    
    public void gSPSetGeometryMode(int mode) {
        super.gSPSetGeometryMode(mode);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPSetGeometryMode( %s%s%s%s%s%s%s%s%s%s );\n",
                    (mode & Gbi.G_SHADE)!=0 ? "G_SHADE | " : "",
                    (mode & Gbi.G_LIGHTING)!=0 ? "G_LIGHTING | " : "",
                    (mode & Gbi.G_SHADING_SMOOTH)!=0 ? "G_SHADING_SMOOTH | " : "",
                    (mode & Gbi.G_ZBUFFER)!=0 ? "G_ZBUFFER | " : "",
                    (mode & Gbi.G_TEXTURE_GEN)!=0 ? "G_TEXTURE_GEN | " : "",
                    (mode & Gbi.G_TEXTURE_GEN_LINEAR)!=0 ? "G_TEXTURE_GEN_LINEAR | " : "",
                    (mode & Gbi.G_CULL_FRONT)!=0 ? "G_CULL_FRONT | " : "",
                    (mode & Gbi.G_CULL_BACK)!=0 ? "G_CULL_BACK | " : "",
                    (mode & Gbi.G_FOG)!=0 ? "G_FOG | " : "",
                    (mode & Gbi.G_CLIPPING)!=0 ? "G_CLIPPING" : "");
        }
    }
    
    public void gSPClearGeometryMode(int mode) {
        super.gSPClearGeometryMode(mode);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPClearGeometryMode( %s%s%s%s%s%s%s%s%s%s );\n",
                    (mode & Gbi.G_SHADE)!=0 ? "G_SHADE | " : "",
                    (mode & Gbi.G_LIGHTING)!=0 ? "G_LIGHTING | " : "",
                    (mode & Gbi.G_SHADING_SMOOTH)!=0 ? "G_SHADING_SMOOTH | " : "",
                    (mode & Gbi.G_ZBUFFER)!=0 ? "G_ZBUFFER | " : "",
                    (mode & Gbi.G_TEXTURE_GEN)!=0 ? "G_TEXTURE_GEN | " : "",
                    (mode & Gbi.G_TEXTURE_GEN_LINEAR)!=0 ? "G_TEXTURE_GEN_LINEAR | " : "",
                    (mode & Gbi.G_CULL_FRONT)!=0 ? "G_CULL_FRONT | " : "",
                    (mode & Gbi.G_CULL_BACK)!=0 ? "G_CULL_BACK | " : "",
                    (mode & Gbi.G_FOG)!=0 ? "G_FOG | " : "",
                    (mode & Gbi.G_CLIPPING)!=0 ? "G_CLIPPING" : "");
        }
    }
    
    public void gSPDisplayList(int address) {
        super.gSPDisplayList(address);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPDisplayList( 0x%08X );\n", address);
        }
    }
    
    public void gSPBranchList(int address) {
        super.gSPBranchList(address);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPBranchList( 0x%08X );\n", address);
        }
    }
    
    public void gSPPopMatrix(int param) {
        super.gSPPopMatrix(param);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "gSPPopMatrix( %s );\n",
                    (param == Gbi.G_MTX_MODELVIEW) ? "G_MTX_MODELVIEW" :
                        (param == Gbi.G_MTX_PROJECTION) ? "G_MTX_PROJECTION" : "G_MTX_INVALID");
        }
    }
    
    public void gSPMatrix(int address, int param) {
        super.gSPMatrix(address, param);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n", tmpmtx[0][0], tmpmtx[0][1], tmpmtx[0][2], tmpmtx[0][3]);
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n", tmpmtx[1][0], tmpmtx[1][1], tmpmtx[1][2], tmpmtx[1][3]);
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n", tmpmtx[2][0], tmpmtx[2][1], tmpmtx[2][2], tmpmtx[2][3]);
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n", tmpmtx[3][0], tmpmtx[3][1], tmpmtx[3][2], tmpmtx[3][3]);
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "gSPMatrix( 0x%08X, %s | %s | %s );\n",
                    address,
                    (param & Gbi.G_MTX_PROJECTION)!=0 ? "G_MTX_PROJECTION" : "G_MTX_MODELVIEW",
                    (param & Gbi.G_MTX_LOAD)!=0 ? "G_MTX_LOAD" : "G_MTX_MUL",
                    (param & Gbi.G_MTX_PUSH)!=0 ? "G_MTX_PUSH" : "G_MTX_NOPUSH");
        }
    }
    
    public void gSPForceMatrix(int address) {
        super.gSPForceMatrix(address);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "gSPForceMatrix( 0x%08X );\n", address);
        }
    }
    
    public void gSPLight(int address, int n) {
        super.gSPLight(address, n);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED,
                    "// x = %2.6f    y = %2.6f    z = %2.6f\n",
                    rdram.get(address + 8) * Gbi.FIXED2FLOATRECIP7, rdram.get(address + 9) * Gbi.FIXED2FLOATRECIP7, rdram.get(address + 10) * Gbi.FIXED2FLOATRECIP7);
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED,
                    "// r = %3d    g = %3d   b = %3d\n", (rdram.get(address)&0xFF), (rdram.get(address + 1)&0xFF), (rdram.get(address + 2)&0xFF));
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPLight( 0x%08X, LIGHT_%d );\n", address, n);
        }
    }
    
    public void gSPLightBg(int address, int n) {
        super.gSPLightBg(address, n);
    }
    
    public void gSPVertex(int address, int n, int v0) {
        super.gSPVertex(address, n, v0);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_VERTEX,
                    "gSPVertex( 0x%08X, %d, %d );\n", address, n, v0);
        }
    }
    
    public void gSP1Triangle(int v0, int v1, int v2, int flag) {
        super.gSP1Triangle(v0, v1, v2, flag);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "gSP1Triangle( %d, %d, %d, %d );\n", v0, v1, v2, flag);
        }
    }
    
    public void gSP4Triangles(
            int v00, int v01, int v02,
            int v10, int v11, int v12,
            int v20, int v21, int v22,
            int v30, int v31, int v32) {
        super.gSP4Triangles(v00, v01, v02, v10, v11, v12, v20, v21, v22, v30, v31, v32);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "gSP4Triangles( %d, %d, %d,\n", v00, v01, v02);
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "               %d, %d, %d,\n", v10, v11, v12);
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "               %d, %d, %d,\n", v20, v21, v22);
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "               %d, %d, %d );\n", v30, v31, v32);
        }
    }
    
    public void gSP1Quadrangle(int v0, int v1, int v2, int v3) {
        super.gSP1Quadrangle(v0, v1, v2, v3);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "gSP1Quadrangle( %d, %d, %d, %d );\n", v0, v1, v2, v3);
        }
    }
    
    public void gSPCullDisplayList(int v0, int vn) {
        super.gSPCullDisplayList(v0, vn);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPCullDisplayList( %d, %d );\n\n", v0, vn);
        }
    }
    
    public void gSPSegment(int seg, int base) {
        super.gSPSegment(seg, base);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPSegment( %s, 0x%08X );\n",
                    Gbi.SegmentText[seg], base);
        }
    }
    
    public void gSPClipRatio(int r) {
        super.gSPClipRatio(r);
    }
    
    public void gSPInsertMatrix(int where, int num) {
        super.gSPInsertMatrix(where, num);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "gSPInsertMatrix( %s, %d );\n", Gbi.MWOMatrixText[where >> 2], num);
        }
    }
    
    public void gSPModifyVertex(int vtx, int where, int val) {
        super.gSPModifyVertex(vtx, where, val);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_UNHANDLED,
                    "gSPModifyVertex( %d, %s, 0x%08X );\n",
                    vtx, Gbi.MWOPointText[(where - 0x10) >> 2], val);
        }
    }
    
    public void gSPNumLights(int n) {
        super.gSPNumLights(n);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPNumLights( %d );\n", n);
        }
    }
    
    public void gSPLightColor(int lightNum, int packedColor) {
        super.gSPLightColor(lightNum, packedColor);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPLightColor( %d, 0x%08X );\n", lightNum, packedColor);
        }
    }
    
    public void gSPFogFactor(short fm, short fo) {
        super.gSPFogFactor(fm, fo);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPFogFactor( %i, %i );\n", fm, fo);
        }
    }
    
    public void gSPPerspNormalize(short scale) {
        super.gSPPerspNormalize(scale);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_UNHANDLED,
                    "gSPPerspNormalize( %d );\n", scale);
        }
    }
    
    public void gSPLoadUcodeEx(int uc_start, int uc_dstart, short uc_dsize) {
        super.gSPLoadUcodeEx(uc_start, uc_dstart, uc_dsize);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPLoadUcodeEx( 0x%08X, 0x%08X, %d );\n", uc_start, uc_dstart, uc_dsize);
        }
    }
    
    public void gSPBranchLessZ(int address, int vtx, float zval) {
        super.gSPBranchLessZ(address, vtx, zval);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPBranchLessZ( 0x%08X, %d, %f );\n", address, vtx, zval);
        }
    }
    
    public void gSP2Triangles(int v00, int v01, int v02, int flag0, int v10, int v11, int v12, int flag1) {
        super.gSP2Triangles(v00, v01, v02, flag0, v10, v11, v12, flag1);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "gSP2Triangles( %d, %d, %d, %d,\n",
                    v00, v01, v02, flag0);
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "               %d, %d, %d, %d );\n",
                    v10, v11, v12, flag1);
        }
    }
    
    public void gSPGeometryMode(int clear, int set) {
        super.gSPGeometryMode(clear, set);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPGeometryMode( %s%s%s%s%s%s%s%s%s%s, %s%s%s%s%s%s%s%s%s%s );\n",
                    (clear & Gbi.G_SHADE)!=0 ? "G_SHADE | " : "",
                    (clear & Gbi.G_LIGHTING)!=0 ? "G_LIGHTING | " : "",
                    (clear & Gbi.G_SHADING_SMOOTH)!=0 ? "G_SHADING_SMOOTH | " : "",
                    (clear & Gbi.G_ZBUFFER)!=0 ? "G_ZBUFFER | " : "",
                    (clear & Gbi.G_TEXTURE_GEN)!=0 ? "G_TEXTURE_GEN | " : "",
                    (clear & Gbi.G_TEXTURE_GEN_LINEAR)!=0 ? "G_TEXTURE_GEN_LINEAR | " : "",
                    (clear & Gbi.G_CULL_FRONT)!=0 ? "G_CULL_FRONT | " : "",
                    (clear & Gbi.G_CULL_BACK)!=0 ? "G_CULL_BACK | " : "",
                    (clear & Gbi.G_FOG)!=0 ? "G_FOG | " : "",
                    (clear & Gbi.G_CLIPPING)!=0 ? "G_CLIPPING" : "",
                    (set & Gbi.G_SHADE)!=0 ? "G_SHADE | " : "",
                    (set & Gbi.G_LIGHTING)!=0 ? "G_LIGHTING | " : "",
                    (set & Gbi.G_SHADING_SMOOTH)!=0 ? "G_SHADING_SMOOTH | " : "",
                    (set & Gbi.G_ZBUFFER)!=0 ? "G_ZBUFFER | " : "",
                    (set & Gbi.G_TEXTURE_GEN)!=0 ? "G_TEXTURE_GEN | " : "",
                    (set & Gbi.G_TEXTURE_GEN_LINEAR)!=0 ? "G_TEXTURE_GEN_LINEAR | " : "",
                    (set & Gbi.G_CULL_FRONT)!=0 ? "G_CULL_FRONT | " : "",
                    (set & Gbi.G_CULL_BACK)!=0 ? "G_CULL_BACK | " : "",
                    (set & Gbi.G_FOG)!=0 ? "G_FOG | " : "",
                    (set & Gbi.G_CLIPPING)!=0 ? "G_CLIPPING" : "");
        }
    }
    
    public void gSPPopMatrixN(int param, int num) {
        super.gSPPopMatrixN(param, num);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "gSPPopMatrixN( %s, %d );\n",
                    (param == Gbi.G_MTX_MODELVIEW) ? "G_MTX_MODELVIEW" :
                        (param == Gbi.G_MTX_PROJECTION) ? "G_MTX_PROJECTION" : "G_MTX_INVALID", num);
        }
    }
    
    public void gSPNormals(int address) {
        super.gSPNormals(address);
    }
    
    public void gSPCoordMod(int coord, int index, int pos) {
        super.gSPCoordMod(coord, index, pos);
    }
    
    public void gSPVertexBg(int address, int n, int v0) {
        super.gSPVertexBg(address, n, v0);
    }
    
    public void gSPMatrixBillboard(int x) {
        super.gSPMatrixBillboard(x);
    }
    
    public void gSPMatrixModelViewi(int x) {
        super.gSPMatrixModelViewi(x);
    }
    
    public void gSPDMAMatrix(int address, int index, boolean multiply) {
        super.gSPDMAMatrix(address, index, multiply);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n",
                    tmpmtx[0][0], tmpmtx[0][1], tmpmtx[0][2], tmpmtx[0][3]);
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n",
                    tmpmtx[1][0], tmpmtx[1][1], tmpmtx[1][2], tmpmtx[1][3]);
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n",
                    tmpmtx[2][0], tmpmtx[2][1], tmpmtx[2][2], tmpmtx[2][3]);
            Debug.DebugMsg(Debug.DEBUG_DETAIL | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "// %12.6f %12.6f %12.6f %12.6f\n",
                    tmpmtx[3][0], tmpmtx[3][1], tmpmtx[3][2], tmpmtx[3][3]);
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_MATRIX,
                    "gSPDMAMatrix( 0x%08X, %d, %s );\n",
                    address, index, multiply ? "TRUE" : "FALSE");
        }
    }
    
    public void gSPDMAVertex(int address, int n, int v0, boolean append) {
        super.gSPDMAVertex(address, n, v0, append);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_VERTEX,
                    "gSPDMAVertex( 0x%08X, %d, %d );\n", address, n, v0);
        }
    }
    
    public void gSPDMADisplayList(int dl, int n) {
        super.gSPDMADisplayList(dl, n);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPDMADisplayList( 0x%08X, %d );\n", dl, n);
        }
    }
    
    public void gSPDMATriangles(int address, int n) {
        super.gSPDMATriangles(address, n);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED | Debug.DEBUG_TRIANGLE,
                    "gSPDMATriangles( 0x%08X, %d );\n", address, n);
        }
    }
    
    public void gSPSetDMAOffsets(int mtxoffset, int vtxoffset) {
        super.gSPSetDMAOffsets(mtxoffset, vtxoffset);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_HANDLED,
                    "gSPSetDMAOffsets( 0x%08X, 0x%08X );\n", mtxoffset, vtxoffset);
        }
    }
    
    public void gSPLine3D(int v0, int v1, int flag) {
        super.gSPLine3D(v0, v1, flag);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_UNHANDLED,
                    "gSPLine3D( %d, %d, %d );\n", v0, v1, flag);
        }
    }
    
    public void gSPLineW3D(int v0, int v1, int wd, int flag) {
        super.gSPLineW3D(v0, v1, wd, flag);
        if (DEBUG) {
            Debug.DebugMsg(Debug.DEBUG_HIGH | Debug.DEBUG_UNHANDLED,
                    "gSPLineW3D( %d, %d, %d, %d );\n", v0, v1, wd, flag);
        }
    }
    
}
