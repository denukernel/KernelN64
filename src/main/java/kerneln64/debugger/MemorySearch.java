package kerneln64.debugger;
import kerneln64.memory.Memory;
import kerneln64.Main;


import java.util.BitSet;

/** Unsigned, aligned, big-endian RDRAM searches. No live buffers escape the CPU. */
public final class MemorySearch {
    private final int width;
    private byte[] previous;
    private final BitSet candidates = new BitSet();
    public MemorySearch(byte[] initial, int width) {
        if (width != 1 && width != 2 && width != 4) throw new IllegalArgumentException("Choose 8, 16 or 32 bits.");
        this.width = width; previous = initial.clone(); candidates.set(0, initial.length / width);
    }
    public int count() { return candidates.cardinality(); }
    public int width() { return width; }
    public int[] addresses(int limit) {
        int[] result = new int[Math.min(limit, count())]; int i = 0;
        for (int bit = candidates.nextSetBit(0); bit >= 0 && i < result.length; bit = candidates.nextSetBit(bit + 1)) result[i++] = bit * width;
        return result;
    }
    public static long value(byte[] data, int address, int width) {
        long result = 0; for (int i = 0; i < width; i++) result = (result << 8) | (data[address + i] & 255); return result;
    }
    public void refine(byte[] current, String comparison, long expected) {
        if (current.length != previous.length) throw new IllegalArgumentException("Memory size changed; start a new search.");
        for (int bit = candidates.nextSetBit(0); bit >= 0; bit = candidates.nextSetBit(bit + 1)) {
            int address = bit * width; long now = value(current, address, width), old = value(previous, address, width);
            boolean keep = switch (comparison) {
                case "Equals value" -> now == expected;
                case "Not equal to value" -> now != expected;
                case "Less than value" -> now < expected;
                case "Greater than value" -> now > expected;
                case "Changed" -> now != old;
                case "Unchanged" -> now == old;
                case "Increased" -> now > old;
                case "Decreased" -> now < old;
                default -> throw new IllegalArgumentException("Unknown comparison");
            };
            if (!keep) candidates.clear(bit);
        }
        previous = current.clone();
    }
    public static long parse(String text, int width) {
        String value = text.trim();
        long number = value.startsWith("0x") || value.startsWith("0X") ? Long.parseUnsignedLong(value.substring(2), 16) : Long.parseLong(value);
        long max = width == 4 ? 0xFFFFFFFFL : (1L << (width * 8)) - 1;
        if (number < 0 || number > max) throw new IllegalArgumentException("Value is outside the selected unsigned range.");
        return number;
    }
    public static String cheat(int address, int width, long value) {
        if (width == 1) return String.format("%08X %04X", 0x80000000 | address, value);
        if (width == 2) return String.format("%08X %04X", 0x81000000 | address, value);
        return String.format("%08X %04X%n%08X %04X", 0x81000000 | address, value >>> 16, 0x81000000 | (address + 2), value & 65535);
    }
}
