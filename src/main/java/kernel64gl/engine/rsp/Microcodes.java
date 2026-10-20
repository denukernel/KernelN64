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

import kernel64gl.engine.debug.Debug;
import kernel64gl.engine.HlePlugin;
import kernel64gl.engine.core.Gbi;
import kernel64gl.engine.core.Rsp;
import kernel64gl.engine.opcodes.F3d;
import kernel64gl.engine.opcodes.F3ddkr;
import kernel64gl.engine.opcodes.F3dex;
import kernel64gl.engine.opcodes.F3dex2;
import kernel64gl.engine.opcodes.F3dexbg;
import kernel64gl.engine.opcodes.F3dpd;
import kernel64gl.engine.opcodes.F3dwrus;
import kernel64gl.engine.opcodes.L3d;
import kernel64gl.engine.opcodes.L3dex;
import kernel64gl.engine.opcodes.L3dex2;
import kernel64gl.engine.opcodes.Rdpfuncs2;
import kernel64gl.engine.opcodes.S2dex;
import kernel64gl.engine.opcodes.S2dex2;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import javax.swing.JOptionPane;


public class Microcodes {

	// GBI

	// used by Rsp, Gsp
	public GBIFunc[] cmds = new GBIFunc[256];
//    // used by Gsp, Opcodes
//    public int pcStackSize;
	// used by Gsp
	public Microcode current;

	private int numMicrocodes;
	private Microcode top;
	private Microcode bottom;
	private Checksum crc32 = new Adler32();
	private int uc_crc;
	private int uc_dcrc;
	private String uc_str;

	private Microcode[] specialMicrocodes = {
			new Microcode(Microcode.F3DWRUS, false, 0xd17906e2, "RSP SW Version: 2.0D, 04-01-96"),
			new Microcode(Microcode.F3DWRUS, false, 0x94c4c833, "RSP SW Version: 2.0D, 04-01-96"),
			new Microcode(Microcode.S2DEX, false, 0x9df31081, "RSP Gfx ucode S2DEX  1.06 Yoshitaka Yasumoto Nintendo."),
			new Microcode(Microcode.F3DDKR, false, 0x8d91244f, "Diddy Kong Racing"),
			new Microcode(Microcode.F3DDKR, false, 0x6e6fc893, "Diddy Kong Racing"),
			new Microcode(Microcode.F3DDKR, false, 0xbde9d1fb, "Jet Force Gemini"),
			new Microcode(Microcode.F3DPD, false, 0x1c4f7869, "Perfect Dark") };

	private static GBIFunc gbiUnknown = new GBIFunc() {
		public void exec(int w0, int w1) {
			if (Debug.DEBUG_GBI)
				Debug.DebugMsg(Debug.DEBUG_LOW | Debug.DEBUG_UNKNOWN, "UNKNOWN GBI COMMAND 0x%02X\n",
						(w0 >> 24) & Gbi.SR_MASK_8);
		}
	};

	/**
	 * Creates a new instance of Microcodes
	 */
	public Microcodes() {
	}

	// called by HlePlugin
	public void init() {
		top = null;
		bottom = null;
		current = null;
		numMicrocodes = 0;

		for (int i = 0; i <= 0xFF; i++)
			cmds[i] = gbiUnknown;
	}

	// called by Gsp
	public void makeCurrent(Microcode microcode) {
		if (microcode != top) {
			if (microcode == bottom) {
				bottom = microcode.higher;
				bottom.lower = null;
			} else {
				microcode.higher.lower = microcode.lower;
				microcode.lower.higher = microcode.higher;
			}

			microcode.higher = null;
			microcode.lower = top;
			top.higher = microcode;
			top = microcode;
		}

		if (current == null || (current.type != microcode.type)) {
			for (int i = 0; i <= 0xFF; i++)
				cmds[i] = gbiUnknown;


			Rdpfuncs2.RDP_Init(Rsp.gsp, Rsp.gdp);

			switch (microcode.type) {
			case Microcode.F3D:
				F3d.F3D_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.F3DEX:
				F3dex.F3DEX_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.F3DEX2:
				F3dex2.F3DEX2_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.L3D:
				L3d.L3D_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.L3DEX:
				L3dex.L3DEX_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.L3DEX2:
				L3dex2.L3DEX2_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.S2DEX:
				S2dex.S2DEX_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.S2DEX2:
				S2dex2.S2DEX2_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.F3DDKR:
				F3ddkr.F3DDKR_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.F3DWRUS:
				F3dwrus.F3DWRUS_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.F3DPD:
				F3dpd.F3DPD_Init(Rsp.gsp, Rsp.gdp);
				break;
			case Microcode.F3DEXBG:
				F3dexbg.F3DEXBG_Init(Rsp.gsp, Rsp.gdp);
				break;
			}
		}

		current = microcode;
	}

