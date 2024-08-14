package biouml.workbench.diagram;

import java.awt.Point;
import java.util.List;

import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionUndoManager;

import biouml.model.Diagram;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.SelectionManager;
import ru.biosoft.graphics.editor.ViewEditorHelper;
import ru.biosoft.graphics.editor.ViewEditorPane;

/**
 * Stub for ViewEditorPane when working with Diagram without actual GUI frontend
 */
public class ViewEditorPaneStub extends ViewEditorPane
{
    protected Diagram diagram;
    protected TransactionUndoManager undoManager;
    ViewEditorHelper helper;

    public ViewEditorPaneStub(ViewEditorHelper helper, Diagram diagram)
    {
        this(helper, diagram, null);
    }

    public ViewEditorPaneStub(ViewEditorHelper helper, Diagram diagram, TransactionUndoManager undoManager)
    {
        super(helper);
        this.helper = helper;
        this.diagram = diagram;
        this.undoManager = undoManager;
    }

    @Override
    public CompositeView getView()
    {
        return (CompositeView)diagram.getView();
    }

    @Override
    public void fillToolbar(ViewEditorHelper helper)
    {
        //nothing to do
    }

    @Override
    public void setSelectionManager(SelectionManager selectionManager)
    {
        //nothing to do
    }

    @Override
    synchronized public void add(Object obj, Point point)
    {
        if( undoManager != null )
            undoManager.startTransaction(new TransactionEvent(diagram, "Add"));
        helper.add(obj, point);
        if( undoManager != null )
            undoManager.completeTransaction();
    }

    @Override
    synchronized public void add(List<?> objects, Point point)
    {
        if( undoManager != null )
            undoManager.startTransaction(new TransactionEvent(diagram, "Add elements"));
        for( Object obj : objects )
            helper.add(obj, point);
        if( undoManager != null )
            undoManager.completeTransaction();
    }

    @Override
    public void startTransaction(String name)
    {
        if(undoManager != null)
            undoManager.startTransaction(new TransactionEvent(diagram, name));
    }

    @Override
    public void startTransaction(TransactionEvent event)
    {
        if(undoManager != null)
            undoManager.startTransaction(event);
    }

    @Override
    public void completeTransaction()
    {
        if(undoManager != null)
            undoManager.completeTransaction();
    }

    @Override
    protected void initUIComponents()
    {
        //do nothing
    }
}
