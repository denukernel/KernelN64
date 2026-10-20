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
import kernel64gl.engine.opengl.OpenGlGdp;
import static kernel64gl.engine.rsp.Gsp.*;

/** Matrix loading, stack commands, and viewport conversion. */
final class GspMatrixCommands {
    private GspMatrixCommands() {}

	static void gSPMatrix(Gsp state, int address, int param) {
		if (address + 64 > state.rdramSize)
			return;
		state.loadMatrix(state.tmpmtx, address);
		if ((param & Gbi.G_MTX_PROJECTION) != 0) {
			if ((param & Gbi.G_MTX_LOAD) != 0)
				Math3D.copyMatrix(state.matrix.projection, state.tmpmtx);
			else
				Math3D.multMatrix(state.matrix.projection, state.tmpmtx);
		} else {
			if ((param & Gbi.G_MTX_PUSH) != 0 && (state.matrix.modelViewi < (state.matrix.stackSize - 1))) {
				Math3D.copyMatrix(state.matrix.modelView[state.matrix.modelViewi + 1], state.matrix.modelView[state.matrix.modelViewi]);
				state.matrix.modelViewi++;
			}
			if ((param & Gbi.G_MTX_LOAD) != 0)
				Math3D.copyMatrix(state.matrix.modelView[state.matrix.modelViewi], state.tmpmtx);
			else
				Math3D.multMatrix(state.matrix.modelView[state.matrix.modelViewi], state.tmpmtx);
		}
		state.changed |= CHANGED_MATRIX;
	}

	static void gSPForceMatrix(Gsp state, int address) {
		if (address + 64 > state.rdramSize)
			return;
		state.loadMatrix(state.matrix.combined, address);
		state.changed &= ~CHANGED_MATRIX;
	}

	static void gSPPopMatrix(Gsp state, int param) {
		if (state.matrix.modelViewi > 0) {
			state.matrix.modelViewi--;
			state.changed |= CHANGED_MATRIX;
		}
	}

	static void gSPPopMatrixN(Gsp state, int param, int num) {
		if (state.matrix.modelViewi > num - 1) {
			state.matrix.modelViewi -= num;
			state.changed |= CHANGED_MATRIX;
		}
	}

	static void gSPInsertMatrix(Gsp state, int where, int num) {
		float fraction, integer;

		if ((state.changed & CHANGED_MATRIX) != 0)
			state.gSPCombineMatrices();

		if ((where & 0x3) != 0 || (where > 0x3C))
			return;

		if (where < 0x20) {
			int w = where >> 1;
			int r = w / 4;
			int c = w % 4;
			int w2 = w + 1;
			int r2 = w2 / 4;
			int c2 = w2 % 4;
			integer = (int) state.matrix.combined[r][c];
			fraction = state.matrix.combined[r][c] - integer;
			state.matrix.combined[r][c] = (short) ((num >> 16) & Gbi.SR_MASK_16) + StrictMath.abs(fraction);

			integer = (int) state.matrix.combined[r2][c2];
			fraction = state.matrix.combined[r2][c2] - integer;
			state.matrix.combined[r2][c2] = (short) (num & Gbi.SR_MASK_16) + StrictMath.abs(fraction);
		} else {
			int w = (where - 0x20) >> 1;
			int r = w / 4;
			int c = w % 4;
			int w2 = w + 1;
			int r2 = w2 / 4;
			int c2 = w2 % 4;
			float newValue;

			integer = (int) state.matrix.combined[r][c];
			fraction = state.matrix.combined[r][c] - integer;
			newValue = integer + (((num >> 16) & Gbi.SR_MASK_16) * Gbi.FIXED2FLOATRECIP16);

			// Make sure the sign isn't lost
			if ((integer == 0.0f) && (fraction != 0.0f))
				newValue = newValue * (fraction / StrictMath.abs(fraction));

			state.matrix.combined[r][c] = newValue;

			integer = (int) state.matrix.combined[r2][c2];
			fraction = state.matrix.combined[r2][c2] - integer;
			newValue = integer + ((num & Gbi.SR_MASK_16) * Gbi.FIXED2FLOATRECIP16);

			// Make sure the sign isn't lost
			if ((integer == 0.0f) && (fraction != 0.0f))
				newValue = newValue * (fraction / StrictMath.abs(fraction));

			state.matrix.combined[r2][c2] = newValue;
		}
	}

	static void gSPDMAMatrix(Gsp state, int address, int index, boolean multiply) {
		address = state.DMAOffsets.mtx + address;
		if (address + 64 > state.rdramSize)
			return;

		state.loadMatrix(state.tmpmtx, address);
		state.matrix.modelViewi = index;

		if (multiply) {
			Math3D.copyMatrix(state.matrix.modelView[state.matrix.modelViewi], state.matrix.modelView[0]);
			Math3D.multMatrix(state.matrix.modelView[state.matrix.modelViewi], state.tmpmtx);
		} else
			Math3D.copyMatrix(state.matrix.modelView[state.matrix.modelViewi], state.tmpmtx);

		Math3D.copyMatrix(state.matrix.projection, state.identityMatrix);
		state.changed |= CHANGED_MATRIX;
	}

	static void loadMatrix(Gsp state, float[][] mtx, int address) {
		final float recip = 1.5258789e-05f;
		for (int i = 0; i < 4; i++) {
			mtx[i][0] = state.rdram.getShort(address) + (state.rdram.getShort(address + 32) & 0xFFFF) * recip;
			mtx[i][1] = state.rdram.getShort(address + 2) + (state.rdram.getShort(address + 34) & 0xFFFF) * recip;
			mtx[i][2] = state.rdram.getShort(address + 4) + (state.rdram.getShort(address + 36) & 0xFFFF) * recip;
			mtx[i][3] = state.rdram.getShort(address + 6) + (state.rdram.getShort(address + 38) & 0xFFFF) * recip;
			address += 8;
		}
	}

	static void gSPCombineMatrices(Gsp state) {
		Math3D.copyMatrix(state.matrix.combined, state.matrix.projection);
		Math3D.multMatrix(state.matrix.combined, state.matrix.modelView[state.matrix.modelViewi]);
		state.changed &= ~CHANGED_MATRIX;
	}

	static void gSPViewport(Gsp state, int address) {
		if (address < 0 || address > state.rdramSize - 16) {
			return;
		}
        // RDRAM is in normal big-endian N64 order: x, y, z, w.
        // The old halfword swaps exchanged z with w and clipped HUD rectangles.
		state.viewport.vscale[0] = state.rdram.getShort(address) * Gbi.FIXED2FLOATRECIP2;
		state.viewport.vscale[1] = state.rdram.getShort(address + 2) * Gbi.FIXED2FLOATRECIP2;
		state.viewport.vscale[2] = state.rdram.getShort(address + 4) * Gbi.FIXED2FLOATRECIP10;
		state.viewport.vscale[3] = state.rdram.getShort(address + 6);
		state.viewport.vtrans[0] = state.rdram.getShort(address + 8) * Gbi.FIXED2FLOATRECIP2;
		state.viewport.vtrans[1] = state.rdram.getShort(address + 10) * Gbi.FIXED2FLOATRECIP2;
		state.viewport.vtrans[2] = state.rdram.getShort(address + 12) * Gbi.FIXED2FLOATRECIP10;
		state.viewport.vtrans[3] = state.rdram.getShort(address + 14);

		OpenGlGdp.setView(state.viewport.vtrans[2], state.viewport.vscale[2]);

	}
}
