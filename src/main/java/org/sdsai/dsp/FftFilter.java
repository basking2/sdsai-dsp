package org.sdsai.dsp;

/**
 * A filter based on the Fast Fourier transform.
 *
 * This class uses double to represent the signal points.
 * This almost certainly differs from how a sound card will provide you data.
 */
public class FftFilter
{
    final double[] filterKernel;

    /**
     * Constructor.
     *
     * @param signalGenerator Used to generate the filterKernel.
     * @param filterKernelSize The size of the filter kernel to use. This plus
     *        the signalSize must be a power of 2.
     * @param signalSize The size of the signal that the user will be giving to
     *        this algorithm. This plus the filterKernelSize must be a power of two.
     */
    public FftFilter(
        final SignalGenerator signalGenerator,
        final int filterKernelSize,
        final int signalSize
    )
    {
        filterKernel = new double[filterKernelSize];

        signalGenerator.read(filterKernel);
    }

    public void filter(final double[] signal)
    {

    }
}