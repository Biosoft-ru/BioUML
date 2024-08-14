
package ru.biosoft.access.support;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementReadException;
import com.developmentontheedge.beans.swing.table.AbstractRowModel;

public class DataCollectionRowModelAdapter extends AbstractRowModel implements DataCollectionListener
{
    protected static final Logger cat = Logger.getLogger(DataCollectionRowModelAdapter.class.getName());
    private DataCollection<?> dc = null;

    public DataCollectionRowModelAdapter(DataCollection dc)
    {
        this.dc = dc;
        registerListener();
    }

    public void registerListener()
    {
        unregisterListener();
        if (dc != null)
        {
            dc.addDataCollectionListener(this);
        }
    }

    public void unregisterListener()
    {
        if (dc != null)
        {
            dc.removeDataCollectionListener(this);
        }
    }

    ///////////////////////////////////////////////////////
    //
    // RowModel implementation
    //

    public DataCollection<?> getDataCollection()
    {
        return dc;
    }

    /** Returns a bean at the index. */
    @Override
    public Object getBean(int index)
    {
        Object bean = null;

        String name = null;
        try
        {
            name = dc.getNameList().get(index);
            bean = dc.get(name);
        }
        catch(LoggedException e)
        {
            throw e;
        }
        catch(Throwable e)
        {
            throw new DataElementReadException(e, dc, "row#" + index);
        }
        if(bean == null)
            throw new DataElementNotFoundException(dc.getCompletePath().getChildPath(name));
        return bean;
    }

    /**
     * Returns a number of beans in the model.
     * A <code>TabularPropertyInspector</code> uses this method to determine how many rows
     * it should display. This method should be quick, as it is called frequently during rendering.
     */
    @Override
    public int size()
    {
        return dc != null ? dc.getSize() : 0;
    }

    ///////////////////////////////////////////////////////
    //
    // ru.biosoft.access.core.DataCollectionListener implementation
    //

    private int willRemovedIdx;

    /**
     * Called before data element will be removed.
     * @param e DataCollectionEvent information about will removed data element.
     * @throws DataCollectionVetoException If listener cancel removing of data element.
     * @throws Exception If error occured.
     */
    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        String name  = e.getDataElementName();
        willRemovedIdx = dc.getNameList().lastIndexOf(name);
    }

    /**
     * Called after data element was added.
     * @param e DataCollectionEvent information about added data element.
     * @throws Exception If error occured.
     */
    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        String name  = e.getDataElementName();
        int idx = dc.getNameList().lastIndexOf(name);
        fireTableRowsInserted(idx,idx);
    }

    /**
     * Called before data element will be added.
     * @param e DataCollectionEvent information about will added data element.
     * @throws DataCollectionVetoException If listener cancel adding of data element.
     * @throws Exception If error occured.
     */
    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException,Exception
    {
    }

    /**
     * Called after data element was changed.
     * <code>e</code> contains old data element (which already changed).
     * @param e DataCollectionEvent information about changed data element.
     * @throws Exception If error occured.
     */
    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        String name  = e.getDataElementName();
        int idx = dc.getNameList().lastIndexOf(name);
        fireTableRowsUpdated(idx,idx);
    }

    /**
     * Called before data element will be changed.
     * <code>e</code> contains old data element (which will be changed).
     * @param e DataCollectionEvent information about will change data element.
     * @throws DataCollectionVetoException If listener cancel changing of data element.
     * @throws Exception If error occured.
     */
    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException,Exception
    {
    }

    /**
     * Called after data element was removed.
     * @param e DataCollectionEvent information about removed data element.
     * @throws Exception If error occured.
     */
    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        fireTableRowsDeleted(willRemovedIdx, willRemovedIdx);
    }

    public void cleanup()
    {
        unregisterListener();
        if (dc != null)
        {
            try
            {
                dc.close();
            }
            catch ( Exception e )
            {
                cat.log(Level.FINE, "Closing data collection error", e);
            }
            //dc = null;
        }
    }
}

