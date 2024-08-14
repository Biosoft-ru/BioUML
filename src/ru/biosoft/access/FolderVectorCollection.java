package ru.biosoft.access;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.VectorDataCollection;

public class FolderVectorCollection extends VectorDataCollection<DataElement> implements FolderCollection
{
    public FolderVectorCollection(String name, DataCollection<?> origin)
    {
        super( name, DataElement.class, origin );
    }

    @Override
    public DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz)
    {
        try
        {
            FolderVectorCollection folder = new FolderVectorCollection( name, this );
            put(folder);
            return folder;
        }
        catch( Exception e )
        {
            throw new DataElementCreateException( getCompletePath().getChildPath( name ) );
        }
    }
}
