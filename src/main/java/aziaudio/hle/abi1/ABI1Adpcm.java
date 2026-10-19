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

package aziaudio.hle.abi1;
import aziaudio.hle.HLEMain;

import java.nio.ByteBuffer;
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI1 ADPCM codebook loading and sample decoding. */
final class ABI1Adpcm {
    private ABI1Adpcm() {}

    static void ADPCM() {
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

        // Use static scratch buffers (optimization)
        int[] a = adpcm_a;
        int[] inp1 = adpcm_inp1;
        int[] inp2 = adpcm_inp2;

        int book1_p, book2_p;

        Arrays.fill(out.array(), out_p, out_p + 32, (byte) 0);

        if ((flags & 0x1) == 0) {
            if ((flags & 0x2) != 0) {
                System.arraycopy(rdram.array(), loopval & 0xffffff, out.array(), out_p, 32);
            } else {
                System.arraycopy(rdram.array(), Address, out.array(), out_p, 32);
            }
        }

        int l1 = out.getShort(out_p + 28);
        int l2 = out.getShort(out_p + 30);
        // Removed local array allocation

        out_p += 32;
        while (count > 0) {
            code = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
            index = code & 0xF;
            index <<= 4; // index into the adpcm code table
            book1_p = index;
            book2_p = book1_p + 8;
            code >>= 4; // upper nibble is scale
            vscale = (0x8000 >> ((12 - code) - 1));	// very strange. 0x8000 would be .5 in 16:16 format

            inPtr++; // coded adpcm data lies next
            j = 0;
            while (j < 8) { // loop of 8, for 8 coded nibbles from 4 bytes which yields 8 short pcm values
                icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
                inPtr++;

                inp1[j] = (short) ((icode & 0xF0) << 8); // this will in effect be signed
                if (code < 12) {
                    inp1[j] = ((int) ((int) inp1[j] * (int) vscale) >> 16);
                }
                j++;

                inp1[j] = (short) ((icode & 0xF) << 12);
                if (code < 12) {
                    inp1[j] = ((int) ((int) inp1[j] * (int) vscale) >> 16);
                }
                j++;
            }
            j = 0;
            while (j < 8) {
                icode = BufferSpace.get((AudioInBuffer + inPtr)) & 0xFF;
                inPtr++;

                inp2[j] = (short) ((icode & 0xF0) << 8); // this will in effect be signed
                if (code < 12) {
                    inp2[j] = ((int) ((int) inp2[j] * (int) vscale) >> 16);
                }
                j++;

                inp2[j] = (short) ((icode & 0xF) << 12);
                if (code < 12) {
                    inp2[j] = ((int) ((int) inp2[j] * (int) vscale) >> 16);
                }
                j++;
            }

            if (book1_p < 0 || book1_p + 7 >= adpcmtable.length
                || book2_p < 0 || book2_p + 7 >= adpcmtable.length) {

                // corrupted predictor index → fallback to silence
                // Since 'a' is reused, explicit zeroing is good safety, though loop overwrites usually
                Arrays.fill(a, 0);

                // Skip broken block safely
                count -= 0x10; 
                continue;
            }

            a[0] = (int) adpcmtable[book1_p + 0] * (int) l1;
            a[0] += (int) adpcmtable[book2_p + 0] * (int) l2;
            a[0] += (int) inp1[0] * (int) 2048;

            a[1] = (int) adpcmtable[book1_p + 1] * (int) l1;
            a[1] += (int) adpcmtable[book2_p + 1] * (int) l2;
            a[1] += (int) adpcmtable[book2_p + 0] * inp1[0];
            a[1] += (int) inp1[1] * (int) 2048;

            a[2] = (int) adpcmtable[book1_p + 2] * (int) l1;
            a[2] += (int) adpcmtable[book2_p + 2] * (int) l2;
            a[2] += (int) adpcmtable[book2_p + 1] * inp1[0];
            a[2] += (int) adpcmtable[book2_p + 0] * inp1[1];
            a[2] += (int) inp1[2] * (int) 2048;

            a[3] = (int) adpcmtable[book1_p + 3] * (int) l1;
            a[3] += (int) adpcmtable[book2_p + 3] * (int) l2;
            a[3] += (int) adpcmtable[book2_p + 2] * inp1[0];
            a[3] += (int) adpcmtable[book2_p + 1] * inp1[1];
            a[3] += (int) adpcmtable[book2_p + 0] * inp1[2];
            a[3] += (int) inp1[3] * (int) 2048;

            a[4] = (int) adpcmtable[book1_p + 4] * (int) l1;
            a[4] += (int) adpcmtable[book2_p + 4] * (int) l2;
            a[4] += (int) adpcmtable[book2_p + 3] * inp1[0];
            a[4] += (int) adpcmtable[book2_p + 2] * inp1[1];
            a[4] += (int) adpcmtable[book2_p + 1] * inp1[2];
            a[4] += (int) adpcmtable[book2_p + 0] * inp1[3];
            a[4] += (int) inp1[4] * (int) 2048;

            a[5] = (int) adpcmtable[book1_p + 5] * (int) l1;
            a[5] += (int) adpcmtable[book2_p + 5] * (int) l2;
            a[5] += (int) adpcmtable[book2_p + 4] * inp1[0];
            a[5] += (int) adpcmtable[book2_p + 3] * inp1[1];
            a[5] += (int) adpcmtable[book2_p + 2] * inp1[2];
            a[5] += (int) adpcmtable[book2_p + 1] * inp1[3];
            a[5] += (int) adpcmtable[book2_p + 0] * inp1[4];
            a[5] += (int) inp1[5] * (int) 2048;

            a[6] = (int) adpcmtable[book1_p + 6] * (int) l1;
            a[6] += (int) adpcmtable[book2_p + 6] * (int) l2;
            a[6] += (int) adpcmtable[book2_p + 5] * inp1[0];
            a[6] += (int) adpcmtable[book2_p + 4] * inp1[1];
            a[6] += (int) adpcmtable[book2_p + 3] * inp1[2];
            a[6] += (int) adpcmtable[book2_p + 2] * inp1[3];
            a[6] += (int) adpcmtable[book2_p + 1] * inp1[4];
            a[6] += (int) adpcmtable[book2_p + 0] * inp1[5];
            a[6] += (int) inp1[6] * (int) 2048;

            a[7] = (int) adpcmtable[book1_p + 7] * (int) l1;
            a[7] += (int) adpcmtable[book2_p + 7] * (int) l2;
            a[7] += (int) adpcmtable[book2_p + 6] * inp1[0];
            a[7] += (int) adpcmtable[book2_p + 5] * inp1[1];
            a[7] += (int) adpcmtable[book2_p + 4] * inp1[2];
            a[7] += (int) adpcmtable[book2_p + 3] * inp1[3];
            a[7] += (int) adpcmtable[book2_p + 2] * inp1[4];
            a[7] += (int) adpcmtable[book2_p + 1] * inp1[5];
            a[7] += (int) adpcmtable[book2_p + 0] * inp1[6];
            a[7] += (int) inp1[7] * (int) 2048;

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

            a[0] = (int) adpcmtable[book1_p + 0] * (int) l1;
            a[0] += (int) adpcmtable[book2_p + 0] * (int) l2;
            a[0] += (int) inp2[0] * (int) 2048;

            a[1] = (int) adpcmtable[book1_p + 1] * (int) l1;
            a[1] += (int) adpcmtable[book2_p + 1] * (int) l2;
            a[1] += (int) adpcmtable[book2_p + 0] * inp2[0];
            a[1] += (int) inp2[1] * (int) 2048;

            a[2] = (int) adpcmtable[book1_p + 2] * (int) l1;
            a[2] += (int) adpcmtable[book2_p + 2] * (int) l2;
            a[2] += (int) adpcmtable[book2_p + 1] * inp2[0];
            a[2] += (int) adpcmtable[book2_p + 0] * inp2[1];
            a[2] += (int) inp2[2] * (int) 2048;

            a[3] = (int) adpcmtable[book1_p + 3] * (int) l1;
            a[3] += (int) adpcmtable[book2_p + 3] * (int) l2;
            a[3] += (int) adpcmtable[book2_p + 2] * inp2[0];
            a[3] += (int) adpcmtable[book2_p + 1] * inp2[1];
            a[3] += (int) adpcmtable[book2_p + 0] * inp2[2];
            a[3] += (int) inp2[3] * (int) 2048;

            a[4] = (int) adpcmtable[book1_p + 4] * (int) l1;
            a[4] += (int) adpcmtable[book2_p + 4] * (int) l2;
            a[4] += (int) adpcmtable[book2_p + 3] * inp2[0];
            a[4] += (int) adpcmtable[book2_p + 2] * inp2[1];
            a[4] += (int) adpcmtable[book2_p + 1] * inp2[2];
            a[4] += (int) adpcmtable[book2_p + 0] * inp2[3];
            a[4] += (int) inp2[4] * (int) 2048;

            a[5] = (int) adpcmtable[book1_p + 5] * (int) l1;
            a[5] += (int) adpcmtable[book2_p + 5] * (int) l2;
            a[5] += (int) adpcmtable[book2_p + 4] * inp2[0];
            a[5] += (int) adpcmtable[book2_p + 3] * inp2[1];
            a[5] += (int) adpcmtable[book2_p + 2] * inp2[2];
            a[5] += (int) adpcmtable[book2_p + 1] * inp2[3];
            a[5] += (int) adpcmtable[book2_p + 0] * inp2[4];
            a[5] += (int) inp2[5] * (int) 2048;

            a[6] = (int) adpcmtable[book1_p + 6] * (int) l1;
            a[6] += (int) adpcmtable[book2_p + 6] * (int) l2;
            a[6] += (int) adpcmtable[book2_p + 5] * inp2[0];
            a[6] += (int) adpcmtable[book2_p + 4] * inp2[1];
            a[6] += (int) adpcmtable[book2_p + 3] * inp2[2];
            a[6] += (int) adpcmtable[book2_p + 2] * inp2[3];
            a[6] += (int) adpcmtable[book2_p + 1] * inp2[4];
            a[6] += (int) adpcmtable[book2_p + 0] * inp2[5];
            a[6] += (int) inp2[6] * (int) 2048;

            a[7] = (int) adpcmtable[book1_p + 7] * (int) l1;
            a[7] += (int) adpcmtable[book2_p + 7] * (int) l2;
            a[7] += (int) adpcmtable[book2_p + 6] * inp2[0];
            a[7] += (int) adpcmtable[book2_p + 5] * inp2[1];
            a[7] += (int) adpcmtable[book2_p + 4] * inp2[2];
            a[7] += (int) adpcmtable[book2_p + 3] * inp2[3];
            a[7] += (int) adpcmtable[book2_p + 2] * inp2[4];
            a[7] += (int) adpcmtable[book2_p + 1] * inp2[5];
            a[7] += (int) adpcmtable[book2_p + 0] * inp2[6];
            a[7] += (int) inp2[7] * (int) 2048;

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

    static void LOADADPCM() {
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
}
