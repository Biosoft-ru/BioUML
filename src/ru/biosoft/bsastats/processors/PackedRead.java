package ru.biosoft.bsastats.processors;

import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;

/**
 * This class represents single read (up to 75bp) in packed manner (for the purpose of DuplicateSequencesProcessor)
 * @author lan
 */
public class PackedRead
{
    private static final Alphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
    private long data1, data2, data3;
    private int hashCode;
    
    public PackedRead(byte[] seq, int length)
    {
        data1 = pack(seq, 0, 25, length);
        data2 = pack(seq, 26, 51, length);
        data3 = (length>52?pack(seq, 52, 74, length):0)*100+length;
        final int prime = 31;
        hashCode = 1;
        hashCode = prime * hashCode + (int) ( data1 ^ ( data1 >>> 32 ) );
        hashCode = prime * hashCode + (int) ( data2 ^ ( data2 >>> 32 ) );
        hashCode = prime * hashCode + (int) ( data3 ^ ( data3 >>> 32 ) );
    }
    
    protected long pack(byte[] seq, int start, int end, int maxLength)
    {
        long result = 0;
        for(int i=start; i<=end; i++)
        {
            byte code = i>=maxLength?0:alphabet.letterToCodeMatrix()[seq[i]];
            result = result*5+code;
        }
        return result;
    }
    
    protected void unpack(byte[] target, int start, int end, long packed)
    {
        for(int i=end; i>=start; i--)
        {
            byte code = (byte) ( packed%5 );
            packed/=5;
            if(target.length > i)
                target[i] = alphabet.codeToLetterMatrix()[code];
        }
    }

    @Override
    public int hashCode()
    {
        return hashCode;
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
        PackedRead other = (PackedRead)obj;
        if( data1 != other.data1 )
            return false;
        if( data2 != other.data2 )
            return false;
        if( data3 != other.data3 )
            return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        int length = (int) ( data3%100 );
        byte[] result = new byte[length];
        unpack(result, 0, 25, data1);
        unpack(result, 26, 51, data2);
        unpack(result, 52, 74, data3/100);
        return new String(result).toUpperCase();
    }
}
