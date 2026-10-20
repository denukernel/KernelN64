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
import static kernel64gl.engine.rsp.Gsp.*;

/** Vertex decoding, lighting, transformation, and clipping. */
final class GspVertexProcessor {
    private GspVertexProcessor() {}

	static void gSPVertex(Gsp state, int address, int n, int v0) {
		if (address < 0 || n < 0 || v0 < 0 || (long) address + 16L * n > state.rdramSize)
			return;
		if ((long) n + v0 <= state.vertices.length) {
			SPVertex vertex;
			for (int i = v0; i < n + v0; i++) {
				vertex = state.vertices[i];
				vertex.vtx[0] = state.rdram.getShort(address);
				vertex.vtx[1] = state.rdram.getShort(address + 2);
				vertex.vtx[2] = state.rdram.getShort(address + 4);

				vertex.flag = state.rdram.getShort(address + 6);
				vertex.tex[0] = state.rdram.getShort(address + 8) * Gbi.FIXED2FLOATRECIP5;
				vertex.tex[1] = state.rdram.getShort(address + 10) * Gbi.FIXED2FLOATRECIP5;

				if ((state.geometryMode & Gbi.G_LIGHTING) != 0) {
					vertex.norm[0] = state.rdram.get(address + 12);
					vertex.norm[1] = state.rdram.get(address + 13);
					vertex.norm[2] = state.rdram.get(address + 14);
					vertex.color[3] = (state.rdram.get(address + 15) & 0xFF) * 0.0039215689f;
				} else {
					vertex.color[0] = (state.rdram.get(address + 12) & 0xFF) * 0.0039215689f;
					vertex.color[1] = (state.rdram.get(address + 13) & 0xFF) * 0.0039215689f;
					vertex.color[2] = (state.rdram.get(address + 14) & 0xFF) * 0.0039215689f;
					vertex.color[3] = (state.rdram.get(address + 15) & 0xFF) * 0.0039215689f;
				}

				state.gSPProcessVertex(vertex);
				address += 16;
			}
		}
	}

