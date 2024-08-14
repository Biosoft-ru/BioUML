package ru.biosoft.analysis.diagram;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class RemoveUnobservableMoleculesParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputPath;
    private String[] elementNames = new String[] {};
    private DataElementPath outputPath;
    private boolean deleteAllElements = false;

    @PropertyName ( "Input path" )
    @PropertyDescription ( "Path to input diagram" )
    public DataElementPath getInputPath()
    {
        return inputPath;
    }
    public void setInputPath(DataElementPath modelPath)
    {
        Object oldValue = this.inputPath;
        this.inputPath = modelPath;
        firePropertyChange("inputPath", oldValue, modelPath);
    }

    @PropertyName ( "Output path" )
    @PropertyDescription ( "Path to resulting diagram" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath modelPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = modelPath;
        firePropertyChange("outputPath", oldValue, modelPath);
    }

    @PropertyName ( "Element names" )
    @PropertyDescription ( "list with element names" )
    public String[] getElementNames()
    {
        return elementNames;
    }
    public void setElementNames(String ... elementNames)
    {
        Object oldValue = this.elementNames;
        this.elementNames = elementNames;
        firePropertyChange("equationNames", oldValue, elementNames);
    }

    @PropertyName ( "Delete all elements" )
    @PropertyDescription ( "flag used to show should all unobservable nodes be deleted or not" )
    public boolean isDeleteAllElements()
    {
        return deleteAllElements;
    }
    public void setDeleteAllElements(boolean deleteAllElements)
    {
        Object oldValue = this.deleteAllElements;
        this.deleteAllElements = deleteAllElements;
        firePropertyChange("deleteAllElements", oldValue, deleteAllElements);
    }
}
