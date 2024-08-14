package ru.biosoft.table;

import java.sql.Statement;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataElementsSqlTransformer;
import ru.biosoft.access.SqlDataInfo;

public class TablesSqlTransformer extends DataElementsSqlTransformer<TableDataCollection>
{
    @Override
    public void addInsertCommands(Statement statement, TableDataCollection de)
            throws Exception {
        if(de instanceof SqlTableDataCollection && de.getOrigin().getCompletePath().equals(owner.getCompletePath())) return;
        if(de != null)
        {
            Properties prop = new Properties();
            prop.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, de.getName());
            owner.put(new SqlTableDataCollection(owner, prop, de));
        } else
            throw new Exception("This element type is not supported");
    }

    @Override
    public void addUpdateCommands(Statement statement, TableDataCollection de)
            throws Exception {
        if(de instanceof SqlTableDataCollection && de.getOrigin().getCompletePath().equals(owner.getCompletePath())) return;
        if(de != null)
        {
            Properties prop = new Properties();
            prop.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, de.getName());
            prop.setProperty(SqlDataInfo.ID_PROPERTY, String.valueOf(SqlDataInfo.getIdByName(statement.getConnection(), DataElementPath
                    .create(owner, de.getName()).toString())));
            owner.put(new SqlTableDataCollection(owner, prop, de));
        } else
            throw new Exception("This element type is not supported");
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        DataElement de = owner.get(name);
        if(!(de instanceof SqlTableDataCollection)) return;
        SqlTableDataCollection table = (SqlTableDataCollection)de;
        int id = table.getId();
        statement.addBatch("DELETE FROM data_element WHERE id = "+id);
        statement.addBatch("DELETE FROM column_info WHERE data_element_id = "+id);
        statement.addBatch("DELETE FROM de_info WHERE data_element_id = "+id);
        statement.addBatch("DROP TABLE IF EXISTS "+table.getTableId());
    }

    @Override
    public Class getTemplateClass()
    {
        return SqlTableDataCollection.class;
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"data_element", "column_info", "de_info"};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals("column_info") )
        {
            return "CREATE TABLE IF NOT EXISTS `column_info` (" +
                    "    `id` int(11) NOT NULL auto_increment," +
                    "`data_element_id` int(11) NOT NULL," +
                    "`name` varchar(255) NOT NULL," +
                    "`display_name` varchar(255) NOT NULL default ''," +
                    "`description` varchar(255) NOT NULL default ''," +
                    "`type` varchar(32) NOT NULL default 'Text'," +
                    "`expression` varchar(255) default NULL," +
                    "`nature` varchar(32) NOT NULL default 'NONE'," +
                    "`position` int(11) NOT NULL," +
                    "PRIMARY KEY  (`id`)," +
                    "UNIQUE KEY `data_element_id` (`data_element_id`,`name`)," +
                    "CONSTRAINT `column_info_ibfk_1` FOREIGN KEY (`data_element_id`) REFERENCES `data_element` (`id`) ON DELETE CASCADE" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