	static void gSPVertexBg(Gsp state, int address, int n, int v0) {
		if ((state.changed & CHANGED_MATRIX) != 0)
			state.gSPCombineMatrices();

		if (v0 - n < 0)
			return;

		for (int i = 0; i < (n << 4); i += 16) {
			SPVertex vertex = state.vertices[v0 - n + (i >> 4)];
			vertex.vtx[0] = (float) state.rdram.asShortBuffer().get(((address + i) >> 1) + 0);
			vertex.vtx[1] = (float) state.rdram.asShortBuffer().get(((address + i) >> 1) + 1);
			vertex.vtx[2] = (float) state.rdram.asShortBuffer().get(((address + i) >> 1) + 2);
			vertex.flag = state.rdram.asShortBuffer().get(((address + i) >> 1) + 3);
			vertex.tex[0] = (float) state.rdram.asShortBuffer().get(((address + i) >> 1) + 4) * 0.03125f;
			vertex.tex[1] = (float) state.rdram.asShortBuffer().get(((address + i) >> 1) + 5) * 0.03125f;
			vertex.color[3] = (state.rdram.get((address + i) + 15) & 0xFF) * 0.0039215689f;

			Math3D.transformVertex(vertex.vtx, state.matrix.combined);

			if (vertex.vtx[0] < -vertex.vtx[3])
				vertex.clip[0] = -1.0f;
			else if (vertex.vtx[0] > vertex.vtx[3])
				vertex.clip[0] = 1.0f;
			else
				vertex.clip[0] = 0.0f;

			if (vertex.vtx[1] < -vertex.vtx[3])
				vertex.clip[1] = -1.0f;
			else if (vertex.vtx[1] > vertex.vtx[3])
				vertex.clip[1] = 1.0f;
			else
				vertex.clip[1] = 0.0f;

			if (vertex.vtx[3] <= 0.0f)
				vertex.clip[2] = -1.0f;
			else if (vertex.vtx[2] < -vertex.vtx[3])
				vertex.clip[2] = -0.1f;
			else if (vertex.vtx[2] > vertex.vtx[3])
				vertex.clip[2] = 1.0f;
			else
				vertex.clip[2] = 0.0f;

			vertex.color[0] = (state.rdram.get((address + i) + 12) & 0xFF) * 0.0039215689f;
			vertex.color[1] = (state.rdram.get((address + i) + 13) & 0xFF) * 0.0039215689f;
			vertex.color[2] = (state.rdram.get((address + i) + 14) & 0xFF) * 0.0039215689f;

			if ((state.geometryMode & 0x00020000 /* Gbi.G_LIGHTING */) != 0) {
				int shift = (v0 - n) << 1;
				vertex.norm[0] = state.rdram.get(state.uc8_normale_addr + (i >> 3) + shift + 0);
				vertex.norm[1] = state.rdram.get(state.uc8_normale_addr + (i >> 3) + shift + 1);
				vertex.norm[2] = (byte) (vertex.flag & 0xff);

				Math3D.transformVector(vertex.norm, state.matrix.modelView[state.matrix.modelViewi]);
				Math3D.normalize(vertex.norm);
				if ((state.geometryMode & 0x00080000 /* Gbi.G_TEXTURE_GEN_LINEAR */) != 0) {
					vertex.tex[0] = (float) StrictMath.acos(vertex.norm[0]) * 325.94931f;
					vertex.tex[1] = (float) StrictMath.acos(vertex.norm[1]) * 325.94931f;
				} else { // G_TEXTURE_GEN
					vertex.tex[0] = (vertex.norm[0] + 1.0f) * 512.0f;
					vertex.tex[1] = (vertex.norm[1] + 1.0f) * 512.0f;
				}

				float[] color = { state.lights[state.numLights].color[0], state.lights[state.numLights].color[1], state.lights[state.numLights].color[2] };
				float light_intensity = 0.0f;
				int l;
				if ((state.geometryMode & 0x00400000) != 0) {
					Math3D.normalize(vertex.norm);
					for (l = 0; l < state.numLights - 1; l++) {
						if (state.lights[l].nonblack == 0)
							continue;
						light_intensity = Math3D.dotProduct(vertex.norm, state.lights[l].vec);
						if (light_intensity < 0.0f)
							continue;
						if (state.lights[l].ca > 0.0f) {
							float vx = (vertex.vtx[0] + state.uc8_coord_mod[8]) * state.uc8_coord_mod[12] - state.lights[l].x;
							float vy = (vertex.vtx[1] + state.uc8_coord_mod[9]) * state.uc8_coord_mod[13] - state.lights[l].y;
							float vz = (vertex.vtx[2] + state.uc8_coord_mod[10]) * state.uc8_coord_mod[14] - state.lights[l].z;
							float vw = (vertex.vtx[3] + state.uc8_coord_mod[11]) * state.uc8_coord_mod[15] - state.lights[l].w;
							float len = (vx * vx + vy * vy + vz * vz + vw * vw) / 65536.0f;
							float p_i = state.lights[l].ca / len;
							if (p_i > 1.0f)
								p_i = 1.0f;
							light_intensity *= p_i;
						}
						color[0] += state.lights[l].color[0] * light_intensity;
						color[1] += state.lights[l].color[1] * light_intensity;
						color[2] += state.lights[l].color[2] * light_intensity;
					}
					light_intensity = Math3D.dotProduct(vertex.norm, state.lights[l].vec);
					if (light_intensity > 0.0f) {
						color[0] += state.lights[l].color[0] * light_intensity;
						color[1] += state.lights[l].color[1] * light_intensity;
						color[2] += state.lights[l].color[2] * light_intensity;
					}
				} else {
					for (l = 0; l < state.numLights; l++) {
						if (state.lights[l].nonblack != 0 && state.lights[l].nonzero != 0) {
							float vx = (vertex.vtx[0] + state.uc8_coord_mod[8]) * state.uc8_coord_mod[12] - state.lights[l].x;
							float vy = (vertex.vtx[1] + state.uc8_coord_mod[9]) * state.uc8_coord_mod[13] - state.lights[l].y;
							float vz = (vertex.vtx[2] + state.uc8_coord_mod[10]) * state.uc8_coord_mod[14] - state.lights[l].z;
							float vw = (vertex.vtx[3] + state.uc8_coord_mod[11]) * state.uc8_coord_mod[15] - state.lights[l].w;
							float len = (vx * vx + vy * vy + vz * vz + vw * vw) / 65536.0f;
							light_intensity = state.lights[l].ca / len;
							if (light_intensity > 1.0f)
								light_intensity = 1.0f;
							color[0] += state.lights[l].color[0] * light_intensity;
							color[1] += state.lights[l].color[1] * light_intensity;
							color[2] += state.lights[l].color[2] * light_intensity;
						}
					}
				}
				if (color[0] > 1.0f)
					color[0] = 1.0f;
				if (color[1] > 1.0f)
					color[1] = 1.0f;
				if (color[2] > 1.0f)
					color[2] = 1.0f;
				vertex.color[0] = vertex.color[0] * color[0];
				vertex.color[1] = vertex.color[1] * color[1];
				vertex.color[2] = vertex.color[2] * color[2];
			}
		}
	}

