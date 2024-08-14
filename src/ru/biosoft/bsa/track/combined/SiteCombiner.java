package ru.biosoft.bsa.track.combined;

import java.util.ArrayList;
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
        if( type.equals( "union" ) )
            return true;
        else if( type.equals( "intersection" ) )
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
        return true;
    }

    public static List<SiteGroup> getSiteGroups(List<Site> sites)
    {
        List<SiteGroup> groups = new ArrayList<>();

        return groups;
    }

}
