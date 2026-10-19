package aziaudio.playback;
import aziaudio.NativeAudio;
import aziaudio.config.AziAudioSettings;

import java.nio.ByteBuffer;
import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import plugin.AudioPlugin;

public class AudioCode {

	public static final int AI_STATUS_FIFO_FULL = 0x80000000;
	public static final int AI_STATUS_DMA_BUSY = 0x40000000;
	public static final int MI_INTR_AI = 0x04;

	static final int MAX_SIZE = 48000 * 2 * 2;
	static final int OUTPUT_SAMPLE_RATE = 44100;

	private AudioFormat format;
	SourceDataLine line;
	private FloatControl volumeControl;

	private int[] aiRegisters;
	private int[] miRegisters;
	private int MI_INTR_REG;
	private Runnable checkInterrupts;

	private ExecutorService exec;
	private java.util.concurrent.Future<?> playerTask;
	static final boolean USE_NATIVE = (Boolean.getBoolean("kerneln64.nativeAudio") || Boolean.getBoolean("jario64.nativeAudio"));

	byte[] m_Buffer = new byte[MAX_SIZE];
	int m_MaxBufferSize = MAX_SIZE;
	volatile int m_CurrentReadLoc = 0;
	volatile double m_CurrentReadLocFrac = 0.0;
	private volatile int m_CurrentWriteLoc = 0;
	volatile int m_BufferRemaining = 0;
	// Retained for compatibility with existing save-state schemas; no longer affects pitch.
	private double smoothedDeviation = 0.0;

	private byte[] m_AI_DMAPrimaryBuffer = null;
	private int m_AI_DMAPrimaryOffset = 0;
	volatile int m_AI_DMAPrimaryBytes = 0;

	private byte[] m_AI_DMASecondaryBuffer = null;
	private int m_AI_DMASecondaryOffset = 0;
	volatile int m_AI_DMASecondaryBytes = 0;

	volatile boolean running;
	int sampleRate;
	private int lastDivider = 0;
	
	// Configuration
	private boolean configSyncAudio = true;
	private boolean configForceSync = false;
	private boolean configAiEmulation = true;

