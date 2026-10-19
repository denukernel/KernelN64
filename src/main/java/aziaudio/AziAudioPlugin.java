package aziaudio;
import aziaudio.config.AziAudioConfig;
import aziaudio.config.AziAudioSettings;
import aziaudio.hle.HLEMain;
import aziaudio.playback.AudioCode;

import javax.swing.JFrame;
import plugin.AudioPlugin;

public class AziAudioPlugin implements AudioPlugin {
    
    public static final String PLUGIN_VERSION = "0.55.1 Alpha";
    
    private static AudioInfo audioInfo;
    private static AudioCode snd;
    private static int dacrate = -1;
    
    public AziAudioPlugin() {
        snd = new AudioCode();
    }

    public static void refreshSettings() { if (snd != null) snd.updateSettings(); }

    public static AudioCode stateAudio() { return snd; }

    public void aiDacrateChanged(int systemType) {
        int frequency = 0;
        if (dacrate != audioInfo.aiRegisters[AI_DACRATE_REG]) {
            dacrate = audioInfo.aiRegisters[AI_DACRATE_REG];
            switch (systemType) {
                case SYSTEM_NTSC: frequency = 48681812 / (dacrate + 1); break;
                case SYSTEM_PAL:  frequency = 49656530 / (dacrate + 1); break;
                case SYSTEM_MPAL: frequency = 48628316 / (dacrate + 1); break;
            }
            snd.SetFrequency(frequency);
        }
    }

    public void aiLenChanged() {
        int addr = audioInfo.aiRegisters[AI_DRAM_ADDR_REG] & 0x00FFFFF8;
        int len  = audioInfo.aiRegisters[AI_LEN_REG] & 0x3FFF8;

        if (len <= 0 || addr > audioInfo.rdram.capacity() - len) return;

        audioInfo.aiRegisters[AI_STATUS_REG] = snd.AddBuffer(audioInfo.rdram, addr, len);
    }


    public int aiReadLength() {
        audioInfo.aiRegisters[AI_LEN_REG] = snd.GetReadStatus();
        return audioInfo.aiRegisters[AI_LEN_REG];
    }

    public void aiUpdate(boolean wait) {
    }

    public void closePlugin() {
        snd.DeInitialize();
    }

    public void pluginAbout(JFrame hParent) {
    	// TODO: Add about box
    }

    public void pluginConfig(JFrame hParent) {
    	AziAudioConfig config = new AziAudioConfig(hParent);
    	config.setVisible(true);
    	// After config closes, update settings
    	if (snd != null) {
    		snd.updateSettings();
    	}
    }

    public void pluginTest(JFrame hParent) {
    }

    public void getPluginInfo(PluginInfo pluginInfo) {
        pluginInfo.memoryBswaped = true;
        pluginInfo.normalMemory  = false;
        pluginInfo.name = "KernelN64 Audio based on: Azimer's HLE Audio v" + PLUGIN_VERSION;
        pluginInfo.type = PLUGIN_TYPE_AUDIO;
        pluginInfo.version = 0x0101;
    }

    public boolean initiateAudio(AudioInfo audioInfo) {
        closePlugin();
        this.audioInfo = audioInfo;
        HLEMain.dmem = audioInfo.dmem;
        HLEMain.rdram = audioInfo.rdram;
        snd.Initialize(audioInfo);
        dacrate = -1;
        return true;
    }
    
    public void processAList() {
    	// Check HLE setting
    	if (AziAudioSettings.hle) {
    		HLEMain.HLEStart();
    	}
    }

    public void romClosed() {
        snd.DeInitialize();
        dacrate = -1;
    }
}
