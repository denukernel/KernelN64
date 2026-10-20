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


package kernel64gl.engine.rsp;

import kernel64gl.engine.core.Gbi;
import kernel64gl.engine.opengl.OpenGl;
import kernel64gl.engine.opengl.OpenGlGdp;
import kernel64gl.engine.core.Rsp;
import kernel64gl.engine.rdp.Gdp;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.media.opengl.GL;


/** State owner and command facade; implementation helpers share this state.
 * Keep state fields here for compatibility with existing save states. */
public class Gsp {

	public static final int CHANGED_MATRIX = 0x02;
//    public static final int CHANGED_COLORBUFFER =	0x04;
	public static final int CHANGED_GEOMETRYMODE = 0x08;
	public static final int CHANGED_FOGPOSITION = 0x10;

//    public static final int CHANGED_VIEWPORT =		0x20;

	public static class SPVertex {
		public float[] vtx = new float[4];
		public float[] norm = new float[4];
		public float[] color = new float[4];
		public float[] tex = new float[2];
		public float[] clip = new float[3];
		public short flag;

		public String toString() {
			return String.format("%f %f %f %f - %f %f %f %f\n", vtx[0], vtx[1], vtx[2], vtx[3], color[0], color[1],
					color[2], color[3]);
		}

		public void clip() {
			if (vtx[0] < -vtx[3]) {
				clip[0] = -1.0f;
			} else if (vtx[0] > vtx[3]) {
				clip[0] = 1.0f;
			} else {
				clip[0] = 0.0f;
			}

			if (vtx[1] < -vtx[3]) {
				clip[1] = -1.0f;
			} else if (vtx[1] > vtx[3]) {
				clip[1] = 1.0f;
			} else {
				clip[1] = 0.0f;
			}

			if (vtx[3] <= 0.0f) {
				clip[2] = -1.0f;
			} else if (vtx[2] < -vtx[3]) {
				clip[2] = -0.1f;
			} else if (vtx[2] > vtx[3]) {
				clip[2] = 1.0f;
			} else {
				clip[2] = 0.0f;
			}
		}

		public void copyVertex(SPVertex src) {
			vtx[0] = src.vtx[0];
			vtx[1] = src.vtx[1];
			vtx[2] = src.vtx[2];
			vtx[3] = src.vtx[3];
			color[0] = src.color[0];
			color[1] = src.color[1];
			color[2] = src.color[2];
			color[3] = src.color[3];
			tex[0] = src.tex[0];
			tex[1] = src.tex[1];
		}

		public void interpolateVertex(float percent, SPVertex first, SPVertex second) {
			vtx[0] = first.vtx[0] + percent * (second.vtx[0] - first.vtx[0]);
			vtx[1] = first.vtx[1] + percent * (second.vtx[1] - first.vtx[1]);
			vtx[2] = first.vtx[2] + percent * (second.vtx[2] - first.vtx[2]);
			vtx[3] = first.vtx[3] + percent * (second.vtx[3] - first.vtx[3]);
			color[0] = first.color[0] + percent * (second.color[0] - first.color[0]);
			color[1] = first.color[1] + percent * (second.color[1] - first.color[1]);
			color[2] = first.color[2] + percent * (second.color[2] - first.color[2]);
			color[3] = first.color[3] + percent * (second.color[3] - first.color[3]);
			tex[0] = first.tex[0] + percent * (second.tex[0] - first.tex[0]);
			tex[1] = first.tex[1] + percent * (second.tex[1] - first.tex[1]);
		}
	};

	public static class Matrix {
		public int modelViewi;
		public int stackSize;
		public int billboard;
		public float[][][] modelView = new float[32][4][4];
		public float[][] projection = new float[4][4];
		public float[][] combined = new float[4][4];
	};

	public static class Light {
		public float[] color = new float[3];
		public float[] vec = new float[3];
		// for microcode F3DEXBG
		public float x;
		public float y;
		public float z;
		public float w;
		public int nonzero;
		public int nonblack;
		public float a;
		public float ca;
	};

