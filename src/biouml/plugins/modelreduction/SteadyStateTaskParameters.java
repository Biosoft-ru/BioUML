package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineWrapper;
import biouml.plugins.simulation.SimulationTaskParameters;

@PropertyName ("Parameters")
public class SteadyStateTaskParameters extends SimulationTaskParameters
{
    private double startSearchTime = 0.0;
    private int validationSize = 100;
    private double absoluteTolerance = 1.0E-7;
    private double relativeTolerance = 1.0E-7;
    private String[] variableNames = new String[] {};


    public void setEngineWrapper(SimulationEngineWrapper engineWrapper)
    {
        this.engineWrapper = engineWrapper;
        this.engineWrapper.setParent(this);
    }

    @Override
    public SimulationEngine getSimulationEngine()
    {
        return ( engineWrapper != null ) ? engineWrapper.getEngine() : null;
    }

    public void setVariableNames(String ... variableNames)
    {
        this.variableNames = variableNames;
    }

    @PropertyName("Variable names")
    @PropertyDescription("Variables which values will be used for steady state detection." + " If no variables selected then all variables will be used.")
    public String[] getVariableNames()
    {
        return variableNames;
    }

    @PropertyName("Start search time")
    @PropertyDescription("A time point to start steady state searching.")
    public double getStartSearchTime()
    {
        return startSearchTime;
    }

    public void setStartSearchTime(double startSearchTime)
    {
        Object oldValue = this.startSearchTime;
        this.startSearchTime = startSearchTime;
        firePropertyChange("startSearchTime", oldValue, this.startSearchTime);
    }

    @PropertyName("Absolute tolerance")
    @PropertyDescription("An absolute tolerance to check a steady state.")
    public double getAbsoluteTolerance()
    {
        return absoluteTolerance;
    }

    public void setAbsoluteTolerance(double tolerance)
    {
        Object oldValue = this.absoluteTolerance;
        this.absoluteTolerance = tolerance;
        firePropertyChange("absoluteTolerance", oldValue, this.absoluteTolerance);
    }

    @PropertyName("Relative tolerance")
    @PropertyDescription("A relative tolerance to check a steady state.")
    public double getRelativeTolerance()
    {
        return relativeTolerance;
    }

    public void setRelativeTolerance(double tolerance)
    {
        Object oldValue = this.relativeTolerance;
        this.relativeTolerance = tolerance;
        firePropertyChange("relativeTolerance", oldValue, this.relativeTolerance);
    }

    @PropertyName("Validation series size")
    @PropertyDescription("A size of parameter time series to check a steady state.")
    public int getValidationSize()
    {
        return validationSize;
    }

    public void setValidationSize(int validationSize)
    {
        Object oldValue = this.validationSize;
        this.validationSize = validationSize;
        firePropertyChange("validationSize", oldValue, this.validationSize);
    }

    @Override
    public Option getParametersBean()
    {
        return this;
    }
}
