package biouml.plugins.ensembl.tracks;

import java.awt.Color;
import java.awt.Graphics;

import biouml.plugins.ensembl.tracks.TranscriptsTrack.Translation;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;

public class TranscriptsTrackViewBuilder extends TrackViewBuilder
{

    private static int getPointXOrBound(int x, int start, int end, double nucleotideWidth)
    {
        if( x < start )
            return 0;
        if( x > end )
            return (int) ( ( end - start + 1 ) * nucleotideWidth );
        return (int) ( ( x - start ) * nucleotideWidth );
    }

    @Override
    protected View createSiteView(SequenceView sequenceView, Site site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int sitesCount)
    {
        CompositeView result = new CompositeView();
        if( site == null || site.getFrom() > end || site.getTo() < start )
            return null;
        
        double nucleotideWidth = sequenceView.getNucleotideWidth();
        int fromPoint = getPointXOrBound( site.getFrom(), start, end, nucleotideWidth  );
        int toPoint = getPointXOrBound( site.getTo(), start, end, nucleotideWidth  );
        
        Pen linePen = new Pen(1, Color.BLACK);
        result.add(new LineView(linePen, fromPoint, 0, toPoint, 0) );
        Interval[] exons = (Interval[])site.getProperties().getValue( "exons" );
        for(Interval exon : exons)
        {
            if(exon.getFrom() > end || exon.getTo() < start)
                continue;
            fromPoint = getPointXOrBound( exon.getFrom(), start, end, nucleotideWidth );
            toPoint = getPointXOrBound( exon.getTo(), start, end, nucleotideWidth );
            result.add( createBoxView( fromPoint, toPoint, Color.BLUE ) );
        }
        
        Translation translation = (Translation)site.getProperties().getValue( "translation" );
        if( translation != null )
        {
                for( int i = translation.firstExonRank; i <= translation.lastExonRank; i++ )
                {
                    int from = exons[i - 1].getFrom();
                    int to = exons[i - 1].getTo();
                    if( i == translation.firstExonRank )
                    {
                        int delta = translation.offsetInFirstExon - 1;
                        if( site.getStrand() == StrandType.STRAND_MINUS )
                            to -= delta;
                        else
                            from += delta;
                    }
                    if( i == translation.lastExonRank )
                    {
                        int delta = translation.offsetInLastExon - 1;
                        if( site.getStrand() == StrandType.STRAND_MINUS )
                            from = exons[i - 1].getTo() - delta;
                        else
                            to = exons[i - 1].getFrom() + delta;
                    }
                    fromPoint = getPointXOrBound( from, start, end, nucleotideWidth );
                    toPoint = getPointXOrBound( to, start, end, nucleotideWidth );
                    result.add( createBoxView( fromPoint, toPoint, Color.RED ) );
                }
        }
        result.setModel(site);
        result.setActive(true);
        return result;
    }
    
    private View createBoxView(int x0, int x1, Color color)
    {
        Pen pen = new Pen( 1, color );
        Brush brush = new Brush( color );
        return new BoxView( pen, brush, x0, 0, x1 - x0 + 1, 5 );
    }
}
