package biouml.plugins.gtrd.analysis.maos;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;

public class SiteReference
{
    private DataElementPath trackPath;
    private String siteId;
    public SiteReference(Track track, String siteId)
    {
        this.trackPath = track.getCompletePath();
        this.siteId = siteId;
    }
    
    public Site getSite() throws Exception
    {
        Track track = trackPath.getDataElement( Track.class );
        DataCollection<Site> dc = track.getAllSites();
        Site site = dc.get( siteId );
        return site;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( siteId == null ) ? 0 : siteId.hashCode() );
        result = prime * result + ( ( trackPath == null ) ? 0 : trackPath.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        SiteReference other = (SiteReference)obj;
        if( siteId == null )
        {
            if( other.siteId != null )
                return false;
        }
        else if( !siteId.equals( other.siteId ) )
            return false;
        if( trackPath == null )
        {
            if( other.trackPath != null )
                return false;
        }
        else if( !trackPath.equals( other.trackPath ) )
            return false;
        return true;
    }
    
}
