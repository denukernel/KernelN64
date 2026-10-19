package kerneln64.cheats;
import kerneln64.hardware.Audio;
import kerneln64.Main;


import java.util.*;

/** Compile guarded SM64 note tables into one relative-pitch mix. Saved entries stay unchanged. */
final class Sm64AudioMixer {
    static final float MIN_FREQUENCY=1.0f/4096,MAX_FREQUENCY=1024;
    private record Pitch(CheatRepository.Entry entry,float[] values,String canonical,String remainder) {}
    static List<CheatRepository.Entry> compile(List<CheatRepository.Entry> entries,Sm64DecompCodes build,boolean legacy) {
        List<Pitch> pitches=new ArrayList<>();
        for(var entry:entries)if(entry.enabled && AudioCodeSelection.audio(entry)) {
            Pitch pitch=read(entry,build,legacy);if(pitch!=null)pitches.add(pitch);
        }
        if(pitches.size()<2)return entries;
        // A deterministic product makes the resulting program and undo ownership
        // independent of checkbox order and stable across saving/loading a state.
        pitches.sort(Comparator.comparing(Pitch::canonical));
        String code=Sm64AudioCodes.pitchCode(i->{
            double value=Sm64AudioCodes.FREQUENCIES[i];
            for(Pitch pitch:pitches)value*=pitch.values[i]/(double)Sm64AudioCodes.FREQUENCIES[i];
            return Math.max(MIN_FREQUENCY,Math.min(MAX_FREQUENCY,value));
        });
        if(build!=null)code=build.port(code);
        Map<CheatRepository.Entry,Pitch> replaced=new IdentityHashMap<>();for(Pitch pitch:pitches)replaced.put(pitch.entry,pitch);
        List<CheatRepository.Entry> result=new ArrayList<>();
        for(var entry:entries) {
            Pitch pitch=replaced.get(entry);
            if(pitch==null){result.add(entry);continue;}
            if(!pitch.remainder.isBlank()) {
                var remainder=new CheatRepository.Entry(entry.name,entry.notes,pitch.remainder,true,entry.group);
                remainder.addressing=entry.addressing;result.add(remainder);
            }
        }
        var mixed=new CheatRepository.Entry("Audio\\Combined pitch","Runtime mix of selected guarded pitch tables",code,true,"sound");
        if(build!=null)mixed.addressing=build.hash;result.add(mixed);return result;
    }
    private static Pitch read(CheatRepository.Entry entry,Sm64DecompCodes build,boolean legacy) {
        int notes;
        try{notes=build==null?Sm64AudioCodes.NOTES:build.address(Sm64AudioCodes.NOTES,512);}
        catch(IllegalArgumentException unsupported){return null;}
        CheatEngine engine=new CheatEngine();engine.setSm64Compatibility(legacy);engine.setSm64Build(build);engine.setCodes(entry.codes);
        Map<Integer,Integer> bytes=engine.writeFootprint().bytes();float[] values=new float[128];
        for(int i=0;i<128;i++) {
            int bits=0;
            for(int j=0;j<4;j++) {
                Integer b=bytes.get(notes+i*4+j);if(b==null || b<0)return null;bits=bits<<8|b;
            }
            values[i]=Float.intBitsToFloat(bits);if(!Float.isFinite(values[i]) || values[i]<=0)return null;
        }
        String canonical=Sm64AudioCodes.pitchCode(i->values[i]);if(build!=null)canonical=build.port(canonical);
        canonical=normalized(canonical);String actual=normalized(engine.diagnosticCodes());
        // Recognize the actual guarded program, not a user-editable preset name.
        // A partial table, modified guards, hooks and arbitrary patches cannot be inferred.
        if(!actual.equals(canonical) && !actual.startsWith(canonical+"\n"))return null;
        String rest=actual.substring(canonical.length()).strip();
        return new Pitch(entry,values,canonical,rest);
    }
    private static String normalized(String code){return code.replace("\r\n","\n").strip();}
}
