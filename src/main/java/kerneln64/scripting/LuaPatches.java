package kerneln64.scripting;

import java.util.*;
import org.luaj.vm2.LuaError;

/** Byte-granular undo ownership for all Lua RAM writes, on the CPU thread. */
final class LuaPatches {
    private static final int MAX_BYTES=65536;
    private static final NavigableMap<Integer,Patch> patches=new TreeMap<>();
    // Overlapping access widths form atomic regions. A guest float/word update
    // must preserve all its bytes, including bytes equal to the last Lua write.
    private static final NavigableMap<Integer,Integer> regions=new TreeMap<>();
    private static void claim(int address,int width){
        int start=address,end=address+width;
        Map.Entry<Integer,Integer> previous=regions.floorEntry(start);
        if(previous!=null&&previous.getValue()>start){start=previous.getKey();end=Math.max(end,previous.getValue());regions.remove(previous.getKey());}
        Map.Entry<Integer,Integer> next=regions.ceilingEntry(start);
        while(next!=null&&next.getKey()<end){end=Math.max(end,next.getValue());regions.remove(next.getKey());next=regions.ceilingEntry(start);}
        regions.put(start,end);
    }
    private static void reconcile(LuaScripts.Session session,int start,int end){
        boolean changed=false;
        for(var entry:patches.subMap(start,true,end,false).entrySet())if(LuaApi.read(session,entry.getKey(),1)!=entry.getValue().last){changed=true;break;}
        if(changed)for(var entry:patches.subMap(start,true,end,false).entrySet()){
            Patch patch=entry.getValue();patch.original=patch.last=LuaApi.read(session,entry.getKey(),1);patch.owners.clear();
        }
    }
    private static final class Patch {
        int original,last;
        final LinkedHashMap<LuaScripts.Session,Integer> owners=new LinkedHashMap<>();
        Patch(int original){this.original=this.last=original;}
    }
    static void write(LuaScripts.Session session,int address,int width,int value){
        // Older presets restore saved values in onstop. Ignore writes to their
        // owned bytes there: the journal knows which values are still live.
        if(session.finishing){
            boolean owned=false;
            for(int i=0;i<width;i++){
                Patch patch=patches.get(address+i);
                if(patch!=null&&patch.owners.containsKey(session)){owned=true;break;}
            }
            if(!owned)LuaApi.write(session,address,width,value);
            return;
        }
        int missing=0;
        for(int i=0;i<width;i++)if(!patches.containsKey(address+i))missing++;
        if(patches.size()+missing>MAX_BYTES)throw new LuaError("temporary writes exceed 64 KiB");
        claim(address,width);
        var region=regions.floorEntry(address);reconcile(session,region.getKey(),region.getValue());
        int current=LuaApi.read(session,address,width);
        for(int i=0;i<width;i++){
            int at=address+i,shift=(width-i-1)*8,now=(current>>>shift)&255,next=(value>>>shift)&255;
            Patch patch=patches.computeIfAbsent(at,key->new Patch(now));
            if(now!=patch.last){patch.original=now;patch.owners.clear();}
            patch.owners.remove(session);patch.owners.put(session,next);patch.last=next;
        }
        LuaApi.write(session,address,width,value);
    }
    static int original(LuaScripts.Session session,int address,int width){
        for(var region:regions.entrySet())if(region.getKey()<address+width&&region.getValue()>address)reconcile(session,region.getKey(),region.getValue());
        int current=LuaApi.read(session,address,width),result=0;
        for(int i=0;i<width;i++){
            int now=(current>>>((width-i-1)*8))&255;
            Patch patch=patches.get(address+i);
            result=(result<<8)|(patch!=null&&now==patch.last?patch.original:now);
        }
        return result;
    }
    static void release(LuaScripts.Session session){
        for(var region:regions.entrySet())reconcile(session,region.getKey(),region.getValue());
        Iterator<Map.Entry<Integer,Patch>> iterator=patches.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer,Patch> entry=iterator.next();Patch patch=entry.getValue();
            if(patch.owners.remove(session)==null){if(patch.owners.isEmpty())iterator.remove();continue;}
            int current=LuaApi.read(session,entry.getKey(),1);
            if(current==patch.last){
                int restore=patch.original;
                for(int value:patch.owners.values())restore=value;
                if(current!=restore)LuaApi.write(session,entry.getKey(),1,restore);
                patch.last=restore;
            }else{
                patch.original=current;patch.last=current;patch.owners.clear();
            }
            if(patch.owners.isEmpty())iterator.remove();
        }
        regions.entrySet().removeIf(region->{Integer next=patches.ceilingKey(region.getKey());return next==null||next>=region.getValue();});
    }
    /** A loaded state replaces RAM; never restore patches from the old state. */
    static void stateRestored(){patches.clear();regions.clear();}
    private LuaPatches(){}
}
