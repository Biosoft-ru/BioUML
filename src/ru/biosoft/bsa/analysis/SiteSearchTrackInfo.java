package ru.biosoft.bsa.analysis;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementGetException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.util.TransformedIterator;

public class SiteSearchTrackInfo
{
    protected static final Logger log = Logger.getLogger(SiteSearchTrackInfo.class.getName());

    private final Track track, intervals;
    private final DataCollection<?> trackDC;
    private final DataCollection<AnnotatedSequence> sequencesDC;
    private final String[] seqList;
    private int totalLength;
    private int sitesCount = -1;
    private int intervalsOffset = 0;
    
    public SiteSearchTrackInfo(Track track) throws LoggedException
    {
        this.track = track;
        trackDC = track.cast( ru.biosoft.access.core.DataCollection.class );
        String intervals = trackDC.getInfo().getProperty(SiteSearchAnalysis.INTERVALS_COLLECTION_PROPERTY);
        String sequences = DataCollectionUtils.getPropertyStrict(trackDC, SiteSearchAnalysis.SEQUENCES_COLLECTION_PROPERTY);
        seqList = SiteSearchAnalysis.deserializeSequencesList(DataCollectionUtils.getPropertyStrict(trackDC, SiteSearchAnalysis.SEQUENCES_LIST_PROPERTY));
        String totalLengthStr = DataCollectionUtils.getPropertyStrict(trackDC, SiteSearchAnalysis.TOTAL_LENGTH_PROPERTY);
        totalLength = Integer.parseInt(totalLengthStr);
        if(totalLength == 0) totalLength = 1; // to avoid division by zero
        this.sequencesDC = DataElementPath.create(sequences).getDataCollection(AnnotatedSequence.class);
        this.intervals = intervals == null?null:(Track)CollectionFactory.getDataElement(intervals);
        if(this.intervals instanceof DataCollection) {
            String from = ( (DataCollection<?>)this.intervals ).getInfo().getProperty( AnalysisParametersFactory.ANALYSIS_PREFIX + "from" );
            if(from != null) {
                try
                {
                    intervalsOffset = Integer.parseInt( from );
                }
                catch( NumberFormatException e )
                {
                    // ignore
                }
            }
        }
    }
    
    public int getIntervalsOffset()
    {
        return intervalsOffset;
    }



    public Track getTrack()
    {
        return track;
    }

    public Track getIntervals()
    {
        return intervals;
    }

    public DataCollection getTrackDC()
    {
        return trackDC;
    }

    public DataCollection<AnnotatedSequence> getSequencesDC()
    {
        return sequencesDC;
    }

    public String[] getSeqList()
    {
        return seqList;
    }

    public int getTotalLength()
    {
        return totalLength;
    }

    public Iterator<Site> getTrackIterator()
    {
        return new TrackInfoIterator(getTrack());
    }
    
    public Iterator<Site> getIntervalsIterator()
    {
        if(getIntervals() == null)
            return new SequencesAsSitesIterator(getSequencesDC());
        return new TrackInfoIterator(getIntervals());
    }
    
    public Iterator<Site> getAnyTrackIterator(Track track)
    {
        return new TrackInfoIterator(track);
    }
    
    public int getSitesCount()
    {
        if(sitesCount < 0)
        {
            try
            {
                sitesCount = track.getAllSites().getSize();
            }
            // getAllSites is not supported by this track: use fallback method
            catch(UnsupportedOperationException e)
            {
                sitesCount = 0;
                for(String sequence: getSeqList())
                {
                    try
                    {
                        sitesCount += (new TrackRegion(getTrack(), DataElementPath.create(getSequencesDC(), sequence))).countSites();
                    }
                    catch( Exception e1 )
                    {
                        log.log(Level.SEVERE, e1.getMessage(), e1);
                    }
                }
            }
        }
        return sitesCount;
    }
    
    private static class SequencesAsSitesIterator extends TransformedIterator<String, Site>
    {
        private final DataCollection<AnnotatedSequence> dc;
        
        public SequencesAsSitesIterator(DataCollection<AnnotatedSequence> dc)
        {
            super(dc.getNameList().iterator());
            this.dc = dc;
        }

        @Override
        public Site transform(String name)
        {
            try
            {
                AnnotatedSequence map = dc.get(name);
                Sequence sequence = map.getSequence();
                return new SiteImpl(null, map.getName(), sequence.getStart(), sequence.getLength(), StrandType.STRAND_PLUS, sequence);
            }
            catch( Exception e )
            {
                throw new DataElementGetException(e, dc.getCompletePath().getChildPath(name), Site.class);
            }
        }
    }
    
    private class TrackInfoIterator implements Iterator<Site>
    {
        private Iterator<Site> allSitesIterator = null;
        int seqNumber = 0;
        private Iterator<Site> seqSitesIterator = null;
        private final Track track;
        
        public TrackInfoIterator(Track track)
        {
            this.track = track;
            try
            {
                allSitesIterator = this.track.getAllSites().iterator();
            }
            // getAllSites is not supported by this track: use fallback method
            catch(UnsupportedOperationException e)
            {
                nextSequence();
            }
        }
        
        private void nextSequence()
        {
            for(;seqNumber<getSeqList().length;seqNumber++)
            {
                try
                {
                    DataCollection<Site> sites = (new TrackRegion(this.track, DataElementPath.create(getSequencesDC(), getSeqList()[seqNumber]))).getSites();
                    seqSitesIterator = sites.iterator();
                    if(seqSitesIterator.hasNext())
                    {
                        seqNumber++;
                        return;
                    }
                }
                catch(Exception e)
                {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            seqSitesIterator = null;
        }

        @Override
        public boolean hasNext()
        {
            if(allSitesIterator != null) return allSitesIterator.hasNext();
            return seqSitesIterator != null && seqSitesIterator.hasNext();
        }

        @Override
        public Site next()
        {
            if(allSitesIterator != null) return allSitesIterator.next();
            if(seqSitesIterator == null) throw new NoSuchElementException();
            Site site = seqSitesIterator.next();
            if(!seqSitesIterator.hasNext()) nextSequence();
            return site;
        }
    }
}
