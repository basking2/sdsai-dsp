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

    public FilterKernel lowPass(final double hz) {
        return new FilterKernel() {

            final int length = (int) (sampleRate / hz / 2);

            @Override
            public int getSize() {
                return length;
            }

            @Override
            public void apply(final double[] buffer) {
                for (int i = 0; i < length; ++i) {
                    buffer[i] = -0.1;
                }
                buffer[length / 2] = 2;
            }
        };
    }

    public FilterKernel highPass(final double hz) {
        return new FilterKernel() {

            final int length = (int) (sampleRate / hz / 2);

            @Override
            public int getSize() {
                return length;
            }

            @Override
            public void apply(final double[] buffer) {
                /* Put 1/2 a cycle of signal into `real`. */
                new SignalGenerator(hz, sampleRate, 1).read(buffer, 0, length);
            }
        };
    }
}