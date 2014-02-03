package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

import java.io.IOException;
import org.junit.Assert;

public class BpskGeneratorTest {
    @Test
    @Ignore
    public void playArray() throws LineUnavailableException, IOException {
        BpskGenerator p = new BpskGenerator(1000, 44100);

        SourceDataLine sdl = AudioSystem.getSourceDataLine(p.getAudioFormat());

        byte[] b = p.generateSignal(
            new byte[]{
                0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 0, 0
            }
        );

        sdl.open(p.getAudioFormat());
        sdl.start();

        sdl.write(b, 0, b.length);

        sdl.drain();
        sdl.stop();
        sdl.close();

    }

    @Test
    public void encodeDecode() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1};
        byte[] b = p1.generateSignal(testPattern);
        byte[] checkPattern = p2.detectSignal(b, 0, b.length);

        Assert.assertArrayEquals(testPattern, checkPattern);
    }

    @Test
    public void encodeDecodeAudioSkewUp() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1};
        byte[] b = p1.generateSignal(testPattern);
        for (int i = 0; i < b.length; ++i){
            b[i] += 128;
        }
        byte[] checkPattern = p2.detectSignal(b, 0, b.length);

        Assert.assertArrayEquals(testPattern, checkPattern);
    }

    @Test
    public void encodeDecodeAudioSkewDown() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1};
        byte[] b = p1.generateSignal(testPattern);
        for (int i = 0; i < b.length; ++i){
            b[i] -= 128;
        }
        byte[] checkPattern = p2.detectSignal(b, 0, b.length);

        Assert.assertArrayEquals(testPattern, checkPattern);
    }

    @Test
    public void encodeDecodeWithNoise() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1,0};
        byte[] b = p1.generateSignal(testPattern);

        addWhiteNoise(b, 0.5);

        byte[] checkPattern = p2.detectSignal(b);

        Assert.assertArrayEquals(testPattern, checkPattern);
    }

    @Test
    public void encodeDecodeWithNoiseAndShift1() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        /* First value is a dummy value. */
        byte[] testPattern = new byte[]{1,0,0,0,1,0,1,1,0,0,0,1};
        byte[] b = p1.generateSignal(testPattern);

        addWhiteNoise(b, 0.5);

        b = prefixWithSilence(b, 0.333);

        byte[] checkPattern = p2.detectSignal(b);

        printArrays("encodeDecodeWithNoiseAndShift1", testPattern, checkPattern);

        /* Ignore the last character as it's stuck in the buffer due to phase shift. */
        for (int i = 0; i < testPattern.length-1; ++i) {
            Assert.assertEquals(testPattern[i], checkPattern[i+4]);
        }
    }

    @Test
    public void encodeDecodeWithNoiseAndShift2() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1,0,1,0,1,1,0,0,1};
        byte[] b = p1.generateSignal(testPattern);

        addWhiteNoise(b, 0.5);

        b = prefixWithSilence(b, 0.5);

        byte[] checkPattern = p2.detectSignal(b);

        printArrays("encodeDecodeWithNoiseAndShift2", testPattern, checkPattern);

        /* Ignore the last character as it's stuck in the buffer due to phase shift. */
        for (int i = 0; i < testPattern.length-1; ++i) {
            Assert.assertEquals(testPattern[i], checkPattern[i+8]);
        }
    }

    @Test
    public void encodeDecode1000Hz100SymRate() throws IOException {
        final BpskGenerator gen = new BpskGenerator(1000, 44100, 100);
        final BpskDetector det = new BpskDetector(1000, 44100, 100);

        final byte[] testPattern = new byte[]{1,0,1,0,1,0,1,0,1,1,0,0,0,1,0,1,0,1,1,0,0,1};
        final byte[] sig = prefixWithSilence(
            addWhiteNoise(
                gen.generateSignal(testPattern), 0.5), 0.5);

        final byte[] checkPattern = det.detectSignal(sig);

        printArrays("encodeDecode1000Hz100SymRate", testPattern, checkPattern);

        /* Ignore the last character as it's stuck in the buffer due to phase shift. */
        for (int i = 0; i < testPattern.length-1; ++i) {
            Assert.assertEquals(testPattern[i], checkPattern[i+11]);
        }

    }

    private void printArrays(String name, byte[] expect, byte[] test) {
        System.out.println("----"+name+"----");
        for (int i = 0; i < Math.max(expect.length, test.length); i++) {
            if (i < expect.length) {
                System.out.print("expect: "+expect[i]);
            }
            else {
                System.out.print("expect: -");
            }

            if (i < test.length) {
                System.out.println(" test: "+test[i]);
            }
            else {
                System.out.println(" test: -");
            }
        }
        System.out.println("---- end "+name+"----");
    }

    private static byte[] addWhiteNoise(final byte[] b, final double p) {
        /* Add some white noise. */
        for (int i = 0; i < b.length/2; i++) {

            short s1 = (short)(((b[2*i] << 8) & 0xff00) | (b[2*i+1] & 0xff));
            short s2 = (short)(Short.MAX_VALUE * p * Math.random());

            if (Math.random() > .5) {
                s1 += s2;
            }
            else {
                s1 -= s2;
            }

            b[2*i] = (byte)((s1>>8)&0xff);
            b[2*i+1] = (byte)((s1)&0xff);
        }

        return b;
    }

    private byte[] prefixWithSilence(final byte[] b, final double p) {
        byte[] b2 = new byte[(int)(b.length * p + b.length)];

        for (int i = 1; i <= b.length; i++) {
            b2[b2.length-i] = b[b.length-i];
        }

        return b2;
    }
}