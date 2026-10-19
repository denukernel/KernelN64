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


package kerneln64.hardware;
import kerneln64.memory.Memory;
import kerneln64.cpu.Registers;
import kerneln64.Main;


import java.nio.ByteBuffer;
import java.util.Properties;
import javax.swing.JFrame;
import plugin.AudioPlugin;


public class Audio {
    
    public static final String AUDIO_PLUGIN = "AUDIO_PLUGIN";
    
//    // regAI Audio Interface (AI) Registers
//    public static final int AI_DRAM_ADDR_REG=                   0;
//    public static final int AI_LEN_REG=                         1;
//    public static final int AI_CONTROL_REG=			2;
//    public static final int AI_STATUS_REG=			3;
//    public static final int AI_DACRATE_REG=			4;
//    public static final int AI_BITRATE_REG=			5;
    
    // Audio Interface (AI) Registers
    // used by Memory, Plugin(Aud)
    public int[] regAI = new int[6];
    
    // used by Memory
    public int[] audioIntrReg = new int[1];
    
    // used by Rsp
    public AudioPlugin audioPlugin;
    
    private Runnable checkInterrupts;
    private ByteBuffer rDram;
    private ByteBuffer dMem;
    private ByteBuffer iMem;
    
    /** Creates a new instance of Audio */
    public Audio(Runnable checkInterrupts, ByteBuffer rDram, ByteBuffer dMem, ByteBuffer iMem) {
        this.checkInterrupts = checkInterrupts;
        this.rDram = rDram;
        this.dMem = dMem;
        this.iMem = iMem;
    }
    
    public boolean setupPlugin(JFrame hWnd, Properties cfg) {

        String audio_plugin = cfg.getProperty(AUDIO_PLUGIN, "NO_AUDIO_PLUGIN");
        try {
            Class c = Class.forName(audio_plugin);
            audioPlugin = (AudioPlugin)c.newInstance();
        } catch (Exception ex) {
            System.err.println("No audio plugin loaded. " + ex.getMessage());

            return false;
        }
        
        AudioPlugin.AudioInfo audioInfo = new AudioPlugin.AudioInfo();
        
        audioInfo.hwnd = hWnd;
        audioInfo.memoryBswaped = false; //true;
        audioInfo.rdram = rDram;
        audioInfo.dmem = dMem;
        audioInfo.imem = iMem;
        
        audioInfo.MI_INTR_REG = 0;
        audioInfo.miRegisters = audioIntrReg;

        audioInfo.aiRegisters = regAI;
        audioInfo.checkInterrupts = checkInterrupts;
        
        if (!audioPlugin.initiateAudio(audioInfo)) {
            System.err.println("Failed to Initilize Audio!");
            return false;
        }
        
        audioPlugin.aiUpdate(true);

            
        return true;
    }


}
