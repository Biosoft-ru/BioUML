package biouml.plugins.kegg.type.access;

import ru.biosoft.access.Entry;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.type.Protein;
import biouml.standard.type.access.SetAttributeCommand;

public class OrthologTransformer extends TagEntryTransformer<Protein>
{
    public OrthologTransformer()
    {
        addCommand(new SetKeggPropertyCommand("NAME", "setTitle", this));
        addCommand(new SetKeggPropertyCommand("DEFINITION", "setCompleteName", this));
        addCommand(new SetAttributeCommand("CLASS", "classification", String.class, this));
        addCommand(new SetAttributeCommand("GENES", "genes", String.class, this));
        addCommand(new DBLinksTagCommand("DBLINKS", this));
    }

    @Override
    public Class<Protein> getOutputType()
    {
        return Protein.class;
    }

    @Override
    public Protein transformInput(Entry input) throws Exception
    {
        Protein enzyme = new Protein(getTransformedCollection(), input.getName());
        readObject(enzyme, input.getReader());
        return enzyme;
    }

    @Override
    public String getStartTag()
    {
        return "ENTRY";
    }

    @Override
    public String getEndTag()
    {
        return "///";
    }
}
