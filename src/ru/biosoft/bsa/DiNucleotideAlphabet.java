package ru.biosoft.bsa;

/**
 * Alphabet where each code represents two nucleotides.
 * @author lan
 */
public class DiNucleotideAlphabet extends Nucleotide5LetterAlphabet
{
    private static final int BASIC_CODES = 16;
    
    private volatile static DiNucleotideAlphabet instance;

    protected DiNucleotideAlphabet()
    {
        super();
    }

    @Override
    public byte[] codeComplementMatrix()
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public byte[] basicCodes(byte code)
    {
        return code == codeForAny() ? basicCodes() : new byte[] {code};
    }

    @Override
    public byte[] basicCodes()
    {
        byte[] codes = new byte[16];
        for(byte i=0; i<size()-1; i++)
        {
            codes[i] = i;
        }
        return codes;
    }

    @Override
    public String codeToLetters(byte code)
    {
        return code == codeForAny() ? new String(new byte[] {letterForAny(), letterForAny()}) :
            new String(new byte[] {codeToLetterMatrix()[code/4], codeToLetterMatrix()[code%4]});
    }

    @Override
    public byte lettersToCode(byte[] letters, int offset)
    {
        byte[] matrix = letterToCodeMatrix();
        byte letter1 = matrix[letters[offset]];
        byte letter2 = matrix[letters[offset+1]];
        if(letter1 == ERROR_CHAR || letter2 == ERROR_CHAR) return ERROR_CHAR;
        if(letter1 == IGNORED_CHAR || letter2 == IGNORED_CHAR) return IGNORED_CHAR;
        // 4 -> 'N' in original matrix
        if(letter1 == 4 || letter2 == 4) return codeForAny();
        return (byte) ( letter1*4+letter2 );
    }

    @Override
    public byte size()
    {
        return BASIC_CODES+1;
    }
    
    @Override
    public byte basicSize()
    {
        return BASIC_CODES;
    }

    public static DiNucleotideAlphabet getInstance()
    {
        if( instance == null )
            instance = new DiNucleotideAlphabet();

        return instance;
    }

    @Override
    public int codeLength()
    {
        return 2;
    }

    @Override
    public byte letterForAny()
    {
        return 'n';
    }
}
