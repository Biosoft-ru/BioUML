package ru.biosoft.bsa;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.view.TrackViewBuilder;

/**
 * Abstract Track which maps all sites of existing track using mapping function processSite
 */
public abstract class TransformedTrack extends DataElementSupport implements Track
{
    protected Track source;

    public TransformedTrack(Track source)
    {
        super(source.getName(), source.getOrigin());
        this.source = source;
    }

    abstract protected Site transformSite(Site s);

    protected DataCollection<Site> transformSiteCollection(DataCollection<Site> input)
    {
        Properties transformed = new Properties();
        transformed.put(DataCollectionConfigConstants.TRANSFORMER_CLASS, TrackTransformer.class.getName());
        transformed.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, input);
        transformed.put(DataCollectionConfigConstants.NAME_PROPERTY, input.getName());
        transformed.put(TrackTransformer.TRACK, this);
        try
        {
            return new TransformedDataCollection(input.getOrigin(), transformed);
        } catch (Exception e)
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        return source.countSites(sequence, from, to);
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        try
        {
            return transformSiteCollection(source.getAllSites());
        }
        catch( Exception e )
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        return transformSite(source.getSite(sequence, siteName, from, to));
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        return transformSiteCollection(source.getSites(sequence, from, to));
    }

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return source.getViewBuilder();
    }

    public static class TrackTransformer extends AbstractTransformer<Site, Site>
    {
        static final String TRACK = "Track";

        private TransformedTrack getTrack()
        {
            return (TransformedTrack)transformedCollection.getInfo().getProperties().get(TRACK);
        }

        @Override
        public Class<Site> getInputType()
        {
            return Site.class;
        }

        @Override
        public Class<Site> getOutputType()
        {
            return Site.class;
        }

        @Override
        public Site transformInput(Site input) throws Exception
        {
            return getTrack().transformSite(input);
        }

        @Override
        public Site transformOutput(Site output) throws Exception
        {
            throw new UnsupportedOperationException();
        }
    }
}
