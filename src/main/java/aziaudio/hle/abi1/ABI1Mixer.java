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

/** ABI1 Volume, gain, and envelope mixing commands. */
final class ABI1Mixer {
    private ABI1Mixer() {}

    static void ENVMIXER() {
        int flags = (inst1 >>> 16) & 0xFF;
        int addy = inst2 & 0xFFFFFF;
        ShortBuffer inp = BufferSpaceShort;
        int inp_p = AudioInBuffer / 2;
        ShortBuffer out = BufferSpaceShort;
        int out_p = AudioOutBuffer / 2;
        ShortBuffer aux1 = BufferSpaceShort;
        int aux1_p = AudioAuxA / 2;
        ShortBuffer aux2 = BufferSpaceShort;
        int aux2_p = AudioAuxC / 2;
        ShortBuffer aux3 = BufferSpaceShort;
        int aux3_p = AudioAuxE / 2;
        int MainR;
        int MainL;
        int AuxR;
        int AuxL;
        int i1, o1, a1, a2 = 0, a3 = 0;
        int AuxIncRate = 1;

        // Note: Allocation here, but small (8 bytes). 
        // Could be optimized, but ADPCM is the bigger offender.
        ShortBuffer zero = ShortBuffer.allocate(8); 

        int LVol, RVol;
        int LAcc, RAcc;
        int LTrg, RTrg;
        short Wet, Dry;
        int ptr = 0;
        int RRamp, LRamp;
        int LAdderStart, RAdderStart, LAdderEnd, RAdderEnd;
        int oMainR, oMainL, oAuxR, oAuxL;

        if ((flags & A_INIT) != 0) {
            LVol = ((Vol_Left * (int) VolRamp_Left));
            RVol = ((Vol_Right * (int) VolRamp_Right));
            Wet = (short) Env_Wet;
            Dry = (short) Env_Dry;
            LTrg = (VolTrg_Left << 16);
            RTrg = (VolTrg_Right << 16);
            LAdderStart = Vol_Left << 16;
            RAdderStart = Vol_Right << 16;
            LAdderEnd = LVol;
            RAdderEnd = RVol;
            RRamp = VolRamp_Right;
            LRamp = VolRamp_Left;
        } else {
            System.arraycopy(rdram.array(), addy, hleMixerWorkArea.array(), 0, 80);
            Wet = hleMixerWorkArea.getShort(0 + 2); // 0-1
            Dry = hleMixerWorkArea.getShort(4 + 2); // 2-3
            LTrg = hleMixerWorkArea.getInt(8); // 4-5
            RTrg = hleMixerWorkArea.getInt(12); // 6-7
            LRamp = hleMixerWorkArea.getInt(16); // 8-9 (hleMixerWorkArea is a 16bit pointer)
            RRamp = hleMixerWorkArea.getInt(20); // 10-11
            LAdderEnd = hleMixerWorkArea.getInt(24); // 12-13
            RAdderEnd = hleMixerWorkArea.getInt(28); // 14-15
            LAdderStart = hleMixerWorkArea.getInt(32); // 12-13
            RAdderStart = hleMixerWorkArea.getInt(36); // 14-15
        }

        if ((flags & A_AUX) == 0) {
            AuxIncRate = 0;
            aux2 = aux3 = zero;
        }

        oMainL = (Dry * (LTrg >> 16) + 0x4000) >> 15;
        oAuxL = (Wet * (LTrg >> 16) + 0x4000) >> 15;
        oMainR = (Dry * (RTrg >> 16) + 0x4000) >> 15;
        oAuxR = (Wet * (RTrg >> 16) + 0x4000) >> 15;

        for (int y = 0; y < AudioCount; y += 16) {
            if (LAdderStart != LTrg) {
                LAcc = LAdderStart;
                LVol = (LAdderEnd - LAdderStart) >> 3;
                LAdderEnd = (int) (((long) LAdderEnd * (long) LRamp) >> 16);
                LAdderStart = (int) (((long) LAcc * (long) LRamp) >> 16);
            } else {
                LAcc = LTrg;
                LVol = 0;
            }

            if (RAdderStart != RTrg) {
                RAcc = RAdderStart;
                RVol = (RAdderEnd - RAdderStart) >> 3;
                RAdderEnd = (int) (((long) RAdderEnd * (long) RRamp) >> 16);
                RAdderStart = (int) (((long) RAcc * (long) RRamp) >> 16);
            } else {
                RAcc = RTrg;
                RVol = 0;
            }

            for (int x = 0; x < 8; x++) {
                i1 = (int) inp.get(inp_p + ptr);
                o1 = (int) out.get(out_p + ptr);
                a1 = (int) aux1.get(aux1_p + ptr);
                if (AuxIncRate != 0) {
                    a2 = (int) aux2.get(aux2_p + ptr);
                    a3 = (int) aux3.get(aux3_p + ptr);
                }

                LAcc += LVol;
                RAcc += RVol;

                if (LVol <= 0) { // Decrementing
                    if (LAcc < LTrg) {
                        LAcc = LTrg;
                        LAdderStart = LTrg;
                        MainL = oMainL;
                        AuxL = oAuxL;
                    } else {
                        MainL = (Dry * ((int) LAcc >> 16) + 0x4000) >> 15;
                        AuxL = (Wet * ((int) LAcc >> 16) + 0x4000) >> 15;
                    }
                } else {
                    if (LAcc > LTrg) {
                        LAcc = LTrg;
                        LAdderStart = LTrg;
                        MainL = oMainL;
                        AuxL = oAuxL;
                    } else {
                        MainL = (Dry * ((int) LAcc >> 16) + 0x4000) >> 15;
                        AuxL = (Wet * ((int) LAcc >> 16) + 0x4000) >> 15;
                    }
                }

                if (RVol <= 0) { // Decrementing
                    if (RAcc < RTrg) {
                        RAcc = RTrg;
                        RAdderStart = RTrg;
                        MainR = oMainR;
                        AuxR = oAuxR;
                    } else {
                        MainR = (Dry * ((int) RAcc >> 16) + 0x4000) >> 15;
                        AuxR = (Wet * ((int) RAcc >> 16) + 0x4000) >> 15;
                    }
                } else {
                    if (RAcc > RTrg) {
                        RAcc = RTrg;
                        RAdderStart = RTrg;
                        MainR = oMainR;
                        AuxR = oAuxR;
                    } else {
                        MainR = (Dry * ((int) RAcc >> 16) + 0x4000) >> 15;
                        AuxR = (Wet * ((int) RAcc >> 16) + 0x4000) >> 15;
                    }
                }

                o1 += ((i1 * MainR) + 0x4000) >> 15;
                a1 += ((i1 * MainL) + 0x4000) >> 15;

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

                out.put(out_p + ptr, (short) o1);
                aux1.put(aux1_p + ptr, (short) a1);
                if (AuxIncRate != 0) {
                    a2 += ((i1 * AuxR) + 0x4000) >> 15;
                    a3 += ((i1 * AuxL) + 0x4000) >> 15;

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

                    aux2.put(aux2_p + ptr, (short) a2);
                    aux3.put(aux3_p + ptr, (short) a3);
                }
                ptr++;
            }
        }

        hleMixerWorkArea.putShort(0 + 2, Wet); // 0-1
        hleMixerWorkArea.putShort(4 + 2, Dry); // 2-3
        hleMixerWorkArea.putInt(8, LTrg); // 4-5
        hleMixerWorkArea.putInt(12, RTrg); // 6-7
        hleMixerWorkArea.putInt(16, LRamp); // 8-9 (hleMixerWorkArea is a 16bit pointer)
        hleMixerWorkArea.putInt(20, RRamp); // 10-11
        hleMixerWorkArea.putInt(24, LAdderEnd); // 12-13
        hleMixerWorkArea.putInt(28, RAdderEnd); // 14-15
        hleMixerWorkArea.putInt(32, LAdderStart); // 12-13
        hleMixerWorkArea.putInt(36, RAdderStart); // 14-15
        System.arraycopy(hleMixerWorkArea.array(), 0, rdram.array(), addy, 80);
    }

