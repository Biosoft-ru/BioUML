package ru.biosoft.bsa;

/**
 * These are the official IUPAC-IUB single-letter base codes
 * (Cornish-Bowden A., Nucl. Acids Res. 13:3021-3030(1985)).
 *
 * <pre>
 * -----------------    --------------------------------------------------------------
 * Code (compliment)      Base Description
 * -----------------    --------------------------------------------------------------
 * A (T)                Adenine
 * C (G)                Cytosine
 * G (C)                Guanine
 * T (A)                Thymine
 * R (Y)                Purine               (A or G)
 * Y (R)                Pyrimidine           (C or T or U)
 * M (K)                Amino                (A or C)
 * K (M)                Ketone               (G or T)
 * S (W)                Strong interaction   (C or G)
 * W (S)                Weak interaction     (A or T)
 * H (D)                Not-G                (A or C or T) H follows G in the alphabet
 * B (V)                Not-A                (C or G or T) B follows A
 * V (B)                Not-T (not-U)        (A or C or G) V follows U
 * D (H)                Not-C                (A or G or T) D follows C
 * N (N)                Any                  (A or C or G or T)
 * -----------------    ---------------------------------------------------------------
 * </pre>
 */
public class Nucleotide15LetterAlphabet extends NucleotideAlphabet
{
    private static volatile Nucleotide15LetterAlphabet instance;

    private Nucleotide15LetterAlphabet()
    {
        super("acgtrymkswhbvdn",
                new String[] {
                    "a", "c", "g", "t",
                    "ag", "ct", "ac", "gt", "cg", "at",
                    "act", "cgt", "acg", "agt",
                    "acgt"},
                "tgcayrkmwsdvbhn");
    }

    /** Returns the alphabet instance. */
    public static Nucleotide15LetterAlphabet getInstance()
    {
        if (instance == null)
            instance = new Nucleotide15LetterAlphabet();

        return instance;
    }
}