	public static class Fog {
		public short multiplier;
		public short offset;
	};

	public static class DMAOffsets {
		public int vtx;
		public int mtx;
	};

	public static class Viewport {
		public float[] vscale = new float[4];
		public float[] vtrans = new float[4];
//        public float x;
//        public float y;
//        public float width;
//        public float height;
//        public float nearz;
//        public float farz;
	};

	// used by OpenGl
	public Viewport viewport = new Viewport();
	// used by Opcodes
	public int pcStackSize;
//    // used by OpenGl, TextureCache
//    public int DList;
	// only used by OpenGl
	public Fog fog = new Fog();
	// used by OpenGl, S2dex, S2dex2
	public int geometryMode;

	public int changed;

	private int[] PC = new int[18];
	private int PCi;
	private int cmd;
	int nextCmd;
	private boolean halt;
	public int[] segment = new int[16];
	Matrix matrix = new Matrix();
	int vertexi;
	Light[] lights = new Light[12];
	SPVertex[] vertices = new SPVertex[80];
	private int[] status = new int[4];
	DMAOffsets DMAOffsets = new DMAOffsets();
	int numLights;
	int uc8_normale_addr = 0;
	float[] uc8_coord_mod = new float[16];
	protected float[][] tmpmtx = new float[4][4];

	protected ByteBuffer rdram;
	private ByteBuffer dmem;
	int rdramSize;
	Microcodes gbi = new Microcodes();

	float[][] identityMatrix = { { 1.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 1.0f, 0.0f, 0.0f },
			{ 0.0f, 0.0f, 1.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 1.0f } };

