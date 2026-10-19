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
 * Originally based on Zilmar's Basic Audio Plugin.
 *
 */

package basicaudio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import plugin.AudioPlugin;


public class BasicAudioPlugin implements AudioPlugin {
    
    public static final int AI_STATUS_FIFO_FULL = 0x80000000; /* Bit 31: full */
    public static final int AI_STATUS_DMA_BUSY = 0x40000000; /* Bit 30: busy */
    public static final int MI_INTR_AI = 0x04; /* Bit 2: AI intr */
    
    public static final int BUFFER_SIZE = 0x5000; // 20,480
    
    private int frequency;
    private int dacrate = 0;
    private int snd1Len;
    private int snd1ReadPos;
    private AudioFormat wfm;
    private SourceDataLine line;
    private ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
    
    private int[] regAI;
    private int[] regMI;
    private byte[] rdram;
    private int MI_INTR_REG;
    private Runnable checkInterrupts;

    public class WriteLineData implements Runnable {
        byte[] buffer;

        public WriteLineData(byte[] source, int offset, int length) {
            buffer = new byte[length];
            System.arraycopy(source, offset, buffer, 0, length);
        }

        public void run() {
            SourceDataLine output=line;
            if(output!=null && output.isOpen())try{output.write(buffer,0,buffer.length);}catch(IllegalStateException closed){}
        }
    };
    
    public BasicAudioPlugin() {
    }
    
    public void aiDacrateChanged(int systemType) {
        if (dacrate != regAI[AI_DACRATE_REG]) {
            dacrate = regAI[AI_DACRATE_REG];
            switch (systemType) {
                case SYSTEM_NTSC: frequency = 48681812 / (dacrate + 1); break;
                case SYSTEM_PAL:  frequency = 49656530 / (dacrate + 1); break;
                case SYSTEM_MPAL: frequency = 48628316 / (dacrate + 1); break;
            }
            wfm = new AudioFormat(frequency, 16, 2, true, true);
            SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, wfm, BUFFER_SIZE);
            try {
                line = (SourceDataLine)AudioSystem.getLine(info);
                line.open(wfm);
            } catch(Exception e) {
                e.printStackTrace();
                return;
            }
            line.start();
        }
    }
    
    public void aiLenChanged() {
        if (regAI[AI_LEN_REG] == 0)
            return;
        
        regAI[AI_STATUS_REG] |= AI_STATUS_FIFO_FULL;
        
        snd1Len = regAI[AI_LEN_REG] & 0x3FFF8;
        snd1ReadPos = regAI[AI_DRAM_ADDR_REG] & 0x00FFFFF8;
        

        threadExecutor.execute(new WriteLineData(rdram, snd1ReadPos, snd1Len));
        
        snd1Len = 0;
        
        regAI[AI_STATUS_REG] &= ~AI_STATUS_FIFO_FULL;
        regMI[MI_INTR_REG] |= MI_INTR_AI;
        checkInterrupts.run();
    }
    
    public int aiReadLength() {
        return snd1Len;
    }
    
    public void aiUpdate(boolean wait) {
        
    }
    
    public void closePlugin() {
        if(line!=null){line.stop();line.flush();line.close();line=null;}
        threadExecutor.shutdownNow();
    }
    
    public void pluginAbout(JFrame hParent) {
    }
    
    public void pluginConfig(JFrame hParent) {
    }
    
    public void pluginTest(JFrame hParent) {
    }
    
    public void getPluginInfo(PluginInfo pluginInfo){
        pluginInfo.version = 0x0101;
        pluginInfo.type    = PLUGIN_TYPE_AUDIO;
        pluginInfo.name = "Basic Audio Plugin";
        pluginInfo.normalMemory  = true;
        pluginInfo.memoryBswaped = true;
    }
    
    public boolean initiateAudio(AudioInfo audioInfo) {
        if(threadExecutor.isShutdown())threadExecutor=Executors.newSingleThreadExecutor();
        aziaudio.hle.HLEMain.rdram=audioInfo.rdram;aziaudio.hle.HLEMain.dmem=audioInfo.dmem;
        this.regAI = audioInfo.aiRegisters;
        this.regMI = audioInfo.miRegisters;
        this.rdram = audioInfo.rdram.array();
        this.MI_INTR_REG = audioInfo.MI_INTR_REG;
        this.checkInterrupts = audioInfo.checkInterrupts;
        dacrate = 0;
        return true;
    }
    
    public void romClosed() { closePlugin(); }
    
    public void processAList() {
        aziaudio.hle.HLEMain.HLEStart();
    }
    
}
