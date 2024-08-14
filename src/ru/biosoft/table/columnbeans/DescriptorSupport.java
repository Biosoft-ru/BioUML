package ru.biosoft.table.columnbeans;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.table.TableColumn;

public abstract class DescriptorSupport extends DataElementSupport implements Descriptor
{
    public DescriptorSupport( String name, DataCollection<?> origin )
    {
        super(name, origin);
    }
    
    @Override
    public ReferenceType getInputReferenceType()
    {
        return null;
    }

    @Override
    public TableColumn createColumn()
    {
        return new TableColumn(getName(), String.class);
    }
}
