package ru.biosoft.access.generic;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.Properties;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlDataElement;
import ru.biosoft.access.sql.SqlUtil;

public class DataElementSQLTypeDriver implements DataElementTypeDriver
{
    @Override
    public DataCollection createBaseCollection(GenericDataCollection gdc)
    {
        return gdc;
    }

    @Override
    public DataElement doGet(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        Class<? extends DataElement> elementClass = ClassLoading.loadSubClass( dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS), dei.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY), DataElement.class );
        Constructor<? extends DataElement> constructor = elementClass.getConstructor(DataCollection.class, Properties.class);
        Properties prop = dei.getProperties();
        prop.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, dei.getName());
        DataElement de = constructor.newInstance(gdc.getRealParent(), prop);
        return de;
    }

    @Override
    public void doPut(GenericDataCollection gdc, DataElement de, DataElementInfo dei) throws Exception
    {
        // Seems that nothing should be done
    }

    @Override
    public void doRemove(GenericDataCollection gdc, DataElementInfo dei) throws Exception
    {
        SqlDataElement de = (SqlDataElement)gdc.get(dei.getName());
        if(de == null) return;
        Connection conn = de.getConnection();
        try
        {
            for(String table: de.getUsedTables())
                SqlUtil.dropTable(conn, table);
        }
        catch(BiosoftSQLException e)
        {
            e.log();
        }
    }

    @Override
    public boolean isSupported(Class<? extends DataElement> childClass)
    {
        // TODO: remove implicit bsa dependency
        return SqlDataElement.class.isAssignableFrom(childClass)||childClass.getSimpleName().equals("Track")||childClass.getSimpleName().equals("WritableTrack");
    }

    @Override
    public boolean isLeafElement(GenericDataCollection gdc, DataElementInfo dei)
    {
        String className = dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS);
        if(className.contains("SqlTrack")) return false;
        return true;
    }

    @Override
    public long estimateSize(GenericDataCollection gdc, DataElementInfo dei, boolean recalc)
    {
        try
        {
            long size = 0;
            SqlDataElement element = (SqlDataElement)gdc.get(dei.getName());
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
}
