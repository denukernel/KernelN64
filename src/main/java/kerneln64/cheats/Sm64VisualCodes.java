package kerneln64.cheats;

import java.util.*;

/** US MarioState fields, verified against the matching decomp layout. */
final class Sm64VisualCodes {
    static List<CheatRepository.Entry> presets() {
        List<CheatRepository.Entry> list=new ArrayList<>();
        for(int timer:new int[]{1,8,16,30})add(list,"GFX\\Mario squash\\Hold phase "+timer,
            String.format(Locale.ROOT,"8033B224 %04X",timer),"freezes squishTimer at "+timer+". phases 1 through 16 hold rubber recovery poses; 30 holds flat scale (1.4, 0.4, 1.4). also affects movement and jumping.","squash");
        for(int timer:new int[]{16,30})add(list,"Animations\\Squash recovery\\GS button phase "+timer,
            String.format(Locale.ROOT,"8833B224 %04X",timer),"press F9 during gameplay to start the guest squash recovery animation. it advances normally after the button write. this changes scale animation, not skeletal animation IDs.","squash");
        StringBuilder pulse=new StringBuilder();
        for(int phase:new int[]{0,64,128,192})pulse.append(String.format(Locale.ROOT,"D032D5D7 %04X\n8033B224 001E\n",phase));
        add(list,"Animations\\Squash recovery\\Repeat every 64 game frames",pulse.toString().strip(),"restarts squash from phase 30 every 64 game frames using the low byte of gGlobalTimer at 8032D5D4. uses KernelN64 conditions. game updates can affect timing.","squash");
        for(int speed:new int[]{16,32,64})add(list,"Mario\\Movement\\Forward speed "+speed,word(0x33B1C4,Float.floatToIntBits(speed),0x81),"freezes MarioState.forwardVel, offset 0x54, to "+speed+". actions can recompute speed before movement; this is not a uniform physics multiplier.","mario-speed");
        add(list,"Mario\\Movement\\Rise on GS button",word(0x33B1BC,Float.floatToIntBits(40),0x89),"press F9 while airborne to set MarioState.vel[1], offset 0x4c, to 40. the current action still controls gravity and landing. does not force flight.","vertical-speed");
        add(list,"Mario\\Movement\\No quicksand depth",word(0x33B230,0,0x81),"freezes MarioState.quicksandDepth, offset 0xc0, at zero. floor logic can update it again. does not change collision geometry.","");
        String[] names={"Castle grounds","Castle interior","Castle courtyard","Bob-omb Battlefield","Whomps Fortress","Cool Cool Mountain","Jolly Roger Bay","Big Boos Haunt","Hazy Maze Cave","Lethal Lava Land","Shifting Sand Land","Dire Dire Docks","Snowmans Land","Wet-Dry World","Tall Tall Mountain","Tiny-Huge Island","Tick Tock Clock","Rainbow Ride"};
        int[] ids={0x10,6,0x1a,9,0x18,5,0xc,4,7,0x16,8,0x17,0xa,0xb,0x24,0xd,0xe,0xf};
        for(int i=0;i<ids.length;i++)add(list,"World\\Destinations\\"+names[i],String.format(Locale.ROOT,"D033AFA1 0020\n8032DDF9 %04X",ids[i]),"gCurrLevelNum low byte at 8032DDF9. enter a painting, press A at star select, then hold L during the fade. choose one destination, including existing Level Select entries. redirects a transition, not an immediate warp or geometry edit. source: project64 Config/Cheats/Super Mario 64 (U).cht.","level");
        return list;
    }
    private static String word(int address,int bits,int type) {
        return String.format(Locale.ROOT,"%08X %04X\n%08X %04X",type<<24|address,bits>>>16,type<<24|address+2,bits&65535);
    }
    private static void add(List<CheatRepository.Entry> list,String name,String code,String notes,String group) {
        String source="sm64 us v1.0 / compatible us layouts. gMarioStates = 8033B170. source: n64decomp/sm64 9921382a68bb0c865e5e45eb594d9c64db59b1af include/types.h, src/game/mario.c and src/game/game_init.c. ";
        list.add(new CheatRepository.Entry(name,source+notes+" enable during gameplay. disable to release owned bytes; newer guest writes may prevent restoring the initial value. restore a clean state if an action persists. source and encoding checked; gameplay runtime verification pending.",code,false,group));
    }
}
