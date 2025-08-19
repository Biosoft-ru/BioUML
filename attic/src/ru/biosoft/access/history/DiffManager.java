package ru.biosoft.access.history;

import ru.biosoft.access.core.DataElement;

/**
 * Basic interface for difference builder
 */
public interface DiffManager
{
    public int getPriority(Class elementType);
    public void fillDifference(HistoryElement historyElement, DataElement oldElement, DataElement newElement) throws Exception;
    public Object parseDifference(DataElement de, HistoryElement historyElement) throws Exception;
    public DataElement applyDifference(DataElement de, HistoryElement[] historyElements) throws Exception;
    public DataElement getDifferenceElement(DataElement first, DataElement second) throws Exception;
}
