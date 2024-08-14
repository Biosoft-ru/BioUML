package ru.biosoft.bsa.finder;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Track;

public class TrackSummary implements DataElement
{
    private Track track;
    public TrackSummary(Track track)
    {
        this(track, track.getName());
    }
    
    public TrackSummary(Track track, String name)
    {
        this.track = track;
        this.name = name;
    }

    private String name;
    @PropertyName("Track name")
    @Override
    public String getName()
    {
        return name;
    }
    
    private int size;
    @PropertyName("Site count")
    public int getSize()
    {
        return size;
    }
    public void setSize(int size)
    {
        this.size = size;
    }

    @PropertyName("Visibility")
    public String getPath()
    {
        return track.getCompletePath().toString();
    }
    
    @Override
    public DataCollection<?> getOrigin()
    {
        return null;
    }
    
    public Track getTrack()
    {
        return track;
    }
}
