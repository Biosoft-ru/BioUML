package biouml.plugins.uniprot;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.Entry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.access.support.TagCommand;
import biouml.standard.type.Protein;

public class UniProtEntryTransformer extends BeanInfoEntryTransformer<Protein>
{
    private final List<String> TAGS = new ArrayList<>();

    private String delimitersPriority[] = {"\t", " "};

    public UniProtEntryTransformer()
    {
        super();

        setDelimitersPriority(delimitersPriority);

        for( String tagName : UniProtTagCommand.TAGS.keySet() )
        {
            TAGS.add(tagName);
        }

        for( String tag : TAGS )
        {
            super.addCommand(new UniProtTagCommand(tag, this));
        }
    }

    @Override
    public Class<Protein> getOutputType()
    {
        return Protein.class;
    }

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public void addCommand(TagCommand command)
    {
        for( String tag : TAGS )
        {
            if( tag.equals(command.getTag()) )
            {
                super.addCommand(command);
            }
        }
    }

    @Override
    synchronized public Protein transformInput(Entry input) throws Exception
    {
        Class<? extends Protein> clazz = getOutputType().asSubclass(Protein.class);

        Constructor<? extends Protein> constructor = null;
        try
        {
            constructor = clazz.getConstructor(DataCollection.class, String.class);
        }
        catch( NoSuchMethodException e )
        {
            constructor = clazz.getConstructor();
        }

        String name = input.getName();

        Protein de = constructor.newInstance(getTransformedCollection(), name);

        readObject(de, input.getReader());

        return de;
    }
}
