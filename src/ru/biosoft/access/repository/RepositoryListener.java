package ru.biosoft.access.repository;

import ru.biosoft.access.core.DataElement;

/**
 * Listener of users actions with repository pane
 */
public interface RepositoryListener
{

    /**
     * If user clicked on the valid node in the repository pane
     */
    public void nodeClicked ( ru.biosoft.access.core.DataElement node, int clickCount );

    /**
     * If selection of nodes was changed by user
     */
    public void selectionChanged ( ru.biosoft.access.core.DataElement node );
    
}
