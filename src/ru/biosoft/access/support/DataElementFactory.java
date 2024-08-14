package ru.biosoft.access.support;

import java.lang.reflect.Constructor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class DataElementFactory
{
    private static DataElementFactory instance = new DataElementFactory();
    
    public static DataElementFactory getInstance()
    {
        return instance;
    }
    
    public DataElement create(DataCollection<?> parent, String name) throws Exception
    {
        Class<? extends DataElement> c = parent.getDataElementType();

        Constructor<? extends DataElement> constructor;
        try
        {
            constructor = c.getConstructor( ru.biosoft.access.core.DataCollection.class, String.class );
        }
        catch( NoSuchMethodException e )
        {
            return c.getConstructor( String.class, DataCollection.class ).newInstance( name, parent );
        }
        return constructor.newInstance( parent, name );
    }
}
