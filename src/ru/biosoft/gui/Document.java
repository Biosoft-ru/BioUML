package ru.biosoft.gui;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.swing.Action;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPane;
import biouml.workbench.ExportElementDialog;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 * Document concept definition.
 */
public abstract class Document implements ApplicationDocument
{
    protected static final Logger log = Logger.getLogger( Document.class.getName() );
    protected int transactionStack;

    protected boolean justSaved = true;

    public Document(Object model, DocumentTransactionUndoManager undoManager)
    {
        this.model = model;
        this.undoManager = undoManager;
    }

    public Document(Object model)
    {
        this.model = model;

        undoManager = new DocumentTransactionUndoManager()
        {
            @Override
            public void completeTransaction()
            {
                if( !super.isRedo && !super.isUndo )
                {
                    super.completeTransaction();
                    update();
                    updateActionsState();
                    justSaved = false;
                }
            }
        };
    }

    public @CheckForNull OkCancelDialog getExportDialog()
    {
        if(!(model instanceof DataElement) || !DataElementExporterRegistry.hasExporter((DataElement)model))
            return null;
        return new ExportElementDialog(Application.getApplicationFrame(), (DataElement)model);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    private Object model;
    public Object getModel()
    {
        return model;
    }

    /**
     * setModel is used to set different versions of document
     */
    public void setModel(Object model)
    {
        this.model = model;
    }

    protected ViewPane viewPane;
    public ViewPane getViewPane()
    {
        return viewPane;
    }

    public void updateViewPane()
    {
    }

    protected DocumentTransactionUndoManager undoManager = null;
    public DocumentTransactionUndoManager getUndoManager()
    {
        return undoManager;
    }

    public boolean isChanged()
    {
        return ( !justSaved && undoManager.canUndo() );
    }

    public List<DataElement> getSelectedItems()
    {
        return Collections.<DataElement> emptyList();
    }

    /**
     * Returns actions for this document
     */
    public Action[] getActions(ActionType actionType)
    {
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Document life cycle issues
    //

    /** Removes listeners from shared editors and save changes */
    protected void removeListeners()
    {
        DocumentManager manager = DocumentManager.getDocumentManager();
        for( ViewPart viewPart: manager.getEditorList() )
        {
            if( viewPart instanceof EditorPart )
            {
                EditorPart editor = (EditorPart)viewPart;
                editor.removeTransactionListener(undoManager);
                editor.save();
            }
        }
    }

    public void setActive(boolean isActive)
    {
        DocumentManager manager = DocumentManager.getDocumentManager();
        List<ViewPart> editors = manager.getEditorList();

        if( !isActive )
        {
            removeListeners();

            // clear editors
            if( manager.getActiveDocument() == null )
            {
                for( ViewPart view : editors )
                {
                    if( view.canExplore(null) )
                        view.explore(null, null);
                }
            }

            return;
        }

        // explore model and add listeners to shared editors
        for( ViewPart view : editors )
        {
            if( view.canExplore(model) )
            {
                view.explore(model, this);
                if( view instanceof EditorPart )
                {
                    EditorPart editor = (EditorPart)view;
                    editor.addTransactionListener(undoManager);
                }
            }

            updateActionsState();
        }
    }

    public void applyEditorChanges()
    {
        for( ViewPart viewPart: DocumentManager.getDocumentManager().getEditorList() )
        {
            if( viewPart instanceof EditorPart )
            {
                ( (EditorPart)viewPart ).save();
            }
        }
    }

    public void save()
    {
        justSaved = true;
        undoManager.discardAllEdits();
        updateActionsState();
    }

    @Override
    public void close()
    {
        removeListeners();
        undoManager.die();
        updateActionsState();
    }

    ////////////////////////////////////////////////////////////////////////////
    // update issues
    //

    public void updateActionsState()
    {
        ActionManager actionManager = Application.getActionManager();
        actionManager.enableActions( undoManager.canUndo(), UndoAction.KEY);
        actionManager.enableActions( undoManager.canRedo(), RedoAction.KEY);
    }

    public void update()
    {
        doUpdate();
    }

    protected void doUpdate()
    {
    }

    public boolean isMutable()
    {
        return false;
    }

    /////////////////////////////////////////
    public enum ActionType
    {
        MENU_ACTION, TOOLBAR_ACTION, POPUP_ACTION, ENABLED_ACTIONS
    }

    public static @CheckForNull Document getActiveDocument()
    {
        ApplicationFrame frame = Application.getApplicationFrame();
        if(frame == null) return null;
        com.developmentontheedge.application.DocumentManager documentManager = frame.getDocumentManager();
        if(documentManager == null) return null;
        ApplicationDocument document = documentManager.getActiveDocument();
        if(!(document instanceof Document)) return null;
        return (Document)document;
    }

    public static @CheckForNull ru.biosoft.access.core.DataElement getActiveModel()
    {
        Document document = getActiveDocument();
        if(document == null)
            return null;
        Object model = document.getModel();
        if(model instanceof DataElement)
            return (DataElement)model;
        return null;
    }

    /**
     *  return selected document from documentPane
     */
    public static @CheckForNull Document getCurrentDocument()
    {
        try
        {
            return GUI.getManager().getCurrentDocument();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public void startTransaction(String name)
    {
        if(getViewPane() instanceof ViewEditorPane)
        {
            ((ViewEditorPane)getViewPane()).startTransaction( name );
        }
    }

    public void completeTransaction()
    {
        if(getViewPane() instanceof ViewEditorPane)
        {
            ((ViewEditorPane)getViewPane()).completeTransaction();
        }
    }
}
