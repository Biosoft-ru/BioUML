
package ru.biosoft.bsa;

/**
 * Alphabet represents set of letters or letter sequences with codes associated
 * Codes from 0 to size()-1 are actually used
 */
public abstract class Alphabet
{
    /**
     * Code for character to be ignored (for example, space or tab characters).
     */
    public static final byte IGNORED_CHAR = (byte) 254;

    /**
     * Code for letter that is not included in the alphabet
     * and is not ignored character).
     */
    public static final byte ERROR_CHAR = (byte) 255;

    ////////////////////////////////////////
    // Information about the alphabet
    //

    /** Returns code representing 'any' character. */
    abstract public byte codeForAny();

    /** Returns code representing 'any' letter. */
    abstract public byte letterForAny();

    /**
     * Specifies the alphabet size and number of codes used by alphabet (codes from 0 to size()-1 are used)
     */
    abstract public byte size();

    /** Returns basic codes of alphabet - all codes except 'any code' and other ambiguous codes. */
    public abstract byte[] basicCodes();
    
    /** Returns number of basic letters in the alphabet */
    public abstract byte basicSize();
    
    /**
     * Returns basic codes for the ambiguous code or
     * code itself if the code is not ambiguous.
     */
    public abstract byte[] basicCodes(byte code);

    /**
     * <pre>
     * letterToCodeMatrix[sequence letter]   <i>is</i> letter code
     * letterToCodeMatrix[ignored character] <i>is</i> IGNORED_CHAR
     * letterToCodeMatrix[error character]   <i>is</i> ERROR_CHAR
     * </pre>
     *
     * PENDING: usage, comments
     * @see ru.biosoft.bsa#Sequence
     */
    abstract public byte[] letterToCodeMatrix();

    /**
     * <pre>
     * letterToCodeMatrix[sequence letter] <i>is</i> letter code
     * </pre>
     *
     * PENDING: usage, comments
     * @see ru.biosoft.bsa#Sequence
     */
    abstract public byte[] codeToLetterMatrix();

    ////////////////////////////////////////
    // Functions specific for nucleotide alphabet
    // PENDING: ? should be a subclass

    /** Should return true if this nucleotide alphabet. */
    abstract public boolean isNucleotide();

    /**
     * <pre>
     * letterComplimentMatrix[letter] <i>is</i> complimentary letter
     * </pre>
     *
     * PENDING: usage, comments
     * @see ru.biosoft.bsa#Sequence
     */
    abstract public byte[] letterComplementMatrix();

    /**
     * <pre>
     * letterCodeComplimentMatrix[letter code] <i>is</i> code of complimentary letter
     * </pre>
     *
     * PENDING: usage, comments
     * @see ru.biosoft.bsa#Sequence
     */
    abstract public byte[] codeComplementMatrix();
    
    /**
     * converts code to letters
     * @return
     */
    abstract public String codeToLetters(byte code);

    /**
     * Converts letters to code
     * @param letters
     * @param offset
     * @return
     */
    abstract public byte lettersToCode(byte[] letters, int offset);
    
    /**
     * @return number of letters encoded by single code
     */
    abstract public int codeLength();
}
