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

public class BpskDetectGeneratedTest
{
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
}