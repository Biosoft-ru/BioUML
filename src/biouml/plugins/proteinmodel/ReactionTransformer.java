package biouml.plugins.proteinmodel;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class ReactionTransformer extends SqlTransformerSupport<Reaction>
{
    @Override
    public boolean init(SqlDataCollection<Reaction> owner)
    {
        table = "protein";
        idField = "id";
        return super.init(owner);
    }
    @Override
    public Class<Reaction> getTemplateClass()
    {
        return Reaction.class;
    }

    @Override
    public Reaction create(ResultSet resultSet, Connection connection) throws Exception
    {
        Reaction reaction = new Reaction(owner, resultSet.getString("id")+ "_r");
        reaction.setTitle(resultSet.getString("protein_names"));
        SpecieReference[] specieReference = new SpecieReference[2];
        //specieReference[0] = new SpecieReference(reaction, resultSet.getString("refseq_mrna_ids")+" as "+SpecieReference.REACTANT, SpecieReference.REACTANT);
        specieReference[0] = new SpecieReference(reaction, resultSet.getString("id") + "_m as "+SpecieReference.REACTANT, SpecieReference.REACTANT);
        specieReference[0].setSpecie("Data/rna/"+resultSet.getString("id") + "_m");
        //specieReference[1] = new SpecieReference(reaction, resultSet.getString("protein_ids")+" as "+SpecieReference.PRODUCT, SpecieReference.PRODUCT);
        specieReference[1] = new SpecieReference(reaction, resultSet.getString("id")+" as "+SpecieReference.PRODUCT, SpecieReference.PRODUCT);
        specieReference[1].setSpecie("Data/protein/"+resultSet.getString("id"));
        reaction.setSpecieReferences(specieReference);
        return reaction;
    }

   
    @Override
    public void addInsertCommands(Statement statement, Reaction de) throws Exception
    {
        throw new UnsupportedOperationException();
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
        return "SELECT CONCAT(" + idField + ",\"_r\") FROM " + table + " WHERE " + ProteinModelUtils.getRNAExistsCondition() + " ORDER BY "
                + idField;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT 1 FROM " + table + " WHERE " + idField + "=" + validateValue(convertReactionToProtein(name)) + " AND "
                + ProteinModelUtils.getRNAExistsCondition();
    }

    @Override
    public String getElementQuery(String name)
    {
        return "SELECT * FROM " + table + " WHERE " + ProteinModelUtils.getRNAExistsCondition() + " AND " + idField + "="
                + validateValue(convertReactionToProtein(name));
    }

    private String convertReactionToProtein(String name)
    {
        return name.replaceFirst("_r$", "");
    }
    
}
