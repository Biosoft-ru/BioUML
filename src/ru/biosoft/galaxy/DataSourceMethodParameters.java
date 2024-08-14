package ru.biosoft.galaxy;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.journal.JournalRegistry;

public class DataSourceMethodParameters extends AbstractAnalysisParameters
{
    private static final long serialVersionUID = 1L;
    private DataSourceMethodInfo methodInfo;
    private DataElementPath outputPath = DataElementPath.create("data");
    private DataSourceURLBuilder urlBuilder;

    public DataSourceMethodParameters(DataSourceMethodInfo methodInfo)
    {
        this.methodInfo = methodInfo;
        urlBuilder = new DataSourceURLBuilder(this.methodInfo, this);
        try
        {
            outputPath = JournalRegistry.getProjectPath().getChildPath("Data");
        }
        catch(Exception e)
        {
        }
    }

    /**
     * @return the outputPath
     */
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    /**
     * @param outputPath the outputPath to set
     */
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, outputPath);
    }
    
    public String getURLDisplayName()
    {
        return methodInfo.getUrlTitle();
    }
    
    public DataSourceURLBuilder getUrlBuilder()
    {
        return urlBuilder;
    }
}