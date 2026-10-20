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
import kerneln64.state.SaveStates;
import kerneln64.Main;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import kerneln64.memory.Memory.MemoryException;
import kerneln64.debugger.trace.Debug;
import java.io.InputStream;
import java.net.URL;
import resources.ResourceAnchor;
import static kerneln64.Main.*;

/** Desktop application startup and command-line ROM loading. */
public final class ApplicationStartup {
    private ApplicationStartup() {}

	public static void main(String[] args)  {
        try {
            LaunchArguments arguments = LaunchArguments.parse(args);
            if (arguments.help()) { System.out.println(LaunchArguments.HELP); return; }
            if(!SingleInstance.start(arguments))return;
        } catch (IllegalArgumentException error) { System.err.println(error.getMessage()); System.err.println(LaunchArguments.HELP); System.exit(2); return; }
        catch(IOException error){System.err.println(error.getMessage());return;}

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Debug.init();
		loadConfig();

		int WindowWidth = 640;
		int WindowHeight = 480;

        try { initializeHardware(); }
        catch (MemoryException error) {
            System.err.println("Failed to initialize application: " + error.getMessage());
            return;
        }

		hMainWindow = new JFrame("KernelN64");
        KeyboardRouting.install();
        EmulationControl.install();
        hMainWindow.addWindowFocusListener(new WindowAdapter(){
            public void windowLostFocus(WindowEvent event){KeyboardRouting.release();}
        });
		hMainWindow.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit("Window Closed.");
			}
		});
		hMainWindow.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_BACK_SPACE) Rewind.held = false;
                if (key == KeyEvent.VK_F4) {
					f4Held = false; // Reset the guard so the next press works immediately
				}
				// stop shift fast-forward
				if (key == KeyEvent.VK_SHIFT) {
                    releaseShift();
				}

				if (pif != null && pif.controllerPlugin != null && !(pif.controllerPlugin instanceof basiccontroller.BasicKeyboardPlugin))
					pif.controllerPlugin.wmKeyUp(key, 0);
			}

			@Override
			public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && Rewind.enabled) { Rewind.held = true; return; }
				int key = e.getKeyCode();

				// F4 toggles the configured fast-mode speed.
				if (key == KeyEvent.VK_F4) {
					if (!f4Held) { // Only trigger on the initial press
						f4Held = true;
                        setFastForward(!isFastForward);
					}
					return;
				}
				
				// SHIFT = hold fast-forward (momentary)
				if (key == KeyEvent.VK_SHIFT) {
                    if(!shiftHeld){shiftHeld=true;shiftAudio=UseAudio;}
					SpeedMultiplier = 4.0f; // super-turbo
					FrameLimit = false;
					UseAudio = false;
				}

				if (pif != null && pif.controllerPlugin != null && !(pif.controllerPlugin instanceof basiccontroller.BasicKeyboardPlugin))
					pif.controllerPlugin.wmKeyDown(key, 0);
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});

		hMainWindow.setJMenuBar(new MenuBar());

		panel = new JPanel();
		panel.setPreferredSize(new java.awt.Dimension(WindowWidth, WindowHeight));
		hMainWindow.getContentPane().add(panel);
        emulationStatus = new EmulationStatus();
        hMainWindow.add(emulationStatus, java.awt.BorderLayout.SOUTH);
		try {
			URL iconURL = ResourceAnchor.class.getResource("/resources/kerneln64-icon.png");
			if (iconURL != null) {
				ImageIcon icon = new ImageIcon(iconURL);
				hMainWindow.setIconImage(icon.getImage());
			}
		} catch (Exception e) {
			System.err.println("Could not load application icon.");
		}

		hMainWindow.pack();
        MainWindowVisibility.showInitial();

		// Plugins
		URL url = null;
		File file = new File("./plugins.properties");
		if (file.exists()) {
			try {
				url = file.toURI().toURL();
			} catch (Exception ex) {
			}
		} else {
			url = ResourceAnchor.class.getClassLoader().getResource("resources/plugins.properties");
		}

		if (url == null) {
			System.out.println("can't find resource: " + "plugins.properties");
			return;
		}

		Properties cfg = new Properties();
		try {
			try (InputStream input = url.openStream()) { cfg.load(input); }
            cfg.forEach((key, value) -> config.putIfAbsent(key, value));
            PluginSettings.migrate(config);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (DEBUG)
			System.out.printf("Plugins Setup\n");


        IdleScreen.show();
        SingleInstance.ready();
	}
}
