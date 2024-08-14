package ru.biosoft.bsa.track.combined;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.List;
import java.util.Map;

import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;

public class CombinedTrackViewBuilder extends DefaultTrackViewBuilder
{
    private Map<String, Color> colors;
    @Override
    public SiteViewOptions createViewOptions()
    {
        return new CombinedTrackViewOptions();
    }

    @Override
    protected View createSiteView(SequenceView sequenceView, Site site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int sitesCount)
    {
        if( site instanceof SiteGroup )
        {
            return createSiteGroupView( sequenceView, (SiteGroup)site, siteViewOptions, start, end, graphics, sitesCount );
        }
        else
            return super.createSiteView( sequenceView, site, siteViewOptions, start, end, graphics, sitesCount );
    }

    private View createSiteGroupView(SequenceView sequenceView, SiteGroup site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int sitesCount)
    {
        CombinedTrackViewOptions viewOptions = (CombinedTrackViewOptions)siteViewOptions;
        CompositeView cvsg = new CompositeView();

        if( site == null )
        {
            return cvsg;
        }

        int height = siteViewOptions.getTrackDisplayMode().equals( SiteViewOptions.TRACK_MODE_COMPACT ) ? 4
                : (int)siteViewOptions.getBoxHeight();

        List<Site> sites = site.getSites();
        int nextYShift = 0;
        for( Site s : sites )
        {
            int sx = s.getFrom();
            int ex = s.getTo();
            if( sx > end || ex < start )
                continue;

            int arrowStartS = 1;
            int arrowEndS = 1;

            if( sx < start )
            {
                sx = start;
                arrowStartS = 0;
            }
            if( ex > end )
            {
                ex = end;
                arrowEndS = 0;
            }
            sx -= start - 1;
            ex -= start - 1;

            Point startPointS = sequenceView.getStartPoint( sx, graphics );
            Point endPointS = sequenceView.getEndPoint( ex, graphics );
            if( sx == ex )
            {
                if( arrowStartS == 0 )
                    endPointS.translate( +1, 0 );
                else
                    startPointS.translate( -1, 0 );
            }
            if( startPointS.x > endPointS.x )
            {
                startPointS.x -= sequenceView.getNucleotideWidth() / 2;
                endPointS.x += sequenceView.getNucleotideWidth() / 2;
            }
            startPointS.y += nextYShift;

            CompositeView cv = new CompositeView();
            Brush brush = viewOptions.getColorScheme().getBrush( s );
            if( viewOptions.getVisualStyle().equals( CombinedTrackViewOptions.COLOR_OVERLAY ) )
            {
                Color c = ( (Color)brush.getPaint() );
                brush = new Brush( new Color( c.getRed(), c.getGreen(), c.getBlue(), 0x80 ) );
            }

            Pen pen = null;//new Pen(1, (Color)brush.getPaint());
            View siteView = new BoxView( pen, brush, startPointS.x, startPointS.y, endPointS.x - startPointS.x,
                    siteViewOptions.getBoxHeight() );

            if( siteViewOptions.isShowBox() )
            {
                siteView = new BoxView( pen, brush, startPointS.x, startPointS.y, endPointS.x - startPointS.x,
                        siteViewOptions.getBoxHeight() );
            }
            else
            {
                int strand = siteViewOptions.isShowStrand() && endPointS.x - startPointS.x > 1 ? s.getStrand()
                        : Site.STRAND_NOT_APPLICABLE;
                switch( strand )
                {
                    case Site.STRAND_PLUS:
                        siteView = createArrow( height, pen, brush, startPointS, endPointS, 0, arrowEndS );
                        break;
                    case Site.STRAND_MINUS:
                        siteView = createArrow( height, pen, brush, endPointS, startPointS, 0, arrowStartS );
                        break;
                    case Site.STRAND_BOTH:
                        siteView = createArrow( height, pen, brush, startPointS, endPointS, arrowStartS, arrowEndS );
                        break;
                    default:
                        siteView = createArrow( height, pen, brush, startPointS, endPointS, 0, 0 );
                        break;
                }
            }

            cv.add( siteView );

            if( viewOptions.getVisualStyle().equals( CombinedTrackViewOptions.COLOR_SHIFT ) )
                nextYShift += height;

            String siteTitle = s.getName();
            if( siteTitle == null || siteTitle.equals( "" ) )
            {
                siteTitle = s.getType();
            }

            cv.setDescription( siteTitle );
            cv.setModel( s );
            cv.setActive( true );
            cvsg.add( cv );

        }

        //cvsg.setModel( site );
        //cvsg.setActive( true );

        return cvsg;
    }

    public Map<String, Color> getColors()
    {
        return colors;
    }
}