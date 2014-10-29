package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;

import java.util.Arrays;

public class DspUtilsTest
{
    @Test
    public void testFftDouble() {

        final int sampleRate = 32;
        final int hz   = 2;
        double[] signal = new double[1024];
        double[] img    = new double[signal.length];

        new SignalGenerator(hz, sampleRate, (short)100).read(signal);

        System.out.println("--- FFT OUT DOUBLE --- ");
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
        Assert.assertEquals(hz, DspUtils.fftIdxToHz(maxidx, sampleRate, signal.length), 0.1);
    }

    @Test
    public void testIfftDouble() {

        final int sampleRate = 32;
        final int hz   = 2;
        double[] signal = new double[1024];
        double[] real;
        double[] img    = new double[signal.length];

        new SignalGenerator(hz, sampleRate, (short)100).read(signal);

        System.out.println("--- IFFT OUT DOUBLE --- ");
        real = Arrays.copyOf(signal, signal.length);
        DspUtils.fft(real, img);
        DspUtils.ifft(real, img);

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

        for (int i = 0; i < signal.length; ++i) {
            Assert.assertEquals(signal[i], real[i], 0.1);
            Assert.assertEquals(0.0, img[i], 0.1);
        }
        System.out.println("hz = "+hz+" peak at "+maxidx +" of "+mag);
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