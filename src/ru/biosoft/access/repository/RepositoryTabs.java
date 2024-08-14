package ru.biosoft.access.repository;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JTabbedPane;

import java.util.logging.Logger;

import com.developmentontheedge.beans.ActionsProvider;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;


/**
 * JTabbedPane containing RepositoryPane tabs
 * @author lan
 */
public class RepositoryTabs extends JTabbedPane
{
    private static final Logger log = Logger.getLogger(RepositoryTabs.class.getName());
    private static final long serialVersionUID = 1L;
    Set<RepositoryListener> listeners = new LinkedHashSet<>();
    private RepositoryListener tabsListener = new TabsListener();
    private Map<String, RepositoryPane> panes = new HashMap<>();
    protected ActionsProvider actionsProvider;

    private class TabsListener implements RepositoryListener
    {
        @Override
        public void nodeClicked(DataElement node, int clickCount)
        {
            for(RepositoryListener listener: listeners)
            {
                try
                {
                    listener.nodeClicked(node, clickCount);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, ExceptionRegistry.log(t));
                }
            }
        }

        @Override
        public void selectionChanged(DataElement node)
        {
            for(RepositoryListener listener: listeners)
            {
                try
                {
                    listener.selectionChanged(node);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, ExceptionRegistry.log(t));
                }
            }
        }
    }

    public ActionsProvider getActionsProvider()
    {
        return actionsProvider;
    }
    /**
     * Sets actionsProvider for all panes in this container
     * @param actionsProvider
     */
    public void setActionsProvider(ActionsProvider actionsProvider)
    {
        this.actionsProvider = actionsProvider;
    }

    /**
     * Creates new RepositoryPane, adds it and returns
     * @param title title for the new pane
     * @param root root DataCollection to be displayed in the pane
     * @return
     */
    public synchronized RepositoryPane addRepositoryPane(String title, DataCollection root)
    {
        RepositoryPane newPane;
        if(panes.containsKey(title))
        {
            newPane = panes.get(title);
        } else
        {
            newPane = new RepositoryPane(root);
            panes.put(title, newPane);
            newPane.setActionsProvider(actionsProvider);
        }
        newPane.addListener(tabsListener);
        addTab(title, newPane);
        return newPane;
    }

    public synchronized void removeRepositoryPane(String title)
    {
        RepositoryPane pane = panes.get(title);
        if(pane != null)
        {
            pane.removeListener(tabsListener);
            remove(pane);
        }
    }

    @Override
    public void removeAll()
    {
        setSelectedIndex(-1);
        while(getTabCount() > 0)
        {
            removeRepositoryPane(getTitleAt(0));
        }
    }

    /**
     * Adds listener to all repository panes
     * Does nothing it this listener was already added before
     * @param listener
     */
    public synchronized void addListener(RepositoryListener listener)
    {
        // Clone it as previous instance might be used right now in TabsListener iterators
        listeners = new LinkedHashSet<>( listeners );
        listeners.add(listener);
    }

    public void selectElement(DataElementPath de)
    {
        selectElement( de, false );
    }

    public void selectElement(DataElementPath de, boolean expand)
    {
        for(RepositoryPane pane : panes.values())
        {
            if(pane.getRootDataCollection().getCompletePath().isAncestorOf( de ))
            {
                setSelectedComponent( pane );
                pane.selectElement( de, expand );
            }
        }
    }
}
