
package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.Structure;

public class StructureSqlTransformer extends ReferrerSqlTransformer<Structure>
{
    @Override
    public boolean init(SqlDataCollection<Structure> owner)
    {
        table = "structures";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<Structure> getTemplateClass()
    {
        return Structure.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, " + " format, data " + "FROM " + table;
    }

    @Override
    protected Structure createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Structure structure = new Structure(owner, resultSet.getString(1));

        // Structure info specific fields
        structure.setFormat(resultSet.getString(6));
        structure.setData(resultSet.getString(7));

        return structure;
    }

    @Override
    protected String getSpecificFields(Structure de)
    {
        return ", format, data";
    }

    @Override
    protected String[] getSpecificValues(Structure structure)
    {
        return new String[] {structure.getFormat(), structure.getData()};
    }

    @Override
    public void addInsertCommands(Statement statement, Structure structure) throws Exception
    {
        super.addInsertCommands(statement, structure);

        // add molecule references
        String[] refs = structure.getMoleculeReferences();
        if( refs != null )
        {
            for( int i = 0; i < refs.length; i++ )
            {
                statement.addBatch("INSERT INTO structureReferences (structureId, moleculeId)" + "VALUES(" + validateValue(structure.getName()) + ", "
                        + validateValue(refs[i].trim()) + ")");
            }
        }
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        super.addDeleteCommands(statement, name);
        statement.addBatch("DELETE FROM structureReferences WHERE structureId=" + validateValue(name));
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
            return "CREATE TABLE `structures` (" + getIDFieldFormat() + "," + "  `type` varchar(50) default '',"
                    + getTitleFieldFormat()+ "," + "  `format` varchar(10) NOT NULL default '',"
                    + "  `description` text," + "  `comment` text," + "  `data` text" +
                    "  `attributes` text,"+
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
