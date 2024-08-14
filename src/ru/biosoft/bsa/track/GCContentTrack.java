package ru.biosoft.bsa.track;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyDescriptor;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.SubSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.MapJobControl;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.TrackView;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.bsa.view.colorscheme.AutoTagColorScheme;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author lan
 *
 */
public class GCContentTrack extends DataElementSupport implements Track
{
    public static final String NAME = "GC content";
    public static final int MAX_SEQUENCE_LENGTH = 200000;
    public static final int MAX_SITES = 1000;
    public static final String SCORE = "GC%";
    private static final int TRACK_HEIGHT = 40;
    private static final byte[] gcPoints = new byte[256];
    private static final byte[] atPoints = new byte[256];

    static
    {
        gcPoints['g']=1;
        gcPoints['c']=1;
        gcPoints['G']=1;
        gcPoints['C']=1;
        atPoints['t']=1;
        atPoints['a']=1;
        atPoints['T']=1;
        atPoints['A']=1;
    }

    private static final PropertyDescriptor SCORE_PD = StaticDescriptor.create(SCORE);
    private static final Color PLUS_COLOR = new Color(128, 128, 255);
    private static final Color MINUS_COLOR = new Color(255, 128, 128);
    private final TrackViewBuilder viewBuilder = new GCContentViewBuilder();

    public GCContentTrack(DataCollection<?> origin)
    {
        super(NAME, origin);
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        SubSequence seq = new SubSequence(sequence, from, to);
        int count = countSites(sequence, from, to);
        return count == 0 ? new VectorDataCollection<>( "Sites", Site.class, null ) : new GCContentCollection( seq, count );
    }

    @Override
    public int countSites(String sequence, int from, int to)
    {
        if(to-from > MAX_SEQUENCE_LENGTH) return 0;
        return to-from+1 > MAX_SITES ? MAX_SITES : (to-from+1);
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        return getSites(sequence, from, to).get(siteName);
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    private static class GCContentCollection extends AbstractDataCollection<Site>
    {
        private final Interval interval;
        final List<Interval> intervalList;
        private final Sequence seq;
        private final List<String> nameList;

        public GCContentCollection(SubSequence seq, int count)
        {
            super("Sites", null, null);
            this.seq = seq.getCompletePath().getDataElement(AnnotatedSequence.class).getSequence();
            this.interval = seq.getInterval();
            this.intervalList = interval.split(count);
            this.nameList = new AbstractList<String>()
            {
                @Override
                public String get(int index)
                {
                    return intervalList.get(index).toString();
                }

                @Override
                public int size()
                {
                    return intervalList.size();
                }
            };
        }

        @Override
        public @Nonnull List<String> getNameList()
        {
            return nameList;
        }

        @Override
        protected Site doGet(String name) throws Exception
        {
            Interval interval = new Interval(name);
            if(!this.interval.inside(interval)) return null;
            int gcCount = 0;
            int atCount = 0;
            for(int i=interval.getFrom(); i<=interval.getTo(); i++)
            {
                byte letter = seq.getLetterAt(i);
                gcCount += gcPoints[letter];
                atCount += atPoints[letter];
            }
            DynamicPropertySet properties = new DynamicPropertySetAsMap();
            String siteType = "N";
            if(atCount > 0 || gcCount > 0)
            {
                siteType = gcCount > atCount ? "GC" : gcCount < atCount ? "AT" : "N";
                properties.add(new DynamicProperty(SCORE_PD, Double.class, gcCount*100.0/(atCount+gcCount)));
            }
            return new SiteImpl(null, name, siteType, Site.BASIS_ANNOTATED, interval.getFrom(), interval.getLength(),
                    Site.PRECISION_EXACTLY, StrandType.STRAND_BOTH, seq, properties);
        }

        @Override
        public int getSize()
        {
            return intervalList.size();
        }
    }

    public static class GCContentViewBuilder extends TrackViewBuilder
    {
        @Override
        public CompositeView createTrackView(SequenceView sequenceView, DataCollection<Site> sites, SiteViewOptions siteViewOptions,
                int start, int end, int direction, Graphics graphics, MapJobControl control)
        {
            if(sites.isEmpty())
            {
                CompositeView trackView = new CompositeView();
                String labelString = "Zoom in to see GC content";
                TextView errorLabel = new TextView(labelString, siteViewOptions.getTrackTitleFont(), ApplicationUtils.getGraphics());
                ( trackView ).add(errorLabel, CompositeView.X_CC,
                        new Point(sequenceView.getStartPoint( ( start + end ) / 2 - start + 1, graphics).x, 0));
                return trackView;
            }
            TrackView trackView = new TrackView();

            for(Site site: sites)
            {
                Interval interval = getInterval(site, start, end, sequenceView, graphics);
                Object value = site.getProperties().getValue(SCORE);
                double score = value == null ? -1 : (Double)value;
                int height = (int) ( Math.abs(score-50)/(100.0/TRACK_HEIGHT) );
                Brush brush = siteViewOptions.getColorScheme().getBrush(site);
                int startY;
                if( score == -1 )
                {
                    startY = -TRACK_HEIGHT / 4;
                    height = TRACK_HEIGHT / 2;
                }
                else if( score > 50 )
                {
                    startY = -height;
                }
                else
                {
                    startY = 0;
                }
                View view = interval.getLength()==1?
                        new LineView(new Pen(1.0f, (Color)brush.getPaint()), interval.getFrom(), startY, interval.getFrom(), startY+height):
                        new BoxView(null, brush, interval.getFrom(), startY, interval.getLength(), height);
                view.setActive(true);
                view.setModel(site);
                view.setDescription(score == -1 ? "N" : String.format("%.2f", score)+"%");
                trackView.add(view);
            }
            return trackView;
        }

        @Override
        public SiteViewOptions createViewOptions()
        {
            SiteViewOptions viewOptions = new GCContentViewOptions();
            return viewOptions;
        }
    }

    public static class GCContentViewOptions extends SiteViewOptions
    {
        {
            AutoTagColorScheme scheme = new AutoTagColorScheme();
            scheme.getColors().add(new DynamicProperty("GC", Color.class, PLUS_COLOR));
            scheme.getColors().add(new DynamicProperty("AT", Color.class, MINUS_COLOR));
            scheme.getColors().add(new DynamicProperty("N", Color.class, Color.GRAY));
            schemes = Collections.singletonMap( scheme.getName(), scheme );
            setColorScheme( scheme );
        }
    }

    public static class GCContentViewOptionsBeanInfo extends BeanInfoEx2<GCContentViewOptions>
    {
        public GCContentViewOptionsBeanInfo()
        {
            super(GCContentViewOptions.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("trackDisplayMode").hidden().tags( SiteViewOptions.getTrackDisplayModes() ).add();
            add( "trackTitleFont", FontEditor.class );
            add( "font", FontEditor.class );
            add("colorScheme");
        }
    }
}
