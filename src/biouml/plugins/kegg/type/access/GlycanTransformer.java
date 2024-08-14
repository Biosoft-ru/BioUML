package biouml.plugins.kegg.type.access;

import ru.biosoft.access.Entry;
import ru.biosoft.access.support.EmptyCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.plugins.kegg.type.Glycan;

public class GlycanTransformer extends TagEntryTransformer<Glycan>
{
    public GlycanTransformer()
    {
        addCommand(new ConceptNameTagCommand("NAME", this));
        addCommand(new SetKeggPropertyCommand("COMPOSITION", "setComposition", this));
        addCommand(new MassTagCommand("MASS", this));
        addCommand(new SetKeggPropertyCommand("CLASS", "setGlycanClass", this));
        addCommand(new SetKeggPropertyCommand("BINDING", "setBinding", this));
        addCommand(new SetKeggPropertyCommand("COMMENT", "setComment", this));
        addCommand(new SetKeggPropertyCommand("COMPOUND", "setCompound", this));
        addCommand(new SetKeggPropertyCommand("REACTION", "setReaction", this));
        addCommand(new SetKeggPropertyCommand("ENZYME", "setEnzyme", this));
        addCommand(new SetKeggPropertyCommand("ORTHOLOG", "setOrtholog", this));
        addCommand(new EmptyCommand("REFERENCE"));
        addCommand(new EmptyCommand("REACTION"));
        addCommand(new EmptyCommand("PATHWAY"));
        addCommand(new EmptyCommand("ENZYME"));
        addCommand(new EmptyCommand("NODE"));
        addCommand(new EmptyCommand("EDGE"));
        addCommand(new DBLinksTagCommand("DBLINKS", this));
    }

    @Override
    public Class<Glycan> getOutputType()
    {
        return Glycan.class;
    }

    @Override
    public Glycan transformInput(Entry input) throws Exception
    {
        Glycan glycan = new Glycan(getTransformedCollection(), input.getName());
        readObject(glycan, input.getReader());
        return glycan;
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