    static void SETVOL() {
        int flags = (inst1 >>> 16) & 0xFF;
        int vol = inst1 & 0xFFFF;
        int volrate = inst2 & 0xFFFF;

        if ((flags & A_AUX) != 0) {
            Env_Dry = (short) vol; // m_MainVol
            Env_Wet = (short) volrate; // m_AuxVol
            return;
        }

        if ((flags & A_VOL) != 0) { // Set the Source(start) Volumes
            if ((flags & A_LEFT) != 0) {
                Vol_Left = (short) vol; // m_LeftVolume
            } else { // A_RIGHT
                Vol_Right = (short) vol; // m_RightVolume
            }
            return;
        }

        if ((flags & A_LEFT) != 0) { // Set the Ramping values Target, Ramp
            VolTrg_Left = (short) inst1; // m_LeftVol
            VolRamp_Left = (int) inst2;
        } else { // A_RIGHT
            VolTrg_Right = (short) inst1; // m_RightVol
            VolRamp_Right = (int) inst2;
        }
    }

    static void MIXER() {
        int dmemin = inst2 >>> 16;
        int dmemout = inst2 & 0xFFFF;
        int gain = (short) (inst1 & 0xFFFF);
        int temp;

        if (AudioCount == 0) {
            return;
        }

        for (int x = 0; x < AudioCount; x += 2) {
            temp = (BufferSpace.getShort(dmemin + x) * gain) >> 15;
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
