package ru.biosoft.bsa.macs;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.macs.MACSAnalysis.ChromosomeMACSTrack;
import ru.biosoft.bsa.macs.MACSAnalysis.MACSOptions;

public class PeakModel
{

    private Map<String, ChromosomeMACSTrack> treatment;
    private double genomeSize;
    private int fold;
    private int tagSize;
    private int mr;
    private int bandWidth;
    private double bgRedundantRate;
    private int maxPairNum;
    private int[] plusLine;
    private int[] minusLine;
    private int[] shiftedLine;
    private int d;
    private int scanWindow;
    private int minTags;
    private int peakSize;
    private int totalUnique;
    private Logger log;

    public PeakModel(Map<String, ChromosomeMACSTrack> chroms, int maxPairnum, MACSOptions options, Logger log) throws Exception
    {
        //maxPairNum=500, genomeSize = 0, fold=32, bandWidth=200, tagSize = 25, bg=0, maxDupTags=1
        this.treatment = chroms;
        this.log = log;
        if( options != null )
        {
            this.genomeSize = options.genomeSize;
            this.fold = options.mFold;
            this.tagSize = options.tagSize;
            this.bgRedundantRate = options.bgRedundant;
            this.mr = options.maxDupTags;
            this.bandWidth = options.bandWidth;
            this.totalUnique = options.treatUnique;
        }
        else
        {
            this.genomeSize = 0;
            this.fold = 32;
            this.tagSize = 25;
            this.mr = 1;
            this.bandWidth = 200;
        }
        this.maxPairNum = maxPairnum;
        this.plusLine = null;
        this.minusLine = null;
        this.shiftedLine = null;
        this.d = 0;
        this.scanWindow = 0;
        this.minTags = 0;
        this.peakSize = 0;
        this.build();
    }

    private void build() throws Exception
    {
        /*Build the model.
         * prepare this.d, this.scanWindow, this.plusLine, this.minusLine and this.shiftedLine to use.
        */
        peakSize = 2 * bandWidth;
        //# mininum unique hits on single strand
        this.minTags = (int) ( (double)this.totalUnique * this.fold * this.peakSize / this.genomeSize / 2 );
        // use treatment data to build model
        int numPairedPeakpos = 0;
        int numPairedPeakposPicked = 0;
        Map<String, List<Integer>> pairedPeakpos = new HashMap<>();
        for( Map.Entry<String, ChromosomeMACSTrack> entry : treatment.entrySet() )
        {
            String chr = entry.getKey();
            List<Integer> chrPairedPeakpos = getPairedPeaks(entry.getValue().treat);
            numPairedPeakpos += chrPairedPeakpos.size();
            numPairedPeakposPicked = numPairedPeakpos;
            if( numPairedPeakpos > maxPairNum )
            {
                chrPairedPeakpos = chrPairedPeakpos.subList(0, chrPairedPeakpos.size()-(numPairedPeakposPicked-maxPairNum));
                numPairedPeakposPicked = maxPairNum;
                pairedPeakpos.put(chr, chrPairedPeakpos);
                break;
            }
            pairedPeakpos.put(chr, chrPairedPeakpos);
        }
        log.info("#2 number of paired peaks: " + numPairedPeakpos);
        if( numPairedPeakpos < 100 )
        {
            log.log(Level.SEVERE, "Too few paired peaks (%d) so I can not build the model! Lower your MFOLD parameter may erase this error.");
            throw new Exception("Too few paired peaks");
        }
        else if( numPairedPeakpos < this.maxPairNum )
        {
            log.warning("Fewer paired peaks " + numPairedPeakpos + " than " + this.maxPairNum
                    + ". Model may not be build well! Lower your MFOLD parameter may erase this warning.");
        }
        log.log(Level.FINE, "Use " + numPairedPeakposPicked + " pairs to build the model.");
        pairedPeakModel(pairedPeakpos);
    }

