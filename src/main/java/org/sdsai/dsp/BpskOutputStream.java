package org.sdsai.dsp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FilterOutputStream;

/**
 * BPSK encoder with a varaible carrier frequency and symbol rate.
 *
 * The default is 31.25 symbols per second.
 */
public class BpskOutputStream extends FilterOutputStream {

    public static final int[][] CHARSET = new int[256][];

    static {
        // NUL - Null
        CHARSET[0] = new int[]{1,0,1,0,1,0,1,0,1,1};
        // SOH - Start of Header
        CHARSET[1] = new int[]{1,0,1,1,0,1,1,0,1,1};
        // STX - Start of Text
        CHARSET[2] = new int[]{1,0,1,1,1,0,1,1,0,1};
        // ETX - End of Text
        CHARSET[3] = new int[]{1,1,0,1,1,1,0,1,1,1};
        // EOT - End of Transmission
        CHARSET[4] = new int[]{1,0,1,1,1,0,1,0,1,1};
        // ENQ - Enquiry
        CHARSET[5] = new int[]{1,1,0,1,0,1,1,1,1,1};
        // Ack - Acknowledgement
        CHARSET[6] = new int[]{1,0,1,1,1,0,1,1,1,1};
        // BEL - Bell
        CHARSET[7] = new int[]{1,0,1,1,1,1,1,1,0,1};
        // BS - back space.
        CHARSET[8] = new int[]{1,0,1,1,1,1,1,1,1,1};
        // HT - Horizontal Tab
        CHARSET[9] = new int[]{1,1,1,0,1,1,1,1};
        // LF - Line feed
        CHARSET[10] = new int[]{1,1,1,0,1} ;
        // VT - Vertical Tab
        CHARSET[11] = new int[]{1,1,0,1,1,0,1,1,1,1};
        // FF - Form feed
        CHARSET[12] = new int[]{1,0,1,1,0,1,1,1,0,1};
        // CR - Carriage return
        CHARSET[13] = new int[]{1,1,1,1,1} ;
        // SO - Shift Out
        CHARSET[14] = new int[]{1,1,0,1,1,1,0,1,0,1};
        // SI - Shift In
        CHARSET[15] = new int[]{1,1,1,0,1,0,1,0,1,1};
        // DLE - Data Link Escape
        CHARSET[16] = new int[]{1,0,1,1,1,1,0,1,1,1};
        // DC1 - Device Control 1 (XON)
        CHARSET[17] = new int[]{1,0,1,1,1,1,0,1,0,1};
        // DC2 - Device Control 2
        CHARSET[18] = new int[]{1,1,1,0,1,0,1,1,0,1};
        // DC3 - Device Control 3 (XOFF)
        CHARSET[19] = new int[]{1,1,1,0,1,0,1,1,1,1};
        // DC4 - Device Control 4
        CHARSET[20] = new int[]{1,1,0,1,0,1,1,0,1,1};
        // NAK - Negative Acknowledgement
        CHARSET[21] = new int[]{1,1,0,1,1,0,1,0,1,1};
        // SYN - Synchronous Idle
        CHARSET[22] = new int[]{1,1,0,1,1,0,1,1,0,1};
        // ETB - End of Trans. Block
        CHARSET[23] = new int[]{1,1,0,1,0,1,0,1,1,1};
        // CAN - Cancel
        CHARSET[24] = new int[]{1,1,0,1,1,1,1,0,1,1};
        // EM - End of Medium
        CHARSET[25] = new int[]{1,1,0,1,1,1,1,1,0,1};
        // SUB - Substitute
        CHARSET[26] = new int[]{1,1,1,0,1,1,0,1,1,1};
        // ESC - Escape
        CHARSET[27] = new int[]{1,1,0,1,0,1,0,1,0,1};
        // FS - File Separator
        CHARSET[28] = new int[]{1,1,0,1,0,1,1,1,0,1};
        // GS - Group Separator
        CHARSET[29] = new int[]{1,1,1,0,1,1,1,0,1,1};
        // RS - Record Separator
        CHARSET[30] = new int[]{1,0,1,1,1,1,1,0,1,1};
        // US - Unit Separator
        CHARSET[31] = new int[]{1,1,0,1,1,1,1,1,1,1};
        // DEL - Delete
        CHARSET[127] = new int[]{1,1,1,0,1,1,0,1,0,1};
        // SP
        CHARSET[32] = new int[]{1};
        // !
        CHARSET[33] = new int[]{1,1,1,1,1,1,1,1,1};
        // "
        CHARSET[34] = new int[]{1,0,1,0,1,1,1,1,1};
        // #
        CHARSET[35] = new int[]{1,1,1,1,1,0,1,0,1};
        // $
        CHARSET[36] = new int[]{1,1,1,0,1,1,0,1,1};
        // %
        CHARSET[37] = new int[]{1,0,1,1,0,1,0,1,0,1};
        // &
        CHARSET[38] = new int[]{1,0,1,0,1,1,1,0,1,1};
        // '
        CHARSET[39] = new int[]{1,0,1,1,1,1,1,1,1};
        // (
        CHARSET[40] = new int[]{1,1,1,1,1,0,1,1};
        // )
        CHARSET[41] = new int[]{1,1,1,1,0,1,1,1};
        // *
        CHARSET[42] = new int[]{1,0,1,1,0,1,1,1,1};
        // +
        CHARSET[43] = new int[]{1,1,1,0,1,1,1,1,1};
        // ,
        CHARSET[44] = new int[]{1,1,1,0,1,0,1};
        // -
        CHARSET[45] = new int[]{1,1,0,1,0,1};
        // .
        CHARSET[46] = new int[]{1,0,1,0,1,1,1};
        // /
        CHARSET[47] = new int[]{1,1,0,1,0,1,1,1,1};
        // 0
        CHARSET[48] = new int[]{1,0,1,1,0,1,1,1};
        // 1
        CHARSET[49] = new int[]{1,0,1,1,1,1,0,1};
        // 2
        CHARSET[50] = new int[]{1,1,1,0,1,1,0,1};
        // 3
        CHARSET[51] = new int[]{1,1,1,1,1,1,1,1};
        // 4
        CHARSET[52] = new int[]{1,0,1,1,1,0,1,1,1};
        // 5
        CHARSET[53] = new int[]{1,0,1,0,1,1,0,1,1};
        // 6
        CHARSET[54] = new int[]{1,0,1,1,0,1,0,1,1};
        // 7
        CHARSET[55] = new int[]{1,1,0,1,0,1,1,0,1};
        // 8
        CHARSET[56] = new int[]{1,1,0,1,0,1,0,1,1};
        // 9
        CHARSET[57] = new int[]{1,1,0,1,1,0,1,1,1};
        // :
        CHARSET[58] = new int[]{1,1,1,1,0,1,0,1};
        // ;
        CHARSET[59] = new int[]{1,1,0,1,1,1,1,0,1};
        // <
        CHARSET[60] = new int[]{1,1,1,1,0,1,1,0,1};
        // =
        CHARSET[61] = new int[]{1,0,1,0,1,0,1};
        // >
        CHARSET[62] = new int[]{1,1,1,0,1,0,1,1,1};
        // ?
        CHARSET[63] = new int[]{1,0,1,0,1,0,1,1,1,1};
        // @
        CHARSET[64] = new int[]{1,0,1,0,1,1,1,1,0,1};
        // A
        CHARSET[65] = new int[]{1,1,1,1,1,0,1};
        // B
        CHARSET[66] = new int[]{1,1,1,0,1,0,1,1};
        // C
        CHARSET[67] = new int[]{1,0,1,0,1,1,0,1};
        // D
        CHARSET[68] = new int[]{1,0,1,1,0,1,0,1};
        // E
        CHARSET[69] = new int[]{1,1,1,0,1,1,1};
        // F
        CHARSET[70] = new int[]{1,1,0,1,1,0,1,1};
        // G
        CHARSET[71] = new int[]{1,1,1,1,1,1,0,1};
        // H
        CHARSET[72] = new int[]{1,0,1,0,1,0,1,0,1};
        // I
        CHARSET[73] = new int[]{1,1,1,1,1,1,1};
        // J
        CHARSET[74] = new int[]{1,1,1,1,1,1,1,0,1};
        // K
        CHARSET[75] = new int[]{1,0,1,1,1,1,1,0,1};
        // L
        CHARSET[76] = new int[]{1,1,0,1,0,1,1,1};
        // M
        CHARSET[77] = new int[]{1,0,1,1,1,0,1,1};
        // N
        CHARSET[78] = new int[]{1,1,0,1,1,1,0,1};
        // O
        CHARSET[79] = new int[]{1,0,1,0,1,0,1,1};
        // P
        CHARSET[80] = new int[]{1,1,0,1,0,1,0,1};
        // Q
        CHARSET[81] = new int[]{1,1,1,0,1,1,1,0,1};
        // R
        CHARSET[82] = new int[]{1,0,1,0,1,1,1,1};
        // S
        CHARSET[83] = new int[]{1,1,0,1,1,1,1};
        // T
        CHARSET[84] = new int[]{1,1,0,1,1,0,1};
        // U
        CHARSET[85] = new int[]{1,0,1,0,1,0,1,1,1};
        // V
        CHARSET[86] = new int[]{1,1,0,1,1,0,1,0,1};
        // W
        CHARSET[87] = new int[]{1,0,1,0,1,1,1,0,1};
        // X
        CHARSET[88] = new int[]{1,0,1,1,1,0,1,0,1};
        // Y
        CHARSET[89] = new int[]{1,0,1,1,1,1,0,1,1};
        // Z
        CHARSET[90] = new int[]{1,0,1,0,1,0,1,1,0,1};
        // [
        CHARSET[91] = new int[]{1,1,1,1,1,0,1,1,1};
        // \\
        CHARSET[92] = new int[]{1,1,1,1,0,1,1,1,1};
        // ]
        CHARSET[93] = new int[]{1,1,1,1,1,1,0,1,1,1};
        // ^
        CHARSET[94] = new int[]{1,0,1,0,1,1,1,1,1,1};
        // _
        CHARSET[95] = new int[]{1,0,1,1,0,1,1,0,1};
        // `
        CHARSET[96] = new int[]{1,0,1,1,0,1,1,1,1,1};
        // a
        CHARSET[97] = new int[]{1,0,1,1};
        // b
        CHARSET[98] = new int[]{1,0,1,1,1,1,1};
        // c
        CHARSET[99] = new int[]{1,0,1,1,1,1};
        // d
        CHARSET[100] = new int[]{1,0,1,1,0,1};
        // e
        CHARSET[101] = new int[]{1,1};
        // f
        CHARSET[102] = new int[]{1,1,1,1,0,1};
        // g
        CHARSET[103] = new int[]{1,0,1,1,0,1,1};
        // h
        CHARSET[104] = new int[]{1,0,1,0,1,1};
        // i
        CHARSET[105] = new int[]{1,1,0,1};
        // j
        CHARSET[106] = new int[]{1,1,1,1,0,1,0,1,1};
        // k
        CHARSET[107] = new int[]{1,0,1,1,1,1,1,1};
        // l
        CHARSET[108] = new int[]{1,1,0,1,1};
        // m
        CHARSET[109] = new int[]{1,1,1,0,1,1};
        // n
        CHARSET[110] = new int[]{1,1,1,1};
        // o
        CHARSET[111] = new int[]{1,1,1};
        // p
        CHARSET[112] = new int[]{1,1,1,1,1,1};
        // q
        CHARSET[113] = new int[]{1,1,0,1,1,1,1,1,1};
        // r
        CHARSET[114] = new int[]{1,0,1,0,1};
        // s
        CHARSET[115] = new int[]{1,0,1,1,1};
        // t
        CHARSET[116] = new int[]{1,0,1};
        // u
        CHARSET[117] = new int[]{1,1,0,1,1,1};
        // v
        CHARSET[118] = new int[]{1,1,1,1,0,1,1};
        // w
        CHARSET[119] = new int[]{1,1,0,1,0,1,1};
        // x
        CHARSET[120] = new int[]{1,1,0,1,1,1,1,1};
        // y
        CHARSET[121] = new int[]{1,0,1,1,1,0,1};
        // z
        CHARSET[122] = new int[]{1,1,1,0,1,0,1,0,1};
        // {
        CHARSET[123] = new int[]{1,0,1,0,1,1,0,1,1,1};
        // |
        CHARSET[124] = new int[]{1,1,0,1,1,1,0,1,1};
        // }
        CHARSET[125] = new int[]{1,0,1,0,1,1,0,1,0,1};
        // ~
        CHARSET[126] = new int[]{1,0,1,1,0,1,0,1,1,1};
    }

