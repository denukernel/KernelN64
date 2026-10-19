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

package gln64j.rdp.textures;

import java.nio.ByteBuffer;
import static gln64j.rdp.textures.TextureCache.*;

/** RDRAM-to-TMEM tile, block, and palette transfers. */
final class TextureTransfers {
    private TextureTransfers() {}

	static void loadTile(TextureCache state, int tile) {
		InterleaveFunc Interleave;
		int line;

		state.loadTile = state.tiles[tile];
        state.stateLoadTileIndex = tile;

		if (state.loadTile.line == 0)
			return;

		int address = state.textureImage.address + state.loadTile.ult * state.textureImage.bpl + (state.loadTile.uls << state.textureImage.size >> 1);
		int dest = state.loadTile.tmem << 3;
		int bpl = (state.loadTile.lrs - state.loadTile.uls + 1) << state.loadTile.size >> 1;
		int height = state.loadTile.lrt - state.loadTile.ult + 1;
		int src = address;

		// Stay within TMEM
		if (((address + height * bpl) > state.rdramSize) || (((state.loadTile.tmem << 3) + bpl * height) > 4096))
			return;


		d3d11.framebuffer.D3DFramebuffers.textureRead(address, Math.max(1,state.loadTile.lrt-state.loadTile.ult+1)*state.textureImage.bpl);
		byte[] rdramArray = state.rdram.array();
		byte[] tmemArray = state.tmem.array();

		// Line given for 32-bit is half what it seems it should since they split the
		// high and low words. I'm cheating by putting them together.
		if (state.loadTile.size == G_IM_SIZ_32b) {
			line = state.loadTile.line << 1;
			Interleave = state.QWordInterleave;
		} else {
			line = state.loadTile.line;
			Interleave = state.DWordInterleave;
		}

		int bplTex = state.textureImage.bpl;
		int lineBytes = line << 3;
		for (int y = 0; y < height; y++) {
			System.arraycopy(rdramArray, src, tmemArray, dest, bpl);
			if ((y & 1) != 0) {
				Interleave.Interleave(tmemArray, dest, line);
			}

			src += bplTex;
			dest += lineBytes;
		}

		state.textureMode = TEXTUREMODE_NORMAL;
		state.loadType = LOADTYPE_TILE;
		state.changed |= CHANGED_TMEM;
	}

	static void loadBlock(TextureCache state, int tile, int uls, int ult, int lrs, int dxt) {
		state.loadTile = state.tiles[tile];
        state.stateLoadTileIndex = tile;

		int bytes = (lrs + 1) << state.loadTile.size >>> 1;
		int address = state.textureImage.address + ult * state.textureImage.bpl + (uls << state.textureImage.size >>> 1);

		if ((bytes == 0) || ((address + bytes) > state.rdramSize) || (((state.loadTile.tmem << 3) + bytes) > 4096)) {
			return;
		}


		int src = address;
		int dest = state.loadTile.tmem << 3;

		d3d11.framebuffer.D3DFramebuffers.textureRead(address, bytes);
		byte[] rdramArray = state.rdram.array();
		byte[] tmemArray = state.tmem.array();

		if (dxt > 0) {
			InterleaveFunc Interleave;

			int line = (2047 + dxt) / dxt;
			int bpl = line << 3;
			int height = bytes / bpl;

			if (state.loadTile.size == G_IM_SIZ_32b)
				Interleave = state.QWordInterleave;
			else
				Interleave = state.DWordInterleave;

			for (int y = 0; y < height; y++) {
				System.arraycopy(rdramArray, src, tmemArray, dest, bpl);
				if ((y & 1) != 0) {
					Interleave.Interleave(tmemArray, dest, line);
				}

				src += bpl;
				dest += bpl;
			}
		} else {
			System.arraycopy(rdramArray, src, tmemArray, dest, bytes);
		}

		state.textureMode = TEXTUREMODE_NORMAL;
		state.loadType = LOADTYPE_BLOCK;
		state.changed |= CHANGED_TMEM;
	}

	static void loadLUT(TextureCache state, int tile) {
		int count = ((state.tiles[tile].lrs - state.tiles[tile].uls + 1) * (state.tiles[tile].lrt - state.tiles[tile].ult + 1)) & 0xFFFF;
		int address = state.textureImage.address + state.tiles[tile].ult * state.textureImage.bpl
				+ (state.tiles[tile].uls << state.textureImage.size >>> 1);
		d3d11.framebuffer.D3DFramebuffers.textureRead(address,count*2);
		int dest = state.tiles[tile].tmem * 8;
		int pal = ((state.tiles[tile].tmem - 256) >>> 4) & 0xFFFF;

		state.tmem.position(0);
		ByteBuffer tmemPtr = state.tmem.slice();
		state.paletteCRC16.position(0);

		int rdramLimit = state.rdram.capacity();
		int tmemLimit = state.tmem.limit();

		int i = 0;
		while (i < count) {
			for (int j = 0; (j < 16) && (i < count); j++, i++) {

				int readAddr = address + (i * 2);

				if (readAddr < 0 || readAddr + 1 >= rdramLimit) {
					continue;
				}

				short color = state.rdram.getShort(readAddr);

				if (dest < 0 || dest + 1 >= tmemLimit) {
					dest = dest % tmemLimit;
					if (dest < 0)
						dest += tmemLimit;
				}

				state.tmem.putShort(dest, color);
				dest += 8;
			}

			int pos = (256 + (pal << 4)) * 8;

			if (pos < 0 || pos >= tmemPtr.limit()) {
				pos = 0;
			}

			tmemPtr.position(pos);
			int safePal = pal & 0xF;

			state.paletteCRC16.asIntBuffer().put(safePal, state.crcCalculatePalette(0xFFFFFFFF, tmemPtr.slice(), 16));
			pal++;
		}

		state.crc32.reset();
		state.crc32.update(state.paletteCRC16.array(), 0, 64);
		state.paletteCRC256.asIntBuffer().put(0, (int) state.crc32.getValue());
		state.changed |= CHANGED_TMEM;
	}

	static int crcCalculatePalette(TextureCache state, int crc, ByteBuffer buffer, int count) {
		int p = 0;
		int orig = crc;

		while ((count--) != 0) {
			crc = (crc >>> 8) ^ state.crcTable[(crc & 0xFF) ^ (buffer.get(p++) & 0xFF)];
			crc = (crc >>> 8) ^ state.crcTable[(crc & 0xFF) ^ (buffer.get(p++) & 0xFF)];
			p += 6;
		}
		return crc ^ orig;
	}

	static void crcBuildTable(TextureCache state) {
		int crc;

		for (int i = 0; i <= 255; i++) {
			crc = state.reflect(i, 8) << 24;
			for (int j = 0; j < 8; j++)
				crc = (crc << 1) ^ (((crc & (1 << 31)) != 0) ? CRC32_POLYNOMIAL : 0);

			state.crcTable[i] = state.reflect(crc, 32);
		}
	}

	static int reflect(TextureCache state, int ref, int ch) {
		int value = 0;
		// Swap bit 0 for bit 7
		// bit 1 for bit 6, etc.
		for (int i = 1; i < (ch + 1); i++) {
			if ((ref & 1) != 0)
				value |= (1 << (ch - i));
			ref = ref >> 1;
		}
		return value;
	}
}
