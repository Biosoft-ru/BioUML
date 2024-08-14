package ru.biosoft.bsa._test;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.MapViewOptions;
import ru.biosoft.bsa.view.SequenceView;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.bsa.view.ViewFactory;
import ru.biosoft.bsa.view.sitelayout.SiteLayoutAlgorithm;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author lan
 *
 */
public class AbstractTrackTest extends AbstractBioUMLTest
{
    protected void checkViewBuilder(Track track, AnnotatedSequence seq, Interval interval) throws Exception
    {
        TrackViewBuilder viewBuilder = track.getViewBuilder();
        DataCollection<Site> sites = track.getSites(DataElementPath.create(seq).toString(), interval.getFrom(), interval.getTo());
        SequenceView sequenceView = ViewFactory.createSequenceView(seq.getSequence(), new MapViewOptions().getSequenceViewOptions(),
                interval.getFrom(), interval.getTo(), ApplicationUtils.getGraphics());
        assertNotNull("Sequence view created", sequenceView);
        CompositeView cv = viewBuilder.createTrackView(sequenceView, sites, viewBuilder.createViewOptions(), interval.getFrom(), interval.getTo(),
                SiteLayoutAlgorithm.BOTTOM, ApplicationUtils.getGraphics(), null);
        assertNotNull("View created", cv);
        Deque<View> views = new LinkedList<>();
        views.add(cv);
        Set<String> siteNames = sites.names().collect( Collectors.toSet() );
        while(!views.isEmpty())
        {
            View view = views.poll();
            if(view.getModel() instanceof Site)
            {
                Site site = (Site)view.getModel();
                assertTrue(sites.contains(site.getName()));
                siteNames.remove(site.getName());
            }
            if(view instanceof CompositeView)
            {
                for(View child: (CompositeView)view)
                    views.add(child);
            }
        }
        // Second condition is for the "Too many sites to display" case
        assertTrue(siteNames.isEmpty() || (siteNames.size() == sites.getSize() && siteNames.size() >= TrackViewBuilder.SITE_COUNT_LIMIT));
    }
}