    private Bpsk psk;
    private static final int[] CHARACTER_SEPARATOR = {0, 0};
    public BpskOutputStream(final OutputStream outputStream, final Bpsk psk) throws IOException {
        super(outputStream);
        this.psk = psk;
    }

    /**
     * Get a single character of PSK data.
     * This must be separated from other data with {0,0}.
     */
    private int[] getPskData(int b) throws IOException {
        if (b >= CHARSET.length || b < 0) {
            return CHARSET[0];
        }

        int[] i = CHARSET[b];
        if (i == null) {
            return CHARSET[0];
        }

        return i;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public void write(int b) throws IOException {
        out.write(psk.generateSignal(getPskData(b)));
        out.write(psk.generateSignal(CHARACTER_SEPARATOR));
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int size = 0;

        for (int i = off; i < off+len; ++i) {
            size += getPskData(b[i]).length + CHARACTER_SEPARATOR.length;
        }

        int[] symbols = new int[size];

        for (int i = off, sym_i = 0; i < off+len; ++i) {
            int[] symbol = getPskData(b[i]);

            /* Append symbol data. */
            for (int j = 0; j < symbol.length; ++j) {
                symbols[sym_i++] = symbol[j];
            }

            /* Append character separator sequence. */
            for (int j = 0; j < CHARACTER_SEPARATOR.length; j++) {
                symbols[sym_i++] = CHARACTER_SEPARATOR[j];
            }
        }

        final byte[] finalData = psk.generateSignal(symbols);

        out.write(finalData, 0, finalData.length);
    }

    public void preamble() throws IOException {
        out.write(psk.generateSignal(new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));
    }

    public void postamble() throws IOException {
        out.write(psk.generateSignal(new int[]{0,0,1,1,1,1,1,1,1,1,1,1}));
    }
}