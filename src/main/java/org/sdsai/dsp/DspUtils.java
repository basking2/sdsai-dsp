package org.sdsai.dsp;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Useful equations wrapped in functions for use with DSP.
 *
 */
public final class DspUtils {
    /**
     * Compute the phase from {@code real} and {@code imaginary}.
     *
     * @param imaginary Imaginary component of the signal.
     * @param real Real component of the signal.
     *
     * @return The phase, given the two values.
     */
    public static final double phase(final double imaginary, final double real) {
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
     * Compute the phase from {@code real} and {@code imaginary}.
     *
     * @param imaginary Imaginary component of the signal.
     * @param real Real component of the signal.
     *
     * @return The phase, given the two values.
     */
    public static final double magnitude(final double imaginary, final double real) {
        return Math.sqrt(real * real + imaginary * imaginary);
    }

    /**
     * Reverse the 31 low order bits used to comprise the integer i.
     *
     * The highorder bit is ignored because it is the signed bit.
     *
     * @param i The integer to reverse the bits of.
     *
     * @return The reversed bits.
     */
    private static final int bitReverse31(int i)
    {
        return
            ((i >>> 30) & 0x00000001) |
            ((i <<  30) & 0x40000000) |
            ((i >>> 28) & 0x00000002) |
            ((i <<  28) & 0x20000000) |
            ((i >>> 26) & 0x00000004) |
            ((i <<  26) & 0x10000000) |
            ((i >>> 24) & 0x00000008) |
            ((i <<  24) & 0x08000000) |
            ((i >>> 22) & 0x00000010) |
            ((i <<  22) & 0x04000000) |
            ((i >>> 20) & 0x00000020) |
            ((i <<  20) & 0x02000000) |
            ((i >>> 18) & 0x00000040) |
            ((i <<  18) & 0x01000000) |
            ((i >>> 16) & 0x00000080) |
            ((i <<  16) & 0x00800000) |
            ((i >>> 14) & 0x00000100) |
            ((i <<  14) & 0x00400000) |
            ((i >>> 12) & 0x00000200) |
            ((i <<  12) & 0x00200000) |
            ((i >>> 10) & 0x00000400) |
            ((i <<  10) & 0x00100000) |
            ((i >>>  8) & 0x00000800) |
            ((i <<   8) & 0x00080000) |
            ((i >>>  6) & 0x00001000) |
            ((i <<   6) & 0x00040000) |
            ((i >>>  4) & 0x00002000) |
            ((i <<   4) & 0x00020000) |
            ((i >>>  2) & 0x00004000) |
            ((i <<   2) & 0x00010000)
            ;
    }

    /**
     * Compute the discrete fourier transform on samples.
     * @param samples Signal data. This must be N elements long.
     * @param re_x The real portion of the signal is written here. This must be N/2+1 elements in length.
     * @param im_x The imaginary portion of the signal is written here. This must be N/2+1 elements in length.
     * @param N The number of samples to compute. The number of samples should represent a complete wave form.
     */
    public static final void dft(
        final short[]  samples,
        final double[] re_x,
        final double[] im_x,
        final int      N
    )
    {
        for (int x_i = 0; x_i < re_x.length; ++x_i) {
            for (int samples_i = 0; samples_i < N; ++samples_i) {
                re_x[x_i] += samples[samples_i] * Math.cos(2 * Math.PI * x_i * samples_i / samples.length);
                im_x[x_i] -= samples[samples_i] * Math.sin(2 * Math.PI * x_i * samples_i / samples.length);
            }
        }
    }

    private static Comparator<int[]> intPairComparator = new Comparator<int[]>(){
        @Override
        public int compare(final int[] i, int[] j) {
            return i[1] - j[1];
        }
    };

    private static final void bitReversalSort(final short[] real) {
        /* First, make a tranlation map from the index of the array to the integer it points to. */
        final int mapping[][] = new int[real.length][2];

        /* TODO - cache / memoize this mapping for a particular size array? */
        for (int i = 0; i < mapping.length; ++i) {
            mapping[i][0] = i;
            mapping[i][1] = bitReverse31(i);
        }

        Arrays.sort(mapping, intPairComparator);

        for (int i = 0; i < real.length; ++i) {
            final int j = mapping[i][0];
            final short tmp = real[i];
            real[i] = real[j];
            real[j] = tmp;
        }
    }

    /**
     * Fast Fourier Transform.
     *
     * @param real The time-domain signal. This must be a power of two in length.
     * @param img The imaginary components. This array must start zeroed and be the same length as {@code real}.
     */
    public static final void fft(final short[] real, final short[] img)
    {
        /* Bit reversal sort. We do not sort img because it should be 0s. */
        bitReversalSort(real);

        for (int segment = 1, stride = 2; stride <= real.length; ) {

            /* Iterate around the sinusoid using complex multiplication. */
            final double sinusoid_real_itr =  Math.cos(Math.PI / segment);
            final double sinusoid_img_itr  = -Math.sin(Math.PI / segment);

            /* Start the sinusoid at 0 degrees. */
            double sinusoid_real = 1;
            double sinusoid_img  = 0;

            for (int j = 0; j < segment; ++j) {
                for (int i1 = j, i2 = j + segment; i1 < real.length; i1 += stride, i2 += stride) {
                    final double real_tmp = real[i2] * sinusoid_real - img[i2] * sinusoid_img;
                    final double img_tmp  = real[i2] * sinusoid_img + img[i2] * sinusoid_real;
                    real[i2] = (short)(real[i1] - real_tmp);
                    img[i2]  = (short)(img[i1] - img_tmp);
                    real[i1] = (short)(real[i1] + real_tmp);
                    img[i1]  = (short)(img[i1] + img_tmp);
                }

                /* Advance the sinusoid. */
                final double real_tmp = sinusoid_real;
                sinusoid_real = real_tmp * sinusoid_real_itr - sinusoid_img * sinusoid_img_itr;
                sinusoid_img = real_tmp * sinusoid_img_itr + sinusoid_img * sinusoid_real_itr;
            }

            /* Advance the loop variables.
             * Segment is always 1/2 of stride
             * (which happens to be the previous stride value). */
            segment  = stride;
            stride  *= 2;
        }
    }

}