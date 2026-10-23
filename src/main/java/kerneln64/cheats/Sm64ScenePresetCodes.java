package kerneln64.cheats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bundled codes authored from verified runtime scene captures. */
final class Sm64ScenePresetCodes {
    static List<CheatRepository.Entry> presets() {
        try(var stream=Sm64ScenePresetCodes.class.getResourceAsStream("/resources/sm64-scene-gfx.properties")) {
            if(stream==null)throw new IllegalStateException("missing bundled scene gfx catalog; rebuild the application");
            Properties data=new Properties();data.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
            List<CheatRepository.Entry> out=new ArrayList<>();
            for(int i=0;i<Integer.parseInt(data.getProperty("count"));i++)out.add(new CheatRepository.Entry(data.getProperty(i+".name"),data.getProperty(i+".notes"),data.getProperty(i+".codes"),false,data.getProperty(i+".group")));
            return out;
        }catch(IOException error){throw new UncheckedIOException(error);}
    }
}
