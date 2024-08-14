package biouml.plugins.ensembl.tracks;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;

import com.developmentontheedge.beans.BeanInfoEx;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.MapJobControl;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackView;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.graphics.font.ColorFont;

public class KaryotypeTrackViewBuilder extends TrackViewBuilder
{
    private static class Stain
    {
        String name;
        Color background;
        Color title;
        public Stain(String name, Color background, Color title)
        {
            super();
            this.name = name;
            this.background = background;
            this.title = title;
        }
    }

    private static final Stain[] stains = {new Stain("default", Color.WHITE, Color.BLACK),
            new Stain("acen", new Color(112, 128, 144), Color.BLACK), new Stain("stalk", new Color(112, 128, 144), Color.BLACK),
            new Stain("gpos25", new Color(217, 217, 217), Color.BLACK), new Stain("gpos50", new Color(153, 153, 153), Color.BLACK),
            new Stain("gpos75", new Color(102, 102, 102), Color.WHITE), new Stain("gpos100", Color.BLACK, Color.WHITE),
            new Stain("gpos", Color.BLACK, Color.WHITE), new Stain("gvar", new Color(224, 224, 224), Color.BLACK),};

    @Override
    protected View createSiteView(SequenceView sequenceView, Site site, SiteViewOptions siteViewOptions, int start, int end,
            Graphics graphics, int siteCount)
    {
        if( site == null )
        {
            return null;
        }
        int startX = site.getStart();
        int endX = site.getStart() + site.getLength() - 1;
        if( startX > end || endX < start )
        {
            return null;
        }
        if( startX < start )
        {
            startX = start;
        }
        if( endX > end )
        {
            endX = end;
        }
        CompositeView cv = new CompositeView();
        String stain = (String)site.getProperties().getProperty("stain").getValue();
        Stain selectedStain = StreamEx.of(stains).findFirst( s -> s.name.equals( stain ) ).orElse( stains[0] );
        Color backgroundColor = selectedStain.background;
        Color titleColor = selectedStain.title;
        String name = (String)site.getProperties().getProperty("name").getValue();

        // Beginning drawing sites from 1
        startX -= start - 1;
        endX -= start - 1;

        Point startPoint = sequenceView.getStartPoint(startX, graphics);
        Point endPoint = sequenceView.getEndPoint(endX, graphics);
        startPoint.x--;
        ColorFont font = new ColorFont(siteViewOptions.getFont().getFont(), titleColor);
        FontMetrics fm = graphics.getFontMetrics(font.getFont());
        int height = fm.getHeight() + 4;
        Pen pen = new Pen(1, Color.BLACK);
        if( stain.equals("acen"))
        {
            if( name.startsWith("q") || name.endsWith("q") )
            {
                cv.add(new PolygonView(pen, new Brush(backgroundColor), new int[] {endPoint.x, startPoint.x, endPoint.x}, new int[] {0,
                        height / 2, height}));
            }
            else
            {
                cv.add(new PolygonView(pen, new Brush(backgroundColor), new int[] {startPoint.x, endPoint.x, startPoint.x}, new int[] {0,
                        height / 2, height}));
            }
        }
        else if( stain.equals("stalk"))
        {
            cv.add(new PolygonView(pen, new Brush(backgroundColor), new int[] {startPoint.x, ( 3 * startPoint.x + endPoint.x ) / 4,
                    ( startPoint.x + 3 * endPoint.x ) / 4, endPoint.x, endPoint.x, ( startPoint.x + 3 * endPoint.x ) / 4,
                    ( 3 * startPoint.x + endPoint.x ) / 4, startPoint.x}, new int[] {0, height / 4, height / 4, 0, height, 3 * height / 4,
                    3 * height / 4, height}));
        }
        else
        {
            cv.add(new BoxView(pen, new Brush(backgroundColor), startPoint.x, 0, endPoint.x - startPoint.x, height));
            if(fm.stringWidth(name) < endPoint.x-startPoint.x-4)
            {
                cv.add(new TextView(name, font, graphics), CompositeView.Y_CC|CompositeView.X_CC);
            }
        }
        cv.setModel(site);
        cv.setActive(true);
        cv.setDescription(name);

        return cv;
    }

    @Override
    public CompositeView createTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions,
            int start, int end, int direction, Graphics graphics, MapJobControl control)
    {
        int size = sites.getSize();

        int i = 0;
        TrackView trackView = new TrackView();

        for( Site site : sites )
        {
            View view = createSiteView(sequenceView, site, siteViewOptions, start, end, graphics, size);
            if( view != null )
                trackView.add(view);
            if( control != null )
                control.setCurrentLength(start + (int) ( ( (double) ( end - start ) ) * ( (double) ( i++ ) ) / ( size ) ));
        }

        return trackView;
    }

    @Override
    public SiteViewOptions createViewOptions()
    {
        return new KaryotypeViewOptions();
    }

    public static class KaryotypeViewOptions extends SiteViewOptions
    {
        public KaryotypeViewOptions()
        {
            super();
        }
    }

    public static class KaryotypeViewOptionsBeanInfo extends BeanInfoEx
    {
        public KaryotypeViewOptionsBeanInfo()
        {
            super( KaryotypeViewOptions.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "trackTitleFont", FontEditor.class );

            add( "font", FontEditor.class );
        }
    }
}
