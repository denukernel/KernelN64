package kerneln64.state;
import kerneln64.Main;


import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Host-only slot selection/names. Guest state files and their schema stay unchanged. */
public final class StateSlots {
    public static final int COUNT = 10;
    private static StateSlots cached;
    private final File rom;
    private final String hash;
    private final Path saveRoot, directory, metadata;
    private Properties settings = new Properties();
    public StateSlots(Path saveRoot, File rom, String hash) throws IOException {
        if (rom == null || hash == null || !hash.matches("[0-9a-fA-F]{64}"))
            throw new IllegalArgumentException("Open a ROM before selecting a state slot.");
        this.saveRoot=saveRoot.toAbsolutePath().normalize();this.rom=rom;this.hash=hash.toLowerCase(Locale.ROOT);
        directory=readableDirectory(this.saveRoot.resolve("States"));
        metadata=directory.resolve("slots.properties");
        if(Files.exists(metadata))try(var in=Files.newInputStream(metadata)){settings.load(in);}
        else {
            // Copy old hash-based slots once. Originals remain available for older builds.
            Path legacy=this.saveRoot.resolve("States").resolve(this.hash);
            Path legacyMetadata=legacy.resolve("slots.properties");
            if(Files.exists(legacyMetadata))try(var in=Files.newInputStream(legacyMetadata)){settings.load(in);}
            String savedHash=settings.getProperty("rom.sha256",this.hash);
            if(!savedHash.equalsIgnoreCase(this.hash))throw new IOException("Legacy state folder belongs to a different ROM.");
            Files.createDirectories(directory);
            for(int slot=1;slot<=COUNT;slot++) {
                String filename=String.format(Locale.ROOT,"slot%02d.j64state",slot);
                Path source=legacy.resolve(filename);
                if(Files.exists(source))Files.copy(source,directory.resolve(filename));
            }
            persist(copy());
        }
    }
    public static synchronized StateSlots current() throws IOException {
        Path root=Path.of(Main.AUTO_SAVE_DIR).toAbsolutePath().normalize();
        if(cached==null || !Objects.equals(cached.rom,Main.currentRomFile)
                || !Objects.equals(cached.hash,Main.currentRomHash) || !cached.saveRoot.equals(root))
            cached=new StateSlots(root,Main.currentRomFile,Main.currentRomHash);
        return cached;
    }
    public synchronized int selected() {
        try {int n=Integer.parseInt(settings.getProperty("selected","1"));return n>=0&&n<=COUNT?n:1;}
        catch(NumberFormatException e){return 1;}
    }
    public synchronized String name(int slot) {check(slot);return settings.getProperty("slot."+slot+".name","");}
    public synchronized String label(int slot) {
        String base=slot==0?"Legacy quick state":"Slot "+slot;
        String name=name(slot);return name.isBlank()?base:base+" — "+name;
    }
    public Path path(int slot) {
        check(slot);
        return slot==0?saveRoot.resolve(rom.getName()+".j64state")
            :directory.resolve(String.format(Locale.ROOT,"slot%02d.j64state",slot));
    }
    public synchronized void select(int slot) throws IOException {
        check(slot);Properties next=copy();next.setProperty("selected",Integer.toString(slot));persist(next);
    }
    public synchronized void rename(int slot,String name) throws IOException {
        check(slot);String value=Objects.requireNonNull(name).strip();
        if(value.length()>80 || value.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Use up to 80 characters without line breaks or control characters.");
        Properties next=copy();
        if(value.isEmpty())next.remove("slot."+slot+".name");else next.setProperty("slot."+slot+".name",value);
        persist(next);
    }
    private Properties copy(){Properties p=new Properties();p.putAll(settings);return p;}
    private Path readableDirectory(Path states) throws IOException {
        String name=rom.getName().replaceAll("[\\x00-\\x1f<>:\"/\\\\|?*]","_")
            .replaceAll("[ .]+$","");
        if(name.isBlank())name="rom";
        if(name.length()>120)name=name.substring(0,120).replaceAll("[ .]+$","");
        if(name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?"))name="_"+name;
        // Only same-name ROMs with different contents need an identity suffix.
        for(String suffix:new String[]{""," ["+hash.substring(0,12)+"]"," ["+hash+"]"}) {
            Path candidate=states.resolve(name+suffix);
            if(!Files.exists(candidate))return candidate;
            Path info=candidate.resolve("slots.properties");
            if(Files.isRegularFile(info)) {
                Properties stored=new Properties();
                try(var in=Files.newInputStream(info)){stored.load(in);}
                if(hash.equalsIgnoreCase(stored.getProperty("rom.sha256")))return candidate;
            }
        }
        throw new IOException("Cannot find an unused save-state folder for "+rom.getName());
    }
    private void persist(Properties next) throws IOException {
        next.setProperty("rom.name",rom.getName());next.setProperty("rom.sha256",hash);
        Files.createDirectories(directory);Path temp=Files.createTempFile(directory,"slots-",".tmp");
        try {
            try(var out=Files.newOutputStream(temp)){next.store(out,"KernelN64 save-state slot names and selection");}
            try{Files.move(temp,metadata,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,metadata,StandardCopyOption.REPLACE_EXISTING);}
            settings=next;
        }finally{Files.deleteIfExists(temp);}
    }
    private static void check(int slot){if(slot<0||slot>COUNT)throw new IllegalArgumentException("Invalid state slot");}
}