	public AudioCode() {
		exec = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r);
			t.setName("N64 Audio Thread");
			t.setPriority(Thread.MAX_PRIORITY);
			t.setDaemon(true);
			return t;
		});
	}
	
	public void updateSettings() {
		configSyncAudio = AziAudioSettings.syncAudio;
		configForceSync = AziAudioSettings.forceOldSync;
		configAiEmulation = AziAudioSettings.aiEmulation;
		setVolume(AziAudioSettings.mute ? 0 : AziAudioSettings.volume);
		if (sampleRate > 0) {
			SetFrequency(sampleRate);
		}
	}

	private void openLine(int rate) {
		try {
			if (line != null && line.isOpen()) {
				if (format != null && format.getSampleRate() == rate) {
					return;
				}
				line.close();
			}

			// Little Endian for PC compatibility
			format = new AudioFormat(rate, 16, 2, true, false);
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				System.err.println("Audio Format not supported: " + format);
				return;
			}

			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format, 16384);
			
			if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				setVolume(AziAudioSettings.mute ? 0 : AziAudioSettings.volume);
			}
			
			line.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setVolume(int value) {
		if (volumeControl == null) return;
		
        // Gain and mute are applied to PCM for devices without MASTER_GAIN too.
        float dB = Math.max(volumeControl.getMinimum(), Math.min(volumeControl.getMaximum(), 0f));

		volumeControl.setValue(dB);
	}

	public boolean Initialize(AudioPlugin.AudioInfo info) {
		aiRegisters = info.aiRegisters;
		miRegisters = info.miRegisters;
		checkInterrupts = info.checkInterrupts;
		MI_INTR_REG = info.MI_INTR_REG;

		m_CurrentReadLoc = 0;
		m_CurrentReadLocFrac = 0.0;
		m_CurrentWriteLoc = 0;
		m_BufferRemaining = 0;
		m_MaxBufferSize = MAX_SIZE;
		m_AI_DMAPrimaryBytes = 0;
		m_AI_DMASecondaryBytes = 0;
		m_AI_DMAPrimaryBuffer = null;
		m_AI_DMASecondaryBuffer = null;
		smoothedDeviation = 0.0;

		running = true;
		sampleRate = 32000;
		lastDivider = 0;
		
		updateSettings();

		openLine(OUTPUT_SAMPLE_RATE);

		if ((USE_NATIVE && NativeAudio.isLoaded())) {
			NativeAudio.init(MAX_SIZE);
		}

		playerTask = exec.submit(new AudioPlayer(this));

		return true;
	}

	public void DeInitialize() {
		running = false;
		if (line != null) {
			try {
				line.stop();
				line.flush();
				line.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (playerTask != null) {
            try { playerTask.get(2, java.util.concurrent.TimeUnit.SECONDS); }
            catch (Exception e) { throw new IllegalStateException("Audio worker did not stop", e); }
            playerTask = null;
        }
        line = null;
        volumeControl = null;
	}

	public void SetFrequency(int rate) {
		if (rate <= 0)
			return;
		int divider = AziAudioSettings.bufferDivider;
		if (divider <= 0) divider = 10;
		if (rate == sampleRate && divider == lastDivider)
			return;

		sampleRate = rate;
		lastDivider = divider;
		m_MaxBufferSize = (rate / divider) * 8;
		if (m_MaxBufferSize > MAX_SIZE) {
			m_MaxBufferSize = MAX_SIZE;
		}
		if (m_MaxBufferSize < 4096) {
			m_MaxBufferSize = 4096;
		}

		synchronized (this) {
			openLine(OUTPUT_SAMPLE_RATE);
		}
	}

	void bufferAudio() {
		while ((m_BufferRemaining < m_MaxBufferSize) && (m_AI_DMAPrimaryBytes > 0 || m_AI_DMASecondaryBytes > 0)) {
			if (m_AI_DMAPrimaryBytes > 0 && m_AI_DMAPrimaryBuffer != null) {
				if ((USE_NATIVE && NativeAudio.isLoaded())) {
					int len = m_AI_DMAPrimaryBytes;
					byte[] chunkData = new byte[len];
					for (int i = 0; i < len; i += 4) {
						chunkData[i] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset + i + 1];
						chunkData[i + 1] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset + i];
						chunkData[i + 2] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset + i + 3];
						chunkData[i + 3] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset + i + 2];
					}
					NativeAudio.write(chunkData, 0, len);
					m_AI_DMAPrimaryOffset += len;
					m_AI_DMAPrimaryBytes = 0;
					m_BufferRemaining = NativeAudio.getRemainingBytes();
				} else {
					m_Buffer[m_CurrentWriteLoc] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset + 1];
					m_Buffer[m_CurrentWriteLoc + 1] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset];
					m_Buffer[m_CurrentWriteLoc + 2] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset + 3];
					m_Buffer[m_CurrentWriteLoc + 3] = m_AI_DMAPrimaryBuffer[m_AI_DMAPrimaryOffset + 2];

					m_CurrentWriteLoc += 4;
					m_AI_DMAPrimaryOffset += 4;
					m_CurrentWriteLoc %= MAX_SIZE;
					m_BufferRemaining += 4;
					m_AI_DMAPrimaryBytes -= 4;
				}
			}

			if (m_AI_DMAPrimaryBytes <= 0) {
				m_AI_DMAPrimaryBytes = m_AI_DMASecondaryBytes;
				m_AI_DMAPrimaryBuffer = m_AI_DMASecondaryBuffer;
				m_AI_DMAPrimaryOffset = 0;
				m_AI_DMASecondaryBytes = 0;
				m_AI_DMASecondaryBuffer = null;

				if (aiRegisters != null && AudioPlugin.AI_STATUS_REG < aiRegisters.length) {
					aiRegisters[AudioPlugin.AI_STATUS_REG] = AI_STATUS_DMA_BUSY;
				}

				if (miRegisters != null && checkInterrupts != null && MI_INTR_REG < miRegisters.length) {
					miRegisters[MI_INTR_REG] |= MI_INTR_AI;
					checkInterrupts.run();
				}

				if (m_AI_DMAPrimaryBytes <= 0) {
					if (aiRegisters != null && AudioPlugin.AI_STATUS_REG < aiRegisters.length) {
						aiRegisters[AudioPlugin.AI_STATUS_REG] = 0;
					}
				}
			}
		}
	}

	public int AddBuffer(ByteBuffer data, int offset, int length) {
		if (length <= 0) {
			if (aiRegisters != null && AudioPlugin.AI_STATUS_REG < aiRegisters.length) {
				return aiRegisters[AudioPlugin.AI_STATUS_REG];
			}
			return 0;
		}

		length &= ~0x7;

		byte[] start = new byte[length];
		data.duplicate().clear().position(offset).get(start);

        if (!running || line == null) {
            if (aiRegisters != null) aiRegisters[AudioPlugin.AI_STATUS_REG] = 0;
            if (miRegisters != null && checkInterrupts != null) {
                miRegisters[MI_INTR_REG] |= MI_INTR_AI;
                checkInterrupts.run();
            }
            return 0;
        }

        while (running) {
            synchronized (this) {
                bufferAudio();
                // The secondary DMA slot is a queue entry, not a replaceable buffer.
                // Backpressure also applies when optional audio sync is disabled.
                if (m_AI_DMASecondaryBytes <= 0
                        && (!configSyncAudio || m_BufferRemaining < m_MaxBufferSize)) {
                    m_AI_DMASecondaryBuffer = start;
                    m_AI_DMASecondaryOffset = 0;
                    m_AI_DMASecondaryBytes = length;
                    if (m_AI_DMAPrimaryBytes <= 0) {
                        m_AI_DMAPrimaryBuffer = m_AI_DMASecondaryBuffer;
                        m_AI_DMAPrimaryOffset = 0;
                        m_AI_DMAPrimaryBytes = m_AI_DMASecondaryBytes;
                        m_AI_DMASecondaryBuffer = null;
                        m_AI_DMASecondaryBytes = 0;
                    }
                    if (aiRegisters != null) {
                        aiRegisters[AudioPlugin.AI_STATUS_REG] = AI_STATUS_DMA_BUSY;
                        if (m_AI_DMAPrimaryBytes > 0 && m_AI_DMASecondaryBytes > 0)
                            aiRegisters[AudioPlugin.AI_STATUS_REG] |= AI_STATUS_FIFO_FULL;
                    }
                    bufferAudio();
                    return aiRegisters == null ? AI_STATUS_DMA_BUSY : aiRegisters[AudioPlugin.AI_STATUS_REG];
                }
            }
            try { Thread.sleep(1); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }
        return 0;
    }

    public static double stableInterpolate(double y0, double y1, double y2, double y3, double t) {
        return AudioSamples.cubic(y0, y1, y2, y3, t);
    }


	public void StopAudio() {
		if (line != null)
			line.stop();
	}

    public void flushOutput() {
        if (line != null && line.isOpen()) line.flush();
    }

	void StartAudio() {
		if (line != null)
			line.start();
	}

	public int GetReadStatus() {
		synchronized (this) {
			return m_AI_DMAPrimaryBytes & ~0x7;
		}
	}
}
