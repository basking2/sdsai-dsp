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
        BpskGenerator p = new BpskGenerator(1000, 22000);

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
    public void encodeDecodeWithNoise() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1,0};
        byte[] b = p1.generateSignal(testPattern);

        /* Add some white noise. */
        for (int i = 0; i < b.length/2; i++) {

            short s1 = (short)(((b[2*i] << 8) & 0xff00) | (b[2*i+1] & 0xff));
            short s2 = (short)((Short.MAX_VALUE / 2) * Math.random());
            if (Math.random() > .5) {
                s1 += s2;
            }
            else {
                s1 -= s2;
            }

            b[2*i] = (byte)((s1>>8)&0xff);
            b[2*i+1] = (byte)((s1)&0xff);
        }

        byte[] checkPattern = p2.detectSignal(b, 0, b.length);

        Assert.assertArrayEquals(testPattern, checkPattern);
    }

    @Test
    public void encodeDecodeWithNoiseAndShift1() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        /* First value is a dummy value. */
        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1};
        byte[] b = p1.generateSignal(testPattern);

        /* Add some white noise. */
        for (int i = 0; i < b.length/2; i++) {

            short s1 = (short)(((b[2*i] << 8) & 0xff00) | (b[2*i+1] & 0xff));
            short s2 = (short)((Short.MAX_VALUE / 2) * Math.random());
            if (Math.random() > .5) {
                s1 += s2;
            }
            else {
                s1 -= s2;
            }

            b[2*i] = (byte)((s1>>8)&0xff);
            b[2*i+1] = (byte)((s1)&0xff);
        }

        /* Shift b with some zero audio. */
        byte[] b2 = new byte[(b.length / testPattern.length / 3)+b.length];
        for (int i = 1; i <= b.length; i++) {
            b2[b2.length-i] = b[b.length-i];
        }
        b = b2;

        byte[] checkPattern = p2.detectSignal(b, 0, b.length);

        printArrays("1", testPattern, checkPattern);

        /* Ignore the last character as it's stuck in the buffer due to phase shift. */
        for (int i = 0; i < testPattern.length-1; ++i) {
            Assert.assertEquals(testPattern[i], checkPattern[i]);
        }
    }

    @Test
    public void encodeDecodeWithNoiseAndShift2() throws IOException {
        BpskGenerator p1 = new BpskGenerator();
        BpskDetector p2 = new BpskDetector();

        byte[] testPattern = new byte[]{1,0,1,1,0,0,0,1,0,1,0,1,1,0,0,1};
        byte[] b = p1.generateSignal(testPattern);

        /* Shift b with some zero audio. */
        byte[] b2 = new byte[(b.length / testPattern.length / 2)+b.length];
        for (int i = 1; i <= b.length; i++) {
            b2[b2.length-i] = b[b.length-i];
        }
        b = b2;

        byte[] checkPattern = p2.detectSignal(b, 0, b.length);

        printArrays("2", testPattern, checkPattern);

        /* Ignore the last character as it's stuck in the buffer due to phase shift. */
        for (int i = 0; i < testPattern.length-1; ++i) {
            Assert.assertEquals(testPattern[i], checkPattern[i+1]);
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
}