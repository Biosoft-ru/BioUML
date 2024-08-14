package biouml.plugins.chebi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.SemanticRelation;

public class RelationSqlTransformer extends SqlTransformerSupport<SemanticRelation>
{
    private static final Query VERTICE_QUERY = new Query("SELECT compound_id FROM vertice WHERE id=$id$");

    @Override
    public SemanticRelation create(ResultSet resultSet, Connection connection) throws Exception
    {
        SemanticRelation sr = new SemanticRelation(owner, resultSet.getString(1));
        sr.setRelationType(resultSet.getString(2));

        String inputCompound = SqlUtil.queryString( connection, VERTICE_QUERY.str( resultSet.getString(3) ) );
        if( inputCompound != null )
        {
            sr.setInputElementName("substance/" + inputCompound);
        }
        String outputCompound = SqlUtil.queryString( connection, VERTICE_QUERY.str( resultSet.getString(4) ) );
        if( outputCompound != null )
        {
            sr.setOutputElementName("substance/" + outputCompound);
        }

        return sr;
    }

    @Override
    public boolean init(SqlDataCollection<SemanticRelation> owner)
    {
        table = "relation";
        this.owner = owner;
        return true;
    }

    @Override
    public Class<SemanticRelation> getTemplateClass()
    {
        return SemanticRelation.class;
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT id FROM " + table + " ORDER BY id";
    }

    @Override
    public boolean isNameListSorted()
    {
        return true;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, init_id, final_id " + "FROM " + table;
    }

    @Override
    public void addInsertCommands(Statement statement, SemanticRelation de) throws Exception
    {
        throw new Exception("You can't add or remove elements from this module");
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new Exception("You can't add or remove elements from this module");
    }
}
