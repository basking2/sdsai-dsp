package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import org.junit.Assert;
import javax.sound.sampled.AudioFormat;

import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * Run the dectecot on live {@code .wav} files.
 */
public class BpskDetectLiveTest {

    @Test
    public void readGenerated() throws IOException, UnsupportedAudioFileException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final BpskOutputStream os = new BpskOutputStream(bos, new BpskGenerator());
        final String testString = "This is a very nice test.";

        os.preamble(11);
        os.write(testString.getBytes());
        os.close();

        final BpskInputStream is = new BpskInputStream(
            new ByteArrayInputStream(bos.toByteArray()),
            new BpskDetector());

        final byte[] bytes = new byte[1024];

        String result = "";

        for (int read = is.read(bytes); read != -1; read = is.read(bytes)) {
            if (read > 0) {
                result += new String(bytes, 0, read);
            }
        }
        Assert.assertThat(result, containsString(testString));
    }

    @Test
    public void readFilteredLivePskFileLinpsk() throws IOException, UnsupportedAudioFileException {

        final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
            new AudioFormat(44100, 16, 1, true, true),
            AudioSystem.getAudioInputStream(getClass().getResourceAsStream("cq.wav")));

        final BpskInputStream is = new BpskInputStream(
            audioInputStream,
            new BpskDetector(
                1000,
                (int)audioInputStream.getFormat().getSampleRate(),
                BpskGenerator.PSK31_SYMBOLS_PER_SECOND));

        final byte[] bytes = new byte[1024];

        final int read = is.read(bytes);
        System.out.println(new String(bytes, 0, read));
        Assert.assertEquals(
            "CQ CQ CQ de N2SWT N2SWT K\n"+
            "CQ CQ CQ de N2SWT N2SWT K\n"+
            "No one out there? Too bad!\n"+
            "N2SWT SK",
            new String(bytes, 2, read-2));
    }

    @Test
    public void readLivePskFileLinpsk() throws IOException, UnsupportedAudioFileException {

        final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
            new AudioFormat(44100, 16, 1, true, true),
            AudioSystem.getAudioInputStream(getClass().getResourceAsStream("cq.wav")));

        final BpskInputStream is = new BpskInputStream(
            audioInputStream,
            new BpskDetector(
                1000,
                (int)audioInputStream.getFormat().getSampleRate(),
                BpskGenerator.PSK31_SYMBOLS_PER_SECOND));

        final byte[] bytes = new byte[1024];

        final int read = is.read(bytes);
        System.out.println(new String(bytes, 0, read));
        Assert.assertEquals(
            "CQ CQ CQ de N2SWT N2SWT K\n"+
            "CQ CQ CQ de N2SWT N2SWT K\n"+
            "No one out there? Too bad!\n"+
            "N2SWT SK",
            new String(bytes, 2, read-2));
    }

    @Test
    public void readLivePskFileDroid() throws IOException, UnsupportedAudioFileException {
        final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
            new AudioFormat(44100, 16, 1, true, true),
            AudioSystem.getAudioInputStream(getClass().getResourceAsStream("droid.wav")));

        final BpskInputStream is = new BpskInputStream(
            audioInputStream,
            new BpskDetector(
                700,
                (int)audioInputStream.getFormat().getSampleRate(),
                BpskGenerator.PSK31_SYMBOLS_PER_SECOND));

        final byte[] bytes = new byte[1024];

        final int read = is.read(bytes);
        System.out.println(new String(bytes, 0, read));

        Assert.assertTrue(
            new String(bytes, 0, read).contains("I am the very model of a modern, major, general."));
    }

    @Test
    public void readLivePskFileDroid2() throws IOException, UnsupportedAudioFileException {
        final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
            new AudioFormat(44100, 16, 1, true, true),
            AudioSystem.getAudioInputStream(getClass().getResourceAsStream("droid2.wav")));

        final BpskInputStream is = new BpskInputStream(
            audioInputStream,
            new BpskDetector(
                700,
                (int)audioInputStream.getFormat().getSampleRate(),
                BpskGenerator.PSK31_SYMBOLS_PER_SECOND));

        final byte[] bytes = new byte[1024];

        final int read = is.read(bytes);
        System.out.println(new String(bytes, 0, read));

        Assert.assertTrue(
            new String(bytes, 0, read).contains("I am the very model of a modern, major, general."));

    }

}