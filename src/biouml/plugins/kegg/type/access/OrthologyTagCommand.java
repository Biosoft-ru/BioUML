package biouml.plugins.kegg.type.access;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.DividedLineTagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.model.Module;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class OrthologyTagCommand extends DividedLineTagCommand<Reaction>
{
    protected static final Logger log = Logger.getLogger(OrthologyTagCommand.class.getName());

    private List<String> orthologies;

    public OrthologyTagCommand(String tag, TagEntryTransformer<? extends Reaction> transformer)
    {
        super(tag, transformer);
    }

    @Override
    public void startTag(String tag)
    {
        orthologies = new ArrayList<>();
    }

    @Override
    public void addLine(String line)
    {
        if( line == null || line.length() == 0 )
            return;

        int idx = line.indexOf("KO: ");
        if( idx != -1 )
        {
            int start = idx + 4;
            int end = line.indexOf(' ', start);
            if( start >= 0 && end >= 0 && end > start )
            {
                orthologies.add(line.substring(start, end));
            }
        }
    }

    @Override
    public void endTag(String tag)
    {
        try
        {
            Reaction reaction = transformer.getProcessedObject();
            for( String name : orthologies )
            {
                SpecieReference ref = new SpecieReference(reaction, name + " as " + SpecieReference.MODIFIER);
                ref.setSpecie(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + "ortholog" + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + "" + ru.biosoft.access.core.DataElementPath.escapeName(name));
                ref.setRole(SpecieReference.MODIFIER);
                ref.setModifierAction(SpecieReference.ACTION_CATALYST);
                reaction.put(ref);
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "cannot put orthology to reaction: " + t.getMessage());
        }
    }
    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}
