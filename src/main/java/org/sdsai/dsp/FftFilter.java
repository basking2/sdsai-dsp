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

    public FftFilter(
        final double hz,
        final int    sampleRate
    )
    {
        initFilterKernelLowPass(hz, sampleRate);

        /* Check the side effects of the previous init routine. */
        assert dftSize > 0;
        assert filterKernelSize > 0;
        assert kreal != null;
        assert kimg != null;

        /* Set simple, unprocessed values. */
        this.sampleCount = 2 * dftSize - (filterKernelSize - 1);
        this.overlap     = new double[filterKernelSize - 1];
    }

    public FftFilter(
        double[] filterKernel
    )
    {
        filterKernelSize = filterKernel.length;
        for (dftSize = 1; dftSize < filterKernel.length; dftSize *= 2);

        kreal = new double[dftSize * 2];
        kimg  = new double[dftSize * 2];
        for (int i = 0; i < filterKernelSize; ++i) {
            kreal[i] = filterKernel[i];
        }

        DspUtils.fft(kreal, kimg);

        this.sampleCount = 2 * dftSize - (filterKernelSize - 1);
        this.overlap     = new double[filterKernelSize - 1];
    }

    private void initFilterKernelLowPass(final double hz, final int sampleRate) {
        filterKernelSize = (int) (sampleRate / hz / 2);

        for (dftSize = 1; dftSize < filterKernelSize; dftSize *= 2)
            ;

        kreal = new double[dftSize*2];
        kimg  = new double[dftSize*2];
        for (int i = 0; i < filterKernelSize; ++i) {
            kreal[i] = -0.1;
        }
        kreal[filterKernelSize / 2] = 2;

        DspUtils.fft(kreal, kimg);
    }

    private void initFilterKernelHighPass(final double hz, final int sampleRate) {

        filterKernelSize = (int) (sampleRate / hz / 2);
        for (dftSize = 1; dftSize < filterKernelSize; dftSize *= 2)
            ;

        kreal = new double[dftSize*2];
        kimg  = new double[dftSize*2];
        /* Put 1/2 a cycle of signal into `real`. */
        new SignalGenerator(hz, sampleRate, 1).read(kreal, 0, filterKernelSize);

        DspUtils.fft(kreal, kimg);
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
