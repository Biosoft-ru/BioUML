package ru.biosoft.bsa.macs14;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import java.util.logging.Logger;

import ru.biosoft.analysis.Stat;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.macs14.MACS14Analysis.MACSOptions;
import ru.biosoft.util.IntArray;

public class PeakDetect
{
    private final Logger log;

    MACSFWTrack treat;
    MACSFWTrack control;
    MACSOptions options;
    
    String chrName;
    int minTags;
    double treatToControlRatio;
    List<Peak> finalPeaks, finalNegativePeaks;

    public PeakDetect(MACSOptions options, Logger log)
    {
        this.options = options;
        this.log = log;
        finalPeaks = new ArrayList<>();
        finalNegativePeaks = new ArrayList<>();
    }

    public void callPeaks(MACSFWTrack treat, MACSFWTrack control, String chrName)
    {
        this.treat = treat;
        this.control = control;
        this.chrName = chrName;
        if(this.control != null)
            callPeaksWithControl();
        else
            callPeaksWithoutControl();
    }
    
    public List<Peak> getPeaks()
    {
        return finalPeaks;
    }
    
    private void callPeaksWithControl()
    {
        /*lambdaBackground = (double)options.scanWindow*options.treatFiltered/options.genomeSize;
        log.log(Level.FINE, "Background lambda: "+lambdaBackground);
        minTags = Stat.poissonDistributionInv(1-options.pvalue, lambdaBackground)+1;
        log.log(Level.FINE, "MinTags: "+minTags);*/
        treatToControlRatio = ((double)options.treatFiltered)/options.controlFiltered;
        if(treatToControlRatio > 2 || treatToControlRatio < 0.5)
            log.warning("Treatment tags and Control tags are uneven! FDR may be wrong!");
        log.log(Level.FINE, "Shifting treat...");
        treat.shift(options.d/2);
        log.log(Level.FINE, "Merging treat...");
        treat.mergePlusMinusLocationsNaive();
        log.log(Level.FINE, "Number of tags: "+treat.getTotal());
        // TODO: save profiled track if necessary (see PeakDetect.zbdg_write in MACS source)
        log.log(Level.FINE, "callPeaksFromTrackI(treat)...");
        List<Peak> peakCandidates = callPeaksFromTrackI(treat, options.treatFiltered);
        log.log(Level.FINE, "Shifting control...");
        control.shift(options.d/2);
        log.log(Level.FINE, "Merging control...");
        control.mergePlusMinusLocationsNaive();
        log.log(Level.FINE, "Number of tags: "+control.getTotal());
        
        // TODO: save profiled track if necessary
        log.log(Level.FINE, "callPeaksFromTrackI(control)...");
        List<Peak> negativePeakCandidates = callPeaksFromTrackI(control, options.controlFiltered);
        log.log(Level.FINE, "use control data to filter peak candidates...");
        finalPeaks.addAll(filterWithControl(peakCandidates, treat, options.treatFiltered, control, options.controlFiltered, options.toSmall, false, true));
        log.log(Level.FINE, "find negative peaks by swapping treat and control...");
        finalNegativePeaks.addAll(filterWithControl(negativePeakCandidates, control, options.controlFiltered, treat, options.treatFiltered, options.toSmall, false, true));
    }
    
    private void callPeaksWithoutControl()
    {
        /*lambdaBackground = (double)options.scanWindow*options.treatFiltered/options.genomeSize;
        log.log(Level.FINE, "Background lambda: "+lambdaBackground);
        minTags = Stat.poissonDistributionInv(1-options.pvalue, lambdaBackground)+1;
        log.log(Level.FINE, "MinTags: "+minTags);*/
        log.log(Level.FINE, "Shifting treat...");
        treat.shift(options.d/2);
        log.log(Level.FINE, "Merging treat...");
        treat.mergePlusMinusLocationsNaive();
        log.log(Level.FINE, "Number of tags: "+treat.getTotal());
        // TODO: save profiled track if necessary
        log.log(Level.FINE, "callPeaksFromTrackI(treat)...");
        List<Peak> peakCandidates = callPeaksFromTrackI(treat, options.treatFiltered);
        log.log(Level.FINE, "use self to calculate local lambda and filter peak candidates...");
        finalPeaks.addAll(filterWithControl(peakCandidates, treat, options.treatFiltered, treat, options.treatFiltered, false, true, false));
    }
    
