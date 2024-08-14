package ru.biosoft.bsa.view;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MessageBundle;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.TextUtil;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class DefaultTrackViewBuilder extends TrackViewBuilder
{
    protected List<Interval> getSiteStructure(Site site)
    {
        String exons = site.getProperties().getValueAsString("exons");
        if(exons != null)
        {
            List<Interval> result = new ArrayList<>();
            for(String exon: TextUtil.split(exons, ';'))
            {
                try
                {
                    result.add(new Interval(exon));
                }
                catch( IllegalArgumentException e )
                {
                }
            }
            return result;
        }
        String blockSizes = site.getProperties().getValueAsString("blockSizes");
        String blockStarts = site.getProperties().getValueAsString("blockStarts");
        if(blockSizes != null && blockStarts != null)
        {
            String[] starts = TextUtil.split(blockStarts, ',');
            String[] sizes = TextUtil.split(blockSizes, ',');
            int count = Math.min(starts.length, sizes.length);
            List<Interval> result = new ArrayList<>();
            for(int i=0; i<count; i++)
            {
                try
                {
                    int start = Integer.parseInt(starts[i]);
                    int size = Integer.parseInt(sizes[i]);
                    result.add(new Interval(start, start+size-1));
                }
                catch( NumberFormatException e )
                {
                }
            }
            return result;
        }
        return null;
    }
    
    @Override
    protected View createSiteView(SequenceView sequenceView, Site site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int sitesCount)
    {
        CompositeView cv = new CompositeView();

        if( site == null )
        {
            return cv;
        }

        // evaluate coordinates and name
        int startX = site.getFrom();
        int endX = site.getTo();
        if( startX > end || endX < start )
        {
            return null;
        }

        int arrowStart = 1;
        int arrowEnd = 1;
        boolean startVisible = true, endVisible = true;

        if( startX < start )
        {
            startX = start;
            arrowStart = 0;
            startVisible = false;
        }
        if( endX > end )
        {
            endX = end;
            arrowEnd = 0;
            endVisible = false;
        }

        // Beginning drawing sites from 1
        startX -= start - 1;
        endX -= start - 1;

        Point startPoint = sequenceView.getStartPoint(startX, graphics);
        Point endPoint = sequenceView.getEndPoint(endX, graphics);
        if( startX == endX )
        {
            if( arrowStart == 0 )
                endPoint.translate( +1, 0);
            else
                startPoint.translate( -1, 0);
        }
        if(startPoint.x > endPoint.x)
        {
            startPoint.x -= sequenceView.getNucleotideWidth()/2;
            endPoint.x += sequenceView.getNucleotideWidth()/2;
        }

        // draw site
        View siteView = null;

        // @pending high solve the problem with color schemes
        Brush brush = siteViewOptions.getColorScheme().getBrush(site);

        int width = siteViewOptions.getTrackDisplayMode().equals(SiteViewOptions.TRACK_MODE_COMPACT) ? 4 : (int)siteViewOptions
                .getBoxHeight();

        Pen pen = null;//new Pen(1, (Color)brush.getPaint());
        
        if( siteViewOptions.getTrackDisplayMode().equals(SiteViewOptions.TRACK_MODE_COMPACT))
        {
            // Semi-transparent
            if(brush.getPaint() instanceof Color)
            {
                Color c = ((Color)brush.getPaint());
                brush = new Brush(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0x80));
            }
        }
        
        List<Interval> blocks;
        
        if( site.getLength() == 0 )
        {
            CompositeView compositeSiteView = new CompositeView();
            Pen linePen = new Pen(1, (Color)brush.getPaint());
            compositeSiteView.add(new LineView(linePen, startPoint.x, startPoint.y + width / 2, endPoint.x, startPoint.y
                    + width / 2));
            compositeSiteView.add(new LineView(linePen, startPoint.x, startPoint.y - width / 2, endPoint.x, startPoint.y
                    - width / 2));
            compositeSiteView.add(new LineView(linePen, (startPoint.x+endPoint.x)/2.0f, startPoint.y - width / 2.0f, (startPoint.x+endPoint.x)/2.0f, startPoint.y
                    + width / 2.0f));
            siteView = compositeSiteView;
        }
        else if( siteViewOptions.isShowStructure() && endPoint.x - startPoint.x > 20 && ( ( blocks = getSiteStructure(site) ) != null ) )
        {
            CompositeView compositeSiteView = new CompositeView();
            Pen linePen = new Pen(1, Color.BLACK);
            compositeSiteView.add(new LineView(linePen, startPoint.x, startPoint.y + width / 2 - 1, endPoint.x, startPoint.y
                    + width / 2 - 1));

            for( Interval interval : blocks )
            {
                int from = interval.getFrom();
                int to = interval.getTo();
                int startBlock, endBlock;
                Point startBlockPoint, endBlockPoint;
                if( site.getStrand() == Site.STRAND_MINUS )
                {
                    startBlock = site.getStart() - to;
                    endBlock = site.getStart() - from;
                }
                else
                {
                    startBlock = site.getStart() + from;
                    endBlock = site.getStart() + to;
                }
                if( endBlock < start || startBlock > end )
                    continue;
                if( startBlock < start )
                    startBlock = start;
                if( endBlock > end )
                    endBlock = end;
                arrowStart = site.getFrom() == startBlock?1:0;
                arrowEnd = site.getTo() == endBlock?1:0;
                startBlockPoint = sequenceView.getStartPoint(startBlock - start + 1, graphics);
                endBlockPoint = sequenceView.getEndPoint(endBlock - start + 1, graphics);
                if( startBlockPoint.x == endBlockPoint.x )
                {
                    startBlockPoint.translate( -1, 0);
                    endBlockPoint.translate(1, 0);
                }
                View blockView;
                int strand = siteViewOptions.isShowStrand() && endPoint.x - startPoint.x > 2?site.getStrand():Site.STRAND_NOT_APPLICABLE;
                switch( strand )
                {
                    case Site.STRAND_PLUS:
                        blockView = createArrow(width, pen, brush, startBlockPoint, endBlockPoint, 0, arrowEnd);
                        break;
                    case Site.STRAND_MINUS:
                        blockView = createArrow(width, pen, brush, endBlockPoint, startBlockPoint, 0, arrowStart);
                        break;
                    case Site.STRAND_BOTH:
                        blockView = createArrow(width, pen, brush, startBlockPoint, endBlockPoint, arrowStart, arrowEnd);
                        break;
                    default:
                        blockView = createArrow(width, pen, brush, startBlockPoint, endBlockPoint, 0, 0);
                        break;
                }
                compositeSiteView.add(blockView);
            }
            siteView = compositeSiteView;
        }
        else if( siteViewOptions.isShowStructure() && site.getProperties().getValue("profile") instanceof double[] )
        {
            double[] values = (double[])site.getProperties().getValue("profile");
            double max = 0;
            for(int i=0; i<values.length; i++) max = values[i]>max?values[i]:max;
            CompositeView compositeSiteView = new CompositeView();
            Pen linePen = new Pen(1, (Color)brush.getPaint());
            compositeSiteView.add(new LineView(linePen, startPoint.x + 1, startPoint.y + width, endPoint.x, startPoint.y + width - 1));
            if(max == 0) max = 1;
            double scale = ((double)endPoint.x-startPoint.x)/(endX-startX);
            if(scale > 1)
            {
                int lastHeight = (int) ( values[0]/max*siteViewOptions.getBoxHeight() );
                int lastPos = 0;
                for(int i=1; i<=values.length; i++)
                {
                    int height = i==values.length?-1:(int) ( values[i]/max*siteViewOptions.getBoxHeight() );
                    if(height != lastHeight)
                    {
                        int startBlock = lastPos+site.getFrom();
                        int endBlock = i+site.getFrom();
                        if(startBlock < start) startBlock = start;
                        if(startBlock > end) startBlock = end;
                        if(endBlock < start) endBlock = start;
                        if(endBlock > end) endBlock = end;
                        if(startBlock < endBlock && lastHeight > 0)
                        {
                            Point startBlockPoint = sequenceView.getStartPoint(startBlock-start+1, graphics);
                            Point endBlockPoint = sequenceView.getEndPoint((endBlock-1)-start+1, graphics);
                            compositeSiteView.add(new BoxView(null, brush, startBlockPoint.x, startPoint.y + siteViewOptions.getBoxHeight()
                                    - lastHeight, endBlockPoint.x-startBlockPoint.x, lastHeight));
                        }
                        lastHeight = height;
                        lastPos = i;
                    }
                }
            } else
            {
                int lastHeight = -1;
                int lastPos = -1;
                for(int i=startPoint.x; i<=endPoint.x+1; i++)
                {
                    int from = sequenceView.getPosition(i, graphics)+start-1-site.getFrom();
                    int to = sequenceView.getPosition(i+1, graphics)+start-1-site.getFrom();
                    if(from < 0) from = 0;
                    if(to >= values.length) to = values.length;
                    double subMax = 0;
                    for(int j=from; j<to; j++)
                    {
                        subMax = Math.max(subMax, values[j]);
                    }
                    int height = (int) ( subMax/max*siteViewOptions.getBoxHeight() );
                    if(height != lastHeight || i==endPoint.x+1)
                    {
                        if(lastHeight > 0)
                        {
                            compositeSiteView.add(new BoxView(null, brush, lastPos, startPoint.y + siteViewOptions.getBoxHeight()
                                    - lastHeight, i-lastPos, lastHeight));
                        }
                        lastHeight = height;
                        lastPos = i;
                    }
                }
            }
            siteView = compositeSiteView;
        }
        else if( siteViewOptions.isShowBox() )
        {
            siteView = new BoxView(pen, brush, startPoint.x, startPoint.y, endPoint.x - startPoint.x, siteViewOptions.getBoxHeight());
        }
        else
        {
            int strand = siteViewOptions.isShowStrand() && endPoint.x - startPoint.x > 1?site.getStrand():Site.STRAND_NOT_APPLICABLE;
            switch( strand )
            {
                case Site.STRAND_PLUS:
                    siteView = createArrow(width, pen, brush, startPoint, endPoint, 0, arrowEnd);
                    break;
                case Site.STRAND_MINUS:
                    siteView = createArrow(width, pen, brush, endPoint, startPoint, 0, arrowStart);
                    break;
                case Site.STRAND_BOTH:
                    siteView = createArrow(width, pen, brush, startPoint, endPoint, arrowStart, arrowEnd);
                    break;
                default:
                    siteView = createArrow(width, pen, brush, startPoint, endPoint, 0, 0);
                    break;
            }
        }
        
        cv.add(siteView);
        
        if( siteViewOptions.isShowStructure() && site.getProperties().getValue("summit") instanceof Integer )
        {
            Pen linePen = new Pen(1, Color.BLACK);
            Integer summit = (Integer)site.getProperties().getValue("summit");
            Point summitPoint = sequenceView.getStartPoint( site.getFrom() + summit - start + 1, graphics );
            cv.add( new LineView( linePen, summitPoint.x, summitPoint.y-width/2, summitPoint.x, summitPoint.y + width/2 ) );
        }

        String siteTitle = null;
        switch( siteViewOptions.getDisplayName() )
        {
            case SiteViewOptions.DISPLAY_SITE_NAME:
                siteTitle = site.getName();
                break;
            case SiteViewOptions.DISPLAY_SITE_TYPE:
                siteTitle = site.getType();
                break;
            case SiteViewOptions.DISPLAY_SITE_TITLE:
            {
                DynamicPropertySet siteProperties = site.getProperties();
                if( siteProperties != null )
                {
                    DynamicProperty title = siteProperties.getProperty(SiteModel.SITE_MODEL_PROPERTY);
                    if( title == null || title.getValue() == null )
                        title = siteProperties.getProperty("symbol");
                    if( title == null || title.getValue() == null )
                        title = siteProperties.getProperty("gene");
                    if( title == null || title.getValue() == null )
                        title = siteProperties.getProperty("name");
                    if( title != null && title.getValue() != null )
                    {
                        Object value = title.getValue();
                        siteTitle = (value instanceof DataElement)?((DataElement)value).getName():value.toString();
                        
                    }
                }
            }
                break;
            case SiteViewOptions.DISPLAY_SITE_TF_NAME:
            {
                DynamicPropertySet siteProperties = site.getProperties();
                if( siteProperties != null )
                {
                    DynamicProperty matrix = siteProperties.getProperty(SiteModel.SITE_MODEL_PROPERTY);
                    if( matrix != null && matrix.getValue() != null && matrix.getValue() instanceof SiteModel )
                        siteTitle = ( (SiteModel)matrix.getValue() ).getBindingElement().getName();
                }
            }
                break;
            case SiteViewOptions.DISPLAY_SITE_STRAND:
            {
                siteTitle = MessageBundle.strandTypes[site.getStrand()];
            }
            break;
        }
        if( siteTitle == null || siteTitle.equals("") )
        {
            siteTitle = site.getType();
        }
        if(siteTitle != null && !siteTitle.isEmpty())
        {
            siteTitle = StringEscapeUtils.unescapeHtml( siteTitle );

            ColorFont font = siteViewOptions.getFont();
            //float fontSize = (float) ( font.getFont().getSize()*Math.pow(density/10, 0.3));
            if( ( endPoint.x - startPoint.x > 20 || sitesCount < 100 ) && siteViewOptions.isShowTitle()
                    && siteViewOptions.getTrackDisplayMode().equals( SiteViewOptions.TRACK_MODE_FULL ) )
            {
                //font = new ColorFont(font.getFont().deriveFont(fontSize), font.getColor());
                // draw title
                FontMetrics fm = graphics.getFontMetrics( font.getFont() );

                String displayedTitle = siteTitle;
                if( !startVisible )
                {
                    displayedTitle = "..." + displayedTitle;
                }
                if( !endVisible )
                {
                    displayedTitle = displayedTitle + "...";
                }
                Point titleStart = new Point( ( endPoint.x + startPoint.x ) / 2 - fm.stringWidth( displayedTitle ) / 2,
                        startPoint.y - siteViewOptions.getInterval() );

                TextView text = new TextView( displayedTitle, titleStart, View.LEFT + View.BOTTOM, font, graphics );
                cv.add( text );
            }
        }

        ColorFont font = siteViewOptions.getSequenceFont();
        //fontSize = (float) ( font.getFont().getSize()*Math.pow(density/10, 0.3));
        // draw sequence
        if( (endPoint.x - startPoint.x > 20 || sitesCount < 100) && siteViewOptions.getTrackDisplayMode().equals(SiteViewOptions.TRACK_MODE_FULL) )
        {
            //font = new ColorFont(font.getFont().deriveFont(fontSize), font.getColor());
            FontMetrics fm = graphics.getFontMetrics(font.getFont());
            if( siteViewOptions.isShowSequence() && ( endPoint.x - startPoint.x > 50 || sitesCount < 50 ) )
            {
                int shift = siteViewOptions.isShowBox() ? siteViewOptions.getBoxHeight() : 0;
                String sequence = sequenceView.getSubSequence(start + startX - 1, start + endX - 1);
                Point sequenceStart = new Point( ( endPoint.x + startPoint.x ) / 2 - fm.stringWidth(sequence) / 2, startPoint.y
                        + fm.getHeight() + shift + siteViewOptions.getInterval());

                TextView text = new TextView(sequence, sequenceStart, View.BOTTOM, font, graphics);
                cv.add(text);
            }
            // draw positions
            if( siteViewOptions.isShowPositions() )
            {
                int shift = siteViewOptions.isShowBox() ? siteViewOptions.getBoxHeight() / 2 : 0;
                String value = TextUtil.valueOf(start + startX - 1, 0);
                Point startStart = new Point(startPoint.x - fm.stringWidth(value) - siteViewOptions.getInterval(), startPoint.y
                        + fm.getHeight() / 2 + shift);
                TextView text = new TextView(value, startStart, View.BOTTOM, font, graphics);
                cv.add(text);

                value = TextUtil.valueOf(start + endX - 1, 0);
                startStart.x = endPoint.x + siteViewOptions.getInterval();
                text = new TextView(value, startStart, View.BOTTOM, font, graphics);
                cv.add(text);
            }
        }
        // end

        cv.setDescription(siteTitle);
        cv.setModel(site);
        cv.setActive(true);

        return cv;
    }

    /**
     * @param pen
     * @param brush
     * @param startPoint
     * @param endPoint
     * @param arrowStart
     * @param arrowEnd
     * @return
     */
    protected View createArrow(int width, Pen pen, Brush brush, Point startPoint, Point endPoint, int arrowStart, int arrowEnd)
    {
        if(startPoint.x > endPoint.x)
            return createArrow(width, pen, brush, endPoint, startPoint, arrowEnd, arrowStart);
        int x1 = startPoint.x;
        int x2 = endPoint.x;
        if(x2 == x1) x2++;
        int y = startPoint.y;
        if(arrowStart == 0 && arrowEnd == 0)
            return new PolygonView(pen, brush, new int[] {x1, x1, x2, x2}, new int[] {y-width/2, y+width/2, y+width/2, y-width/2});
        if(arrowStart == 0 && arrowEnd == 1)
        {
            if(x2-x1 < width)
                return new PolygonView(pen, brush, new int[] {x1, x2, x1}, new int[] {y-width, y, y+width});
            return new PolygonView(pen, brush, new int[] {x1, x2-width, x2-width, x2, x2-width, x2-width, x1}, new int[] {y-width/2, y-width/2, y-width, y, y+width, y+width/2, y+width/2});
        }
        if(arrowStart == 1 && arrowEnd == 0)
        {
            if(x2-x1 < width)
                return new PolygonView(pen, brush, new int[] {x2, x1, x2}, new int[] {y-width, y, y+width});
            return new PolygonView(pen, brush, new int[] {x2, x1+width, x1+width, x1, x1+width, x1+width, x2}, new int[] {y-width/2, y-width/2, y-width, y, y+width, y+width/2, y+width/2});
        }
        if(arrowStart == 1 && arrowEnd == 1)
        {
            if(x2-x1 < width/2)
                return new PolygonView(pen, brush, new int[] {x1, (x1+x2)/2, x2, (x1+x2)/2}, new int[] {y, y+width, y, y-width});
            return new PolygonView(pen, brush, new int[] {x2, x2-width, x2-width, x1+width, x1+width, x1, x1+width, x1+width, x2-width, x2-width}, new int[] {y, y-width, y-width/2, y-width/2, y-width, y, y+width, y+width/2, y+width/2, y+width});
        }
        return null;
    }
}
