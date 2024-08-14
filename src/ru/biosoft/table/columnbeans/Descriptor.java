package ru.biosoft.table.columnbeans;

import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Data element describes how to add new column to table.
 * Can be added as new column in {@link ColumnModel}
 */
@ClassIcon("resources/descriptor.gif")
@PropertyName("descriptor")
public interface Descriptor extends DataElement
{
    public ReferenceType getInputReferenceType();
    public Map<String, Object> getColumnValues(List<String> names) throws Exception;
    public TableColumn createColumn();
}
