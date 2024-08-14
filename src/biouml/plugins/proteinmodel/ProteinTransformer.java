package biouml.plugins.proteinmodel;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import one.util.streamex.EntryStream;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Protein;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.SqlUtil;

public class ProteinTransformer extends SqlTransformerSupport<Protein>
{
    private static final Map<String, String> dbs = new LinkedHashMap<>();
    
    static
    {
        dbs.put("ensembl_ids", "Ensembl");
        dbs.put("protein_ids", "IPI");
        dbs.put("refseq_protein_ids", "RefSeq");
        dbs.put("uniprot_ids", "UniProt");
        dbs.put("mgi_ids", "MGI");
    }
    
    @Override
    public Class<Protein> getTemplateClass()
    {
        return Protein.class;
    }

    @Override
    public Protein create(ResultSet resultSet, Connection connection) throws Exception
    {
        Protein protein = new Protein(owner, resultSet.getString("id"));
        protein.setTitle(resultSet.getString("id"));
        protein.setComment(resultSet.getString("protein_names"));
        protein.setDescription(resultSet.getString("protein_desc"));
        for( String fieldName : new String[] {"protein_length", "protein_weight", "protein_copies_exp", "protein_copies_rep",
                "protein_copies_avg", "protein_halflife_exp",
                "protein_halflife_rep", "protein_halflife_avg", "ksp_exp", "ksp_rep", "ksp_avg"} )
        {
            Double value = resultSet.getDouble(fieldName);
            if( resultSet.wasNull() )
                value = null;
            if( fieldName.equals("protein_length") )
            {
                protein.getAttributes().add(new DynamicProperty(fieldName, Integer.class, value.intValue()));
            }
            else
            {
                protein.getAttributes().add(new DynamicProperty(fieldName, Double.class, value));
            }
        }
        DatabaseReference[] refs = EntryStream.of( dbs ).mapKeys( SqlUtil.getStringByName( resultSet ) )
                .flatMapKeyValue( ProteinModelUtils::createReferences ).toArray( DatabaseReference[]::new );
        protein.setDatabaseReferences(refs);

        return protein;
    }

    @Override
    public void addInsertCommands(Statement statement, Protein de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUpdateCommands(Statement statement, Protein de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean init(SqlDataCollection<Protein> owner)
    {
        super.init(owner);
        table = "protein";
        idField = "id";
        return true;
    }
}
