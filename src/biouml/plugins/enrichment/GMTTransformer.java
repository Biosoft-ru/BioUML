package biouml.plugins.enrichment;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.Entry;

public class GMTTransformer extends AbstractTransformer<Entry, FunctionalCategory>
{
    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<FunctionalCategory> getOutputType()
    {
        return FunctionalCategory.class;
    }

    @Override
    public FunctionalCategory transformInput(Entry entry) throws Exception
    {
        return new FunctionalCategory(entry.getName(), entry.getOrigin(), entry.getEntryData());
    }

    @Override
    public Entry transformOutput(FunctionalCategory category) throws Exception
    {
        return new Entry(category.getOrigin(), category.getName(), category.getOriginalLine());
    }
}
