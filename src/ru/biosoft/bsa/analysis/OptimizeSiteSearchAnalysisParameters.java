package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
public class OptimizeSiteSearchAnalysisParameters extends AbstractAnalysisParameters
{
    public static final String OPTIMIZE_CUTOFF = "Cutoffs";
    public static final String OPTIMIZE_WINDOW = "Window";
    public static final String OPTIMIZE_BOTH = "Both";
    
    private DataElementPath inYesTrack, outYesTrack, inNoTrack, outNoTrack, outSummaryTable, outProfile;
    private Double pvalueCutoff;
    private String optimizationType;
    private boolean overrepresentedOnly = false;
    
    public OptimizeSiteSearchAnalysisParameters()
    {
        pvalueCutoff = 0.1;
        optimizationType = OPTIMIZE_CUTOFF;
    }

    public DataElementPath getInYesTrack()
    {
        return inYesTrack;
    }
    public void setInYesTrack(DataElementPath inYesTrack)
    {
        Object oldValue = this.inYesTrack;
        this.inYesTrack = inYesTrack;
        firePropertyChange("inYesTrack", oldValue, this.inYesTrack);
    }
    public DataElementPath getOutYesTrack()
    {
        return outYesTrack;
    }
    public void setOutYesTrack(DataElementPath outYesTrack)
    {
        Object oldValue = this.outYesTrack;
        this.outYesTrack = outYesTrack;
        firePropertyChange("outYesTrack", oldValue, this.outYesTrack);
    }
    public DataElementPath getInNoTrack()
    {
        return inNoTrack;
    }
    public void setInNoTrack(DataElementPath inNoTrack)
    {
        Object oldValue = this.inNoTrack;
        this.inNoTrack = inNoTrack;
        firePropertyChange("inNoTrack", oldValue, this.inNoTrack);
    }
    public DataElementPath getOutNoTrack()
    {
        return outNoTrack;
    }
    public void setOutNoTrack(DataElementPath outNoTrack)
    {
        Object oldValue = this.outNoTrack;
        this.outNoTrack = outNoTrack;
        firePropertyChange("outNoTrack", oldValue, this.outNoTrack);
    }
    public DataElementPath getOutSummaryTable()
    {
        return outSummaryTable;
    }
    public void setOutSummaryTable(DataElementPath outSummaryTable)
    {
        Object oldValue = this.outSummaryTable;
        this.outSummaryTable = outSummaryTable;
        firePropertyChange("outSummaryTable", oldValue, this.outSummaryTable);
    }
    public Double getPvalueCutoff()
    {
        return pvalueCutoff;
    }
    public void setPvalueCutoff(Double pvalueCutoff)
    {
        Object oldValue = this.pvalueCutoff;
        this.pvalueCutoff = pvalueCutoff;
        firePropertyChange("pvalueCutoff", oldValue, this.pvalueCutoff);
    }

    public String getOptimizationType()
    {
        return optimizationType;
    }

    public void setOptimizationType(String optimizationType)
    {
        Object oldValue = this.optimizationType;
        this.optimizationType = optimizationType;
        firePropertyChange("optimizationType", oldValue, this.optimizationType);
    }

    public void setOverrepresentedOnly(boolean overrepresentedOnly)
    {
        Object oldValue = this.overrepresentedOnly;
        this.overrepresentedOnly = overrepresentedOnly;
        firePropertyChange("overrepresentedOnly", oldValue, this.overrepresentedOnly);
    }

    public boolean isOverrepresentedOnly()
    {
        return overrepresentedOnly;
    }

    public DataElementPath getOutProfile()
    {
        return outProfile;
    }

    public void setOutProfile(DataElementPath outputProfile)
    {
        Object oldValue = this.outProfile;
        this.outProfile = outputProfile;
        firePropertyChange( "outProfile", oldValue, outputProfile );
    }
}
