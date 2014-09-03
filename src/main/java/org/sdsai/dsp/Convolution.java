package org.sdsai.dsp;

public final class Convolution {

    /**
     * @param x Input signal.
     * @param x_off x offset to read from.
     * @param x_len Length to read from x.
     * @param h Filter kernel, or, the other signal.
     * @param h_off The offset in h to start reading from.
     * @param h_len The length to read from h.
     * @param y This must have enough space after {@code y_off} to hold {@code x_len + h_len - 1}.
     *        The range of y to be written to must also be set to zero before this is called.
     * @param y_off The offset into y to start writing too. {@code x_len + h_len - 1} elements will be written.
     */
    public static final void convolve(
        final short[] x,
        final int     x_off,
        final int     x_len,
        final short[] h,
        final int     h_off,
        final int     h_len,
        final short[] y,
        final int     y_off
    )
    {
        for (int x_i = x_off; x_i < x_off+x_len; ++x_i) {
            for (int h_i = h_off; h_i < h_off+h_len; ++h_i) {
                y[x_i + h_i] += x[x_i] * h[h_i];
            }
        }
    }

    /**
     * Call {@link #convolve(short[], int, int, short[], int, int, short[], int)}.
     *
     * @param x Input signal.
     * @param x_off x offset to read from.
     * @param x_len Length to read from x.
     * @param h Filter kernel, or, the other signal.
     * @param y This must have enough space after {@code y_off} to hold {@code x_len + h_len - 1}.
     *        The range of y to be written to must also be set to zero before this is called.
     * @param y_off The offset into y to start writing too. {@code x_len + h_len - 1} elements will be written.
     */
    public static final void convolve(
        final short[] x,
        final int     x_off,
        final int     x_len,
        final short[] h,
        final short[] y,
        final int     y_off
    )
    {
        convolve(x, x_off, x_len, h, 0, h.length, y, y_off);
    }

    /**
     * Call {@link #convolve(short[], int, int, short[], int, int, short[], int)}.
     *
     * @param x Input signal.
     * @param h Filter kernel, or, the other signal.
     * @param h_off The offset in h to start reading from.
     * @param h_len The length to read from h.
     * @param y This must have enough space after {@code y_off} to hold {@code x_len + h_len - 1}.
     *        The range of y to be written to must also be set to zero before this is called.
     * @param y_off The offset into y to start writing too. {@code x_len + h_len - 1} elements will be written.
     */
    public static final void convolve(
        final short[] x,
        final short[] h,
        final int     h_off,
        final int     h_len,
        final short[] y,
        final int     y_off
    )
    {
        convolve(x, 0, x.length, h, h_off, h_len, y, y_off);
    }

    /**
     * Call {@link #convolve(short[], int, int, short[], int, int, short[], int)}.
     *
     * @param x Input signal.
     * @param h Filter kernel, or, the other signal.
     * @param y This must have enough space after {@code y_off} to hold {@code x_len + h_len - 1}.
     *        The range of y to be written to must also be set to zero before this is called.
     * @param y_off The offset into y to start writing too. {@code x_len + h_len - 1} elements will be written.
     */
    public static final void convolve(
        final short[] x,
        final short[] h,
        final short[] y,
        final int     y_off
    )
    {
        convolve(x, 0, x.length, h, 0, h.length, y, y_off);
    }

    /**
     * Call {@link #convolve(short[], int, int, short[], int, int, short[], int)}.
     *
     * @param x Input signal.
     * @param x_off x offset to read from.
     * @param x_len Length to read from x.
     * @param h Filter kernel, or, the other signal.
     * @param y This must have enough space after {@code y_off} to hold {@code x_len + h_len - 1}.
     *        The range of y to be written to must also be set to zero before this is called.
     */
    public static final void convolve(
        final short[] x,
        final int     x_off,
        final int     x_len,
        final short[] h,
        final short[] y
    )
    {
        convolve(x, x_off, x_len, h, 0, h.length, y, 0);
    }

    /**
     * Call {@link #convolve(short[], int, int, short[], int, int, short[], int)}.
     *
     * @param x Input signal.
     * @param h Filter kernel, or, the other signal.
     * @param h_off The offset in h to start reading from.
     * @param h_len The length to read from h.
     * @param y This must have enough space after {@code y_off} to hold {@code x_len + h_len - 1}.
     *        The range of y to be written to must also be set to zero before this is called.
     */
    public static final void convolve(
        final short[] x,
        final short[] h,
        final int     h_off,
        final int     h_len,
        final short[] y
    )
    {
        convolve(x, 0, x.length, h, h_off, h_len, y, 0);
    }

    /**
     * Call {@link #convolve(short[], int, int, short[], int, int, short[], int)}.
     *
     * @param x Input signal.
     * @param h Filter kernel, or, the other signal.
     * @param y This must have enough space after {@code y_off} to hold {@code x_len + h_len - 1}.
     *        The range of y to be written to must also be set to zero before this is called.
     */
    public static final void convolve(
        final short[] x,
        final short[] h,
        final short[] y
    )
    {
        convolve(x, 0, x.length, h, 0, h.length, y, 0);
    }
}