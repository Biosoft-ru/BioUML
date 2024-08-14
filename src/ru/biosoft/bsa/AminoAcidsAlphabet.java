package ru.biosoft.bsa;


import java.util.HashMap;
import java.util.Map;

/**
 * AminoAcids code:
 *  Alanine         Ala A
 *  Arginine        Arg R
 *  Asparagine      Asn N
 *  Aspartic acid   Asp D
 *  Cysteine        Cys C
 *  Glutamic acid   Glu E
 *  Glutamine       Gln Q
 *  Glycine         Gly G
 *  Histidine       His H
 *  Isoleucine      Ile I
 *  Leucine         Leu L
 *  Lysine          Lys K
 *  Methionine      Met M
 *  Phenylalanine   Phe F
 *  Proline         Pro P
 *  Serine          Ser S
 *  Threonine       Thr T
 *  Tryptophan      Trp W
 *  Tyrosine        Tyr Y
 *  Valine          Val V
 *  Selenocysteine  Sec U
 *  Pyrrolysine     Pyl O
 *  Asparagine or aspartic acid         Asx B
 *  Glutamine or glutamic acid          Glx Z
 *  Leucine or Isoleucine               Xle J
 *  Unspecified or unknown amino acid   Xaa X
 * @author lan
 */
public class AminoAcidsAlphabet extends DefaultAlphabet
{
    private volatile static AminoAcidsAlphabet instance;

    final String[][] aminoAcidToTriplet = new String[128][];
    final Map<String, Byte> tripletToAminoAcid = new HashMap<>();

    private AminoAcidsAlphabet()
    {
        super("abcdefghijklmnopqrstuvwxyz", new String[] {"a", "nd", "c", "d", "e", "f", "g", "h", "i", "li", "k", "l", "m", "n", "o", "p",
                "q", "r", "s", "t", "u", "v", "w", "acdefghiklmnpqrstvwy", "y", "eq"});

        aminoAcidToTriplet['a'] = new String[] {"gca", "gcc", "gcg", "gct"};
        aminoAcidToTriplet['r'] = new String[] {"aga", "agg", "cga", "cgc", "cgg", "cgt"};
        aminoAcidToTriplet['n'] = new String[] {"aac", "aat"};
        aminoAcidToTriplet['d'] = new String[] {"gat", "gac"};
        aminoAcidToTriplet['c'] = new String[] {"tgt", "tgc"};
        aminoAcidToTriplet['e'] = new String[] {"gaa", "gag"};
        aminoAcidToTriplet['q'] = new String[] {"caa", "cag"};
        aminoAcidToTriplet['g'] = new String[] {"gga", "ggc", "ggg", "ggt"};
        aminoAcidToTriplet['h'] = new String[] {"cac", "cat"};
        aminoAcidToTriplet['i'] = new String[] {"ata", "atc", "att"};
        aminoAcidToTriplet['l'] = new String[] {"cta", "ctc", "ctg", "ctt", "tta", "ttg"};
        aminoAcidToTriplet['k'] = new String[] {"aaa", "aag"};
        aminoAcidToTriplet['m'] = new String[] {"atg"};
        aminoAcidToTriplet['f'] = new String[] {"ttc", "ttt"};
        aminoAcidToTriplet['p'] = new String[] {"cca", "ccc", "ccg", "cct"};
        aminoAcidToTriplet['s'] = new String[] {"agc", "agt", "tca", "tcc", "tcg", "tct"};
        aminoAcidToTriplet['t'] = new String[] {"aca", "acc", "acg", "act"};
        aminoAcidToTriplet['w'] = new String[] {"tgg"};
        aminoAcidToTriplet['y'] = new String[] {"tac", "tat"};
        aminoAcidToTriplet['v'] = new String[] {"gta", "gtc", "gtg", "gtt"};
        //Stop codons
        aminoAcidToTriplet['*'] = new String[] {"taa", "tag", "tga"};

        for( int i = 0; i < 128; i++ )
            if( aminoAcidToTriplet[i] != null )
                for( String triplet : aminoAcidToTriplet[i] ) {
                    byte aminoAcid = i == '*' ? -1 : (byte)i;
                    tripletToAminoAcid.put(triplet, aminoAcid);
                    tripletToAminoAcid.put(triplet.toUpperCase(), aminoAcid);
                }

        for( char i = 0; i < 128; i++ )
            if( aminoAcidToTriplet[i] != null )
                aminoAcidToTriplet[Character.toUpperCase(i)] = aminoAcidToTriplet[i];
    }

    /** Returns the alphabet instance. */
    public static AminoAcidsAlphabet getInstance()
    {
        if( instance == null )
            instance = new AminoAcidsAlphabet();

        return instance;
    }

    public String[] getTripletsForAminoAcid(byte aminoAcidLetter)
    {
        return aminoAcidToTriplet[aminoAcidLetter];
    }

    /** Returns lowercase iupac letter of the amino acid encoded by given triplet
     *  If triplet is stop codon returns -1
     */
    public byte getAminoAcidForTriplet(String triplet)
    {
        Byte result = tripletToAminoAcid.get(triplet);
        if(result == null)
            throw new IllegalArgumentException("Invalid triplet " + triplet);
        return result;
    }
}
