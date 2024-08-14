package ru.biosoft.bsa.analysis._test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.DiNucleotideAlphabet;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.transformer.WeightMatrixTransformer;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;

/**
 * @author lan
 *
 */
public class FrequencyMatrixTest extends TestCase
{
    public void testBindingElement() throws Exception
    {
        TranscriptionFactor tf = new TranscriptionFactor("SP1", null, "SP-1", null, "Homo sapiens");
        BindingElement be = new BindingElement("SP1 element", Arrays.asList(tf));
        assertTrue(Arrays.equals(new String[] {"SP-1/Homo sapiens (SP1)"}, be.getFactorNames()));
        assertTrue(Arrays.equals(new TranscriptionFactor[] {tf}, be.getFactors()));
        assertEquals("SP1 element", be.getName());
        assertEquals("SP1 element", be.toString());
        
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        TranscriptionFactor tf2 = new TranscriptionFactor("SP1", null, "SP-1", null, null);
        vdc.put(tf2);
        DataElementPathSet factors = new DataElementPathSet();
        factors.add(DataElementPath.create("test/SP1"));
        BindingElement be2 = new BindingElement("SP1 element-2", factors);
        assertTrue(Arrays.equals(new String[] {"SP-1 (SP1)"}, be2.getFactorNames()));
        assertTrue(Arrays.equals(new TranscriptionFactor[] {tf2}, be2.getFactors()));
        for(TranscriptionFactor factor: be2)
        {
            assertEquals(tf2, factor);
        }
        
        CollectionFactory.unregisterAllRoot();
    }
    
    public void testFrequencyMatrixBasics() throws Exception
    {
        BindingElement be = new BindingElement("SP1", Arrays.asList(new TranscriptionFactor("SP1", null, "SP-1", null, "Homo sapiens")));
        FrequencyMatrix matrix = new FrequencyMatrix(null, "m1", Nucleotide5LetterAlphabet.getInstance(), be, new double[][] {
            {1,2,3,4},
            {4,3,2,1},
            {10,0,0,0},
            {1,9,0,0},
            {0,0,5,5},
            }, false);
        assertEquals(Nucleotide5LetterAlphabet.getInstance(), matrix.getAlphabet());
        assertEquals(5, matrix.getLength());
        assertEquals(0.1, matrix.getFrequency(0, (byte)0), 0.000001);
        assertEquals(1.0, matrix.getFrequency(0, Nucleotide5LetterAlphabet.getInstance().codeForAny()));
        assertEquals(be, matrix.getBindingElement());
        assertEquals("SP1", matrix.getBindingElementName());
        assertEquals("m1", matrix.getAccession());
        double[] expectedContent = new double[] {0.15356065532898444, 0.15356065532898444, 2.0, 1.5310044064107187, 1.0};
        double[] gotContent = matrix.informationContent();
        assertEquals(expectedContent.length, gotContent.length);
        for(int i=0; i<expectedContent.length; i++)
            assertEquals(expectedContent[i], gotContent[i], 0.0001);
        
        FrequencyMatrix matrix2 = matrix.clone(null, "m2");
        assertEquals(matrix.getLength(), matrix2.getLength());
        assertEquals("m2", matrix2.getName());
        assertEquals(matrix.getAccession(), matrix2.getAccession());
        for(int i=0; i<matrix.getLength(); i++)
        {
            for(byte code=0; code<Nucleotide5LetterAlphabet.getInstance().size(); code++)
                assertEquals(matrix.getFrequency(i, code), matrix2.getFrequency(i, code));
        }
    }
    
    public void testUpdateMatrix() throws Exception
    {
        BindingElement be = new BindingElement("SP1", Arrays.asList(new TranscriptionFactor("SP1", null, "SP-1", null, "Homo sapiens")));
        Nucleotide5LetterAlphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
        FrequencyMatrix matrix = new FrequencyMatrix(null, "m1", alphabet, be, new double[][] {
            {},
            {},
            {},
            {},
            {},
            }, false);
        Sequence seq = new LinearSequence("ACGTA", alphabet);
        Sequence seq2 = new LinearSequence("AACCG", alphabet);
        matrix.updateFromSequences(Arrays.asList(seq, seq2));
        byte aCode = alphabet.lettersToCode(new byte[] {'A'}, 0);
        assertEquals(1.0, matrix.getFrequency(0, aCode));
        assertEquals(0.5, matrix.getFrequency(1, aCode));
        assertEquals(0.0, matrix.getFrequency(2, aCode));
        assertEquals(0.0, matrix.getFrequency(3, aCode));
        assertEquals(0.5, matrix.getFrequency(4, aCode));
    }
    
