package ru.biosoft.bsa;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.ClassIcon;

/**
 * Sequence is a set of characters in some alphabet.
 *
 * <p>Generally, <code>Sequence</code> is designed for readOnly usage.
 * While it has set methods, they intended for only <code>SequenceFactory</code>,
 * that is designed for sequences creation and manipulations.
 *
 * PENDING: alphabet related issues
 */
@ClassIcon("resources/sequence.gif")
public interface Sequence extends DataElement
{
    /** Returns the sequence length. */
    public int getLength();
    
    /** Returns the beginning of available part*/
    public int getStart();
    
    /** Returns sequence Interval (start, start+length-1)*/
    public Interval getInterval();

    /** Indicates whether the sequence is circular. */
    public boolean isCircular();

    /** Returns sequence letter at the specified position. */
    public byte getLetterAt(int position) throws RuntimeException;

    /**
     * Set up the specified sequence letter at the specified position.
     */
    public void setLetterAt(int position, byte letter) throws RuntimeException;

    ////////////////////////////////////////
    // Alphabet related issues
    //

    /** Returns the sequence alphabet. */
    public Alphabet getAlphabet();

    /**
     * Returns sequence letter code at the specified position.
     */
    public byte getLetterCodeAt(int position);

    /**
     * Returns sequence letter code at the specified position for custom alphabet.
     */
    public byte getLetterCodeAt(int position, Alphabet alphabet);
    
    /**
     * Set up the specified sequence character
     * (byte - internal presentation, provided by the alphabet)
     * in the specified position.
     */
    public void setLetterCodeAt(int position, byte code);
    
    // PENDING: error processing.
    // Suggested, to have CodeErrorPolicy
    
    public byte[] getBytes();
}
