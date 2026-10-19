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

package aziaudio.hle.abi3;
import aziaudio.hle.HLEMain;
import aziaudio.hle.abi1.ABI1;

import java.nio.ShortBuffer;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi3.ABI3.*;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI3 Sample-rate conversion and interpolation. */
final class ABI3Resampler {
    private ABI3Resampler() {}

    static void RESAMPLE3() {
        int flags = ((inst2 >>> 0x1E)) & 0xFF;
        int Pitch = ((inst2 >>> 0xE) & 0xFFFF) << 1;
        int addy = inst1 & 0xFFFFFF;
        int Accum = 0;
        int location;
        ShortBuffer dst = BufferSpaceShort;
        ShortBuffer src = BufferSpaceShort;
        int srcPtr = (((inst2 >>> 2) & 0xFFF) + 0x4F0) / 2;
        int dstPtr;
        int temp;
        int accum;

        srcPtr -= 4;

        if ((inst2 & 0x3) != 0) {
            dstPtr = 0x660 / 2;
        } else {
            dstPtr = 0x4F0 / 2;
        }

        if ((flags & 0x1) == 0) {
            for (int x = 0; x < 4; x++) {
                src.put((srcPtr + x) ^ 1, rdram.getShort((((addy / 2) + x) ^ 1) * 2));
            }
            Accum = rdram.getShort(addy + 8) & 0xFFFF;
        } else {
            for (int x = 0; x < 4; x++) {
                src.put((srcPtr + x) ^ 1, (short) 0);
            }
        }

        for (int i = 0; i < 0x170 / 2; i++) {
            location = (((Accum * 0x40) >>> 16) * 8) / 2;

            temp = ((int) src.get((srcPtr + 0) ^ 1) * ((int) ((short) ResampleLUT[location + 0])));
            accum = (temp >> 15);

            temp = ((int) src.get((srcPtr + 1) ^ 1) * ((int) ((short) ResampleLUT[location + 1])));
            accum += (temp >> 15);

            temp = ((int) src.get((srcPtr + 2) ^ 1) * ((int) ((short) ResampleLUT[location + 2])));
            accum += (temp >> 15);

            temp = ((int) src.get((srcPtr + 3) ^ 1) * ((int) ((short) ResampleLUT[location + 3])));
            accum += (temp >> 15);

            if (accum > 32767) {
                accum = 32767;
            }
            if (accum < -32768) {
                accum = -32768;
            }

            dst.put(dstPtr ^ 1, (short) accum);
            dstPtr++;
            Accum += Pitch;
            srcPtr += (Accum >>> 16);
            Accum &= 0xFFFF;
        }

        for (int x = 0; x < 4; x++) {
            rdram.putShort((((addy / 2) + x) ^ 1) * 2, src.get((srcPtr + x) ^ 1));
        }
        rdram.putShort(addy + 8, (short) Accum);
    }
}
