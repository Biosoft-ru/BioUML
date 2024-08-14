package ru.biosoft.workbench.documents;

import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;

/**
 * @author axec
 *
 */
@ClassIcon ( "resources/htmlDocument.gif" )
public class HtmlDocument extends Document
{
    private final TextPanel panel;
    
    public HtmlDocument(HtmlDataElement model)
    {
        super(model, new TextPaneUndoManager());
        viewPane = new ViewPane();
        
        if( model == null )
            panel = new TextPanel("text/html", null );
        else
            panel = new TextPanel( "text/html", model.getContent(), null);
        
        viewPane.add( panel );
//        panel.addUndoableEditListener(getUndoManager());
    }
    
//    protected StyledDocument getStyledDocument()
//    {
//        return null;
//    }
    
    @Override
    public String getDisplayName()
    {
        return getModel().getName();
    }

    @Override
    public HtmlDataElement getModel()
    {
        return (HtmlDataElement)super.getModel();
    }
    
//    @Override
//    public void save()
//    {
////        return;
////        getModel().setContent(panel.getText());
////        DataElementPath.create(getModel()).save(getModel());
////        super.save();
//    }

    @Override
    public boolean isChanged()
    {
        return false;
    }

//    private static boolean actionInitialized = false;
//    @Override
//    public Action[] getActions(ActionType actionType)
//    {
//        ActionManager actionManager = Application.getActionManager();
//        if( !actionInitialized )
//        {
//            actionInitialized = true;
//
//            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
//
//            //toolbar actions
//            Action action = new UndoAction();
//            actionManager.addAction(UndoAction.KEY, action);
//            initializer.initAction(action, UndoAction.KEY);
//
//            action = new RedoAction();
//            actionManager.addAction(RedoAction.KEY, action);
//            initializer.initAction(action, RedoAction.KEY);
//
//            updateActionsState();
//        }
//        if( actionType == ActionType.TOOLBAR_ACTION )
//        {
//            Action undoAction = actionManager.getAction(UndoAction.KEY);
//            Action redoAction = actionManager.getAction(RedoAction.KEY);
//            return new Action[] {undoAction, redoAction};
//        }
//        return null;
//    }
    
//    @Override
//    public void updateActionsState()
//    {
////        ActionManager actionManager = Application.getActionManager();
////        actionManager.enableActions( getUndoManager().canUndo(), UndoAction.KEY, SaveDocumentAction.KEY );
////        actionManager.enableActions( getUndoManager().canRedo(), RedoAction.KEY );
//    }

    @Override
    public void close()
    {
//        panel.removeUndoableEditListener(getUndoManager());
        super.close();
    }
}
