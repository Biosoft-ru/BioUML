
package ru.biosoft.analysis.diagram;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

/**
 * @author anna
 *
 */
@SuppressWarnings ( "serial" )
public class DiagramExtensionAnalysisParameters extends AbstractAnalysisParameters
{
    protected DataElementPath inputDiagramPath;
    protected DataElementPath outputDiagramPath;
    protected int stepNumber;
    protected boolean reactionsOnly;

    public DiagramExtensionAnalysisParameters()
    {
        stepNumber = 1;
        reactionsOnly = true;
    }

    public DataElementPath getInputDiagramPath()
    {
        return inputDiagramPath;
    }

    public void setInputDiagramPath(DataElementPath inputDiagramPath)
    {
        DataElementPath oldValue = this.inputDiagramPath;
        this.inputDiagramPath = inputDiagramPath;
        firePropertyChange("inputDiagramPath", oldValue, this.inputDiagramPath);
    }

    public DataElementPath getOutputDiagramPath()
    {
        return outputDiagramPath;
    }

    public void setOutputDiagramPath(DataElementPath outputDiagramPath)
    {
        DataElementPath oldValue = this.outputDiagramPath;
        this.outputDiagramPath = outputDiagramPath;
        firePropertyChange("outputDiagramPath", oldValue, this.outputDiagramPath);
    }

    public int getStepNumber()
    {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber)
    {
        int oldValue = this.stepNumber;
        this.stepNumber = stepNumber;
        firePropertyChange("iterationNumber", oldValue, this.stepNumber);
    }

    public boolean isReactionsOnly()
    {
        return reactionsOnly;
    }

    public void setReactionsOnly(boolean reactionsOnly)
    {
        Object oldValue = this.reactionsOnly;
        this.reactionsOnly = reactionsOnly;
        firePropertyChange("reactionsOnly", oldValue, reactionsOnly);
    }
}
