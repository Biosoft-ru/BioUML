package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.Gene;

public class GeneSqlTransformer extends BiopolymerSqlTransformer<Gene>
{
    @Override
    public boolean init( SqlDataCollection<Gene> owner )
    {
        table = "genes";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<Gene> getTemplateClass()
    {
        return Gene.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, completeName, speciesId, chromosome " +
               "FROM " + table;
    }

    @Override
    protected Gene createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        Gene gene = new Gene(owner, resultSet.getString(1));
        gene.setCompleteName(resultSet.getString(6));

        // Gene specific fields
        gene.setSpecies     (resultSet.getString(7));
        gene.getAttributes().add(new DynamicProperty(Gene.LOCATION_PD, String.class, resultSet.getString( 8 )));

        return gene;
    }

    @Override
    protected String getSpecificFields(Gene gene)
    {
        return ", chromosome";
    }

    @Override
    protected String[] getSpecificValues(Gene gene)
    {
        return new String[] {(String)gene.getAttributes().getValue( Gene.LOCATION_PD.getName() )};
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
            return "CREATE TABLE `genes` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'molecule-gene'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `speciesID` varchar(50) default NULL," +
                    "  `chromosome` varchar(20) default NULL," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_genes_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
