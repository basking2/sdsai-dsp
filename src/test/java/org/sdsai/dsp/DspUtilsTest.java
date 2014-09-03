package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;

public class DspUtilsTest
{
    @Test
    public void testFft() {

        short[] signal = new short[32];
        short[] img    = new short[signal.length];
        final int hz   = 4;

        new SignalGenerator(hz, 32, (short)100).read(signal);

        System.out.println("--- FFT OUT --- ");
        DspUtils.fft(signal, img);

        print(img, signal);

        int maxidx = 0;
        double mag = 0;
        for (int i = 0; i < signal.length; ++i) {
            double m = DspUtils.magnitude(img[i], signal[i]);
            if (m > mag) {
                mag = m;
                maxidx = i;
            }
        }

        System.out.println("hz = "+hz+" peak at "+maxidx +" of "+mag);
        Assert.assertEquals(8, maxidx);
    }

    @Test
    public void testDft() {
        short[] signal = new short[16];
        double[] real  = new double[signal.length/2+1];
        double[] img   = new double[signal.length/2+1];

        final int hz = 4;

        /* Since the sample rate = signal.length the frequency will
         * be an index into signal[], allowing for very easy verification. */
        new SignalGenerator(hz, signal.length, (short)100).read(signal);

        DspUtils.dft(signal, real, img, signal.length);

        final double mag_hz = DspUtils.magnitude(img[hz], real[hz]);
        for (int i = 0; i < real.length; ++i) {
            if (i != hz) {
                final double mag_i = DspUtils.magnitude(img[i], real[i]);
                Assert.assertTrue(
                    String.format("i=%d mag[hz]=%f mag[i]=%f", i, mag_hz, mag_i),
                    mag_hz > mag_i / 10);
            }
        }

        System.out.println("--- DFT OUT --- ");
        print(img, real);
    }

    private static void print(final short[] img, final short[] real) {
        final double[] dimg = new double[img.length];
        final double[] dreal = new double[real.length];
        for (int i = 0; i < dimg.length; ++i) {
            dimg[i] = img[i];
        }
        for (int i = 0; i < dreal.length; ++i) {
            dreal[i] = real[i];
        }
        print(dimg, dreal);
    }

    private static void print(final double[] img, final double[] real) {
        for (int i = 0; i < img.length; ++i) {
            System.out.println(
                String.format(
                    "i=%d (%f, %f) phase=%f magnitude=%f",
                    i,
                    real[i],
                    img[i],
                    DspUtils.phase(img[i], real[i]),
                    DspUtils.magnitude(img[i], real[i])));
        }
    }

}