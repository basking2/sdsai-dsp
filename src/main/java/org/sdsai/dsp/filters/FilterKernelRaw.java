package org.sdsai.dsp.filters;

/**
 * Create a filter kernel for use in {@link ConvolutionFilter}.
 */
public class FilterKernelRaw implements FilterKernel {

    final double[] h;

    public FilterKernelRaw(final double[] h) {
        this.h = h;
    }

    /**
     * The minimum length of a filter kernel.
     */
    @Override
    public int getSize()
    {
        return h.length;
    }

    /**
     * Set the bytes in {@code buffer}.
     *
     * @param buffer A buffer that is at least the length of the value returned
     *        {@link #length()}.
     */
    @Override
    public void apply(double[] buffer)
    {
        for (int i = 0; i < h.length; ++i) {
            buffer[i] = h[i];
        }
    }
}