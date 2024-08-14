package ru.biosoft.workbench.script;

import javax.swing.Action;
import javax.swing.text.StyledDocument;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.workbench.MessageBundle;
import ru.biosoft.workbench.documents.RedoAction;
import ru.biosoft.workbench.documents.TextDocument;
import ru.biosoft.workbench.documents.UndoAction;

/**
 * @author lan
 *
 */
@ClassIcon ( "resources/scriptDocument.gif" )
public class ScriptDocument extends TextDocument
{
    public ScriptDocument(ScriptDataElement model)
    {
        super(model);
    }

    private static boolean actionInitialized = false;
    @Override
    public Action[] getActions(ActionType actionType)
    {
        super.getActions(actionType);
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;

            //toolbar actions
            Action action = new ExecuteAction();
            actionManager.addAction(ExecuteAction.KEY, action);
            new ActionInitializer(MessageBundle.class).initAction(action, ExecuteAction.KEY);

            updateActionsState();
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action undoAction = actionManager.getAction(UndoAction.KEY);
            Action redoAction = actionManager.getAction(RedoAction.KEY);
            Action executeAction = actionManager.getAction(ExecuteAction.KEY);
            return new Action[] {undoAction, redoAction, executeAction};
        }
        return null;
    }
    @Override
    protected StyledDocument getStyledDocument()
    {
        return ScriptTypeRegistry.getScriptType(getModel()).getHighlightedDocument();
    }

    @Override
    public ScriptDataElement getModel()
    {
        return (ScriptDataElement)super.getModel();
    }
}
