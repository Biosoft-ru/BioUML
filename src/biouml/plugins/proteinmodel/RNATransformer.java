package biouml.plugins.proteinmodel;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.standard.type.DatabaseReference;
import biouml.standard.type.RNA;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.util.TextUtil2;

public class RNATransformer extends SqlTransformerSupport<RNA>
{
    @Override
    public Class<RNA> getTemplateClass()
    {
        return RNA.class;
    }

    @Override
    public RNA create(ResultSet resultSet, Connection connection) throws Exception
    {
        RNA rna = new RNA(owner, resultSet.getString("id") + "_m");
        rna.setTitle(resultSet.getString("id") + "_m");
        rna.setDescription(resultSet.getString("protein_desc"));
        for( String fieldName : new String[] {"mrna_copies_exp", "mrna_copies_rep", "mrna_copies_avg", "mrna_halflife_exp",
                "mrna_halflife_rep", "mrna_halflife_avg", "vsr_exp", "vsr_rep", "vsr_avg",} )
        {
            Double value = resultSet.getDouble(fieldName);
            if( resultSet.wasNull() )
                value = null;
            rna.getAttributes().add(new DynamicProperty(fieldName, Double.class, value));
        }
        rna.getAttributes().add(new DynamicProperty("gene_names", String[].class, TextUtil2.split( resultSet.getString("gene_names"), ';' )));
        DatabaseReference[] refs = ProteinModelUtils.createReferences( resultSet.getString( "refseq_mrna_ids" ), "RefSeq" ).toArray(
                DatabaseReference[]::new );
        rna.setDatabaseReferences(refs);

        return rna;
    }

    @Override
    public void addInsertCommands(Statement statement, RNA de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUpdateCommands(Statement statement, RNA de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean init(SqlDataCollection<RNA> owner)
    {
        super.init(owner);
        table = "protein";
        idField = "id";
        return true;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(*) FROM " + table + " WHERE " + ProteinModelUtils.getRNAExistsCondition();
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT * FROM " + table + " WHERE " + ProteinModelUtils.getRNAExistsCondition();
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT CONCAT(" + idField + ",\"_m\") FROM " + table + " WHERE " + ProteinModelUtils.getRNAExistsCondition() + " ORDER BY " + idField;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT 1 FROM " + table + " WHERE " + idField + "=" + validateValue(convertRNAtoProtein(name)) + " AND " + ProteinModelUtils.getRNAExistsCondition();
    }

    @Override
    public String getElementQuery(String name)
    {
        return "SELECT * FROM " + table + " WHERE " + ProteinModelUtils.getRNAExistsCondition() + " AND " + idField + "=" + validateValue(convertRNAtoProtein(name));
    }

    private String convertRNAtoProtein(String name)
    {
        return name.replaceFirst("_m$", "");
    }
}
