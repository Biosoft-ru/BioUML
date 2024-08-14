package ru.biosoft.bsa;

/**
 * Describes the part of sequence
 */
public class Slice
{
    /**
     * Start position (inclusive)
     */
    public int from;
    /**
     * End position (exclusive)
     */
    public int to;
    /**
     * Sequence for the slice (for Sequences)
     */
    public byte[] data;
}
