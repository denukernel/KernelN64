/**
 * Copyright 2006 Jason LaDere
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
 * Originally based on Azimer's Audio Plugin.
 *
 */

package aziaudio.hle.abi2;
import aziaudio.hle.HLEMain;
import aziaudio.hle.abi1.ABI1;

import java.nio.ByteBuffer;
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi2.ABI2.*;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI2 ADPCM codebook loading and sample decoding. */
final class ABI2Adpcm {
    private ABI2Adpcm() {}

    static void LOADADPCM2() {
        int v0 = inst2 & 0xFFFFFF;
        int cnt = (inst1 & 0xFFFF) >> 4;
        for (int x = 0; x < cnt; x++) {
        	adpcmtable[0 + (x << 3)] = rdram.getShort(v0 + 0);
        	adpcmtable[1 + (x << 3)] = rdram.getShort(v0 + 2);
        	adpcmtable[2 + (x << 3)] = rdram.getShort(v0 + 4);
        	adpcmtable[3 + (x << 3)] = rdram.getShort(v0 + 6);
        	adpcmtable[4 + (x << 3)] = rdram.getShort(v0 + 8);
        	adpcmtable[5 + (x << 3)] = rdram.getShort(v0 + 10);
        	adpcmtable[6 + (x << 3)] = rdram.getShort(v0 + 12);
        	adpcmtable[7 + (x << 3)] = rdram.getShort(v0 + 14);
        	v0 += 16;
        }
    }

