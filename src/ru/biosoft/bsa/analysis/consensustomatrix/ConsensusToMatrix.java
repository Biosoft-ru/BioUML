package ru.biosoft.bsa.analysis.consensustomatrix;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.JavaScriptBSA;
import ru.biosoft.bsa.transformer.WeightMatrixTransformer;

public class ConsensusToMatrix extends AnalysisMethodSupport<ConsensusToMatrixParameters>
{
    public ConsensusToMatrix(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new ConsensusToMatrixParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths(parameters.getInputNames(), new String[] {"outputCollection"});
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String consensus = parameters.getConsensus();
        if(!parameters.getOutputCollection().exists())
            WeightMatrixTransformer.createMatrixLibrary(parameters.getOutputCollection());
        DataCollection<DataElement> dc = parameters.getOutputCollection().getDataCollection();
        FrequencyMatrix matrix = consensusToMatrix(consensus, dc, parameters.getMatrixName(), null);
        dc.put(matrix);
        return new Object[] {dc, matrix};
    }

    public static @Nonnull FrequencyMatrix consensusToMatrix(String consensus, DataCollection<?> origin, String name, BindingElement bindingElement) throws IllegalArgumentException
    {
        Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();
        byte[] consensusBytes = consensus.getBytes();
        double[][] m = new double[consensusBytes.length-alphabet.codeLength()+1][alphabet.size()];
        for( int i = 0; i < m.length; i++ )
        {
            byte code = alphabet.lettersToCode(consensusBytes, i);
            if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR )
                throw new IllegalArgumentException("Consensus "+consensus+" contains invalid character at position " + i + "");
            for( byte basicCode : alphabet.basicCodes(code) )
                m[i][basicCode] = 1;
        }
        return new FrequencyMatrix(origin, name, alphabet, bindingElement, m, false);
    }

}
