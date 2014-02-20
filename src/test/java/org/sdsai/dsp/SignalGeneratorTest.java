package org.sdsai.dsp;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;

public class SignalGeneratorTest {
    @Test
    public void generate1hz() {
        final int hz = 1;
        final short amplitude = 100;
        short[] s = new short[16];
        SignalGenerator g = new SignalGenerator(hz, s.length, amplitude);

        assertEquals(s.length, g.read(s));

        /* Check the nodes. */
        assertEquals(0, s[0]);
        assertEquals(0, s[s.length/2]);

        /* Check the peaks. */
        assertEquals(amplitude, s[s.length/4]);
        assertEquals(-amplitude, s[s.length/4*3]);

        /* Detect the inverse wave. */
        for (int i = 0; i < s.length/2; ++i) {
            int j = i+s.length / 2;
            assertEquals(s[i], -s[j]);
        }
    }

    @Test
    public void generate2hz() {
        final int hz = 2;
        final short amplitude = 100;
        short[] s = new short[16];
        SignalGenerator g = new SignalGenerator(hz, s.length, amplitude);

        assertEquals(s.length, g.read(s));

        /* Check the nodes. */
        assertEquals(0, s[0]);
        assertEquals(0, s[s.length/4]);
        assertEquals(0, s[s.length/4*2]);
        assertEquals(0, s[s.length/4*3]);

        /* Check the peaks. */
        assertEquals(amplitude, s[s.length/8]);
        assertEquals(-amplitude, s[s.length/8*3]);
        assertEquals(amplitude, s[s.length/8*5]);
        assertEquals(-amplitude, s[s.length/8*7]);

        /* Detect second wave. */
        for (int i = 0; i < s.length/2; ++i) {
            int j = i+s.length / 2;
            assertEquals(s[i], s[j]);
        }
    }
}