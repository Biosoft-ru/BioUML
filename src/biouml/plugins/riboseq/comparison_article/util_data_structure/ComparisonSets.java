package biouml.plugins.riboseq.comparison_article.util_data_structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComparisonSets
{
    private Set<GeneralPointInfo> intersectionArticleSet = new HashSet<>();
    private Set<GeneralPointInfo> intersectionTrackSet = new HashSet<>();

    private Set<GeneralPointInfo> uniqueArticleSet = new HashSet<>();
    private Set<GeneralPointInfo> uniqueTrackSet = new HashSet<>();

    public void addToIntersection(GeneralPointInfo articlePoint, GeneralPointInfo trackPoint)
    {
        intersectionArticleSet.add( articlePoint );
        intersectionTrackSet.add( trackPoint );
    }

    public int getIntersectionArticlePointNumber()
    {
        return intersectionArticleSet.size();
    }

    public int getIntersectionTrackPointNumber()
    {
        return intersectionTrackSet.size();
    }

    public int getUniqueArticlePointNumber()
    {
        return uniqueArticleSet.size();
    }

    public int getUniqueTrackPointNumber()
    {
        return uniqueTrackSet.size();
    }

    public void summarize(List<GeneralPointInfo> articlePointList, List<GeneralPointInfo> trackPointList)
    {
        final List<GeneralPointInfo> articlePointRemainingList = new ArrayList<>( articlePointList );

        articlePointRemainingList.removeAll( intersectionArticleSet );
        uniqueArticleSet.addAll( articlePointRemainingList );

        final List<GeneralPointInfo> trackPointRemainingList = new ArrayList<>( trackPointList );

        trackPointRemainingList.removeAll( intersectionTrackSet );
        uniqueTrackSet.addAll( trackPointRemainingList );
    }

    public ArrayList<GeneralPointInfo> getUniqueArticlePointList() {
        return new ArrayList<>( uniqueArticleSet );
    }
}
