package ru.biosoft.bsa.analysis.maos;

import ru.biosoft.bsa.Interval;

public class TSS
{
    public final int position;
    public final boolean forwardStrand;
    public final String transcriptId;
    public String geneSymbol;
    
    public TSS(int position, boolean forwardStrand, String transcriptId, String geneSymbol)
    {
        super();
        this.position = position;
        this.forwardStrand = forwardStrand;
        this.transcriptId = transcriptId;
        this.geneSymbol = geneSymbol;
    }
    
    public Interval getPromoter(int upstream, int downstream)
    {
        return forwardStrand
                ? new Interval( position - upstream, position + downstream )
                : new Interval( position - downstream, position + upstream );
    }

    public TSS translateToInterval(Interval parent, int start)
    {
        int newPosition = position - parent.getFrom() + start;
        return new TSS( newPosition, forwardStrand, transcriptId, geneSymbol );
    }
    
    public TSS getReverseComplement(Interval interval, int start)
    {
        int newPosition = interval.getTo() - position + start;
        return new TSS( newPosition, !forwardStrand, transcriptId, geneSymbol );
    }
    
    public int getPosRelativeToTSS(int x)
    {
        int res = x - this.position;
        return forwardStrand ? res : -res;
    }
    
    public int getPosRelativeToTSS(Interval interval)
    {
        if(interval.inside( position ))
            return 0;
        int res1 = getPosRelativeToTSS( interval.getFrom() );
        int res2 = getPosRelativeToTSS( interval.getTo() );
        return Math.abs( res1 ) < Math.abs( res2 ) ? res1 : res2;
    }
    
    public String getRelativePosDescription(Interval interval)
    {
        int relPos = getPosRelativeToTSS( interval );
        return toString() + "(" + (relPos >= 0 ? "+" : "") + relPos + ")";
    }
    
    @Override
    public String toString()
    {
        return geneSymbol == null ? transcriptId : geneSymbol;
    }
    
    
}
