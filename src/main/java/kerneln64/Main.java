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


/*
 * Project 64 - A Nintendo 64 emulator.
 *
 * (c) Copyright 2001 zilmar (zilmar@emulation64.com) and
 * Jabo (jabo@emulation64.com).
 *
 * pj64 homepage: www.pj64.net
 *
 * Permission to use, copy, modify and distribute Project64 in both binary and
 * source form, for non-commercial purposes, is hereby granted without fee,
 * providing that this license information and copyright notice appear with
 * all copies and any derived work.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event shall the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Project64 is freeware for PERSONAL USE only. Commercial users should
 * seek permission of the copyright holders first. Commercial use includes
 * charging money for Project64 or software derived from Project64.
 *
 * The copyright holders request that bug fixes and improvements to the code
 * should be forwarded to them so if they want them.
 *
 */

package kerneln64;
import kerneln64.ui.ApplicationConfig;
import kerneln64.ui.ApplicationStartup;
import kerneln64.hardware.Audio;
import kerneln64.cheats.CheatEngine;
import kerneln64.cpu.Cop0;
import kerneln64.cpu.Cop1;
import kerneln64.cpu.Cpu;
import kerneln64.cpu.CpuOpcodeTable;
import kerneln64.memory.Dma;
import kerneln64.ui.EmulationStatus;
import kerneln64.ui.FastForwardOptions;
import kerneln64.memory.Memory;
import kerneln64.hardware.Pif;
import kerneln64.ui.RecentRoms;
import kerneln64.cpu.Registers;
import kerneln64.state.Rewind;
import kerneln64.rom.Rom;
import kerneln64.rom.RomBoot;
import kerneln64.ui.RomLifecycle;
import kerneln64.hardware.Rsp;
import kerneln64.cheats.Sm64Compatibility;
import kerneln64.cheats.Sm64DecompCodes;
import kerneln64.state.StateSlots;
import kerneln64.hardware.Video;
import kerneln64.Main;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import kerneln64.cpu.Cop0.TlbException;
import kerneln64.memory.Memory.MemoryException;
import kerneln64.savechips.Eeprom;
import kerneln64.savechips.FlashRam;
import kerneln64.savechips.Mempak;
import kerneln64.savechips.Sram;
import plugin.AudioPlugin;
import plugin.GfxPlugin;
import kerneln64.debugger.trace.Debug;
import java.io.InputStream;
import java.net.URL;
import resources.ResourceAnchor;
import gln64j.rdp.Gdp;
import gln64j.rsp.Gsp;


public class Main {

	public static final boolean DEBUG = false;

	public static final boolean Default_ShowUnhandledMemory = true; // false;
	public static final boolean Default_ShowPifRamErrors = true; // false;
	public static final boolean Default_ShowTLBMisses = false;
	public static final boolean Default_UseTlb = true;
	public static final boolean Default_CacheInstructions = false;
	public static final boolean Default_UseAudio = true;
	public static final boolean Default_FrameLimit = true;

	public static final String AUTO_SAVE_DIR = "./Save/";
	public static final File CONFIG_FILE = new File("./config.properties");
	public static final Properties config = new Properties();
	public static volatile float SpeedMultiplier = 1.0f;
	public static boolean ShowCpuUsage = true;
    public static EmulationStatus emulationStatus;
    public static final RecentRoms recentRoms = new RecentRoms();

	public static String lastRomDir = ".";
	public static KeyListener globalKeyListener;
	public static volatile boolean isFastForward = false;
	public static boolean f4Held = false; // Dedicated guard for F4
    public static boolean shiftHeld, shiftAudio;
    public static void releaseShift() {
        if(!shiftHeld)return;
        shiftHeld=false;setFastForward(isFastForward);UseAudio=shiftAudio;
    }
    public static void releaseTemporaryKeys() { Rewind.held=false;f4Held=false;releaseShift(); }

	public static final int R4300I_OPCODE = 0;
	public static final int R4300I_SPECIAL = 1;
	public static final int R4300I_REGIMM = 2;
	public static final int R4300I_COP0 = 3;
	public static final int R4300I_COP0_FUNCTION = 4;
	public static final int R4300I_COP1 = 5;
	public static final int R4300I_COP1_BC = 6;
	public static final int R4300I_COP1_S = 7;
	public static final int R4300I_COP1_D = 8;
	public static final int R4300I_COP1_W = 9;
	public static final int R4300I_COP1_L = 10;

	public static final int Default_CountPerOp = 2;

	public static Sram sram;
	public static Mempak mempak;
	public static FlashRam flashRam;
	public static Eeprom eeprom;

	// used by Registers, Memory, Dma
	public static Registers regs;
	public static Memory mem;
	public static Pif pif;
	public static Dma dma;

