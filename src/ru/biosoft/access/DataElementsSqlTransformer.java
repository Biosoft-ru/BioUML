package ru.biosoft.access;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;


/**
 * SqlTransformer which works with standard BioUML database
 */
public abstract class DataElementsSqlTransformer<T extends DataElement> extends SqlTransformerSupport<T>
{
    protected String completeName;

    @Override
    public void addInsertCommands(Statement statement, T de) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addUpdateCommands(Statement statement, T de) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getCountQuery()
    {
        return "SELECT count(*) FROM data_element WHERE parent=" + validateValue(completeName);
    }

    /**
     * Constructs data element by ID returned in Sql query
     * It supposes that child data element has constructor (DataCollection owner, Properties prop)
     * If so, then you just have to return proper type in getTemplateClass()
     * If not, then you must override this 'create' method
     * @see ru.biosoft.table.TablesSqlTransformer for implementation example
     */
    @SuppressWarnings ( "unchecked" )
    @Override
    public T create(ResultSet resultSet, Connection connection) throws Exception
    {
        Class<? extends DataElement> elementClass = getTemplateClass();
        Constructor<? extends DataElement> constructor = elementClass.getConstructor(DataCollection.class, Properties.class);
        if( constructor == null )
            return null;
        Properties prop = SqlDataInfo.getProperties(connection, resultSet.getInt(1));
        return (T)constructor.newInstance(owner, prop);
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT name FROM data_element WHERE parent=" + validateValue(completeName) + " AND name=" + validateValue(name);
    }

    @Override
    public String getElementQuery(String name)
    {
        return "SELECT id, name FROM data_element WHERE parent=" + validateValue(completeName) + " AND name=" + validateValue(name);
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT name FROM data_element WHERE parent=" + validateValue(completeName) + " ORDER BY name";
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id,name FROM data_element WHERE parent=" + validateValue(completeName) + " ORDER BY name";
    }

    @Override
    public String[] getUsedTables()
    {
        return null;
    }

    @Override
    public boolean init(SqlDataCollection<T> owner)
    {
        super.init(owner);
        this.completeName = owner.getCompletePath().toString();
        return true;
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals("data_element") )
        {
            return "CREATE TABLE IF NOT EXISTS `data_element` (" +
                    "  `id` int(11) NOT NULL auto_increment," +
                    "`name` varchar(255) default NULL," +
                    "`parent` text," +
                    "PRIMARY KEY  (`id`)," +
                    "KEY `name` (`name`(10))," +
                    "KEY `parent` (`parent`(80))" +
                    ") ENGINE=MyISAM";
        }
        else if( tableName.equals("de_info") )
        {
            return "CREATE TABLE IF NOT EXISTS `de_info` (" +
                    "  `id` int(11) NOT NULL auto_increment," +
                    "`data_element_id` int(11) NOT NULL," +
                    "`name` varchar(255) default NULL," +
                    "`value` text," +
                    "PRIMARY KEY  (`id`)," +
                    "UNIQUE KEY `data_element_id` (`data_element_id`,`name`)," +
                    "CONSTRAINT `de_info_id` FOREIGN KEY (`data_element_id`) REFERENCES `data_element` (`id`) ON DELETE CASCADE" +
                    ") ENGINE=MyISAM";
        }
        return null;
    }
}
