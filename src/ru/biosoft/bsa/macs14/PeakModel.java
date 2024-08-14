package ru.biosoft.bsa.macs14;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.macs14.MACS14Analysis.ChromosomeMACSTrack;
import ru.biosoft.bsa.macs14.MACS14Analysis.MACSOptions;
import ru.biosoft.util.IntArray;

public class PeakModel
{
    private final Map<String, ChromosomeMACSTrack> treatment;
    private double genomeSize;
    private int umFold;
    private int lmFold;
    private int tagSize;
    private int bandWidth;
    private final int maxPairNum;
    private int[] plusLine;
    private int[] minusLine;
    private int[] shiftedLine;
    private int d;
    private int scanWindow;
    private double minTags;
    private double maxTags;
    private int peakSize;
    private int total;
    private final Logger log;

    public PeakModel(Map<String, ChromosomeMACSTrack> chroms, int maxPairnum, MACSOptions options, Logger log) throws Exception
    {
        //maxPairNum=500, genomeSize = 0, fold=32, bandWidth=200, tagSize = 25, bg=0, maxDupTags=1
        this.treatment = chroms;
        this.log = log;
        if( options != null )
        {
            this.genomeSize = options.genomeSize;
            this.lmFold = options.lmFold;
            this.umFold = options.umFold;
            this.tagSize = options.tagSize;
            this.bandWidth = options.bandWidth;
            this.total = options.treatFiltered;
        }
        else
        {
            this.genomeSize = 0;
            this.lmFold = 10;
            this.umFold = 30;
            this.tagSize = 25;
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
        this.minTags = (double)this.total * this.lmFold * this.peakSize / this.genomeSize / 2;
        this.maxTags = (double)this.total * this.umFold * this.peakSize / this.genomeSize / 2;
        // use treatment data to build model
        int numPairedPeakpos = 0;
        int numPairedPeakposPicked = 0;
        Map<String, List<Integer>> pairedPeakpos = new HashMap<>();
        for(String chr: StreamEx.ofKeys(treatment).sorted())
        {
            List<Integer> chrPairedPeakpos = getPairedPeaks(treatment.get(chr).treat);
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
            log.log(Level.SEVERE, "Too few paired peaks (" + numPairedPeakpos
                    + ") so I can not build the model! Broader your MFOLD range parameter may erase this error.");
            throw new Exception("Not enough pairs to build model");
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
        for( Entry<String, List<Integer>> entry : pairedPeakpos.entrySet() )
        {
            MACSFWTrack treat = treatment.get(entry.getKey()).treat;
            IntArray tagsPlus = treat.getRangesByStrand(StrandType.STRAND_PLUS);
            IntArray tagsMinus = treat.getRangesByStrand(StrandType.STRAND_MINUS);
            // every paired peak has plus line and minus line
            //  add plusLine
            modelAddLine(entry.getValue(), tagsPlus, plusLine);
            //  add minusLine
            modelAddLine(entry.getValue(), tagsMinus, minusLine);
        }

        // find top
        int[] plusTops = getTopIndices( plusLine );
        int[] minusTops = getTopIndices( minusLine );
        d = minusTops[minusTops.length / 2] - plusTops[plusTops.length / 2] + 1;
        int shiftSize = d / 2;
        scanWindow = Math.max(d, tagSize) * 2;
        // a shifted model
        int[] plusShifted = IntStreamEx.of( plusLine, shiftSize, windowSize ).prepend( IntStreamEx.constant( 0, shiftSize ) ).toArray();
        int[] minusShifted = IntStreamEx.of( minusLine, 0, windowSize-shiftSize ).append( IntStreamEx.constant( 0, shiftSize ) ).toArray();
        shiftedLine = IntStreamEx.zip( plusShifted, minusShifted, Integer::sum ).toArray();
        //#print "d:",d,"shiftSize:",shiftSize
        //#print len(plusLine),len(minusLine),len(plusShifted),len(minusShifted),len(shiftedLine)
        return true;
    }

    private int[] getTopIndices(int[] numbers)
    {
        int top = IntStreamEx.of( numbers ).max().getAsInt();
        return IntStreamEx.ofIndices( numbers, val -> val == top ).toArray();
    }

    private void modelAddLine(List<Integer> pos1, IntArray tagsPlus, int[] line)
    {
        /*Project each pos in pos2 which is included in
        [pos1-self.peaksize,pos1+self.peaksize] to the line.*/
        int i1 = 0; // index for pos1
        int i2 = 0; // index for pos2
        int i2Prev = 0; // index for pos2 in previous pos1
        // [pos1-self.peaksize,pos1+self.peaksize]
        // region
        int i1Max = pos1.size();
        int i2Max = tagsPlus.size();
        boolean flagFindOverlap = false;

        while( i1 < i1Max && i2 < i2Max )
        {
            int p1 = pos1.get(i1);
            int p2 = tagsPlus.get(i2);
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

        List<Integer> plusPeaksInfo = findPeaksNaive(treatment.getRangesByStrand(StrandType.STRAND_PLUS));
        log.log(Level.FINE, "Number of unique tags on + strand: " + treatment.getRangesByStrand(StrandType.STRAND_PLUS).size());
        log.log(Level.FINE, "Number of peaks in + strand: " + plusPeaksInfo.size()/2);
        List<Integer> minusPeaksInfo = findPeaksNaive(treatment.getRangesByStrand(StrandType.STRAND_MINUS));
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

    private List<Integer> findPeaksNaive(IntArray intArray)
    {
        /*Naively call peaks based on tags counting.

        Return peak positions and the tag number in peak region by a tuple list [(pos,num)].*/

        // store peak pos in every peak region and unique tag number in every peak region
        List<Integer> peakInfo = new ArrayList<>();
        if( intArray.size() < 2 )
        {
            return peakInfo;
        }
        int pos = intArray.get(0);
        // list to find peak pos
        List<Integer> currentTagList = new ArrayList<>();
        currentTagList.add(pos);
        for( int i = 1; i < intArray.size(); i++ )
        {
            pos = intArray.get(i);
            if( ( pos - currentTagList.get(0) + 1 ) > peakSize )
            { // call peak in currentTagList
                // a peak will be called if tag number is ge min tags.
                if( currentTagList.size() >= minTags  && currentTagList.size() <= maxTags )
                {
                    peakInfo.add(getPeakPosNaive(currentTagList));
                    peakInfo.add(currentTagList.size());
                }
                currentTagList.clear(); // reset currentTagList
            }
            currentTagList.add(pos); // add pos while 1. no need to call peak; 2. currentTagList is []
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

    public int getD()
    {
        return d;
    }

    public double getMinTags()
    {
        return minTags;
    }

    public int getScanWindow()
    {
        return scanWindow;
    }

    public double getMaxTags()
    {
        return maxTags;
    }
}
