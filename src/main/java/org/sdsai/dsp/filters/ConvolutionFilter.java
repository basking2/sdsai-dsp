package org.sdsai.dsp.filters;

import java.util.Arrays;

import org.sdsai.dsp.DspUtils;
import org.sdsai.dsp.SignalGenerator;

/**
 * A filter based on the Fast Fourier transform.
 *
 * This class uses double to represent the signal points.
 * This almost certainly differs from how a sound card will provide you data.
 */
public class ConvolutionFilter {

   /**
     * Size of the filter kernel.
     * This is less than {@link #kreal} and {@link #kimg}.
     */
    int filterKernelSize;

    double[] kreal;
    double[] kimg;

    /**
     * The number of samples that {@link #filter(double[])} requires.
     */
    int sampleCount;

    /**
     * The dftSize required to compute the kernel image.
     */
    int dftSize;

    double[] overlap;

    public ConvolutionFilter(final FilterKernel filterKernel)
    {
        filterKernelSize = filterKernel.getSize();

        for (dftSize = 1; dftSize < filterKernelSize; dftSize *= 2)
            ;

        kreal = new double[dftSize*2];
        kimg  = new double[dftSize*2];

        filterKernel.apply(kreal);

        DspUtils.fft(kreal, kimg);

        /* Check the side effects of the previous init routine. */
        assert dftSize > 0;
        assert filterKernelSize > 0;
        assert kreal != null;
        assert kimg != null;

        /* Set simple, unprocessed values. */
        this.sampleCount = 2 * dftSize - (filterKernelSize - 1);
        this.overlap     = new double[filterKernelSize - 1];
    }

    /**
     * Return the size of the sample buffer that this filter can process.
     */
    public int sampleCount() {
        return sampleCount;
    }

    public void filter(final double[] signal) {
        if (signal.length != sampleCount()) {
            throw new IllegalArgumentException("Signal length must be " + sampleCount());
        }

        final double[] real = new double[dftSize*2];
        final double[] img  = new double[dftSize*2];

        for (int i = 0; i < signal.length; ++i) {
            real[i] = signal[i];
        }

        /* Do the convolution. Only the bottom half has good data. */
        DspUtils.fft(real, img);

        for (int i = 0; i < dftSize*2; ++i) {
            final double temp = real[i] * kreal[i] - img[i] * kimg[i];
            img[i]            = real[i] * kimg[i]  + img[i] * kreal[i];
            real[i]           = temp;
        }

        DspUtils.ifft(real, img);

        /* End of the convolution. */

        /* Compute and copy the overlap. */
        for (int i = 0; i < overlap.length; ++i) {
            real[i]   = real[i] + overlap[i];
        }

        /* Record the overlap of the next iteration. */
        for (int i = 0; i < overlap.length; ++i) {
            overlap[i] = real[real.length-overlap.length + i];
        }

        /* Output the filtered signal. */
        for (int i = 0; i < sampleCount; ++i) {
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