package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.undo.UndoManager;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionManager;

public class RedoAction extends AbstractAction
{
    public static final String KEY = "Redo";

    public RedoAction()
    {
        super(KEY);
    }

    public RedoAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Document activeDocument = Document.getActiveDocument();
        if( activeDocument != null )
        {
            UndoManager undoManager = activeDocument.getUndoManager();
            undoManager.redo();
            setEnabled(undoManager.canRedo());

            ActionManager actionManager = Application.getActionManager();
            actionManager.enableActions( undoManager.canUndo(), UndoAction.KEY );

            activeDocument.update();
        }
    }
}
