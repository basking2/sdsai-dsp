package org.sdsai.dsp;

import java.util.Arrays;

import org.junit.Test;
import org.junit.Ignore;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class FftFilterTest {

    @Test
    @Ignore
    public void testFft() throws IOException {

        final int hz = 3;
        final int sampleRate = 100;

        final SignalGenerator sg = new SignalGenerator(hz, sampleRate, (short) 1000);
        final double[] real = new double[1024*8];
        final double[] img  = new double[real.length];
        // final FftFilter filter = new FftFilter(hz, sampleRate, 64, 20);
        final FftFilter filter = new FftFilter(sg, 64, 21);

        sg.read(real);

        double[] real2 = Arrays.copyOf(real, real.length);
        double[] img2  = new double[real2.length];

        for (int i = 0; i + filter.sampleCount()< real2.length; i += filter.sampleCount()) {
            final double[] b = new double[filter.sampleCount()];
            for (int j = 0; j < b.length; ++j) {
                b[j] = real2[i+j];
            }
            filter.filter(b);
            for (int j = 0; j < b.length; ++j) {
                real2[i+j] = b[j];
            }
        }


        DspUtils.fft(real, img);
        DspUtils.fft(real2, img2);

        for (int i = 0; i < real.length/2; ++i) {
            System.out.println(
                DspUtils.fftIdxToHz(i, sampleRate, real.length) + ":\t"+
                DspUtils.magnitude(img[i], real[i]) + "\t" +
                DspUtils.magnitude(img2[i], real2[i])
            );
        }
    }

    @Test
    @Ignore
    public void testFft2() throws IOException {

        final int hz = 3;
        final int sampleRate = 100;

        final SignalGenerator sg = new SignalGenerator(hz, sampleRate, (short) 1000);
        final double[] real = new double[1024*8];
        final double[] img  = new double[real.length];
        // final FftFilter filter = new FftFilter(hz, sampleRate, 64, 20);
        final FftFilter filter = new FftFilter(sg, 64, 21);

        sg.read(real);
        for (int i = 0; i < real.length; ++i) {
            real[i] *= Math.random();
        }

        double[] real2 = Arrays.copyOf(real, real.length);
        double[] img2  = new double[real2.length];

        for (int i = 0; i + filter.sampleCount()< real2.length; i += filter.sampleCount()) {
            final double[] b = new double[filter.sampleCount()];
            for (int j = 0; j < b.length; ++j) {
                b[j] = real2[i+j];
            }
            filter.filter(b);
            for (int j = 0; j < b.length; ++j) {
                real2[i+j] = b[j];
            }
        }


        DspUtils.fft(real, img);
        DspUtils.fft(real2, img2);

        for (int i = 0; i < real.length/2; ++i) {
            System.out.println(
                DspUtils.fftIdxToHz(i, sampleRate, real.length) + ":\t"+
                DspUtils.magnitude(img[i], real[i]) + "\t" +
                DspUtils.magnitude(img2[i], real2[i])
            );
        }
    }
}