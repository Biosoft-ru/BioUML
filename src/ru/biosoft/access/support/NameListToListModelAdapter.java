package ru.biosoft.access.support;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;

@SuppressWarnings ( "serial" )
public class NameListToListModelAdapter extends AbstractListModel<String> implements DataCollectionListener
{
    private DataCollection<?> dataCollection;
    protected List<String> nameList;

    public NameListToListModelAdapter(DataCollection<?> dataCollection)
    {
        this.dataCollection = dataCollection;
        nameList = dataCollection.names().sorted().collect( Collectors.toList() );
        dataCollection.addDataCollectionListener(this);
    }

    public void close()
    {
        dataCollection.removeDataCollectionListener(this);
    }

    ////////////////////////////////////////////////////////////////////////////
    // implements ListModel interface
    //

    @Override
    public int getSize()
    {
        return nameList.size();
    }

    @Override
    public String getElementAt(int index)
    {
        if( index >= 0 )
            return nameList.get(index);
        return null;
    }

    public boolean contain(String name)
    {
        return nameList.contains("" + name);
    }

    ////////////////////////////////////////////////////////////////////////////
    // implements DataCollectionListener interface
    //

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        String name = e.getDataElement().getName();
        int index = Collections.binarySearch(nameList, name);
        if( index < 0 )
        {
            index = - ( index + 1 );
            nameList.add(index, name);

            fireIntervalAdded(this, index, index);
        }
    }

    private int index;
    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        index = Collections.binarySearch(nameList, e.getDataElement().getName());
    }
    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        if( index >= 0 )
        {
            nameList.remove(index);
            fireIntervalRemoved(this, index, index);
        }
    }

    // do nothing
    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }
    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
    }
    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }
}
