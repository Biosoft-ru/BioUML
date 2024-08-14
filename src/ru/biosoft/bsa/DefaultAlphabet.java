
package ru.biosoft.bsa;

import java.io.ByteArrayOutputStream;

/**
 * The default (not nucleotide) alphabet implementation.
 *
 * <p> It is supposed that codes for letters corresponds to letters order
 * in <code>allLetters</code> set and last letter is letter for any.
 * Then all other data can be derived from <code>allLetters</code>
 * by following way:
 * <ul>
 * <li> <code>letterForAny</code> is a last lettter in <code>allLetters</code>; </li>
 * <li> <code>codeToLetterMatrix</code> is <code>allLetters</code>; </li>
 * <li> <code>letterToCodeMatrix</code> see implementation details.
 * PENDING: should be described. </li>
 * </ul>
 */
public class DefaultAlphabet extends Alphabet
{
    protected byte[] allLetters;
    protected byte[] letterToCodeMatrix;
    protected byte[] basicLetters;
    protected byte[][] basicLettersForLetter;


    ////////////////////////////////////////
    // Constructors
    //
    
    public DefaultAlphabet(String letters, String[] basicLettersForLetter)
    {
        // initialize allLetters
        int length = letters.length();
        allLetters = new byte[length];
        for(int i=0; i<length; i++)
        {
            allLetters[i] = (byte)(Character.toLowerCase(letters.charAt(i)));
        }
    
        letterToCodeMatrix = new byte[128];
        for( int i = 0; i < letterToCodeMatrix.length; i++ )
            letterToCodeMatrix[i] = Character.isLetter( i ) ? ERROR_CHAR : IGNORED_CHAR;
    
        for( int i = 0; i < allLetters.length; i++ )
            letterToCodeMatrix[Character.toLowerCase( allLetters[i] )] =
            letterToCodeMatrix[Character.toUpperCase( allLetters[i] )] = (byte)i;
        
        if(letters.length() != basicLettersForLetter.length)
            throw new IllegalArgumentException("Basic letters should be specified for all letters in default alphabet");
        this.basicLettersForLetter = new byte[letters.length()][];
        ByteArrayOutputStream allBasicLetters = new ByteArrayOutputStream();
        for(int i = 0; i < letters.length(); i++)
        {
            String basic = basicLettersForLetter[i];
            if(basic.length() == 1)
                allBasicLetters.write(basic.charAt(0));
            this.basicLettersForLetter[i] = basic.getBytes();
        }
        this.basicLetters = allBasicLetters.toByteArray();
    }

    /** @return <code>allLetters<code>. */
    @Override
    public byte[] codeToLetterMatrix()
    {
        return allLetters;
    }

    /** @return last letter in <code>allLetters<code>. */
    @Override
    public byte letterForAny()
    {
        return allLetters[size()-1];
    }

    @Override
    public byte codeForAny()
    {
        return (byte) ( size()-1 );
    }

    @Override
    public byte[] letterToCodeMatrix()
    {
        return letterToCodeMatrix;
    }
    
    @Override
    public byte[] basicCodes(byte code)
    {
        byte[] letters = basicLettersForLetter[code];
        byte[] codes = new byte[letters.length];
        for(int i=0; i<letters.length; i++)
        {
            codes[i] = letterToCodeMatrix()[letters[i]];
        }
        return codes;
    }

    @Override
    public byte[] basicCodes()
    {
        byte[] letters = basicLetters;
        byte[] codes = new byte[letters.length];
        for(int i=0; i<letters.length; i++)
        {
            codes[i] = letterToCodeMatrix()[letters[i]];
        }
        return codes;
    }

    @Override
    public byte basicSize()
    {
        return (byte)basicLetters.length;
    }

    ////////////////////////////////////////
    // Functions specific for nucleotide alphabet
    //

    /** @ returns <code>false</code>. */
    @Override
    public boolean isNucleotide()
    {
        return false;
    }

    /** @throws UnsupportedOperationException */
    @Override
    public byte[] letterComplementMatrix()
    {
        throw new UnsupportedOperationException();
    }

    /** @throws UnsupportedOperationException */
    @Override
    public byte[] codeComplementMatrix()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String codeToLetters(byte code)
    {
        return new String(new byte[] {codeToLetterMatrix()[code]});
    }

    @Override
    public byte lettersToCode(byte[] letters, int offset)
    {
        return letterToCodeMatrix()[letters[offset]];
    }

    @Override
    public int codeLength()
    {
        return 1;
    }

    @Override
    public byte size()
    {
        return (byte)allLetters.length;
    }
}
