package kerneln64.state;
import gln64j.GLN64jPlugin;
import kerneln64.cheats.CheatEngine;
import kerneln64.hardware.Rsp;
import kerneln64.Main;


import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Versioned local states. Call only at a CPU instruction boundary. No Java deserialization. */
public final class SaveStates {
    private static final int MAGIC = 0x4A363453, VERSION = 3;
    private SaveStates() {}

    public static String romHash(ByteBuffer rom) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(rom.duplicate().clear());
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }

    private record Leaf(String name, Object owner, Field field) {
        Object get() throws ReflectiveOperationException { return field.get(owner); }
        boolean variable() { return name.contains("m_AI_DMAPrimaryBuffer") || name.contains("m_AI_DMASecondaryBuffer"); }
    }

    // Only data owned by these emulator objects is traversed. Callbacks, plugins, threads,
    // file handles and native graphics resources are deliberately outside the state format.
    private static void collect(List<Leaf> leaves, String path, Object object, Class<?> type, boolean statics)
            throws ReflectiveOperationException {
        Field[] fields = type.getDeclaredFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (field.isSynthetic() || Modifier.isStatic(modifiers) != statics
                    || Modifier.isFinal(modifiers) || field.getName().equals("running")) continue;
            if (Set.of("cachedOpcodes", "cachedOp", "tmpFPR", "cacheInstructions").contains(field.getName())) continue;
            if ((type == gln64j.rdp.textures.TextureCache.class || type == kernel64gl.engine.rdp.textures.TextureCache.class) && !Set.of("texture", "tiles", "texRect", "textureImage",
                    "loadType", "paletteCRC16", "paletteCRC256", "textureMode", "tmem", "bgImage", "stateLoadTileIndex")
                    .contains(field.getName())) continue;
            Class<?> ft = field.getType();
            field.setAccessible(true);
            String key = path + "." + field.getName();
            if (ft.isPrimitive() || (ft.isArray() && dataArray(ft))
                    || ft == ByteBuffer.class) {
                if (field.getName().equals("rom")) continue;
                leaves.add(new Leaf(key, object, field));
            } else {
                Object value = field.get(object);
                if (value == null) continue;
                if (ft.isArray()) {
                    for (int i = 0; i < Array.getLength(value); i++) {
                        Object item = Array.get(value, i);
                        if (item != null && item.getClass().getEnclosingClass() == type)
                            collect(leaves, key + "[" + i + "]", item, item.getClass(), false);
                    }
                } else if (ft.getEnclosingClass() == type)
                    collect(leaves, key, value, ft, false);
            }
        }
    }

    private static boolean dataArray(Class<?> type) {
        while (type.isArray()) type = type.getComponentType();
        return type.isPrimitive();
    }

    private static List<Leaf> leaves() throws ReflectiveOperationException {
        List<Leaf> result = new ArrayList<>();
        Object[] roots = { Main.cpu, Main.cpu.cop0, Main.cpu.cop1, Main.regs, Main.mem,
                Main.video, Main.audio, Main.rsp, Main.pif, Main.dma, Main.eeprom, Main.mempak, Main.flashRam };
        for (Object root : roots) if (root != null) collect(result, stateTypeName(root.getClass()), root, root.getClass(), false);
        for (Class<?> type : new Class<?>[] { aziaudio.hle.HLEMain.class, aziaudio.hle.abi1.ABI1.class, aziaudio.hle.abi2.ABI2.class, aziaudio.hle.abi3.ABI3.class })
            collect(result, "aziaudio." + type.getSimpleName(), null, type, true);
        if (Main.audio.audioPlugin instanceof aziaudio.AziAudioPlugin) {
            Object sound = aziaudio.AziAudioPlugin.stateAudio();
            collect(result, "audioPlayback", sound, sound.getClass(), false);
        }
        collectGraphics(result);
        return result;
    }
    private static void collectGraphics(List<Leaf> fields)throws ReflectiveOperationException {
        Object gsp=GraphicsState.gsp(),gdp=GraphicsState.gdp(),cache=GraphicsState.cache();
        if(gsp!=null)collect(fields,"gsp",gsp,gsp.getClass(),false);
        if(gdp!=null)collect(fields,"gdp",gdp,gdp.getClass(),false);
        if(cache!=null)collect(fields,"textureCache",cache,cache.getClass(),false);
    }
    // Field labels are part of v1-v3 state files, not the current Java namespace.
    // Keep their original spelling so package refactors do not invalidate saves.
    private static String stateTypeName(Class<?> type) {
        String name=type.getName();
        return name.startsWith("kerneln64.")?"jario64."+(name.startsWith("kerneln64.savechips.") ? "savechips." : "")+type.getSimpleName():name;
    }

    private static Object lock() {
        return Main.audio.audioPlugin instanceof aziaudio.AziAudioPlugin
                ? aziaudio.AziAudioPlugin.stateAudio() : Main.cpu;
    }

    private static void requireRom() throws IOException {
        if (Main.currentRomHash == null) throw new IOException("Open a ROM first.");
        if ((Boolean.getBoolean("kerneln64.nativeAudio") || Boolean.getBoolean("jario64.nativeAudio"))) throw new IOException("Save states require the default Java audio backend.");
    }

    public static void save(Path destination) throws IOException {
        requireRom();
        Path absolute = destination.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temp = Files.createTempFile(absolute.getParent(), "jario-state-", ".tmp");
        try {
            Files.write(temp, capture());
            try { Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    /** Capture at a CPU instruction boundary. Also used by the in-memory rewind ring. */
    public static byte[] capture() throws IOException {
        requireRom();
        d3d11.framebuffer.D3DFramebuffers.capture();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            synchronized (lock()) {
                if (Main.eeprom != null) Main.eeprom.prepareState();
                if (Main.mempak != null) Main.mempak.prepareState();
                boolean mode32 = Main.cpu.cop1.mode32;
                Main.cpu.cop1.setMode32(false);
                try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes) { { def.setLevel(1); } })) {
                    out.writeInt(MAGIC);
                    out.writeInt(VERSION);
                    out.writeUTF(Main.currentRomHash);
                    out.writeUTF(Main.audio.audioPlugin == null ? "none" : Main.audio.audioPlugin.getClass().getName());
                    out.writeUTF(Main.video.gfxPlugin == null ? "none" : Main.video.gfxPlugin.getClass().getName());
                    out.writeBoolean(mode32);
                    List<Leaf> leaves = leaves();
                    out.writeInt(leaves.size());
                    IdentityHashMap<Object,Integer> shared = new IdentityHashMap<>();
                    for (int index = 0; index < leaves.size(); index++) {
                        Leaf leaf = leaves.get(index);
                        out.writeUTF(leaf.name);
                        Object object = leaf.get();
                        if (object instanceof ByteBuffer || object instanceof byte[]) {
                            Integer previous = shared.putIfAbsent(object, index);
                            if (previous != null) { out.writeInt(-previous - 1); continue; }
                        }
                        ByteArrayOutputStream valueBytes = new ByteArrayOutputStream();
                        writeValue(new DataOutputStream(valueBytes), leaf.field.getType(), leaf.get());
                        out.writeInt(valueBytes.size());
                        valueBytes.writeTo(out);
                    }
                    Main.cheats.writeUndo(out,Main.mem.RDRAM);
                } catch (ReflectiveOperationException e) { throw new IOException("Cannot capture emulator state", e); }
                finally { Main.cpu.cop1.setMode32(mode32); }
            }
        return bytes.toByteArray();
    }

    public static void load(Path source) throws IOException {
        restore(Files.readAllBytes(source));
        Rewind.clear();
    }

    public static void restore(byte[] state) throws IOException {
        requireRom();
        synchronized (lock()) {
            boolean previousMode = Main.cpu.cop1.mode32;
            Main.cpu.cop1.setMode32(false);
            boolean restored = false;
            try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(state)))) {
                if(in.readInt()!=MAGIC)throw new IOException("Unsupported save-state format.");
                int version=in.readInt();if(version<1 || version>VERSION)throw new IOException("Unsupported save-state format.");
                if (!in.readUTF().equals(Main.currentRomHash)) throw new IOException("This state belongs to a different ROM.");
                String audio = Main.audio.audioPlugin == null ? "none" : Main.audio.audioPlugin.getClass().getName();
                String graphics = Main.video.gfxPlugin == null ? "none" : Main.video.gfxPlugin.getClass().getName();
                String savedAudio = in.readUTF(), savedGraphics = in.readUTF();
                if (!savedAudio.equals(audio) || !graphicsCompatible(savedGraphics, graphics)) throw new IOException("State plugins differ from the active plugins.");
                boolean mode32 = in.readBoolean();
                List<Leaf> leaves = leaves();
                if (in.readInt() != leaves.size()) throw new IOException("Incompatible state schema.");
                List<Object> values = new ArrayList<>();
                long total = 0;
                for (Leaf leaf : leaves) {
                    if (!in.readUTF().equals(leaf.name)) throw new IOException("Incompatible state field: " + leaf.name);
                    int length = in.readInt();
                    if (version >= 3 && length < 0) {
                        int reference = -length - 1;
                        if (reference < 0 || reference >= values.size()) throw new IOException("Invalid shared state reference");
                        Object shared = values.get(reference);
                        if (shared == null || !leaf.field.getType().isInstance(shared)) throw new IOException("Invalid shared state type");
                        Object current = leaf.get();
                        if (current instanceof ByteBuffer b && shared instanceof ByteBuffer saved && b.capacity() != saved.capacity())
                            throw new IOException("State RAM size differs from configured RDRAM. Select the matching RAM size and restart.");
                        if(current!=null && current.getClass().isArray() && !leaf.variable() && Array.getLength(current)!=Array.getLength(shared))
                            throw new IOException("Invalid shared state array length: "+leaf.name);
                        values.add(shared); continue;
                    }
                    total += length;
                    if (length < 0 || length > 65 * 1024 * 1024 || total > 768 * 1024 * 1024) throw new IOException("Invalid state size.");
                    byte[] bytes = in.readNBytes(length);
                    if (bytes.length != length) throw new EOFException("Truncated state.");
                    DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
                    Object value = readValue(data, leaf.field.getType());
                    if (data.available() != 0) throw new IOException("Invalid field data.");
                    Object current = leaf.get();
                    if (current != null && current.getClass().isArray() && !leaf.variable()
                            && (value == null || Array.getLength(current) != Array.getLength(value)))
                        throw new IOException("Invalid array length: " + leaf.name);
                    if (current instanceof ByteBuffer buffer
                            && (!(value instanceof ByteBuffer saved) || buffer.capacity() != saved.capacity()))
                        throw new IOException("State RAM size differs from configured RDRAM: " + leaf.name + ". Select the matching RAM size and restart.");
                    values.add(value);
                }
                CheatEngine.UndoState undo=version>=2?CheatEngine.readUndo(in):null;
                if (in.read() != -1) throw new IOException("Unexpected state data."); // Also verifies GZIP checksum before mutation.
                if (Main.eeprom != null) Main.eeprom.prepareState();
                if (Main.mempak != null) Main.mempak.prepareState();
                for (int i = 0; i < leaves.size(); i++) {
                    Leaf leaf = leaves.get(i);
                    Object current = leaf.get(), value = values.get(i);
                    if (current instanceof ByteBuffer buffer && value instanceof ByteBuffer saved) {
                        buffer.clear().put(saved.duplicate().clear());
                        buffer.limit(saved.limit()).position(saved.position());
                    } else if (current != null && value != null && current.getClass().isArray()
                            && Array.getLength(current) == Array.getLength(value))
                        System.arraycopy(value, 0, current, 0, Array.getLength(value));
                    else leaf.field.set(leaf.owner, value);
                }
                Main.cpu.cop1.setMode32(mode32);
                d3d11.framebuffer.D3DFramebuffers.restored();
                restored = true;
                Main.cpu.invalidateAllCode();
                Main.cheats.restoreUndo(undo,Main.mem.RDRAM);
                Main.video.resetTimer();
                if (Main.audio.audioPlugin instanceof aziaudio.AziAudioPlugin) aziaudio.AziAudioPlugin.stateAudio().flushOutput();
                GraphicsState.restored();
                kerneln64.scripting.LuaScripts.stateRestored();
                Main.rsp.lastValidAudioTaskTime = System.currentTimeMillis();
            } catch (ReflectiveOperationException e) { throw new IOException("Cannot restore emulator state", e); }
            finally { if (!restored) Main.cpu.cop1.setMode32(previousMode); }
        }
    }

    /** Only guest HLE graphics data; CPU/device state and host GPU objects stay untouched. */
    public static Map<String,byte[]> graphicsState() throws IOException {
        try {
            Map<String,byte[]> result=new LinkedHashMap<>();
            List<Leaf> fields=new ArrayList<>();
            collectGraphics(fields);


            for(Leaf leaf:fields) {
                if(leaf.name.endsWith(".rdram") || leaf.name.endsWith(".dmem"))continue;
                ByteArrayOutputStream data=new ByteArrayOutputStream();
                writeValue(new DataOutputStream(data),leaf.field.getType(),leaf.get());result.put(leaf.name,data.toByteArray());
            }
            return result;
        } catch(ReflectiveOperationException error){throw new IOException(error);}
    }
    public static void graphicsState(Map<String,byte[]> state) throws IOException {
        try {
            List<Leaf> fields=new ArrayList<>();
            collectGraphics(fields);


            for(Leaf leaf:fields) {
                byte[] data=state.get(leaf.name);if(data==null)continue;
                Object value=readValue(new DataInputStream(new ByteArrayInputStream(data)),leaf.field.getType()),current=leaf.get();
                if(current instanceof ByteBuffer buffer && value instanceof ByteBuffer saved) {
                    buffer.clear().put(saved.duplicate().clear());
                    buffer.limit(saved.limit()).position(saved.position());
                }
                else if(current!=null && value!=null && current.getClass().isArray() && Array.getLength(current)==Array.getLength(value))System.arraycopy(value,0,current,0,Array.getLength(value));
                else leaf.field.set(leaf.owner,value);
            }
            GraphicsState.restored();
        } catch(ReflectiveOperationException error){throw new IOException(error);}
    }

    private static boolean graphicsCompatible(String saved, String active) {
        if (saved.equals(active)) return true;
        Set<String> sharedHle = Set.of("gln64j.GLN64jPlugin", "d3d11.D3D11Plugin", "kernel64gl.Kernel64GLPlugin");
        return sharedHle.contains(saved) && sharedHle.contains(active);
    }

    private static void writeValue(DataOutputStream out, Class<?> type, Object value) throws IOException {
        if (type == ByteBuffer.class) {
            out.writeBoolean(value != null);
            if (value == null) return;
            ByteBuffer buffer = (ByteBuffer) value;
            out.writeInt(buffer.capacity()); out.writeInt(buffer.position()); out.writeInt(buffer.limit());
            byte[] data = new byte[buffer.capacity()]; buffer.duplicate().clear().get(data); out.write(data);
        } else if (type.isArray()) {
            out.writeInt(value == null ? -1 : Array.getLength(value));
            if (value instanceof byte[] bytes) out.write(bytes);
            else if (value != null) for (int i = 0; i < Array.getLength(value); i++) writeValue(out, type.getComponentType(), Array.get(value, i));
        } else if (type == boolean.class) out.writeBoolean((boolean) value);
        else if (type == byte.class) out.writeByte((byte) value);
        else if (type == short.class) out.writeShort((short) value);
        else if (type == char.class) out.writeChar((char) value);
        else if (type == int.class) out.writeInt((int) value);
        else if (type == long.class) out.writeLong((long) value);
        else if (type == float.class) out.writeFloat((float) value);
        else if (type == double.class) out.writeDouble((double) value);
        else throw new IOException("Unsupported state type");
    }

    private static Object readValue(DataInputStream in, Class<?> type) throws IOException {
        if (type == ByteBuffer.class) {
            if (!in.readBoolean()) return null;
            int capacity = in.readInt(), position = in.readInt(), limit = in.readInt();
            if (capacity < 0 || capacity > 0x4000000 || position < 0 || position > limit || limit > capacity) throw new IOException("Invalid buffer.");
            byte[] bytes = in.readNBytes(capacity);
            if (bytes.length != capacity) throw new EOFException();
            return ByteBuffer.wrap(bytes).limit(limit).position(position);
        }
        if (type.isArray()) {
            int length = in.readInt();
            if (length == -1) return null;
            if (length < 0 || length > 0x4000000 || length > in.available()) throw new IOException("Invalid array.");
            if (type == byte[].class) {
                byte[] bytes = in.readNBytes(length);
                if (bytes.length != length) throw new EOFException();
                return bytes;
            }
            Object array = Array.newInstance(type.getComponentType(), length);
            for (int i = 0; i < length; i++) Array.set(array, i, readValue(in, type.getComponentType()));
            return array;
        }
        if (type == boolean.class) return in.readBoolean();
        if (type == byte.class) return in.readByte();
        if (type == short.class) return in.readShort();
        if (type == char.class) return in.readChar();
        if (type == int.class) return in.readInt();
        if (type == long.class) return in.readLong();
        if (type == float.class) return in.readFloat();
        if (type == double.class) return in.readDouble();
        throw new IOException("Unsupported state type");
    }
}
