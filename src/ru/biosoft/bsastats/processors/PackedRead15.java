package ru.biosoft.bsastats.processors;

import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;

/**
 * This class represents single read (up to 15bp) in packed manner (for the purpose of OverrepresentedPrefixes)
 * @author lan
 */
public class PackedRead15
{
    public static final PackedRead15 NULL_READ = new PackedRead15();
    private static final Alphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
    private int data;
    private byte length;
    
    public PackedRead15(PackedRead15 parent, byte next)
    {
        data = parent.data*4+next;
        length = (byte) ( parent.length+1 );
    }
    
    public PackedRead15(PackedRead15 child)
    {
        data = child.data/4;
        length = (byte) ( child.length-1 );
    }
    
    private PackedRead15()
    {
        data = 0;
        length = 0;
    }

    @Override
    public int hashCode()
    {
        return data;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        PackedRead15 other = (PackedRead15)obj;
        return length == other.length && data == other.data;
    }
    
    @Override
    public String toString()
    {
        int packed = data;
        byte[] result = new byte[length];
        for(int i=length-1; i>=0; i--)
        {
            byte code = (byte) ( packed%4 );
            packed/=4;
            result[i] = alphabet.codeToLetterMatrix()[code];
        }
        return new String(result).toUpperCase();
    }
}
