package ru.biosoft.bsa.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.ViewOptions;

/**
 * Project implementation where tracks saved in VectorDataCollection and regions saved in List
 */
public class ProjectAsLists extends DataElementSupport implements Project, CloneableDataElement
{
    private final class PropertyChangeForwarder implements PropertyChangeListener
    {
        private final String prefix;

        public PropertyChangeForwarder(String prefix)
        {
            this.prefix = prefix;
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            firePropertyChange(prefix+"/"+evt.getSource(), evt.getOldValue(), evt.getNewValue());
        }
    }

    private final List<PropertyChangeListener> listeners = new Vector<>();
    private final PropertyChangeListener regionListener;
    private final PropertyChangeListener trackListener;
    private final PropertyChangeListener viewOptionsListener;
    
    /**
     * Creates project by AnnotatedSequence.
     * @param parent Parent collection to put project to
     * @param name name of the project element
     */
    public static Project createProjectByMap(DataCollection<?> parent, String name, AnnotatedSequence map) throws Exception
    {
        Project result = new ProjectAsLists(name, parent);

        for(Track track: map)
        {
            TrackInfo trackInfo = new TrackInfo(track);
            trackInfo.setTitle(map.getName());
            result.addTrack(trackInfo);
        }
        Region region = new Region(map);
        Interval interval = map.getSequence().getInterval();
        region.setInterval(new Interval(interval.getFrom(), (int) ( interval.getFrom() + Math.sqrt(interval.getLength()) * 10 )));
        result.addRegion(region);

        return result;
    }

    public ProjectAsLists(String name, DataCollection<?> parent)
    {
        super(name, parent);
        this.viewOptions = new ViewOptions();
        viewOptionsListener = new PropertyChangeForwarder("viewOptions");
        regionListener = new PropertyChangeForwarder("regions");
        trackListener = new PropertyChangeForwarder("tracks");
        this.viewOptions.setPropertyChangeListener(viewOptionsListener);
    }

    protected List<Region> regions = new ArrayList<>();
    @Override
    public Region[] getRegions()
    {
        return StreamEx.of(regions).sorted(Comparator.comparingInt( Region::getOrder )).toArray( Region[]::new );
    }

    @Override
    public void addRegion(Region region)
    {
        regions.add(region);
        region.addPropertyChangeListener(regionListener);
        firePropertyChange("regions", null, null);
    }

    @Override
    public void removeRegion(Region region)
    {
        region.removePropertyChangeListener(regionListener);
        regions.remove(region);
        firePropertyChange("regions", null, null);
    }

    protected List<TrackInfo> tracks = new ArrayList<>();
    @Override
    public TrackInfo[] getTracks()
    {
        return tracks.toArray(new TrackInfo[tracks.size()]);
    }

    @Override
    public String[] getTrackNames()
    {
        return StreamEx.of( tracks ).map( ti -> ti.getTrack().getName() ).toArray( String[]::new );
    }

    @Override
    public void addTrack(TrackInfo track)
    {
        tracks.add(track);
        track.addPropertyChangeListener(trackListener);
        firePropertyChange("tracks", null, null);
    }

    @Override
    public void removeTrack(TrackInfo track)
    {
        track.removePropertyChangeListener(trackListener);
        tracks.remove(track);
        firePropertyChange("tracks", null, null);
    }

    protected String description;
    @Override
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    @Override
    public String getDescription()
    {
        return description;
    }
    
    protected ViewOptions viewOptions;
    @Override
    public ViewOptions getViewOptions()
    {
        return viewOptions;
    }

    @Override
    public DataElement clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        ProjectAsLists clone = (ProjectAsLists)super.clone(origin, name);
        clone.regions = StreamEx.of( regions ).map( Region::clone ).toList();
        clone.tracks = StreamEx.of( tracks ).map( TrackInfo::clone ).toList();
        // TODO: clone ViewOptions
        return clone;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        listeners.add(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        listeners.remove(l);
    }
    
    protected void firePropertyChange(String what, Object oldValue, Object newValue)
    {
        if(listeners.isEmpty()) return;
        PropertyChangeEvent event = new PropertyChangeEvent(this, what, oldValue, newValue);
        for(PropertyChangeListener l: listeners)
        {
            try
            {
                l.propertyChange(event);
            }
            catch( Exception e )
            {
            }
        }
    }
}
