package biouml.plugins.optimization.analysis;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
public abstract class AbstractIdentifiabilityAnalysisParameters extends AbstractAnalysisParameters
{
    protected DataElementPath outputPath;
    protected double delta = 0.1;
    protected int maxSteps = 10;
    protected @Nonnull String plotType = IdentifiabilityHelper.PLOT_TYPE_PNG;
    protected double maxStepSize = 0.1;
    private String stepsRight = "";
    private String stepsLeft = "";
    private boolean manualSteps = false;
    private boolean manualBound = false;
    private boolean logX = false;
    private boolean logY = false;
    private double confidenceLevel = 0.95;
    private boolean saveSolutions = false;

    @PropertyName ( "Steps to the right" )
    public String getStepsRight()
    {
        return stepsRight;
    }
    public void setStepsRight(String stepsRight)
    {
        Object oldValue = this.stepsRight;
        this.stepsRight = stepsRight;
        firePropertyChange("stepsRight", oldValue, this.stepsRight);
    }

    @PropertyName ( "Steps to the left" )
    public String getStepsLeft()
    {
        return stepsLeft;
    }
    public void setStepsLeft(String stepsLeft)
    {
        Object oldValue = this.stepsLeft;
        this.stepsLeft = stepsLeft;
        firePropertyChange("stepsLeft", oldValue, this.stepsLeft);
    }

    public boolean isAutoSteps()
    {
        return !manualSteps;
    }

    @PropertyName ( "Set steps manually" )
    public boolean isManualSteps()
    {
        return manualSteps;
    }
    public void setManualSteps(boolean manualSteps)
    {
        Object oldValue = this.manualSteps;
        this.manualSteps = manualSteps;
        firePropertyChange("manualSteps", oldValue, this.manualSteps);
        firePropertyChange("*", null, null);
    }

    @PropertyName ( "Set Objective bound manually" )
    @PropertyDescription ( "If true then objective function devitation bound is set manually" )
    public boolean isManualBound()
    {
        return manualBound;
    }
    public void setManualBound(boolean manualBound)
    {
        boolean oldValue = this.manualBound;
        this.manualBound = manualBound;
        firePropertyChange( "manualBound", oldValue, this.manualBound );
        firePropertyChange( "*", null, null );
    }
    public boolean isAutoBound()
    {
        return !isManualBound();
    }

    @PropertyName ( "Manual deviation bound" )
    @PropertyDescription ( "Maximal deviation from initial objective function value" )
    public double getDelta()
    {
        return delta;
    }
    public void setDelta(double delta)
    {
        double oldValue = this.delta;
        this.delta = delta;
        firePropertyChange( "delta", oldValue, this.delta );
    }

    @PropertyName ( "Output path" )
    @PropertyDescription ( "Path to the output table" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange( "outputPath", oldValue, this.outputPath );
    }

    @PropertyName ( "Max step size (fraction of range)" )
    @PropertyDescription ( "Max step size fraction of range." )
    public double getMaxStepSize()
    {
        return maxStepSize;
    }
    public void setMaxStepSize(double maxStepSize)
    {
        double oldValue = this.maxStepSize;
        this.maxStepSize = maxStepSize;
        firePropertyChange("maxStepSize", oldValue, this.maxStepSize);
    }

    @PropertyName ( "Maximum identifiability steps" )
    @PropertyDescription ( "Maximum number of steps during parameter identifiability testing (in one direction)" )
    public int getMaxStepsNumber()
    {
        return maxSteps;
    }
    public void setMaxStepsNumber(int maxStepsNumber)
    {
        int oldValue = this.maxSteps;
        this.maxSteps = maxStepsNumber;
        firePropertyChange( "maxStepsNumber", oldValue, this.maxSteps );
    }

    @PropertyName ("Save solutions")
    @PropertyDescription ("Should solutions found by the analysis be saved?")
    public boolean isSaveSolutions()
    {
        return saveSolutions;
    }
    public void setSaveSolutions(boolean saveSolutions)
    {
        this.saveSolutions = saveSolutions;
    }

    @PropertyName ( "Plot element type" )
    @PropertyDescription ( "Type of created result plot elements" )
    public @Nonnull String getPlotType()
    {
        return plotType;
    }
    public void setPlotType(String plotType)
    {
        if( plotType == null )
            plotType = IdentifiabilityHelper.PLOT_TYPE_PNG;
        String oldValue = this.plotType;
        this.plotType = plotType;
        firePropertyChange( "plotType", oldValue, this.plotType );
    }

    @PropertyName ( "Confidence level" )
    @PropertyDescription ( "Confidence level." )
    public double getConfidenceLevel()
    {
        return confidenceLevel;
    }
    public void setConfidenceLevel(double confidenceLevel)
    {
        double oldValue = this.confidenceLevel;
        this.confidenceLevel = confidenceLevel;
        firePropertyChange( "confidenceLevel", oldValue, confidenceLevel );
    }

    public abstract Diagram getDiagram();
    protected abstract OptimizationMethod<?> getOptimizationMethod();

    @PropertyName ( "Logarithm Y" )
    public boolean isLogY()
    {
        return logY;
    }
    public void setLogY(boolean logY)
    {
        this.logY = logY;
    }

    @PropertyName ( "Logarithm X" )
    public boolean isLogX()
    {
        return logX;
    }
    public void setLogX(boolean logX)
    {
        this.logX = logX;
    }
}