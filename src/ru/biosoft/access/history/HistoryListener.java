package ru.biosoft.access.history;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

/**
 * History data collection listener
 */
public class HistoryListener extends DataCollectionListenerSupport
{
    protected HistoryDataCollection historyDC;

    public HistoryListener(HistoryDataCollection historyDC)
    {
        this.historyDC = historyDC;
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        if( e.getOldElement() == null )
        {
            //log.log(Level.SEVERE, "History: previous element version is not available");
            return;
        }

        if( e.getPrimaryEvent() != null )
        {
            return;//ignore propagated events
        }

        DataElement newElement = e.getOwner().get(e.getDataElementName());
        HistoryElement historyElement = HistoryFacade.getDifference(historyDC.getNextID(),
                historyDC.getNextVersion(DataElementPath.create(newElement)), e.getOldElement(), newElement);
        if( historyElement != null )
        {
            historyDC.put(historyElement);
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent e)
    {
        //TODO: save object to history
    }
}
