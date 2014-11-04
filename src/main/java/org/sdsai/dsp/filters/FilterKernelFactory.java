package org.sdsai.dsp.filters;

import org.sdsai.dsp.SignalGenerator;

/**
 * A factory for generating filter kernels.
 */
public class FilterKernelFactory {

    private int sampleRate;

    /**
     * Create a FilterKernelFactory that uses the given sample rate.
     *
     * @param sampleRate The sampleRate in samples / second.
     */
    public FilterKernelFactory(final int sampleRate)
    {
        this.sampleRate = sampleRate;
    }

    public FilterKernel lowPass(final double hz, final double peak, final double trough, final double cycles) {
        return new FilterKernel() {

            final int length = (int) (sampleRate / hz * cycles);

            @Override
            public int getSize() {
                return length;
            }

            @Override
            public void apply(final double[] buffer) {
                for (int i = 0; i < length; ++i) {
                    if (i % (length / 2) == 0) {
                        buffer[length / 2] = peak;
                    }
                    else {
                        buffer[i] = trough;
                    }
                }
            }
        };
    }

    public FilterKernel lowPass(final double hz)
    {
        return lowPass(hz, 4.0, -0.1, 0.5);
    }

    public FilterKernel highPass(final double hz) {
        return highPass(hz, 1, 0.5);
    }

    public FilterKernel highPass(final double hz, final int amplitude, final double cycles) {
        return new FilterKernel() {

            final int length = (int) (sampleRate / hz * cycles);

            @Override
            public int getSize() {
                return length;
            }

            @Override
            public void apply(final double[] buffer) {
                /* Put 1/2 a cycle of signal into `real`. */
                new SignalGenerator(hz, sampleRate, amplitude).read(buffer, 0, length);
            }
        };
    }
}