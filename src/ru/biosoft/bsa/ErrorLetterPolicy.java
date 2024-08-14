
package ru.biosoft.bsa;

/**
 * This interface is used to specify what should be done
 * if error letter or character is occur during sequence processing.
 */
public interface ErrorLetterPolicy
{
    /** Indicates that error letter should be skipped. */
    public static final int SKIP = 1;

    /** Indicates that error letter should be replaced by 'any' letter. */
    public static final int REPLACE_BY_ANY = 2;

    /** Indictes that exception should be thrown. */
    public static final int EXCEPTION = 3;
}
