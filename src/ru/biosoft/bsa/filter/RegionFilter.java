package ru.biosoft.bsa.filter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.filter.MutableFilter;
import ru.biosoft.access.core.filter.QueryFilter;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.BTreeIndex.IntKey;
import ru.biosoft.access.core.Index.StringIndexEntry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.transformer.SiteQuerySystem;

import com.developmentontheedge.beans.Option;


/**
 * Class containing and describing a part of large Map.
 * Used for optimization of creating map views.
 */
@SuppressWarnings ( "serial" )
public class RegionFilter extends MutableFilter<Site> implements QueryFilter<Site>
{
    protected static final Logger cat = Logger.getLogger(RegionFilter.class.getName());

    //for serialization
    public RegionFilter(String mapName)
    {
        this(null, DataElementPath.create(mapName).getDataElement(AnnotatedSequence.class));
    }

    /**
     * Creates <code>RegionFilter</code> for the specified map
     * with default options (filter is enabled, a region is equal to all map).
     *
     * @param parent a parent to be notified about filter properties changing
     * @param map map whose region a filter is controlled.
     */
    public RegionFilter(Option parent, AnnotatedSequence map)
    {
        super(parent);
        mapName     = map.getCompletePath().toString();
        enabled     = true;

        totalLength      = map.getSequence().getLength();
        totalSiteNumber  = map.getSize();

        regionFrom  = 1;
        regionTo    = totalLength;
    }

    /**
     * Creates <code>RegionFilter</code> for the specified map.
     * If map sites number more then specified, then region length will be
     * defined to contains not more then specified site number.
     *
     * @param parent a parent to be notified about filter properties changing
     * @param map map whose region a filter is controlled.
     */
    public RegionFilter(Option parent, AnnotatedSequence map, int maxSiteNumber)
    {
        this(parent, map);
        if (totalSiteNumber > maxSiteNumber)
        {
            // we use long because totalLength * maxSiteNumber can produce
            // integer to overflow
            int rl = (int) ( ((long)totalLength * maxSiteNumber) / totalSiteNumber );
            regionTo = (rl)/10*10;
        }
    }


    /** Map complete name */
    protected String mapName;
    public String getMapName()
    {
        return mapName;
    }

    //////////////////////////////////////////////
    // Filter method
    //
    @Override
    public List<String> doQuery( DataCollection siteSet )
    {
        QuerySystem querySystem = null;

        if( siteSet!=null )
        {
            DataCollectionInfo info = siteSet.getInfo();
            if( info!=null )
            {
                querySystem = info.getQuerySystem();
            }
        }

        if( querySystem ==null )
            return null;

        List<String> sites = new LinkedList<>();
        Index fromIndex = querySystem.getIndex( SiteQuerySystem.FROM );
        if( fromIndex==null )
        {
            cat.log(Level.SEVERE, "RegionFilter: index 'from' not found.");
            return null;
        }

        Index toIndex = querySystem.getIndex( SiteQuerySystem.TO );
        if( toIndex==null )
        {
            cat.log(Level.SEVERE, "RegionFilter: index 'to' not found.");
            return null;
        }

        Iterator<Entry<String, StringIndexEntry>> iter = toIndex.nodeIterator( new IntKey(regionFrom) );
        while( iter.hasNext() )
        {
            Entry<String, StringIndexEntry> entry = iter.next();
            StringIndexEntry indexEntry = entry.getValue();
            String name = indexEntry.value;
            int to   = Integer.parseInt(entry.getKey());
            if( to > regionTo )
                break;
            try
            {
                Site site = (Site)siteSet.get(name);
                if( site.getFrom()<regionFrom )
                    if( isAcceptable(site) )
                        sites.add( name );
            }
            catch( Throwable t )
            {
                cat.log(Level.WARNING, "RegionFilter: error in using 'from' indexes.", t);
            }
        }

        iter = fromIndex.nodeIterator( new IntKey(regionFrom) );
        while( iter.hasNext() )
        {
            Entry<String, StringIndexEntry> entry = iter.next();
            StringIndexEntry indexEntry = entry.getValue();
            String name = indexEntry.value;
            int from = Integer.parseInt(entry.getKey());
            if( from > regionTo )
                break;
            try
            {
                Site site = (Site)siteSet.get(name);
                if( isAcceptable(site) )
                    sites.add( name );
            }
            catch( Throwable t )
            {
                cat.log(Level.WARNING, "RegionFilter: error 'to' in using indexes.", t);
            }
        }

        return sites;
    }

    @Override
    public boolean isAcceptable(Site site)
    {
        if (!isEnabled())
            return false;

        return !(site.getTo()<regionFrom || site.getFrom()>regionTo);
    }

    public boolean hasEffect()
    {
        return (regionTo-regionFrom+1) != totalLength;
    }

    //////////////////////////////////////////////
    //  Properties
    //

    /** Should this map be dispalyed */
    protected boolean enabled;
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    /** Total length of the map */
    protected int totalLength;
    public int getTotalLength()
    {
        return totalLength;
    }

    /** Total number of sites in the map */
    protected int totalSiteNumber;
    public int getTotalSiteNumber()
    {
        return totalSiteNumber;
    }

    private int regionFrom = 1;
    public int getRegionFrom()
    {
        return regionFrom;
    }
    public void setRegionFrom( int regionFrom )
    {
        int oldRegionFrom = this.regionFrom;
        if( regionFrom > regionTo )
            regionFrom = regionTo;
        if( regionFrom < 1 )
            regionFrom = 1;
        if(regionFrom != oldRegionFrom)
        {
            this.regionFrom = regionFrom;
            firePropertyChange("regionFrom", oldRegionFrom, regionFrom);
            firePropertyChange("regionSiteNumber", null, null);
        }
    }

    private int regionTo = 1;
    public int getRegionTo()
    {
        return regionTo;
    }
    public void setRegionTo( int regionTo )
    {
        int oldRegionTo = this.regionTo;
        if( regionTo > totalLength )
            regionTo = totalLength;
        if( regionTo < regionFrom )
            regionTo = regionFrom;
        if( regionTo!=oldRegionTo )
        {
            this.regionTo = regionTo;
            firePropertyChange("regionTo", oldRegionTo, regionTo);
            firePropertyChange("regionSiteNumber", null, null);
        }
    }

    /** @return aproximate site number in the region. */
    public int getRegionSiteNumber()
    {
        float sn = (regionTo-regionFrom) * ((float)totalSiteNumber / (float)totalLength);
        return Math.min((int)sn, totalSiteNumber);
    }
}