	static void gSPDMAVertex(Gsp state, int address, int n, int v0, boolean append) {
		v0 = state.vertexi + v0;
		if (append) {
			if ((state.matrix.billboard) != 0) {
				state.vertexi = 1;
			}
		} else {
			state.vertexi = 0;
		}

		address = state.DMAOffsets.vtx + address;

		if ((address + 10 * n) > state.rdramSize)
			return;

		if ((n + v0) < (80)) {
			SPVertex vertex;
			for (int i = v0; i < n + v0; i++) {
				vertex = state.vertices[i];
				vertex.vtx[0] = state.rdram.getShort(address ^ 2);
				vertex.vtx[1] = state.rdram.getShort((address + 2) ^ 2);
				vertex.vtx[2] = state.rdram.getShort((address + 4) ^ 2);

				if ((state.geometryMode & Gbi.G_LIGHTING) != 0) {
					vertex.norm[0] = state.rdram.get((address + 6) ^ 3);
					vertex.norm[1] = state.rdram.get((address + 7) ^ 3);
					vertex.norm[2] = state.rdram.get((address + 8) ^ 3);
					vertex.color[3] = (state.rdram.get((address + 9) ^ 3) & 0xFF) * 0.0039215689f;
				} else {
					vertex.color[0] = (state.rdram.get((address + 6) ^ 3) & 0xFF) * 0.0039215689f;
					vertex.color[1] = (state.rdram.get((address + 7) ^ 3) & 0xFF) * 0.0039215689f;
					vertex.color[2] = (state.rdram.get((address + 8) ^ 3) & 0xFF) * 0.0039215689f;
					vertex.color[3] = (state.rdram.get((address + 9) ^ 3) & 0xFF) * 0.0039215689f;
				}

				state.gSPProcessVertex(vertex);
				address += 10;
			}
		}

		state.vertexi += n;
	}

	static void gSPModifyVertex(Gsp state, int vtx, int where, int val) {
		switch (where) {
		case Gbi.G_MWO_POINT_RGBA:
			state.vertices[vtx].color[0] = ((val >> 24) & Gbi.SR_MASK_8) * 0.0039215689f;
			state.vertices[vtx].color[1] = ((val >> 16) & Gbi.SR_MASK_8) * 0.0039215689f;
			state.vertices[vtx].color[2] = ((val >> 8) & Gbi.SR_MASK_8) * 0.0039215689f;
			state.vertices[vtx].color[3] = (val & Gbi.SR_MASK_8) * 0.0039215689f;
			break;
		case Gbi.G_MWO_POINT_ST:
			state.vertices[vtx].tex[0] = ((short) ((val >> 16) & Gbi.SR_MASK_16)) * Gbi.FIXED2FLOATRECIP5;
			state.vertices[vtx].tex[1] = ((short) (val & Gbi.SR_MASK_16)) * Gbi.FIXED2FLOATRECIP5;
			break;
		case Gbi.G_MWO_POINT_XYSCREEN:
			break;
		case Gbi.G_MWO_POINT_ZSCREEN:
			break;
		}
	}