    public void addFDR()
    {
        HashMap<Double, Double> pvalue2Fdr = new HashMap<>();
        List<Double> pvaluesFinal = new ArrayList<>();
        List<Double> pvaluesNegative = new ArrayList<>();
        for(Peak p: finalPeaks)
        {
            pvaluesFinal.add(p.pvalue);
            pvalue2Fdr.put(p.pvalue, null);
        }
        for(Peak p: finalNegativePeaks)
        {
            pvaluesNegative.add(p.pvalue);
        }
        Collections.sort(pvaluesFinal, Collections.reverseOrder());
        int pvaluesFinalLength = pvaluesFinal.size();
        Collections.sort(pvaluesNegative, Collections.reverseOrder());
        int pvaluesNegativeLength = pvaluesNegative.size();
        int indexP2fPos = 0;
        int indexP2fNeg = 0;
        List<Double> pvalues = Collections.list(Collections.enumeration(pvalue2Fdr.keySet()));
        Collections.sort(pvalues, Collections.reverseOrder());
        for(Double p: pvalues)
        {
            while(indexP2fPos<pvaluesFinalLength && p<=pvaluesFinal.get(indexP2fPos))
                indexP2fPos++;
            int nFinal = indexP2fPos;
            
            while(indexP2fNeg<pvaluesNegativeLength && p<=pvaluesNegative.get(indexP2fNeg))
                indexP2fNeg++;
            int nNegative = indexP2fNeg;
            pvalue2Fdr.put(p, 100.0*nNegative/nFinal);
        }
        for(Peak p: finalPeaks)
            p.fdr = pvalue2Fdr.get(p.pvalue);
    }
    
