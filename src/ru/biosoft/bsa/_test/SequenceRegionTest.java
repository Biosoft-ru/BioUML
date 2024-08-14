package ru.biosoft.bsa._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import ru.biosoft.bsa.CircularSequence;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.SequenceRegion;


public class SequenceRegionTest extends TestCase
{
    /** Standart JUnit constructor */
    public SequenceRegionTest( String name )
    {
        super( name );
    }

    /** linear and circular sequences for testing */
    private LinearSequence linearSequence;
    private CircularSequence circularSequence;

    /** Temporary matrix */
    byte[] reverseMatrix;

    @Override
    public void setUp()
    {
        String string = "agtcagtcagtcagtc"; //16
        linearSequence = new LinearSequence( string, Nucleotide5LetterAlphabet.getInstance() );
        circularSequence = new CircularSequence( string, Nucleotide5LetterAlphabet.getInstance(), true );
        reverseMatrix = Nucleotide5LetterAlphabet.getInstance().codeComplementMatrix();
    }



    //////////////////////////////////////
    // Tests for reading
    //

    /** Compare sequences - are there equal or not? */
    public void testSequencesCompare()
    {
        assertTrue( " Sequences are different!",
            linearSequence.getLetterAt( 1 ) == circularSequence.getLetterAt( 1 ) &&
            linearSequence.getLetterAt( 1 ) == circularSequence.getLetterAt( 17 ) );
    }

