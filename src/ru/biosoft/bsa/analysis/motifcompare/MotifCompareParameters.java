package ru.biosoft.bsa.analysis.motifcompare;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class MotifCompareParameters extends AbstractAnalysisParameters
{
    private DataElementPathSet siteModels;
    private DataElementPath sequences;
    private DataElementPath backgroundSequences;
    private int numberOfPermutations = 10;
    private long seed;
    private DataElementPath output;
    private double modelFDR = -1;

    public DataElementPathSet getSiteModels()
    {
        return siteModels;
    }

    public void setSiteModels(DataElementPathSet siteModels)
    {
        Object oldValue = this.siteModels;
        this.siteModels = siteModels;
        firePropertyChange("siteModels", oldValue, siteModels);
    }

    public DataElementPath getSequences()
    {
        return sequences;
    }

    public void setSequences(DataElementPath sequences)
    {
        Object oldValue = this.sequences;
        this.sequences = sequences;
        firePropertyChange("sequences", oldValue, sequences);
    }
    
    public DataElementPath getBackgroundSequences()
    {
        return backgroundSequences;
    }

    public void setBackgroundSequences(DataElementPath backgroundSequences)
    {
        Object oldValue = this.backgroundSequences;
        this.backgroundSequences = backgroundSequences;
        firePropertyChange("backgroundSequences", oldValue, backgroundSequences);
    }

    public DataElementPath getOutput()
    {
        return output;
    }

    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }

    public int getNumberOfPermutations()
    {
        return numberOfPermutations;
    }

    public void setNumberOfPermutations(int numberOfPermutations)
    {
        Object oldValue = this.numberOfPermutations;
        this.numberOfPermutations = numberOfPermutations;
        firePropertyChange("numberOfPermutations", oldValue, numberOfPermutations);
    }

    public long getSeed()
    {
        return seed;
    }

    public void setSeed(long seed)
    {
        Object oldValue = this.seed;
        this.seed = seed;
        firePropertyChange("seed", oldValue, seed);
    }

    /**
     * @return the modelFDR
     */
    public double getModelFDR()
    {
        return modelFDR;
    }

    /**
     * @param modelFDR the modelFDR to set
     */
    public void setModelFDR(double modelFDR)
    {
        Object oldValue = this.modelFDR;
        this.modelFDR = modelFDR;
        firePropertyChange("modelFDR", oldValue, modelFDR);
    }
    


}
