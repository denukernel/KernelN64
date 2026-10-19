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
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi3.ABI3.*;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI3 Audio buffer transfers, interleaving, and loop/segment commands. */
final class ABI3Buffers {
    private ABI3Buffers() {}

    static void CLEARBUFF3() {
        int addr = (inst1 & 0xFFFF) + 0x4F0;
        int count = inst2 & 0xFFFF;
        Arrays.fill(BufferSpace.array(), addr, addr + count, (byte) 0);
    }

    static void LOADBUFF3() {
        int cnt = ((inst1 >>> 0xC) + 3) & 0xFFC;
        int v0 = inst2 & 0xFFFFFC;
        System.arraycopy(rdram.array(), v0, BufferSpace.array(), (inst1 & 0xFFC) + 0x4F0, cnt);
    }

    static void SAVEBUFF3() {
        int cnt = (((inst1 >>> 0xC) + 3) & 0xFFC);
        int v0 = inst2 & 0xFFFFFC;
        System.arraycopy(BufferSpace.array(), (inst1 & 0xFFC) + 0x4F0, rdram.array(), v0, cnt);
    }

    static void DMEMMOVE3() {
        int v0 = (inst1 & 0xFFFF) + 0x4F0;
        int v1 = (inst2 >>> 16) + 0x4F0;
        int count = (inst2 + 3) & 0xFFFC;
        System.arraycopy(BufferSpace.array(), v0, BufferSpace.array(), v1, count);
    }

    static void SETLOOP3() {
        loopval = inst2 & 0xFFFFFF;
    }

    static void INTERLEAVE3() {
        ShortBuffer outbuff = BufferSpaceShort;
        int outbuff_p = 0x4F0 / 2;
        ShortBuffer inSrcR;
        ShortBuffer inSrcL;
        int inSrcR_p;
        int inSrcL_p;

        inSrcR = BufferSpaceShort;
        inSrcR_p = 0xB40 / 2;
        inSrcL = BufferSpaceShort;
        inSrcL_p = 0x9D0 / 2;

        for (int x = 0; x < (0x170 / 4); x++) {
            outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
            outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
            outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
            outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
        }
    }
}
