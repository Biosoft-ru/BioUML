package ru.biosoft.bsa.transformer;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.Entry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackOnSequences;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;

public class FastaSequenceCollection extends TransformedDataCollection<Entry, AnnotatedSequence> implements TrackOnSequences
{
	
	public static String DO_GET_SEQUENCEID_ONLY = "Get sequenceIds only";
	
    public FastaSequenceCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super( parent, properties );
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        VectorDataCollection<Site> sites = new VectorDataCollection<>("Sites", Site.class, null);
        Sequence seq = fetchSequence(sequence);
        if(seq != null)
        {
           Site site = getSite(sequence, seq.getName(), from, to);
           if(site != null) sites.put(site);
        }
        return sites;

    }

    public int countSites(String sequence, int from, int to) throws Exception
    {
        Sequence seq = fetchSequence(sequence);
        if(seq == null || seq.getStart() > to || seq.getStart()+seq.getLength()-1 < from) return 0;
        return 1;
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to)
    {
        Sequence seq = fetchSequence(sequence);
        if(seq == null || seq.getStart() > to || seq.getStart()+seq.getLength()-1 < from || !seq.getName().equals(siteName)) return null;
        return new SiteImpl(this, seq.getName(), SiteType.TYPE_MISC_FEATURE, Site.BASIS_USER, seq.getStart(), seq.getLength(), Site.PRECISION_EXACTLY, StrandType.STRAND_NOT_APPLICABLE, seq, null);
    }


    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        VectorDataCollection<Site> sites = new VectorDataCollection<>("Sites", Site.class, null);
        DataElementPath path = DataElementPath.create(this);
        for(String name: getNameList())
        {
            String sequence = path.getChildPath(name).toString();
            Sequence seq = fetchSequence(sequence);
            Site site = getSite(sequence, name, seq.getStart(), seq.getLength());
            if(site != null) sites.put(site);
        }
        return sites;
    }

    protected TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    
    protected Sequence fetchSequence(String sequence)
    {
        DataElementPath path = DataElementPath.create(sequence);
        if(!path.getParentPath().equals(getCompletePath())) return null;
        try
        {
            return get(path.getName()).getSequence();
        }
        catch( Exception e )
        {
        }
        return null;
    }
}