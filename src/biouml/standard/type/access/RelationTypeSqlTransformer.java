
package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ru.biosoft.access.SqlTransformerSupport;
import biouml.standard.type.RelationType;

public class RelationTypeSqlTransformer extends SqlTransformerSupport<RelationType>
{
    public RelationTypeSqlTransformer()
    {
        table = "relationTypes";
    }

    @Override
    public Class<RelationType> getTemplateClass()
    {
        return RelationType.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, title, description, comment, stroke FROM " + table;
    }

    @Override
    public RelationType create(ResultSet resultSet, Connection connection) throws Exception
    {
        RelationType type = new RelationType(owner, resultSet.getString(1));

        type.setTitle       (resultSet.getString(2));
        type.setDescription (resultSet.getString(3));
        type.setComment     (resultSet.getString(4));
        type.setStroke      (resultSet.getString(5));

        return type;
    }

    @Override
    public void addInsertCommands(Statement statement, RelationType type) throws SQLException
    {
        statement.addBatch(
            "INSERT INTO " + table +
            " (id, title, description, comment, stroke) VALUES(" +
            validateValue(type.getName()) + ", " +
            validateValue(type.getTitle()) + ", " +
            validateValue(type.getDescription()) + ", " +
            validateValue(type.getComment()) + ", " +
            validateValue(type.getStroke()) + ")" );
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {table};
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `relationTypes` (" +
                    "  `ID` varchar(60) NOT NULL default ''," +
                    "  `title` varchar(30) NOT NULL default ''," +
                    "  `description` text," +
                    "  `comment` varchar(200) default NULL," +
                    "  `stroke` text" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
