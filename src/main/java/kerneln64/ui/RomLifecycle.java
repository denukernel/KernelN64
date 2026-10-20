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

package kerneln64.ui;
import kerneln64.hardware.Audio;
import kerneln64.cheats.CheatRepository;
import kerneln64.cpu.Cop0;
import kerneln64.cpu.Cop1;
import kerneln64.cpu.Cpu;
import kerneln64.debugger.CrashDiagnostics;
import kerneln64.memory.Dma;
import kerneln64.rom.GameSharkCartridge;
import kerneln64.memory.Memory;
import kerneln64.hardware.Pif;
import kerneln64.cpu.Registers;
import kerneln64.state.Rewind;
import kerneln64.rom.Rom;
import kerneln64.hardware.Rsp;
import kerneln64.state.SaveStates;
import kerneln64.cheats.Sm64Compatibility;
import kerneln64.cheats.Sm64DecompCodes;
import kerneln64.hardware.Video;
import kerneln64.Main;


import java.io.File;
import java.io.IOException;
import kerneln64.cpu.Cop0.TlbException;
import kerneln64.memory.Memory.MemoryException;
import kerneln64.savechips.Eeprom;
import kerneln64.savechips.FlashRam;
import kerneln64.savechips.Mempak;
import kerneln64.savechips.Sram;
import gln64j.rdp.Gdp;
import gln64j.rsp.Gsp;
import static kerneln64.Main.*;

/** ROM opening, hardware setup, and orderly emulation shutdown. Main owns the lifecycle lock. */
public final class RomLifecycle {
    private RomLifecycle() {}

	public static void CloseCpu()  {
		if (eeprom != null)
			eeprom.close();
		eeprom = null;
		if (mempak != null)
			mempak.close();
		mempak = null;
		if (sram != null)
			sram.close();
		sram = null;
        if (flashRam != null)
            flashRam.close();
		flashRam = null;
		if (video != null && video.gfxPlugin != null) {
			video.gfxPlugin.romClosed();
			video.gfxPlugin = null;
		}
		if (audio != null && audio.audioPlugin != null) {
			audio.audioPlugin.romClosed();
			audio.audioPlugin = null;
		}
		if (pif != null && pif.controllerPlugin != null) {
			pif.controllerPlugin.romClosed();
			pif.controllerPlugin = null;
		}
		if (rsp != null) rsp.shutdown();
	}

    private static void stopCpu() throws IOException {
        if (cpu == null) return;
        Rewind.held = false;
        cpu.running = false;
        if (cpu.emuThread == null || !cpu.emuThread.isAlive()) return;
        if (Thread.currentThread() == cpu.emuThread) throw new IOException("Stop emulation from a host worker thread.");
        java.util.concurrent.locks.LockSupport.unpark(cpu.emuThread);
        try {
            cpu.emuThread.join(5000);
            if (cpu.emuThread.isAlive()) throw new IOException("Emulation thread did not stop");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while stopping emulation", interrupted);
        }
    }
    public static void endEmulation() throws IOException {
        stopCpu();
        KeyboardRouting.release();
        CloseCpu();
        Rewind.clear();
        if (cpu != null) cpu.pauseState.clear();
        cheats.clear();
        currentRomFile = null;
        currentRomHash = null;
        decompCheats = null;
        decompCheatsRejected = sm64UsCheats = false;
        cheatProfile = Sm64Compatibility.NONE;
        setFastForward(false);
        EmulationStatus.reset();
        EmulationControl.refreshTitle();
        IdleScreen.show();
    }

