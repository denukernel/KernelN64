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
import javax.media.opengl.GL;
import static kernel64gl.engine.rsp.Gsp.*;

/** Triangle submission, batching, and near-plane clipping. */
final class GspPrimitiveCommands {
    private GspPrimitiveCommands() {}

	static void gSPTriangle(Gsp state, int v0, int v1, int v2, int flag) {
		if ((v0 < 80) && (v1 < 80) && (v2 < 80)) {
			// Don't bother with triangles completely outside clipping frustrum
			if (((state.vertices[v0].clip[0] < 0.0f) && (state.vertices[v1].clip[0] < 0.0f) && (state.vertices[v2].clip[0] < 0.0f))
					|| ((state.vertices[v0].clip[0] > 0.0f) && (state.vertices[v1].clip[0] > 0.0f) && (state.vertices[v2].clip[0] > 0.0f))
					|| ((state.vertices[v0].clip[1] < 0.0f) && (state.vertices[v1].clip[1] < 0.0f) && (state.vertices[v2].clip[1] < 0.0f))
					|| ((state.vertices[v0].clip[1] > 0.0f) && (state.vertices[v1].clip[1] > 0.0f) && (state.vertices[v2].clip[1] > 0.0f))
					|| ((state.vertices[v0].clip[2] > 0.1f) && (state.vertices[v1].clip[2] > 0.1f) && (state.vertices[v2].clip[2] > 0.1f))
					|| ((state.vertices[v0].clip[2] < -0.1f) && (state.vertices[v1].clip[2] < -0.1f)
							&& (state.vertices[v2].clip[2] < -0.1f)))
				return;

			// NoN work-around, clips triangles, and draws the clipped-off parts with
			// clamped z
			if (state.gbi.current.NoN && ((state.vertices[v0].clip[2] < 0.0f) || (state.vertices[v1].clip[2] < 0.0f)
					|| (state.vertices[v2].clip[2] < 0.0f))) {
				SPVertex[] nearVertices = new SPVertex[4];
				SPVertex[] clippedVertices = new SPVertex[4];
				for (int i = 0; i < 4; i++) { // TMP
					nearVertices[i] = new SPVertex();
					clippedVertices[i] = new SPVertex();
				}

				int nearIndex = 0;
				int clippedIndex = 0;

				int[] v = { v0, v1, v2 };

				for (int i = 0; i < 3; i++) {
					int j = i + 1;
					if (j == 3)
						j = 0;

					if (((state.vertices[v[i]].clip[2] < 0.0f) && (state.vertices[v[j]].clip[2] >= 0.0f))
							|| ((state.vertices[v[i]].clip[2] >= 0.0f) && (state.vertices[v[j]].clip[2] < 0.0f))) {
						float percent = (-state.vertices[v[i]].vtx[3] - state.vertices[v[i]].vtx[2])
								/ ((state.vertices[v[j]].vtx[2] - state.vertices[v[i]].vtx[2])
										+ (state.vertices[v[j]].vtx[3] - state.vertices[v[i]].vtx[3]));


						clippedVertices[clippedIndex].interpolateVertex(percent, state.vertices[v[i]], state.vertices[v[j]]);


						nearVertices[nearIndex].copyVertex(clippedVertices[clippedIndex]);
						nearVertices[nearIndex].vtx[2] = -nearVertices[nearIndex].vtx[3];

						clippedIndex++;
						nearIndex++;
					}

					if (((state.vertices[v[i]].clip[2] < 0.0f) && (state.vertices[v[j]].clip[2] >= 0.0f))
							|| ((state.vertices[v[i]].clip[2] >= 0.0f) && (state.vertices[v[j]].clip[2] >= 0.0f))) {

						clippedVertices[clippedIndex].copyVertex(state.vertices[v[j]]);
						clippedIndex++;
					} else {

						nearVertices[nearIndex].copyVertex(state.vertices[v[j]]);
						nearVertices[nearIndex].vtx[2] = -nearVertices[nearIndex].vtx[3];
						nearIndex++;
					}
				}


				OpenGlGdp.OGL_AddTriangle(clippedVertices[0].vtx, clippedVertices[0].color, clippedVertices[0].tex,
						clippedVertices[1].vtx, clippedVertices[1].color, clippedVertices[1].tex,
						clippedVertices[2].vtx, clippedVertices[2].color, clippedVertices[2].tex);

				if (clippedIndex == 4)

					OpenGlGdp.OGL_AddTriangle(clippedVertices[0].vtx, clippedVertices[0].color, clippedVertices[0].tex,
							clippedVertices[2].vtx, clippedVertices[2].color, clippedVertices[2].tex,
							clippedVertices[3].vtx, clippedVertices[3].color, clippedVertices[3].tex);


				OpenGl.gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);

				OpenGlGdp.OGL_AddTriangle(nearVertices[0].vtx, nearVertices[0].color, nearVertices[0].tex,
						nearVertices[1].vtx, nearVertices[1].color, nearVertices[1].tex, nearVertices[2].vtx,
						nearVertices[2].color, nearVertices[2].tex);
				if (nearIndex == 4)

					OpenGlGdp.OGL_AddTriangle(nearVertices[0].vtx, nearVertices[0].color, nearVertices[0].tex,
							nearVertices[2].vtx, nearVertices[2].color, nearVertices[2].tex, nearVertices[3].vtx,
							nearVertices[3].color, nearVertices[3].tex);

				if (Gdp.RDP_GETOM_Z_MODE(Rsp.gdp.otherMode) == Gbi.ZMODE_DEC)
					OpenGl.gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);

			} else {

				OpenGlGdp.OGL_AddTriangle(state.vertices[v0].vtx, state.vertices[v0].color, state.vertices[v0].tex, state.vertices[v1].vtx,
						state.vertices[v1].color, state.vertices[v1].tex, state.vertices[v2].vtx, state.vertices[v2].color, state.vertices[v2].tex);
			}
		}

		Rsp.gdp.update();
	}

	static void gSPFlushTriangles(Gsp state) {

		if ((state.nextCmd != Gbi.G_TRI1) && (state.nextCmd != Gbi.G_TRI2) && (state.nextCmd != Gbi.G_TRI4) && (state.nextCmd != Gbi.G_QUAD)
				&& (state.nextCmd != Gbi.G_DMA_TRI))
			OpenGlGdp.OGL_DrawTriangles();
	}

	static void gSPDMATriangles(Gsp state, int address, int n) {
		if (address + 16 * n > state.rdramSize)
			return;

		for (int i = 0; i < n; i++) {


			int v0 = state.rdram.get(address + 1) & 0xFF;
			state.vertices[v0].tex[0] = state.rdram.getShort(address + 4) * Gbi.FIXED2FLOATRECIP5;
			state.vertices[v0].tex[1] = state.rdram.getShort(address + 6) * Gbi.FIXED2FLOATRECIP5;

			int v1 = state.rdram.get(address + 2) & 0xFF;
			state.vertices[v1].tex[0] = state.rdram.getShort(address + 8) * Gbi.FIXED2FLOATRECIP5;
			state.vertices[v1].tex[1] = state.rdram.getShort(address + 10) * Gbi.FIXED2FLOATRECIP5;

			int v2 = state.rdram.get(address + 3) & 0xFF;
			state.vertices[v2].tex[0] = state.rdram.getShort(address + 12) * Gbi.FIXED2FLOATRECIP5;
			state.vertices[v2].tex[1] = state.rdram.getShort(address + 14) * Gbi.FIXED2FLOATRECIP5;

			state.gSPTriangle(v0, v1, v2, 0);
			address += 16;
		}

		state.gSPFlushTriangles();

		state.vertexi = 0;
	}

	static void gSP1Triangle(Gsp state, int v0, int v1, int v2, int flag) {
		state.gSPTriangle(v0, v1, v2, flag);
		state.gSPFlushTriangles();
	}

	static void gSP2Triangles(Gsp state, int v00, int v01, int v02, int flag0, int v10, int v11, int v12, int flag1) {
		state.gSPTriangle(v00, v01, v02, flag0);
		state.gSPTriangle(v10, v11, v12, flag1);
		state.gSPFlushTriangles();
	}

	static void gSP4Triangles(Gsp state, int v00, int v01, int v02, int v10, int v11, int v12, int v20, int v21, int v22, int v30,
			int v31, int v32) {
		state.gSPTriangle(v00, v01, v02, 0);
		state.gSPTriangle(v10, v11, v12, 0);
		state.gSPTriangle(v20, v21, v22, 0);
		state.gSPTriangle(v30, v31, v32, 0);
		state.gSPFlushTriangles();
	}

	static void gSP1Quadrangle(Gsp state, int v0, int v1, int v2, int v3) {
		state.gSPTriangle(v0, v1, v2, 0);
		state.gSPTriangle(v0, v2, v3, 0);
		state.gSPFlushTriangles();
	}

	static void gSPLine3D(Gsp state, int v0, int v1, int flag) {

		OpenGlGdp.OGL_DrawLine(state.vertices[v0].vtx, state.vertices[v0].color, state.vertices[v1].vtx, state.vertices[v1].color, 1.5f);
	}

	static void gSPLineW3D(Gsp state, int v0, int v1, int wd, int flag) {

		OpenGlGdp.OGL_DrawLine(state.vertices[v0].vtx, state.vertices[v0].color, state.vertices[v1].vtx, state.vertices[v1].color,
				1.5f + wd * 0.5f);
	}
}
