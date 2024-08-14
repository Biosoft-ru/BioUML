package ru.biosoft.bsastats.processors;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;

/**
 * @author lan
 *
 */
public interface StatisticsProcessor
{
    public enum Quality
    {
        OK, WARN, ERROR
    }
    
    public void init(Logger log);
    
    public void update(byte[] sequence, byte[] qualities);
    
    public void save(DataCollection<DataElement> resultsFolder) throws Exception;
    
    public String[] getReportItemNames();

    public String getName();
    
    public Quality getQuality();
    
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception;
}
