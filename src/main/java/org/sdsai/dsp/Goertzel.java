package org.sdsai.dsp;

/**
 * The Goertzel algorithm is a well-known DSP algorithm
 * for finding phase and magnitude information about a particular
 * signal.
 */
public final class Goertzel {

    private final int k;
    private final double omega;
    private final double cos_omega;
    private final double sin_omega;
    private final double coefficient;

    /**
     * The bin size, or the number of samples to process before
     * a result is returned.
     */
    private final int N;

    /**
     * Internally track the number of samples processed.
     */
    private int n;

    /**
     * The current Goertzel value.
     */
    private double q0;

    /**
     * The previous Goertzel value.
     */
    private double q1;

    /**
     * The Goertzel value before q1.
     *
     * That is, the one from two iterations back.
     */
    private double q2;

    /**
     * Constructor.
     *
     * A note on N, you want your frequency to be an integer multiple of sample_rate / N
     * so that the wave form is "centered" in the bin. Smaller values of N
     * less-well represent the wave form, but take less time to process.
     * Larger values of N take longer to process, and you must wait for an appropriate
     * number of samples have come in.
     *
     * @param hz Target frequency to compute the presense or absents of.
     * @param sampleRate The sample rate of the samples passed to {@link #process(short[], int)}.
     * @param N The number of samples that make up a bucket of samples.
     *
     */
    public Goertzel(double hz, int sampleRate, int N) {
        this.N           = N;
        this.k           = (int)(0.5 + (double)N * (double)hz / (double)sampleRate);
        this.omega       = (2.0 * Math.PI/(double)N)*(double)k;
        this.cos_omega   = Math.cos(omega);
        this.sin_omega   = Math.sin(omega);
        this.coefficient = 2.0 * cos_omega;

        /* Reset all per-sample values. This initializes q0, q1, q2, and n. */
        reset();
    }

    /**
     * Internal routine that does the iteration work of the Goertzel Algorithm.
     *
     * This is called by various setup functions, also named "process()".
     *
     * @param samples The array of 16 bit, signed audio samples.
     * @param len The number of samples to process, staring at {@code samples[0]}.
     */
    private final void process(final short[] samples, final int off, final int len) {
        for (int i = off; i < off+len; ++i) {
            q0 = coefficient * q1 - q2 + samples[i];
            q2 = q1;
            q1 = q0;
        }
    }

    /**
     * Internal method that sets the results.
     *
     * @param result The results set.
     */
    private final void setResult(final Result result) {
        result.real = q1 - q2 * cos_omega;
        result.imaginary = q2 * sin_omega;
    }

    /**
     * Internal method that sets the results.
     *
     * @param result The results set.
     */
    private final void setResult(final FastResult result) {
        result.magnitude_squared  = (q1 * q1) + (q2 * q2) - (q1 * q2 * coefficient);
    }

    /**
     * Reset the internal data stuctures for another iteration.
     */
    public final void reset() {
        q0 = 0;
        q1 = 0;
        q2 = 0;
        n  = 0;
    }

    /**
     * Process a set of audio samples with the Goertzel Algorithm.
     *
     * @param samples The set of 16-bit, signed audio values.
     * @param result The result object to populate if enough samples are provided.
     *        If the returned value greater than or equal to 0, a result was
     *        placed in this object.
     *
     * @return The number of samples processed from the given buffer to yeild a result or -1 if
     *         more samples are still needed.
     */
    public final int process(final short[] samples, final int off, final int len, final Result result) {
        /* Now many samples are left to compute? */
        final int n_left = N-n;

        /* If we will not have a result from the given samples, do them all. */
        if (n_left > len) {
            process(samples, off, len);
            n += len;
            return -1;
        }

        /* Otherwise, do only those samples that we can to get a result, and stop. */
        process(samples, off, n_left);
        setResult(result);
        reset();
        return n_left;
    }

    /**
     * Process a set of audio samples with the Goertzel Algorithm.
     *
     * The computation done when results are available is slightly faster here.
     *
     * @param samples The set of 16-bit, signed audio values.
     * @param result The result object to populate if enough samples are provided.
     *        If the returned value greater than or equal to 0, a result was
     *        placed in this object.
     *
     * @return The number of samples processed from the given buffer to yeild a result or -1 if
     *         more samples are still needed.
     */
    public final int process(final short[] samples, final int off, final int len, final FastResult result) {
        /* Now many samples are left to compute? */
        final int n_left = N-n;

        /* If we will not have a result from the given samples, do them all. */
        if (n_left > len) {
            process(samples, off, len);
            n += len;
            return -1;
        }

        /* Otherwise, do only those samples that we can to get a result, and stop. */
        process(samples, off, n_left);
        setResult(result);
        reset();
        return n_left;
    }

    /**
     * A class that holds the result of the signal computation.
     */
    public static final class Result {
        public double real;
        public double imaginary;

        /**
         * Compute the phase from {@code real} and {@code imaginary}.
         */
        public double phase() {
            if (real > 0) {
                return Math.atan(imaginary/real);
            }
            else if (real < 0) {
                if (imaginary >= 0) {
                    return Math.atan(imaginary/real) + Math.PI;
                }
                else {
                    return Math.atan(imaginary/real) - Math.PI;
                }
            }
            else {
                if (imaginary > 0) {
                    return Math.PI / 2;
                }
                else if (imaginary > 0) {
                    return - Math.PI / 2;
                }
                else {
                    return Double.NaN;
                }
            }
        }

        /**
         * Compute the magnitude.
         */
        public double magnitude() {
            return Math.sqrt(real * real + imaginary * imaginary);
        }
    }

    /**
     * A class that holds the result of the signal computation.
     */
    public static final class FastResult {
        /**
         * The magnitude, squared.
         */
        public double magnitude_squared;

        public double magnitude() {
            return Math.sqrt(magnitude_squared);
        }
    }
}
