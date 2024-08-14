package ru.biosoft.gui;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.RepositoryListener;

public class RepositoryDocument extends Document implements RepositoryListener, PropertyChangeListener
{
    public RepositoryDocument(Component repositoryPane, ExplorerPane explorerPane)
    {
        super(repositoryPane);
        this.explorerPane = explorerPane;

        repositoryPane.addFocusListener(
            new FocusAdapter()
            {
                @Override
                public void focusGained(FocusEvent e)
                {
                    DocumentManager.setActiveRepositoryDocument(RepositoryDocument.this, (Component)getModel());
                }
            });

        DocumentManager.setActiveRepositoryDocument(this, (Component)getModel());
    }

    /** Store changed data element */
    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if( log.isLoggable( Level.FINE ) )
            log.log(Level.FINE, "Property changed: " + pce.getPropertyName() + ", propagated by " + pce.getPropagationId());

        // Saving data element
        Object source = pce.getSource();
        if (source instanceof DataElement)
        {
            try
            {
                DataElement de = (DataElement)source;
                @SuppressWarnings ( "unchecked" )
                DataCollection<DataElement> parent = (DataCollection<DataElement>)de.getOrigin();
                parent.put(de);
            }
            catch(Throwable t)
            {
                log.log(Level.SEVERE, "Can't save data element", t);
            }
        }
    }

    protected ExplorerPane explorerPane;
    public ExplorerPane getExplorerPane()
    {
        return explorerPane;
    }

    ////////////////////////////////////////////////////////////////////////////
    // RepositoryListener interface implmentation
    //

    @Override
    public void nodeClicked(DataElement de, int clickCount)
    {}

    @Override
    public void selectionChanged(DataElement de)
    {
        DocumentManager.setActiveRepositoryDocument(this, (Component)getModel());

        if( explorerPane != null )
        {
            explorerPane.removeBeanPropertyChangeListener(this);
            explorerPane.explore(de, this);

            // warranty that we set up component model to which propertyChangeListener should be added
            explorerPane.getPropertiesEditor().explore(de);
            explorerPane.addBeanPropertyChangeListener(this);
        }
    }

    //----- Document issues  -------------------------------------/

    @Override
    public String getDisplayName()
    {
        return "Repository";
    }

    @Override
    public void setActive(boolean isActive)
    {
        if (!isActive)
        {
            removeListeners();
            if( explorerPane != null )
                explorerPane.removeBeanPropertyChangeListener(this);

            undoManager.discardAllEdits();

            return;
        }

        if( explorerPane != null )
            explorerPane.addBeanPropertyChangeListener(this);

        DocumentManager manager = DocumentManager.getDocumentManager();
        for( ViewPart viewPart: manager.getEditorList() )
        {
            if( viewPart instanceof EditorPart )
            {
                ( (EditorPart)viewPart ).addTransactionListener(undoManager);
            }
        }

        updateActionsState();
    }
}


