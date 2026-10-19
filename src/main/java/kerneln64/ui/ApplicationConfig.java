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
import kerneln64.state.Rewind;
import kerneln64.Main;


import static kerneln64.Main.*;

/** Application configuration persistence. */
public final class ApplicationConfig {
    private ApplicationConfig() {}

	public static void loadConfig()  {

		try {
			if (CONFIG_FILE.exists()) {
				config.load(new java.io.FileInputStream(CONFIG_FILE));
				lastRomDir = config.getProperty("last.rom.dir", ".");
				UseTlb = Boolean.parseBoolean(config.getProperty("use.tlb", "true"));
				CacheInstructions = Boolean.parseBoolean(config.getProperty("use.cache", "false"));
				UseAudio = Boolean.parseBoolean(config.getProperty("use.audio", "true"));
				SpeedUp = Boolean.parseBoolean(config.getProperty("speed.up", "false"));
                CoreOptions.load(config);
                EmulationStatus.loadSettings(config);
                recentRoms.load(config);
                Rewind.setEnabled(Boolean.parseBoolean(config.getProperty("rewind.enabled", "false")));
                PluginSettings.migrate(config);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

        FastForwardOptions.load(config);
		// Keep states in sync - ALWAYS start at normal speed for stability
		isFastForward = false;
		SpeedMultiplier = 1.0f;
		FrameLimit = true;
	}

	public static void saveConfig()  {
		try {
			config.setProperty("last.rom.dir", lastRomDir);
			config.setProperty("use.tlb", Boolean.toString(UseTlb));
			config.setProperty("use.cache", Boolean.toString(CacheInstructions));
			config.setProperty("use.audio", Boolean.toString(UseAudio));
			config.remove("frame.limit");
            CoreOptions.save(config);
            FastForwardOptions.save(config);
            EmulationStatus.saveSettings(config);
            recentRoms.save(config);
            config.setProperty("rewind.enabled", Boolean.toString(Rewind.enabled));
			config.setProperty("speed.up", Boolean.toString(SpeedUp));

			config.store(new java.io.FileOutputStream(CONFIG_FILE), "KernelN64 Settings");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
