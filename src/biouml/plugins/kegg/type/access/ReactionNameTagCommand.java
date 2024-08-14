package biouml.plugins.kegg.type.access;

import ru.biosoft.access.support.DividedLineTagCommand;
import biouml.standard.type.Reaction;

public class ReactionNameTagCommand extends DividedLineTagCommand<Reaction>
{
     private String name;

    public ReactionNameTagCommand(String tag, ReactionTransformer transformer)
    {
        super(tag, transformer);
    }

    @Override
    public void startTag(String tag)
    {
    }

    @Override
    public void addLine(String line)
    {
        if (line == null || line.length() == 0)
            return;

        name = line;
    }

    @Override
    public void endTag(String tag)
    {
        if (name == null || name.length() == 0)
            return;

        Reaction reaction = transformer.getProcessedObject();
        reaction.setTitle(name);
    }

    @Override
    public String getTaggedValue()
    {
        StringBuffer value = new StringBuffer(tag);
        value.append('\t');
        Reaction reaction = transformer.getProcessedObject();
        value.append(reaction.getTitle());
        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}
