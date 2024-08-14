package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.Track;

@SuppressWarnings ( "serial" )
public class SiteSearchSummaryParameters extends AbstractAnalysisParameters
{
    private DataElementPath yesTrackPath, noTrackPath, outputPath;
    private boolean overrepresentedOnly = false;

    public void setYesTrack(Track yesTrack)
    {
        setYesTrackPath(DataElementPath.create(yesTrack));
    }

    public void setNoTrack(Track noTrack)
    {
        setNoTrackPath(DataElementPath.create(noTrack));
    }

    public DataElementPath getYesTrackPath()
    {
        return yesTrackPath;
    }

    public void setYesTrackPath(DataElementPath yesTrackPath)
    {
        DataElementPath oldValue = this.yesTrackPath;
        this.yesTrackPath = yesTrackPath;
        firePropertyChange("yesTrackPath", oldValue, this.yesTrackPath);
    }

    public DataElementPath getNoTrackPath()
    {
        return noTrackPath;
    }

    public void setNoTrackPath(DataElementPath noTrackPath)
    {
        Object oldValue = this.noTrackPath;
        this.noTrackPath = noTrackPath;
        firePropertyChange("noTrackPath", oldValue, this.noTrackPath);
    }

    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, this.outputPath);
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
}
