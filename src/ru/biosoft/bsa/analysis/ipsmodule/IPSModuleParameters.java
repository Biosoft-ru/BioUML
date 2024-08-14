package ru.biosoft.bsa.analysis.ipsmodule;

import java.sql.Connection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

@SuppressWarnings ( "serial" )
public class IPSModuleParameters extends AbstractAnalysisParameters
{
    private DataElementPath siteTrackPath;
    private int windowSize = 500;
    private int minSites = 10;
    private double minAverageScore = 5;
    private String[] siteModels = new String[0];
    private DataElementPath moduleTrack;
    
    public DataElementPath getModuleTrack()
    {
        return moduleTrack;
    }

    public void setModuleTrack(DataElementPath moduleTrack)
    {
        Object oldValue = this.moduleTrack;
        this.moduleTrack = moduleTrack;
        firePropertyChange("moduleTrack", oldValue, moduleTrack);
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
        setSiteModels( getAvailableSiteModels() );
    }
    
    public SqlTrack getSiteTrack()
    {
        return siteTrackPath==null?null:(SqlTrack)siteTrackPath.optDataElement();
    }

    public int getWindowSize()
    {
        return windowSize;
    }
    public void setWindowSize(int windowSize)
    {
        Object oldValue = this.windowSize;
        this.windowSize = windowSize;
        firePropertyChange("windowSize", oldValue, windowSize);
    }

    public int getMinSites()
    {
        return minSites;
    }
    public void setMinSites(int minSites)
    {
        Object oldValue = this.minSites;
        this.minSites = minSites;
        firePropertyChange("minSites", oldValue, minSites);
    }

    public double getMinAverageScore()
    {
        return minAverageScore;
    }
    public void setMinAverageScore(double minAverageScore)
    {
        Object oldValue = this.minAverageScore;
        this.minAverageScore = minAverageScore;
        firePropertyChange("minAverageScore", oldValue, minAverageScore);
    }

    public String[] getSiteModels()
    {
        return siteModels;
    }
    public void setSiteModels(String[] siteModels)
    {
        Object oldValue = this.siteModels;
        this.siteModels = siteModels;
        firePropertyChange("siteModels", oldValue, siteModels);
    }
    
    protected String[] getAvailableSiteModels()
    {
        try
        {
            DataElementPath siteTrackPath = getSiteTrackPath();
            if( siteTrackPath == null )
                return new String[0];
            SqlTrack track = siteTrackPath.getDataElement(SqlTrack.class);
            Connection con = track.getConnection();
            return SqlUtil.stringStream( con, "SELECT distinct prop_" + SiteModel.SITE_MODEL_PROPERTY + " FROM " + track.getTableId() )
                    .toArray( String[]::new );
        }
        catch( BiosoftSQLException | RepositoryException e )
        {
            return new String[0];
        }
    }
    
    public static class SiteModelsSelector extends GenericMultiSelectEditor
    {

        @Override
        protected Object[] getAvailableValues()
        {
            return ((IPSModuleParameters)getBean()).getAvailableSiteModels();
        }
    }
}
