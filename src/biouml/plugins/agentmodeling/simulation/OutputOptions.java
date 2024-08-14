package biouml.plugins.agentmodeling.simulation;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.Options;
import ru.biosoft.access.core.DataElementPath;

@PropertyName("Output options")
public class OutputOptions extends Options
{   
    private boolean saveResults = false;
    private boolean plotResults = true;
    private boolean includeEventPoints = false;
    private DataElementPath resultPath;
    private int initialTime = 0;
    private int completionTime = 100;
    private int timeIncrement = 1; 
    
    @PropertyName("Save simulation results")
    public boolean isSaveResults()
    {
        return saveResults;
    }
    public void setSaveResults(boolean saveResults)
    {
        boolean oldValue = this.saveResults;
        this.saveResults = saveResults;
        firePropertyChange( "saveResults", oldValue, saveResults );
        firePropertyChange( "*", null, null );
    }
    
    @PropertyName("Plot simulation results")
    public boolean isPlotResults()
    {
        return plotResults;
    }
    public void setPlotResults(boolean plotResults)
    {
        boolean oldValue = this.plotResults;
        this.plotResults = plotResults;
        firePropertyChange( "plotResults", oldValue, plotResults );
    }
    
    @PropertyName("Simulation result path")
    public DataElementPath getResultPath()
    {
        return resultPath;
    }
    public void setResultPath(DataElementPath resultPath)
    {
        DataElementPath oldValue = this.resultPath;
        this.resultPath = resultPath;
        firePropertyChange( "resultPath", oldValue, resultPath );
    }

    @PropertyName("Initial time")
    public int getInitialTime()
    {
        return initialTime;
    }
    public void setInitialTime(int initialTime)
    {
        int oldValue = this.initialTime;
        this.initialTime = initialTime;
        firePropertyChange( "initialTime", oldValue, initialTime );
    }
    
    @PropertyName("Completion time")
    public int getCompletionTime()
    {
        return completionTime;
    }
    public void setCompletionTime(int completionTime)
    {
        int oldValue = this.completionTime;
        this.completionTime = completionTime;
        firePropertyChange( "completionTime", oldValue, completionTime );
    }
    
    @PropertyName("Time step")
    public int getTimeIncrement()
    {
        return timeIncrement;
    }
    public void setTimeIncrement(int timeIncrement)
    {
        int oldValue = this.timeIncrement;
        this.timeIncrement = timeIncrement;
        firePropertyChange( "timeIncrement", oldValue, timeIncrement );
    }
    
    @PropertyName("Include event points")
    public boolean isIncludeEventPoints()
    {
        return includeEventPoints;
    }
    public void setIncludeEventPoints(boolean includeEventPoints)
    {
        boolean oldValue = this.includeEventPoints;
        this.includeEventPoints = includeEventPoints;
        firePropertyChange( "includeEventPoints", oldValue, includeEventPoints );
    }
    
    public boolean dontSaveResults()
    {
        return !isSaveResults(); 
    }
}