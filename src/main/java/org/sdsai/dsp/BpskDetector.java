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
     * Buffer for checking for inversions. This is 1/2 the length of the recieve buffer.
     */
    private short[] checkBuffer;

    /**
     * Index into {@link #buffer} that allows it be used as a ring buffer.
     *
     * This points to the next index to be written, or the first index in the buffer
     * to read. Either interpretation is correct.
     */
    private int bufferIdx;

    /**
     * How many audio samples encode 1 full audio wave cycle (2*Math.PI radians).
     */
    private double samplesPerCycle;

    /**
     * The count of the samples currently making up the last reported symbol.
     *
     * When this exceeds samplesPerSymbol a 1 is reported and this is set to 0.
     *
     * This is also used to compute a {@link #checkShift}.
     */
    private int sampleCount;

    /**
     * Shift up when the next set of samples will be checked.
     *
     * When a phase inversion is detected, we have the opportunity to more cleanly
     * center the ring buffer, {@link #buffer}, on the symbols we are recieving.
     * This re-centering is done when a phase inversion is observed and it is
     * determined that the zero symbol is leaving the current frame. If the zero symbol
     * is partly out of our frame, then we can check the next symbol
     * slightly earlier because it will be fully contained in the buffer slightly earlier.
     *
     * We choose to only accelerate checks. We do not delay checks because it is
     * significantly more complex to distinguish between a delayed check and
     * not confuse it with re-checking the symbol that caused the delay.
     */
    private int checkShift;

    /**
     * How many audio samples are gathered for each PSK symbol.
     */
    private int samplesPerSymbol;

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
     * @param hz Frequency of the detected tone.
     * @param sampleRate The audio sample rate.
     * @param symbolsPerSecond How many symbols per second. For PSK31 this is {@link BpskGenerator#PSK31_SYMBOLS_PER_SECOND}.
     */
    public BpskDetector(final double hz, final int sampleRate, final double symbolsPerSecond) {
        this.hz                 = hz;
        this.sampleRate         = sampleRate;
        this.symbolsPerSecond   = symbolsPerSecond;
        this.samplesPerCycle    = sampleRate/hz;
        this.samplesPerSymbol   = (int)(this.sampleRate / this.symbolsPerSecond);
        this.checkShift = 0;

        /* sampleRate / hz = number of samples for 1 full cycle of the wave. */
        this.buffer        = new short[samplesPerSymbol * 2];
        this.bufferIdx     = 0;
        this.checkBuffer   = new short[samplesPerSymbol];
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
     * @param int The length of data to operate on.
     * @param os Output stream that bytes are written too as they are detected.
     *
     * @return A squence of 1 and 0 symbols detected from the given audio data. Note that
     *         partially detected signals may be internally buffered and not necessarily returned.
     */
    public void detectSignal(final byte[] data, final int off, final int len, final OutputStream os) throws IOException {

        short samples[];

        samples = convertToSamples(data, off, len);

        for (int sample = 0; sample < samples.length; ++sample) {

            sampleCount++;

            /* Add the processed data sample to the buffer. */
            buffer[bufferIdx++] = samples[sample];
            if (bufferIdx >= buffer.length) {
                bufferIdx = 0;
            }

            /* Every 1/2 buffer fill, check for an inversion. */
            if ((2 * (bufferIdx+checkShift)) % buffer.length == 0) {

                double avgAmplitude = 0.0;

                /* How many zeros were detected. 50% or more is a phase inversion. */
                int leftZeros = 0;
                int rightZeros = 0;

                /* Write the entire checkBuffer as a combination of the two
                 * halves of buffer, starting at bufferIdx and
                 * bufferIdx+checkBuffer.length.
                 * We could allocate checkBuffer here, but putting it in the
                 * class allows us to avoid asking for memory. It's a performance
                 * choice. */
                for (int chkBufIdx = 0; chkBufIdx < checkBuffer.length; ++chkBufIdx) {

                    short v1 = buffer[(bufferIdx + chkBufIdx) % buffer.length];
                    short v2 = buffer[(bufferIdx + chkBufIdx + buffer.length/2) % buffer.length];

                    // FIXME - move this to a weight moving average in the class.
                    avgAmplitude += (double)(Math.abs(v1) + Math.abs(v2)) / (double)buffer.length;

                    checkBuffer[chkBufIdx] = (short)(v1 + v2);
                }

                for (int chkBufIdx = 0; chkBufIdx < checkBuffer.length/2; ++chkBufIdx) {
                    if (Math.abs(checkBuffer[chkBufIdx]) < avgAmplitude) {
                        leftZeros++;
                    }
                }

                for (int chkBufIdx = checkBuffer.length/2; chkBufIdx < checkBuffer.length; ++chkBufIdx) {
                    if (Math.abs(checkBuffer[chkBufIdx]) < avgAmplitude) {
                        rightZeros++;
                    }
                }

                /* If there are some percentage of zeros, call it a phase inversion. */
                if (leftZeros + rightZeros > checkBuffer.length / 2) {

                    /* More left zeros means a departing inversion. Accelerate next check. */
                    if (leftZeros > rightZeros && leftZeros >= rightZeros * 2) {
                        sampleCount = samplesPerSymbol - (leftZeros + rightZeros);
                        checkShift += sampleCount;
                    }
                    /* More right zeros means an incoming inversion.
                     * Our modulo math does not allow us to checkShift a check,
                     * we can only accelerate them. */
                    else {
                        sampleCount = (leftZeros + rightZeros);
                    }
                    os.write(0);
                }
                /* If not, then check if we've crossed over into another 1. */
                else if (sampleCount >= samplesPerSymbol) {
                    sampleCount = sampleCount % samplesPerSymbol;
                    os.write(1);
                }
            }
        }
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
     */
    AudioFormat getAudioFormat() {
        /* Do not change this. This class expects to decode big-endian 16bit signed audio. */
        return new AudioFormat(sampleRate, 16, 1, true, true);
    }


    /**
     * Return the number of PSK symbols per second.
     */
    public double getSymbolRate() {
        return this.symbolsPerSecond;
    }
}