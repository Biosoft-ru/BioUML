package ru.biosoft.bsa.analysis.createsitemodel;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class CreateSiteModelParameters extends AbstractAnalysisParameters
{
    protected DataElementPath outputCollection;
    protected String modelName;

    public DataElementPath getOutputCollection()
    {
        return outputCollection;
    }
    
    public void setOutputCollection(DataElementPath outputCollection)
    {
        Object oldValue = this.outputCollection;
        this.outputCollection = outputCollection;
        firePropertyChange("output", oldValue, outputCollection);
    }
    
    public String getModelName()
    {
        return modelName;
    }
    
    public void setModelName(String modelName)
    {
        Object oldValue = this.modelName;
        this.modelName = modelName;
        firePropertyChange("modelName", oldValue, modelName);
    }
    
    public DataElementPath getModelPath()
    {
        return getOutputCollection() == null || getModelName() == null?null:getOutputCollection().getChildPath(getModelName());
    }
    
    public void setModelPath(DataElementPath path)
    {
        if(path == null) return;
        setOutputCollection(path.getParentPath());
        setModelName(path.getName());
    }

    @Override
    public @Nonnull String[] getOutputNames()
    {
        return new String[] {"modelPath"};
    }
}
