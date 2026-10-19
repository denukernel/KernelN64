package aziaudio.playback;

import aziaudio.NativeAudio;
import aziaudio.config.AziAudioSettings;
import javax.sound.sampled.SourceDataLine;

/** Host playback worker; synchronizes on AudioCode, which owns all saved ring/DMA state. */
final class AudioPlayer implements Runnable {
    private final AudioCode audio;
    AudioPlayer(AudioCode audio) { this.audio = audio; }

    public void run() {
        int MAX_SAMPLES = AudioCode.MAX_SIZE / 2;
        while (audio.running) {
                if (kerneln64.state.Rewind.enabled && kerneln64.state.Rewind.held) {
                    sleep(5);
                    continue;
                }
            if (audio.line == null) {
                sleep(1);
                continue;
            }

            int chunk = 0;
            byte[] tempBuf = null;

            synchronized (audio) {
                audio.bufferAudio();

                if (audio.sampleRate > 0) {
                        // Occupancy never modulates pitch. Explicit fast-forward consumes
                        // proportionally more guest audio, preserving its sequence at 2x.
                        double step = (double) audio.sampleRate * kerneln64.Main.SpeedMultiplier / AudioCode.OUTPUT_SAMPLE_RATE;

                    // Try to output 512 stereo samples (2048 bytes)
                    int outSamples = 512;
                    double requiredInputBytes = outSamples * step * 4 + 8;

                    if (audio.m_BufferRemaining >= requiredInputBytes) {
                        chunk = outSamples * 4;
                    } else if (audio.m_BufferRemaining > 8 && (audio.m_AI_DMAPrimaryBytes <= 0 && audio.m_AI_DMASecondaryBytes <= 0)) {
                        // Flush out remaining buffer samples
                        outSamples = (int) ((audio.m_BufferRemaining - 8) / (step * 4));
                        if (outSamples > 0) {
                            chunk = outSamples * 4;
                        }
                    } else {
                        outSamples = 0;
                    }

                    if (chunk > 0) {
                        tempBuf = new byte[chunk];
                        if ((AudioCode.USE_NATIVE && NativeAudio.isLoaded())) {
                            NativeAudio.readAndResample(tempBuf, outSamples, step);
                            audio.m_BufferRemaining = NativeAudio.getRemainingBytes();
                        } else {
                            int readStart = ((int) audio.m_CurrentReadLocFrac) & ~1;

                            for (int i = 0; i < outSamples; i++) {
                                double readPosSamples = audio.m_CurrentReadLocFrac;
                                int idxL0 = ((int) readPosSamples) & ~1;
                                double frac = (readPosSamples - idxL0) / 2.0;

                                // Retrieve 4 consecutive channel-aligned samples for Left and Right channels
                                int idxL_0 = (idxL0 - 2 + MAX_SAMPLES) % MAX_SAMPLES;
                                int idxL_1 = idxL0 % MAX_SAMPLES;
                                int idxL_2 = (idxL0 + 2) % MAX_SAMPLES;
                                int idxL_3 = (idxL0 + 4) % MAX_SAMPLES;

                                int idxR_0 = (idxL0 - 1 + MAX_SAMPLES) % MAX_SAMPLES;
                                int idxR_1 = (idxL0 + 1) % MAX_SAMPLES;
                                int idxR_2 = (idxL0 + 3) % MAX_SAMPLES;
                                int idxR_3 = (idxL0 + 5) % MAX_SAMPLES;

                                int offsetL0 = idxL_0 * 2;
                                int offsetL1 = idxL_1 * 2;
                                int offsetL2 = idxL_2 * 2;
                                int offsetL3 = idxL_3 * 2;

                                int offsetR0 = idxR_0 * 2;
                                int offsetR1 = idxR_1 * 2;
                                int offsetR2 = idxR_2 * 2;
                                int offsetR3 = idxR_3 * 2;

                                // Reconstruct 16-bit signed samples (interleaved stereo)
                                short L0 = (short) (((audio.m_Buffer[offsetL0 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetL0] & 0xFF));
                                short L1 = (short) (((audio.m_Buffer[offsetL1 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetL1] & 0xFF));
                                short L2 = (short) (((audio.m_Buffer[offsetL2 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetL2] & 0xFF));
                                short L3 = (short) (((audio.m_Buffer[offsetL3 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetL3] & 0xFF));

                                short R0 = (short) (((audio.m_Buffer[offsetR0 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetR0] & 0xFF));
                                short R1 = (short) (((audio.m_Buffer[offsetR1 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetR1] & 0xFF));
                                short R2 = (short) (((audio.m_Buffer[offsetR2 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetR2] & 0xFF));
                                short R3 = (short) (((audio.m_Buffer[offsetR3 + 1] & 0xFF) << 8) | (audio.m_Buffer[offsetR3] & 0xFF));

                                // Hermite interpolation resampler per channel
                                double interpL = AziAudioSettings.interpolate(L0, L1, L2, L3, frac);
                                double interpR = AziAudioSettings.interpolate(R0, R1, R2, R3, frac);

                                // Clamp results to prevent digital clipping/distortion
                                if (interpL > 32767.0) interpL = 32767.0;
                                else if (interpL < -32768.0) interpL = -32768.0;

                                if (interpR > 32767.0) interpR = 32767.0;
                                else if (interpR < -32768.0) interpR = -32768.0;

                                short L = (short) interpL;
                                short R = (short) interpR;

                                int outOffset = i * 4;
                                tempBuf[outOffset] = (byte) (L & 0xFF);
                                tempBuf[outOffset + 1] = (byte) ((L >> 8) & 0xFF);
                                tempBuf[outOffset + 2] = (byte) (R & 0xFF);
                                tempBuf[outOffset + 3] = (byte) ((R >> 8) & 0xFF);

                                // Advance by 2 samples (1 stereo frame) scaled by step
                                audio.m_CurrentReadLocFrac += step * 2.0;
                                if (audio.m_CurrentReadLocFrac >= MAX_SAMPLES) {
                                    audio.m_CurrentReadLocFrac -= MAX_SAMPLES;
                                }
                            }

                            int readEnd = ((int) audio.m_CurrentReadLocFrac) & ~1;
                            int samplesConsumed = readEnd - readStart;
                            if (samplesConsumed < 0) {
                                samplesConsumed += MAX_SAMPLES;
                            }
                            int bytesConsumed = samplesConsumed * 2;

                            audio.m_CurrentReadLoc = (readEnd * 2) % AudioCode.MAX_SIZE;
                            audio.m_BufferRemaining -= bytesConsumed;
                            if (audio.m_BufferRemaining < 0) {
                                audio.m_BufferRemaining = 0;
                            }
                        }
                    }
                }
            }

            if (chunk > 0 && tempBuf != null) {
                    AziAudioSettings.processOutput(tempBuf, chunk);
                SourceDataLine output = audio.line;
                    if (audio.running && output != null && output.isOpen()) output.write(tempBuf, 0, chunk);
            } else {
                sleep(1);
            }
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
