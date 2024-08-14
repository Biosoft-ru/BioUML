package biouml.plugins.kegg.type.access;

import ru.biosoft.access.support.SetPropertyCommand;
import ru.biosoft.access.support.TagEntryTransformer;

public class SetKeggPropertyCommand extends SetPropertyCommand
{
    public SetKeggPropertyCommand(String tag, String property, TagEntryTransformer transformer)
    {
        this(tag, property, transformer, endl);
    }
        
    public SetKeggPropertyCommand(String tag, String property, TagEntryTransformer transformer, String delimiter)
    {
        super(tag, transformer.getOutputType(),
              String.class, "g"+property.substring(1), property,
              transformer, 12, false);

        //this.delimiter = delimiter;
    }

    @Override
    public void addValue(String appendValue)
    {
        if( appendValue.trim().startsWith("$") )
        {
            String str = appendValue.trim();
            str = str.substring(1);
            value.append( str );
        }
        else
        {
            super.addValue( appendValue );
        }
    }
}