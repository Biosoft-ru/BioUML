package biouml.plugins.kegg.type.access;

import ru.biosoft.access.Entry;
import ru.biosoft.access.support.EmptyCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.type.Protein;
import biouml.standard.type.access.SetAttributeCommand;

public class EnzymeTransformer extends TagEntryTransformer<Protein>
{
    public EnzymeTransformer()
    {
        addCommand(new EnzymeNameTagCommand("NAME", this));
        addCommand(new SetAttributeCommand("CLASS", "enzymeClass", String.class, this));
        addCommand(new SysnameTagCommand("SYSNAME", this));
        addCommand(new SetAttributeCommand("REACTION", "reaction", String.class, this));
        addCommand(new EmptyCommand("ALL_REAC"));
        addCommand(new SetAttributeCommand("SUBSTRATE", "substrate", String.class, this));
        addCommand(new SetAttributeCommand("PRODUCT", "product", String.class, this));
        addCommand(new SetKeggPropertyCommand("COMMENT", "setComment", this));
        addCommand(new ReferenceTagCommand("REFERENCE", this));
        addCommand(new EmptyCommand("PATHWAY"));
        addCommand(new SetAttributeCommand("ORTHOLOGY", "ortholog", String.class, this));
        addCommand(new SetKeggPropertyCommand("GENES", "setGene", this));
        addCommand(new SetAttributeCommand("DISEASE", "disease", String.class, this));
        addCommand(new SetAttributeCommand("MOTIF", "motif", String.class, this));
        addCommand(new SetKeggPropertyCommand("STRUCTURES", "setStructure", this));
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
