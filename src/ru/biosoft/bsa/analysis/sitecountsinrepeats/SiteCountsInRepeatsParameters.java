
package ru.biosoft.bsa.analysis.sitecountsinrepeats;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.SqlTrack;

/**
 * @author yura
 *
 */
public class SiteCountsInRepeatsParameters extends AbstractAnalysisParameters
{
    private DataElementPath siteTrackPath;
    private DataElementPath siteCountsInRepeatsTable;

    public DataElementPath getSiteCountsInRepeatsTable()
    {
        return siteCountsInRepeatsTable;
    }

    public void setSiteCountsInRepeatsTable(DataElementPath siteCountsInRepeatsTable)
    {
        Object oldValue = this.siteCountsInRepeatsTable;
        this.siteCountsInRepeatsTable = siteCountsInRepeatsTable;
        firePropertyChange("siteCountsInRepeatsTable", oldValue, siteCountsInRepeatsTable);
    }

    public DataElementPath getSummaryTablePath()
    {
        return DataElementPath.create(siteCountsInRepeatsTable.optParentCollection(), siteCountsInRepeatsTable.getName() + " summary");
    }
    public DataElementPath getSiteTrackPath()
    {
        return siteTrackPath;
    }

    public void setSiteTrackPath(DataElementPath siteTrackPath)
    {
        Object oldValue = this.siteTrackPath;
        this.siteTrackPath = siteTrackPath;
        firePropertyChange("siteTrackPath", oldValue, siteTrackPath);
    }

    public SqlTrack getSiteTrack()
    {
        return siteTrackPath == null ? null : (SqlTrack)siteTrackPath.optDataElement();
    }
}
