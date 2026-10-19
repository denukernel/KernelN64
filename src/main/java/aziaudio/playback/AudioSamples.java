package aziaudio.playback;

import static aziaudio.config.AziAudioSettings.*;

/** PCM interpolation, volume, balance and channel processing. */
public final class AudioSamples {
    private AudioSamples() {}
    public static double cubic(double y0, double y1, double y2, double y3, double t) {
        double a = -0.5 * y0 + 1.5 * y1 - 1.5 * y2 + 0.5 * y3;
        double b = y0 - 2.5 * y1 + 2.0 * y2 - 0.5 * y3;
        double c = -0.5 * y0 + 0.5 * y2;
        double d = y1;
        return a * t * t * t + b * t * t + c * t + d;
    }
    public static double interpolate(short previous, short first, short second, short next, double fraction) {
        return switch (interpolation) {
            case 1 -> first + (second - first) * fraction;
            case 2 -> fraction < 0.5 ? first : second;
            default -> cubic(previous, first, second, next, fraction);
        };
    }

    public static void processOutput(byte[] pcm, int length) {
        boolean silent = mute || (muteFastForward && kerneln64.Main.SpeedMultiplier > 1);
        double gain = silent ? 0 : volume / 100.0;
        int pan = balance;
        double leftGain = gain * (pan > 0 ? 1 - pan / 100.0 : 1);
        double rightGain = gain * (pan < 0 ? 1 + pan / 100.0 : 1);
        boolean mix = mono, swap = swapChannels;
        for (int i = 0; i + 3 < length; i += 4) {
            int left = (short)((pcm[i] & 255) | (pcm[i + 1] << 8));
            int right = (short)((pcm[i + 2] & 255) | (pcm[i + 3] << 8));
            if (swap) { int temp = left; left = right; right = temp; }
            if (mix) left = right = (left + right) / 2;
            left = (int)Math.round(left * leftGain); right = (int)Math.round(right * rightGain);
            pcm[i] = (byte)left; pcm[i + 1] = (byte)(left >> 8);
            pcm[i + 2] = (byte)right; pcm[i + 3] = (byte)(right >> 8);
        }
    }
}
