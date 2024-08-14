package ru.biosoft.bsa.analysis.consensustomatrix;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class ConsensusToMatrixParameters extends AbstractAnalysisParameters
{
    private String consensus;
    private DataElementPath outputCollection;
    private String matrixName;
    
    /**
     * @return the consensus
     */
    public String getConsensus()
    {
        return consensus;
    }
    /**
     * @param consensus the consensus to set
     */
    public void setConsensus(String consensus)
    {
        Object oldValue = this.consensus;
        this.consensus = consensus;
        firePropertyChange("consensus", oldValue, consensus);
    }
    /**
     * @return the matrix
     */
    public DataElementPath getMatrix()
    {
        return (outputCollection == null || matrixName == null)?null:outputCollection.getChildPath(matrixName);
    }
    /**
     * @param matrix the matrix to set
     */
    public void setMatrix(DataElementPath matrix)
    {
        if(matrix == null)
            return;
        setOutputCollection(matrix.getParentPath());
        setMatrixName(matrix.getName());
    }
    /**
     * @return the outputCollection
     */
    public DataElementPath getOutputCollection()
    {
        return outputCollection;
    }
    /**
     * @param outputCollection the outputCollection to set
     */
    public void setOutputCollection(DataElementPath outputCollection)
    {
        Object oldValue = this.outputCollection;
        this.outputCollection = outputCollection;
        firePropertyChange("outputCollection", oldValue, outputCollection);
    }
    /**
     * @return the matrixName
     */
    public String getMatrixName()
    {
        return matrixName;
    }
    /**
     * @param matrixName the matrixName to set
     */
    public void setMatrixName(String matrixName)
    {
        Object oldValue = this.matrixName;
        this.matrixName = matrixName;
        firePropertyChange("matrixName", oldValue, matrixName);
    }
    
    @Override
    public @Nonnull String[] getOutputNames()
    {
        return new String[]{"matrix"};
    }

    
}
