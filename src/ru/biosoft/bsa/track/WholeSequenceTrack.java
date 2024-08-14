package ru.biosoft.bsa.track;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;

/**
 * Track which contains exactly one site with length of the whole sequence
 */
public class WholeSequenceTrack extends DataElementSupport implements Track
{

    public WholeSequenceTrack(String name, DataCollection origin)
    {
        super(name, origin);
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        return 1;
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to)
    {
        if(!siteName.equals(getName())) return null;
        Sequence seq = DataElementPath.create( sequence ).getDataElement( AnnotatedSequence.class ).getSequence();
        return new SiteImpl(null, getName(), SiteType.TYPE_MISC_FEATURE, Site.BASIS_ANNOTATED, seq.getStart(), seq.getLength(),
                Site.STRAND_NOT_APPLICABLE, seq);
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        VectorDataCollection<Site> sites = new VectorDataCollection<>("Sites", Site.class, null);
        Site site = getSite(sequence, getName(), from, to);
        if(site != null) sites.put(site);
        return sites;
    }

    TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        throw new UnsupportedOperationException("Operation is not applicable for this type of track");
    }
}
