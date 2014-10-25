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
    final double[] filterKernel;
    final double[] filterKernelReal;
    final double[] filterKernelImg;

    double[] overlap;

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
        this.filterKernel = new double[signalSize];

        signalGenerator.read(this.filterKernel);

        final double[] real = Arrays.copyOf(filterKernel, signalSize);
        final double[] img  = new double[signalSize];

        DspUtils.fft(real, img);

        this.filterKernelReal = Arrays.copyOf(real, signalSize / 2);
        this.filterKernelImg  = Arrays.copyOf(img,  signalSize / 2);

        this.overlap = new double[overlap];
    }

    /**
     * Return the size of the sample buffer that this filter can process.
     */
    public int sampleSize() {
        return filterKernel.length - overlap.length;
    }

    public void filter(final double[] signal)
    {
        final double[] real = new double[filterKernel.length];
        final double[] img  = new double[filterKernel.length];

        if (signal.length != sampleSize()) {
            throw new IllegalArgumentException("Signal length must be " + sampleSize());
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

        /* Copy to the overlap. */
        for (int i = 0; i < overlap.length; ++i) {
            real[i]   = real[i] + overlap[i];
            signal[i] = real[i];
        }

        for (int i = 0, offset = real.length - overlap.length; i < overlap.length; ++i) {
            overlap[i] = real[offset + i];
        }

        /* At this point 0 through (real.length - overlap.length) is available.
         * The overlap is still stored. */
    }
}