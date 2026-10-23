package kerneln64.cheats;
import kerneln64.hardware.Audio;
import kerneln64.ui.CheatCodeInput;
import kerneln64.Main;


import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Named per-ROM cheats. Existing plain-text code files remain readable. */
public final class CheatRepository {
    public static final class Entry {
        public String name,notes,codes,group; public boolean enabled;
        public String addressing="";
        public Entry(String name,String notes,String codes,boolean enabled,String group) {
            this.name=name;this.notes=notes;this.codes=codes;this.enabled=enabled;this.group=group;
        }
    }
    static boolean sm64Us(ByteBuffer rom) { return Sm64Compatibility.detect(rom).usLayout(); }
    public static CheatEngine validator() {
        CheatEngine engine=new CheatEngine();engine.setSm64Compatibility(Main.sm64UsCheats);engine.setSm64Build(Main.decompCheats);return engine;
    }
    public static List<Entry> load(Path path,boolean sm64)throws IOException {
        String text=Files.exists(path)?Files.readString(path):"";
        List<Entry> entries=parse(Sm64BurstMusicCodes.upgrade(text));
        for(Entry entry:entries)if(entry.codes.replace("\r\n","\n").equals(Sm64BurstMusicCodes.speedCode(1).replace("\r\n","\n")) || entry.codes.replace("\r\n","\n").equals(Sm64BurstMusicCodes.speedCode(2).replace("\r\n","\n"))){
            boolean enabled=entry.enabled;Entry replacement=entry.codes.contains("B0222618 0002")?Sm64BurstMusicCodes.extremePreset():Sm64BurstMusicCodes.fasterPreset();entry.notes=replacement.notes;entry.enabled=enabled;
        }
        if(sm64)for(Entry entry:entries)if(entry.name.equals("Sound Experiments\\Alien sounds and music") && entry.codes.equals("A031C1AF DDDD")) {
            entry.name="Audio\\Documented\\Alien sounds and music (experimental)";entry.group="sound";
        }
        if(sm64 && !text.startsWith("# KernelN64 named GameShark codes") && !text.startsWith("# Jario64+ named GameShark codes"))
            for(Entry preset:presets())if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);
        if(sm64 && !text.contains("# Mario-head stretch preset v1") && entries.stream().noneMatch(e->e.name.equals(Sm64HeadStretch.NAME)))
            entries.addAll(forBuild(List.of(new Entry(Sm64HeadStretch.NAME,Sm64HeadStretch.NOTES,Sm64HeadStretch.CODES,false,""))));
        if(sm64 && !text.contains("# SM64 expanded preset catalog v2"))
            for(Entry preset:forBuild(Sm64ExtraCodes.presets()))if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);
        if(sm64 && !text.contains("# SM64 music presets v4"))
            for(Entry preset:forBuild(Sm64MusicCodes.presets()))if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);
        if(sm64 && !text.contains("# SM64 visual presets v1"))
            for(Entry preset:forBuild(Sm64VisualCodes.presets()))if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);
        if(sm64 && !text.contains("# SM64 display list presets v1"))
            for(Entry preset:forBuild(Sm64DisplayListCodes.presets()))if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);
        if(sm64 && !text.contains("# SM64 scene gfx presets v1"))
            for(Entry preset:forBuild(Sm64ScenePresetCodes.presets()))if(entries.stream().noneMatch(e->e.name.equals(preset.name)))entries.add(preset);
        if(Main.decompCheats!=null)Main.decompCheats.prepare(entries);
        if(Main.decompCheatsRejected)for(var entry:entries)entry.enabled=false;
        AudioCodeSelection.loaded(entries);
        return entries;
    }
    public static List<Entry> parse(String text) {
        List<Entry> result=parseStructure(text);
        validateParsed(result);return result;
    }
    static List<Entry> parseStructure(String text) {
        List<Entry> result=new ArrayList<>();String name="",notes="",group="",addressing="";boolean enabled=true;StringBuilder codes=new StringBuilder();
        boolean named=text.lines().anyMatch(line->line.strip().equalsIgnoreCase("[CHEAT]"));
        for(String raw:(text+"\n[CHEAT]").split("\\R")) {
            String line=raw.trim();
            if((line.isEmpty() && !named) || line.equalsIgnoreCase("[CHEAT]") || line.startsWith("$")) {
                if(!codes.isEmpty()){Entry entry=new Entry(name.isEmpty()?"Custom\\Cheat "+(result.size()+1):name,notes,codes.toString().strip(),enabled,group);entry.addressing=addressing;result.add(entry);name="";notes="";group="";addressing="";enabled=true;codes.setLength(0);}
                if(line.startsWith("$"))name=line.substring(1).trim();
            } else if(line.isEmpty()){if(!codes.isEmpty())codes.append('\n');}
            else if(line.startsWith("NAME="))name=line.substring(5);
            else if(line.startsWith("NOTE=") || line.startsWith("Note="))notes=line.substring(5).replace("\\n","\n");
            else if(line.startsWith("GROUP="))group=line.substring(6);
            else if(line.startsWith("ADDRESSING="))addressing=line.substring(11);
            else if(line.startsWith("ENABLED="))enabled=Boolean.parseBoolean(line.substring(8));
            else if(!line.startsWith("#") && !line.startsWith(";"))codes.append(line.startsWith("CODE=")?line.substring(5):line).append('\n');
        }
        return result;
    }
    private static void validateParsed(List<Entry> result) {
        for(Entry e:result){
            CheatEngine validation=validator();
            String problem=Main.sm64UsCheats?CheatEngine.knownProblem(e.codes):null;
            if(problem!=null && CheatEngine.replacesHeadCode(e.codes)) {
                validation.setCodes(e.codes);
                e.codes=Sm64HeadStretch.CODES;
                e.notes=e.notes.replaceAll("(?m)^Unsupported SM64 Mario-head patch 811954DB 7644\\.[^\\r\\n]*(?:\\RSource: https://www\\.supercheats\\.com/[^\\r\\n]*)?","").strip();
                if(!e.notes.contains(Sm64HeadStretch.NOTES))e.notes+=(e.notes.isBlank()?"":"\n\n")+"Replaced the old 811954DB 7644 patch with the KernelN64 version. "+Sm64HeadStretch.NOTES;
            } else validation.setCodes(e.codes);
        }
    }
    public static String serialize(List<Entry> entries) {
        StringBuilder out=new StringBuilder("# KernelN64 named GameShark codes\n# Mario-head stretch preset v1\n# SM64 expanded preset catalog v2\n# SM64 music presets v4\n# SM64 music-only bursts v5\n");
        out.append("# SM64 visual presets v1\n");
        out.append("# SM64 display list presets v1\n");
        out.append("# SM64 scene gfx presets v1\n");
        for(Entry e:entries)out.append("[CHEAT]\nNAME=").append(e.name.replace('\n',' ')).append("\nNOTE=").append(e.notes.replace("\n","\\n")).append("\nGROUP=").append(e.group).append("\nADDRESSING=").append(e.addressing).append("\nENABLED=").append(e.enabled).append('\n').append(e.codes.strip()).append("\n\n");
        return out.toString();
    }
    public static void save(Path path,List<Entry> entries)throws IOException {
        AudioCodeSelection.validate(entries);
        String text=serialize(entries);validator().setCodes(text);
        Path absolute=path.toAbsolutePath();Files.createDirectories(absolute.getParent());
        Path temp=Files.createTempFile(absolute.getParent(),"cheats-",".tmp");
        try {Files.writeString(temp,text);try{Files.move(temp,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,absolute,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temp);}
    }
    static void toggle(List<Entry> entries,Entry selected,boolean enabled) {
        AudioCodeSelection.toggle(entries,List.of(selected),enabled);
    }
    public static List<Entry> importCodes(String text,boolean nativeAddresses) {
        List<Entry> entries=parse(text);
        for(var entry:entries) {
            entry.enabled=false;
            if(Main.decompCheats!=null) {
                if(!entry.addressing.isEmpty() && !entry.addressing.equals(Main.decompCheats.hash))throw new IllegalArgumentException("This file contains native codes for a different ROM build.");
                if(!nativeAddresses && entry.addressing.isEmpty())entry.codes=CheatCodeInput.retail(entry.codes,Main.decompCheats);
                entry.addressing=Main.decompCheats.hash;
            }
        }
        return entries;
    }
    private static void add(List<Entry> list,String name,String codes,String notes,String group){list.add(new Entry(name,notes,codes,false,group));}
    public static List<Entry> presets() {
        return forBuild(retailPresets());
    }
    private static List<Entry> forBuild(List<Entry> stock) {
        return Main.decompCheats==null?stock:Main.decompCheats.presets(stock);
    }
    static List<Entry> retailPresets() {
        List<Entry> list=new ArrayList<>();String us="SM64 US v1.0 memory layout. ";
        add(list,Sm64HeadStretch.NAME,Sm64HeadStretch.CODES,Sm64HeadStretch.NOTES,"");
        for(int slot=0;slot<4;slot++) {
            int base=0x207700+slot*0x70;String folder="[Slot "+(char)('A'+slot)+" Codes]\\Have All\\";
            add(list,folder+"120 Stars",String.format("%08X 0001\n%08X 00C7\n50001101 0000\n%08X 00FF",0x80000000|base+0x23,0x80000000|base+0x0B,0x80000000|base+0x0C),us+"Changes this save slot's progress. Collect a star to save the progress, then disable the code.","");
            add(list,folder+"Doors unlocked",String.format("%08X 007E",0x80000000|base+0x0A),us+"Unlocks key doors and drains the moat in this save slot.","");
            add(list,folder+"100-coin records",String.format("50000F01 0000\n%08X 0064",0x80000000|base+0x25),us+"Sets the slot's course coin records to 100.","");
        }
        add(list,"Don't Hurt Mario\\Falling","8033B22C 00E8\n8133B178 FFFF",us+"Prevents fall damage.","");
        add(list,"Don't Hurt Mario\\Monsters","A033B197 0001",us+"Prevents damage from many enemies.","");
        add(list,"Infinite\\Energy and breath","8033B21E 0008",us+"Keeps the power meter full during gameplay.","");
        add(list,"Infinite\\Lives","8033B21D 0064",us+"Keeps lives at 100.","");
        add(list,"Have\\Turbo running","8033B1C3 00FF",us+"Changes Mario's movement speed.","");
        add(list,"Funny Mario Options\\Walk on lava","81250900 1000",us+"Patches lava collision behavior.","");
        add(list,"Funny Mario Options\\Walk on sand","81255490 1000\n8125572C 1000\n8125572E 0031",us+"Patches quicksand behavior.","");
        int[] values={0x01,0x02,0x04,0x08,0x0A,0x0F,0x12,0x15,0x19,0x20,0x24,0x28};
        String[] caps={"No cap","Invisible","Metal","Wing","Invisible wing","Invisible metal wing","Invisible with cap","Metal with cap","Wing with cap","Cap in hand","Metal with cap in hand","Wing with cap in hand"};
        for(int i=0;i<values.length;i++)add(list,"Mario's Cap\\"+caps[i],String.format("8033B177 %04X",values[i]),us+"Choose one cap mode. Disabling restores patched bytes while preserving any newer game-written values.","cap");
        String[] levels={"Castle grounds","Bob-omb Battlefield","Whomp's Fortress","Cool, Cool Mountain","Jolly Roger Bay","Big Boo's Haunt","Hazy Maze Cave","Lethal Lava Land","Shifting Sand Land","Dire, Dire Docks","Snowman's Land","Wet-Dry World","Tall, Tall Mountain","Tiny-Huge Island","Tick Tock Clock","Rainbow Ride"};
        int[] ids={0x10,9,0x18,5,0x0C,4,7,0x16,8,0x17,0x0A,0x0B,0x24,0x0D,0x0E,0x0F};
        for(int i=0;i<ids.length;i++)add(list,"Level Select\\"+levels[i],String.format("D033AFA1 0020\nA032DDF9 %04X",ids[i]),us+"Enter a painting. At star select, press A; hold L as the screen fades to enter this level.","level");
        add(list,"Press GS for 255 coins","8933B218 00FF",us+"Press F9 or Tools > GameShark Button during a course.","");
        add(list,"Press L to levitate","D033AFA1 0020\n8133B1BC 4220\nD033AFA1 0020\n8133B17C 0300\nD033AFA1 0020\n8133B17E 0880",us+"Hold L to rise and release it to land.","");
        add(list,"Audio\\Documented\\Alien sounds and music (experimental)","A031C1AF DDDD",us+"Historical instruction corruption in the audio sequence parser; can disrupt sound or fault the audio thread. Up to four Audio effects can be enabled within the code budget. Supported pitch tables mix their relative changes; other writes must agree. Enable during gameplay. Disabling restores patched bytes; already queued sound may finish. Generated distortion presets change bounded audio data instead. Source: https://almarsguides.com/retro/walkthroughs/N64/Games/SuperMario64/Gameshark/GlitchCodes/.","sound");
        list.addAll(Sm64ExtraCodes.presets());
        list.addAll(Sm64VisualCodes.presets());
        list.addAll(Sm64DisplayListCodes.presets());
        list.addAll(Sm64ScenePresetCodes.presets());
        return list;
    }
}


