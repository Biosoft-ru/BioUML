package biouml.plugins.riboseq._test;

import biouml.plugins.riboseq.mappability.SequenceMappability;
import junit.framework.TestCase;

public class TestSequenceMappability extends TestCase
{
    public void test1()
    {
        int[][] res = SequenceMappability.computeMappableLength( "AAAAA\n".getBytes(), 5, 5, 1 );
        assertEquals( res.length, 1 );
        assertEquals( res[0].length, 1 );
        assertEquals(1, res[0][0]);
    }
    
    public void test2()
    {
        int[][] res = SequenceMappability.computeMappableLength( "AAAAA\n".getBytes(), 1, 5, 1 );
        assertEquals( res.length, 5 );
        assertEquals( res[0].length, 1 );
        assertEquals(0, res[0][0]);
        assertEquals( res[1].length, 1 );
        assertEquals(0, res[1][0]);
        assertEquals( res[2].length, 1 );
        assertEquals(0, res[2][0]);
        assertEquals( res[3].length, 1 );
        assertEquals(0, res[3][0]);
        assertEquals( res[4].length, 1 );
        assertEquals(1, res[4][0]);
    }
    
    public void test3()
    {
        int[][] res = SequenceMappability.computeMappableLength( "AAAAA\n".getBytes(), 1, 5, 2 );
        assertEquals( res.length, 5 );
        assertEquals( res[0].length, 1 );
        assertEquals(0, res[0][0]);
        assertEquals( res[1].length, 1 );
        assertEquals(0, res[1][0]);
        assertEquals( res[2].length, 1 );
        assertEquals(0, res[2][0]);
        assertEquals( res[3].length, 1 );
        assertEquals(2, res[3][0]);
        assertEquals( res[4].length, 1 );
        assertEquals(1, res[4][0]);
    }
    
    public void test4()
    {
        int[][] res = SequenceMappability.computeMappableLength( "ACGTA\nCGTA\n".getBytes(), 4, 5, 1 );
        assertEquals( res.length, 2 );
        
        assertEquals( res[0].length, 2 );
        assertEquals(1, res[0][0]);
        assertEquals(0, res[0][1]);

        assertEquals( res[1].length, 2 );
        assertEquals(1, res[1][0]);
        assertEquals(0, res[1][1]);
    }
    
    public void test5()
    {
        int[][] res = SequenceMappability.computeMappableLength( "ACGTA\nCGTA\nCGT\nGG\n".getBytes(), 3, 3, 2 );
        assertEquals( res.length, 1 );
        
        assertEquals( res[0].length, 4 );
        assertEquals(2, res[0][0]);
        assertEquals(1, res[0][1]);
        assertEquals(0, res[0][2]);
        assertEquals(0, res[0][3]);
    }
}
