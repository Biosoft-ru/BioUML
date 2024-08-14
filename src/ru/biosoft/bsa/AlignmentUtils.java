package ru.biosoft.bsa;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import ru.biosoft.bsa.BAMTrack.SitesCollection.BAMSite;

public class AlignmentUtils
{
    public static Interval[] getMatchedIntervals(int start, Cigar cigar)
    {
        List<Interval> result = new ArrayList<>();
        int end = start;
        for( CigarElement e : cigar.getCigarElements() )
        {
            CigarOperator op = e.getOperator();
            if( op == CigarOperator.N )
            {
                result.add( new Interval( start, end - 1 ) );
                end += e.getLength();
                start = end;
            }
            else if( op.consumesReferenceBases() )
                end += e.getLength();
        }
        result.add( new Interval(start, end - 1) );

        return result.toArray( new Interval[result.size()] );
    }
    
    public static Interval[] getMatchedIntervals(SAMRecord r)
    {
        return getMatchedIntervals( r.getAlignmentStart() - 1, r.getCigar() );
    }
    
    public static Interval[] getMatchedIntervals(BAMSite s)
    {
        return getMatchedIntervals( s.getFrom() - 1, s.getCigar() );
    }
    
    public static boolean isContinuousAlignment(Interval[] exons, Interval[] alignment)
    {
        if(alignment.length == 0)
            return true;
        int startPos = alignment[0].getFrom();
        int exonIdx = 0;
        if(startPos < exons[exonIdx].getFrom())
            return false;
        while(startPos > exons[exonIdx].getTo())
        {
            exonIdx++;
            if(exonIdx >= exons.length)
                return false;
        }
        if(startPos < exons[exonIdx].getFrom())
            return false;
        for(int i = 1; i < alignment.length; i++)
        {
            if(alignment[i-1].getTo() != exons[exonIdx].getTo())
                return false;
            exonIdx++;
            if(exonIdx >= exons.length)
                return false;
            if(alignment[i].getFrom() != exons[exonIdx].getFrom())
                return false;
        }
        int endPos = alignment[alignment.length - 1].getTo();
        if(endPos > exons[exonIdx].getTo())
            return false;
        return true;
    }
}
