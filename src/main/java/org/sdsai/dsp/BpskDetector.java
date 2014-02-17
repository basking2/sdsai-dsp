package org.sdsai.dsp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

/**
 * Detect and decode BPSK modulated audio signals.
 *
 * This class uses default settings from {@link BpskGenerator} when possible.
 */
public class BpskDetector {

    /**
     * The target frequency to generate or decode a PSK31 signal on.
     */
    private double hz;

    /**
     * The audio sample rate. Typically 11025, 22050, or 44100.
     */
    private int sampleRate;

    /**
     * The rate that symbols are generated per second.
     *
     * This is used to compute how many samples to populate to represent a symbol.
     */
    private double symbolsPerSecond;

    /**
     * Filter buffer stores previous signal data for filtering and inversion detection.
     *
     * This contains exactly two symbols-worth of data.
     */
    private short[] buffer;

    /**
     * The count of the samples currently making up the last reported symbol.
     *
     * When this exceeds samplesPerSymbol a 1 is reported and this is set to 0.
     */
    private int sampleCount;

    /**
     * How many audio samples are gathered for each PSK symbol.
     */
    private int samplesPerSymbol;

    /**
     * The size of a single sample is currently always 2 bytes.
     */
    private static final int sampleSize = 16;

    private final Goertzel signalDetector;

    private final Goertzel.Result signalDetectorResult;

    /**
     * The number of signal samples necessary to do any work.
     *
     * This should be less than the number of {@link #samplesPerSymbol}
     * but larger than two samples per wave form.
     */
    private final int binSize;

    /**
     * The current phase of the signal as detected by the {@link #signalDetector}.
     */
    private double phase;

    /**
     * The symbol detected in the previous match of {@link #signalDetector} or by {@link #sampleCount}.
     *
     * If a symbol is matched twice in a row it is written out.
     */
    private int lastSymbol;

    /**
     * Used to help find the peak of a signal.
     */
    private double lastMagnitude;

    /**
     * Constructor with sensible defaults.
     * <ul>
     * <li>{@link BpskGenerator#DEFAULT_FREQUENCY}</li>
     * <li>{@link BpskGenerator#DEFAULT_SAMPLE_RATE}</li>
     * <li>{@link BpskGenerator#PSK31_SYMBOLS_PER_SECOND}</li>
     * </ul>
     */
    public BpskDetector() {
        this(
            BpskGenerator.DEFAULT_FREQUENCY,
            BpskGenerator.DEFAULT_SAMPLE_RATE,
            BpskGenerator.PSK31_SYMBOLS_PER_SECOND);
    }

        /**
     * Constructor.
     *
     * @param hz Frequency of the detected tone.
     * @param sampleRate The audio sample rate.
     */
    public BpskDetector(final double hz, final int sampleRate) {
        this(hz, sampleRate, BpskGenerator.PSK31_SYMBOLS_PER_SECOND);
    }

    /**
     * Constructor.
     *
     * @param hz Frequency of the detected tone.
     * @param sampleRate The audio sample rate.
     * @param symbolsPerSecond How many symbols per second. For PSK31 this is {@link BpskGenerator#PSK31_SYMBOLS_PER_SECOND}.
     */
    public BpskDetector(final double hz, final int sampleRate, final double symbolsPerSecond) {
        this.hz                 = hz;
        this.sampleRate         = sampleRate;
        this.symbolsPerSecond   = symbolsPerSecond;
        this.samplesPerSymbol   = (int)(this.sampleRate / this.symbolsPerSecond);

        /* sampleRate / hz = number of samples for 1 full cycle of the wave. */
        this.buffer        = new short[samplesPerSymbol * 2];

        /* Build filtering / detecting algorith object. */
        this.binSize              = (int)(sampleRate / hz)*2;
        this.signalDetector       = new Goertzel(hz, sampleRate, this.binSize);
        this.signalDetectorResult = new Goertzel.Result();

        this.phase = Double.NaN;

        this.sampleCount = 0;
        this.lastSymbol = 1;
    }

    /**
     * Given a 16bit, big endian, signed audio sample, conver it to short[].
     */
    private short[] convertToSamples(final byte[] buffer, final int off, final int len) {
        short[] samples = new short[len / 2];

        /* Convert the incoming buffer into an array of samples. */
        for (int i = 0; i < samples.length; ++i) {
            samples[i] = (short)(((buffer[off+i*2] << 8) & 0xff00) | (buffer[off+i*2+1] & 0xff));
        }

        return samples;
    }


