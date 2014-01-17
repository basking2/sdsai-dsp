package org.sdsai.dsp;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

/**
 * Generate or decode a BPSK signal.
 *
 * This operates on 16 bit audio samples.
 */
public class BpskGenerator
{
    /**
     * PSK31 has a symbol rate of 31.25 symbols per second.
     */
    public static final double PSK31_SYMBOLS_PER_SECOND = 31.25;

    /**
     * Default frequency of the audio tone to use.
     */
    public static final double DEFAULT_FREQUENCY = 1000;

    /**
     * Default sample rate used to generate audio.
     */
    public static final int DEFAULT_SAMPLE_RATE = 44100;

    /**
     * The target frequency to generate or decode a PSK31 signal on.
     */
    private double hz;

    /**
     * The audio sample rate. Typically 11025, 22050, or 44100.
     */
    private int sampleRate;

    /**
     * Current audio sample that this object will generate.
     *
     * To maintain a clean sinusoidal wave across multiple calls
     * to audio-generating methods in this class, the current
     * audio sample is maintained here. It starts at 0 and increases
     * over the life of this object.
     */
    private long currentSample;

    /**
     * The number of radians a single audio sampel represents.
     */
    private double radiansPerSample;

    /**
     * The rate that symbols are generated per second.
     *
     * This is used to compute how many samples to populate to represent a symbol.
     */
    private double symbolsPerSecond;

    /**
     * How many samples are required to encode a symbole?
     */
    private double samplesPerSymbol;

    /**
     * Coefficients that are used to scale the start of a new symbol.
     */
    private double[] symbolStartFilter;

    /**
     * Coefficients that are used to scale the start of a new symbol.
     */
    private double[] symbolEndFilter;

    /**
     * Build an instance with sensible defaults.
     */
    public BpskGenerator() {
        this(DEFAULT_FREQUENCY, DEFAULT_SAMPLE_RATE, PSK31_SYMBOLS_PER_SECOND);
    }

    /**
     * Constructor.
     *
     * @param hz Frequency of carrier.
     * @param sampleRate The rate at which the signal is generated or recieved.
     */
    public BpskGenerator(final double hz, final int sampleRate) {
        this(hz, sampleRate, PSK31_SYMBOLS_PER_SECOND);
    }

    public BpskGenerator(final double hz, final int sampleRate, double symbolsPerSecond) {
        this.hz               = hz;
        this.sampleRate       = sampleRate;
        this.symbolsPerSecond = symbolsPerSecond;
        this.samplesPerSymbol = this.sampleRate / this.symbolsPerSecond;
        this.radiansPerSample = this.hz * 2.0 * Math.PI / (double)sampleRate;
        this.currentSample    = 0;

        /* Two 1/4 wave arrays to hold shaping coeeficients. */
        symbolStartFilter = new double[(int)(this.sampleRate / this.symbolsPerSecond / 2.0)];
        symbolEndFilter   = new double[(int)(this.sampleRate / this.symbolsPerSecond / 2.0)];

        /* Cos filter the wave (yes, we use sin to generate the coefficients). */
        for (int i = 0; i < symbolStartFilter.length; ++i) {
            symbolStartFilter[i] = Math.sin(i * Math.PI / this.samplesPerSymbol);
            symbolEndFilter[i]   = Math.cos(i * Math.PI / this.samplesPerSymbol);
        }
    }

    public double getSymbolRate() {
        return this.symbolsPerSecond;
    }

    /**
     * Generate a Cos wave with a sin filter around it.
     */
    private byte[] generateTone(
        final double hz,
        final int sampleRate,
        final double shift,
        final double duration)
    {
        byte[] buffer = new byte[(int)(sampleRate * duration + 1D)];
        double radians = hz * 2 * Math.PI / (double)sampleRate;

        /* Generate the shifted cos wave. */
        for (int i = 0; i < buffer.length; ++i) {
            double amplitude = Math.cos(radians * i + shift);
            buffer[i] = (byte)((double)Byte.MAX_VALUE * amplitude);
        }

        /* Sin filter the wave. */
        for (int i = 0; i < buffer.length; ++i) {
            buffer[i] = (byte)((double)buffer[i] * Math.sin(i * Math.PI/buffer.length));
        }

        return buffer;
    }

