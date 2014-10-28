package org.sdsai.dsp;

import java.util.Arrays;
/**
 * A filter based on the Fast Fourier transform.
 *
 * This class uses double to represent the signal points.
 * This almost certainly differs from how a sound card will provide you data.
 */
public class FftFilter
{
    final double[] filterKernelReal;
    final double[] filterKernelImg;

    /**
     * The number of samples that {@link #filter(double[])} requires.
     */
    final int sampleCount;

    /**
     * The number of filter kernel sample count.
     * Equal to {@code filterKernelReal.length * 2}.
     */
    final int filterKernelSampleCount;

    final double[] overlap;

    /**
     * Constructor.
     *
     * @param signalGenerator Used to generate the filterKernel.
     * @param signalSize The size of the signal that the user will be giving to
     *        this algorithm. This must be a power of two.
     * @param overlap The size of the overlap signal. This must
     *        be less than signalSize.
     */
    public FftFilter(
        final SignalGenerator signalGenerator,
        final int             signalSize,
        final int             overlap
    )
    {
        /* Set simple, unprocessed values. */
        this.filterKernelSampleCount = signalSize;
        this.sampleCount             = filterKernelSampleCount - overlap;
        this.overlap                 = new double[overlap];

        /* Begin Filter Kernel Initialization. */

        final double[] filterKernel = new double[filterKernelSampleCount];

        /* Populate filterKernel buffer. */
        signalGenerator.read(filterKernel);

        final double[] real = Arrays.copyOf(filterKernel, filterKernelSampleCount);
        final double[] img  = new double[filterKernelSampleCount];

        DspUtils.fft(real, img);

        this.filterKernelReal = Arrays.copyOf(real, filterKernelSampleCount / 2);
        this.filterKernelImg  = Arrays.copyOf(img,  filterKernelSampleCount / 2);
        /* End Filter Kernel Initialization. */
    }

    public FftFilter(
        final double hz,
        final int    sampleRate,
        final int    signalSize,
        final int    overlap
    )
    {
        this(new SignalGenerator(hz, sampleRate), signalSize, overlap);
    }

    /**
     * Return the size of the sample buffer that this filter can process.
     */
    public int sampleCount() {
        return sampleCount;
    }

    public void filter(final double[] signal) {
        final double[] real = new double[filterKernelSampleCount];
        final double[] img  = new double[filterKernelSampleCount];

        if (signal.length != sampleCount()) {
            throw new IllegalArgumentException("Signal length must be " + sampleCount());
        }

        /* Load overlap into real buffer. */
        for (int i = 0; i < overlap.length; ++i) {
            real[i] = overlap[i];
        }

        /* Load signal into real buffer. */
        for (int i = 0; i < signal.length; ++i) {
            real[i + overlap.length] = signal[i];
        }

        /* Do the convolution. */
        DspUtils.fft(real, img);

        for (int i = 0; i < real.length / 2; ++i) {
            final double temp = real[i] * filterKernelReal[i] - img[i] * filterKernelImg[i];
            img[i] = real[i] * filterKernelImg[i] + img[i] * filterKernelReal[i];
            real[i] = temp;
        }

        DspUtils.ifft(real, img);

        /* End of the convolution. */

        /* Compute and copy the overlap. */
        for (int i = 0; i < overlap.length; ++i) {
            real[i]   = real[i] + overlap[i];
        }

        /* Record the overlap of the next iteration. */
        for (int i = 0, offset = real.length - overlap.length; i < overlap.length; ++i) {
            overlap[i] = real[offset + i];
        }

        /* Output the filtered signal. */
        for (int i = 0; i < signal.length; ++i) {
            signal[i] = real[i];
        }
    }

    /**
     * Call when the filter is done.
     *
     * The Overlap buffer starts as all zeros, but eventually
     * contains part of the filtered signal and should be output
     * at the end of the filter's use.
     */
    public double[] getOverlap() {
        return overlap;
    }
}