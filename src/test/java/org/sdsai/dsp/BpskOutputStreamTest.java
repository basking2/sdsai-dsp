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
        final BpskGenerator         psk  = new BpskGenerator(700, 11025, 31.25);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        /* Output. */
        final BpskOutputStream      os   = new BpskOutputStream(baos, psk);
        os.preamble(10);
        os.write("This is a very nice test.".getBytes());
        os.postamble(10);
        os.close();

        /* Audio. */
        final SourceDataLine sdl  = AudioSystem.getSourceDataLine(psk.getAudioFormat());
        sdl.open(psk.getAudioFormat());
        sdl.start();
        sdl.write(baos.toByteArray(), 0, baos.size());
        sdl.drain();
        sdl.stop();
        sdl.close();
    }

    @Test
    public void writeToSpeakers2x() throws IOException, LineUnavailableException {
        final BpskGenerator         psk  = new BpskGenerator(700, 11025, 62.50);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        /* Output. */
        final BpskOutputStream      os   = new BpskOutputStream(baos, psk);
        os.preamble(10);
        os.write("This is a very nice test.".getBytes());
        os.postamble(10);
        os.close();

        /* Audio. */
        final SourceDataLine sdl  = AudioSystem.getSourceDataLine(psk.getAudioFormat());
        sdl.open(psk.getAudioFormat());
        sdl.start();
        sdl.write(baos.toByteArray(), 0, baos.size());
        sdl.drain();
        sdl.stop();
        sdl.close();
    }
}