    static void ADPCM2() {
        int flags = (inst1 >>> 16) & 0xFF;
        int Address = inst2 & 0xFFFFFF;
        int inPtr = 0;
        ByteBuffer out = BufferSpace;
        int out_p = AudioOutBuffer;
        short count = (short) AudioCount;
        int icode;
        int code;
        int vscale;
        int index;
        int j;
        int[] a = new int[8];
        int book1_p, book2_p;
        int srange;
        int mask1;
        int mask2;
        int shifter;

        Arrays.fill(out.array(), out_p, out_p + 32, (byte) 0);

        // Tricky lil Zelda MM and ABI2!!! hahaha I know your secrets! :DDD
        if ((flags & 0x4) != 0) {
        	srange = 0xE;
        	mask1 = 0xC0;
        	mask2 = 0x30;
        	shifter = 10;
        } else {
        	srange = 0xC;
        	mask1 = 0xF0;
        	mask2 = 0x0F;
        	shifter = 12;
        }

        if ((flags & 0x1) == 0) {
        	if ((flags & 0x2) != 0) {
        		System.arraycopy(rdram.array(), loopval, out.array(), out_p, 32);
        	} else {
        		System.arraycopy(rdram.array(), Address, out.array(), out_p, 32);
        	}
        }

        int l1 = out.getShort(out_p + 28);
        int l2 = out.getShort(out_p + 30);
        int[] inp1 = new int[8];
        int[] inp2 = new int[8];
        out_p += 32;

        while (count > 0) {
        	code = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
        	index = code & 0xF;
        	index <<= 4;
        	book1_p = index;
        	book2_p = book1_p + 8;
        	code >>= 4;
        	vscale = (0x8000 >> ((srange - code) - 1));

        	inPtr++;
        	j = 0;

        	while (j < 8) {
        		icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
        		inPtr++;

        		inp1[j] = (short) ((icode & mask1) << 8); // this will in effect be signed
        		if (code < srange) {
        			inp1[j] = (inp1[j] * vscale) >> 16;
        		}
        		j++;

        		inp1[j] = (short) ((icode & mask2) << shifter);
        		if (code < srange) {
        			inp1[j] = (inp1[j] * vscale) >> 16;
        		}
        		j++;

        		if ((flags & 4) != 0) {
        			inp1[j] = (short) ((icode & 0xC) << 12); // this will in effect be signed
        			if (code < 0xE) {
        				inp1[j] = (inp1[j] * vscale) >> 16;
        			}
        			j++;

        			inp1[j] = (short) ((icode & 0x3) << 14);
        			if (code < 0xE) {
        				inp1[j] = (inp1[j] * vscale) >> 16;
        			}
        			j++;
        		}
        	}

        	j = 0;
        	while (j < 8) {
        		icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
        		inPtr++;

        		inp2[j] = (short) ((icode & mask1) << 8);
        		if (code < srange) {
        			inp2[j] = (inp2[j] * vscale) >> 16;
        		}
        		j++;

        		inp2[j] = (short) ((icode & mask2) << shifter);
        		if (code < srange) {
        			inp2[j] = (inp2[j] * vscale) >> 16;
        		}
        		j++;

        		if ((flags & 4) != 0) {
        			inp2[j] = (short) ((icode & 0xC) << 12);
        			if (code < 0xE) {
        				inp2[j] = (inp2[j] * vscale) >> 16;
        			}
        			j++;

        			inp2[j] = (short) ((icode & 0x3) << 14);
        			if (code < 0xE) {
        				inp2[j] = (inp2[j] * vscale) >> 16;
        			}
        			j++;
        		}
        	}

        	a[0] = (int) adpcmtable[book1_p + 0] * l1;
        	a[0] += (int) adpcmtable[book2_p + 0] * l2;
        	a[0] += inp1[0] * 2048;

        	a[1] = (int) adpcmtable[book1_p + 1] * l1;
        	a[1] += (int) adpcmtable[book2_p + 1] * l2;
        	a[1] += (int) adpcmtable[book2_p + 0] * inp1[0];
        	a[1] += inp1[1] * 2048;

        	a[2] = (int) adpcmtable[book1_p + 2] * l1;
        	a[2] += (int) adpcmtable[book2_p + 2] * l2;
        	a[2] += (int) adpcmtable[book2_p + 1] * inp1[0];
        	a[2] += (int) adpcmtable[book2_p + 0] * inp1[1];
        	a[2] += inp1[2] * 2048;

        	a[3] = (int) adpcmtable[book1_p + 3] * l1;
        	a[3] += (int) adpcmtable[book2_p + 3] * l2;
        	a[3] += (int) adpcmtable[book2_p + 2] * inp1[0];
        	a[3] += (int) adpcmtable[book2_p + 1] * inp1[1];
        	a[3] += (int) adpcmtable[book2_p + 0] * inp1[2];
        	a[3] += inp1[3] * 2048;

        	a[4] = (int) adpcmtable[book1_p + 4] * l1;
        	a[4] += (int) adpcmtable[book2_p + 4] * l2;
        	a[4] += (int) adpcmtable[book2_p + 3] * inp1[0];
        	a[4] += (int) adpcmtable[book2_p + 2] * inp1[1];
        	a[4] += (int) adpcmtable[book2_p + 1] * inp1[2];
        	a[4] += (int) adpcmtable[book2_p + 0] * inp1[3];
        	a[4] += inp1[4] * 2048;

        	a[5] = (int) adpcmtable[book1_p + 5] * l1;
        	a[5] += (int) adpcmtable[book2_p + 5] * l2;
        	a[5] += (int) adpcmtable[book2_p + 4] * inp1[0];
        	a[5] += (int) adpcmtable[book2_p + 3] * inp1[1];
        	a[5] += (int) adpcmtable[book2_p + 2] * inp1[2];
        	a[5] += (int) adpcmtable[book2_p + 1] * inp1[3];
        	a[5] += (int) adpcmtable[book2_p + 0] * inp1[4];
        	a[5] += inp1[5] * 2048;

        	a[6] = (int) adpcmtable[book1_p + 6] * l1;
        	a[6] += (int) adpcmtable[book2_p + 6] * l2;
        	a[6] += (int) adpcmtable[book2_p + 5] * inp1[0];
        	a[6] += (int) adpcmtable[book2_p + 4] * inp1[1];
        	a[6] += (int) adpcmtable[book2_p + 3] * inp1[2];
        	a[6] += (int) adpcmtable[book2_p + 2] * inp1[3];
        	a[6] += (int) adpcmtable[book2_p + 1] * inp1[4];
        	a[6] += (int) adpcmtable[book2_p + 0] * inp1[5];
        	a[6] += inp1[6] * 2048;

        	a[7] = (int) adpcmtable[book1_p + 7] * l1;
        	a[7] += (int) adpcmtable[book2_p + 7] * l2;
        	a[7] += (int) adpcmtable[book2_p + 6] * inp1[0];
        	a[7] += (int) adpcmtable[book2_p + 5] * inp1[1];
        	a[7] += (int) adpcmtable[book2_p + 4] * inp1[2];
        	a[7] += (int) adpcmtable[book2_p + 3] * inp1[3];
        	a[7] += (int) adpcmtable[book2_p + 2] * inp1[4];
        	a[7] += (int) adpcmtable[book2_p + 1] * inp1[5];
        	a[7] += (int) adpcmtable[book2_p + 0] * inp1[6];
        	a[7] += inp1[7] * 2048;

        	for (j = 0; j < 8; j++) {
        		a[j] >>= 11;
        		if (a[j] > 32767) {
        			a[j] = 32767;
        		} else if (a[j] < -32768) {
        			a[j] = -32768;
        		}
        		out.putShort(out_p, (short) a[j]);
        		out_p += 2;
        	}
        	l1 = a[6];
        	l2 = a[7];

        	a[0] = (int) adpcmtable[book1_p + 0] * l1;
        	a[0] += (int) adpcmtable[book2_p + 0] * l2;
        	a[0] += inp2[0] * 2048;

        	a[1] = (int) adpcmtable[book1_p + 1] * l1;
        	a[1] += (int) adpcmtable[book2_p + 1] * l2;
        	a[1] += (int) adpcmtable[book2_p + 0] * inp2[0];
        	a[1] += inp2[1] * 2048;

        	a[2] = (int) adpcmtable[book1_p + 2] * l1;
        	a[2] += (int) adpcmtable[book2_p + 2] * l2;
        	a[2] += (int) adpcmtable[book2_p + 1] * inp2[0];
        	a[2] += (int) adpcmtable[book2_p + 0] * inp2[1];
        	a[2] += inp2[2] * 2048;

        	a[3] = (int) adpcmtable[book1_p + 3] * l1;
        	a[3] += (int) adpcmtable[book2_p + 3] * l2;
        	a[3] += (int) adpcmtable[book2_p + 2] * inp2[0];
        	a[3] += (int) adpcmtable[book2_p + 1] * inp2[1];
        	a[3] += (int) adpcmtable[book2_p + 0] * inp2[2];
        	a[3] += inp2[3] * 2048;

        	a[4] = (int) adpcmtable[book1_p + 4] * l1;
        	a[4] += (int) adpcmtable[book2_p + 4] * l2;
        	a[4] += (int) adpcmtable[book2_p + 3] * inp2[0];
        	a[4] += (int) adpcmtable[book2_p + 2] * inp2[1];
        	a[4] += (int) adpcmtable[book2_p + 1] * inp2[2];
        	a[4] += (int) adpcmtable[book2_p + 0] * inp2[3];
        	a[4] += inp2[4] * 2048;

        	a[5] = (int) adpcmtable[book1_p + 5] * l1;
        	a[5] += (int) adpcmtable[book2_p + 5] * l2;
        	a[5] += (int) adpcmtable[book2_p + 4] * inp2[0];
        	a[5] += (int) adpcmtable[book2_p + 3] * inp2[1];
        	a[5] += (int) adpcmtable[book2_p + 2] * inp2[2];
        	a[5] += (int) adpcmtable[book2_p + 1] * inp2[3];
        	a[5] += (int) adpcmtable[book2_p + 0] * inp2[4];
        	a[5] += inp2[5] * 2048;

        	a[6] = (int) adpcmtable[book1_p + 6] * l1;
        	a[6] += (int) adpcmtable[book2_p + 6] * l2;
        	a[6] += (int) adpcmtable[book2_p + 5] * inp2[0];
        	a[6] += (int) adpcmtable[book2_p + 4] * inp2[1];
        	a[6] += (int) adpcmtable[book2_p + 3] * inp2[2];
        	a[6] += (int) adpcmtable[book2_p + 2] * inp2[3];
        	a[6] += (int) adpcmtable[book2_p + 1] * inp2[4];
        	a[6] += (int) adpcmtable[book2_p + 0] * inp2[5];
        	a[6] += inp2[6] * 2048;

        	a[7] = (int) adpcmtable[book1_p + 7] * l1;
        	a[7] += (int) adpcmtable[book2_p + 7] * l2;
        	a[7] += (int) adpcmtable[book2_p + 6] * inp2[0];
        	a[7] += (int) adpcmtable[book2_p + 5] * inp2[1];
        	a[7] += (int) adpcmtable[book2_p + 4] * inp2[2];
        	a[7] += (int) adpcmtable[book2_p + 3] * inp2[3];
        	a[7] += (int) adpcmtable[book2_p + 2] * inp2[4];
        	a[7] += (int) adpcmtable[book2_p + 1] * inp2[5];
        	a[7] += (int) adpcmtable[book2_p + 0] * inp2[6];
        	a[7] += inp2[7] * 2048;

        	for (j = 0; j < 8; j++) {
        		a[j] >>= 11;
        		if (a[j] > 32767) {
        			a[j] = 32767;
        		} else if (a[j] < -32768) {
        			a[j] = -32768;
        		}
        		out.putShort(out_p, (short) a[j]);
        		out_p += 2;
        	}
        	l1 = a[6];
        	l2 = a[7];

        	count -= 32;
        }
        out_p -= 32;
        System.arraycopy(out.array(), out_p, rdram.array(), Address, 32);
    }
}
