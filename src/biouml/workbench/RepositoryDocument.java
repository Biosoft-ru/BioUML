package biouml.workbench;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.logging.Level;

import javax.swing.Action;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.application.action.ApplicationAction;
import com.developmentontheedge.beans.Option;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.standard.type.DiagramInfo;
import biouml.workbench.htmlgen.GenerateHTMLAction;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.undo.DataCollectionUndoListener;
import ru.biosoft.access.repository.RepositoryListener;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.EditorPart;
import ru.biosoft.gui.ExplorerPane;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPart;

class RepositoryDocument extends Document implements RepositoryListener, PropertyChangeListener
{
    BioUMLApplication application;

    public RepositoryDocument(BioUMLApplication application, Component repositoryPane)
    {
        super( repositoryPane );
        this.application = application;

        repositoryPane.addFocusListener( new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                DocumentManager.setActiveRepositoryDocument( RepositoryDocument.this, (Component)getModel() );
            }
        } );

        DocumentManager.setActiveRepositoryDocument( this, (Component)getModel() );
    }

    /** Store changed data element */
    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if( log.isLoggable( Level.FINE ) )
            log.log(Level.FINE,  "Property changed: " + pce.getPropertyName() + ", propagated by " + pce.getPropagationId() );

        // Saving data element
        Object source = pce.getSource();
        if( source != null )
        {
            if( source instanceof Diagram && pce.getPropertyName().equals( "currentStateName" ) )
                return;

            if( source instanceof DataElement )
            {
                autoSave( (DataElement)source );
            }
            else if( source instanceof Option )
            {
                Option de = (Option)source;
                StreamEx.iterate( de, Option::getParent ).takeWhile( Objects::nonNull ).filter( ru.biosoft.access.core.DataElement.class::isInstance )
                        .map( ru.biosoft.access.core.DataElement.class::cast ).findFirst().ifPresent( this::autoSave );
            }
        }
    }

    protected void autoSave(DataElement de)
    {
        try
        {
            boolean needAutoSave = true;

            DataCollection origin = de.getOrigin();
            if( origin != null && origin.contains( de ) )
            {
                if( de instanceof DiagramInfo )
                    de = origin.get( de.getName() );

                for( Document document : GUI.getManager().getDocuments() )
                {
                    if( document.getModel().equals( de ) )
                    {
                        needAutoSave = false;
                        break;
                    }
                }
                if( needAutoSave && de.getClass().isAssignableFrom( origin.getDataElementType() ) )
                {
                    origin.put( de );
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE,  "Can't save data element", t );
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    // Repository interface implementation
    //

    @Override
    public void nodeClicked(DataElement de, int clickCount)
    {
    }

    @Override
    public void selectionChanged(DataElement de)
    {
        DocumentManager.setActiveRepositoryDocument( this, (Component)getModel() );

        ExplorerPane explorerPane = (ExplorerPane)application.getPanelManager().getPanel( BioUMLApplication.EXPLORER_PANE_NAME );
        explorerPane.removeBeanPropertyChangeListener( this );
        explorerPane.explore( de, this );
        // warranty that we set up component model to which propertyChangeListener should be added
        explorerPane.getPropertiesEditor().explore( de );
        explorerPane.addBeanPropertyChangeListener( this );

        boolean isDiagram = de instanceof Diagram;
        ActionManager actionManager = Application.getActionManager();
        Action generateHTMLAction = actionManager.getAction( GenerateHTMLAction.KEY );
        if( isDiagram && generateHTMLAction != null )
        {
            DataElementPath path = DataElementPath.create( de );
            generateHTMLAction.putValue( ApplicationAction.PARAMETER, path );
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
        if( !isActive )
        {
            removeListeners();
            ExplorerPane explorerPane = (ExplorerPane)application.getPanelManager().getPanel( BioUMLApplication.EXPLORER_PANE_NAME );
            explorerPane.removeBeanPropertyChangeListener( this );

            undoManager.discardAllEdits();

            return;
        }

        ExplorerPane explorerPane = (ExplorerPane)application.getPanelManager().getPanel( BioUMLApplication.EXPLORER_PANE_NAME );
        explorerPane.addBeanPropertyChangeListener( this );

        DocumentManager manager = DocumentManager.getDocumentManager();
        for( ViewPart viewPart : manager.getEditorList() )
        {
            if( viewPart instanceof EditorPart )
            {
                ( (EditorPart)viewPart ).addTransactionListener( undoManager );
            }
        }

        updateActionsState();
    }

    ////////////////////////////////////////////////////////////////////////////
    // RepositoryPane issues
    //

    /** Special listener to undo/redo changes in repository document. */
    public static class RepositoryUndoListener extends DataCollectionUndoListener
    {
        protected RepositoryDocument repositoryDocument;
        public RepositoryUndoListener(RepositoryDocument repositoryDocument)
        {
            super( repositoryDocument.getUndoManager() );
            this.repositoryDocument = repositoryDocument;
        }

        @Override
        public void elementRemoved(DataCollectionEvent e) throws Exception
        {
            // module removing can not be undo.
            if( elementToRemove instanceof Module )
            {
                repositoryDocument.getUndoManager().discardAllEdits();
                repositoryDocument.updateActionsState();
                return;
            }

            super.elementRemoved( e );
        }
    }
}
