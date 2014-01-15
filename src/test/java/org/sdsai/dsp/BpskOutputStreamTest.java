package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class BpskOutputStreamTest {

    @Test
    public void writeToSpeakers() throws IOException, LineUnavailableException {
        Bpsk psk = new Bpsk(700, 11025, 31.25);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final SourceDataLine        sdl  = AudioSystem.getSourceDataLine(psk.getAudioFormat());
        final BpskOutputStream      os   = new BpskOutputStream(baos, psk);

        os.preamble();
        os.write("This is a very nice test. CQ CQ CQ de N2SWT N2SWT K".getBytes());
        os.write("Welcome to Wikipedia, the free encyclopedia that anyone can edit.".getBytes());
        os.postamble();
        os.close();

        sdl.open(psk.getAudioFormat());
        sdl.start();

        final byte[] b = baos.toByteArray();

        sdl.write(b, 0, b.length);

        sdl.drain();
        sdl.stop();
        sdl.close();

        java.io.FileOutputStream fos = new java.io.FileOutputStream("a.raw");
        fos.write(b);
        fos.close();
    }
}