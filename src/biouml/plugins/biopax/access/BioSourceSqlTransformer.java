
package biouml.plugins.biopax.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.plugins.biopax.model.BioSource;
import biouml.standard.type.access.ConceptSqlTransformer;

public class BioSourceSqlTransformer extends ConceptSqlTransformer<BioSource>
{
    @Override
    public boolean init(SqlDataCollection<BioSource> owner)
    {
        table = "bioSources";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }
    
    @Override
    public Class<BioSource> getTemplateClass()
    {
        return BioSource.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT ID, type, title, description, comment, completeName, cellType, tissue  " + "FROM " + table;
    }

    @Override
    protected BioSource createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        BioSource bioSource = new BioSource(owner, resultSet.getString(1));
        bioSource.setCompleteName(resultSet.getString(6));
        return bioSource;
    }

    @Override
    public BioSource create(ResultSet resultSet, Connection connection) throws Exception
    {
        BioSource bioSource = super.create(resultSet, connection);
        bioSource.setCelltype(resultSet.getString(7));
        bioSource.setTissue(resultSet.getString(8));
        return bioSource;
    }

    @Override
    protected String getSpecificFields(BioSource bioSource)
    {
        return ", completeName, cellType, tissue";
    }

    @Override
    protected String[] getSpecificValues(BioSource bioSource)
    {
        return new String[] { bioSource.getCompleteName(), bioSource.getCelltype(), bioSource.getTissue()};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `bioSources` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'compartment-cell'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `cellType` varchar(100)," +
                    "  `tissue` varchar(100)," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_bioSources_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
