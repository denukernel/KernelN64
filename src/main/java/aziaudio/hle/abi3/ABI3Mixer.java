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

/** ABI3 Volume, gain, and envelope mixing commands. */
final class ABI3Mixer {
    private ABI3Mixer() {}

    static void SETVOL3() {
        int flags = (inst1 >>> 16) & 0xFF;
        if ((flags & 0x4) != 0) { // 288
            if ((flags & 0x2) != 0) { // 290
                Vol_Left = (short) inst1; // 0x50
                Env_Dry = (short) ((int) inst2 >> 16); // 0x4E
                Env_Wet = (short) inst2; // 0x4C
            } else {
                VolTrg_Right = (short) inst1; // 0x46
                VolRamp_Right = (int) inst2; // 0x48/0x4A
            }
        } else {
            VolTrg_Left = (short) inst1; // 0x40
            VolRamp_Left = (int) inst2; // 0x42/0x44
        }
    }

    static void ENVMIXER3() {
        int flags = (inst1 >>> 16) & 0xFF;
        int addy = inst2 & 0xFFFFFF;
        ShortBuffer inp = BufferSpaceShort;
        int inp_p = 0x4F0 / 2;
        ShortBuffer out = BufferSpaceShort;
        int out_p = 0x9D0 / 2;
        ShortBuffer aux1 = BufferSpaceShort;
        int aux1_p = 0xB40 / 2;
        ShortBuffer aux2 = BufferSpaceShort;
        int aux2_p = 0xCB0 / 2;
        ShortBuffer aux3 = BufferSpaceShort;
        int aux3_p = 0xE20 / 2;
        int MainR;
        int MainL;
        int AuxR;
        int AuxL;
        int i1, o1, a1, a2, a3;
        int LAdder, LAcc, LVol;
        int RAdder, RAcc, RVol;
        short RSig, LSig; // Most significant part of the Ramp Value
        short Wet, Dry;
        short LTrg, RTrg;

        Vol_Right = (short) inst1;

        if ((flags & A_INIT) != 0) {
            LAdder = VolRamp_Left / 8;
            LAcc = 0;
            LVol = Vol_Left;
            LSig = (short) (VolRamp_Left >> 16);
            RAdder = VolRamp_Right / 8;
            RAcc = 0;
            RVol = Vol_Right;
            RSig = (short) (VolRamp_Right >> 16);
            Wet = (short) Env_Wet;
            Dry = (short) Env_Dry;
            LTrg = VolTrg_Left;
            RTrg = VolTrg_Right;
        } else {
            System.arraycopy(rdram.array(), addy, hleMixerWorkArea.array(), 0, 80);
            Wet = hleMixerWorkArea.getShort(0 + 2); // 0-1
            Dry = hleMixerWorkArea.getShort(4 + 2); // 2-3
            LTrg = hleMixerWorkArea.getShort(8 + 2); // 4-5
            RTrg = hleMixerWorkArea.getShort(12 + 2); // 6-7
            LAdder = hleMixerWorkArea.getInt(16); // 8-9 (hleMixerWorkArea is a 16bit pointer)
            RAdder = hleMixerWorkArea.getInt(20); // 10-11
            LAcc = hleMixerWorkArea.getInt(24); // 12-13
            RAcc = hleMixerWorkArea.getInt(28); // 14-15
            LVol = hleMixerWorkArea.getInt(32); // 16-17
            RVol = hleMixerWorkArea.getInt(36); // 18-19
            LSig = hleMixerWorkArea.getShort(40 + 2); // 20-21
            RSig = hleMixerWorkArea.getShort(44 + 2); // 22-23
        }

        for (int y = 0; y < (0x170 / 2); y++) {
            // Left
            LAcc += LAdder;
            LVol += (LAcc >> 16);
            LAcc &= 0xFFFF;
            // Right
            RAcc += RAdder;
            RVol += (RAcc >> 16);
            RAcc &= 0xFFFF;

            // Clamp Left
            if (LSig >= 0) { // VLT
                if (LVol > LTrg) {
                    LVol = LTrg;
                }
            } else { // VGE
                if (LVol < LTrg) {
                    LVol = LTrg;
                }
            }

            // Clamp Right
            if (RSig >= 0) { // VLT
                if (RVol > RTrg) {
                    RVol = RTrg;
                }
            } else { // VGE
                if (RVol < RTrg) {
                    RVol = RTrg;
                }
            }

            MainL = ((Dry * LVol) + 0x4000) >> 15;
            MainR = ((Dry * RVol) + 0x4000) >> 15;

            o1 = out.get(out_p + (y ^ 1));
            a1 = aux1.get(aux1_p + (y ^ 1));
            i1 = inp.get(inp_p + (y ^ 1));

            o1 += ((i1 * MainL) + 0x4000) >> 15;
            a1 += ((i1 * MainR) + 0x4000) >> 15;

            if (o1 > 32767) {
                o1 = 32767;
            } else if (o1 < -32768) {
                o1 = -32768;
            }

            if (a1 > 32767) {
                a1 = 32767;
            } else if (a1 < -32768) {
                a1 = -32768;
            }

            out.put(out_p + (y ^ 1), (short) o1);
            aux1.put(aux1_p + (y ^ 1), (short) a1);

            a2 = aux2.get(aux2_p + (y ^ 1));
            a3 = aux3.get(aux3_p + (y ^ 1));

            AuxL = ((Wet * LVol) + 0x4000) >> 15;
            AuxR = ((Wet * RVol) + 0x4000) >> 15;

            a2 += ((i1 * AuxL) + 0x4000) >> 15;
            a3 += ((i1 * AuxR) + 0x4000) >> 15;

            if (a2 > 32767) {
                a2 = 32767;
            } else if (a2 < -32768) {
                a2 = -32768;
            }

            if (a3 > 32767) {
                a3 = 32767;
            } else if (a3 < -32768) {
                a3 = -32768;
            }

            aux2.put(aux2_p + (y ^ 1), (short) a2);
            aux3.put(aux3_p + (y ^ 1), (short) a3);
        }

        hleMixerWorkArea.putShort(0 + 2, Wet); // 0-1
        hleMixerWorkArea.putShort(4 + 2, Dry); // 2-3
        hleMixerWorkArea.putShort(8 + 2, LTrg); // 4-5
        hleMixerWorkArea.putShort(12 + 2, RTrg); // 6-7
        hleMixerWorkArea.putInt(16, LAdder); // 8-9 (hleMixerWorkArea is a 16bit pointer)
        hleMixerWorkArea.putInt(20, RAdder); // 10-11
        hleMixerWorkArea.putInt(24, LAcc); // 12-13
        hleMixerWorkArea.putInt(28, RAcc); // 14-15
        hleMixerWorkArea.putInt(32, LVol); // 16-17
        hleMixerWorkArea.putInt(36, RVol); // 18-19
        hleMixerWorkArea.putShort(40 + 2, LSig); // 20-21
        hleMixerWorkArea.putShort(44 + 2, RSig); // 22-23
        System.arraycopy(hleMixerWorkArea.array(), 0, rdram.array(), addy, 80);
    }

    static void MIXER3() {
        int dmemin = ((inst2 >>> 16) + 0x4F0) & 0xFFFF;
        int dmemout = ((inst2 & 0xFFFF) + 0x4F0) & 0xFFFF;
        int gain = (short) (inst1 & 0xFFFF) * 2;
        int temp;

        for (int x = 0; x < 0x170; x += 2) {
            temp = (BufferSpace.getShort(dmemin + x) * gain) >> 16;
            temp += BufferSpace.getShort(dmemout + x);

            if (temp > 32767) {
                temp = 32767;
            }
            if (temp < -32768) {
                temp = -32768;
            }

            BufferSpace.putShort(dmemout + x, (short) temp);
        }
    }
}
