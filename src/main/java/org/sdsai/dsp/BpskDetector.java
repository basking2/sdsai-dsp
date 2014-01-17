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
     */
    private int sampleCount;

    private int delay;

    private int samplesPerSymbol;

    /**
     * Constructor with sensible defaults.
     */
    public BpskDetector() {
        this(
            BpskGenerator.DEFAULT_FREQUENCY,
            BpskGenerator.DEFAULT_SAMPLE_RATE,
            BpskGenerator.PSK31_SYMBOLS_PER_SECOND);
    }

    public BpskDetector(final double hz, final int sampleRate, final double symbolsPerSecond) {
        this.hz                 = hz;
        this.sampleRate         = sampleRate;
        this.symbolsPerSecond   = symbolsPerSecond;
        this.samplesPerCycle    = sampleRate/hz;
        this.samplesPerSymbol   = (int)(this.sampleRate / this.symbolsPerSecond);
        this.delay = 0;

        /* sampleRate / hz = number of samples for 1 full cycle of the wave. */
        this.buffer        = new short[samplesPerSymbol * 2];
        this.bufferIdx     = 0;
        this.checkBuffer   = new short[samplesPerSymbol];
    }

    public double getSymbolRate() {
        return this.symbolsPerSecond;
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
     * @return An array of 1's and 0's representing the signal recieved.
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
            if ((2 * (bufferIdx+delay)) % buffer.length == 0) {

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
                        delay += sampleCount;
                    }
                    /* More right zeros means an incoming inversion.
                     * Our modulo math does not allow us to delay a check,
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
}