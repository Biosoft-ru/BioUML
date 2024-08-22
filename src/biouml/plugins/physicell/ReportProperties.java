package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;

public class ReportProperties extends Option
{
    private boolean customReport = false;
    private DataElementPath reportPath = null;
    private boolean customGlobalReport = false;
    private DataElementPath globalReportPath = null;
    private boolean customVisualizer = false;
    private DataElementPath visualizerPath = null;
    
    @PropertyName("Report")
    public DataElementPath getReportPath()
    {
        return reportPath;
    }
    public void setReportPath(DataElementPath reportPath)
    {
        this.reportPath = reportPath;
    }
    
    
    @PropertyName("Global report")
    public DataElementPath getGlobalReportPath()
    {
        return globalReportPath;
    }
    public void setGlobalReportPath(DataElementPath globalReportPath)
    {
        this.reportPath = globalReportPath;
    }
    
    @PropertyName("Visualizer")
    public DataElementPath getVisualizerPath()
    {
        return visualizerPath;
    }
    public void setVisualizerPath(DataElementPath visualizerPath)
    {
        this.visualizerPath = visualizerPath;
    }
    
    @PropertyName("Custom report")
    public boolean isCustomReport()
    {
        return customReport;
    }
    public void setCustomReport(boolean customReport)
    {
        boolean oldValue = this.customReport;
        this.customReport = customReport;
        firePropertyChange( "customReport", oldValue, customReport );
        firePropertyChange( "*", null, null );
    }
    
    @PropertyName("Custom global report")
    public boolean isCustomGlobalReport()
    {
        return customGlobalReport;
    }
    public void setCustomGlobalReport(boolean customGlobalReport)
    {
        boolean oldValue = this.customGlobalReport;
        this.customGlobalReport = customGlobalReport;
        firePropertyChange( "customGlobalReport", oldValue, customGlobalReport );
        firePropertyChange( "*", null, null );
    }
    
    public boolean isDefaultGlobalReport()
    {
        return !isCustomGlobalReport();
    }
    
    
    public boolean isDefaultReport()
    {
        return !isCustomReport();
    }
    
    @PropertyName("Custom visualizer")
    public boolean isCustomVisualizer()
    {
        return customVisualizer;
    }
    public void setCustomVisualizer(boolean customVisualizer)
    {
        boolean oldValue = this.customVisualizer;
        this.customVisualizer = customVisualizer;
        firePropertyChange( "customVisualizer", oldValue, customVisualizer );
        firePropertyChange( "*", null, null );
    }
    public boolean isDefaultVisualizer()
    {
        return !isCustomVisualizer();
    }
    
    @Override
    public ReportProperties clone()
    {
        ReportProperties result = new ReportProperties();
        result.customReport = customReport;
        result.customVisualizer = customVisualizer;
        if( reportPath != null )
            result.reportPath = DataElementPath.create( reportPath.toString() );
        if( visualizerPath != null )
            result.visualizerPath = DataElementPath.create( visualizerPath.toString() );
        return result;
    }
}
