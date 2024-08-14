package ru.biosoft.bsa;

import java.security.InvalidParameterException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.bsa.analysis.GeneSetToTrackParameters;

/**
 * Track based on promoters track (GeneSetToTrack analysis result) which has truncated sites compared to original ones
 * Unlike {@link ResizedTrack} it resizes sites relative to TSS position
 * Used in {@link OptimizeSiteSearchAnalysis}
 */
public class ShrinkedTrack extends TransformedTrack
{
    private int newFrom, newTo;
    private int from, to;
    
    public ShrinkedTrack(Track source, int newFrom, int newTo)
    {
        super(source);
        this.newFrom = newFrom;
        this.newTo = newTo;
        readPromoterWindow();
        if(this.newFrom > 0) this.newFrom--;
        if(this.newTo > 0) this.newTo--;
        if(this.from > 0) this.from--;
        if(this.to > 0) this.to--;
    }
    
    private void readPromoterWindow()
    {
        Integer from = null, to = null;
        if(source instanceof DataCollection)
        {
            AnalysisParameters parameters = AnalysisParametersFactory.read(source);
            if(parameters instanceof GeneSetToTrackParameters)
            {
                from = ((GeneSetToTrackParameters)parameters).getFrom();
                to = ((GeneSetToTrackParameters)parameters).getTo();
            }
        }
        if(from == null)
        {
            throw new InvalidParameterException("Specified track wasn't created by GeneSetToTrack analysis: "+source.getName());
        }
        this.from = from;
        this.to = to;
    }
    
    @Override
    protected Site transformSite(Site s)
    {
        if(s == null) return null;
        if(this.from == this.newFrom && this.to == this.newTo) return s;
        int siteFrom = s.getFrom();
        int siteTo = s.getTo();
        siteFrom += s.getStrand()==StrandType.STRAND_PLUS?newFrom-from:to-newTo;
        siteTo += s.getStrand()==StrandType.STRAND_PLUS?newTo-to:from-newFrom;
        return new SiteImpl(s.getOrigin(), s.getName(), s.getType(), s.getBasis(), s.getStrand() == StrandType.STRAND_MINUS ? siteTo
                : siteFrom, siteTo - siteFrom + 1, s.getPrecision(), s.getStrand(), s.getOriginalSequence(), s.getComment(), s.getProperties());
    }
}
