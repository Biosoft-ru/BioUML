package biouml.plugins.riboseq.comparison_article.util_data_structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackPointCollection
{
    private static final String CHR_PREFIX = "chr";

    private final Map<String, List<TrackPointInfo>> pointInfoMap = new HashMap<>();

    public void add(TrackPointInfo pointInfo)
    {
        final String chrName = CHR_PREFIX + pointInfo.chrName;
        pointInfoMap.computeIfAbsent(chrName, k -> new ArrayList<>()).add(pointInfo);
    }

    public List<GeneralPointInfo> getPointList(String chrName, boolean strandPlus)
    {
        final List<GeneralPointInfo> pointList = new ArrayList<>();

        final List<TrackPointInfo> pointInfoList = pointInfoMap.get( chrName );
        for( TrackPointInfo pointInfo : pointInfoList )
        {
            final boolean pointStrandPlus = pointInfo.strandPlus;
            if( pointStrandPlus == strandPlus )
            {
                final GeneralPointInfo generalPointInfo = new GeneralPointInfo( pointInfo.point, chrName, pointStrandPlus, pointInfo.clusterLength );

                pointList.add( generalPointInfo );
            }
        }

        return pointList;
    }

    public List<String> getChrList()
    {
        return new ArrayList<>( pointInfoMap.keySet() );
    }
}
