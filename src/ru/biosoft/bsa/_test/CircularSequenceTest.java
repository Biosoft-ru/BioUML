

package ru.biosoft.bsa._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.bsa.CircularSequence;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;

public class CircularSequenceTest extends TestCase
{
    /** Standart JUnit constructor */
    public CircularSequenceTest(String name)
    {
        super(name);
    }

    public void testCircularSequenceReadPlus()
    {
        assertEquals( sequenceString.getBytes()[0], circularSequence.getLetterAt( 1 ) );
        assertEquals( sequenceString.getBytes()[0], circularSequence.getLetterAt( 17 ) );
    }
    public void testCircularSequenceReadMinus()
    {
        assertEquals( sequenceString.getBytes()[15], circularSequence.getLetterAt( -1 ) );
        assertEquals( sequenceString.getBytes()[14], circularSequence.getLetterAt( -2 ) );
        assertEquals( sequenceString.getBytes()[13], circularSequence.getLetterAt( -3 ) );
    }
    public void testCircularSequenceWritePlus()
    {
        circularSequence.setLetterCodeAt( 1, (byte)1 );
        assertEquals( 'c', circularSequence.getLetterAt( 1 ) );
        circularSequence.setLetterCodeAt( 18, (byte)0 );
        assertEquals( 'a', circularSequence.getLetterAt( 2 ) );
    }
    public void testCircularSequenceWriteMinus()
    {
        circularSequence.setLetterCodeAt( -1, (byte)1 );
        assertEquals( 'c', circularSequence.getLetterAt( 16 ) );
        circularSequence.setLetterCodeAt( -1, (byte)0 );
        assertEquals( 'a', circularSequence.getLetterAt( 16 ) );
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite()
    {
        return new TestSuite(CircularSequenceTest.class);
    }

    @Override
    public void setUp()
    {
        sequenceString = "agtcagtcagtcagtc"; //16
        linearSequence = new LinearSequence(sequenceString, Nucleotide5LetterAlphabet.getInstance());
        circularSequence = new CircularSequence(sequenceString, Nucleotide5LetterAlphabet.getInstance(), true );
        reverseMatrix = Nucleotide5LetterAlphabet.getInstance().codeComplementMatrix();
    }

    String sequenceString;
    byte[] reverseMatrix;
    private CircularSequence circularSequence;
    private LinearSequence linearSequence;
}
