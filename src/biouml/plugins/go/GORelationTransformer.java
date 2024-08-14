package biouml.plugins.go;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import biouml.standard.type.SemanticRelation;

import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.SqlUtil;

public class GORelationTransformer extends SqlTransformerSupport<SemanticRelation>
{
    private static final String[] TABLES = {"term","term2term"};

    @Override
    public void addInsertCommands(Statement statement, SemanticRelation de) throws Exception
    {
    }

    @Override
    public SemanticRelation create(ResultSet resultSet, Connection connection) throws Exception
    {
        SemanticRelation element = new SemanticRelation(owner, resultSet.getString("t1")+"->"+resultSet.getString("t2"));
        element.setParticipation(resultSet.getString("type").equals("is_a")?SemanticRelation.PARTICIPATION_DIRECT:SemanticRelation.PARTICIPATION_INDIRECT);
        element.setRelationType(resultSet.getString("type").replace("_", "-"));
        element.setInputElementName("terms/"+resultSet.getString("t1"));
        element.setOutputElementName("terms/"+resultSet.getString("t2"));
        return element;
    }

    @Override
    public String getSelectQuery()
    {
        return "select t2.acc t1,t1.acc t2,t3.acc type from term t1,term t2,term t3,term2term where "
                + "term1_id=t1.id and term2_id=t2.id and relationship_type_id=t3.id "
                + "and t1.term_type IN ('biological_process','molecular_function','cellular_component') "
                + "and t2.term_type IN ('biological_process','molecular_function','cellular_component') order by t1,t2";
    }

    @Override
    public Class<SemanticRelation> getTemplateClass()
    {
        return SemanticRelation.class;
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
    }

    @Override
    public void addUpdateCommands(Statement statement, SemanticRelation de) throws Exception
    {
    }

    @Override
    public String getCountQuery()
    {
        return "select count(*) from term t1,term t2,term2term where term1_id=t1.id and term2_id=t2.id and "
                + "t1.term_type IN ('biological_process','molecular_function','cellular_component') and "
                + "t2.term_type IN ('biological_process','molecular_function','cellular_component')";
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        return null;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        String[] terms = name.split("->");
        if(terms.length != 2) return null;
        return "select concat(t2.acc,'->',t1.acc) name from term t1,term t2,term2term where term1_id=t1.id and term2_id=t2.id and "
                + "t1.term_type IN ('biological_process','molecular_function','cellular_component') and "
                + "t2.term_type IN ('biological_process','molecular_function','cellular_component') and " + "t2.acc="
                + SqlUtil.quoteString(terms[0]) + " and t1.acc=" + SqlUtil.quoteString(terms[1]);
    }

    @Override
    public String getElementQuery(String name)
    {
        String[] terms = name.split("->");
        if(terms.length != 2) return null;
        return "select t2.acc t1,t1.acc t2,t3.acc type from term t1,term t2,term t3,term2term where "
        + "term1_id=t1.id and term2_id=t2.id and relationship_type_id=t3.id "
        + "and t1.term_type IN ('biological_process','molecular_function','cellular_component') "
        + "and t2.term_type IN ('biological_process','molecular_function','cellular_component') "
        + "and t2.acc=" + SqlUtil.quoteString(terms[0]) + " and t1.acc=" + SqlUtil.quoteString(terms[1]);
    }

    @Override
    public String getNameListQuery()
    {
        return "select concat(t2.acc,'->',t1.acc) name from term t1,term t2,term2term where term1_id=t1.id and term2_id=t2.id and "
                + "t1.term_type IN ('biological_process','molecular_function','cellular_component') and "
                + "t2.term_type IN ('biological_process','molecular_function','cellular_component') order by name";
    }

    @Override
    public String[] getUsedTables()
    {
        return TABLES.clone();
    }
}
