package biouml.plugins.riboseq.mappability._test;

import java.util.Random;

import biouml.plugins.riboseq.mappability.SuffixArraysLong;
import biouml.plugins.riboseq.mappability.SuffixArraysLong.ByteArray;
import biouml.plugins.riboseq.mappability.SuffixArraysLong.LongArray;
import junit.framework.TestCase;

public class TestSuffixArrays extends TestCase
{
    public void testLong1()
    {
        long n = 3;
        ByteArray t = new ByteArray( n );
        t.set( 0, 'A' );
        t.set( 1, 'T' );
        t.set( 2, 'G' );
        LongArray sa = new LongArray( n );
        SuffixArraysLong.suffixsort( t, sa, n );
        assertTrue( SuffixArraysLong.check( t, sa, n ) );
        assertEquals( 0, sa.get( 0 ) );
        assertEquals( 2, sa.get( 1 ) );
        assertEquals( 1, sa.get( 2 ) );
    }
    
    public void testLong2()
    {
        long n = 10_000_000;
        ByteArray t = new ByteArray( n );
        Random rnd = new Random();
        byte[] alphabet = new byte[] {'A','C','G','T'};
        for(int i = 0; i < n; i++)
            t.set( i, alphabet[rnd.nextInt( alphabet.length )] );
        LongArray sa = new LongArray( n );
        SuffixArraysLong.suffixsort( t, sa, n );
        assertTrue( SuffixArraysLong.check( t, sa, n ) );
    }
    
}
