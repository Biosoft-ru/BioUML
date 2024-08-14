package ru.biosoft.bsa.analysis.createsitemodel;

import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataElementPath;

public class CreateWeightMatrixModelParameters extends CreateSiteModelParameters
{
    private DataElementPath matrixPath;
    static final String[] MODEL_TYPES = {"Identity", "Log", "LogOdds"};
    private String modelType = MODEL_TYPES[0];
    private NucleotideFrequencies nucleotideFrequencies = new NucleotideFrequencies();
    private double threshold;

    public DataElementPath getMatrixPath()
    {
        return matrixPath;
    }

    public void setMatrixPath(DataElementPath matrixPath)
    {
        Object oldValue = this.matrixPath;
        this.matrixPath = matrixPath;
        firePropertyChange("matrixPath", oldValue, matrixPath);
    }

    public String getModelType()
    {
        return modelType;
    }
    public void setModelType(String modelType)
    {
        Object oldValue = this.modelType;
        this.modelType = modelType;
        firePropertyChange("*", null, null);
    }

    public NucleotideFrequencies getNucleotideFrequencies()
    {
        return nucleotideFrequencies;
    }

    public void setNucleotideFrequencies(NucleotideFrequencies nucleotideFrequencies)
    {
        Object oldValue = this.nucleotideFrequencies;
        this.nucleotideFrequencies = nucleotideFrequencies;
        firePropertyChange("nucleotideFrequencies", oldValue, nucleotideFrequencies);
    }
    
    public double getThreshold()
    {
        return threshold;
    }
    
    public void setThreshold(double threshold)
    {
        Object oldValue = this.threshold;
        this.threshold = threshold;
        firePropertyChange("threshold", oldValue, threshold);
    }
    
    public boolean isNucleotideFrequenciesHidden()
    {
        return !modelType.equals("LogOdds");
    }
    
    public static class ModelTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return MODEL_TYPES;
        }
    }
}