    private List<Peak> filterWithControl(List<Peak> source, MACSFWTrack treat, int treatSize, MACSFWTrack control, int controlSize,
            boolean toSmall, boolean pass1k, boolean fakeWhenMissing)
    {
        double lambdaBg0 = ((double)options.scanWindow)*treatSize/options.genomeSize;
        double tRatio, cRatio;
        if(treatSize > controlSize)
        {
            tRatio = 1;
            cRatio = ((double)treatSize)/controlSize;
        } else
        {
            tRatio = ((double)controlSize)/treatSize;
            cRatio = 1;
        }
        if(toSmall)
        {
            double tmp = tRatio;
            tRatio = 1/cRatio;
            cRatio = 1/tmp;
        }
        List<Peak> finalPeaks = new ArrayList<>();
        IntArray ctags = control.getRangesByStrand(StrandType.STRAND_PLUS);
        IntArray ttags = treat.getRangesByStrand(StrandType.STRAND_PLUS);
        int indexCtag = 0;
        int indexTtag = 0;
        boolean flagFindCtagLocally = false;
        boolean flagFindTtagLocally = false;
        int prevIndexCtag = 0;
        int prevIndexTtag = 0;
        int lenCtags = ctags.size();
        int lenTtags = ttags.size();
        for(int i=0; i<source.size(); i++)
        {
            Peak peak = source.get(i);
            int windowSizeForLambda = Math.max(peak.length, options.scanWindow);
            double lambdaBackground = lambdaBg0/options.scanWindow*windowSizeForLambda;
            double localLambda;
            double tlambdaPeak = lambdaBackground;
            if(options.noLambda)
            {
                localLambda = lambdaBackground;
                tlambdaPeak = ((double)peak.numTags)/peak.length*windowSizeForLambda;
            } else
            {
                int leftPeak = peak.start+options.d/2;
                int rightPeak = peak.end-options.d/2;
                int leftLRegion = peak.summit-options.lLocal/2;
                int leftSRegion = peak.summit-options.sLocal/2;
                int rightLRegion = peak.summit+options.lLocal/2;
                int rightSRegion = peak.summit+options.sLocal/2;
                int cnumSRegion = 0, cnumLRegion = 0, cnumPeak = 0;
                int tnumSRegion = 0, tnumLRegion = 0, tnumPeak = 0;
                while(indexCtag < lenCtags)
                {
                    if(ctags.get(indexCtag) < leftLRegion)
                        indexCtag++;
                    else if(indexCtag+1>=lenCtags || rightLRegion < ctags.get(indexCtag))
                    {
                        flagFindCtagLocally = false;
                        indexCtag = prevIndexCtag;
                        break;
                    }
                    else
                    {
                        if(!flagFindCtagLocally)
                        {
                            flagFindCtagLocally = true;
                            prevIndexCtag = indexCtag;
                        }
                        int p = ctags.get(indexCtag);
                        if(p >= leftPeak && p <= rightPeak)
                            cnumPeak++;
                        if(leftSRegion <= p && p <= rightSRegion)
                        {
                            cnumSRegion++;
                            cnumLRegion++;
                        }
                        else
                            cnumLRegion++;
                        indexCtag++;
                    }
                }
                while(indexTtag < lenTtags)
                {
                    if(ttags.get(indexTtag) < leftLRegion)
                        indexTtag++;
                    else if(indexTtag+1>=lenTtags || rightLRegion < ttags.get(indexTtag))
                    {
                        flagFindTtagLocally = false;
                        indexTtag = prevIndexTtag;
                        break;
                    }
                    else
                    {
                        if(!flagFindTtagLocally)
                        {
                            flagFindTtagLocally = true;
                            prevIndexTtag = indexTtag;
                        }
                        int p = ttags.get(indexTtag);
                        if(p >= leftPeak && p <= rightPeak)
                            tnumPeak++;
                        if(leftSRegion <= p && p <= rightSRegion)
                        {
                            tnumSRegion++;
                            tnumLRegion++;
                        }
                        else
                            tnumLRegion++;
                        indexTtag++;
                    }
                }
                double clambdaPeak = ((double)cnumPeak)/peak.length*cRatio*windowSizeForLambda;
                double clambdaLRegion = ((double)cnumLRegion)/options.lLocal*cRatio*windowSizeForLambda;
                double clambdaSRegion = ((double)cnumSRegion)/options.sLocal*cRatio*windowSizeForLambda;
                tlambdaPeak = ((double)tnumPeak)/peak.length*tRatio*windowSizeForLambda;
                double tlambdaLRegion = ((double)tnumLRegion)/options.lLocal*tRatio*windowSizeForLambda;
                //double tlambdaSRegion = ((double)tnumSRegion)/options.sLocal*tRatio*windowSizeForLambda;
                if(pass1k)
                {
                    localLambda = Math.max(lambdaBackground, tlambdaLRegion);
                } else
                {
                    localLambda = Math.max(Math.max(Math.max(lambdaBackground, clambdaPeak), clambdaLRegion), clambdaSRegion);
                }
            }
            double pTmp = Stat.poissonDistribution((int)tlambdaPeak, localLambda, false);
            double peakPvalue = pTmp <= 0?3100:-10 * Math.log10(pTmp);
            if(peakPvalue > options.logPvalue)
            {
                peak.pvalue = peakPvalue;
                peak.foldEnrichment = (peak.height)/localLambda*windowSizeForLambda/options.d;
                finalPeaks.add(peak);
            }
        }
        if(log.isLoggable( Level.FINE ))
            log.info("Total number of peaks: "+finalPeaks.size());
        return finalPeaks;
    }

