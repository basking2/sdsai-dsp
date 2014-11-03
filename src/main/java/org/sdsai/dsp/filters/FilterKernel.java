package org.sdsai.dsp.filters;

/**
 * Create a filter kernel for use in {@link ConvolutionFilter}.
 */
public interface FilterKernel {

    /**
     * The minimum length of a filter kernel.
     */
    int getSize();

    /**
     * Set the bytes in {@code buffer}.
     *
     * @param buffer A buffer that is at least the length of the value returned
     *        {@link #length()}.
     */
    void apply(double[] buffer);
}