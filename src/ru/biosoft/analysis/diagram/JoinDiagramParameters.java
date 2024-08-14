package ru.biosoft.analysis.diagram;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

/**
 * @author anna
 *
 */
@SuppressWarnings ( "serial" )
public class JoinDiagramParameters extends AbstractAnalysisParameters
{
    private DataElementPathSet inputDiagrams = new DataElementPathSet();
    private DataElementPath outputDiagramPath;
    private String layouterName = AUTO_LAYOUTER;

    public static final String NONE_LAYOUTER = "none";
    public static final String AUTO_LAYOUTER = "auto";

    public DataElementPathSet getInputDiagrams()
    {
        return inputDiagrams;
    }
    public void setInputDiagrams(DataElementPathSet inputDiagrams)
    {
        Object oldValue = this.inputDiagrams;
        this.inputDiagrams = inputDiagrams;
        firePropertyChange("inputDiagrams", oldValue, inputDiagrams);
    }

    public DataElementPath getOutputDiagramPath()
    {
        return outputDiagramPath;
    }
    public void setOutputDiagramPath(DataElementPath outputDiagramPath)
    {
        Object oldValue = this.outputDiagramPath;
        this.outputDiagramPath = outputDiagramPath;
        firePropertyChange("outputDiagramPath", oldValue, outputDiagramPath);
    }

    public String getLayouterName()
    {
        return layouterName;
    }

    public void setLayouterName(String layouterName)
    {
        Object oldValue = this.layouterName;
        this.layouterName = layouterName;
        firePropertyChange("layouterName", oldValue, layouterName);
    }

    public void setLayouterNameString(String name)
    {
        setLayouterName(name);
    }
}
