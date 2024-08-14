package ru.biosoft.access.generic2;

import java.io.File;
import java.sql.Connection;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlDataElement;
import ru.biosoft.access.sql.SqlUtil;

public class GenericSQLTypeDriver extends GenericElementTypeDriver
{
    @Override
    public DataElement doGet(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        try
        {
            return getElementClass( properties ).getConstructor(DataCollection.class, Properties.class).newInstance(gdc, properties);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    public void doRemove(GenericDataCollection2 gdc, File folder, Properties properties) throws LoggedException
    {
        DataElement de = gdc.getFromCache( properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ) );
        if(de == null)
            de = doGet(gdc, folder, properties);
        SqlDataElement sde = de.cast( SqlDataElement.class );
        Connection conn = sde.getConnection();
        for(String table: sde.getUsedTables())
            SqlUtil.dropTable(conn, table);
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        // TODO: remove implicit bsa dependency
        return SqlDataElement.class.isAssignableFrom(childClass)||childClass.getSimpleName().equals("Track")||childClass.getSimpleName().equals("WritableTrack");
    }

    @Override
    public boolean isLeaf(GenericDataCollection2 gdc, Properties properties)
    {
        String className = properties.getProperty( DataCollectionConfigConstants.CLASS_PROPERTY );
        return !className.contains("SqlTrack");
    }

    @Override
    public long estimateSize(GenericDataCollection2 gdc, File folder, Properties properties, boolean recalc)
    {
        try
        {
            long size = 0;
            SqlDataElement element = doGet(gdc, folder, properties).cast( SqlDataElement.class );
            for(String table: element.getUsedTables())
            {
                size+=SqlUtil.getTableSize(element.getConnection(), table);
            }
            return size;
        }
        catch( Exception e )
        {
        }
        return -1;
    }

    @Override
    protected void doPut(GenericDataCollection2 gdc, File folder, DataElement de, Properties properties) throws LoggedException
    {
        // Nothing should be done here
    }
}
