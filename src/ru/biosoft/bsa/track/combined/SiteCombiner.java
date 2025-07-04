package ru.biosoft.bsa.track.combined;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import ru.biosoft.bsa.Site;

public class SiteCombiner
{
    public static boolean isAcceptable(CombinedTrack track, SiteGroup siteGroup)
    {
        //TODO: use formula to check site groups acceptance
        String type = track.getCondition().getConditionType();
        if( type.equals( CombineCondition.CC_UNION ) )
            return true;
        else if( type.equals( CombineCondition.CC_INTERSECTION ) )
        {
            //fast draft of intersection
            Set<String> paths = StreamEx.of( track.getTrackColorItems() ).map( ci -> ci.getPath().toString() ).toSet();
            for( Site site : siteGroup.getSites() )
            {
                String orig = site.getProperties().getValueAsString( "OriginalTrack" );
                paths.remove( orig );
                if( paths.isEmpty() )
                    return true;
            }
            return false;
        }
        else if( type.equals( CombineCondition.CC_DIFFERENCE ) )
        {
            CombinedItem[] items = track.getTrackColorItems();
            if( items.length > 0 )
            {
                String first = items[0].getPath().toString();
                for ( Site site : siteGroup.getSites() )
                {
                    String orig = site.getProperties().getValueAsString( "OriginalTrack" );
                    if( !first.equals( orig ) )
                        return false;
                }
            }
            return true;
        }
        else if( type.equals( CombineCondition.CC_SYMMETRIC_DIFFERENCE ) )
        {
            CombinedItem[] items = track.getTrackColorItems();
            if( items.length <= 1 )
                return true;
            Set<String> origs = new HashSet<>();
            for ( Site site : siteGroup.getSites() )
            {
                origs.add( site.getProperties().getValueAsString( "OriginalTrack" ) );
            }
            if( origs.size() > 1 )
                return false;
        }
        return true;
    }

    public static List<SiteGroup> getSiteGroups(List<Site> sites)
    {
        List<SiteGroup> groups = new ArrayList<>();

        return groups;
    }

}
