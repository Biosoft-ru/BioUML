package biouml.plugins.chemoinformatics;

import biouml.standard.type.Structure;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.Entry;

public class SDFTransformer extends AbstractTransformer<Entry, Structure>
{
    public SDFTransformer()
    {
        super();
    }

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<Structure> getOutputType()
    {
        return Structure.class;
    }

    @Override
    public Structure transformInput(Entry source) throws Exception
    {
        Structure structure = new Structure(getTransformedCollection(), source.getName());
        structure.setData(source.getData());

        return structure;
    }
    
    @Override
    public Entry transformOutput(Structure input) throws Exception
    {
        Entry entry = new Entry(getPrimaryCollection(), input.getName(), input.getData());
        return entry;
    }
}