    /**
     * Generate a single symbol tone into the given buffer.
     * Writing begins at buf_i and continues to samplesPerSymbol
     * bytes are written into the array.
     * @param buffer The buffer to write into.
     * @param sample Which audio sample are we generating in buffer? This is
     *        converted into an index into buffer by the expression 2*sample.
     * @param shift The number of radians to shift the signal by.
     *        In PSK {@link Math.PI} will shift the signal 180
     *        degrees, turning the normal {@code 1} into {@code 0}.
     * @return A new value for sample.
     */
    private int generateSignal(
        final byte[]  buffer,
        int           sample,
        final double  shift
    )
    {
        for (int i = 0; i < (int)samplesPerSymbol; ++i)
        {
            final double amplitude = Math.cos((radiansPerSample * currentSample++) + shift);

            short s = (short) (Short.MAX_VALUE * 0.8 * amplitude);

            buffer[2*sample] = (byte) ((s >> 8) & 0xff);
            buffer[2*sample+1] = (byte) ((s) & 0xff);
            sample++;
        }

        return sample;
    }

    private void fade(final byte[] buffer, int sample, double[] filter) {
        for (int i = 0; i < filter.length; i++) {

            final int buf_i = (sample + i) * 2;
            short s = (short)(((buffer[buf_i] << 8) & 0xff00) | (buffer[buf_i+1] & 0xff));

            s = (short)((double)s * filter[i]);

            buffer[buf_i] = (byte)((s>>8)&0xff);
            buffer[buf_i+1] = (byte)((s)&0xff);
        }
    }

    /**
     * Fade in 1/2 a symbol worth of the buffer.
     * @param buffer The buffer to write to.
     * @param buf_i The offset into buffer to start writing at.
     */
    private void fadeIn(final byte[] buffer, int sample) {
        fade(buffer, sample, symbolStartFilter);
    }

    /**
     * Fade out 1/2 a symbol worth of the buffer.
     * @param buffer The buffer to write to.
     * @param buf_i The offset into buffer to start writing at.
     */
    private void fadeOut(final byte[] buffer, int sample) {
        fade(buffer, sample, symbolEndFilter);
    }

    public byte[] generateSignal(final byte[] symbols) throws IOException {
        return generateSignal(symbols, 0, symbols.length);
    }

    /**
     * Given an array of 1s or 0s this will generate a PSK audio array.
     *
     * @param symbols The array of 1s and 0s. Any other value is an error.
     *
     * @throws IOException if a value in {@param symbols} is not a 1 or 0.
     * @return Array representing and audio segment of modulated PSK.
     */
    public byte[] generateSignal(final byte[] symbols, final int off, final int len) throws IOException {

        if (len == 0 ) {
            return new byte[0];
        }

        /* How many audio samples must we generate? */
        final int    samples = (int)(len * (int)samplesPerSymbol);

        /* We generate 16 bit audio, so there are 2 bytes per sample. */
        final byte[] buffer  = new byte[2 * samples];

        /* The previous symbol is always different than the very first symbol we get in symbols[]. */
        double shift      = 0;
        int    sample     = 0;

        sample = generateSignal(buffer, sample, shift);

        for (int sym_i = 1; sym_i < len; ++sym_i) {

            final byte symbol = symbols[off+sym_i];
            if (symbol == 0) {
                shift = (shift == 0) ? Math.PI : 0;

                fadeOut(buffer, (int)(sample - samplesPerSymbol/2));

                sample = generateSignal(buffer, sample, shift);

                fadeIn(buffer, (int)(sample - samplesPerSymbol));
            }
            else if (symbol == 1) {
                sample = generateSignal(buffer, sample, shift);
            }
            else {
                throw new IOException("Symbol at index "+sym_i+" was not a 1 or 0.");
            }
        }

        /* Fade in at the start. */
        fadeIn(buffer, 0);

        /* Fade out at the end. */
        fadeOut(buffer, samples - symbolEndFilter.length);

        return buffer;
    }


    /**
     * Return the audio format this class generates.
     */
    AudioFormat getAudioFormat() {
        /* Do not change this. This class produces big-endian 16bit signed audio. */
        return new AudioFormat(sampleRate, 16, 1, true, true);
    }

