package org.sdsai.dsp;

import java.io.InputStream;
import java.io.IOException;

/**
 * BPSK decoder with a varaible carrier frequency and symbol rate.
 *
 * The default is 31.25 symbols per second.
 */
public class BpskInputStream extends InputStream {

    @Override
    public int read() throws IOException {
        // FIXME - srb
        return -1;
    }
}