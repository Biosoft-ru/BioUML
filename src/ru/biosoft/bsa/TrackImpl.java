package ru.biosoft.bsa;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.analysis.FilteredTrack;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;

public class TrackImpl extends TransformedDataCollection<Site, AnnotatedSequence> implements WritableTrack
{
    protected @Nonnull DataCollection<Site> sites;
    protected AtomicInteger siteNum = new AtomicInteger();

    public TrackImpl(String name, DataCollection<?> origin) throws Exception
    {
        super(origin, createProperties(name, null));
        sites = getPrimaryCollection();
    }

    public TrackImpl(DataCollection<?> origin, Properties properties) throws Exception
    {
        super(origin, createProperties(null, properties));
        sites = getPrimaryCollection();
    }

    private static Properties createProperties(String name, Properties defProperties)
    {
        Properties properties = new Properties(defProperties);
        if(name != null)
        {
            properties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        }

        VectorDataCollection<Site> sitesDC = new VectorDataCollection<>(properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY), Site.class, null);

        properties.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, AnnotatedSequence.class);
        properties.put(DataCollectionConfigConstants.TRANSFORMER_CLASS, SiteSequenceTransformer.class.getName());
        properties.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, sitesDC);
        return properties;
    }

    @Override
    public void addSite(Site site)
    {
        if(site.getName() == null)
        {
            site = new SiteImpl(site.getOrigin(), String.valueOf(siteNum.incrementAndGet()), site.getType(), site.getBasis(),
                    site.getStart(), site.getLength(), site.getPrecision(), site.getStrand(), site.getOriginalSequence(),
                    site.getComment(), site.getProperties());
        }
        if( !sites.contains(site) )
        {
            try
            {
                sites.put(site);
            }
            catch( Exception e )
            {
                //TODO:
            }
        }
    }

    @Override
    public boolean contains(String name)
    {
        return sites.contains(name);
    }

    @Override
    public DataCollection<Site> getSites(final String sequence, final int from, final int to)
    {
        return new FilteredDataCollection<>(sites, new FilteredTrack.SiteFilter()
        {
            @Override
            public boolean isAcceptable(Site site)
            {
                Sequence originalSequence = site.getOriginalSequence();
                return ( originalSequence == null || DataElementPath.create(sequence).getName().equals(originalSequence.getName()) )
                        && site.getFrom() <= to && site.getTo() >= from;
            }
        });
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        return getSites( sequence, from, to ).getSize();
    }

    TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        return getSites(sequence, from, to).get(siteName);
    }

    @Override
    public void finalizeAddition()
    {
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        return sites;
    }

    public static class SiteSequenceTransformer extends AbstractTransformer<Site, AnnotatedSequence>
    {
        @Override
        public Class<Site> getInputType()
        {
            return Site.class;
        }

        @Override
        public Class<AnnotatedSequence> getOutputType()
        {
            return AnnotatedSequence.class;
        }

        @Override
        public AnnotatedSequence transformInput(Site input) throws Exception
        {
            return new MapAsVector(input.getName(), getTransformedCollection(), input.getSequence(), null);
        }

        @Override
        public Site transformOutput(AnnotatedSequence output) throws Exception
        {
            throw new UnsupportedOperationException();
        }
    }
}