	static void gSPProcessVertex(Gsp state, SPVertex vert) {
		float intensity;
		float r, g, b;

		if ((state.changed & CHANGED_MATRIX) != 0)
			state.gSPCombineMatrices();

		Math3D.transformVertex(vert.vtx, state.matrix.combined);

		if (state.matrix.billboard != 0) {
			vert.vtx[0] += state.vertices[0].vtx[0];
			vert.vtx[1] += state.vertices[0].vtx[1];
			vert.vtx[2] += state.vertices[0].vtx[2];
			vert.vtx[3] += state.vertices[0].vtx[3];
		}

		if ((state.geometryMode & Gbi.G_ZBUFFER) == 0) {
			vert.vtx[2] = -vert.vtx[3];
		}

		if ((state.geometryMode & Gbi.G_LIGHTING) != 0) {
			Math3D.transformVector(vert.norm, state.matrix.modelView[state.matrix.modelViewi]);
			Math3D.normalize(vert.norm);

			r = state.lights[state.numLights].color[0];
			g = state.lights[state.numLights].color[1];
			b = state.lights[state.numLights].color[2];

			for (int i = 0; i < state.numLights; i++) {
				intensity = Math3D.dotProduct(vert.norm, state.lights[i].vec);

				if (intensity < 0.0f) {
					intensity = 0.0f;
				}

				r += state.lights[i].color[0] * intensity;
				g += state.lights[i].color[1] * intensity;
				b += state.lights[i].color[2] * intensity;
			}

			vert.color[0] = r;
			vert.color[1] = g;
			vert.color[2] = b;

			if ((state.geometryMode & Gbi.G_TEXTURE_GEN) != 0) {
				Math3D.transformVector(vert.norm, state.matrix.projection);

				Math3D.normalize(vert.norm);

				if ((state.geometryMode & Gbi.G_TEXTURE_GEN_LINEAR) != 0) {
					vert.tex[0] = (float) StrictMath.acos(vert.norm[0]) * 325.94931f;
					vert.tex[1] = (float) StrictMath.acos(vert.norm[1]) * 325.94931f;
				} else { // G_TEXTURE_GEN
					vert.tex[0] = (vert.norm[0] + 1.0f) * 512.0f;
					vert.tex[1] = (vert.norm[1] + 1.0f) * 512.0f;
				}
			}
		}

		vert.clip();
	}

	static boolean gSPCullVertices(Gsp state, int v0, int vn) {

		// ---- corruption-safe clamp ----
		if (v0 < 0)
			v0 = 0;
		if (vn < 0)
			vn = 0;

		if (v0 >= state.vertices.length)
			return false;
		if (vn >= state.vertices.length)
			return false;

		if (v0 > vn) {
			int tmp = v0;
			v0 = vn;
			vn = tmp;
		}
		// --------------------------------

		float xClip = 0.0f;
		float yClip = 0.0f;
		float zClip = 0.0f;

		for (int i = v0; i <= vn; i++) {
			SPVertex v = state.vertices[i];
			if (v == null)
				return false;

			if (v.clip[0] == 0.0f)
				return false;
			if (v.clip[1] == 0.0f)
				return false;
			if (v.clip[2] == 0.0f)
				return false;

			if (v.clip[0] < 0.0f) {
				if (xClip > 0.0f)
					return false;
				xClip = v.clip[0];
			} else if (v.clip[0] > 0.0f) {
				if (xClip < 0.0f)
					return false;
				xClip = v.clip[0];
			}

			if (v.clip[1] < 0.0f) {
				if (yClip > 0.0f)
					return false;
				yClip = v.clip[1];
			} else if (v.clip[1] > 0.0f) {
				if (yClip < 0.0f)
					return false;
				yClip = v.clip[1];
			}

			if (v.clip[2] < 0.0f) {
				if (zClip > 0.0f)
					return false;
				zClip = v.clip[2];
			} else if (v.clip[2] > 0.0f) {
				if (zClip < 0.0f)
					return false;
				zClip = v.clip[2];
			}
		}

		return true;
	}
}
