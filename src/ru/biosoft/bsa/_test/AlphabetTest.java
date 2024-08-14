
package ru.biosoft.bsa._test;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.DiNucleotideAlphabet;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabetEx;

public class AlphabetTest extends TestCase
{
    public void testAlphabetBasics()
    {
        Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();
        assertEquals(1, alphabet.codeLength());
        assertEquals(15, alphabet.size());
        assertEquals(4, alphabet.basicSize());
        assertEquals('n', alphabet.letterForAny());
    }
    
    public void testAmbiguousLetters()
    {
        Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();
        byte[] basicCodes = alphabet.basicCodes(alphabet.lettersToCode(new byte[] {'R'}, 0));
        Set<String> result = new TreeSet<>();
        for(byte code: basicCodes)
            result.add(alphabet.codeToLetters(code));
        assertEquals(new TreeSet<>(Arrays.asList("a", "g")), result);
    }
    
    public void testDiNucleotideAlphabet()
    {
        Alphabet alphabet = DiNucleotideAlphabet.getInstance();
        assertNotNull(alphabet);
        byte[] bytes = "AAACAGATNN".getBytes();
        assertEquals(16, alphabet.basicCodes().length);
        assertEquals(17, alphabet.size());
        assertEquals(2, alphabet.codeLength());
        assertEquals(0, alphabet.lettersToCode(bytes, 0));
        assertEquals(1, alphabet.lettersToCode(bytes, 2));
        assertEquals(2, alphabet.lettersToCode(bytes, 4));
        assertEquals(3, alphabet.lettersToCode(bytes, 6));
        assertEquals(alphabet.codeForAny(), alphabet.lettersToCode(bytes, 7));
        assertEquals(alphabet.codeForAny(), alphabet.lettersToCode(bytes, 8));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Nucleotide5LetterAlphabetEx tests
    //

    public void testLetterToCodeMatrix()
    {
        byte[] letterToCode = Nucleotide5LetterAlphabetEx.getInstance().letterToCodeMatrix();
        for (byte i = 0; i < sample.length; i++)
        {
            assertEquals("Wrong code for letter " + String.valueOf((char)sample[i][LETTER]),
                         sample[i][CODE],
                         letterToCode[sample[i][LETTER]]);
        }
    }

    public void testLetterComplimentMatrix()
    {
        byte[] letterCompliment = Nucleotide5LetterAlphabetEx.getInstance().letterComplementMatrix();
        for (byte i = 0; i < sample.length; i++)
        {
            assertEquals("Wrong compliment letter for letter " + String.valueOf((char)sample[i][LETTER]),
                         sample[i][COMPLEMENT_LETTER],
                         letterCompliment[sample[i][LETTER]]);
        }
    }

    private byte[][] sample =
    {
        {'a', 0, 't'},
        {'A', 0, 't'},
        {'c', 1, 'g'},
        {'C', 1, 'g'},
        {'g', 2, 'c'},
        {'G', 2, 'c'},
        {'t', 3, 'a'},
        {'T', 3, 'a'},
        {'u', 3, 'a'},
        {'U', 3, 'a'},
        {'n', 4, 'n'},
        {'N', 4, 'n'},
        {'r', 4, 'n'},
        {'R', 4, 'n'},
        {'y', 4, 'n'},
        {'Y', 4, 'n'},
        {'m', 4, 'n'},
        {'M', 4, 'n'},
        {'k', 4, 'n'},
        {'K', 4, 'n'},
        {'w', 4, 'n'},
        {'W', 4, 'n'},
        {'s', 4, 'n'},
        {'S', 4, 'n'},
        {'b', 4, 'n'},
        {'B', 4, 'n'},
        {'d', 4, 'n'},
        {'D', 4, 'n'},
        {'h', 4, 'n'},
        {'H', 4, 'n'},
        {'v', 4, 'n'},
        {'V', 4, 'n'}
    };

    private static int LETTER = 0;
    private static int CODE = 1;
    private static int COMPLEMENT_LETTER = 2;
}
