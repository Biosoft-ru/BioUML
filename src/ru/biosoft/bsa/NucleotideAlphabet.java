package ru.biosoft.bsa;

public class NucleotideAlphabet extends DefaultAlphabet
{

    ////////////////////////////////////////
    // Functions specific for nucleotide alphabet
    //

    /** @return <code>true</code>. */
    @Override
    public boolean isNucleotide()
    {
        return true;
    }

    protected byte[] codeComplimentMatrix;

    protected byte[] letterComplimentMatrix;
    @Override
    public byte[] letterComplementMatrix()
    {
        return letterComplimentMatrix;
    }

    @Override
    public byte[] codeComplementMatrix()
    {
        return codeComplimentMatrix;
    }

    public NucleotideAlphabet(String letters, String[] basicLettersForLetter, String complimentLetters)
    {
        super(letters, basicLettersForLetter);

        // initialize code compliment matrix
        int length = allLetters.length;
        codeComplimentMatrix = new byte[length];
        for(int i=0; i<length; i++)
            codeComplimentMatrix[i] = letterToCodeMatrix[complimentLetters.charAt(i)];

        // initialize letter compliment matrix
        length = letterToCodeMatrix.length;
        letterComplimentMatrix = new byte[length];
        for(int ch=0; ch<length; ch++)
        {
            byte code = letterToCodeMatrix[ch];
            if(code == IGNORED_CHAR || code == ERROR_CHAR ) // PENDING: may be allLetters.length
                letterComplimentMatrix[ch] = ERROR_CHAR;
            else
            {
                code = codeComplimentMatrix[code];
                letterComplimentMatrix[ch] = allLetters[code];
            }
        }
    }
}
