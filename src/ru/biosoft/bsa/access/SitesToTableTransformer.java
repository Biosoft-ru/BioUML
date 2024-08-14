package ru.biosoft.bsa.access;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.bsa.Site;

/**
 * @author lan
 */
public class SitesToTableTransformer extends AbstractTransformer<Site, TransformedSite>
{
    public static final String PROPERTY_PREFIX = "Property: ";
    public static final String PROPERTY_SEQUENCE = "Sequence (chromosome) name";
    public static final String PROPERTY_FROM = "From";
    public static final String PROPERTY_TO = "To";
    public static final String PROPERTY_LENGTH = "Length";
    public static final String PROPERTY_STRAND = "Strand";
    public static final String PROPERTY_TYPE = "Type";

    @Override
    public Class<Site> getInputType()
    {
        return Site.class;
    }

    @Override
    public Class<TransformedSite> getOutputType()
    {
        return TransformedSite.class;
    }

    @Override
    public TransformedSite transformInput(Site input) throws Exception
    {
        return new TransformedSite(input);
    }

    @Override
    public Site transformOutput(TransformedSite output) throws Exception
    {
        throw new UnsupportedOperationException();
    }
}
