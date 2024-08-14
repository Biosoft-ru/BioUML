package ru.biosoft.bsa.track.combined;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.GenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceFactory;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.access.SequencesDatabaseInfoSelector;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.HashMapWeakValues;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * CombinedTrack dynamically combines sites from several tracks and displays their combination - <code>SiteGroup</code>.
 * 
 * The main ideas are following:
 * 
 * <li> The track combines only visible in genome browser sites from combined tracks. 
 * It allows dynamically recalculate visualised site set for relatively small region.</li>
 * 
 * <li> Combined sites are merged into one bigger site - SiteGroup that contains all combined sites.
 * This information is used by <code>CombinedTrackViewBuilder</code> for <code>SiteGroup</code> structure visualisation.</li>
 * 
 * <li> Sites (from all combined tracks) are combined into SiteGroup if they overlap 
 * or distance between them less then <code>siteDistance</code> parameter. 
 * When this condition is false, then new <code>SiteGroup</code> is created.
 * <code>SiteGroup</code> can include only 1 site. </li>
 * 
 * <li> If all sites in combined tracks are sorted by their coordinates 
 * then we can use the algorithm like merge sort to quickly merge sites into one <code>SiteGroup</code>.</li>
 * 
 * <li> Otherwise we can create one list with all visible sites on combined tracks,
 * sort them by <code>from</code> coordinate and then create corresponding SiteGroups. </li>     
 */
public class CombinedTrack extends DataElementSupport implements Track
{
    private CombinedTrackViewBuilder viewBuilder;
    private CombinedItem[] trackColorItems;
    private String name;
    private CombineCondition condition;
    private GenomeSelector genomeSelector;

    private final StaticDescriptor TRACK_DESCRIPTOR = StaticDescriptor.create( "OriginalTrack" );
    /**
     * Map with weak values to cache previous results. 
     */
    private Map<String, DataCollection<Site>> cache = new HashMapWeakValues();

