package org.sdsai.dsp;

import java.io.IOException;
import java.io.Closeable;

/**
 * A class that wraps {@link FftFilter} into a streaming API.
 *
 * This does not implement any part of {@code java.io} because
 * this class operates on doubles, not bytes.
 */
public class FftFilterStream implements Closeable {

    private FftFilter filter;

    private double[] buffer;

    private int bufferFill;

    private boolean closed;

    public FftFilterStream(final FftFilter filter) {
        setFilter(filter);
        closed = false;
    }

    /**
     * Transform the given input with this filter.
     *
     * The number of bytes written into samples is returned.
     *
     * @param samples The samples to transform and replace.
     *
     * @return the number of bytes written.
     */
    public int apply(final double[] samples, int start, int length) throws IOException {

        int writtenTotal = 0;

        for (int i = 0; i < length; )
        {
            final int written = write(samples, start+i, length-i);

            /* If results are available, copy them out of buffer into samples. */
            if (resultsAvailable()) {
                for (int j = 0; j < bufferFill; ++j) {
                    samples[start+i+j] = buffer[j];
                }

                /* Record that we've written filtered data. */
                writtenTotal += bufferFill;

                /* Reset the buffer fill. */
                bufferFill    = 0;
            }

            i += written;
        }

        return writtenTotal;
    }

    public int apply(final double[] samples, int length) throws IOException {
        return apply(samples, 0, length);
    }

    public int apply(final double[] samples) throws IOException {
        return apply(samples, 0, samples.length);
    }

    /**
     * Write a single byte of data to the buffer.
     * If a write causes results to be available, this returns true.
     * Otherwise this returns false.
     *
     * @return True when data is available in {@link #getBuffer()}.
     *         False otherwise.
     */
    public void write(final double sample) throws IOException {
        double[] samples = { sample };
        write(samples, 0, 1);
    }

    public int write(final double[] samples) throws IOException {
        return write(samples, 0, samples.length);
    }

    public int write(final double[] samples, int length) throws IOException {
        return write(samples, 0, length);
    }

    /**
     * Write samples to the buffer or internal filter.
     *
     * The caller must check {@link resultsAvailable()} after this method returns.
     * It is true that if the returned length is less than the submitted length of samples to
     * write, then there is data available.
     *
     * If the returned length is equal to the length submitted, there may or may not
     * be data available.
     *
     * @return The number of bytes written.
     */
    public int write(final double[] samples, int start, int length)  throws IOException {

        if (closed) {
            throw new IOException("This stream is closed.");
        }

        if (length == 0) {
            return 0;
        }

        if (resultsAvailable()) {
            throw new IOException("Buffer is full. Results must be retrieved and reset() called.");
        }

        final int samplesToWrite = (bufferFill + length <= buffer.length) ? length : buffer.length - bufferFill;

        for (int i = 0; i < samplesToWrite; ++i) {
            buffer[bufferFill + i] = samples[start + i];
        }
        bufferFill += samplesToWrite;

        if (resultsAvailable()) {
            filter.filter(buffer);
        }

        return samplesToWrite;
    }

    /**
     * Reset the internal buffer pointer.
     *
     * This does not erase the contents of a buffer until
     * the next {@link #write()} call is made. As a convenience
     * the buffer is returned. This is the same buffer returned by
     * {@link #getBuffer()}.
     *
     * @return The internal data buffer completely filled with filtered data.
     *         {@link #getBufferFill()} is set to zero when this called.
     */
    public double[] reset() {
        bufferFill = 0;
        return buffer;
    }

    /**
     * Return true if the buffer of data is filled.
     */
    public boolean resultsAvailable() {
        return bufferFill == buffer.length;
    }

    public void setFilter(final FftFilter filter) {
        this.filter     = filter;
        this.buffer     = new double[filter.sampleCount()];
        this.bufferFill = 0;
    }

    public FftFilter getFilter() {
        return this.filter;
    }

    public double[] getBuffer() {
        return buffer;
    }

    public int getBufferFill() {
        return bufferFill;
    }

    public void close() throws IOException
    {
        double[] overlap = filter.getOverlap();

        for (int i = 0; i < overlap.length; ++i) {
            buffer[i] = overlap[i];
        }

        bufferFill = overlap.length;
    }
}