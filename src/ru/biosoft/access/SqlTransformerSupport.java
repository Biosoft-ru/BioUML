package ru.biosoft.access;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlUtil;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSerializer;


abstract public class SqlTransformerSupport<T extends DataElement> implements SqlTransformer<T>
{
    protected SqlDataCollection<T> owner;
    protected String idField = "id";
    protected String table;
    
    @Override
    public boolean isNameListSorted()
    {
        return false;
    }

    @Override
    public boolean init(SqlDataCollection<T> owner)
    {
        this.owner = owner;
        return true;
    }

    public String getTable()
    {
        return table;
    }

    public String getIdField()
    {
        return idField;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(*) FROM " + table;
    }
    
    @Override
    public String getSelectQuery()
    {
        return "SELECT * FROM " + table;
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " ORDER BY " + idField;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT " + idField + " FROM " + table + " WHERE " + idField + "=" + validateValue(name);
    }

    @Override
    public String getElementQuery(String name)
    {
        String query = getSelectQuery();
        if( query.contains(" WHERE ") )
        {
            return query + " AND " + idField + "=" + validateValue(name);
        }
        return query + " WHERE " + idField + "=" + validateValue(name);
    }

    @Override
    public String[] getUsedTables()
    {
        return null;
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        return null;
    }

    protected String validateValue(String value)
    {
        return SqlUtil.quoteString(value);
    }
    
    protected Connection getConnection()
    {
        return owner.getConnection();
    }

    /**
     * Adds set of SQL commands to the statement to update data element in the table.
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which UPDATE statements will be generated.
     */
    @Override
    public void addUpdateCommands(Statement statement, T de) throws Exception
    {
        addDeleteCommands(statement, de.getName());
        addInsertCommands(statement, de);
    }

    /**
     * Adds set of SQL commands to the statement to remove data element from the table.
     * @param statement - statement to which SQL commands should be added.
     * @param de - object for which DELETE statements will be generated.
     */
    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        statement.addBatch("DELETE FROM " + table + " WHERE " + idField + "=" + validateValue(name));
    }
    
    
    /**
     * Manage DynamicPropertySet attributes.
     * Attributes are stored as xml in attributes column of the table.
     */
    protected String getAttributesString(DynamicPropertySet attributes)
    {
        String attrString = null;
        try
        {
            if( attributes != null && attributes.size() > 0 )
            {
                OutputStream output = new OutputStream()
                {
                    private StringBuilder string = new StringBuilder();
                    @Override
                    public void write(int b) throws IOException
                    {
                        this.string.append((char)b);
                    }

                    @Override
                    public String toString()
                    {
                        return this.string.toString();
                    }
                };
                ( new DynamicPropertySetSerializer() ).save(output, attributes);
                attrString = output.toString();
            }
        }
        catch( Exception e )
        {
        }
        return attrString;
    }
    
    protected void addInsertAttributesCommand(Statement statement, String name, String attrStr)
    {
        try
        {
            if( attrStr != null )
            {
                String query = "UPDATE " + table + " SET attributes= " + validateValue(attrStr) + " WHERE " + idField + "="
                        + validateValue(name);
                statement.addBatch(query);
            }
        }
        catch( Exception e )
        {
        }
    }
    
    protected DynamicPropertySet getAttributes(Connection connection, String name, DynamicPropertySet dps)
    {
        try
        {
            String attrStr = SqlUtil.queryString(connection, "SELECT attributes FROM " + table + " WHERE " + idField + "=" + validateValue(name));
            if(attrStr != null)
            {
                return loadAttributes(attrStr, dps);
            }
        }
        catch( Exception e )
        {
        }
        return null;
    }
    
    protected void checkAttributesColumn(SqlConnectionHolder connectionHolder)
    {
        checkAttributesColumn(connectionHolder, table);
    }

    protected void checkAttributesColumn(SqlConnectionHolder connectionHolder, String tableName)
    {
        Statement st = null;
        ResultSet resultSet = null;
        try
        {
            Connection connection = connectionHolder.getConnection();
            boolean tableExists = false;
            boolean attributesExist = false;
            st = connection.createStatement();
            try
            {
                resultSet = st.executeQuery("DESC " + tableName);
                tableExists = true;
                while( resultSet.next() )
                {
                    String name = resultSet.getString("Field");
                    if( name.equals("attributes") )
                    {
                        attributesExist = true;
                        break;
                    }
                }
            }
            catch( SQLException e )
            {
                //No table exist, will be created with attributes column
            }
            if( tableExists && !attributesExist )
            {
                st.execute("ALTER TABLE " + tableName + " ADD COLUMN `attributes` text");
            }
        }
        catch( Exception e )
        {
        }
        finally
        {
            SqlUtil.close(st, resultSet);
        }
    }
    
    protected DynamicPropertySet loadAttributes(String attrStr, DynamicPropertySet dps)
    {
        if( attrStr != null && !attrStr.isEmpty() )
        {
            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());
            ( new DynamicPropertySetSerializer() ).load(dps, new ByteArrayInputStream(attrStr.getBytes()), ClassLoading.getClassLoader());
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
            return dps;
        }
        return null;
    }

    @Override
    public boolean isSortingSupported()
    {
        return false;
    }

    @Override
    public String[] getSortableFields()
    {
        return null;
    }

    @Override
    public String getSortedNameListQuery(String field, boolean direction)
    {
        return getNameListQuery();
    }
}
