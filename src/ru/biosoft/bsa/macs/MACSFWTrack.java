package ru.biosoft.bsa.macs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;

/**
 * Fixed Width Ranges along the sequence (chromosome) (commonly with the same annotation type).
 * Ranges can be sorted by calling this.sort() function.
 */
public class MACSFWTrack
{
    private int fixedWidth;
    //fixedWidth is the fixed-width for all ranges
    private List<Integer> rangesPos;
    private List<Integer> rangesNeg;
    private List<Integer> commentsPos;
    private List<Integer> commentsNeg;
    //private boolean wellMerged;
    private int total;
    private int totalUnique;

    public MACSFWTrack(Track track, String chromName, int length)
    {
        fixedWidth = 0;
        rangesPos = new ArrayList<>();
        rangesNeg = new ArrayList<>();
        commentsPos = new ArrayList<>();
        commentsNeg = new ArrayList<>();
        //wellMerged = false;
        total = 0; // total tags
        totalUnique = 0; // total unique tags

        try
        {
            DataCollection<Site> dc = track.getSites(chromName, 1, length);
            if( dc == null )
            {
                return;
            }
            for(Site s : dc)
            {
                // BioUML has 1-based positions, while MACS algorithm has 0-based
                addLocation(s.getStrand()==StrandType.STRAND_MINUS?s.getStart():s.getStart()-1, s.getStrand());
                if( fixedWidth == 0 )
                {
                    fixedWidth = s.getLength();
                }
            }
            mergeOverlap();
        }
        catch( Exception e )
        {
            //TODO:
            //process exception
        }

    }

    /**
     * Add a range to the list according to the sequence name.
     * @param fiveendpos -- 5' end pos, left for plus strand, neg for neg strand
     * @param strand     -- 0: plus, 1: minus
    */
    private void addLocation(int fiveEndPos, int strand)
    {
        if( strand == StrandType.STRAND_PLUS )
        {
            rangesPos.add(fiveEndPos);
        }
        else if( strand == StrandType.STRAND_MINUS )
        {
            rangesNeg.add(fiveEndPos);
        }
        total++;
    }

    /**
     * Returns array of locations by strand.
     * Not recommended! Use generate_rangeI_by_chr() instead.
     */
    public List<Integer> getRangesByStrand(int strand)
    {
        return strand == StrandType.STRAND_PLUS ? rangesPos : rangesNeg;
    }

    public List<Integer> getCommentsByStrand(int strand)
    {
        /*Return array of comments by chromosome.*/
        return strand == StrandType.STRAND_PLUS ? commentsPos : commentsNeg;
    }

    private int length()
    {
        /*Total sequenced length = total number of tags * width of tag*/
        return total * fixedWidth;
    }

    private void sort()
    {
        /*Naive sorting for tags. After sorting, comments are massed up.*/
        Collections.sort(rangesPos);
        Collections.sort(rangesNeg);
    }
    
    public void shift(int shiftSize)
    {
        rangesPos.replaceAll( p -> p+shiftSize );
        rangesNeg.replaceAll( p -> p-shiftSize );
    }

    private void mergeOverlap()
    {
        sort();
        totalUnique = 0;
        rangesPos = mergeOverlapStrand(rangesPos, commentsPos);
        rangesNeg = mergeOverlapStrand(rangesNeg, commentsNeg);
        //wellMerged = true;
    }

    private List<Integer> mergeOverlapStrand(List<Integer> ranges, List<Integer> comments)
    {
        /*merge the SAME ranges. Record the duplicate number in __comments{}
        *Note: different with the merge_overlap() in TrackI class,
        which merges the overlapped ranges.*/
        
        List<Integer> newPlus = new ArrayList<>();
        comments.clear();
        if( ranges.size() > 1 )
        {
            newPlus.add(ranges.get(0));
            comments.add(1);
            int n = 0; //the position in new list
            for( int p = 1; p < ranges.size(); p++ )
            {
                if( ranges.get(p).equals(newPlus.get(n)) )
                {
                    comments.set(n, comments.get(n) + 1);
                }
                else
                {
                    newPlus.add(ranges.get(p));
                    comments.add(1);
                    n++;
                }
            }
            totalUnique += newPlus.size();
        }
        return newPlus;
    }

    public void mergePlusMinusRangesWithDuplicates()
    {
        /*Merge minus strand ranges to plus strand. The duplications
        on a single strand is erased. But if the same position is on
        both pos and neg strand, keep them both.
        Side effect: Reset the comments. totalUnique is set to 0.*/

        totalUnique = 0;
        commentsPos.clear();
        commentsNeg.clear();

        List<Integer> newPlus = new ArrayList<>();
        int ip = 0;
        int im = 0;
        int lenp = rangesPos.size();
        int lenm = rangesNeg.size();
        while( ip < lenp && im < lenm )
        {
            if( rangesPos.get(ip) < rangesNeg.get(im) )
            {
                newPlus.add(rangesPos.get(ip));
                ip++;
            }
            else
            {
                newPlus.add(rangesNeg.get(im));
                im++;
            }
        }
        while( im < lenm )
        {
            //# add rest of minus tags
            newPlus.add(rangesNeg.get(im));
            im++;
        }
        while( ip < lenp )
        {
            //# add rest of plus tags
            newPlus.add(rangesPos.get(ip));
            ip++;
        }
        rangesPos = newPlus;
        rangesNeg.clear();
        total = newPlus.size();
    }

    public int getTotal()
    {
        return total;
    }

    public int getTotalUnique()
    {
        return totalUnique;
    }
}
