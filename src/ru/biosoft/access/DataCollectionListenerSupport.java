package ru.biosoft.access;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;

/**
 * Utility class that implements stub for ru.biosoft.access.core.DataCollectionListener.
 */
public class DataCollectionListenerSupport implements DataCollectionListener
{
    @Override
    public void elementAdded(DataCollectionEvent e) throws  Exception                                  {}
    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception    {}
    @Override
    public void elementChanged(DataCollectionEvent e) throws  Exception                                {}
    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception {}
    @Override
    public void elementRemoved(DataCollectionEvent e) throws  Exception                                {}
    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception {}
}

