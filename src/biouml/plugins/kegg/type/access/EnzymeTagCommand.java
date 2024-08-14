package biouml.plugins.kegg.type.access;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.TagCommand;
import biouml.model.Module;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class EnzymeTagCommand implements TagCommand
{
    protected static final String LN = System.getProperty("line.separator");
    protected static final String TAB = "\t";
    protected static final Logger log = Logger.getLogger(EnzymeTagCommand.class.getName());

    private String tag;
    private StringBuffer names;
    private ReactionTransformer transformer;

    public EnzymeTagCommand(String tag, ReactionTransformer transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void start(String tag)
    {
        names = new StringBuffer();
    }

    @Override
    public void addValue(String value)
    {
        if( value == null || value.length() == 0 )
            return;

        names.append(value).append(" ");
    }

    @Override
    public void complete(String tag)
    {
        Reaction reaction = transformer.getProcessedObject();
        StringTokenizer st = new StringTokenizer(names.toString(), " " + TAB + LN);
        try
        {
            while( st.hasMoreTokens() )
            {
                String name = st.nextToken().trim();
                if( name.length() > 0 )
                {
                    SpecieReference ref = new SpecieReference(reaction, "EC " + name + " as " + SpecieReference.MODIFIER);
                    ref.setSpecie(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + "enzyme" + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + "EC "
                            + ru.biosoft.access.core.DataElementPath.escapeName(name));
                    ref.setRole(SpecieReference.MODIFIER);
                    ref.setModifierAction(SpecieReference.ACTION_CATALYST);
                    reaction.put(ref);
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "cannot put enzyme to reaction: " + t.getMessage());
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
        StringBuffer value = new StringBuffer(tag);
        Reaction reaction = transformer.getProcessedObject();
        SpecieReference[] refs = reaction.getSpecieReferences();
        for( int i = 0; i < refs.length; i++ )
        {
            if( ( i % 4 == 0 ) && ( i / 4 > 0 ) )
                value.append(LN);

            if( refs[i].getRole().equals(SpecieReference.MODIFIER) )
                value.append(TAB).append(refs[i].getName());
        }
        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}
