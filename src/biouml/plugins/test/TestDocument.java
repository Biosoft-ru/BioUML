package biouml.plugins.test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.undo.PropertyChangeUndo;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;

import biouml.plugins.test.TableTestModelWrapper.TestRow;
import biouml.plugins.test.tests.Test;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.SaveDocumentAction;

/**
 * Document for integration model and a set of {@link AcceptanceTestSuite}
 */
public class TestDocument extends Document implements PropertyChangeListener
{
    protected static final Logger log = Logger.getLogger(TestDocument.class.getName());

    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected TableTestModelWrapper tableWrapper;
    protected TabularPropertyInspector table;

    public TestDocument(TestModel model)
    {
        super(model);

        Properties prop = new Properties();
        prop.put(DataCollectionConfigConstants.NAME_PROPERTY, "testModelTable");
        tableWrapper = new TableTestModelWrapper(model, prop);
        model.addPropertyChangeListener(this);

        table = new TabularPropertyInspector();
        table.setSortEnabled(false);
        table.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setVariableRowHeight(true);

        viewPane = new ViewPane();
        viewPane.add(table);

        listenerList.add(TransactionListener.class, undoManager);

        refreshPane();
    }

    public void addSelectionListener(ListSelectionListener listener)
    {
        table.removeListSelectionListener(listener);
        table.addListSelectionListener(listener);
    }

    public AcceptanceTestSuite getSelectedTestSuite()
    {
        Object selectedModel = table.getModelOfSelectedRow();
        if( ( selectedModel instanceof TestRow ) )
        {
            return ( (TestRow)selectedModel ).getTestSuite();
        }
        return null;
    }

    public Test getSelectedTest()
    {
        Object selectedModel = table.getModelOfSelectedRow();
        if( ( selectedModel instanceof TestRow ) && ! ( (TestRow)selectedModel ).isTestSuite() )
        {
            return ( (TestRow)selectedModel ).getTest();
        }
        return null;
    }

    @Override
    public String getDisplayName()
    {
        return ( (TestModel)getModel() ).getName();
    }

    @Override
    protected void doUpdate()
    {
        refreshPane();
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        fireStartTransaction(new TransactionEvent(getModel(), "Test model changed"));
        fireAddEdit(new PropertyChangeUndo(pce));
        fireCompleteTransaction();

        tableWrapper.updateModel();
        refreshPane();

        if( Document.getActiveDocument() == this )
        {
            if( isMutable() )
            {
                Application.getActionManager().enableActions( true, SaveDocumentAction.KEY );
            }
        }
    }

    @Override
    public boolean isMutable()
    {
        return ( (TestModel)getModel() ).getOrigin().isMutable();
    }

    @Override
    public void save()
    {
        try
        {
            TestModel testModel = (TestModel)getModel();
            CollectionFactoryUtils.save( testModel );

            for( AcceptanceTestSuite testSuite : testModel.getChangedTestSuites() )
            {
                CollectionFactoryUtils.save( testSuite );
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, messageBundle.getString("ERROR_MODEL_SAVING"), e);
        }

        super.save();
    }


    protected void refreshPane()
    {
        table.explore(tableWrapper.iterator());
        table.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }
    ////////////////////////////////////////////////////////////////////////////
    // Transaction issues
    //

    protected EventListenerList listenerList = new EventListenerList();

    protected void fireStartTransaction(TransactionEvent evt)
    {
        Object[] listeners = listenerList.getListenerList();
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == TransactionListener.class )
                ( (TransactionListener)listeners[i + 1] ).startTransaction(evt);
        }
    }

    protected void fireAddEdit(UndoableEdit ue)
    {
        Object[] listeners = listenerList.getListenerList();
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == TransactionListener.class )
                ( (TransactionListener)listeners[i + 1] ).addEdit(ue);
        }
    }

    protected void fireCompleteTransaction()
    {
        Object[] listeners = listenerList.getListenerList();
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == TransactionListener.class )
                ( (TransactionListener)listeners[i + 1] ).completeTransaction();
        }
    }
}
