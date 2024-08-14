package biouml.plugins.kegg.type.access;

import ru.biosoft.access.support.DividedLineTagCommand;
import biouml.standard.type.Reaction;

public class DefinitionTagCommand extends DividedLineTagCommand<Reaction>
{
    private static final String DEFINITION_PREFIX = "Definition: ";
    public DefinitionTagCommand(String tag, ReactionTransformer transformer)
    {
        super(tag, transformer);
    }

    @Override
    public void startTag(String tag)
    {
        definition = new StringBuffer(DEFINITION_PREFIX);
    }

    @Override
    public void addLine(String line)
    {
        definition.append(line);
    }

    @Override
    public void endTag(String tag)
    {
        Reaction reaction = transformer.getProcessedObject();
        reaction.setDescription(definition.toString());
    }

    @Override
    public String getTaggedValue()
    {
        Reaction reaction = transformer.getProcessedObject();
        StringBuffer value = new StringBuffer(tag);
        value.append('\t');
        String descr = reaction.getDescription();
        if( descr.startsWith(DEFINITION_PREFIX) )
            descr = descr.substring(DEFINITION_PREFIX.length());

        value.append(descr);
        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }

    private StringBuffer definition;
}