    /**
     */
    public CombinedTrack(String name, DataCollection origin, List<Track> tracks, DataElementPath sequencePath)
    {
        super( name, origin );
        this.name = name;
        viewBuilder = new CombinedTrackViewBuilder();
        condition = new CombineCondition();
        if( tracks != null )
        {
            List<CombinedItem> items = new ArrayList<>();
            for( int i = 0; i < tracks.size(); i++ )
            {
                items.add( new CombinedItem( tracks.get( i ).getCompletePath(), ColorUtils.getDefaultColor( i ) ) );
            }
            trackColorItems = items.toArray( new CombinedItem[0] );
            condition.setFormula( getDefaultFomula() );
            condition.addPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    if( evt.getPropertyName().equals( "conditionType" ) )
                    {
                        ( (CombineCondition)evt.getSource() ).setFormula( getDefaultFomula() );
                    }
                }
            } );
        }
        genomeSelector = new GenomeSelector();
        if( sequencePath != null )
        {
            genomeSelector.setDbSelector( SequencesDatabaseInfoSelector.getDatabase( sequencePath ) );
            genomeSelector.setSequenceCollectionPath( sequencePath );
        }
    }

    public CombinedTrack(DataCollection origin, String name)
    {
        this( name, origin, null, null );
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        if( trackColorItems != null )
        {
            for( CombinedItem item : trackColorItems )
            {
                if( item.getPath() == null )
                    continue;
                Track track = item.getPath().getDataElement( Track.class );
                Site s = track.getSite( sequence, siteName, from, to );
                if( s != null )
                {
                    DynamicProperty trackProp = new DynamicProperty( TRACK_DESCRIPTOR, String.class, track.getCompletePath().toString() );
                    s.getProperties().add( trackProp );
                    return s;
                }
            }
        }
        return null;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        String key = sequence + ":" + from + "-" + to;
    	if( cache.containsKey(key) )
    		return cache.get(key);

        Sequence seq = SequenceFactory.getSequence( sequence );

        DataCollection<Site> allSitesDC = new VectorDataCollection<>( "siteGroups", Site.class, null );
        List<SiteGroup> groups = new ArrayList<>();
        List<Site> allSites = new ArrayList<>();
        for( CombinedItem item : trackColorItems )
        {
            if( item.getPath() == null )
                continue;
            Track track = item.getPath().getDataElement( Track.class );
            DynamicProperty trackProp = new DynamicProperty( TRACK_DESCRIPTOR, String.class, track.getCompletePath().toString() );
            for( Site site : track.getSites( sequence, from, to ) )
            {
                site.getProperties().add( trackProp );
                allSites.add( site );
            }
        }
        int i = 1;
        if( !allSites.isEmpty() )
        {
            allSites.sort( Comparator.comparingInt( Site::getFrom ) );
            Iterator<Site> iter = allSites.iterator();
            Site site = iter.next();
            int end = site.getTo();
            List<Site> sgsites = new ArrayList<>();
            sgsites.add( site );
            int dist = condition.getDistance();
            while( iter.hasNext() )
            {
                site = iter.next();
                if( site.getFrom() - end - 1 < dist )
                {
                    sgsites.add( site );
                    end = Math.max( end, site.getTo() );
                }
                else
                {
                    SiteGroup sg = new SiteGroup( allSitesDC, "sg" + i, seq, sgsites, end );
                    groups.add( sg );
                    sgsites = new ArrayList<>();
                    sgsites.add( site );
                    end = site.getTo();
                    i++;
                }
            }
            if( !sgsites.isEmpty() )
            {
                SiteGroup sg = new SiteGroup( allSitesDC, "sg" + i, seq, sgsites, end );
                groups.add( sg );
            }
        }
        for( SiteGroup sg : groups )
        {
            if( SiteCombiner.isAcceptable( this, sg ) )
            {
                allSitesDC.put( sg );
            }
        }
        //TODO: cache all site groups not depending on display
        cache.put( sequence + ":" + from + "-" + to, allSitesDC );
        return allSitesDC;
    }
    
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    public static CombinedTrack createTrack(DataElementPath path, List<Track> tracks, DataElementPath seqCollectionPath)
    {
        path.remove();
        CombinedTrack result = new CombinedTrack( path.getName(), path.optParentCollection(), tracks, seqCollectionPath );
        return result;
    }

    @PropertyName ( "Tracks" )
    @PropertyDescription ( "Tracks to combine" )
    public CombinedItem[] getTrackColorItems()
    {
        return trackColorItems;
    }

    public void setTrackColorItems(CombinedItem[] items)
    {
        trackColorItems = items;
        if( trackColorItems != null )
        {
            condition.setFormula( getDefaultFomula() );
        }
    }


    @PropertyName ( "Condition" )
    @PropertyDescription ( "Rules to manage sites from various track" )
    public CombineCondition getCondition()
    {
        return condition;
    }
    public void setCondition(CombineCondition condition)
    {
        this.condition = condition;
        condition.setFormula( getDefaultFomula() );
        condition.addPropertyChangeListener( new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if( evt.getPropertyName().equals( "conditionType" ) )
                {
                    ( (CombineCondition)evt.getSource() ).setFormula( getDefaultFomula() );
                }
            }
        } );
    }

    @PropertyName ( "Genome" )
    @PropertyDescription ( "Genome (sequences collection)" )
    public GenomeSelector getGenomeSelector()
    {
        return genomeSelector;
    }

    public void setGenomeSelector(GenomeSelector genomeSelector)
    {
        if( genomeSelector != this.genomeSelector )
        {
            //TODO: update when genome changes?
        }
        this.genomeSelector = genomeSelector;
    }

    private String getDefaultFomula()
    {
        if( trackColorItems == null || trackColorItems.length == 0 )
            return null;
        String type = getCondition().getConditionType();
        String joiner = type.equals( "union" ) ? " or " : " and ";
        String formula = "1";

        for( int i = 2; i <= trackColorItems.length; i++ )
        {
            formula += joiner + i;
        }
        return formula;
    }

}
