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
import kerneln64.rom.GameSharkCartridge;
import kerneln64.hardware.Pif;
import kerneln64.cpu.Registers;
import kerneln64.hardware.Rsp;
import kerneln64.Main;


import plugin.AudioPlugin;
import plugin.GfxPlugin;
import static kerneln64.memory.Memory.*;

/** Reads from ROM and memory-mapped hardware registers. */
final class MemoryMappedReads {
    private MemoryMappedReads() {}

	static byte lbNonMemory(Memory state, int pAddr) throws MemoryException {
		Integer cartridge=GameSharkCartridge.readRegister(state.rom,pAddr&~3);
		if(cartridge!=null)return (byte)(cartridge>>((3-(pAddr&3))*8));
		if (pAddr >= 0x10000000 && pAddr < 0x16000000 || GameSharkCartridge.flashWindow(state.rom,pAddr)) {
			if (state.writtenToRom)
				throw new MemoryException("Illegal LB: writtenToRom");
			int offset=GameSharkCartridge.romOffset(state.rom,pAddr);
			if (offset >= 0 && offset < state.rom.capacity())
				return state.rom.get(offset);
			else
				return 0;
		}

		switch (pAddr & 0xFFF00000) {
		default:
			Byte b = (Byte) state.bMem.get(pAddr);
			return ((b == null) ? 0 : b);
		}
	}

	static short lhNonMemory(Memory state, int pAddr) throws MemoryException {
		Integer cartridge=GameSharkCartridge.readRegister(state.rom,pAddr&~3);
		if(cartridge!=null)return (short)(cartridge>>((2-(pAddr&2))*8));
		if(pAddr>=0x10000000 && pAddr<0x16000000 || GameSharkCartridge.flashWindow(state.rom,pAddr)) {
			int offset=GameSharkCartridge.romOffset(state.rom,pAddr);
			return offset>=0 && offset<=state.rom.capacity()-2?state.rom.getShort(offset):0;
		}
		switch (pAddr & 0xFFF00000) {
		default:
			Byte bb1 = (Byte) state.bMem.get(pAddr);
			int b1 = ((bb1 == null) ? 0 : bb1) & 0xFF;
			Byte bb2 = (Byte) state.bMem.get(pAddr + 1);
			int b2 = ((bb2 == null) ? 0 : bb2) & 0xFF;
			return (short) ((b1 << 8) | b2);
		}
	}

