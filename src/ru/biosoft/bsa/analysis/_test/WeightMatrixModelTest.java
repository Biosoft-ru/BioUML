package ru.biosoft.bsa.analysis._test;

import ru.biosoft.bsa.DiNucleotideAlphabet;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class WeightMatrixModelTest extends TestCase
{
    public void testWeightMatrixModelBasics() throws Exception
    {
        Nucleotide5LetterAlphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
        FrequencyMatrix matrix = new FrequencyMatrix(null, "mat1", alphabet, null, new double[][] {
            {1,0,0,0},
            {0,1,0,0},
            {0,0,1,0},
            {0,0,0,1},
            {0,0,1,0},
            }, false);
        WeightMatrixModel model = new WeightMatrixModel("m1", null, matrix, 3.0);
        assertEquals("m1", model.getName());
        assertEquals("mat1", model.getMatrixPath().getName());
        assertEquals(alphabet, model.getAlphabet());
        assertEquals(5, model.getLength());
        assertEquals(3.0, model.getThreshold());
        assertEquals(0.0, model.getMinScore());
        assertEquals(5.0, model.getMaxScore());
        assertEquals(model.getMaxScore(), model.getScore(new LinearSequence("ACGTG", alphabet), 1));
        assertEquals(model.getMinScore(), model.getScore(new LinearSequence("CGTGA", alphabet), 1));
        assertEquals(3.0, model.getScore(new LinearSequence("AAGGG", alphabet), 1));
        
        WeightMatrixModel model2 = model.clone(null, "m2");
        assertEquals(matrix, model2.getFrequencyMatrix());
        assertEquals(5, model2.getLength());
        assertEquals(3.0, model2.getThreshold());
        assertEquals(0.0, model2.getMinScore());
        assertEquals(5.0, model2.getMaxScore());
        assertEquals(model2.getMaxScore(), model2.getScore(new LinearSequence("ACGTG", alphabet), 1));
        assertEquals(model2.getMinScore(), model2.getScore(new LinearSequence("CGTGA", alphabet), 1));
        assertEquals(3.0, model2.getScore(new LinearSequence("AAGGG", alphabet), 1));
    }
    
    public void testSequentialMatching() throws Exception
    {
        Nucleotide5LetterAlphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
        FrequencyMatrix matrix = new FrequencyMatrix(null, "mat1", alphabet, null, new double[][] {
            {1,0,0,0},
            {0,1,0,0},
            {0,0,1,0},
            {0,0,0,1},
            {0,0,1,0},
            }, false);
        WeightMatrixModel model = new WeightMatrixModel("m1", null, matrix, 3.0);
        Sequence seq = new LinearSequence("ACGTACGTACGT", alphabet);
        double[] expectedScores = new double[] {4.0, 0.0, 1.0, 0.0, 4.0, 0.0, 1.0, 0.0};
        for(int i = seq.getStart(); i <= seq.getStart() + seq.getLength() - model.getLength(); i++)
        {
            assertEquals(expectedScores[i-seq.getStart()], model.getScore(seq, i));
        }
    }
    
    public void testDinucleotideModel() throws Exception
    {
        DiNucleotideAlphabet alphabet = DiNucleotideAlphabet.getInstance();
        FrequencyMatrix matrix = new FrequencyMatrix(null, "m1", alphabet, null, new double[][] {
            {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1},
            {0,0,0,1, 0,0,1,0, 0,1,0,0, 1,0,0,0},
            }, false);
        WeightMatrixModel model = new WeightMatrixModel("m1", null, matrix, 3.0);
        assertEquals(3, model.getLength());
        assertEquals(0.5, model.getMaxScore());
        assertEquals(0.0, model.getMinScore());
        assertEquals(0.5, model.getScore(new LinearSequence("AAT", alphabet), 1));
        assertEquals(0.25, model.getScore(new LinearSequence("AAA", alphabet), 1));
        assertEquals(0.25, model.getScore(new LinearSequence("ATA", alphabet), 1));
        assertEquals(0.0, model.getScore(new LinearSequence("ATT", alphabet), 1));
    }
}
