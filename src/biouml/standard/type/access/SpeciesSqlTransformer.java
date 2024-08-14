
package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ru.biosoft.access.SqlTransformerSupport;
import biouml.standard.type.Species;

public class SpeciesSqlTransformer extends SqlTransformerSupport<Species>
{
    public SpeciesSqlTransformer()
    {
        table = "species";
    }

    @Override
    public Class<Species> getTemplateClass()
    {
        return Species.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, name, abreviation, description FROM " + table;
    }

    @Override
    public Species create(ResultSet resultSet, Connection connection) throws Exception
    {
        Species species = new Species(owner, resultSet.getString(1));

        species.setCommonName  (resultSet.getString(2));
        species.setAbbreviation(resultSet.getString(3));
        species.setDescription (resultSet.getString(4));

        return species;
    }

    @Override
    public void addInsertCommands(Statement statement, Species species) throws SQLException
    {
        statement.addBatch(
            "INSERT INTO " + table +
            " (id, name, abreviation, description ) VALUES(" +
            validateValue(species.getName()) + ", " +
            validateValue(species.getCommonName()) + ", " +
            validateValue(species.getAbbreviation()) + ", " +
            validateValue(species.getDescription()) + ")" );
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
            return "CREATE TABLE `species` (" +
                    "  `ID` varchar(100) NOT NULL default ''," +
                    "  `name` varchar(100) default NULL," +
                    "  `abreviation` varchar(5) default NULL," +
                    "  `description` text," +
                    "  UNIQUE KEY `IDX_UNIQUE_species_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
