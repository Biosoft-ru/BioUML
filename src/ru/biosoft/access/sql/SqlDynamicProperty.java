package ru.biosoft.access.sql;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.List;

import java.util.logging.Logger;

import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.beans.DynamicProperty;

/**
 * Creates read-only dynamic property which value is the result of SQL query (either String or String[])
 * Query will be actually executed only when getValue is called
 * @author lan
 */
public class SqlDynamicProperty extends DynamicProperty
{
    private static final Logger log = Logger.getLogger(SqlDynamicProperty.class.getName());

    private SqlConnectionHolder holder;
    private String query;
    private volatile boolean initialized = false;

    public SqlDynamicProperty(String name, SqlConnectionHolder holder, String query, boolean isArray) throws IntrospectionException
    {
        this(new PropertyDescriptor(name, null, null), holder, query, isArray);
    }

    public SqlDynamicProperty(PropertyDescriptor descriptor, SqlConnectionHolder holder, String query, boolean isArray)
    {
        super(descriptor, isArray?String[].class:String.class);
        this.holder = holder;
        this.query = query;
    }

    @Override
    public Object getValue()
    {
        if(!initialized)
        {
            synchronized(this)
            {
                if(!initialized)
                {
                    try
                    {
                        if(getType() == String.class)
                            value = SqlUtil.queryString(holder.getConnection(), query);
                        else
                        {
                            List<String> list = SqlUtil.queryStrings(holder.getConnection(), query);
                            value = list.toArray(new String[list.size()]);
                        }
                    }
                    catch( BiosoftSQLException e )
                    {
                        log.log(Level.SEVERE, "Cannot fetch SqlValue for "+getName()+": "+ExceptionRegistry.log(e));
                        value = "";
                    }
                    // Release unnecessary objects
                    holder = null;
                    query = null;
                    initialized = true;
                }
            }
        }
        return super.getValue();
    }

    @Override
    public void setValue(Object value)
    {
        // Ignore
    }
}
