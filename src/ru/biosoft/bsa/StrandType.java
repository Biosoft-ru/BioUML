package ru.biosoft.bsa;

/**
 * Indicates whether <code>Site</code> refers to original plus-strand,
 * complementary minus-strand, or both strands of a double-stranded molecule.
 */
public interface StrandType
{
    public static final int STRAND_NOT_KNOWN        = 0;
    public static final int STRAND_NOT_APPLICABLE   = 1;
    public static final int STRAND_PLUS             = 2;
    public static final int STRAND_MINUS            = 3;
    public static final int STRAND_BOTH             = 4;
}
