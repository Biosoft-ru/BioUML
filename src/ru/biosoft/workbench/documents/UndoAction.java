package ru.biosoft.workbench.documents;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import ru.biosoft.gui.Document;

// The Undo action
public class UndoAction extends AbstractAction
{
    public static final String KEY = "TextPaneUndoAction";

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        try
        {
            Document document = Document.getCurrentDocument();
            if( document == null )
                return;
            UndoManager manager = document.getUndoManager();
            if( manager != null && manager.canUndo() )
            {
                manager.undo();
                document.updateActionsState();
            }
        }
        catch( CannotUndoException e )
        {
        }
    }
}