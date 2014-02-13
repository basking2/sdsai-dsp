package org.sdsai;

import org.hamcrest.Matcher;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Class that provides matching of arrays similar to {@link String#contains()}.
 */
public class ContainsByteArray extends BaseMatcher<byte[]> {

    final byte[] expected;

    public ContainsByteArray(final byte[] expected) {
        this.expected = expected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeMismatch(final Object item, final Description description) {
        description.
            appendText("Could not find given array ").
            appendValue(item).
            appendText("in").
            appendValue(expected);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeTo(final Description description) {
        description.
            appendText("Subarray was not found in array").
            appendValue(expected);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final Object item) {
        final byte[] check = (byte[]) item;

        for (int start = 0; start + check.length <= expected.length; ++start) {
            boolean match = true;
            for (int i = start; i < check.length; ++i) {
                if (expected[start+i] != check[i]) {
                    match = false;
                }
            }
            if (match) {
                return true;
            }
        }

        return false;
    }

    /**
     * Construct a new {@link ContainsByteArray} {@link Matcher}.
     */
    public static Matcher<byte[]> containsByteArray(final byte[] expected) {
        return new ContainsByteArray(expected);
    }

}