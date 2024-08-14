package ru.biosoft.workbench.documents;

import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.undo.UndoableEdit;

import ru.biosoft.gui.DocumentTransactionUndoManager;
import ru.biosoft.gui.SaveDocumentAction;

import com.developmentontheedge.application.Application;

// UndoManager
public class TextPaneUndoManager extends DocumentTransactionUndoManager
{
    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit)
    {
        if( anEdit instanceof DefaultDocumentEvent )
        {
            if( ( (DefaultDocumentEvent)anEdit ).getType() != EventType.CHANGE )
            {
                Application.getActionManager().enableActions( true, SaveDocumentAction.KEY );
                return super.addEdit(anEdit);
            }
        }
        return false;
    }
}