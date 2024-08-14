package biouml.plugins.biopax.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.standard.type.access.ConceptSqlTransformer;

public class OpenControlledVocabularySqlTransfromer extends ConceptSqlTransformer<OpenControlledVocabulary>
{
    @Override
    public boolean init(SqlDataCollection<OpenControlledVocabulary> owner)
    {
        table = "vocabularies";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }
    
    @Override
    public Class<OpenControlledVocabulary> getTemplateClass()
    {
        return OpenControlledVocabulary.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT ID, type, title, description, comment, completeName, term, vocabularyType  " + "FROM " + table;
    }

    @Override
    protected OpenControlledVocabulary createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        OpenControlledVocabulary vocabulary = new OpenControlledVocabulary(owner, resultSet.getString(1));
        vocabulary.setType(resultSet.getString(2));
        vocabulary.setCompleteName(resultSet.getString(6));
        return vocabulary;
    }

    @Override
    public OpenControlledVocabulary create(ResultSet resultSet, Connection connection) throws Exception
    {
        OpenControlledVocabulary vocabulary = super.create(resultSet, connection);
        vocabulary.setTerm(resultSet.getString(7));
        vocabulary.setVocabularyType(resultSet.getString(8));
        return vocabulary;
    }

    @Override
    protected String getSpecificFields(OpenControlledVocabulary ocv)
    {
        return ", completeName, term, vocabularyType";
    }

    @Override
    protected String[] getSpecificValues(OpenControlledVocabulary ocv)
    {
        return new String[] { ocv.getCompleteName(), ocv.getTerm(), ocv.getVocabularyType()};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `vocabularies` (" +
                    getIDFieldFormat() + "," +
                    "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'semantic-concept'," +
                    getTitleFieldFormat()+ "," +
                    "  `completeName` varchar(200) default NULL," +
                    "  `term` varchar(100)," +
                    "  `vocabularyType` varchar(30)," +
                    "  `description` text," +
                    "  `comment` text," +
                    "  `attributes` text,"+
                    "  UNIQUE KEY `IDX_UNIQUE_vocabularies_ID` (`ID`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }

}
