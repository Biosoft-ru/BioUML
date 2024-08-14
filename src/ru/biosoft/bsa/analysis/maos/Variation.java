package ru.biosoft.bsa.analysis.maos;

import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;

public class Variation extends Interval
{
    public String id;
    public final String name;
    public final byte[] alt, ref;

    public Variation(String id, String name, int from, int to, byte[] ref, byte[] alt)
    {
        super( from, to );
        this.id = id;
        this.name = name;
        checkSequence(ref);
        checkSequence( alt );
        this.ref = ref;
        this.alt = alt;
    }


    private void checkSequence(byte[] seq)
    {
        for(byte c : seq)
            if(c != 'A' && c != 'C' && c != 'G' && c != 'T')
                throw new IllegalArgumentException("Allele string contains wrong characters: \"" + (char)c + "\"");
    }
    
    @Override
    public String toString()
    {
        return name + ":" + new String( ref ) + ">" + new String(alt);
    }
    
    public static Variation createFromSite(Site site)
    {
        int from = site.getFrom();
        int to = site.getTo();

        String refString = site.getProperties().getValueAsString( "RefAllele" );
        if(refString == null)
            throw new IllegalArgumentException("No 'RefAllele' property in site, probably your input track is not in VCF format");
        refString = refString.toUpperCase();
        
        /*Too slow due to random access of genome
        String expectedRef = site.getSequence().toString().toUpperCase();
        if(!refString.equals( expectedRef ))
            throw new IllegalArgumentException("Wrong RefAllele " + site.getSequence().getName() + ":" + from + "-" + to + " " + refString + " but should be " + expectedRef);
        */

        String altString = site.getProperties().getValueAsString( "AltAllele" );
        if(altString == null)
            throw new IllegalArgumentException("No 'AltAllele' property in site, probably your input track is not in VCF format");
        altString = altString.toUpperCase();
        if( altString.contains( "," ) )
            throw new IllegalArgumentException( "Multiple alternative alleles are not allowed" );
        for(int i = 0; i < altString.length(); i++)
        {
            char c = altString.charAt( i );
            switch(c) {
                case 'A': case 'C': case 'G': case 'T':
                case 'a': case 'c': case 'g': case 't':
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized ALT sequence: " + altString);
            }
        }

        if( ( refString.length() == 1 && altString.length() > 1 ) || ( altString.length() == 1 && refString.length() > 1 ) )
            if( refString.charAt( 0 ) == altString.charAt( 0 ) )
            {
                refString = refString.substring( 1 );
                altString = altString.substring( 1 );
                from++;
            }
        
        String name = site.getProperties().getValueAsString( "name" );
        if(name == null)
            name = "variation_id=" + site.getName();
        Variation result = new Variation(site.getName(), name, from, to, refString.getBytes(), altString.getBytes());
        return result;
    }
    
    public static Sequence applyVariations(Sequence reference, Variation[] variations)
    {
        int lengthDiff = 0;
        for( Variation v : variations )
            lengthDiff += v.alt.length - v.ref.length;

        byte[] altBytes = new byte[reference.getLength() + lengthDiff];

        int refPos = reference.getStart();
        int altPos = 0;
        for( Variation v : variations )
        {
            while( refPos < v.getFrom() )
            {
                altBytes[altPos++] = reference.getLetterAt( refPos++ );
            }
            for( byte element : v.alt )
                altBytes[altPos++] = element;
            
            for( byte ref : v.ref)
            {
                char fromVariation = (char)Character.toLowerCase( ref );
                char fromGenome = (char)Character.toLowerCase( reference.getLetterAt( refPos++ ) );
                if(fromVariation != fromGenome)
                    throw new IllegalArgumentException("Variation (" + v.name + ") is not matched to reference sequence: " + fromVariation + " != " + fromGenome + "."
                            + " Probably, incompatible genome version.");
            }
        }
        while( altPos < altBytes.length )
            altBytes[altPos++] = reference.getLetterAt( refPos++ );

        Sequence alternative = new LinearSequence( reference.getName() + "alternative", altBytes, reference.getAlphabet() );
        return alternative;
    }
    
    //Invert variations from ref->alt to alt->ref
    public static Variation[] invertVariations(Variation[] variations)
    {
        Variation[] result = new Variation[variations.length];
        int offset = 0;
        for(int i = 0; i < variations.length; i++)
        {
            Variation var = variations[i];
            int altFrom = var.getFrom() + offset;
            Variation newVar = new Variation( var.id, var.name, altFrom, altFrom + var.alt.length - 1, var.alt, var.ref );
            result[i] = newVar;
            offset += var.alt.length - var.ref.length;
        }
        return result;
    }
    
    //map variations to the reverse complement strand
    public static Variation[] mapToRC(Interval bounds, Variation[] vars, int seqStart, Alphabet alphabet)
    {
        Variation[] result = new Variation[vars.length];
        for(int i = vars.length - 1; i >= 0; i--)
        {
            Variation var = vars[i];
            int newFrom = bounds.getTo() - var.getTo() + seqStart;
            Variation newVar = new Variation( var.id, var.name, newFrom, newFrom + var.ref.length - 1, rc(var.ref, alphabet), rc(var.alt, alphabet) );
            result[result.length - i - 1] = newVar;
        }
        return result;
    }
    
    private static byte[] rc(byte[] seq, Alphabet alphabet)
    {
        byte[] res = new byte[seq.length];
        for(int i = 0; i < seq.length; i++)
        {
            byte c = alphabet.letterComplementMatrix()[seq[i]];
            res[res.length - i - 1] = (byte)Character.toUpperCase( c );
        }
        return res;
    }

}