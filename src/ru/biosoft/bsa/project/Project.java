package ru.biosoft.bsa.project;

import java.beans.PropertyChangeListener;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.view.ViewOptions;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Main interface for BSA project
 * Project is a ru.biosoft.access.core.DataCollection. Tracks are the elements of it.
 */
@ClassIcon("resources/project.gif")
@PropertyName("genome browser view")
public interface Project extends DataElement, CloneableDataElement
{
    /**
     * Return all regions sorted by order
     */
    public Region[] getRegions();
    /**
     * Add new region to project
     */
    public void addRegion(Region region);
    /**
     * Remove region from project
     */
    public void removeRegion(Region region);
    /**
     * Return all tracks
     */
    public TrackInfo[] getTracks();
    /**
     * Return all track names
     */
    public String[] getTrackNames();
    /**
     * Add new track to project
     */
    public void addTrack(TrackInfo track);
    /**
     * Remove track from project
     */
    public void removeTrack(TrackInfo track);
    /**
     * Set project description
     */
    public void setDescription(String description);
    /**
     * Get project description
     */
    public String getDescription();
    /**
     * Return view options for project
     */
    public ViewOptions getViewOptions();
    
    /**
     * Add a PropertyChangeListener to the listener list. The listener is registered
     * for all the properties.
     * @param l the PropertyChangeListener to be added
     */
    public void addPropertyChangeListener( PropertyChangeListener l );
    
    /**
     * Remove PropertyChangeListener from the listener list. This removes a
     * PropertyChangeListener that was registered for all properties.
     * @param l the PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener( PropertyChangeListener l );
}
