package biouml.plugins.riboseq.datastructure;

import biouml.plugins.riboseq.util.SiteUtil;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;

import java.util.ArrayList;
import java.util.List;

public class IntronSiteCluster extends SiteCluster
{
    private CenterArea centerArea;
    private List<Interval> exonIntervalList;

    public IntronSiteCluster(Site site)
    {
        super(site);
        centerArea = new CenterArea( site );
        exonIntervalList = SiteUtil.getExonsFromCigar( site );
    }

    @Override
    public void addSite(Site site)
    {
        super.addSite( site );

        final CenterArea siteCenterArea = new CenterArea(site);
        centerArea.union( siteCenterArea );

        final List<Interval> siteExons = SiteUtil.getExonsFromCigar(site);
        exonIntervalList = SiteUtil.unionExons( exonIntervalList, siteExons );
    }

    @Override
    public boolean isIntersects(Site site)
    {
        final CenterArea siteCenterArea = new CenterArea( site );

        return centerArea.intersects( siteCenterArea );
    }

    @Override
    protected int computeInitCodonPosition(int absoluteStartModa)
    {
        final int initCodonPosition;

        if( !centerArea.hasIntron() )
        {
            initCodonPosition = super.computeInitCodonPosition( absoluteStartModa );
        }
        else
        {
            initCodonPosition = computeInitCodonPositionWithIntron( absoluteStartModa );
        }

        return initCodonPosition;
    }

    private int computeInitCodonPositionWithIntron(int absoluteStartModa)
    {
        int initCodonPosition;
        int sumOffset = 0;

        final int directionSign = isReversed() ? -1 : 1;
        final List<Site> startModaSiteList = super.getStartModaSiteList( absoluteStartModa );
        for( Site site : startModaSiteList )
        {
            if( !SiteUtil.isSiteContainsIntron( site ) )
            {
                sumOffset += directionSign * site.getLength() / 2;
            }
            else
            {
                sumOffset += CenterArea.getCenterWithIntron( site ) - site.getStart();
            }
        }

        final int offset = sumOffset / startModaSiteList.size();
        initCodonPosition = absoluteStartModa + offset;

        return initCodonPosition;
    }

    @Override
    public List<Interval> getExons()
    {
        final ArrayList<Interval> shiftedExonIntervalList = new ArrayList<>();

        final Interval startExon = exonIntervalList.get( 0 );
        final int shiftDistance = startExon.getFrom();

        for( Interval exonInterval : exonIntervalList )
        {
            final Interval shiftedExonInterval = exonInterval.shift( -shiftDistance );
            shiftedExonIntervalList.add( shiftedExonInterval );
        }

        return shiftedExonIntervalList;
    }
}
