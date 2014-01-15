package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

import java.io.IOException;


public class BpskTest {
    @Test
    @Ignore
    public void playArray() throws LineUnavailableException, IOException {
        Bpsk p = new Bpsk(1000, 22000);

        SourceDataLine sdl = AudioSystem.getSourceDataLine(p.getAudioFormat());

        byte[] b = p.generateSignal(
            new int[]{
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
}