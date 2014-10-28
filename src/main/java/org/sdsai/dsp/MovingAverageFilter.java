package org.sdsai.dsp;

/**
 * A filter based on the moving avarge of samples.
 */
public final class MovingAverageFilter {

    /**
     * Prevous n samples.
     */
    private double samples[];

    /**
     * The sum of the average before it is finally divided.
     */
    private double partialResult;

    /**
     * The current sample to read and replace on the next call to {@link #process(short)}.
     */
    private int current;

    /**
     * Build a moving average filte that uses the given number of samples.
     *
     * @param samples The number of samples to use for the moving average.
     */
    public MovingAverageFilter(final int samples) {
        this.samples = new double[samples];
        this.partialResult = 0;
        this.current = 0;
    }

    /**
     * Construct a filter that targets a particular frequnce at a particular sample rate.
     *
     * The number of saples used to compute the average will be the mathmatical floor
     * of the number of samples in 180 of the frequency. This means that
     * at a particular time, all samples of the wave form will be positive or negative and
     * yeild the maximum averge value at the center of the wave form.
     *
     * @param hz The target frequncy.
     * @param sampleRate The sample rate in samples per second.
     */
    public MovingAverageFilter(final double hz, final int sampleRate) {
        this.samples = new double[(int)(Math.floor(sampleRate / hz / 2))];
    }

    /**
     * Filter a single sample.
     */
    public final double process(final double data) {

        /* Remove previous data value from it. */
        partialResult -= samples[current];

        /* Add in new data. */
        partialResult += data;
        samples[current] = data;

        /* Move current. */
        ++current;
        if (current >= samples.length) {
            current = 0;
        }

        /* Return final result. */
        return (partialResult / (double)samples.length);
    }
}
