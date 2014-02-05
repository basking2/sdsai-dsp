package org.sdsai.dsp;

import java.io.FilterInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * BPSK decoder with a varaible carrier frequency and symbol rate.
 *
 * This class returns the raw symbols detected.
 *
 * The default is 31.25 symbols per second.
 */
public class BpskInputStream extends FilterInputStream {

    /**
     * The fill of {@link #symbolBuffer}.
     */
    private int symbolBufferFill;

    /**
     * The offset into the {@link #symbolBuffer} list.
     */
    private int symbolBufferOff;

    /**
     * The buffer of raw audio data to be converted into symbols.
     * This buffer is never left filled with data that could immediately be
     * turned into symbols. It is always at a minimum-fill level.
     */
    private byte[] dataBuffer;

    /**
     * Buffer holding symbols which will eventually be matched to character in varicode and returned.
     */
    private byte[] symbolBuffer;

    /**
     * The detector to use to decode varicode symbols.
     */
    private BpskDetector psk;

    /**
     * An special output stream that fills {@link #symbolBuffer}.
     */
    private SymbolBufferOutputStream bufferFiller;

    private static final CharsetTree CHARSETTREE = new CharsetTree(-1, 0);

    /**
     * The longest character sequence, not counting the two terminating 00 bytes.
     * This is computed at class load time when {@link #CHARSETTREE}.
     */
    private static final int LONGEST_CHARACTER;

    static {
        int longestCharacter = 0;

        for (int i = 0; i < BpskOutputStream.CHARSET.length; ++i) {
            if (BpskOutputStream.CHARSET[i] != null) {
                CHARSETTREE.addChar(i, BpskOutputStream.CHARSET[i]);
                if (longestCharacter < BpskOutputStream.CHARSET[i].length) {
                    longestCharacter = BpskOutputStream.CHARSET[i].length;
                }
            }
        }

        LONGEST_CHARACTER = longestCharacter;
    }

    /**
     * A tree representing a Varicode dictionary.
     *
     * This class is used to lookup potential matches to varicode symbol strings.
     */
    private static class CharsetTree {

        /**
         * The next character in a path is a 0. Go left.
         */
        public CharsetTree left;

        /**
         * The next character in a path is a 1. Go right.
         */
        public CharsetTree right;

        /**
         * The character this is defined as.
         */
        public int c;

        /**
         * Depth in the tree. This starts at 1, so it may also
         * be viewed as the length of the path required to
         * match this caracter, or the amount of the path
         * consumed by a successful match.
         */
        public final int depth;

        /**
         * Constructor.
         *
         * @param c The character this node will represent. -1 for no character.
         * @param depth The legth of the sequence, also the depth in the
         *        tree that this character resides at.
         */
        public CharsetTree(
            final int c,
            final int depth
        )
        {
            this.c     = c;
            this.depth = depth;
            this.right = null;
            this.left  = null;
        }

        /**
         * Add a new character to this tree at the given path.
         *
         * @param c The character. Use -1 to indicate no character, such as in the case
         *        of an intermediate node.
         * @param path A sequcne of 0s (left) and 1s (right) path selection choices that define
         *        where this character is placed in the tree. This character's depth is set to
         *        path.length.
         * @throws IllegalArgumentException if a path element is not 1 or 0 or
         *         if the character is already defined.
         */
        public void addChar(int c, byte[] path) {
            CharsetTree ct = this;

            for (int i = 0; i < path.length; ++i) {
                if (path[i] == 0) {
                    if (ct.left == null) {
                        ct.left = new CharsetTree(-1, ct.depth+1);
                    }
                    ct = ct.left;
                }
                else if (path[i] == 1) {
                    if (ct.right == null) {
                        ct.right = new CharsetTree(-1, ct.depth+1);
                    }
                    ct = ct.right;
                }
                else {
                    throw new IllegalArgumentException("Expected 1 or 0 and got "+path[i]);
                }
            }

            if (ct.c != -1) {
                throw new IllegalArgumentException("Character "+c+"'s path is already occupided by character "+ct.c);
            }
            ct.c = c;
        }

        /**
         * Return the CharsetTree value that matches the longest
         * path in the given argument, path.
         *
         * @param path A byte array in which a value of 0 goes down the left
         *        subtree and a value of 1 goes down the right subtree.
         *        Any other value results in an IllegalArgumentException being
         *        thrown.
         * @param off Offset into path.
         * @param len The lenth of character to consider in path.
         *
         * @throws IllegalArgumentException if an element in path is not 0 or 1.
         * @return The CharsetTree of the longest match. If no part of the path matched, this
         *         is returned.
         */
        public CharsetTree findChar(final byte[] path, final int off, final int len) {
            CharsetTree ct        = this;
            CharsetTree lastValid = ct;

            for (int i = 0; i < len; ++i) {
                CharsetTree next;
                if (path[i+off] == 0) {
                    next = ct.left;
                }
                else if (path[i+off] == 1) {
                    next = ct.right;
                }
                else {
                    throw new IllegalArgumentException("Expected 1 or 0 and got "+path[i]);
                }

                if (next == null) {
                    break;
                }

                ct = next;
                if (ct.c != -1) {
                    lastValid = ct;
                }
            }

            /* If we consume the entire path... well... match! Return ct. */
            return lastValid;
        }
    }
    /**
     * Constructor.
     *
     * @param in {@link InputStream} to read audio data from.
     * @param psk Ths detector to use to detect a signal in {@code in}.
     */
    public BpskInputStream(final InputStream in, final BpskDetector psk) {
        super(in);
        this.symbolBuffer     = new byte[100];
        this.dataBuffer       = new byte[(int) (
                (double)psk.getFrameSize() *
                (double)psk.getSampleRate() /
                psk.getSymbolRate()
            )];
        this.symbolBufferFill = 0;
        this.symbolBufferOff  = 0;
        this.psk              = psk;
        this.bufferFiller     = new SymbolBufferOutputStream();
    }

