package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.RNA;

public class RNASqlTransformer extends BiopolymerSqlTransformer<RNA>
{
    @Override
    public boolean init( SqlDataCollection<RNA> owner )
    {
        table = "rnas";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<RNA> getTemplateClass()
    {
        return RNA.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, completeName, speciesId, geneId, rnaType " +
               "FROM " + table;
    }

    @Override
    protected RNA createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        RNA rna = new RNA(owner, resultSet.getString(1));
        rna.setCompleteName(resultSet.getString(6));

        // RNA specific fields
        rna.setSpecies(resultSet.getString(7));
        rna.setGene   (resultSet.getString(8));
        rna.setRnaType(resultSet.getString(9));

        return rna;
    }

    @Override
    protected String getSpecificFields(RNA rna)
    {
        return ", geneId, rnaType";
    }

    @Override
    protected String[] getSpecificValues(RNA rna)
    {
        return new String[]{rna.getGene(), rna.getRnaType()};
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
            return "CREATE TABLE `rnas` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'molecule-RNA'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `speciesID` varchar(50) default NULL," +
                    "  `geneID` varchar(100) default NULL," +
                    "  `rnaType` varchar(100) default NULL," +
                    "  `attributes` text," +
                    "  UNIQUE KEY `IDX_UNIQUE_rnas_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