    public void testMatrixSerialization() throws Exception
    {
        BindingElement be = new BindingElement("SP1", Arrays.asList(new TranscriptionFactor("SP1", null, "SP-1", null, "Homo sapiens")));
        FrequencyMatrix matrix = new FrequencyMatrix(null, "m1", Nucleotide5LetterAlphabet.getInstance(), be, new double[][] {
            {1,2,3,4},
            {4,3,2,1},
            {10,0,0,0},
            {1,9,0,0},
            {0,0,5,5},
            }, false);
        WeightMatrixTransformer transformer = new WeightMatrixTransformer();
        Entry entry = transformer.transformOutput(matrix);
        assertNotNull(entry);
        assertEquals("m1", entry.getName());
        assertEquals("ID  m1\n"+
                "ALPHABET  ru.biosoft.bsa.Nucleotide5LetterAlphabet\n"+
                "MATR_LENGTH  5\n"+
                "NA  SP1\n"+
                "BF  SP1\n"+
                "WEIGHTS\n"+
                "1 A:0.1 C:0.2 G:0.3 T:0.4 \n"+
                "2 A:0.4 C:0.3 G:0.2 T:0.1 \n"+
                "3 A:1 C:0 G:0 T:0 \n"+
                "4 A:0.1 C:0.9 G:0 T:0 \n"+
                "5 A:0 C:0 G:0.5 T:0.5 \n"+
                "//", entry.getData().trim().replace("\r", ""));
        FrequencyMatrix matrix2 = transformer.transformInput(entry);
        assertEquals(matrix.getLength(), matrix2.getLength());
        assertEquals("m1", matrix2.getName());
        assertEquals(matrix.getAccession(), matrix2.getAccession());
        for(int i=0; i<matrix.getLength(); i++)
        {
            for(byte code=0; code<Nucleotide5LetterAlphabet.getInstance().size(); code++)
                assertEquals(matrix.getFrequency(i, code), matrix2.getFrequency(i, code), 0.00001);
        }
    }
    
    public void testDiNucleotideMatrix() throws Exception
    {
        BindingElement be = new BindingElement("SP1", Arrays.asList(new TranscriptionFactor("SP1", null, "SP-1", null, "Homo sapiens")));
        DiNucleotideAlphabet alphabet = DiNucleotideAlphabet.getInstance();
        FrequencyMatrix matrix = new FrequencyMatrix(null, "m1", alphabet, be, new double[][] {
            {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1},
            {0,0,0,1, 0,0,1,0, 0,1,0,0, 1,0,0,0},
            }, false);
        assertEquals(2, matrix.getLength());
        assertEquals(0.25, matrix.getFrequency(0, alphabet.lettersToCode("AA".getBytes(), 0)));
        assertEquals(1.0, matrix.getFrequency(0, alphabet.lettersToCode("NN".getBytes(), 0)));
        assertEquals(0.25, matrix.getFrequency(1, alphabet.lettersToCode("AT".getBytes(), 0)));
        assertEquals(0.25, matrix.getFrequency(1, alphabet.lettersToCode("TA".getBytes(), 0)));

        WeightMatrixTransformer transformer = new WeightMatrixTransformer();
        Entry entry = transformer.transformOutput(matrix);
        assertNotNull(entry);
        assertEquals("m1", entry.getName());
        assertEquals("ID  m1\n"+
                "ALPHABET  ru.biosoft.bsa.DiNucleotideAlphabet\n"+
                "MATR_LENGTH  2\n"+
                "NA  SP1\n"+
                "BF  SP1\n"+
                "WEIGHTS\n"+
                "1 AA:0.25 AC:0 AG:0 AT:0 CA:0 CC:0.25 CG:0 CT:0 GA:0 GC:0 GG:0.25 GT:0 TA:0 TC:0 TG:0 TT:0.25 \n"+
                "2 AA:0 AC:0 AG:0 AT:0.25 CA:0 CC:0 CG:0.25 CT:0 GA:0 GC:0.25 GG:0 GT:0 TA:0.25 TC:0 TG:0 TT:0 \n"+
                "//", entry.getData().trim().replace("\r", ""));
        FrequencyMatrix matrix2 = transformer.transformInput(entry);
        assertEquals(matrix.getLength(), matrix2.getLength());
        assertEquals("m1", matrix2.getName());
        assertEquals(matrix.getAccession(), matrix2.getAccession());
        for(int i=0; i<matrix.getLength(); i++)
        {
            for(byte code=0; code<alphabet.size(); code++)
                assertEquals(matrix.getFrequency(i, code), matrix2.getFrequency(i, code), 0.00001);
        }
    }

    public void testMatrixLogo() throws Exception
    {
        Nucleotide5LetterAlphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
        FrequencyMatrix matrix = new FrequencyMatrix(null, "m1", alphabet, null, new double[][] {
            {1,2,3,4},
            {4,3,2,1},
            {10,0,0,0},
            {1,9,0,0},
            {0,0,5,5},
            }, false);
        CompositeView view = matrix.getView();
        assertNotNull(view);
        assertEquals(matrix.getLength()+1, view.size());
        int i=0;
        for(View child: view)
        {
            if(child instanceof CompositeView)
            {
                CompositeView positionView = (CompositeView)child;
                Map<String, Double> weights = new HashMap<>();
                for(byte code: alphabet.basicCodes())
                {
                    double frequency = matrix.getFrequency(i, code);
                    if(frequency > 0)
                    {
                        weights.put(alphabet.codeToLetters(code).toUpperCase(), frequency);
                    }
                }
                assertEquals(weights.size(), positionView.size());
                for(View grandChild: positionView)
                {
                    assertTrue(grandChild instanceof TextView);
                    TextView letterView = (TextView)grandChild;
                    String text = letterView.getText();
                    assertNotNull(weights.remove(text));
                }
                assertTrue(weights.isEmpty());
                i++;
            }
        }
    }
    
}
