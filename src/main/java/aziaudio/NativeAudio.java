package aziaudio;

public class NativeAudio {
    private static boolean loaded = false;
    static {
        try {
            java.io.File dll = new java.io.File("AziAudioJNI.dll");
            if (dll.exists()) {
                System.load(dll.getAbsolutePath());
                loaded = true;
            } else {
                System.loadLibrary("AziAudioJNI");
                loaded = true;
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load native AziAudioJNI library, falling back to Java implementation: " + e.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Initializes the native circular buffer.
     * @param maxSizeBytes physical size of the buffer in bytes
     */
    public static native void init(int maxSizeBytes);

    /**
     * Writes raw Little Endian bytes into the native buffer.
     * @param data byte array of input samples
     * @param offset start offset
     * @param length number of bytes to write
     */
    public static native void write(byte[] data, int offset, int length);

    /**
     * Resamples from the native circular buffer using Hermite cubic interpolation and returns resampled bytes.
     * @param outBuffer destination byte array for the output (stereo 16-bit)
     * @param outSamples number of stereo frames to generate (each frame is 4 bytes)
     * @param step the resampling step (sampleRate_eff / OUTPUT_SAMPLE_RATE)
     * @return actual bytes written to outBuffer
     */
    public static native int readAndResample(byte[] outBuffer, int outSamples, double step);

    /**
     * Returns the remaining bytes in the native circular buffer.
     */
    public static native int getRemainingBytes();
}
