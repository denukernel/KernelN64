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
import java.nio.ShortBuffer;
import java.util.Arrays;
import static aziaudio.hle.HLEMain.inst1;
import static aziaudio.hle.HLEMain.inst2;
import static aziaudio.hle.HLEMain.rdram;
import static aziaudio.hle.abi2.ABI2.*;
import static aziaudio.hle.abi1.ABI1.*;

/** ABI2 Audio buffer transfers, interleaving, and loop/segment commands. */
final class ABI2Buffers {
    private ABI2Buffers() {}

    static void SETLOOP2() {
        loopval = inst2 & 0xFFFFFF;
    }

    static void SETBUFF2() {
        AudioInBuffer = inst1 & 0xFFFF; // 0x00
        AudioOutBuffer = inst2 >>> 16; // 0x02
        AudioCount = inst2 & 0xFFFF; // 0x04
    }

    static void CLEARBUFF2() {
        int addr = inst1 & 0xFFFF;
        int count = inst2 & 0xFFFF;
        if (count > 0) {
        	Arrays.fill(BufferSpace.array(), addr, addr + count, (byte) 0);
        }
    }

    static void LOADBUFF2() {
        int cnt = ((inst1 >>> 0xC) + 3) & 0xFFC;
        int v0 = inst2 & 0xFFFFFC;
        System.arraycopy(rdram.array(), v0, BufferSpace.array(), (inst1 & 0xFFFC), (cnt + 3) & 0xFFFC);
    }

    static void SAVEBUFF2() {
        int cnt = ((inst1 >>> 0xC) + 3) & 0xFFC;
        int v0 = inst2 & 0xFFFFFC;
        System.arraycopy(BufferSpace.array(), (inst1 & 0xFFFC), rdram.array(), v0, (cnt + 3) & 0xFFFC);
    }

    static void DMEMMOVE2() {
        if ((inst2 & 0xFFFF) == 0) {
        	return;
        }
        int v0 = inst1 & 0xFFFF;
        int v1 = inst2 >>> 16;
        int count = (inst2 + 3) & 0xFFFC;
        System.arraycopy(BufferSpace.array(), v0, BufferSpace.array(), v1, count);
    }

    static void DUPLICATE2() {
        int Count = (inst1 >>> 16) & 0xFF;
        int In = inst1 & 0xFFFF;
        int Out = inst2 >>> 16;
        byte[] buff = new byte[128];

        System.arraycopy(BufferSpace.array(), In, buff, 0, 128);

        while (Count != 0) {
        	System.arraycopy(buff, 0, BufferSpace.array(), Out, 128);
        	Out += 128;
        	Count--;
        }
    }

    static void INTERL2() {
        int Count = inst1 & 0xFFFF;
        int Out = inst2 & 0xFFFF;
        int In = inst2 >>> 16;

        ByteBuffer src = BufferSpace; // [In];
        ByteBuffer dst = BufferSpace; // [Out];
        while (Count != 0) {
        	dst.putShort(Out, src.getShort(In));
        	Out += 2;
        	In += 4;
        	Count--;
        }
    }

    static void INTERLEAVE2() {
        ShortBuffer outbuff;
        int outbuff_p;
        ShortBuffer inSrcR;
        ShortBuffer inSrcL;
        int inSrcR_p;
        int inSrcL_p;
        int count;

        count = (inst1 >>> 12) & 0xFF0;
        outbuff = BufferSpaceShort;
        if (count == 0) {
        	outbuff_p = AudioOutBuffer / 2;
        	count = AudioCount;
        } else {
        	outbuff_p = (inst1 & 0xFFFF) / 2;
        }

        inSrcR = BufferSpaceShort;
        inSrcR_p = (inst2 & 0xFFFF) / 2;
        inSrcL = BufferSpaceShort;
        inSrcL_p = (inst2 >>> 16) / 2;

        for (int x = 0; x < (count / 4); x++) {
        	outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
        	outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
        	outbuff.put(outbuff_p++, inSrcL.get(inSrcL_p++));
        	outbuff.put(outbuff_p++, inSrcR.get(inSrcR_p++));
        }
    }

    static void SEGMENT2() {
        			if (isZeldaABI) {
        //		FILTER2 ();
        				return;
        			}
        			if ((inst1 & 0xFFFFFF) == 0) {
        				isMKABI = true;
        			} else {
        				isMKABI = false;
        				isZeldaABI = true;
        //		FILTER2 ();
        			}
    }
}
