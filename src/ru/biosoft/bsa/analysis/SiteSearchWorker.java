package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.analysis.SequenceAccessor.CachedSequenceRegion;

public class SiteSearchWorker implements Callable<Void>
{
    private SequenceAccessor sequence;
    private int from, to;
    private Collection<SiteModel> siteModels;
    private WritableTrack track;
    private int overlap;

    public SiteSearchWorker(SequenceAccessor sequence, int from, int to, Collection<SiteModel> siteModels, WritableTrack track, int overlap)
    {
        this.sequence = sequence;
        this.from = from;
        this.to = to;
        this.siteModels = siteModels;
        this.track = track;
        this.overlap = overlap;
    }

    @Override
    public Void call() throws Exception
    {
        CachedSequenceRegion forward = sequence.getSubSequence(from, to - from + 1, false);
        CachedSequenceRegion reverse = new CachedSequenceRegion(forward, forward.getLength() - 1 + forward.getStart(), forward.getLength(),
                true);

        for( SiteModel siteModel : siteModels )
        {
            if(Thread.currentThread().isInterrupted())
                return null;
            
            int start = forward.getStart() + overlap - siteModel.getLength() + 1;
            start = Math.max(start, forward.getStart());
            int length = forward.getLength() - ( start - forward.getStart() );
            siteModel.findAllSites(new CachedSequenceRegion(forward, start, length, false), track);
            siteModel.findAllSites(new CachedSequenceRegion(reverse, reverse.getStart(), length, false), track);
        }

        return null;
    }

    public static List<SiteSearchWorker> getWorkers(SequenceAccessor sequence, int from, int to, Collection<SiteModel> siteModels,
            WritableTrack track, int maxLengthPerWorker)
    {
        List<SiteSearchWorker> result = new ArrayList<>();
        
        int maxSiteModelLength = 0;
        for(SiteModel siteModel : siteModels)
            maxSiteModelLength = Math.max(maxSiteModelLength, siteModel.getLength());
        
        int overlap = 0;
        while(from <= to)
        {
            int workerFrom = from - overlap;
            int workerTo = Math.min(to, workerFrom + maxLengthPerWorker - 1);
            SiteSearchWorker worker = new SiteSearchWorker(sequence, workerFrom, workerTo, siteModels, track, overlap);
            result.add(worker);
            
            from = workerTo + 1;
            overlap = maxSiteModelLength - 1;
        }
        return result;
    }

}
