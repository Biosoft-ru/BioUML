package ru.biosoft.workbench.documents;


import javax.swing.Action;
import javax.swing.text.StyledDocument;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.SaveDocumentAction;
import ru.biosoft.workbench.MessageBundle;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

/**
 * @author lan
 *
 */
@ClassIcon ( "resources/txtDocument.gif" )
public class TextDocument extends Document
{
    private final TextPanel panel;
    
    public TextDocument(TextDataElement model)
    {
        super(model, new TextPaneUndoManager());
        viewPane = new ViewPane();
        panel = new TextPanel(model == null ? "" : model.getContent(), getStyledDocument());
        viewPane.add(panel);
        panel.addUndoableEditListener(getUndoManager());
    }
    
    protected StyledDocument getStyledDocument()
    {
        return null;
    }
    
    @Override
    public String getDisplayName()
    {
        return getModel().getName();
    }

    @Override
    public TextDataElement getModel()
    {
        return (TextDataElement)super.getModel();
    }
    
    @Override
    public void save()
    {
        getModel().setContent(panel.getText());
        DataElementPath.create(getModel()).save(getModel());
        super.save();
    }

    @Override
    public boolean isChanged()
    {
        return getUndoManager().canUndo();
    }

    private static boolean actionInitialized = false;
    @Override
    public Action[] getActions(ActionType actionType)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;

            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

            //toolbar actions
            Action action = new UndoAction();
            actionManager.addAction(UndoAction.KEY, action);
            initializer.initAction(action, UndoAction.KEY);

            action = new RedoAction();
            actionManager.addAction(RedoAction.KEY, action);
            initializer.initAction(action, RedoAction.KEY);

            updateActionsState();
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action undoAction = actionManager.getAction(UndoAction.KEY);
            Action redoAction = actionManager.getAction(RedoAction.KEY);
            return new Action[] {undoAction, redoAction};
        }
        return null;
    }
    
    @Override
    public void updateActionsState()
    {
        ActionManager actionManager = Application.getActionManager();
        actionManager.enableActions( getUndoManager().canUndo(), UndoAction.KEY, SaveDocumentAction.KEY );
        actionManager.enableActions( getUndoManager().canRedo(), RedoAction.KEY );
    }

    @Override
    public void close()
    {
        panel.removeUndoableEditListener(getUndoManager());
        super.close();
    }
}
