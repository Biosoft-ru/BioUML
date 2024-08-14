package ru.biosoft.table.document;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.undo.PropertyChangeUndo;
import com.developmentontheedge.beans.undo.Transactable;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.repository.DataElementImportTransferHandler;
import ru.biosoft.access.subaction.DynamicActionFactory;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.RedoAction;
import ru.biosoft.gui.UndoAction;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.Descriptor;

@ClassIcon ( "resources/tableDocument.gif" )
public class TableDocument extends Document implements PropertyChangeListener, Transactable, DataCollectionListener
{
    protected ValuesPane valuesPane;

    public TableDocument(final DataCollection<?> tableData)
    {
        super(tableData);

        if( tableData != null )
        {
            if( tableData instanceof TableDataCollection )
            {
                ( (TableDataCollection)tableData ).addPropertyChangeListener(this);
            }
            else
            {
                tableData.addDataCollectionListener(this);
            }

            valuesPane = new ValuesPane(getTableData());
            valuesPane.setDocument(this);
            viewPane = new ViewPane();
            viewPane.add(valuesPane);

            valuesPane.setTransferHandler(new DataElementImportTransferHandler((path, point) -> {
                DataElement de = path.optDataElement();
                if(de instanceof Descriptor && tableData instanceof TableDataCollection)
                {
                    TableDataCollection tdc = ((TableDataCollection)tableData);
                    TableColumn column = tdc.getColumnModel().addColumn((Descriptor)de);
                    tdc.recalculateColumn(column.getName());
                    return true;
                }
                return false;
            }));

            addTransactionListener(undoManager);
        }
    }

    // //////////////////////////////////////////////////////////////////////////
    // Properties
    //

    public DataCollection<?> getTableData()
    {
        applyEditorChanges();
        return (DataCollection<?>)getModel();
    }

    @Override
    public String getDisplayName()
    {
        DataCollection<?> valueList = getTableData();
        if( valueList.getOrigin() != null )
        {
            return valueList.getOrigin().getName() + " : " + valueList.getName();
        }
        return valueList.getName();
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
            Action action = new UndoAction(false);
            actionManager.addAction(UndoAction.KEY, action);
            initializer.initAction(action, UndoAction.KEY);

            action = new RedoAction(false);
            actionManager.addAction(RedoAction.KEY, action);
            initializer.initAction(action, RedoAction.KEY);
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            return StreamEx.of( actionManager.getAction( UndoAction.KEY ), actionManager.getAction( RedoAction.KEY ), null )
                    .append( DynamicActionFactory.getEnabledActions( getModel() ) ).toArray( Action[]::new );
        }
        return null;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Update issues
    //

    @Override
    protected void doUpdate()
    {
        valuesPane.setValueList(getTableData());
    }

    @Override
    public void save()
    {
        DataCollection<?> tableData = getTableData();
        try
        {
            CollectionFactoryUtils.save(tableData);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Saving table data collection error", e);
        }

        super.save();
    }

    public void setRowFilter(Filter rowFilter)
    {
        valuesPane.setRowFilter(rowFilter);
    }
    
    public Filter getRowFilter()
    {
        return valuesPane.getRowFilter();
    }

    public DataCollection<?> getFilteredCollection()
    {
        return valuesPane.getFilteredTable();
    }

    public DataCollection<?> getCurrentFilteredCollection()
    {
        return valuesPane.getCurrentFilteredTable();
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if( log.isLoggable( Level.FINE ) )
            log.log(Level.FINE, "Property changed: " + pce.getPropertyName() + ", propagated by " + pce.getPropagationId());

        fireStartTransaction(new TransactionEvent(getModel(), "Change"));
        fireAddEdit(new PropertyChangeUndo(pce));
        fireCompleteTransaction();
    }

    @Override
    public boolean isMutable()
    {
        if( getTableData().getOrigin() != null )
        {
            return getTableData().getOrigin().isMutable();
        }
        return true;
    }

    @Override
    public List<DataElement> getSelectedItems()
    {
        List<DataElement> selected = new ArrayList<>();
        for( Object row : valuesPane.getSelectedRows() )
        {
            selected.add((DataElement)row);
        }
        return selected;
    }

    @Override
    public void close()
    {
        DataCollection<?> valueList = getTableData();

        if( valueList instanceof TableDataCollection )
        {
            ( (TableDataCollection)valueList ).removePropertyChangeListener(this);
        }
        valuesPane.setDocument(null);
        removeTransactionListener(undoManager);

        if( valueList.getOrigin() != null )
        {
            valueList.getOrigin().release(valueList.getName());
        }

        super.close();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Transactable interface implementation
    //

    protected EventListenerList listenerList = new EventListenerList();

    @Override
    public void addTransactionListener(TransactionListener listener)
    {
        listenerList.add(TransactionListener.class, listener);
    }

    @Override
    public void removeTransactionListener(TransactionListener listener)
    {
        listenerList.remove(TransactionListener.class, listener);
    }

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

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        propertyChange(new PropertyChangeEvent(e.getOwner(), e.getDataElementName(), null, e.getDataElement()));
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        propertyChange(new PropertyChangeEvent(e.getOwner(), e.getDataElementName(), null, e.getDataElement()));
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        propertyChange(new PropertyChangeEvent(e.getOwner(), e.getDataElementName(), null, e.getDataElement()));
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    //Property change event handling
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        valuesPane.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        valuesPane.removePropertyChangeListener(l);
    }

    //Selection listener
    public void addSelectionListener(ListSelectionListener listener)
    {
        valuesPane.addSelectionListener(listener);
    }
    
    public void removeSelectionListener(ListSelectionListener listener)
    {
        valuesPane.removeSelectionListener(listener);
    }

}