    /**
     * Remove the given offset from the symbol buf.
     */
    private void compactSymbolBuffer() {
        for (int i = symbolBufferOff; i < symbolBufferFill; ++i) {
            symbolBuffer[i - symbolBufferOff] = symbolBuffer[i];
        }
        symbolBufferFill = symbolBufferFill - symbolBufferOff;
        symbolBufferOff = 0;
    }

    /**
     * Get a batch of symbols.
     * An offset into the symbolBuffer is returned if it is necessary.
     * This method may wipe the entire symbolBuffer and replace it.
     *
     * @return -1 if no symbols can be fetched, the number of symbols fetched otherwise.
     */
    private int getSymbols() throws IOException {
        final int symbols = symbolBufferFill;

        /* Read into the data buffer. */
        final int bytesRead = in.read(dataBuffer);

        /* Exit the while loop. It may be that we cannot match anything. */
        if (bytesRead == -1) {
            return -1;
        }

        /* Fill the symbol buffer. */
        psk.detectSignal(dataBuffer, 0, bytesRead, bufferFiller);

        /* FIXME - this is somewhat meaningless. */
        return symbolBufferFill - symbols;
    }

    /**
     * Perform maintenance on the symbol buffer.
     *
     * This entails two things.
     * <ul>
     * <li>If the buffer cannot hold 10+2 character, compact it by copying all
     *     characters after the offset to index 0 and adjusting the fill value.</li>
     * <li>Ensure that, if possible, there are 10 + 2 characters in the buffer,
     *     the longest Varicode character we will match.</li>
     * </ul>
     *
     * @return If the buffer cannot be filled to > 0.
     */
    private boolean bufferMaintenance() throws IOException {
        /* Ensure there is LONGEST_CHARACTER + 2 space in the symbolBuffer. */
        if (symbolBuffer.length - symbolBufferOff < LONGEST_CHARACTER + 2) {
            compactSymbolBuffer();
        }

        /* Now, if there are not enough symbols in the buffer, fill it. */
        while (symbolBufferFill - symbolBufferOff < LONGEST_CHARACTER + 2) {
            if (getSymbols() == -1) {
                if (symbolBufferFill - symbolBufferOff < 1) {
                    return false;
                }
                else {
                    return true;
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {

        CharsetTree ct = CHARSETTREE;
        int bytesRead = 0;

        while (true) {
            /* Optimization - skip any leading zeros. */
            // while(symbolBufferOff < symbolBufferFill && symbolBuffer[symbolBufferOff] == 0) {
            //     ++symbolBufferOff;
            // }

            /* Refill the buffer, compacting it if necessary. */
            if (!bufferMaintenance()) {
                return -1;
            }

            /* Search for a matching character. */
            ct = CHARSETTREE.findChar(symbolBuffer, symbolBufferOff, symbolBufferFill-symbolBufferOff);

            /* Take action based on the match. */
            if (ct == CHARSETTREE) {
                /* An unknown character was recieved. Skip 1 symbol and try again. */
                symbolBufferOff++;
            }
            else if (ct.c == -1) {
                /* Unknonwn character. Skip body. */
                symbolBufferOff++;
            }
            else if (
                symbolBufferOff + ct.depth + 2 <= symbolBufferFill &&
                symbolBuffer[symbolBufferOff + ct.depth] == 0 &&
                symbolBuffer[symbolBufferOff + ct.depth + 1] == 0
            )
            {
                symbolBufferOff += ct.depth + 2;
                return ct.c;
            }
            else {
                symbolBufferOff++;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, int off, int len) throws IOException {

        for (int i = off; i < off+len; ++i ){
            int d = read();
            if (d == -1) {
                return ((i-off) == 0)? -1: (i-off);
            }

            b[i] = (byte) d;
        }

        return len;
    }

    /**
     * Private writer to collect symbols generated by {@link BpskDetector}.
     *
     * Only the {@link OutputStream#write(int)} needs to be implemented
     * as only that method is called in the implementation of {@link BpskDetector}.
     */
    private class SymbolBufferOutputStream extends OutputStream {
        @Override
        public void write(int symbol) throws IOException {
            assert(symbol == 0 || symbol == 1);

            if ( symbolBufferFill >= symbolBuffer.length) {
                /* This code should never, in practice, execute. But it's here. */
                symbolBuffer = Arrays.copyOf(symbolBuffer, 2*symbolBuffer.length);
            }

            symbolBuffer[symbolBufferFill++] = (byte)symbol;
        }
    }

    /**
     * Perform an internal check on the character set tree for consistency.
     */
    public void validateTree() {
        for (int i = 0; i < BpskOutputStream.CHARSET.length; ++i) {
            if (BpskOutputStream.CHARSET[i] != null) {
                CharsetTree ct = CHARSETTREE.findChar(
                        BpskOutputStream.CHARSET[i],
                        0,
                        BpskOutputStream.CHARSET[i].length);

                if (i != (int)ct.c) {
                    throw new RuntimeException(String.format("Failed to find char %c (%d)", (char)i, i));
                }
                if (BpskOutputStream.CHARSET[i].length != ct.depth) {
                    throw new RuntimeException("Depth mismatch");
                }
            }
        }
    }

    /**
     * The size of the internal buffer used to collect data.
     *
     * For best performance an audio provider should provide
     * about this much data, in bytes, to optimize throughput.
     */
    public int getBufferSize() {
        return dataBuffer.length;
    }
}