package kerneln64.cheats;
import kerneln64.Main;


import java.util.*;

/** Additional documented US presets; kept separate from persistence and the editor. */
final class Sm64ExtraCodes {
    static List<CheatRepository.Entry> presets() {
        List<CheatRepository.Entry> list=new ArrayList<>();
        String source="SM64 US v1.0 / compatible US-based hacks. Source: https://github.com/project64/project64/blob/develop/Config/Cheats/Super%20Mario%2064%20(U).cht. ";
        String[] levels={"Castle interior","Castle courtyard","Bowser in the Dark World","Vanish Cap under the Moat",
            "Bowser in the Fire Sea","The Secret Aquarium","Bowser in the Sky","The Princess's Secret Slide",
            "Cavern of the Metal Cap","Tower of the Wing Cap","Bowser boss 1","Wing Mario over the Rainbow","Bowser boss 2","Bowser boss 3"};
        int[] ids={6,0x1A,0x11,0x12,0x13,0x14,0x15,0x1B,0x1C,0x1D,0x1E,0x1F,0x21,0x22};
        for(int i=0;i<ids.length;i++)list.add(new CheatRepository.Entry("Level Select\\"+levels[i],source+
            "Enter a painting, press A at star select, then hold L as the screen fades. Select only one destination.",
            String.format(Locale.ROOT,"D033AFA1 0020\nA032DDF9 %04X",ids[i]),false,"level"));
        String[] caps={"Invisible metal (no cap)","Metal wing (no cap)","Invisible metal with cap",
            "Invisible wing with cap","Metal wing with cap","Invisible metal wing with cap"};
        int[] flags={6,0x0C,0x16,0x1A,0x1C,0x1E};
        for(int i=0;i<flags.length;i++)list.add(new CheatRepository.Entry("Mario's Cap\\"+caps[i],"SM64 US v1.0 cap flags. Source: https://sites.google.com/site/jamesskingdom/Home/game-codes-by-james-s/super-mario-64-codes/sm64-n64-cap-power-codes. "+
            "Combines the documented cap flags. Use during gameplay; one cap preset at a time. Flying requires an airborne wing-cap action.",
            String.format(Locale.ROOT,"8033B177 %04X",flags[i]),false,"cap"));
        String archive="SM64 US v1.0. Source: https://gamegenie.com/cheats/gameshark/n64/super_mario_64.html. ";
        list.add(new CheatRepository.Entry("Press GS for 99 coins",archive+"Press F9 / GameShark Button during a course. Changes the live coin counter; does not directly award a saved star.","8933B218 0063",false,""));
        list.add(new CheatRepository.Entry("Infinite\\Cap timer",archive+"Freezes Mario's cap timer at 65535. Obtain a cap first; this does not grant a cap or an airborne action.","8133B226 FFFF",false,""));
        list.addAll(Sm64AudioCodes.presets());
        list.addAll(Sm64MusicCodes.presets());
        return list;
    }
}
