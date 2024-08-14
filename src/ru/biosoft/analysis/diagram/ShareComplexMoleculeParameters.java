package ru.biosoft.analysis.diagram;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class ShareComplexMoleculeParameters extends AbstractAnalysisParameters
{
    private DataElementPath diagramPath;
    private DataElementPath outputPath;
    private String[] elementNames = new String[] {};

    @PropertyName ( "Diagram path" )
    @PropertyDescription ( "Path to input diagram" )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }

    public void setDiagramPath(DataElementPath modelPath)
    {
        Object oldValue = this.diagramPath;
        this.diagramPath = modelPath;
        firePropertyChange("diagramPath", oldValue, modelPath);
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
        firePropertyChange("variableNames", oldValue, elementNames);
    }

    @PropertyName ( "Output path" )
    @PropertyDescription ( "Path to the output diagram" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, outputPath);
    }
}
