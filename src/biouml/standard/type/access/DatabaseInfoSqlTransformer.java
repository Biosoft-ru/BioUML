
package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.DatabaseInfo;

public class DatabaseInfoSqlTransformer extends ReferrerSqlTransformer<DatabaseInfo>
{
    @Override
    public boolean init(SqlDataCollection<DatabaseInfo> owner)
    {
        table = "dbInfos";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<DatabaseInfo> getTemplateClass()
    {
        return DatabaseInfo.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, " +
               " url, queryById, queryByAc " +
               "FROM " + table;
    }

    @Override
    protected DatabaseInfo createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        DatabaseInfo dbInfo = new DatabaseInfo(owner, resultSet.getString(1));

        // Database info specific fields
        dbInfo.setURL       (resultSet.getString(6));
        dbInfo.setQueryById (resultSet.getString(7));
        dbInfo.setQueryByAc (resultSet.getString(8));

        return dbInfo;
    }

    @Override
    protected String getSpecificFields(DatabaseInfo de)
    {
        return ", url, queryById, queryByAc";
    }

    @Override
    protected String[] getSpecificValues(DatabaseInfo de)
    {
        DatabaseInfo dbInfo = de;
        return new String[] { dbInfo.getURL(), dbInfo.getQueryById(), dbInfo.getQueryByAc() };
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", table};
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `dbInfos` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'info-database'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `url` varchar(100) default NULL," +
                    "  `attributes` text," +
                    "  `queryById` varchar(200) default NULL," +
                    "  `queryByAc` varchar(200) default NULL" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
