package kerneln64.scripting;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Explicitly saved, portable script groups. No group is executed while reading it. */
public final class LuaScriptGroups {
    private static final long MAX_BYTES=8L*1024*1024;
    public static List<LuaScripts.Script> read(Path file)throws IOException{
        if(Files.size(file)>MAX_BYTES)throw new IOException("script group is too large");
        Properties data=new Properties();
        try(InputStream input=Files.newInputStream(file)){data.loadFromXML(input);}
        if(!"1".equals(data.getProperty("version")))throw new IOException("unsupported script group version");
        try{
            int count=Integer.parseInt(data.getProperty("count","0"));
            if(count<1||count>LuaScripts.MAX_SCRIPTS)throw new IllegalArgumentException("use 1-32 scripts per group");
            List<LuaScripts.Script> result=new ArrayList<>();
            for(int i=0;i<count;i++)result.add(new LuaScripts.Script(UUID.randomUUID().toString(),
                Objects.requireNonNull(data.getProperty("script."+i+".name"),"missing script name"),
                Objects.requireNonNull(data.getProperty("script."+i+".source"),"missing script source")));
            return LuaScripts.validateGroup(result);
        }catch(IllegalArgumentException|NullPointerException error){throw new IOException("invalid script group: "+error.getMessage(),error);}
    }
    public static void write(Path file,List<LuaScripts.Script> scripts)throws IOException{
        List<LuaScripts.Script> group=LuaScripts.validateGroup(scripts);Properties data=new Properties();
        data.setProperty("version","1");data.setProperty("count",Integer.toString(group.size()));
        for(int i=0;i<group.size();i++){data.setProperty("script."+i+".name",group.get(i).name());data.setProperty("script."+i+".source",group.get(i).source());}
        ByteArrayOutputStream output=new ByteArrayOutputStream();data.storeToXML(output,"KernelN64 lua script group","UTF-8");
        if(output.size()>MAX_BYTES)throw new IOException("script group is too large");
        Path target=file.toAbsolutePath(),temporary=Files.createTempFile(target.getParent(),".lua-group-",".tmp");
        try{
            Files.write(temporary,output.toByteArray());
            try{Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException error){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
    private LuaScriptGroups(){}
}
