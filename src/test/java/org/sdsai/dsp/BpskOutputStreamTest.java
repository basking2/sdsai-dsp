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
        BpskGenerator psk = new BpskGenerator(700, 11025, 31.25);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final SourceDataLine        sdl  = AudioSystem.getSourceDataLine(psk.getAudioFormat());
        final BpskOutputStream      os   = new BpskOutputStream(baos, psk);

        os.preamble();
        os.write("This is a very nice test.".getBytes());
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