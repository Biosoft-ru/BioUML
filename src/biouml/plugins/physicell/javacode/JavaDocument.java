package biouml.plugins.physicell.javacode;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JPanel;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.SaveDocumentAction;
import ru.biosoft.gui.ViewPartRegistry;
import ru.biosoft.workbench.documents.RedoAction;
import ru.biosoft.workbench.documents.TextPaneUndoManager;
import ru.biosoft.workbench.documents.UndoAction;
import ru.biosoft.workbench.script.OutputViewPart;
import ru.biosoft.workbench.script.SwingScriptEnvironment;

public class JavaDocument extends Document
{
    protected static final Logger log = Logger.getLogger( JavaDocument.class.getName() );

    protected JavaPanel jPanel;

    protected int currentLine = 0;

//    private TaskInfo task;

    public JavaDocument(JavaElement javaElement)
    {
        super( javaElement, new TextPaneUndoManager() );

//        Dim dim = new Dim();
//        dim.attachTo( new JSDocumentContextFactory() );
        if( javaElement != null )
        {
//            try
//            {
//                dim.compileScript( jsElement.getName(), jsElement.getContent() );
//            }
//            catch( EvaluatorException ignore )
//            {
//
//            }

            jPanel = new JavaPanel( javaElement.getContent() );
//jPanel.setC
            viewPane = new ViewPane();
            viewPane.add( jPanel );

        }
        else
        {
            jPanel = new JavaPanel(null);// null, dim, null );
        }

        jPanel.addUndoableEditListener( getUndoManager() );
    }
    @Override
    public boolean isChanged()
    {
        return getUndoManager().canUndo();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Properties
    //

    public int getCurrentLine()
    {
        return currentLine;
    }

    public JPanel getJSPanel()
    {
        return jPanel;
    }

    public void setCurrentLine(int currentLine)
    {
        this.currentLine = currentLine;
    }

    public JavaElement getJSElement()
    {
        applyEditorChanges();
        return (JavaElement)getModel();
    }

    @Override
    public String getDisplayName()
    {
        JavaElement valueList = getJSElement();
        return valueList.getOrigin().getName() + " : " + valueList.getName();
    }

    private static boolean actionInitialized = false;
    @Override
    public Action[] getActions(ActionType actionType)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;

            ActionInitializer initializer = new ActionInitializer( MessageBundle.class, ru.biosoft.workbench.MessageBundle.class );

            //toolbar actions
            Action action = new UndoAction();
            actionManager.addAction( UndoAction.KEY, action );
            initializer.initAction( action, UndoAction.KEY );

            action = new RedoAction();
            actionManager.addAction( RedoAction.KEY, action );
            initializer.initAction( action, RedoAction.KEY );

            updateActionsState();
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action undoAction = actionManager.getAction( UndoAction.KEY );
            Action redoAction = actionManager.getAction( RedoAction.KEY );   
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

    // //////////////////////////////////////////////////////////////////////////
    // Update issues
    //

    @Override
    protected void doUpdate()
    {
    }

    @Override
    public boolean isMutable()
    {
        return getJSElement().getOrigin().isMutable();
    }

    @Override
    public void save()
    {
        JavaElement jsElement = getJSElement();
        String newData = jPanel.getText( false );
        jsElement.setContent( newData );
        try
        {
            CollectionFactoryUtils.save( jsElement );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Saving error", e );
        }
    }
    @Override
    public void close()
    {
        if( jPanel != null )
        {
            jPanel.removeUndoableEditListener( getUndoManager() );
        }
        super.close();
    }

    public String getText(boolean onlySelected)
    {
        return jPanel.getText( onlySelected );
    }

    public ScriptEnvironment getEnvironment()
    {
        OutputViewPart outputPane = (OutputViewPart)ViewPartRegistry.getViewPart( "script.output" );
        GUI.getManager().showViewPart( outputPane );
        return new SwingScriptEnvironment( outputPane.getTextPane() );
    }
}

