package biouml.plugins.go;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.standard.type.Concept;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.SqlUtil;

public class GOTypedefTransformer extends SqlTransformerSupport<Concept>
{
    private static final String[] TABLES = {"term"};

    @Override
    public void addInsertCommands(Statement statement, Concept de) throws Exception
    {
    }

    @Override
    public Concept create(ResultSet resultSet, Connection connection) throws Exception
    {
        Concept element = new Concept(owner, resultSet.getString("acc"));
        for(String field: new String[] {"is_obsolete","is_root","is_relation"})
        {
            Boolean value = resultSet.getInt(field)==1;
            element.getAttributes().add(new DynamicProperty(field, Boolean.class, value));
        }
        element.setCompleteName(resultSet.getString("name"));
        return element;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT * FROM term WHERE term_type='gene_ontology'";
    }

    @Override
    public Class<Concept> getTemplateClass()
    {
        return Concept.class;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(*) FROM term WHERE term_type='gene_ontology'";
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
    }

    @Override
    public void addUpdateCommands(Statement statement, Concept de) throws Exception
    {
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        return null;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT acc FROM term WHERE term_type='gene_ontology' AND acc="+SqlUtil.quoteString(name);
    }

    @Override
    public String getElementQuery(String name)
    {
        return "SELECT * FROM term WHERE term_type='gene_ontology' AND acc="+SqlUtil.quoteString(name);
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT acc FROM term WHERE term_type='gene_ontology'";
    }

    @Override
    public String[] getUsedTables()
    {
        return TABLES.clone();
    }
}
