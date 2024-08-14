package ru.biosoft.bsa;

/**
 * @author lan
 */
public interface AlignmentSite extends Site
{
    /**
     * @return {@link Sequence} of the original read (while getSequence will return the sequence this site aligned to)
     */
    public Sequence getReadSequence();

    /**
     * @return array containing the quality values if applicable.
     * Array length must be the same as length of the {@link Sequence} returned by getReadSequence
     */
    public byte[] getBaseQualities();
}
