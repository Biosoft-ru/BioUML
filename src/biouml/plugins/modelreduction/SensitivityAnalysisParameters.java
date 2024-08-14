package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import ru.biosoft.access.core.DataElementPath;

@PropertyName ( "Parameters" )
public class SensitivityAnalysisParameters extends SteadyStateAnalysisParameters
{
    public SensitivityAnalysisParameters()
    {
        setRelativeTolerance(1E-15);
        setAbsoluteTolerance(1E-20);
    }

    private VariableSet[] targetVariables = new VariableSet[0];
    private VariableSet[] inputVariables = new VariableSet[0];
    private double absoluteStep = 1E-6;
    private double relativeStep = 0;
    
    @PropertyName ( "Target variables" )
    @PropertyDescription ( "Sensitivity will be calculated for these variables." )
    public VariableSet[] getTargetVariables()
    {
        return targetVariables;
    }
    public void setTargetVariables(VariableSet ... targetVariables)
    {
        for (VariableSet var: targetVariables)
        {
            var.setEngine(getEngineWrapper().getEngine());
            var.setDiagram(this.getDiagram());
        }
        this.targetVariables = targetVariables;
    }

    @PropertyName ( "Input variables" )
    @PropertyDescription ( "These variables will be used to calculate sensitivity." )
    public VariableSet[] getInputVariables()
    {
        return inputVariables;
    }
    public void setInputVariables(VariableSet ... inputVariables)
    {
        for (VariableSet var: inputVariables)
        {
            var.setEngine(getEngineWrapper().getEngine());
            var.setDiagram(this.getDiagram());
        }
        this.inputVariables = inputVariables;
    }
    
    @Override
    @PropertyName ( "Result path" )
    @PropertyDescription ( "A folder to save results of the analysis." )
    public DataElementPath getOutput()
    {
        return super.getOutput();
    }  
    
    @Override
    public void setDiagram(Diagram diagram)
    {
    	super.setDiagram(diagram);
    	setInputVariables(new VariableSet[] {new VariableSet(diagram, new String[] {VariableSet.CONSTANT_PARAMETERS})});
    	setTargetVariables(new VariableSet[] {new VariableSet(diagram, new String[] {VariableSet.RATE_VARIABLES})});
    	setVariableNames(new VariableSet[] {new VariableSet(diagram, new String[] {VariableSet.RATE_VARIABLES})});
    }
    
    @PropertyName("Absolute step value")
    public double getAbsoluteStep()
    {
        return absoluteStep;
    }
    public void setAbsoluteStep(double absoluteStep)
    {
        this.absoluteStep = absoluteStep;
    }
    
    @PropertyName("Relative step value")
    public double getRelativeStep()
    {
        return relativeStep;
    }
    public void setRelativeStep(double relativeStep)
    {
        this.relativeStep = relativeStep;
    }
}
