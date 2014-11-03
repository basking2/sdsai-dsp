package org.sdsai.dsp.filters;

import org.sdsai.dsp.SignalGenerator;
import org.sdsai.dsp.DspUtils;

import java.util.Arrays;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFormat;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class ConvolutionFilterTest {

    @Test
    public void testFft() throws IOException {

        final int hz             = 2;
        final int sampleRate     = 10;

        final SignalGenerator sg = new SignalGenerator(hz, sampleRate, 10);
        final double[] real      = new double[128];
        final double[] img       = new double[real.length];
        final ConvolutionFilter filter = new ConvolutionFilter(new FilterKernelFactory(sampleRate).lowPass(hz));

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
    public void writeToSpeakers() throws IOException, LineUnavailableException
    {
        final double                hz         = 700;
        final int                   sampleRate = 11025;
        final SignalGenerator       sg         = new SignalGenerator(hz, sampleRate, 2);
        final double[]              buffer     = new double[sampleRate*5];
        final ConvolutionFilterStream filter = new ConvolutionFilterStream(new FilterKernelFactory(sampleRate).highPass(700));
        final byte[]                byteBuffer = new byte[buffer.length * 2];

        sg.read(buffer);

        final int writtenBytes = filter.apply(buffer);

        System.out.println("Bytes written "+writtenBytes+ " of "+buffer.length);

        for (int i = 0; i < buffer.length; ++i) {
            final short s = (short) (buffer[i] * 1000);
            byteBuffer[2*i]   = (byte)((s>>>8)&0xff);
            byteBuffer[2*i+1] = (byte)((s)&0xff);
        }

        /* Audio. */
        final AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, true);
        final SourceDataLine sdl  = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        sdl.write(byteBuffer, 0, byteBuffer.length);
        sdl.drain();
        sdl.stop();
        sdl.close();

        new java.io.FileOutputStream("start.raw").write(byteBuffer);
    }

    @Test
    public void book() throws IOException {
        final double[] h = { 1, 2, 1 };
        final double[] x = { 1, 2, 3, 4, 5, 6};
        final double[] r = { 1, 4, 8, 12, 16, 20, 17, 6 };

        final ConvolutionFilter f = new ConvolutionFilter(new FilterKernelRaw(h));
        ConvolutionFilterStream filter = new ConvolutionFilterStream(f);

        System.out.println("SIZE: "+f.sampleCount());

        f.filter(x);
        for (int i = 0; i < x.length; ++i) {
            System.out.println("1: "+x[i]+"\t"+r[i]);
            Assert.assertEquals(x[i], r[i], 0.1);
        }
        for (int i = x.length; i < r.length; ++i) {
            System.out.println("1: "+f.getOverlap()[i-x.length] +"\t"+r[i]);
            Assert.assertEquals(f.getOverlap()[i-x.length], r[i], 0.1);
        }

    }
}