    private boolean pairedPeakModel(Map<String, List<Integer>> pairedPeakpos)
    {
        /*Use paired peak positions and treatment tag positions to build the model.
        Modify d, modelShift size and scanWindow size. and extra, plusLine, minusLine and shiftedLine for plotting.
        */
        int windowSize = 1 + 2 * peakSize;
        plusLine = new int[windowSize];
        minusLine = new int[windowSize];
        //TODO: think how to build model for all chromosomes
        //for chrom in pairedPeakpos.keys():
        //   pairedPeakposChrom = pairedPeakpos[chrom]
        //tags = treatment.getRangesByChr(chrom)
        for( Map.Entry<String, List<Integer>> entry : pairedPeakpos.entrySet() )
        {
            MACSFWTrack treat = treatment.get(entry.getKey()).treat;
            List<Integer> tagsPlus = treat.getRangesByStrand(StrandType.STRAND_PLUS);
            List<Integer> tagsMinus = treat.getRangesByStrand(StrandType.STRAND_MINUS);
            // every paired peak has plus line and minus line
            //  add plusLine
            modelAddLine(entry.getValue(), tagsPlus, plusLine);
            //  add minusLine
            modelAddLine(entry.getValue(), tagsMinus, minusLine);
        }

        // find top
        List<Integer> plusTops = new ArrayList<>();
        List<Integer> minusTops = new ArrayList<>();
        int plusMax = getMaxValue(plusLine);
        int minusMax = getMaxValue(minusLine);
        for( int i = 0; i < windowSize; i++ )
        {
            if( plusLine[i] == plusMax )
                plusTops.add(i);
            if( minusLine[i] == minusMax )
                minusTops.add(i);
        }
        d = minusTops.get(minusTops.size() / 2) - plusTops.get(plusTops.size() / 2) + 1;
        int shiftSize = d / 2;
        scanWindow = Math.max(d, tagSize) * 2;
        // a shifted model
        shiftedLine = new int[windowSize];
        int[] plusShifted = new int[windowSize];
        for( int i = shiftSize; i < windowSize; i++ )
        {
            plusShifted[i] = plusLine[i - shiftSize];
        }
        int[] minusShifted = new int[windowSize];
        for( int i = shiftSize; i < windowSize; i++ )
        {
            minusShifted[i - shiftSize] = minusLine[i];
        }
        //#print "d:",d,"shiftSize:",shiftSize
        //#print len(plusLine),len(minusLine),len(plusShifted),len(minusShifted),len(shiftedLine)
        for( int i = 0; i < windowSize; i++ )
        {
            shiftedLine[i] = minusShifted[i] + plusShifted[i];
        }
        return true;
    }


    private int getMaxValue(int[] numbers)
    {
        int maxValue = numbers[0];
        for( int i = 1; i < numbers.length; i++ )
        {
            if( numbers[i] > maxValue )
            {
                maxValue = numbers[i];
            }
        }
        return maxValue;
    }

    private void modelAddLine(List<Integer> pos1, List<Integer> pos2, int[] line)
    {
        /*Project each pos in pos2 which is included in
        [pos1-self.peaksize,pos1+self.peaksize] to the line.*/
        int i1 = 0; // index for pos1
        int i2 = 0; // index for pos2
        int i2Prev = 0; // index for pos2 in previous pos1
        // [pos1-self.peaksize,pos1+self.peaksize]
        // region
        int i1Max = pos1.size();
        int i2Max = pos2.size();
        boolean flagFindOverlap = false;

        while( i1 < i1Max && i2 < i2Max )
        {
            int p1 = pos1.get(i1);
            int p2 = pos2.get(i2);
            if( p1 - peakSize > p2 ) // move pos2
            {
                i2++;
            }
            else if( p1 + peakSize < p2 ) // move pos1
            {
                i1++;
                i2 = i2Prev; // search minus peaks from previous index
                flagFindOverlap = false;
            }
            else
            { // overlap!
                if( !flagFindOverlap )
                {
                    flagFindOverlap = true;
                    i2Prev = i2; // only the first index is recorded
                }
                // project
                for( int i = p2 - p1 + peakSize - tagSize / 2; i < p2 - p1 + peakSize + tagSize / 2; i++ )
                {
                    if( i >= 0 && i < line.length )
                        line[i]++;
                }
                i2++;
            }
        }
    }

    private List<Integer> getPairedPeaks(MACSFWTrack treatment)
    {
        /*Call paired peaks from fwtrackI object.
        Return paired peaks center positions.
        */
        List<Integer> pairedPeaksPos = new ArrayList<>();

        List<Integer> plusPeaksInfo = findPeaksNaive(treatment.getRangesByStrand(StrandType.STRAND_PLUS), treatment.getCommentsByStrand(StrandType.STRAND_PLUS));
        log.log(Level.FINE, "Number of unique tags on + strand: " + treatment.getRangesByStrand(StrandType.STRAND_PLUS).size());
        log.log(Level.FINE, "Number of peaks in + strand: " + plusPeaksInfo.size()/2);
        List<Integer> minusPeaksInfo = findPeaksNaive(treatment.getRangesByStrand(StrandType.STRAND_MINUS), treatment.getCommentsByStrand(StrandType.STRAND_MINUS));
        log.log(Level.FINE, "Number of unique tags on - strand:  " + treatment.getRangesByStrand(StrandType.STRAND_MINUS).size());
        log.log(Level.FINE, "Number of peaks in - strand: " + minusPeaksInfo.size()/2);
        if( plusPeaksInfo == null || minusPeaksInfo == null )
        {
            log.log(Level.FINE, "Chromosome is discarded!");
        }
        else
        {
            pairedPeaksPos = findPairCenter(plusPeaksInfo, minusPeaksInfo);
            log.log(Level.FINE, "Number of paired peaks: " + pairedPeaksPos.size());
        }
        return pairedPeaksPos;
    }

