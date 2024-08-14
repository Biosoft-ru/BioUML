package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;

public class ReportProperties extends Option
{
    private boolean customReport = false;
    private DataElementPath reportPath = null;
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
        result.reportPath = DataElementPath.create( reportPath.toString() );
        result.visualizerPath = DataElementPath.create( visualizerPath.toString() );
        return result;
    }
}
