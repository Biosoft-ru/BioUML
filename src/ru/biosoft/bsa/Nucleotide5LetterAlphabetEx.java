package ru.biosoft.bsa;

/**
 * Implementation of extended nucleotide 5 letter alpahabet:
 * <pre>
 * {'a', 'c', 'g', 't', 'n'}.
 *
 * Extensions are following:
 *   u = t
 *   rymkwsbdhv = n
 * </pre>
 */
public class Nucleotide5LetterAlphabetEx extends NucleotideAlphabet
{
    private static final String LETTERS =               "acgtnrymkwsbdhvu";
    private static final String COMPLIMENTARY_LETTERS = "tgcannnnnnnnnnna";

    private static volatile Nucleotide5LetterAlphabetEx instance;

    private Nucleotide5LetterAlphabetEx()
    {
        super(LETTERS, new String[] {
                "a", "c", "g", "t",
                "acgt",
                "ag", "ct", "ac", "gt", "at", "cg",
                "cgt", "agt", "act", "acg", "u"
                },
                COMPLIMENTARY_LETTERS);
        int strlen = LETTERS.length();

        //Fix letter to code matrix for Capitals acgtn letters.
        for (byte i = 0; i < 5; i++)
        {
                letterToCodeMatrix[Character.toUpperCase(LETTERS.charAt(i))] = letterToCodeMatrix[LETTERS.charAt(i)];
        }

        //Fix letter to code matrix for rymkwsbdhv letters.
        for (byte i = 5; i < strlen-1; i++)
        {
                letterToCodeMatrix[LETTERS.charAt(i)] = letterToCodeMatrix['n'];
                letterToCodeMatrix[Character.toUpperCase(LETTERS.charAt(i))] = letterToCodeMatrix['n'];
        }

        //Fix letter to code matrix for u
        letterToCodeMatrix['u'] = letterToCodeMatrix['t'];
        letterToCodeMatrix['U'] = letterToCodeMatrix['t'];
    }

    public static Nucleotide5LetterAlphabetEx getInstance()
    {
        if (instance == null)
            instance = new Nucleotide5LetterAlphabetEx();

        return instance;
    }
}
