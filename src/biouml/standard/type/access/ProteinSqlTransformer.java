package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.Protein;

public class ProteinSqlTransformer extends BiopolymerSqlTransformer<Protein>
{
    @Override
    public boolean init(SqlDataCollection<Protein> owner)
    {
        table = "proteins";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<Protein> getTemplateClass()
    {
        return Protein.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, completeName, speciesId, geneId, "
                + "functionalState, structure, modifications " + "FROM " + table;
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
            return "CREATE TABLE `proteins` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'molecule-protein'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `speciesID` varchar(50) default NULL," +
                    "  `geneID` varchar(100) default NULL," +
                    "  `functionalState` enum('active','inactive','unknown') default NULL," +
                    "  `structure` enum('monomer','homodimer','heterodimer','multimer','unknown') default 'unknown'," +
                    "  `modifications` enum('none','phosphorylated','fatty_acylation','prenylation','cholesterolation','ubiquitination','sumolation','glycation','gpi_anchor','unknown') default 'unknown'," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_proteins_ID` (`ID`)" +
                    ") ENGINE=MyISAM;";
        }
        return super.getCreateTableQuery(tableName);
    }

    @Override
    protected Protein createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Protein protein = new Protein(owner, resultSet.getString(1));
        protein.setCompleteName(resultSet.getString(6));

        // Protein specific fields
        protein.setSpecies(resultSet.getString(7));
        protein.setGene(resultSet.getString(8));
        protein.setFunctionalState(resultSet.getString(9));
        protein.setStructure(resultSet.getString(10));
        protein.setModification(resultSet.getString(11));

        return protein;
    }

    @Override
    protected String getSpecificFields(Protein protein)
    {
        return ", geneId, functionalState, structure, modifications";
    }

    @Override
    protected String[] getSpecificValues(Protein protein)
    {
        return new String[] {protein.getGene(), protein.getFunctionalState(), protein.getStructure(), protein.getModification()};
    }

    @Override
    public void addInsertCommands(Statement statement, Protein protein) throws Exception
    {
        super.addInsertCommands(statement, protein);
        addStructureReferences(statement, protein);
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        super.addDeleteCommands(statement, name);
        removeStructureReferences(statement, name);
    }
}
