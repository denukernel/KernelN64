package kerneln64.cheats;
import kerneln64.hardware.Audio;
import kerneln64.Main;


import java.util.*;

/** Compose supported pitch effects, then check the remaining writes and VI budget. */
public final class AudioCodeSelection {
    public static final int MAX_EFFECTS=4,MAX_OPERATIONS=65536;
    public static final String HELP="Audio: up to 4 effects and 65,536 operations per VI. SM64 pitch distortions mix their per-note ratios; other overlapping writes must agree.";
    public static boolean audio(CheatRepository.Entry entry){return entry.group.equals("sound") || entry.name.startsWith("Audio\\");}
    public static long count(List<CheatRepository.Entry> entries){return entries.stream().filter(e->e.enabled && audio(e)).count();}
    public static void validate(List<CheatRepository.Entry> entries) {
        if(count(entries)>MAX_EFFECTS)throw new IllegalArgumentException("Audio limit: up to "+MAX_EFFECTS+" effects at once.");
        long inputOperations=0;
        for(var entry:entries)if(entry.enabled && audio(entry)) {
            CheatEngine engine=CheatRepository.validator();engine.setCodes(entry.codes);inputOperations+=engine.writeFootprint().operations();
            if(inputOperations>MAX_OPERATIONS)throw new IllegalArgumentException("Audio code budget exceeded: maximum "+MAX_OPERATIONS+" operations per VI.");
        }
        var compiled=Sm64AudioMixer.compile(entries,Main.decompCheats,Main.sm64UsCheats);
        Map<Integer,Integer> values=new HashMap<>();Map<Integer,String> owners=new HashMap<>();long operations=0;
        for(var entry:compiled)if(entry.enabled && audio(entry)) {
            CheatEngine engine=CheatRepository.validator();engine.setCodes(entry.codes);var writes=engine.writeFootprint();
            operations+=writes.operations();
            if(operations>MAX_OPERATIONS)throw new IllegalArgumentException("Audio code budget exceeded: maximum "+MAX_OPERATIONS+" operations per VI.");
            for(var write:writes.bytes().entrySet()) {
                int address=write.getKey(),value=write.getValue();Integer previous=values.get(address);
                if(previous!=null && (previous!=value || value<0))throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "Audio conflict: %s and %s write different values at %08X.",shortName(owners.get(address)),shortName(entry.name),address|0x80000000));
                values.put(address,value);owners.putIfAbsent(address,entry.name);
            }
        }
    }
    private static String shortName(String name){return name.substring(name.lastIndexOf('\\')+1);}
    public static void toggle(List<CheatRepository.Entry> entries,List<CheatRepository.Entry> selected,boolean enabled) {
        Map<CheatRepository.Entry,Boolean> previous=new IdentityHashMap<>();for(var e:entries)previous.put(e,e.enabled);
        try {
            for(var entry:selected) {
                entry.enabled=enabled;
                if(enabled && !audio(entry) && !entry.group.isEmpty())for(var other:entries)if(other!=entry && other.group.equals(entry.group))other.enabled=false;
            }
            validate(entries);
        }catch(IllegalArgumentException error){previous.forEach((entry,value)->entry.enabled=value);throw error;}
    }
    static void loaded(List<CheatRepository.Entry> entries) {
        for(var e:entries)if(audio(e))for(String old:List.of("Select only one Audio effect.","Select only one Audio effect at a time.","Select one Audio effect at a time.","Select one Audio preset at a time.","Choose one Audio effect.","Enable after boot, one Audio preset at a time.","Up to four Audio effects can be enabled if their writes agree and fit the code budget."))
            e.notes=e.notes.replace(old,HELP);
        List<CheatRepository.Entry> selected=entries.stream().filter(e->e.enabled && audio(e)).toList();
        for(var e:selected)e.enabled=false;
        for(var e:selected)try{toggle(entries,List.of(e),true);}catch(IllegalArgumentException error){e.notes+="\nNot activated: "+error.getMessage();}
    }
}
