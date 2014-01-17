package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import org.junit.Assert;

public class BpskDetectorTest {

    @Test
    public void readLivePskFile() throws IOException, UnsupportedAudioFileException {

        ByteArrayOutputStream os  = new ByteArrayOutputStream();
        BpskDetector          psk = new BpskDetector();
        AudioInputStream      is  = AudioSystem.getAudioInputStream(getClass().getResourceAsStream("cq.wav"));

        /* Wrap the incoming audio in a coversion. */
        is = AudioSystem.getAudioInputStream(psk.getAudioFormat(), is);

        /* Just a buffer to move the data around in. */
        byte[] buffer = new byte[100];

        for (int bytesread = is.read(buffer); bytesread > -1; bytesread = is.read(buffer)) {
            os.write(buffer, 0, bytesread);
        }

        byte[] output = psk.detectSignal(os.toByteArray());

        for (int i = 0; i < output.length; ++i) {
            System.out.println(i + "= "+output[i]);
        }
    }

}