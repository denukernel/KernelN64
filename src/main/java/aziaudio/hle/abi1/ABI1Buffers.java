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
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI1 Audio buffer transfers, interleaving, and loop/segment commands. */
final class ABI1Buffers {
    private ABI1Buffers() {}

    static void CLEARBUFF() {
        int addr = inst1 & 0xFFFC;
        int count = ((inst2 & 0xFFFF) + 3) & 0xFFFC;
        Arrays.fill(BufferSpace.array(), addr, addr + count, (byte) 0);
    }

    static void SETLOOP() {
        loopval = inst2 & 0xFFFFFF;
    }

    static void LOADBUFF() {
        if (AudioCount == 0) {
            return;
        }
        int cnt = (AudioCount + 3) & 0xFFFC;
        int v0 = inst2 & 0xFFFFFC;
        for (int i = 0; i < cnt; i++)
            BufferSpace.put((AudioInBuffer & 0xFFFC) + i, rdram.get(v0 + i));
    }

    static void SAVEBUFF() {
        if (AudioCount == 0) {
            return;
        }
        int cnt = (AudioCount + 3) & 0xFFFC;
        int v0 = inst2 & 0xFFFFFF;
        for (int i = 0; i < cnt; i++)
            rdram.put(v0 + i, BufferSpace.get((AudioOutBuffer & 0xFFFC) + i));
    }

    static void SEGMENT() {
        
    }

    static void SETBUFF() {
        if (((inst1 >>> 16) & 0x8) != 0) {
            // A_AUX - Auxillary Sound Buffer Settings
            AudioAuxA = inst1 & 0xFFFF;
            AudioAuxC = inst2 >>> 16;
            AudioAuxE = inst2 & 0xFFFF;
        } else {
            // A_MAIN - Main Sound Buffer Settings
            AudioInBuffer = inst1 & 0xFFFF; // 0x00
            AudioOutBuffer = inst2 >>> 16; // 0x02
            AudioCount = inst2 & 0xFFFF; // 0x04
        }
    }

    static void DMEMMOVE() {
        if ((inst2 & 0xFFFF) == 0) {
            return;
        }
        int v0 = inst1 & 0xFFFF;
        int v1 = inst2 >>> 16;
        int count = (inst2 + 3) & 0xFFFC;
        System.arraycopy(BufferSpace.array(), v0, BufferSpace.array(), v1, count);
    }

    static void INTERLEAVE() {
        ShortBuffer outbuff = BufferSpaceShort;
        int outbuff_p = AudioOutBuffer / 2;
        ShortBuffer inSrcR;
        ShortBuffer inSrcL;
        int inSrcR_p;
        int inSrcL_p;

        inSrcR = BufferSpaceShort;
        inSrcR_p = (inst2 & 0xFFFF) / 2;
        inSrcL = BufferSpaceShort;
        inSrcL_p = (inst2 >>> 16) / 2;

        for (int x = 0; x < (AudioCount / 4); x++) {
            outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
            outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
            outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
            outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
        }
    }
}
