package ru.biosoft.bsa;

public class ColorAlphabet extends DefaultAlphabet
{
    private static final ColorAlphabet instance = new ColorAlphabet();
    private static final byte[][] NUCLEOTIDE_PAIR_TO_COLOR = new byte[][] { {'0', '1', '2', '3', '.'}, {'1', '0', '3', '2', '.'},
        {'2', '3', '0', '1', '.'}, {'3', '2', '1', '0', '.'}, {'.', '.', '.', '.', '.'}};


    private ColorAlphabet()
    {
        super( "0123.", new String[] {"0", "1", "2", "3", "0123"} );
    }
    
    public static ColorAlphabet getInstance()
    {
        return instance;
    }
    
    public static byte ntPairToColor(byte letterCode1, byte letterCode2)
    {
        return NUCLEOTIDE_PAIR_TO_COLOR[letterCode1][letterCode2];
    }
    
    public static Sequence translateToColorSpace(Sequence dna)
    {
        byte[] bytes = new byte[dna.getLength() - 1];
        for(int i = 0; i < dna.getLength() - 1; i++)
            bytes[i] = ntPairToColor(dna.getLetterCodeAt( dna.getStart() + i ), dna.getLetterCodeAt( dna.getStart() + i + 1 ));
        return new LinearSequence(dna.getName(), bytes, getInstance() );
    }
}
