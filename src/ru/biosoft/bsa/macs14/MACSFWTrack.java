package ru.biosoft.bsa.macs14;

import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.IntArray;

/**
 * Fixed Width Ranges along the sequence (chromosome) (commonly with the same annotation type).
 * Ranges can be sorted by calling this.sort() function.
 */
public class MACSFWTrack
{
    //fixedWidth is the fixed-width for all ranges
    private IntArray rangesPos = new IntArray();
    private IntArray rangesNeg = new IntArray();
    private int total;
    private int totalUnique;
    private int totalFiltered;
    private boolean sorted = false;
    
    public MACSFWTrack()
    {
        total = 0; // total tags
        totalUnique = 0; // total unique tags
    }

    /**
     * Should be called after all sites added
     */
    public void finalizeAddition()
    {
        rangesPos.compress();
        rangesNeg.compress();
        sort();
    }

    /**
     * Add site to track
     * @param site
     */
    public void addSite(Site site)
    {
        // BioUML has 1-based positions, while MACS algorithm has 0-based
        addLocation(site.getStrand()==StrandType.STRAND_MINUS?site.getStart():site.getStart()-1, site.getStrand());
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
    public IntArray getRangesByStrand(int strand)
    {
        if( strand == StrandType.STRAND_PLUS )
        {
            return rangesPos;
        }
        else
        {
            return rangesNeg;
        }
    }

    private void sort()
    {
        /*Naive sorting for tags.*/
        rangesPos.sort();
        rangesNeg.sort();
        this.sorted  = true;
    }
    
    public void shift(int shiftSize)
    {
        int i;
        for(i=0; i<rangesPos.size(); i++)
        {
            rangesPos.set(i, rangesPos.get(i)+shiftSize);
        }
        for(i=0; i<rangesNeg.size(); i++)
        {
            rangesNeg.set(i, rangesNeg.get(i)-shiftSize);
        }
    }

    public void mergePlusMinusLocationsNaive()
    {
        /*Merge minus strand ranges to plus strand. The duplications
        on a single strand is erased. But if the same position is on
        both pos and neg strand, keep them both.*/

        totalUnique = 0;

        int lenp = rangesPos.size();
        int lenm = rangesNeg.size();
        IntArray newPlus = new IntArray(lenp+lenm);
        int ip = 0;
        int im = 0;
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
        rangesPos.compress();
        rangesNeg.clear();
        totalUnique = newPlus.size();
    }

    public int getTotal()
    {
        return total;
    }

    public int getTotalFiltered()
    {
        return totalFiltered;
    }

    public int getTotalUnique()
    {
        return totalUnique;
    }

    /**
     * Filter the duplicated reads.
     * @param maxDupTags - maximum number of overlapping tags allowed (-1 = no filter, return immediately)
     */
    public void filterDup(int maxDupTags)
    {
        if(maxDupTags == -1) return;
        if(!sorted) sort();
        totalFiltered = 0;
        IntArray newRangesPos = new IntArray();
        if(rangesPos.size() > 0)
        {
            newRangesPos.add(rangesPos.get(0));
            int n = 1;
            int currentLoc = rangesPos.get(0);
            for(int i=1; i<rangesPos.size(); i++)
            {
                int p = rangesPos.get(i);
                if(p == currentLoc)
                {
                    n++;
                    if(n <= maxDupTags) newRangesPos.add(p);
                } else
                {
                    currentLoc = p;
                    newRangesPos.add(p);
                    n = 1;
                }
            }
            totalFiltered += newRangesPos.size();
        }
        rangesPos = newRangesPos;
        rangesPos.compress();
        IntArray newRangesNeg = new IntArray();
        if(rangesNeg.size() > 0)
        {
            newRangesNeg.add(rangesNeg.get(0));
            int n = 1;
            int currentLoc = rangesNeg.get(0);
            for(int i=1; i<rangesNeg.size(); i++)
            {
                int p = rangesNeg.get(i);
                if(p == currentLoc)
                {
                    n++;
                    if(n <= maxDupTags) newRangesNeg.add(p);
                } else
                {
                    currentLoc = p;
                    newRangesNeg.add(p);
                    n = 1;
                }
            }
            totalFiltered += newRangesNeg.size();
        }
        rangesNeg = newRangesNeg;
        rangesNeg.compress();
    }
}