    static class Alphabet {
/*
1. All characters are separated from each other by two consecutive 0 bits.
2. No character contains more than one consecutive 0 bit.

var oct dec hex
1010101011  000     0   00  NUL     Null character
1011011011  001     1   01  SOH     Start of Header
1011101101  002     2   02  STX     Start of Text
1101110111  003     3   03  ETX     End of Text
1011101011  004     4   04  EOT     End of Transmission
1101011111  005     5   05  ENQ     Enquiry
1011101111  006     6   06  ACK     Acknowledgment
1011111101  007     7   07  BEL     Bell
1011111111  010     8   08  BS  Backspace
11101111    011     9   09  HT  Horizontal Tab
11101   012     10  0A  LF  Line feed
1101101111  013     11  0B  VT  Vertical Tab
1011011101  014     12  0C  FF  Form feed
11111   015     13  0D  CR  Carriage return
1101110101  016     14  0E  SO  Shift Out
1110101011  017     15  0F  SI  Shift In
1011110111  020     16  10  DLE     Data Link Escape
1011110101  021     17  11  DC1     Device Control 1 (XON)
1110101101  022     18  12  DC2     Device Control 2
1110101111  023     19  13  DC3     Device Control 3 (XOFF)
1101011011  024     20  14  DC4     Device Control 4
1101101011  025     21  15  NAK     Negative Acknowledgement
1101101101  026     22  16  SYN     Synchronous Idle
1101010111  027     23  17  ETB     End of Trans. Block
1101111011  030     24  18  CAN     Cancel
1101111101  031     25  19  EM  End of Medium
1110110111  032     26  1A  SUB     Substitute
1101010101  033     27  1B  ESC     Escape
1101011101  034     28  1C  FS  File Separator
1110111011  035     29  1D  GS  Group Separator
1011111011  036     30  1E  RS  Record Separator
1101111111  037     31  1F  US  Unit Separator
1110110101  177     127     7F  DEL     Delete


1   040     32  20  SP
111111111   041     33  21  !
101011111   042     34  22  "
111110101   043     35  23  #
111011011   044     36  24  $
1011010101  045     37  25  %
1010111011  046     38  26  &
101111111   047     39  27  '
11111011    050     40  28  (
11110111    051     41  29  )
101101111   052     42  2A  *
111011111   053     43  2B  +
1110101     054     44  2C  ,
110101  055     45  2D  -
1010111     056     46  2E  .
110101111   057     47  2F  /
10110111    060     48  30  0
10111101    061     49  31  1
11101101    062     50  32  2
11111111    063     51  33  3
101110111   064     52  34  4
101011011   065     53  35  5
101101011   066     54  36  6
110101101   067     55  37  7
110101011   070     56  38  8
110110111   071     57  39  9
11110101    072     58  3A  :
110111101   073     59  3B  ;
111101101   074     60  3C  <
1010101     075     61  3D  =
111010111   076     62  3E  >
1010101111  077     63  3F  ?

1010111101  100     64  40  @
1111101     101     65  41  A
11101011    102     66  42  B
10101101    103     67  43  C
10110101    104     68  44  D
1110111     105     69  45  E
11011011    106     70  46  F
11111101    107     71  47  G
101010101   110     72  48  H
1111111     111     73  49  I
111111101   112     74  4A  J
101111101   113     75  4B  K
11010111    114     76  4C  L
10111011    115     77  4D  M
11011101    116     78  4E  N
10101011    117     79  4F  O
11010101    120     80  50  P
111011101   121     81  51  Q
10101111    122     82  52  R
1101111     123     83  53  S
1101101     124     84  54  T
101010111   125     85  55  U
110110101   126     86  56  V
101011101   127     87  57  W
101110101   130     88  58  X
101111011   131     89  59  Y
1010101101  132     90  5A  Z
111110111   133     91  5B  [
111101111   134     92  5C  \
111111011   135     93  5D  ]
1010111111  136     94  5E  ^
101101101   137     95  5F  _

1011011111  140     96  60  `
1011    141     97  61  a
1011111     142     98  62  b
101111  143     99  63  c
101101  144     100     64  d
11  145     101     65  e
111101  146     102     66  f
1011011     147     103     67  g
101011  150     104     68  h
1101    151     105     69  i
111101011   152     106     6A  j
10111111    153     107     6B  k
11011   154     108     6C  l
111011  155     109     6D  m
1111    156     110     6E  n
111     157     111     6F  o
111111  160     112     70  p
110111111   161     113     71  q
10101   162     114     72  r
10111   163     115     73  s
101     164     116     74  t
110111  165     117     75  u
1111011     166     118     76  v
1101011     167     119     77  w
11011111    170     120     78  x
1011101     171     121     79  y
111010101   172     122     7A  z
1010110111  173     123     7B  {
110111011   174     124     7C  |
1010110101  175     125     7D  }
1011010111  176     126     7E  ~
*/
    }
}