    /** Test of simple linear sequence region for reading */
    public void testRegion()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( linearSequence, 8, 5, false, false );
        assertTrue( "Invalid region", sequenceRegion.getLetterAt( 1 ) ==
            linearSequence.getLetterAt( 8 ) &&
            sequenceRegion.getLetterAt( 2 ) == linearSequence.getLetterAt( 9 ) &&
            sequenceRegion.getLetterAt( 3 ) == linearSequence.getLetterAt( 10 ) &&
            sequenceRegion.getLetterAt( 4 ) == linearSequence.getLetterAt( 11 ) &&
            sequenceRegion.getLetterAt( 5 ) == linearSequence.getLetterAt( 12 ) );
    }

    /** Test of reverse linear sequence region for reading */
    public void testReverseRegion()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( linearSequence, 9, 5, true, false );
        //assert("Wrong sequence char return code :" + linearSequence.getLetterCodeAt(9),
        // linearSequence.getLetterCodeAt(9) == 0 );
        //assertTrue("Compliment code matrix is wrong :" +
        // reverseMatrix[linearSequence.getLetterCodeAt(9)], reverseMatrix[linearSequence.getLetterCodeAt(9)] == 3 );
        assertTrue( "Invalid reverse region",
            sequenceRegion.getLetterCodeAt( 1 ) ==
            reverseMatrix[ linearSequence.getLetterCodeAt( 9 ) ] &&
            sequenceRegion.getLetterCodeAt( 2 ) ==
            reverseMatrix[ linearSequence.getLetterCodeAt( 8 ) ] &&
            sequenceRegion.getLetterCodeAt( 3 ) ==
            reverseMatrix[ linearSequence.getLetterCodeAt( 7 ) ] &&
            sequenceRegion.getLetterCodeAt( 4 ) ==
            reverseMatrix[ linearSequence.getLetterCodeAt( 6 ) ] &&
            sequenceRegion.getLetterCodeAt( 5 ) ==
            reverseMatrix[ linearSequence.getLetterCodeAt( 5 ) ] );
        
        sequenceRegion = SequenceRegion.getReversedSequence(linearSequence);
        assertEquals("Invalid reverse region", sequenceRegion.toString(), "gactgactgactgact");
    }

    /** Test of region on circular sequence for reading */
    public void testCircularRegion()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( circularSequence, 10, 4, false, true );
        assertTrue( "Invalid region", sequenceRegion.getLetterAt( 1 ) ==
            circularSequence.getLetterAt( 10 ) &&
            sequenceRegion.getLetterAt( 4 ) == circularSequence.getLetterAt( 13 ) );
        sequenceRegion = new SequenceRegion( circularSequence, 14, 5, false, true );
        assertTrue( "Invalid region on the joint",
            sequenceRegion.getLetterAt( 1 ) == circularSequence.getLetterAt( 14 ) &&
            sequenceRegion.getLetterAt( 2 ) == circularSequence.getLetterAt( 15 ) &&
            sequenceRegion.getLetterAt( 3 ) == circularSequence.getLetterAt( 16 ) &&
            sequenceRegion.getLetterAt( 4 ) == circularSequence.getLetterAt( 1 ) &&
            sequenceRegion.getLetterAt( 5 ) == circularSequence.getLetterAt( 2 ) );
    }

    /** Test of compliment region on circular sequence for reading */
    public void testComplimentCircularRegion()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( circularSequence, 8, 5, true, true );
        assertTrue( "Invalid reverse region",
            sequenceRegion.getLetterCodeAt( 1 ) ==
            reverseMatrix[ linearSequence.getLetterCodeAt( 8 ) ] &&
            sequenceRegion.getLetterCodeAt( 5 ) ==
            reverseMatrix[ linearSequence.getLetterCodeAt( 4 ) ] );
            sequenceRegion = new SequenceRegion( circularSequence, 13, 5, false, true );
        assertTrue( "Invalid region on the joint",
            sequenceRegion.getLetterAt( 1 ) == circularSequence.getLetterAt( 13 ) &&
            sequenceRegion.getLetterAt( 2 ) == circularSequence.getLetterAt( 14 ) &&
            sequenceRegion.getLetterAt( 3 ) == circularSequence.getLetterAt( 15 ) &&
            sequenceRegion.getLetterAt( 4 ) == circularSequence.getLetterAt( 16 ) &&
            sequenceRegion.getLetterAt( 5 ) == circularSequence.getLetterAt( 1 ) );
    }


    ///////////////////////////////////
    // Tests for writing
    //

    /** simple writing */
    public void testSimpleWriting()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( linearSequence, 8, 5, false, false );
        sequenceRegion.setLetterCodeAt( 1, ( byte )4 );
        sequenceRegion.setLetterCodeAt( 2, ( byte )0 );
        assertTrue( "Writing is not correct",
            sequenceRegion.getLetterAt( 1 ) == 'n' && sequenceRegion.getLetterAt( 2 ) == 'a' );
        assertTrue( "Writing to sequence is not correct",
            linearSequence.getLetterAt( 8 ) == 'n' && linearSequence.getLetterAt( 9 ) == 'a' );
    }

    /** circular sequence writing */
    public void testCycleWriting()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( circularSequence, 8, 18, false, true );
        sequenceRegion.setLetterCodeAt( 16, ( byte )0 );
        assertEquals( 'a', circularSequence.getLetterAt( 7 ) );
        sequenceRegion.setLetterCodeAt( 16, ( byte )1 );
        assertEquals( 'c', circularSequence.getLetterAt( 7 ) );
        sequenceRegion.setLetterCodeAt( 20, ( byte )1 );
        assertEquals( 'c', circularSequence.getLetterAt( 11 ) );
        sequenceRegion.setLetterCodeAt( 20, ( byte )0 );
        assertEquals( 'a', circularSequence.getLetterAt( 11 ) );
    }

    /** Reverse writing test */
    public void testReverseWriting()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( circularSequence, 8, 5, true, true );

        sequenceRegion.setLetterCodeAt( 1, reverseMatrix[ 0 ] );
        sequenceRegion.setLetterCodeAt( 5, reverseMatrix[ 3 ] );
        assertEquals( 0, circularSequence.getLetterCodeAt( 8 ) );
        assertEquals( 3, circularSequence.getLetterCodeAt( 4 ) );
        assertEquals(3, circularSequence.getLetterCodeAt(4, Nucleotide5LetterAlphabet.getInstance()));
        sequenceRegion = new SequenceRegion( circularSequence, 13, 5, false, true );
        sequenceRegion.setLetterCodeAt( 1, ( byte )0 );
        sequenceRegion.setLetterCodeAt( 3, ( byte )1 );
        sequenceRegion.setLetterCodeAt( 4, ( byte )2 );
        sequenceRegion.setLetterCodeAt( 5, ( byte )3 );
        assertTrue( "Invalid region on the joint writing",
            sequenceRegion.getLetterAt( 1 ) == 'a' && sequenceRegion.getLetterAt( 3 ) == 'c' &&
            sequenceRegion.getLetterAt( 4 ) == 'g' && sequenceRegion.getLetterAt( 5 ) == 't' );
    }

    public void testDoubleRegion()
    {
        SequenceRegion sequenceRegion = new SequenceRegion( linearSequence, linearSequence.getLength(), 1, true, false );
        SequenceRegion doubled = new SequenceRegion( sequenceRegion, 1, 5, false, false );
        System.out.println( "test letter = " + doubled.getLetterAt( 2 ) );
    }

    public static Test suite()
    {
        return new TestSuite( SequenceRegionTest.class );
    }

    /**  */
    @Override
    protected void tearDown()
    {
    }

    public static void main( String[] args )
    {
        TestRunner.run( suite() );
    }
}
