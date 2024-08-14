package biouml.plugins.kegg.type.access;

import ru.biosoft.access.Entry;
import ru.biosoft.access.support.EmptyCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.type.Substance;

public class CompoundTransformer extends TagEntryTransformer<Substance>
{
    public CompoundTransformer()
    {
        addCommand(new ConceptNameTagCommand("NAME", this));
        addCommand(new SetKeggPropertyCommand("FORMULA", "setFormula", this));
        addCommand(new DBLinksTagCommand("DBLINKS", this));
        addCommand(new EmptyCommand("MASS"));
        addCommand(new EmptyCommand("REMARK"));
        addCommand(new EmptyCommand("REACTION"));
        addCommand(new EmptyCommand("PATHWAY"));
        addCommand(new EmptyCommand("ENZYME"));
        addCommand(new EmptyCommand("ATOM"));
        addCommand(new EmptyCommand("BOND"));
    }

    @Override
    public Class<Substance> getOutputType()
    {
        return Substance.class;
    }

    @Override
    synchronized public Substance transformInput(Entry input) throws Exception
    {
        Substance substance = new Substance(getTransformedCollection(), input.getName());
        readObject(substance, input.getReader());
        return substance;
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
