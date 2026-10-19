/**
 * Copyright 2005 Jason LaDere
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
 * Originally based on Project64 code.
 *
 */

package kerneln64.memory;
import kerneln64.hardware.Audio;
import kerneln64.savechips.FlashRam;
import kerneln64.rom.GameSharkCartridge;
import kerneln64.hardware.Pif;
import kerneln64.cpu.Registers;
import kerneln64.hardware.Rsp;
import kerneln64.hardware.Video;
import kerneln64.Main;


import plugin.AudioPlugin;
import plugin.GfxPlugin;
import static kerneln64.memory.Memory.*;

/** Writes to ROM and memory-mapped hardware registers. */
final class MemoryMappedWrites {
    private MemoryMappedWrites() {}

	static void sbNonMemory(Memory state, int pAddr, byte value) throws MemoryException {
		if(GameSharkCartridge.writeRegister(state.rom,pAddr&~3,value&255))return;
		switch (pAddr & 0xFFF00000) {
		case 0x00000000:
		case 0x00100000:
		case 0x00200000:
		case 0x00300000:
		case 0x00400000:
		case 0x00500000:
		case 0x00600000:
		case 0x00700000:
			throw new MemoryException("Illegal RAM Memory SB: " + Integer.toHexString(pAddr));
		default:
			state.bMem.put(pAddr, value);
		}
	}

	static void shNonMemory(Memory state, int pAddr, short value) throws MemoryException {
		if(GameSharkCartridge.writeRegister(state.rom,pAddr&~3,value&65535))return;
		switch (pAddr & 0xFFF00000) {
		case 0x00000000:
		case 0x00100000:
		case 0x00200000:
		case 0x00300000:
		case 0x00400000:
		case 0x00500000:
		case 0x00600000:
		case 0x00700000:
			throw new MemoryException("Illegal RAM Memory SH: " + Integer.toHexString(pAddr));
		default:
			state.bMem.put(pAddr, (byte) ((value >> 8) & 0xFFFF));
			state.bMem.put(pAddr + 1, (byte) ((value) & 0xFFFF));
		}
	}