	static int lwNonMemory(Memory state, int pAddr) throws MemoryException {
		Integer cartridge=GameSharkCartridge.readRegister(state.rom,pAddr);
		if(cartridge!=null)return cartridge;
		if (pAddr >= 0x10000000 && pAddr < 0x16000000 || GameSharkCartridge.flashWindow(state.rom,pAddr)) {
			if (state.writtenToRom) {
				state.writtenToRom = false;
				return state.wroteToRom;
			}
			int offset=GameSharkCartridge.romOffset(state.rom,pAddr);
			if (offset >= 0 && offset <= state.rom.capacity()-4)
				return state.rom.getInt(offset);
			else
				return ((pAddr & 0xFFFF) << 16) | (pAddr & 0xFFFF);
		}

		switch (pAddr & 0xFFF00000) {
		case 0x03F00000:
			switch (pAddr) {
			case 0x03F00000:
				return state.regRDRAM[RDRAM_CONFIG_REG];
			case 0x03F00004:
				return state.regRDRAM[RDRAM_DEVICE_ID_REG];
			case 0x03F00008:
				return state.regRDRAM[RDRAM_DELAY_REG];
			case 0x03F0000C:
				return state.regRDRAM[RDRAM_MODE_REG];
			case 0x03F00010:
				return state.regRDRAM[RDRAM_REF_INTERVAL_REG];
			case 0x03F00014:
				return state.regRDRAM[RDRAM_REF_ROW_REG];
			case 0x03F00018:
				return state.regRDRAM[RDRAM_RAS_INTERVAL_REG];
			case 0x03F0001C:
				return state.regRDRAM[RDRAM_MIN_INTERVAL_REG];
			case 0x03F00020:
				return state.regRDRAM[RDRAM_ADDR_SELECT_REG];
			case 0x03F00024:
				return state.regRDRAM[RDRAM_DEVICE_MANUF_REG];
			default:
				throw new MemoryException("Illegal RDRAM Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04000000:
			switch (pAddr) {
			case 0x04040010:
				return state.regSP[Rsp.SP_STATUS_REG];
			case 0x04040014:
				return state.regSP[Rsp.SP_DMA_FULL_REG];
			case 0x04040018:
				return state.regSP[Rsp.SP_DMA_BUSY_REG];
			case 0x04080000:
				return state.regSP[Rsp.SP_PC_REG];
			default:
				throw new MemoryException("Illegal SP Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04100000:
			switch (pAddr) {
			case 0x0410000C:
				return state.regDPC[Registers.DPC_STATUS_REG];
			case 0x04100010:
				return state.regDPC[Registers.DPC_CLOCK_REG];
			case 0x04100014:
				return state.regDPC[Registers.DPC_BUFBUSY_REG];
			case 0x04100018:
				return state.regDPC[Registers.DPC_PIPEBUSY_REG];
			case 0x0410001C:
				return state.regDPC[Registers.DPC_TMEM_REG];
			default:
				throw new MemoryException("Illegal DPC Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04300000:
			switch (pAddr) {
			case 0x04300000:
				return state.regMI[Registers.MI_MODE_REG];
			case 0x04300004:
				return state.regMI[Registers.MI_VERSION_REG];
			case 0x04300008:
				return state.regMI[Registers.MI_INTR_REG];
			case 0x0430000C:
				return state.regMI[Registers.MI_INTR_MASK_REG];
			default:
				throw new MemoryException("Illegal MI Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04400000:
			switch (pAddr) {
			case 0x04400000:
				return state.regVI[GfxPlugin.VI_STATUS_REG];
			case 0x04400004:
				return state.regVI[GfxPlugin.VI_ORIGIN_REG];
			case 0x04400008:
				return state.regVI[GfxPlugin.VI_WIDTH_REG];
			case 0x0440000C:
				return state.regVI[GfxPlugin.VI_INTR_REG];
			case 0x04400010:
				if (state.video.gfxPlugin != null) {
					state.cop0.update();

					return state.video.updateCurrentHalfLine(state.cop0.timer);
				}

			case 0x04400014:
				return state.regVI[GfxPlugin.VI_BURST_REG];
			case 0x04400018:
				return state.regVI[GfxPlugin.VI_V_SYNC_REG];
			case 0x0440001C:
				return state.regVI[GfxPlugin.VI_H_SYNC_REG];
			case 0x04400020:
				return state.regVI[GfxPlugin.VI_LEAP_REG];
			case 0x04400024:
				return state.regVI[GfxPlugin.VI_H_START_REG];
			case 0x04400028:
				return state.regVI[GfxPlugin.VI_V_START_REG];
			case 0x0440002C:
				return state.regVI[GfxPlugin.VI_V_BURST_REG];
			case 0x04400030:
				return state.regVI[GfxPlugin.VI_X_SCALE_REG];
			case 0x04400034:
				return state.regVI[GfxPlugin.VI_Y_SCALE_REG];
			default:
				throw new MemoryException("Illegal VI Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04500000:
			switch (pAddr) {
			case 0x04500004:
				if (state.audio.audioPlugin != null)
					return state.audio.audioPlugin.aiReadLength();
				else
					return 0;
			case 0x0450000C:
				return state.regAI[AudioPlugin.AI_STATUS_REG];
			default:
				throw new MemoryException("Illegal AI Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04600000:
			switch (pAddr) {
			case 0x04600010:
				return state.regPI[Dma.PI_STATUS_REG];
			case 0x04600014:
				return state.regPI[Dma.PI_DOMAIN1_REG];
			case 0x04600018:
				return state.regPI[Dma.PI_BSD_DOM1_PWD_REG];
			case 0x0460001C:
				return state.regPI[Dma.PI_BSD_DOM1_PGS_REG];
			case 0x04600020:
				return state.regPI[Dma.PI_BSD_DOM1_RLS_REG];
			case 0x04600024:
				return state.regPI[Dma.PI_DOMAIN2_REG];
			case 0x04600028:
				return state.regPI[Dma.PI_BSD_DOM2_PWD_REG];
			case 0x0460002C:
				return state.regPI[Dma.PI_BSD_DOM2_PGS_REG];
			case 0x04600030:
				return state.regPI[Dma.PI_BSD_DOM2_RLS_REG];
			default:
				throw new MemoryException("Illegal PI Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04700000:
			switch (pAddr) {
			case 0x04700000:
				return state.regRI[Memory.RI_MODE_REG];
			case 0x04700004:
				return state.regRI[Memory.RI_CONFIG_REG];
			case 0x04700008:
				return state.regRI[Memory.RI_CURRENT_LOAD_REG];
			case 0x0470000C:
				return state.regRI[Memory.RI_SELECT_REG];
			case 0x04700010:
				return state.regRI[Memory.RI_REFRESH_REG];
			case 0x04700014:
				return state.regRI[Memory.RI_LATENCY_REG];
			case 0x04700018:
				return state.regRI[Memory.RI_RERROR_REG];
			case 0x0470001C:
				return state.regRI[Memory.RI_WERROR_REG];
			default:
				throw new MemoryException("Illegal RI Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x04800000:
			switch (pAddr) {
			case 0x04800014:
				// Datel's SI busy helper polls this reserved offset. Keep the
				// compatibility alias confined to recognized firmware images.
				if(GameSharkCartridge.isFirmware(state.rom))return state.regSI[Pif.SI_STATUS_REG];
				throw new MemoryException("Illegal SI Register LW: " + Integer.toHexString(pAddr));
			case 0x04800018:
				return state.regSI[Pif.SI_STATUS_REG];
			default:
				throw new MemoryException("Illegal SI Register LW: " + Integer.toHexString(pAddr));
			}
		case 0x05000000:
			return ((pAddr & 0xFFFF) << 16) | (pAddr & 0xFFFF);
		case 0x08000000:
			if (state.regs.saveUsing == Registers.AUTO)
				state.regs.saveUsing = Registers.FLASHRAM;
			if (state.regs.saveUsing != Registers.FLASHRAM)
				return ((pAddr & 0xFFFF) << 16) | (pAddr & 0xFFFF);
			return state.flashRam.readFromFlashStatus(pAddr);
		case 0x1FC00000:
			if (pAddr < 0x1FC007C0) {
				return (state.pif.pifRom[pAddr - 0x1FC00000] << 24) | (state.pif.pifRom[pAddr - 0x1FC00000 + 1] << 16)
						| (state.pif.pifRom[pAddr - 0x1FC00000 + 2] << 8) | (state.pif.pifRom[pAddr - 0x1FC00000 + 3]);
			} else if (pAddr < 0x1FC00800) {
				return state.pif.pifRam.getInt(pAddr - 0x1FC007C0);
			} else {
				throw new MemoryException("Illegal PifRam LW: " + Integer.toHexString(pAddr));
			}
		default:
			Byte bb1 = (Byte) state.bMem.get(pAddr);
			int b1 = ((bb1 == null) ? 0 : bb1) & 0xFF;
			Byte bb2 = (Byte) state.bMem.get(pAddr + 1);
			int b2 = ((bb2 == null) ? 0 : bb2) & 0xFF;
			Byte bb3 = (Byte) state.bMem.get(pAddr + 2);
			int b3 = ((bb3 == null) ? 0 : bb3) & 0xFF;
			Byte bb4 = (Byte) state.bMem.get(pAddr + 3);
			int b4 = ((bb4 == null) ? 0 : bb4) & 0xFF;
			return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
		}
	}
}
