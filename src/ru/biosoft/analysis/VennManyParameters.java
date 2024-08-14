package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class VennManyParameters extends AbstractAnalysisParameters
{
    private int cases;
    private String counts;
    private DataElementPath outVenn;

    @PropertyName("Number of cases")
    public int getCases()
    {
        return cases;
    }
    public void setCases(int cases)
    {
        Object oldValue=this.cases;
        this.cases = cases;
        firePropertyChange("cases", oldValue, cases );
    }
    @PropertyName("Counts for intersections")
    @PropertyDescription("Comma-separated list of 2^N counts for all intersections")
    public String getCounts()
    {
        return counts;
    }
    public void setCounts(String counts)
    {
        Object oldValue=this.counts;
        this.counts = counts;
        firePropertyChange("counts", oldValue, counts );
    }
    @PropertyName("Output Venn diagram")
    public DataElementPath getOutVenn()
    {
        return outVenn;
    }
    public void setOutVenn(DataElementPath outVenn)
    {
        Object oldValue=this.outVenn;
        this.outVenn = outVenn;
        firePropertyChange("outVenn", oldValue, outVenn );
    }
}
