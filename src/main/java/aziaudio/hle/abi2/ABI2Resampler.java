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

import java.nio.ShortBuffer;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi2.ABI2.*;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI2 Sample-rate conversion and interpolation. */
final class ABI2Resampler {
    private ABI2Resampler() {}

    static void RESAMPLE2() {
        int flags = (inst1 >>> 16) & 0xFF;
        // Command pitch is Q1.15; the running phase is Q16.16.
        int Pitch = (inst1 & 0xFFFF) << 1;
        int addy = inst2 & 0xFFFFFF;

        int Accum;
        int srcPtr = (AudioInBuffer / 2) - 4;
        int dstPtr = AudioOutBuffer / 2;

        ShortBuffer src = BufferSpaceShort;
        ShortBuffer dst = BufferSpaceShort;

        // proper accumulator
        if ((flags & 1) == 0) {
        	for (int i = 0; i < 4; i++)
        		src.put(srcPtr + i, rdram.getShort(addy + i * 2));
        	Accum = rdram.getShort(addy + 8) & 0xFFFF;
        } else {
        	for (int i = 0; i < 4; i++)
        		src.put(srcPtr + i, (short) 0);
        	Accum = 0;
        }

        int count = ((AudioCount + 0xF) & 0xFFF0) >> 1;

        for (int i = 0; i < count; i++) {

        	int idx = ((Accum >> 10) & 0x3F) << 2;

        	short c0 = ResampleLUT[idx];
        	short c1 = ResampleLUT[idx + 1];
        	short c2 = ResampleLUT[idx + 2];
        	short c3 = ResampleLUT[idx + 3];

        	// Java buffers already use N64 byte order; no host word swapping.
        	int s0 = src.get(srcPtr);
        	int s1 = src.get(srcPtr + 1);
        	int s2 = src.get(srcPtr + 2);
        	int s3 = src.get(srcPtr + 3);

        	int out = (s0 * c0);
        	out += (s1 * c1);
        	out += (s2 * c2);
        	out += (s3 * c3);
        	out >>= 15;

        	if (out > 32767)
        		out = 32767;
        	else if (out < -32768)
        		out = -32768;

        	dst.put(dstPtr, (short) out);
        	dstPtr++;

        	Accum += Pitch;
        	srcPtr += (Accum >> 16);
        	Accum &= 0xFFFF;
        }

        // Every command resumes from this history as well as its fractional phase.
        for (int i = 0; i < 4; i++)
            rdram.putShort(addy + i * 2, src.get(srcPtr + i));
        rdram.putShort(addy + 8, (short) Accum);
    }
}