	public static Rsp rsp;
	public static Video video;
	public static Audio audio;

	public static Cpu cpu;
    public static final CheatEngine cheats = new CheatEngine();
    public static boolean sm64UsCheats;
    public static Sm64DecompCodes decompCheats;
    public static boolean decompCheatsRejected;
    public static Sm64Compatibility.Profile cheatProfile=Sm64Compatibility.NONE;
    public static File currentRomFile;
    public static String currentRomHash;


	// used by Dma
	public static boolean ShowUnhandledMemory = Default_ShowUnhandledMemory;

	// used by InterpreterOps
	public static boolean ShowTLBMisses = Default_ShowTLBMisses;

	// used by InterpreterOps, Cpu
	public static boolean UseTlb = Default_UseTlb;
	public static boolean CacheInstructions = Default_CacheInstructions;

	// used by Pif, Eeprom
	public static boolean ShowPifRamErrors = Default_ShowPifRamErrors;

	public static boolean UseAudio = Default_UseAudio;
	public static boolean FrameLimit = Default_FrameLimit;
	public static boolean SpeedUp = false;

	public static long cpuStartTime;
	public static JFrame hMainWindow;
	public static JPanel panel;

	/** Creates a new instance of Main */
	public Main() {
		globalKeyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {}
			@Override
			public void keyTyped(KeyEvent e) {}
		};
	}

	// called by InterpreterOps, Cpu
	public static void exit(String msg) {
        if (cpu != null) cpu.running = false;
        Rewind.held = false;
		CloseCpu();
		long endTime = System.currentTimeMillis();
		float totalTime = endTime - cpuStartTime;
		long millisInCpu = (endTime - cpuStartTime) - video.millisInGfx - rsp.millisInDlist - rsp.millisInAlist
				- mem.millisInAudio;

		System.out.println("DLIST %: " + ((rsp.millisInDlist / totalTime) * 100f));
		System.out.println("ALIST %: " + ((rsp.millisInAlist / totalTime) * 100f));
		System.out.println("GFX   %: " + ((video.millisInGfx / totalTime) * 100f));
		System.out.println("SND   %: " + ((mem.millisInAudio / totalTime) * 100f));
		System.out.println("CPU   %: " + ((millisInCpu / totalTime) * 100f));
		System.out.printf("%s\n", msg);
		Debug.close();
		System.exit(0);
	}

	public static void loadConfig() {
		ApplicationConfig.loadConfig();
	}

	public static void saveConfig() {
		ApplicationConfig.saveConfig();
	}

    public static String pluginSetting(String key, String fallback) {
        return config.getProperty(key, fallback);
    }

    public static void setPluginSetting(String key, String value) {
        config.setProperty(key, value);
    }

	public static void CloseCpu() {
		RomLifecycle.CloseCpu();
	}

	public static synchronized void loadRom(File romFile) throws IOException, TlbException {
		RomLifecycle.loadRom(romFile);
	}

    public static void setFastForward(boolean enabled) {
        isFastForward = enabled;
        SpeedMultiplier = shiftHeld ? 4.0f : enabled ? FastForwardOptions.multiplier() : 1.0f;
        FrameLimit = true;
        if (video != null) {
            video.setFrameLimit(true);
            video.resetTimer();
        }
    }

    public static synchronized void restartEmulation() throws IOException, TlbException {
        if (currentRomFile == null) throw new IOException("Open a ROM first.");
        loadRom(currentRomFile);
    }

	public static void initializeHardware() throws MemoryException {
		RomLifecycle.initializeHardware();
	}

	public static Runnable[][] buildCpuOps(Cpu cpu, Cop0 cop0, Cop1 cop1) {
		return CpuOpcodeTable.buildCpuOps(cpu, cop0, cop1);
	}

	public static void main(String[] args) {
		ApplicationStartup.main(args);
	}

    public static File cheatFile() {
        return new File(AUTO_SAVE_DIR, currentRomFile.getName() + ".cheats.txt");
    }

    public static File stateFile() {
        try {StateSlots slots=StateSlots.current();return slots.path(slots.selected()).toFile();}
        catch(IOException error){throw new java.io.UncheckedIOException(error);}
    }

	public static void openRom() throws IOException, TlbException {
		JFileChooser fileChooser = new JFileChooser(Main.lastRomDir);
		fileChooser.setDialogTitle("Open ROM");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
				"Nintendo 64 ROMs (*.z64, *.n64, *.v64)", "z64", "n64", "v64"));

		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			File selected = fileChooser.getSelectedFile();
			if (selected != null)
				loadRom(selected);
		}
	}

	public static void initialize(Pif pif, Rom rom, Memory mem, Cpu cpu) {
		RomBoot.initialize(pif, rom, mem, cpu);
	}

}