    private List<Integer> findPairCenter(List<Integer> plusPeaks, List<Integer> minusPeaks)
    {
        int ip = 0; // index for plus peaks
        int im = 0; // index for minus peaks
        int imPrev = 0; // index for minus peaks in previous plus peak
        List<Integer> pairCenters = new ArrayList<>();

        //TODO:
        //pluspeaks and minuspeaks are double sized, 1st is pos, 2nd is count
        //may be rewrite findPeaksNaive to return a list of pairs

        int ipMax = plusPeaks.size() / 2;
        int imMax = minusPeaks.size() / 2;
        boolean flagFindOverlap = false;
        while( ip < ipMax && im < imMax )
        {
            int pp = plusPeaks.get(ip * 2); // for (peakposition, tagnumber in peak)
            int pn = plusPeaks.get(ip * 2 + 1);
            int mp = minusPeaks.get(im * 2);
            int mn = minusPeaks.get(im * 2 + 1);
            if( pp - peakSize > mp ) // move minus
                im++;
            else if( pp + peakSize < mp ) // move plus
            {
                ip++;
                im = imPrev; // search minus peaks from previous index
                flagFindOverlap = false;
            }
            else
            { // overlap!
                if( !flagFindOverlap )
                {
                    flagFindOverlap = true;
                    imPrev = im; // only the first index is recorded
                }
                double rel = (double)pn / mn;
                if( rel < 2 && rel > 0.5 )
                { // number tags in plus and minus peak region are comparable...
                    if( pp < mp )
                    {
                        pairCenters.add( ( pp + mp ) / 2);
                        //log.debug ( "distance: %d, minus: %d, plus: %d" % (mp-pp,mp,pp))
                    }
                }
                im++;
            }
        }
        return pairCenters;
    }

    private List<Integer> findPeaksNaive(List<Integer> taglist, List<Integer> countlist)
    {
        /*Naively call peaks based on tags counting. The redundant rate in peak region must be less than 2-fold of background( global) redundant rate

        Return peak positions and the tag number in peak region by a tuple list [(pos,num)].*/

        // store peak pos in every peak region and unique tag number in every peak region
        List<Integer> peakInfo = new ArrayList<>();
        if( taglist.size() < 2 )
        {
            return peakInfo;
        }
        int pos = taglist.get(0);
        int count = countlist.get(0);
        // list to find peak pos
        List<Integer> currentTagList = new ArrayList<>();
        currentTagList.add(pos);
        int currentRedundantTags = Math.max(count - mr, 0);
        for( int i = 1; i < taglist.size(); i++ )
        {
            pos = taglist.get(i);
            count = countlist.get(i);
            if( ( pos - currentTagList.get(0) + 1 ) > peakSize )
            { // call peak in currentTagList
                // a peak will be called if redundant tags are less
                // than 2*redundant rate of background
                double currentRedundantRate = ((double)currentRedundantTags) / ( currentRedundantTags + currentTagList.size() );
                if( currentRedundantRate <= 2 * bgRedundantRate )
                {
                    // a peak will be called if tag number is ge min tags.
                    if( currentTagList.size() >= minTags )
                    {
                        peakInfo.add(getPeakPosNaive(currentTagList));
                        peakInfo.add(currentTagList.size());
                    }
                }
                currentTagList.clear(); // reset currentTagList
                currentRedundantTags = 0; // reset currentRedundantTags number
            }
            currentTagList.add(pos); // add pos while 1. no need to call peak; 2. currentTagList is []
            currentRedundantTags += Math.max(count - mr, 0);
        }
        return peakInfo;
    }

    private int getPeakPosNaive(List<Integer> posList)
    {
        /*Naively calculate the position of peak.
        return the highest peak summit position.
        */
        int size = posList.size();
        int peakLength = posList.get(size - 1) + 1 - posList.get(0) + tagSize;
        int start = posList.get(0) - tagSize / 2;
        int[] horizonLine = new int[peakLength]; // the line for tags to be projected
        for( int pos : posList )
        {
            //# projected point
            for( int pp = ( pos - start - tagSize / 2 ); pp < pos - start + tagSize / 2; pp++ )
            {
                horizonLine[pp]++;
            }
        }
        List<Integer> topPos = new ArrayList<>(); // to record the top positions. Maybe > 1
        int topPNum = 0; // the maximum number of projected points
        for( int pp = 0; pp < peakLength; pp++ )
            // find the peak position as the highest point
            if( horizonLine[pp] > topPNum )
            {
                topPNum = horizonLine[pp];
                topPos.clear();
                topPos.add(pp);
            }
            else if( horizonLine[pp] == topPNum )
            {
                topPos.add(pp);
            }
        return topPos.get(topPos.size() / 2) + start;
    }

    public void setD(int d)
    {
        this.d = d;
    }

    public int getD()
    {
        return d;
    }

    public void setMinTags(int minTags)
    {
        this.minTags = minTags;
    }

    public int getMinTags()
    {
        return minTags;
    }

    public void setScanWindow(int scanWindow)
    {
        this.scanWindow = scanWindow;
    }

    public int getScanWindow()
    {
        return scanWindow;
    }

}
