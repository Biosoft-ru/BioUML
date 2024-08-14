package biouml.plugins.ensembl.analysis;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import biouml.plugins.ensembl.analysis.SiteData.Location;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class ViewSiteAggregator extends SiteAggregator
{
    private static Pen trackPen = new Pen(1, Color.BLUE);
    private static Brush exonBrush = new Brush(Color.BLUE);
    private static Pen sitePen = new Pen(1, Color.RED);
    
    @Override
    public String getName()
    {
        return "Schematic";
    }
    
    private static class RegionInfo
    {
        SiteData.Location location;
        int start;
        int length;
        double width;

        public RegionInfo(Location location, int start, int length)
        {
            super();
            this.location = location;
            this.start = start;
            this.length = length;
        }
    }

    @Override
    public Object aggregate(Site gene, int fivePrimeSize, int threePrimeSize, List<SiteData> siteData)
    {
        if(siteData == null || siteData.isEmpty()) return new CompositeView();
        List<RegionInfo> regions = new ArrayList<>();
        if(fivePrimeSize > 0) regions.add(new RegionInfo(Location.FIVE_PRIME, -fivePrimeSize, fivePrimeSize));
        Object exonsObj = gene.getProperties().getValue("exons");
        if(exonsObj != null)
        {
            int lastPoint = 0;
            for( String blockStr : TextUtil.split( exonsObj.toString(), ';' ) )
            {
                Interval interval;
                try
                {
                    interval = new Interval(blockStr);
                }
                catch( Exception e )
                {
                    continue;
                }
                if(interval.getFrom() > lastPoint)
                    regions.add(new RegionInfo(Location.INTRON, lastPoint, interval.getFrom()-lastPoint));
                regions.add(new RegionInfo(Location.EXON, interval.getFrom(), interval.getTo()-interval.getFrom()+1));
                lastPoint = interval.getTo()+1;
            }
            if(gene.getLength() > lastPoint)
                regions.add(new RegionInfo(Location.INTRON, lastPoint, gene.getLength()-lastPoint));
        } else
        {
            regions.add(new RegionInfo(Location.GENE, 0, gene.getLength()));
        }
        if(threePrimeSize > 0) regions.add(new RegionInfo(Location.THREE_PRIME, gene.getLength(), threePrimeSize));
        double sumLog = 0;
        for(RegionInfo region: regions)
        {
            sumLog+=Math.log(region.length)+1;
        }
        int totalLength = 200;
        CompositeView result = new CompositeView();
        double basePos = 0;
        for(RegionInfo region: regions)
        {
            region.width = (Math.log(region.length)+1)/sumLog*totalLength;
            if(region.location == Location.EXON || region.location == Location.GENE)
                result.add(new BoxView(null, exonBrush, (int)basePos, 0, (int)region.width+1, 10));
            else if(region.location == Location.INTRON)
            {
                result.add(new LineView(trackPen, (int)basePos, 5, (int)(basePos+region.width/2), 2));
                result.add(new LineView(trackPen, (int)(basePos+region.width/2), 2, (int)(basePos+region.width), 5));
            } else
            {
                result.add(new LineView(trackPen, (int)basePos, 5, (int)(basePos+region.width), 5));
            }
            basePos += region.width;
        }
        // TSS arrow
        /*result.add(new LineView(trackPen, 0, 7, totalLength, 7));
        if(regions.size() > 0 && regions.get(0).location == Location.FIVE_PRIME)
        {
            basePos = regions.get(0).width;
            result.add(new LineView(trackPen, (int)basePos, 7, (int) basePos, 2));
            result.add(new LineView(trackPen, (int)basePos, 2, (int) basePos+7, 2));
            result.add(new LineView(trackPen, (int)basePos+7, 2, (int) basePos+5, 0));
            result.add(new LineView(trackPen, (int)basePos+7, 2, (int) basePos+5, 4));
        }*/
        for(SiteData site: siteData)
        {
            basePos = 0;
            for(RegionInfo region: regions)
            {
                if(region.start <= site.getOffset() && region.start+region.length > site.getOffset())
                {
                    int pos = (int)(basePos+(site.getOffset()-region.start)*region.width/region.length);
                    result.add(new LineView(sitePen, pos, 10, pos, 18));
                    break;
                }
                basePos += region.width;
            }
        }
        return result;
    }

    @Override
    public Class<?> getType()
    {
        return CompositeView.class;
    }
}
