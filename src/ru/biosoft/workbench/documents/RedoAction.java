package ru.biosoft.workbench.documents;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;

import ru.biosoft.gui.Document;

// The Redo action
public class RedoAction extends AbstractAction
{
    public static final String KEY = "TextPaneRedoAction";

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        try
        {
            Document document = Document.getCurrentDocument();
            if( document == null )
                return;
            UndoManager manager = document.getUndoManager();
            if( manager != null && manager.canRedo() )
            {
                manager.redo();
                document.updateActionsState();
            }
        }
        catch( CannotRedoException e )
        {
        }
    }
}