	public static void loadRom(File romFile) throws IOException, TlbException {
		if (romFile == null || !romFile.exists()) {
            throw new IOException("ROM file not found: " + romFile);
		}

		// Let queued graphics/EDT work finish before replacing shared plugin state.
		stopCpu();
		CloseCpu(); // Properly shut down plugins and save chips
        Rewind.clear(); Rewind.held = false; CrashDiagnostics.reset();

		// 2. Load the actual ROM data
		Rom rom = new Rom(romFile);
		if (rom.data == null) {
			javax.swing.JOptionPane.showMessageDialog(hMainWindow,
					"Failed to load ROM data.\nThe file may be corrupted or in an unsupported format.", "Load Error",
					javax.swing.JOptionPane.ERROR_MESSAGE);
			return;
		}

        IdleScreen.hide();
        // A restart is a cold boot. ByteBuffer.clear() only resets its cursor;
        // it does not erase guest memory or reset CPU/device state.
        try { initializeHardware(GameSharkCartridge.isFirmware(rom.data)?8:0); }
        catch (MemoryException error) { throw new IOException("Cannot initialize emulation hardware", error); }

		lastRomDir = romFile.getAbsoluteFile().getParent();
		saveConfig();

		new File(AUTO_SAVE_DIR).mkdirs();
        String romName = romFile.getName().substring(0, romFile.getName().lastIndexOf('.'));
		String path = new File(AUTO_SAVE_DIR).getAbsolutePath() + "/" + romName;

		sram = new kerneln64.savechips.Sram(path + ".sra");
		mempak = new kerneln64.savechips.Mempak(path + ".mpk");
		flashRam = new kerneln64.savechips.FlashRam(path + ".fla");
		eeprom = new kerneln64.savechips.Eeprom(path + ".eep");

		pif.setSaveChips(eeprom, mempak);
		dma.setSaveChips(sram, flashRam);
		dma.connect(pif, rsp, rom.data);
		mem.map(cpu.CheckInterrupts, rsp, video, audio, pif, dma, flashRam, regs, rom.data);

		// 4. Reset processors with fresh Cop0
		Cop0 cop0 = new Cop0(Default_CountPerOp);
		cop0.useTlb(UseTlb);
		cop0.setTimerInterrupts(cpu.CheckInterrupts, pif.timerInterrupt, dma.timerInterrupt, video.timerInterrupt);
		video.setTimer(cop0);
		video.setFrameLimit(FrameLimit);
		video.resetTimer();
		mem.setTimer(cop0);

		cpu.connect(cop0, new Cop1());
		cpu.setOps(buildCpuOps(cpu, cop0, cpu.cop1));

		// 5. Initialize ROM and Plugins
		initialize(pif, rom, mem, cpu);

		video.setupPlugin(hMainWindow, config);
		audio.setupPlugin(hMainWindow, config);
		pif.setupPlugin(hMainWindow, config);
        rsp.setupPlugin(hMainWindow, config, video.gfxPlugin, audio.audioPlugin);
        cpu.cacheInstructions(CacheInstructions);
        currentRomFile = romFile;
        EmulationStatus.reset();
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (hMainWindow != null) hMainWindow.setTitle(EmulationStatus.windowTitle(romFile));
        });
        currentRomHash = SaveStates.romHash(rom.data);
        MainWindowVisibility.restore();
        CompatibilityDatabase.recordRom(romFile, currentRomHash);
        cheats.clear();
        decompCheats=null;decompCheatsRejected=false;
        cheatProfile=Sm64Compatibility.detect(rom.data);
        if(cheatProfile.family())try {
            decompCheats=Sm64DecompCodes.find(romFile,rom.data);
            if(decompCheats!=null)cheatProfile=new Sm64Compatibility.Profile(true,true,"SM64 decomp / matched ELF");
        }catch(IOException error){decompCheatsRejected=true;cheatProfile=new Sm64Compatibility.Profile(true,false,error.getMessage());System.err.println("SM64 cheats: "+error.getMessage());}
        try {
            sm64UsCheats=!decompCheatsRejected && Boolean.parseBoolean(config.getProperty("cheats.sm64."+currentRomHash,Boolean.toString(cheatProfile.usLayout())));
            cheats.setSm64Compatibility(sm64UsCheats);cheats.setSm64Build(decompCheats);
            var entries=CheatRepository.load(cheatFile().toPath(),sm64UsCheats);
            cheats.setCodes(CheatRepository.serialize(entries));
        }
        catch (IOException | IllegalArgumentException error) {
            javax.swing.JOptionPane.showMessageDialog(hMainWindow,
                    "Cheats were disabled: " + error.getMessage(), "GameShark", javax.swing.JOptionPane.ERROR_MESSAGE);
        }

		if (video.gfxPlugin != null)
			video.gfxPlugin.romOpen();
		if (pif.controllerPlugin != null)
			pif.controllerPlugin.romOpen();

		cpuStartTime = System.currentTimeMillis();
		cheats.apply(mem.RDRAM, cpu, true);
        EmulationControl.started();
        cpu.startEmulation();
        EmulationControl.refreshTitle();
        recentRoms.add(romFile);
        saveConfig();
		System.out.println("Started ROM: " + romFile.getName());
	}

	public static void initializeHardware() throws MemoryException {
        initializeHardware(0);
    }
    private static void initializeHardware(int minimumRamMb) throws MemoryException {
        Memory freshMemory = new Memory(Math.max(CoreOptions.ramMb,minimumRamMb) * 1024 * 1024);
        regs = new Registers();
        mem = freshMemory;
        cpu = new Cpu(regs, mem);
        rsp = new Rsp(cpu.CheckInterrupts, regs, mem.RDRAM, mem.DMEM, mem.IMEM);
        Rsp.gdp = new Gdp(cpu.CheckInterrupts, regs);
        Rsp.gsp = new Gsp(mem.RDRAM, mem.DMEM);
        video = new Video(cpu.CheckInterrupts, regs, mem.RDRAM, mem.DMEM, mem.IMEM);
        audio = new Audio(cpu.AiCheckInterrupts, mem.RDRAM, mem.DMEM, mem.IMEM);
        pif = new Pif(cpu.CheckInterrupts, regs);
        dma = new Dma(cpu.CheckInterrupts, regs, mem.RDRAM, mem.DMEM);
        pif.showPifRamErrors(ShowPifRamErrors);
        dma.showUnhandledMemory(ShowUnhandledMemory);
    }
}
