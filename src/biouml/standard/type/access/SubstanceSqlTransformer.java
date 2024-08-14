package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.Substance;

public class SubstanceSqlTransformer extends MoleculeSqlTransformer<Substance>
{
    @Override
    public boolean init(SqlDataCollection<Substance> owner)
    {
        table = "substances";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<Substance> getTemplateClass()
    {
        return Substance.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, completeName, formula, casNumber " + "FROM " + table;
    }

    @Override
    protected String getSpecificFields(Substance de)
    {
        return ", completeName, formula, casNumber";
    }

    @Override
    protected String[] getSpecificValues(Substance de)
    {
        return new String[] { de.getCompleteName(), de.getFormula(), de.getCasRegistryNumber()};
    }

    @Override
    protected Substance createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Substance substance = new Substance(owner, resultSet.getString(1));
        substance.setCompleteName(resultSet.getString(6));
        substance.setFormula(resultSet.getString(7));
        substance.setCasRegistryNumber(resultSet.getString(8));

        String[] structures = SqlUtil.stringStream(connection, "SELECT structureID FROM structure2molecule " + "WHERE moleculeID="
                + validateValue(resultSet.getString(1))).toArray(String[]::new);
        substance.setStructureReferences( structures );

        return substance;
    }

    @Override
    public void addInsertCommands(Statement statement, Substance de) throws Exception
    {
        super.addInsertCommands(statement, de);
        addStructureReferences(statement, de);
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        super.addDeleteCommands(statement, name);
        removeStructureReferences(statement, name);
    }

    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", "synonyms", "structure2molecule", table};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `substances` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'molecule-substance'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(250) default NULL," +
                    "  `description` text," + "  `comment` text," +
                    "  `casNumber` varchar(40) default NULL," +
                    "  `formula` varchar(100) default NULL," +
                    "  `attributes` longtext," +
                    "  UNIQUE KEY `IDX_UNIQUE_substances_ID` (`ID`)" + ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
