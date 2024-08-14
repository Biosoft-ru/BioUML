package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.SemanticRelation;

public class RelationSqlTransformer extends ReferrerSqlTransformer<SemanticRelation>
{
    @Override
    public boolean init(SqlDataCollection<SemanticRelation> owner)
    {
        table = "relations";
        this.owner = owner;
        checkAttributesColumn(owner);
        return true;
    }

    @Override
    public Class<SemanticRelation> getTemplateClass()
    {
        return SemanticRelation.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, " + " inputElement, outputElement, participation, relationType " + "FROM "
                + table;
    }

    @Override
    protected SemanticRelation createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        SemanticRelation relation = new SemanticRelation(owner, resultSet.getString(1));

        // SemanticRelation info specific fields
        relation.setInputElementName(resultSet.getString(6));
        relation.setOutputElementName(resultSet.getString(7));
        relation.setParticipation(resultSet.getString(8));
        relation.setRelationType(resultSet.getString(9));

        return relation;
    }

    @Override
    protected String getSpecificFields(SemanticRelation relation)
    {
        String result = ", inputElement, outputElement";
        String participation = relation.getParticipation();
        if( participation != null && participation.length() > 0 )
        {
            result += ", participation";
        }
        result += ", relationType";
        return result;
    }

    @Override
    protected String[] getSpecificValues(SemanticRelation relation)
    {
        String participation = relation.getParticipation();
        if( participation != null && participation.length() > 0 )
        {
            return new String[] {relation.getInputElementName(), relation.getOutputElementName(), participation, relation.getRelationType()};
        }
        return new String[] {relation.getInputElementName(), relation.getOutputElementName(), relation.getRelationType()};
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
            return "CREATE TABLE `relations` ("
                    + getIDFieldFormat() + ","
                    + "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'relation-semantic',"
                    + getTitleFieldFormat()+ "," + "  `description` text," + "  `comment` text,"
                    + "  `inputElement` varchar(100) NOT NULL default ''," + "  `outputElement` varchar(100) NOT NULL default '',"
                    + "  `participation` enum('direct','indirect','unknown') NOT NULL default 'direct',"
                    + "  `relationType` varchar(50) NOT NULL default 'is-a',"
                    + "  `attributes` text,"
                    + "  UNIQUE KEY `IDX_UNIQUE_relations_ID` (`ID`),"
                    + "  KEY `IDX_RELATIONS_INPUT` (`inputElement`)," + "  KEY `IDX_RELATIONS_OUTPUT` (`outputElement`),"
                    + "  KEY `IDX_RELATIONS_TYPE` (`relationType`)" + ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
