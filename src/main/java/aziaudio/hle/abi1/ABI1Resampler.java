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

import java.nio.ShortBuffer;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI1 Sample-rate conversion and interpolation. */
final class ABI1Resampler {
    private ABI1Resampler() {}

    private static int MultAddLUT(ShortBuffer src, int srcPtr, int location) {
    	int lutIdx = location / 2; // Byte offset to short index
    	int accum = 0;
    	for (int i = 0; i < 4; i++) {
    		short sample = src.get(srcPtr + i);
    		short coef = ResampleLUT[lutIdx + i];
    		accum += (sample * coef) >> 15;
    	}
    	return accum;
    }

    static void RESAMPLE() {
        int flags = (inst1 >>> 16) & 0xFF;
        int Pitch = (inst1 & 0xFFFF) << 1;
        int addy = inst2 & 0xFFFFFF;
        int Accum = 0;

        ShortBuffer dst = BufferSpaceShort;
        ShortBuffer src = BufferSpaceShort;
        int srcPtr = AudioInBuffer / 2 - 4;
        int dstPtr = AudioOutBuffer / 2;

        if ((flags & 0x1) == 0) {
            for (int x = 0; x < 4; x++) {
                src.put(srcPtr + x, rdram.getShort(addy + x * 2));
            }
            Accum = rdram.getShort(addy + 8) & 0xFFFF;
        } else {
            for (int x = 0; x < 4; x++) src.put(srcPtr + x, (short) 0);
        }

        int count = ((AudioCount + 0xF) & 0xFFF0) / 2;

        // Pre-calculate loop limit to avoid repeated calculations
        // C++: location = (Accum >> 0xa) << 0x3;

        for (int i = 0; i < count; i++) {
            // 4-point Gaussian Resampling (ported from AziAudio C++)
        	int location = (Accum >> 10) << 3;

            int accum = MultAddLUT(src, srcPtr, location);

            if (accum > 32767) accum = 32767;
            else if (accum < -32768) accum = -32768;

            dst.put(dstPtr, (short) accum);

            dstPtr++;
            Accum += Pitch;
            srcPtr += (Accum >>> 16);
            Accum &= 0xFFFF;
        }

        for (int x = 0; x < 4; x++) {
            rdram.putShort(addy + x * 2, src.get(srcPtr + x));
        }
        rdram.putShort(addy + 8, (short) Accum);
    }
}
