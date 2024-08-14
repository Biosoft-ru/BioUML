package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.undo.UndoManager;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionManager;

@SuppressWarnings ( "serial" )
public class UndoAction extends AbstractAction
{
    public static final String KEY = "Undo";

    public UndoAction()
    {
        super(KEY);
    }

    public UndoAction(boolean enabled)
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
            undoManager.undo();
            setEnabled(undoManager.canUndo());

            ActionManager actionManager = Application.getActionManager();
            actionManager.enableActions( undoManager.canRedo(), RedoAction.KEY );

            activeDocument.update();
        }
    }
}