	/** Creates a new instance of GSP */
	public Gsp(ByteBuffer rdram, ByteBuffer dmem) {
		this.rdram = rdram;
		this.dmem = dmem;
		this.rdramSize = rdram.capacity();
		for (int i = 0; i < lights.length; i++) {
			lights[i] = new Light();
		}
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = new SPVertex();
		}
		gbi.init();
	}
	public void reset() {
	    PCi = 0;
	    for (int i = 0; i < PC.length; i++) PC[i] = 0;
	    cmd = 0;
	    nextCmd = 0;
	    halt = false;

	    for (int i = 0; i < segment.length; i++) segment[i] = 0;

	    matrix.modelViewi = 0;
	    matrix.stackSize = 32;

	    for (int i = 0; i < matrix.modelView.length; i++)
	        for (int j = 0; j < 4; j++)
	            for (int k = 0; k < 4; k++)
	                matrix.modelView[i][j][k] = 0f;

	    for (int j = 0; j < 4; j++)
	        matrix.modelView[0][j][j] = 1f;

	    for (int j = 0; j < 4; j++)
	        for (int k = 0; k < 4; k++) {
	            matrix.projection[j][k] = 0f;
	            matrix.combined[j][k] = 0f;
	        }

	    for (int j = 0; j < 4; j++)
	        matrix.projection[j][j] = 1f;

	    for (int i = 0; i < vertices.length; i++) {
	        SPVertex v = vertices[i];
	        for (int j = 0; j < 4; j++) {
	            v.vtx[j] = 0f;
	            v.norm[j] = 0f;
	            v.color[j] = 0f;
	        }
	        v.tex[0] = 0f;
	        v.tex[1] = 0f;
	        v.clip[0] = v.clip[1] = v.clip[2] = 0f;
	        v.flag = 0;
	    }

	    for (int i = 0; i < lights.length; i++) {
	        lights[i].color[0] = 0f;
	        lights[i].color[1] = 0f;
	        lights[i].color[2] = 0f;
	        lights[i].vec[0] = lights[i].vec[1] = lights[i].vec[2] = 0f;
	        lights[i].x = lights[i].y = lights[i].z = lights[i].w = 0f;
	        lights[i].nonzero = 0;
	        lights[i].nonblack = 0;
	        lights[i].a = 0f;
	        lights[i].ca = 0f;
	    }

	    numLights = 0;
	    vertexi = 0;
	    geometryMode = 0;
	    changed = 0;
	    DMAOffsets.mtx = 0;
	    DMAOffsets.vtx = 0;
	    pcStackSize = 0;
	    fog.multiplier = 0;
	    fog.offset = 0;
	    uc8_normale_addr = 0;

	    for (int i = 0; i < uc8_coord_mod.length; i++)
	        uc8_coord_mod[i] = 0f;

	    for (int i = 0; i < status.length; i++)
	        status[i] = 0;
	}

	public void loadUcode(int uc_start, int uc_dstart) {
		matrix.stackSize = StrictMath.min(32, dmem.getInt(0x0FE4) >> 6);
		if (matrix.stackSize == 0) {
			matrix.stackSize = Gbi.G_MTX_STACKSIZE;
		}
		matrix.modelViewi = 0;
		changed |= CHANGED_MATRIX;

		for (int i = 0; i < 4; i++)
			Arrays.fill(matrix.modelView[0][i], 0.0f);

		matrix.modelView[0][0][0] = 1.0f;
		matrix.modelView[0][1][1] = 1.0f;
		matrix.modelView[0][2][2] = 1.0f;
		matrix.modelView[0][3][3] = 1.0f;

		int uc_start_t = dmem.getInt(0x0FD0);
		int uc_dstart_t = dmem.getInt(0x0FD8);
		int uc_dsize_t = dmem.getInt(0x0FDC);

		if ((uc_start_t != uc_start) || (uc_dstart_t != uc_dstart))
			gSPLoadUcodeEx(uc_start_t, uc_dstart_t, (short) uc_dsize_t);
	}

	// called by OpenGl
	public void RSP_ProcessDList() {
		PC[0] = dmem.getInt(0x0FF0);
		PCi = 0;

		byte[] ram = rdram.array();

		halt = false;

		OpenGlGdp.cache.reset();

		while (!halt) {
			if ((PC[PCi] + 8) > rdramSize) {

				break;
			}

			int pAddr = PC[PCi];
//            int w0 = rdram.getInt(PC[PCi]);
			int w0 = (ram[pAddr] << 24) | ((ram[pAddr + 1] & 0xff) << 16) | ((ram[pAddr + 2] & 0xff) << 8)
					| (ram[pAddr + 3] & 0xff);
//            int w1 = rdram.getInt(PC[PCi] + 4);
			int w1 = (ram[pAddr + 4] << 24) | ((ram[pAddr + 5] & 0xff) << 16) | ((ram[pAddr + 6] & 0xff) << 8)
					| (ram[pAddr + 7] & 0xff);
			cmd = (w0 >> 24) & Gbi.SR_MASK_8; // &0xff

			PC[PCi] += 8;

			pAddr = PC[PCi];
			nextCmd = (ram[pAddr] << 24) | ((ram[pAddr + 1] & 0xff) << 16) | ((ram[pAddr + 2] & 0xff) << 8)
					| (ram[pAddr + 3] & 0xff);
			nextCmd = (nextCmd >> 24) & Gbi.SR_MASK_8;

			gbi.cmds[cmd].exec(w0, w1);
		}


		Rsp.gdp.DList++;
		Rsp.gdp.changed |= Rsp.gdp.CHANGED_COLORBUFFER;
	}

	public int getCmd() {
		int w2 = rdram.getInt(PC[PCi] + 4);
		PC[PCi] += 8;
		return w2;
	}

	// called by Opcodes
	public void setUcode(int cmd, GBIFunc uCode) {
		gbi.cmds[cmd] = uCode;
	}

	// called by Opcodes
	public final int RSP_SegmentToPhysical(int segaddr) {
		return (segment[(segaddr >> 24) & 0x0F] + (segaddr & 0x00FFFFFF)) & 0x03FFFFFF;
	}

	// F3D

	// called by F3d
	public void gSPNoOp() {

	}

	// called by F3d
	public void gSPEndDisplayList() {
		if (PCi > 0)
			PCi--;
		else
			halt = true;
	}

	// called by F3d
	public void gSPSetGeometryMode(int mode) {
		geometryMode |= mode;
		changed |= CHANGED_GEOMETRYMODE;
	}

	// called by F3d
	public void gSPClearGeometryMode(int mode) {
		geometryMode &= ~mode;
		changed |= CHANGED_GEOMETRYMODE;
	}

	// called by F3d
	public void gSPDisplayList(int address) {
		if ((address + 8) > rdramSize)
			return;
		if (PCi < (pcStackSize - 1)) {
			PCi++;
			PC[PCi] = address;
		}
	}

	// called by F3d
	public void gSPBranchList(int address) {
		if ((address + 8) > rdramSize)
			return;
		PC[PCi] = address;
	}

	// called by F3d
	public void gSPPopMatrix(int param) {
		GspMatrixCommands.gSPPopMatrix(this, param);
	}

	// called by F3d, F3dex2, F3dexbg
	public void gSPViewport(int address) {
		GspMatrixCommands.gSPViewport(this, address);
	}

	// called by F3d, F3dex2
	public void gSPMatrix(int address, int param) {
		GspMatrixCommands.gSPMatrix(this, address, param);
	}

	// called by F3d, F3dex2
	public void gSPForceMatrix(int address) {
		GspMatrixCommands.gSPForceMatrix(this, address);
	}

	// called by F3d, F3dex2
	public void gSPLight(int address, int n) {
		if ((address + 12) > rdramSize)
			return;
		n--;
		if (n < 12) {
			lights[n].color[0] = (rdram.get(address) & 0xFF) * 0.0039215689f;
			lights[n].color[1] = (rdram.get(address + 1) & 0xFF) * 0.0039215689f;
			lights[n].color[2] = (rdram.get(address + 2) & 0xFF) * 0.0039215689f;
			lights[n].vec[0] = rdram.get(address + 8);
			lights[n].vec[1] = rdram.get(address + 9);
			lights[n].vec[2] = rdram.get(address + 10);
			Math3D.normalize(lights[n].vec);
		}
	}

	// called by F3dexbg
	public void gSPLightBg(int address, int n) {
		n -= 2;
		if (n >= 0) {
			int col = rdram.get(address + 0) & 0xFF;
			lights[n].color[0] = (float) col / 255.0f;
			lights[n].nonblack = col;
			col = rdram.get(address + 1) & 0xFF;
			lights[n].color[1] = (float) col / 255.0f;
			lights[n].nonblack += col;
			col = rdram.get(address + 2) & 0xFF;
			lights[n].color[2] = (float) col / 255.0f;
			lights[n].nonblack += col;
			lights[n].a = 1.0f;
			lights[n].vec[0] = (float) rdram.get(address + 8) / 127.0f;
			lights[n].vec[1] = (float) rdram.get(address + 9) / 127.0f;
			lights[n].vec[2] = (float) rdram.get(address + 10) / 127.0f;
			int a = address >> 1;
			lights[n].x = (float) rdram.asShortBuffer().get(a + 16);
			lights[n].y = (float) rdram.asShortBuffer().get(a + 17);
			lights[n].z = (float) rdram.asShortBuffer().get(a + 18);
			lights[n].w = (float) rdram.asShortBuffer().get(a + 19);
			lights[n].nonzero = rdram.get(address + 12);
			lights[n].ca = (float) lights[n].nonzero / 16.0f;
		}
	}

	// called by F3d, F3dex, F3dex2
	public void gSPVertex(int address, int n, int v0) {
		GspVertexProcessor.gSPVertex(this, address, n, v0);
	}

	// called by F3d, F3dex, F3dex2
	public void gSP1Triangle(int v0, int v1, int v2, int flag) {
		GspPrimitiveCommands.gSP1Triangle(this, v0, v1, v2, flag);
	}

	// called by F3d, F3dexbg
	public void gSP4Triangles(int v00, int v01, int v02, int v10, int v11, int v12, int v20, int v21, int v22, int v30,
			int v31, int v32) {
		GspPrimitiveCommands.gSP4Triangles(this, v00, v01, v02, v10, v11, v12, v20, v21, v22, v30, v31, v32);
	}

	// called by F3d, F3dex
	public void gSP1Quadrangle(int v0, int v1, int v2, int v3) {
		GspPrimitiveCommands.gSP1Quadrangle(this, v0, v1, v2, v3);
	}

	// called by F3d, F3dex
	public void gSPCullDisplayList(int v0, int vn) {
		if (gSPCullVertices(v0, vn)) {
			if (PCi > 0)
				PCi--;
			else
				halt = true;
		}
	}

	// called by F3d, F3dex2, F3dexbg
	public void gSPSegment(int seg, int base) {
		if (seg > 0xF)
			return;
		if (base > rdramSize - 1)
			return;
		segment[seg] = base;
	}

	// called by F3d, F3dex2
	public void gSPClipRatio(int r) {

	}

	// called by F3d, F3dex2
	public void gSPInsertMatrix(int where, int num) {
		GspMatrixCommands.gSPInsertMatrix(this, where, num);
	}

	// called by F3d, F3dex
	public void gSPModifyVertex(int vtx, int where, int val) {
		GspVertexProcessor.gSPModifyVertex(this, vtx, where, val);
	}

	// called by F3d, F3dex2, F3dexbg
	public void gSPNumLights(int n) {
		if (n <= 8)
			numLights = n;
	}

	// called by F3d, F3dex2
	public void gSPLightColor(int lightNum, int packedColor) {
		lightNum--;
		if (lightNum < 12) {
			lights[lightNum].color[0] = ((packedColor >> 24) & Gbi.SR_MASK_8) * 0.0039215689f;
			lights[lightNum].color[1] = ((packedColor >> 16) & Gbi.SR_MASK_8) * 0.0039215689f;
			lights[lightNum].color[2] = ((packedColor >> 8) & Gbi.SR_MASK_8) * 0.0039215689f;
		}
	}

	// called by F3d, F3dex2, F3dexbg
	public void gSPFogFactor(short fm, short fo) {
        fog.multiplier = fm;
        fog.offset = fo;
        changed |= CHANGED_FOGPOSITION;
	}

	// called by F3d, F3dex2
	public void gSPPerspNormalize(short scale) {

	}

	// F3DEX

	// called by F3dex
	public void gSPLoadUcodeEx(int uc_start, int uc_dstart, short uc_dsize) {
		PCi = 0;
		matrix.modelViewi = 0;
		changed |= CHANGED_MATRIX;
		status[0] = status[1] = status[2] = status[3] = 0;

		if ((((uc_start & 0x1FFFFFFF) + 4096) > rdramSize) || (((uc_dstart & 0x1FFFFFFF) + uc_dsize) > rdramSize))
			return;

		Microcode ucode = gbi.detectMicrocode(uc_start, uc_dstart, uc_dsize);

		if (ucode.type != Microcode.NONE)
			gbi.makeCurrent(ucode);
	}

	// called by F3dex
	public void gSPBranchLessZ(int address, int vtx, float zval) {
		if ((address + 8) > rdramSize)
			return;
		if (vertices[vtx].vtx[2] <= zval)
			PC[PCi] = address;
	}

	// called by F3dex, F3dex2
	public void gSP2Triangles(int v00, int v01, int v02, int flag0, int v10, int v11, int v12, int flag1) {
		GspPrimitiveCommands.gSP2Triangles(this, v00, v01, v02, flag0, v10, v11, v12, flag1);
	}

	// called by F3dex2
	public void gSPGeometryMode(int clear, int set) {
		geometryMode = (geometryMode & ~clear) | set;
		changed |= CHANGED_GEOMETRYMODE;
	}

	// called by F3dex2
	public void gSPPopMatrixN(int param, int num) {
		GspMatrixCommands.gSPPopMatrixN(this, param, num);
	}

	public void gSPNormals(int address) {
		uc8_normale_addr = address;
	}

	public void gSPCoordMod(int coord, int index, int pos) {
		if (pos == 0) {
			uc8_coord_mod[0 + index] = (short) (coord >> 16);
			uc8_coord_mod[1 + index] = (short) (coord & 0xffff);
		} else if (pos == 0x10) {
			uc8_coord_mod[4 + index] = (coord >> 16) / 65536.0f;
			uc8_coord_mod[5 + index] = (coord & 0xffff) / 65536.0f;
			uc8_coord_mod[12 + index] = uc8_coord_mod[0 + index] + uc8_coord_mod[4 + index];
			uc8_coord_mod[13 + index] = uc8_coord_mod[1 + index] + uc8_coord_mod[5 + index];
		} else if (pos == 0x20) {
			uc8_coord_mod[8 + index] = (short) (coord >> 16);
			uc8_coord_mod[9 + index] = (short) (coord & 0xffff);
		}
	}

	// called by F3dexbg
	public void gSPVertexBg(int address, int n, int v0) {
		GspVertexProcessor.gSPVertexBg(this, address, n, v0);
	}

	// F3DDKR

	public void gSPMatrixBillboard(int x) {
		matrix.billboard = x;
	}

	public void gSPMatrixModelViewi(int x) {
		matrix.modelViewi = x;
		changed |= CHANGED_MATRIX;
	}

	// called by F3ddkr
	public void gSPDMAMatrix(int address, int index, boolean multiply) {
		GspMatrixCommands.gSPDMAMatrix(this, address, index, multiply);
	}

	// called by F3ddkr
	public void gSPDMAVertex(int address, int n, int v0, boolean append) {
		GspVertexProcessor.gSPDMAVertex(this, address, n, v0, append);
	}

	// called by F3ddkr
	public void gSPDMADisplayList(int dl, int n) {
		if ((dl + (n << 3)) > rdramSize)
			return;

		int curDL = PC[PCi];

		PC[PCi] = RSP_SegmentToPhysical(dl);

		while ((PC[PCi] - dl) < (n << 3)) {
			if ((PC[PCi] + 8) > rdramSize)
				break;

			int w0 = rdram.getInt(PC[PCi]);
			int w1 = rdram.getInt(PC[PCi] + 4);

			PC[PCi] += 8;
			nextCmd = (rdram.getInt(PC[PCi]) >> 24) & Gbi.SR_MASK_8;

			gbi.cmds[(w0 >> 24) & Gbi.SR_MASK_8].exec(w0, w1);
		}

		PC[PCi] = curDL;
	}

	// called by F3ddkr
	public void gSPDMATriangles(int address, int n) {
		GspPrimitiveCommands.gSPDMATriangles(this, address, n);
	}

	// called by F3ddkr, F3dpd
	public void gSPSetDMAOffsets(int mtxoffset, int vtxoffset) {
		DMAOffsets.mtx = mtxoffset;
		DMAOffsets.vtx = vtxoffset;
	}

	// called by L3d, L3dex, L3dex2
	public void gSPLine3D(int v0, int v1, int flag) {
		GspPrimitiveCommands.gSPLine3D(this, v0, v1, flag);
	}

	// called by L3d, L3dex, L3dex2
	public void gSPLineW3D(int v0, int v1, int wd, int flag) {
		GspPrimitiveCommands.gSPLineW3D(this, v0, v1, wd, flag);
	}

	// Private Methods /////////////////////////////////////////////////////////

	// Matrices

	// f32 mtx[4][4]
	void loadMatrix(float[][] mtx, int address) {
		GspMatrixCommands.loadMatrix(this, mtx, address);
	}

	void gSPCombineMatrices() {
		GspMatrixCommands.gSPCombineMatrices(this);
	}

	// Vertices

	void gSPProcessVertex(SPVertex vert) {
		GspVertexProcessor.gSPProcessVertex(this, vert);
	}


	private boolean gSPCullVertices(int v0, int vn) {
		return GspVertexProcessor.gSPCullVertices(this, v0, vn);
	}

	// Triangles

	void gSPFlushTriangles() {
		GspPrimitiveCommands.gSPFlushTriangles(this);
	}

	void gSPTriangle(int v0, int v1, int v2, int flag) {
		GspPrimitiveCommands.gSPTriangle(this, v0, v1, v2, flag);
	}

}
