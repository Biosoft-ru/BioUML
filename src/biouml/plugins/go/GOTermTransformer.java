package biouml.plugins.go;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Function;
import biouml.standard.type.Process;

import com.developmentontheedge.beans.DynamicProperty;

public class GOTermTransformer extends SqlTransformerSupport<Concept>
{
    private static final String[] TABLES = {"term", "term_synonym", "term2term", "term_subset", "term_dbxref", "dbxref"};

    protected enum StatementId
    {
        DEFINITION, SYNONYMS, ANNOTATION, GENE_COUNTS, GENE_SYMBOLS, REFERENCES
    }

    protected String[] queries = {
            "SELECT * FROM term_definition WHERE term_id=?",

            "SELECT * FROM term_synonym,term t WHERE term_id=? AND t.id=synonym_type_id AND t.acc!='alt_id'",

            "select distinct tt.acc type,t.acc acc from term2term,term t,term tt where term2_id=? and term1_id=t.id and relationship_type_id=tt.id "
                    + "union select distinct 'subset',t.acc from term_subset,term t where term_id=? and subset_id=t.id "
                    + "union select distinct 'alt_id',acc_synonym from term_synonym,term t where t.acc='alt_id' and t.id=synonym_type_id and term_id=?",

            "select genes_count from term_gene_counts,term where term_id=?",

            "select distinct g.symbol,s.common_name from gene_product g,species s,association a where s.id=g.species_id and gene_product_id=g.id and not(s.common_name is null) and a.term_id=?",

            "select distinct xref_dbname,xref_key,xref_keytype,xref_desc,is_for_definition from term_dbxref,dbxref d where term_id=? and d.id=dbxref_id"};

    protected PreparedStatement getStatement(Connection conn, StatementId id) throws SQLException
    {
        return conn.prepareStatement(queries[id.ordinal()]);
    }

    @Override
    public void addInsertCommands(Statement statement, Concept de) throws Exception
    {
    }

    @Override
    public Concept create(ResultSet resultSet, Connection connection) throws Exception
    {
        Concept element = resultSet.getString("term_type").equals("biological_process") ? new Process(owner, resultSet.getString("acc"))
                : resultSet.getString("term_type").equals("molecular_function") ? new Function(owner, resultSet.getString("acc"))
                        : new Concept(owner, resultSet.getString("acc"));
        for( String field : new String[] {"is_obsolete", "is_root", "is_relation"} )
        {
            Boolean value = resultSet.getInt(field) == 1;
            element.getAttributes().add(new DynamicProperty(field, Boolean.class, value));
        }
        element.getAttributes().add(new DynamicProperty("namespace", String.class, resultSet.getString("term_type")));
        element.setCompleteName(resultSet.getString("name"));
        element.setTitle(resultSet.getString("name"));
        int id = resultSet.getInt("id");
        try(PreparedStatement ps = getStatement(connection, StatementId.DEFINITION))
        {
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                {
                    if( rs.getString("term_comment") != null )
                        element.setComment(rs.getString("term_comment"));
                    if( rs.getString("term_definition") != null )
                        element.setDescription(rs.getString("term_definition"));
                }
            }
        }
        try(PreparedStatement ps = getStatement(connection, StatementId.SYNONYMS))
        {
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery())
            {
                List<String> synonyms = new ArrayList<>();
                while( rs.next() )
                {
                    synonyms.add(rs.getString("term_synonym"));
                }
                if( synonyms.size() > 0 )
                    element.setSynonyms(String.join(", ", synonyms));
            }
        }
        Map<String, Set<String>> relations = new HashMap<>();
        try(PreparedStatement ps = getStatement(connection, StatementId.ANNOTATION))
        {
            ps.setInt(1, id);
            ps.setInt(2, id);
            ps.setInt(3, id);
            try(ResultSet rs = ps.executeQuery())
            {
                while( rs.next() )
                {
                    relations.computeIfAbsent( rs.getString("type"), k -> new HashSet<>() ).add(rs.getString("acc"));
                }
            }
        }
        for( Map.Entry<String, Set<String>> entry : relations.entrySet() )
        {
            String relationType = entry.getKey();
            Set<String> set = entry.getValue();
            element.getAttributes().add(
                    set.size() == 1 ? new DynamicProperty(relationType, String.class, set.iterator().next()) : new DynamicProperty(
                            relationType, String[].class, set.toArray(new String[set.size()])));
        }

        try(PreparedStatement ps = getStatement(connection, StatementId.GENE_SYMBOLS))
        {
            ps.setInt(1, id);
            List<String> symbols = new ArrayList<>();
            try(ResultSet rs = ps.executeQuery())
            {
                while( rs.next() )
                {
                    symbols.add(rs.getString("symbol") + " (" + rs.getString("common_name") + ")");
                }
            }
            if( !symbols.isEmpty() )
                element.getAttributes().add(
                        new DynamicProperty("genes_symbol", String[].class, symbols.toArray(new String[symbols.size()])));
        }

        List<DatabaseReference> databaseReferences = new ArrayList<>();
        try(PreparedStatement ps = getStatement(connection, StatementId.REFERENCES))
        {
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery())
            {
                while( rs.next() )
                {
                    DatabaseReference reference = new DatabaseReference(rs.getString("xref_dbname"), rs.getString("xref_key"));
                    if( rs.getInt("is_for_definition") > 0 )
                        reference.setRelationshipType("For definition");
                    databaseReferences.add(reference);
                }
            }
        }
        element.setDatabaseReferences(databaseReferences.toArray(new DatabaseReference[databaseReferences.size()]));
        return element;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT * FROM term WHERE term_type IN ('biological_process','molecular_function','cellular_component')";
    }

    @Override
    public Class<Concept> getTemplateClass()
    {
        return Concept.class;
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
    public String getCountQuery()
    {
        return "SELECT COUNT(DISTINCT acc) FROM term WHERE term_type IN ('biological_process','molecular_function','cellular_component')";
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        return null;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT acc FROM term WHERE acc=" + SqlUtil.quoteString(name)
                + " AND term_type IN ('biological_process','molecular_function','cellular_component')";
    }

    @Override
    public String getElementQuery(String name)
    {
        return "SELECT * FROM term WHERE acc=" + SqlUtil.quoteString(name)
                + " AND term_type IN ('biological_process','molecular_function','cellular_component')";
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT DISTINCT acc FROM term WHERE term_type IN ('biological_process','molecular_function','cellular_component') ORDER BY acc";
    }

    @Override
    public String[] getUsedTables()
    {
        return TABLES.clone();
    }
}