	// called by Gsp
	public Microcode detectMicrocode(int uc_start, int uc_dstart, short uc_dsize) {

		Microcode current;

		for (int i = 0; i < numMicrocodes; i++) {
			current = top;
			while (current != null) {
				if ((current.address == uc_start) && (current.dataAddress == uc_dstart)
						&& (current.dataSize == uc_dsize))
					return current;

				current = current.lower;
			}
		}

		current = addMicrocode();
		current.address = uc_start;
		current.dataAddress = uc_dstart;
		current.dataSize = uc_dsize;
		current.NoN = false;
		current.type = Microcode.NONE;

		byte[] rdram = HlePlugin.RDRAM.array();
		int rdramSize = rdram.length;

		/* --------------------- SAFE CRC BLOCK --------------------- */

		int realStart = uc_start & 0x1FFFFFFF;

		// Hard clamp
		if (realStart < 0)
			realStart = 0;
		if (realStart >= rdramSize)
			return current;

		int safeLen = Math.min(4096, rdramSize - realStart);
		if (safeLen <= 0)
			return current;

		crc32.reset();
		crc32.update(rdram, realStart, safeLen);
		uc_crc = (int) crc32.getValue();

		for (int i = 0; i < specialMicrocodes.length; i++) {
			if (uc_crc == specialMicrocodes[i].crc) {
				current.type = specialMicrocodes[i].type;
				if (Debug.DEBUG_MICROCODE)
					System.out.printf("Found microcode by crc. Using %s\n", Microcode.MicrocodeTypes[current.type]);
				return current;
			}
		}

		/* --------------------- SAFE TEXT DETECTION BLOCK --------------------- */

		int realDStart = uc_dstart & 0x1FFFFFFF;
		if (realDStart < 0)
			realDStart = 0;
		if (realDStart >= rdramSize)
			return current;

		int maxCopy = Math.min(2048, rdramSize - realDStart);
		if (maxCopy < 32)
			return current; // cannot detect ucode text

		byte[] uc_data = new byte[2048];
		System.arraycopy(rdram, realDStart, uc_data, 0, maxCopy);

		uc_str = "Not Found";

		for (int i = 0; i < maxCopy - 32; i++) {
			if ((uc_data[i] == 'R') && (uc_data[i + 1] == 'S') && (uc_data[i + 2] == 'P')) {

				int j = 0;
				uc_str = "";
				while (i + j < maxCopy && uc_data[i + j] > 0x0A) {
					uc_str += (char) uc_data[i + j];
					j++;
				}

				if (Debug.DEBUG_MICROCODE)
					System.out.println(uc_str);

				int type = Microcode.NONE;

				if ((i + 16) < maxCopy) {

					if ((uc_data[i + 4] == 'S') && (uc_data[i + 5] == 'W')) {
						type = Microcode.F3D;

					} else if ((uc_data[i + 4] == 'G') && (uc_data[i + 5] == 'f') && (uc_data[i + 6] == 'x')) {
						current.NoN = uc_str.contains(".NoN");

						if ((uc_data[i + 14] == 'F') && (uc_data[i + 15] == '3') && (uc_data[i + 16] == 'D')) {

							if ((i + 32) < maxCopy) {
								if ((uc_data[i + 19] == 'B') && (uc_data[i + 20] == 'G'))
									type = Microcode.F3DEXBG;
								else if ((uc_data[i + 28] == '0') || (uc_data[i + 28] == '1'))
									type = Microcode.F3DEX;
								else if (uc_data[i + 31] == '2')
									type = Microcode.F3DEX2;
							}

						} else if ((uc_data[i + 14] == 'L') && (uc_data[i + 15] == '3') && (uc_data[i + 16] == 'D')) {
							type = (uc_data[i + 28] == '1') ? Microcode.L3DEX
									: (uc_data[i + 31] == '2') ? Microcode.L3DEX2 : Microcode.NONE;

						} else if ((uc_data[i + 14] == 'S') && (uc_data[i + 15] == '2') && (uc_data[i + 16] == 'D')) {
							type = (uc_data[i + 28] == '1') ? Microcode.S2DEX
									: (uc_data[i + 31] == '2') ? Microcode.S2DEX2 : Microcode.NONE;
						}
					}
				}

				if (type != Microcode.NONE) {
					current.type = type;
					if (Debug.DEBUG_MICROCODE)
						System.out.printf("Found microcode by standard text. Using %s\n",
								Microcode.MicrocodeTypes[current.type]);
					return current;
				}

				break;
			}
		}

		for (int i = 0; i < specialMicrocodes.length; i++) {
			if (uc_str.equals(specialMicrocodes[i].text)) {
				current.type = specialMicrocodes[i].type;
				if (Debug.DEBUG_MICROCODE)
					System.out.printf("Found microcode by special text. Using %s\n",
							Microcode.MicrocodeTypes[current.type]);
				return current;
			}
		}

		current.type = Microcode.F3D; // fallback
		return current;
	}

	// Private Methods /////////////////////////////////////////////////////////

	private Microcode addMicrocode() {
		Microcode newtop = new Microcode();

		newtop.lower = top;
		newtop.higher = null;

		if (top != null)
			top.higher = newtop;

		if (bottom == null)
			bottom = newtop;

		top = newtop;

		numMicrocodes++;

		return newtop;
	}

}
