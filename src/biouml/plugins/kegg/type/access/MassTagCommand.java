package biouml.plugins.kegg.type.access;

import ru.biosoft.access.support.TagCommand;
import biouml.plugins.kegg.type.Glycan;

public class MassTagCommand implements TagCommand
{
    public MassTagCommand(String tag, GlycanTransformer transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void start(String tag)
    {
        mass = 0.0f;
    }

    @Override
    public void addValue(String value)
    {
        value = value.trim();
        int sp = value.indexOf(' ');
        if( sp > 0 )
        {
            try
            {
                mass = Float.parseFloat(value.substring(0, sp));
            }
            catch( NumberFormatException ex )
            {
                return;
            }
        }
    }

    @Override
    public void complete(String tag)
    {
        if( mass > 0.0f )
        {
            Glycan glycan = transformer.getProcessedObject();
            glycan.setMass(mass);
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
        Glycan glycan = transformer.getProcessedObject();
        return tag + '\t' + glycan.getMass();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }

    private String tag;
    private GlycanTransformer transformer;
    private float mass;
}
