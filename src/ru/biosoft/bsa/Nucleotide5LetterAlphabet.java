package ru.biosoft.bsa;

/**
 * Implementation of nucleotide 5 letter alpahabet: <pre>
 * {'a', 'c', 'g', 't', 'n'}.
 * </pre>
 */
public class Nucleotide5LetterAlphabet extends NucleotideAlphabet
{
    private static volatile Nucleotide5LetterAlphabet instance;

    protected Nucleotide5LetterAlphabet()
    {
        super("acgtn", new String[] {"a", "c", "g", "t", "acgt"}, "tgcan");
    }

    public static Nucleotide5LetterAlphabet getInstance()
    {
        if( instance == null )
            instance = new Nucleotide5LetterAlphabet();

        return instance;
    }
}