    /**
     * Given a signal encoded as specified by {@link #getAudioFormat()} demodule it.
     *
     * If any data is made available by demodulating the buffer, b, it is returned to the user
     * as a series of integer values 1 and 0 in the returned array.
     *
     * @param data Raw audio data.
     * @param off The offset into the buffer to operate on.
     * @param len The length of data to operate on.
     * @param os Output stream that bytes are written too as they are detected.
     *
     * @throws IOException on IO errors when populating various internal buffers.
     */
    public void detectSignal(final byte[] data, final int off, final int len, final OutputStream os)
        throws IOException
    {

        final short samples[] = convertToSamples(data, off, len);

        int samplesOff = 0;

        do {
            final int sampled = signalDetector.process(samples, samplesOff, samples.length-samplesOff, signalDetectorResult);

            /* Sampled = -1, the no result is available. Just update the offsets. */
            if (sampled == -1) {
                sampleCount += samples.length - samplesOff;
                samplesOff   = samples.length;
            }
            /* Otherwise, a result is available. */
            else {
                sampleCount += sampled;
                samplesOff  += sampled;

                // FIXME - leave out until the system is a bit more stable.
                // final double magnitude = signalDetectorResult.magnitude();
                // if (magnitude > 4 * lastMagnitude || magnitude * 4 < lastMagnitude) {
                //     lastMagnitude = magnitude;
                //     continue;
                // }
                // lastMagnitude = magnitude;

                final double phaseNow   = signalDetectorResult.phase();
                final double deltaPhase = Math.abs((phaseNow - phase) % (2.0*Math.PI));
// System.out.println("PHASE "+phase+" PHASE NOW "+phaseNow+" DELTA PHASE "+deltaPhase);
                /* This if-else handles signal detection. */
                if (deltaPhase > Math.PI / 2.0 && deltaPhase < 3.0 * Math.PI / 2.0 )
                {
                    if (lastSymbol == 0) {
                        sampleCount = 0;
                        phase       = phaseNow;
                        lastSymbol  = 2;
                        os.write(0);
// System.out.println("write 0");
                    }
                    else {
// System.out.println("might be 0");
                        lastSymbol = 0;
                    }
                }
                else if (sampleCount >= samplesPerSymbol) {
                    if (lastSymbol == 1) {
                        sampleCount = (sampleCount % samplesPerSymbol);
                        phase       = phaseNow;
                        lastSymbol  = 2;
                        os.write(1);
// System.out.println("write 1");
                    }
                    else {
// System.out.println("might be 1");
                        lastSymbol = 1;
                    }
                }
            }

        } while (samplesOff < samples.length);
    }

    /**
     * Detect a signal.
     *
     * @param data The audio data.
     * @param off Offset into data at which to start detecting a signal.
     * @param len The length from offset.
     *
     * @return A squence of 1 and 0 symbols detected from the given audio data. Note that
     *         partially detected signals may be internally buffered and not necessarily returned.
     */
    public byte[] detectSignal(final byte[] data, final int off, final int len) {
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            detectSignal(data, off, len, baos);

            return baos.toByteArray();
        }
        catch (final IOException t) {
            return null;
       }
    }

    /**
     * Detect a signal on audio data.
     *
     * @param data Audio data.
     *
     * @return A squence of 1 and 0 symbols detected from the given audio data. Note that
     *         partially detected signals may be internally buffered and not necessarily returned.
     */
    public byte[] detectSignal(final byte[] data) {
        return detectSignal(data, 0, data.length);
    }

    /**
     * Return the audio format this class generates.
     *
     * @return the audio format this class generates.
     */
    AudioFormat getAudioFormat() {
        /* Do not change this. This class expects to decode big-endian 16bit signed audio. */
        return new AudioFormat(sampleRate, sampleSize, 1, true, true);
    }

    /**
     * Return the number of PSK symbols per second.
     *
     * @return the number of PSK symbols per second.
     */
    public double getSymbolRate() {
        return this.symbolsPerSecond;
    }

    /**
     * Return the size of a single audio frame.
     *
     * An audio frame is one sample * the number of channels. There is 1 channel in PSK
     * so this is the byte width of that single channel.
     */
    public int getFrameSize(){ return sampleSize/8; }

    /**
     * Return the sample rate.
     * @return The sample rate.
     */
    public int getSampleRate() { return sampleRate; }
}
