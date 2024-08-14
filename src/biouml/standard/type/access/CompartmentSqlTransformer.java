package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.Compartment;

public class CompartmentSqlTransformer extends ConceptSqlTransformer<Compartment>
{
    @Override
    public boolean init(SqlDataCollection<Compartment> owner)
    {
        table = "compartments";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<Compartment> getTemplateClass()
    {
        return Compartment.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, completeName "
            + "FROM " + table;
    }

    @Override
    protected Compartment createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Compartment compartment = new Compartment(owner, resultSet.getString(1));
        compartment.setCompleteName(resultSet.getString(6));
        return compartment;
    }

    @Override
    protected String getSpecificFields(Compartment compartment)
    {
        return ", completeName";
    }

    @Override
    protected String[] getSpecificValues(Compartment compartment)
    {
        return new String[] { compartment.getCompleteName() };
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", "synonyms", table};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `compartments` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'compartment-cell'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_compartments_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
