package biouml.plugins.kegg.type.access;

import ru.biosoft.access.Entry;
import ru.biosoft.access.support.EmptyCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.type.Reaction;

public class ReactionTransformer extends TagEntryTransformer<Reaction>
{
    public ReactionTransformer()
    {
        addCommand(new ReactionNameTagCommand("NAME", this));
        addCommand(new SetKeggPropertyCommand("COMMENT", "setComment", this));
        addCommand(new DefinitionTagCommand("DEFINITION", this));
        addCommand(new EquationTagCommand("EQUATION", this));
        addCommand(new EnzymeTagCommand("ENZYME", this));
        addCommand(new OrthologyTagCommand("ORTHOLOGY", this));
        addCommand(new EmptyCommand("RPAIR"));
        addCommand(new EmptyCommand("PATHWAY"));
    }

    @Override
    public Class<Reaction> getOutputType()
    {
        return Reaction.class;
    }

    @Override
    public Reaction transformInput(Entry input) throws Exception
    {
        Reaction reaction = new Reaction(getTransformedCollection(), input.getName());
        readObject(reaction, input.getReader());
        return reaction;
    }

    @Override
    public Entry transformOutput(Reaction input) throws Exception
    {
        //we can't change KEGG data
        return null;
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