    /**
     * Call peak candidates from trackI data. Using every tag as
     * step and scan the scanWindow region around the tag. If
     * tag number is greater than minTags, then the position is
     * recorded.
     */
    private List<Peak> callPeaksFromTrackI(MACSFWTrack trackI, int trackSize)
    {
        double lambdaBg0 = ((double)options.scanWindow)*trackSize/options.genomeSize;
        minTags = Stat.poissonDistributionInv(1-options.pvalue, lambdaBg0)+1;
        List<Peak> peakCandidates = new ArrayList<>();
        IntArray tags = trackI.getRangesByStrand(StrandType.STRAND_PLUS);
        List<Integer> candidateTags = new ArrayList<>();
        int tagsCount = tags.size();
        if( tagsCount < minTags )
            return new ArrayList<>();
        for(int i=0; i<minTags-1; i++)
            candidateTags.add(tags.get(i));
        int p = minTags - 1;
        while(p<tagsCount)
        {
            if(candidateTags.size() >= minTags)
            {
                if(tags.get(p) - candidateTags.get(candidateTags.size()-minTags+1) <= options.scanWindow)
                {
                    // add next tag, if the new tag is less than scanWindow away from previous no. minTags tag
                    candidateTags.add(tags.get(p));
                    p++;
                } else
                {
                    // candidate peak region is ready, call peak...
                    peakCandidates.add(createPeakCandidate(candidateTags));
                    candidateTags.clear();
                    candidateTags.add(tags.get(p));
                    p++;
                }
            } else
            {
                // add next tag, but if the first one in candidateTags
                // is more than scanWindow away from this new
                // tag, remove the first one from the list
                if(tags.get(p) - candidateTags.get(0) >= options.scanWindow)
                {
                    candidateTags.remove(0);
                }
                candidateTags.add(tags.get(p));
                p++;
            }
        }
        if(log.isLoggable( Level.FINE ))
            log.log(Level.FINE, "Total number of candidates: "+peakCandidates.size());
        return removeOverlappingPeaks(peakCandidates);
    }

    private List<Peak> removeOverlappingPeaks(List<Peak> peaks)
    {
        List<Peak> newPeaks = new ArrayList<>();
        Peak prevPeak = null;
        for(Peak peak : peaks)
        {
            if(prevPeak == null)
            {
                prevPeak = peak;
                continue;
            }
            if(peak.start <= prevPeak.end)
            {
                int newPeakStart = prevPeak.start;
                int newPeakEnd = peak.end;
                int newPeakLength = newPeakEnd - newPeakStart;
                int newPeakSummit, newPeakHeight;
                if(peak.height > prevPeak.height)
                {
                    newPeakSummit = peak.summit;
                    newPeakHeight = peak.height;
                } else
                {
                    newPeakSummit = prevPeak.summit;
                    newPeakHeight = prevPeak.height;
                }
                prevPeak = new Peak(newPeakStart, newPeakEnd, newPeakLength, newPeakSummit, newPeakHeight, peak.numTags+prevPeak.numTags, prevPeak.chrom);
            } else
            {
                newPeaks.add(prevPeak);
                prevPeak = peak;
            }
        }
        if(prevPeak != null)
            newPeaks.add(prevPeak);
        return newPeaks;
    }

    private Peak createPeakCandidate(List<Integer> tags)
    {
        int start = tags.get(0) - options.d/2;
        int end = tags.get(tags.size()-1) + options.d/2;
        int regionLength = end - start;
        int[] line = new int[regionLength];
        for(Integer tag: tags)
        {
            int tagProjectedStart = tag - start - options.d / 2;
            int tagProjectedEnd = tag - start + options.d / 2;
            for(int i=tagProjectedStart; i<tagProjectedEnd; i++)
                line[i]++;
        }
        List<Integer> tops = new ArrayList<>();
        int topHeight = 0;
        for(int i = 0; i < line.length; i++)
        {
            if(line[i] > topHeight)
            {
                topHeight = line[i];
                tops.clear();
                tops.add(i);
            } else if(line[i] == topHeight)
                tops.add(i);
        }
        int summit = tops.get(tops.size()/2)+start;
        return new Peak(start, end, regionLength, summit, topHeight, tags.size(), chrName);
    }

    /**
     * Subclass representing a peak
     */
    public static class Peak
    {
        int start, end, length;
        int summit, height;
        int numTags;
        double pvalue;
        double foldEnrichment;
        double fdr;
        String chrom;

        public Peak(int start, int end, int length, int summit, int height, int numTags, String chrom)
        {
            this.start = start;
            this.end = end;
            this.length = length;
            this.summit = summit;
            this.height = height;
            this.numTags = numTags;
            this.chrom = chrom;
        }

        public int getStart()
        {
            return start;
        }

        public int getEnd()
        {
            return end;
        }

        public int getLength()
        {
            return length;
        }

        public int getSummit()
        {
            return summit;
        }

        public int getHeight()
        {
            return height;
        }

        public int getNumTags()
        {
            return numTags;
        }

        public double getPvalue()
        {
            return pvalue;
        }

        public double getFoldEnrichment()
        {
            return foldEnrichment;
        }

        public double getFdr()
        {
            return fdr;
        }
    }
}