package ru.biosoft.bsa.project;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Track;

import com.developmentontheedge.beans.Option;

/**
 * Description of track in the project
 */
@SuppressWarnings ( "serial" )
public class TrackInfo extends Option implements Comparable<TrackInfo>, Cloneable
{
    /**
     * Base track location
     */
    protected String dbName;
    /**
     * Track instance
     */
    protected Track track;
    /**
     * Name of track in the project
     */
    protected String title;
    /**
     * Group of the track
     */
    protected String group;
    /**
     * Preferred position in project
     */
    protected int order;
    /**
     * Indicates if track should be visible
     */
    protected boolean visible;
    /**
     * Track description
     */
    protected String description;

    public TrackInfo(Track track, DataElementPath dbName)
    {
        this.track = track;
        this.dbName = dbName.toString();
        visible = true;
    }
    
    public TrackInfo(Track track)
    {
        this(track, DataElementPath.create(track));
        title = track.getName();
        try
        {
            // Trying to fetch Track title if available
            title = (String)track.getOrigin().getInfo().getQuerySystem().getIndex("title").get(track.getName());
        }
        catch(Exception e)
        {
        }
    }

    public String getDbName()
    {
        return dbName;
    }

    public void setDbName(String dbName)
    {
        Object oldValue = this.dbName;
        this.dbName = dbName;
        firePropertyChange("dbName", oldValue, dbName);
    }

    public Track getTrack()
    {
        return track;
    }

    public void setTrack(Track track)
    {
        Object oldValue = this.track;
        this.track = track;
        firePropertyChange("track", oldValue, track);
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(String group)
    {
        Object oldValue = this.group;
        this.group = group;
        firePropertyChange("group", oldValue, group);
    }

    public int getOrder()
    {
        return order;
    }

    public void setOrder(int order)
    {
        int oldValue = this.order;
        this.order = order;
        firePropertyChange("order", oldValue, order);
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        boolean oldValue = this.visible;
        this.visible = visible;
        firePropertyChange("visible", oldValue, visible);
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        Object oldValue = this.title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        Object oldValue = this.description;
        this.description = description;
        firePropertyChange("description", oldValue, description);
    }

    @Override
    public int compareTo(TrackInfo o)
    {
        try
        {
            return getOrder() > o.getOrder() ? 1 : getOrder() < o.getOrder() ? -1 : getDbName().compareTo(o.getDbName());
        }
        catch( Exception e )
        {
            return 0;
        }
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    @Override
    protected TrackInfo clone()
    {
        try
        {
            return (TrackInfo)super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalError();
        }
    }
}
