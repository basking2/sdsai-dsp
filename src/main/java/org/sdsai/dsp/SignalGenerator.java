package org.sdsai.dsp;

import java.io.IOException;
import java.io.InputStream;

/**
 * A Signal Generator that generates a 16 bit, big endian, signed signal.
 */
public class SignalGenerator {

    private final int sampleRate;
    private double hz;
    private long sample;
    private double w;
    private short amplitude;

    /**
     * Constructor.
     *
     * @param hz The frequency of the signal to generate.
     * @param sampleRate The sample rate.
     */
    public SignalGenerator(final double hz, final int sampleRate) {
        this(hz, sampleRate, Short.MAX_VALUE);
    }

    /**
     * Constructor.
     *
     * @param hz The frequency of the signal to generate.
     * @param sampleRate The sample rate.
     * @param amplitude What should the maximum amplitude be.
     */
    public SignalGenerator(final double hz, final int sampleRate, final short amplitude) {
        this.sampleRate = sampleRate;
        this.amplitude = amplitude;
        tune(hz);
    }

    public void tune(final double hz) {
        this.hz = hz;
        this.sample = 0;
        this.w = 2.0 * Math.PI * hz / sampleRate;
    }

    public final short read() {
        return (short) (amplitude * Math.sin(w * sample++));
    }

    public final double readDouble() {
        return (amplitude * Math.sin(w * sample++));
    }

    public int read(final short[] data) {
        return read(data, 0, data.length);
    }

    public int read(final short[] data, final int off, final int len) {
        for (int i = off; i < off+len; ++i) {
            data[i] = read();
        }
        return len;
    }

    public int read(final double[] data) {
        return read(data, 0, data.length);
    }

    public int read(final double[] data, final int off, final int len) {
        for (int i = off; i < off+len; ++i) {
            data[i] = readDouble();
        }
        return len;
    }}