	static void swNonMemory(Memory state, int pAddr, int value) throws MemoryException {
		if(GameSharkCartridge.writeRegister(state.rom,pAddr,value))return;
		if (pAddr >= 0x10000000 && pAddr < 0x16000000) {
			if ((pAddr - 0x10000000) < state.rom.capacity()) {
				state.writtenToRom = true;
				state.wroteToRom = value;
			} else {
				throw new MemoryException("Illegal ROM Memory SW: " + Integer.toHexString(pAddr));
			}
		}

		switch (pAddr & 0xFFF00000) {
		case 0x00000000:
		case 0x00100000:
		case 0x00200000:
		case 0x00300000:
		case 0x00400000:
		case 0x00500000:
		case 0x00600000:
		case 0x00700000:
			throw new MemoryException("Illegal RAM Memory SW: " + Integer.toHexString(pAddr));
		case 0x03F00000: // RDRAM Registers
			switch (pAddr) {
			case 0x03F00000:
				state.regRDRAM[RDRAM_CONFIG_REG] = value;
				break;
			case 0x03F00004:
				state.regRDRAM[RDRAM_DEVICE_ID_REG] = value;
				break;
			case 0x03F00008:
				state.regRDRAM[RDRAM_DELAY_REG] = value;
				break;
			case 0x03F0000C:
				state.regRDRAM[RDRAM_MODE_REG] = value;
				break;
			case 0x03F00010:
				state.regRDRAM[RDRAM_REF_INTERVAL_REG] = value;
				break;
			case 0x03F00014:
				state.regRDRAM[RDRAM_REF_ROW_REG] = value;
				break;
			case 0x03F00018:
				state.regRDRAM[RDRAM_RAS_INTERVAL_REG] = value;
				break;
			case 0x03F0001C:
				state.regRDRAM[RDRAM_MIN_INTERVAL_REG] = value;
				break;
			case 0x03F00020:
				state.regRDRAM[RDRAM_ADDR_SELECT_REG] = value;
				break;
			case 0x03F00024:
				state.regRDRAM[RDRAM_DEVICE_MANUF_REG] = value;
				break;
			case 0x03F04004:
				break;
			case 0x03F08004:
				break;
			case 0x03F80004:
				break;
			case 0x03F80008:
				break;
			case 0x03F8000C:
				break;
			case 0x03F80014:
				break;
			default:
				throw new MemoryException("Illegal RDRAM Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x04000000: // SP Registers (RSP)
			if (pAddr < 0x04002000) {
				throw new MemoryException("Illegal DMEM Memory SW: " + Integer.toHexString(pAddr));
			}
			switch (pAddr) {
			case 0x04040000:
				state.regSP[Rsp.SP_MEM_ADDR_REG] = value;
				break;
			case 0x04040004:
				state.regSP[Rsp.SP_DRAM_ADDR_REG] = value;
				break;
			case 0x04040008:
				state.regSP[Rsp.SP_RD_LEN_REG] = value;
				state.dma.spDmaRead();
				break;
			case 0x0404000C:
				state.regSP[Rsp.SP_WR_LEN_REG] = value;
				System.out.println("SP_DMA_WRITE");

				break;
			case 0x04040010:
				if ((value & Rsp.SP_CLR_HALT) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_HALT;
				}
				if ((value & Rsp.SP_SET_HALT) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_HALT;
				}
				if ((value & Rsp.SP_CLR_BROKE) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_BROKE;
				}
				if ((value & Rsp.SP_CLR_INTR) != 0) {
					state.regMI[Registers.MI_INTR_REG] &= ~Registers.MI_INTR_SP;
					state.checkInterrupts.run();
				}
				if ((value & Rsp.SP_SET_INTR) != 0)
					System.err.printf("SP_SET_INTR\n");
				if ((value & Rsp.SP_CLR_SSTEP) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SSTEP;
				}
				if ((value & Rsp.SP_SET_SSTEP) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SSTEP;
				}
				if ((value & Rsp.SP_CLR_INTR_BREAK) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_INTR_BREAK;
				}
				if ((value & Rsp.SP_SET_INTR_BREAK) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_INTR_BREAK;
				}
				if ((value & Rsp.SP_CLR_SIG0) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG0;
				}
				if ((value & Rsp.SP_SET_SIG0) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG0;
				}
				if ((value & Rsp.SP_CLR_SIG1) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG1;
				}
				if ((value & Rsp.SP_SET_SIG1) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG1;
				}
				if ((value & Rsp.SP_CLR_SIG2) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG2;
				}
				if ((value & Rsp.SP_SET_SIG2) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG2;
				}
				if ((value & Rsp.SP_CLR_SIG3) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG3;
				}
				if ((value & Rsp.SP_SET_SIG3) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG3;
				}
				if ((value & Rsp.SP_CLR_SIG4) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG4;
				}
				if ((value & Rsp.SP_SET_SIG4) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG4;
				}
				if ((value & Rsp.SP_CLR_SIG5) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG5;
				}
				if ((value & Rsp.SP_SET_SIG5) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG5;
				}
				if ((value & Rsp.SP_CLR_SIG6) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG6;
				}
				if ((value & Rsp.SP_SET_SIG6) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG6;
				}
				if ((value & Rsp.SP_CLR_SIG7) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] &= ~Rsp.SP_STATUS_SIG7;
				}
				if ((value & Rsp.SP_SET_SIG7) != 0) {
					state.regSP[Rsp.SP_STATUS_REG] |= Rsp.SP_STATUS_SIG7;
				}
				state.rsp.runRsp(state.DMEM.getInt(0xFC0));
				break;
			case 0x0404001C:
				state.regSP[Rsp.SP_SEMAPHORE_REG] = 0;
				break;
			case 0x04080000:
				state.regSP[Rsp.SP_PC_REG] = value & 0xFFC;
				break;
			default:
				throw new MemoryException("Illegal SP Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x04100000: // DP Command Registers (RDP)
			switch (pAddr) {
			case 0x04100000:
				state.regDPC[Registers.DPC_START_REG] = value;
				state.regDPC[Registers.DPC_CURRENT_REG] = value;
				break;
			case 0x04100004:
				state.regDPC[Registers.DPC_END_REG] = value;
				if (state.rsp.gfx != null) {
					state.rsp.gfx.processRDPList();
				}
				break;
			case 0x0410000C:
				if ((value & Registers.DPC_CLR_XBUS_DMEM_DMA) != 0) {
					state.regDPC[Registers.DPC_STATUS_REG] &= ~Registers.DPC_STATUS_XBUS_DMEM_DMA;
				}
				if ((value & Registers.DPC_SET_XBUS_DMEM_DMA) != 0) {
					state.regDPC[Registers.DPC_STATUS_REG] |= Registers.DPC_STATUS_XBUS_DMEM_DMA;
				}
				if ((value & Registers.DPC_CLR_FREEZE) != 0) {
					state.regDPC[Registers.DPC_STATUS_REG] &= ~Registers.DPC_STATUS_FREEZE;
				}
				if ((value & Registers.DPC_SET_FREEZE) != 0) {
					state.regDPC[Registers.DPC_STATUS_REG] |= Registers.DPC_STATUS_FREEZE;
				}
				if ((value & Registers.DPC_CLR_FLUSH) != 0) {
					state.regDPC[Registers.DPC_STATUS_REG] &= ~Registers.DPC_STATUS_FLUSH;
				}
				if ((value & Registers.DPC_SET_FLUSH) != 0) {
					state.regDPC[Registers.DPC_STATUS_REG] |= Registers.DPC_STATUS_FLUSH;
				}
				break;
			default:
				throw new MemoryException("Illegal DPC Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x04300000: // MIPS Interface (MI) Registers
			switch (pAddr) {
			case 0x04300000:
				state.regMI[Registers.MI_MODE_REG] &= ~0x7F;
				state.regMI[Registers.MI_MODE_REG] |= (value & 0x7F);
				if ((value & Registers.MI_CLR_INIT) != 0) {
					state.regMI[Registers.MI_MODE_REG] &= ~Registers.MI_MODE_INIT;
				}
				if ((value & Registers.MI_SET_INIT) != 0) {
					state.regMI[Registers.MI_MODE_REG] |= Registers.MI_MODE_INIT;
				}
				if ((value & Registers.MI_CLR_EBUS) != 0) {
					state.regMI[Registers.MI_MODE_REG] &= ~Registers.MI_MODE_EBUS;
				}
				if ((value & Registers.MI_SET_EBUS) != 0) {
					state.regMI[Registers.MI_MODE_REG] |= Registers.MI_MODE_EBUS;
				}
				if ((value & Registers.MI_CLR_DP_INTR) != 0) {
					state.regMI[Registers.MI_INTR_REG] &= ~Registers.MI_INTR_DP;
					state.checkInterrupts.run();
				}
				if ((value & Registers.MI_CLR_RDRAM) != 0) {
					state.regMI[Registers.MI_MODE_REG] &= ~Registers.MI_MODE_RDRAM;
				}
				if ((value & Registers.MI_SET_RDRAM) != 0) {
					state.regMI[Registers.MI_MODE_REG] |= Registers.MI_MODE_RDRAM;
				}
				break;
			case 0x0430000C:
				if ((value & Registers.MI_INTR_MASK_CLR_SP) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] &= ~Registers.MI_INTR_MASK_SP;
				}
				if ((value & Registers.MI_INTR_MASK_SET_SP) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] |= Registers.MI_INTR_MASK_SP;
				}
				if ((value & Registers.MI_INTR_MASK_CLR_SI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] &= ~Registers.MI_INTR_MASK_SI;
				}
				if ((value & Registers.MI_INTR_MASK_SET_SI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] |= Registers.MI_INTR_MASK_SI;
				}
				if ((value & Registers.MI_INTR_MASK_CLR_AI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] &= ~Registers.MI_INTR_MASK_AI;
				}
				if ((value & Registers.MI_INTR_MASK_SET_AI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] |= Registers.MI_INTR_MASK_AI;
				}
				if ((value & Registers.MI_INTR_MASK_CLR_VI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] &= ~Registers.MI_INTR_MASK_VI;
				}
				if ((value & Registers.MI_INTR_MASK_SET_VI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] |= Registers.MI_INTR_MASK_VI;
				}
				if ((value & Registers.MI_INTR_MASK_CLR_PI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] &= ~Registers.MI_INTR_MASK_PI;
				}
				if ((value & Registers.MI_INTR_MASK_SET_PI) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] |= Registers.MI_INTR_MASK_PI;
				}
				if ((value & Registers.MI_INTR_MASK_CLR_DP) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] &= ~Registers.MI_INTR_MASK_DP;
				}
				if ((value & Registers.MI_INTR_MASK_SET_DP) != 0) {
					state.regMI[Registers.MI_INTR_MASK_REG] |= Registers.MI_INTR_MASK_DP;
				}
				break;
			default:
				throw new MemoryException("Illegal MI Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x04400000: // Video Interface (VI) Registers
			switch (pAddr) {
			case 0x04400000:
				if (state.regVI[GfxPlugin.VI_STATUS_REG] != value) {
					state.regVI[GfxPlugin.VI_STATUS_REG] = value;
					if (state.video.gfxPlugin != null)
						state.video.gfxPlugin.viStatusChanged();
				}
				break;
			case 0x04400004:
				state.regVI[GfxPlugin.VI_ORIGIN_REG] = (value & 0xFFFFFF);
				break;
			case 0x04400008:
				if (state.regVI[GfxPlugin.VI_WIDTH_REG] != value) {
					state.regVI[GfxPlugin.VI_WIDTH_REG] = value;
					if (state.video.gfxPlugin != null)
						state.video.gfxPlugin.viWidthChanged();
				}
				break;
			case 0x0440000C:
				state.regVI[GfxPlugin.VI_INTR_REG] = value;
				break;
			case 0x04400010:
				state.regMI[Registers.MI_INTR_REG] &= ~Registers.MI_INTR_VI;
				state.checkInterrupts.run();
				break;
			case 0x04400014:
				state.regVI[GfxPlugin.VI_BURST_REG] = value;
				break;
			case 0x04400018:
				state.regVI[GfxPlugin.VI_V_SYNC_REG] = value;
				break;
			case 0x0440001C:
				state.regVI[GfxPlugin.VI_H_SYNC_REG] = value;
				break;
			case 0x04400020:
				state.regVI[GfxPlugin.VI_LEAP_REG] = value;
				break;
			case 0x04400024:
				state.regVI[GfxPlugin.VI_H_START_REG] = value;
				break;
			case 0x04400028:
				state.regVI[GfxPlugin.VI_V_START_REG] = value;
				break;
			case 0x0440002C:
				state.regVI[GfxPlugin.VI_V_BURST_REG] = value;
				break;
			case 0x04400030:
				state.regVI[GfxPlugin.VI_X_SCALE_REG] = value;
				break;
			case 0x04400034:
				state.regVI[GfxPlugin.VI_Y_SCALE_REG] = value;
				break;
			default:
				throw new MemoryException("Illegal VI Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x04500000: // Audio Interface (AI) Registers
//                long time = System.currentTimeMillis();
			switch (pAddr) {
			case 0x04500000:
				state.regAI[AudioPlugin.AI_DRAM_ADDR_REG] = value;
				break;
			case 0x04500004:
				state.regAI[AudioPlugin.AI_LEN_REG] = value;
				long time = System.currentTimeMillis();
				if (state.audio.audioPlugin != null)
					state.audio.audioPlugin.aiLenChanged();
				state.millisInAudio += (System.currentTimeMillis() - time);
				break;
			case 0x04500008:
				state.regAI[AudioPlugin.AI_CONTROL_REG] = (value & 1);
				break;
			case 0x0450000C:
				/* Clear Interrupt */;
				state.audioIntrReg[0] &= ~Registers.MI_INTR_AI;
				state.regMI[Registers.MI_INTR_REG] &= ~Registers.MI_INTR_AI;
				state.checkInterrupts.run();
				break;
			case 0x04500010:
				state.regAI[AudioPlugin.AI_DACRATE_REG] = value;
				if (state.audio.audioPlugin != null)
					state.audio.audioPlugin.aiDacrateChanged(state.audio.audioPlugin.SYSTEM_NTSC);
				break;
			case 0x04500014:
				state.regAI[AudioPlugin.AI_BITRATE_REG] = value;
				break;
			default:
				throw new MemoryException("Illegal AI Register SW: " + Integer.toHexString(pAddr));
			}
//                millisInAudio += (System.currentTimeMillis() - time);
			break;
		case 0x04600000: // Peripheral Interface (PI) Registers
			switch (pAddr) {
			case 0x04600000:
				state.regPI[Dma.PI_DRAM_ADDR_REG] = value;
				break;
			case 0x04600004:
				state.regPI[Dma.PI_CART_ADDR_REG] = value;
				break;
			case 0x04600008:
				state.regPI[Dma.PI_RD_LEN_REG] = value;
				state.dma.piDmaRead();
				break;
			case 0x0460000C:
				state.regPI[Dma.PI_WR_LEN_REG] = value;
				state.dma.piDmaWrite();
				break;
			case 0x04600010:
				if ((value & Dma.PI_CLR_INTR) != 0) {
					state.regMI[Registers.MI_INTR_REG] &= ~Registers.MI_INTR_PI;
					state.checkInterrupts.run();
				}
				break;
			case 0x04600014:
				state.regPI[Dma.PI_DOMAIN1_REG] = (value & 0xFF);
				break;
			case 0x04600018:
				state.regPI[Dma.PI_BSD_DOM1_PWD_REG] = (value & 0xFF);
				break;
			case 0x0460001C:
				state.regPI[Dma.PI_BSD_DOM1_PGS_REG] = (value & 0xFF);
				break;
			case 0x04600020:
				state.regPI[Dma.PI_BSD_DOM1_RLS_REG] = (value & 0xFF);
				break;
			// NEW
			case 0x04600024:
				state.regPI[Dma.PI_DOMAIN2_REG] = (value & 0xFF);
				break;
			case 0x04600028:
				state.regPI[Dma.PI_BSD_DOM2_PWD_REG] = (value & 0xFF);
				break;
			case 0x0460002C:
				state.regPI[Dma.PI_BSD_DOM2_PGS_REG] = (value & 0xFF);
				break;
			case 0x04600030:
				state.regPI[Dma.PI_BSD_DOM2_RLS_REG] = (value & 0xFF);
				break;
			default:
				throw new MemoryException("Illegal PI Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x04700000: // RDRAM Interface (RI) Registers
			switch (pAddr) {
			case 0x04700000:
				state.regRI[Memory.RI_MODE_REG] = value;
				break;
			case 0x04700004:
				state.regRI[Memory.RI_CONFIG_REG] = value;
				break;
			case 0x04700008:
				state.regRI[Memory.RI_CURRENT_LOAD_REG] = value;
				break;
			case 0x0470000C:
				state.regRI[Memory.RI_SELECT_REG] = value;
				break;
			case 0x04700010:
				state.regRI[Memory.RI_REFRESH_REG] = value;
				break;
			case 0x04700014:
				state.regRI[Memory.RI_LATENCY_REG] = value;
				break;
			case 0x04700018:
				state.regRI[Memory.RI_RERROR_REG] = value;
				break;
			case 0x0470001C:
				state.regRI[Memory.RI_WERROR_REG] = value;
				break;
			default:
				throw new MemoryException("Illegal RI Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x04800000: // Serial Interface (SI) Registers
			switch (pAddr) {
			case 0x04800000:
				state.regSI[Pif.SI_DRAM_ADDR_REG] = value;
				break;
			case 0x04800004:
				state.regSI[Pif.SI_PIF_ADDR_RD64B_REG] = value;
				state.dma.siDmaRead();
				break;
			case 0x04800010:
				state.regSI[Pif.SI_PIF_ADDR_WR64B_REG] = value;
				state.dma.siDmaWrite();
				break;
			case 0x04800018:
				state.regSI[Pif.SI_STATUS_REG] &= ~Pif.SI_STATUS_INTERRUPT;
				state.regMI[Registers.MI_INTR_REG] &= ~Registers.MI_INTR_SI;
				// regSI[Pif.SI_STATUS_REG] &= ~Pif.SI_STATUS_INTERRUPT;
				state.checkInterrupts.run();
				break;
			default:
				throw new MemoryException("Illegal SI Register SW: " + Integer.toHexString(pAddr));
			}
			break;
		case 0x08000000: // Cartridge Domain 2 Address 2
			if (pAddr != 0x08010000)
				throw new MemoryException("Illegal FlashRam SW: " + Integer.toHexString(pAddr));
			if (state.regs.saveUsing == Registers.AUTO)
				state.regs.saveUsing = Registers.FLASHRAM;
			if (state.regs.saveUsing != Registers.FLASHRAM)
				return;
			state.flashRam.writeToFlashCommand(value);
			break;
		case 0x1FC00000: // PIF
			if (pAddr < 0x1FC007C0) { // PIF Boot rom
				throw new MemoryException("Illegal PifRam SW: " + Integer.toHexString(pAddr));
			} else if (pAddr < 0x1FC00800) { // PIF RAM
				state.pif.pifRam.putInt(pAddr - 0x1FC007C0, value);
				if (pAddr == 0x1FC007FC) {
					state.pif.pifRamWrite();
				}
				return;
			}
			throw new MemoryException("Illegal PifRam Register SW: " + Integer.toHexString(pAddr));
		default:
			state.bMem.put(pAddr, (byte) ((value >> 24) & 0xFFFF));
			state.bMem.put(pAddr + 1, (byte) ((value >> 16) & 0xFFFF));
			state.bMem.put(pAddr + 2, (byte) ((value >> 8) & 0xFFFF));
			state.bMem.put(pAddr + 3, (byte) ((value) & 0xFFFF));
		}
	}
}
