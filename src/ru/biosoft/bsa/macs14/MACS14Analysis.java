package ru.biosoft.bsa.macs14;

import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.analysis.JavaScriptBSA;
import ru.biosoft.bsa.macs14.PeakDetect.Peak;
import ru.biosoft.util.IntArray;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.JobControl;

@ClassIcon("resources/macs.gif")
public class MACS14Analysis extends AnalysisMethodSupport<MACS14AnalysisParameters>
{
    private static final int MAX_PAIRNUM = 1000;
    
    private Map<String, ChromosomeMACSTrack> chroms;
    private MACSOptions options;
    private final MACS14Profiler profiler = new MACS14Profiler();
    
    private static final PropertyDescriptor pdProfile = StaticDescriptor.create("profile");
    private static final PropertyDescriptor pdSummit = StaticDescriptor.create("summit");
    private static final PropertyDescriptor pdFDR = StaticDescriptor.create("fdr");
    private static final PropertyDescriptor pdFoldEnrichment = StaticDescriptor.create("fold_enrichment");
    private static final PropertyDescriptor pdTags = StaticDescriptor.create("tags");

    public MACS14Analysis(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new MACS14AnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        // TODO: validate other parameters, check ranges
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        profiler.start();
        Track output = doMACS();
        profiler.end();
        log.log(Level.FINE, profiler.toString());
        if(jobControl.isStopped()) return null;
        return output;
    }

    private void initChromosomes(Track[] tracks)
    {
        log.info("Reading track...");
        jobControl.setPreparedness(1);
        chroms = new HashMap<>();
        int s = 0;
        int n = 0;
        int nTracks = tracks.length == 1 || tracks[1] == null ? 1 : 2;
        int fixedWidth = 0;
        for( int trackNum = 0; trackNum < tracks.length; trackNum++ )
        {
            Track track = tracks[trackNum];
            if( track == null )
                continue;
            DataCollection<Site> dc = track.getAllSites();
            int size = dc.getSize(), i=0;
            for(Site site: dc)
            {
                String name = site.getOriginalSequence().getName();
                if( !chroms.containsKey(name) )
                {
                    ChromosomeMACSTrack chromosomeMACSTrack = new ChromosomeMACSTrack();
                    chroms.put(name, chromosomeMACSTrack);
                    chromosomeMACSTrack.treat = new MACSFWTrack();
                    if(nTracks == 2)
                        chromosomeMACSTrack.control = new MACSFWTrack();
                }
                MACSFWTrack fwTrack = trackNum == 0?chroms.get(name).treat:chroms.get(name).control;
                fwTrack.addSite(site);
                i++;
                if(n < 10)
                {
                    n++;
                    s+=site.getLength();
                    fixedWidth  = s/n;
                }
                if(i % 1000 == 0)
                {
                    jobControl.setPreparedness(1 + (int) ( 49 * ( trackNum + ( (double)i ) / size ) / nTracks ));
                    if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
                }
            }
            jobControl.setPreparedness(1 + 49 * ( trackNum + 1 ) / tracks.length);
        }
        for(ChromosomeMACSTrack chromosomeMACSTrack: chroms.values())
        {
            chromosomeMACSTrack.treat.finalizeAddition();
            if(chromosomeMACSTrack.control != null)
                chromosomeMACSTrack.control.finalizeAddition();
        }
        jobControl.setPreparedness(50);
        if(options.tagSize == 0)
        {
            options.tagSize = fixedWidth;
            log.info("Tag size is determined as "+options.tagSize+" bps");
        }
    }

    protected Track doMACS() throws Exception
    {
        validateParameters();
        options = new MACSOptions(getParameters());
        Track track = parameters.getTrack();
        Track controlTrack = parameters.getControlTrack();
        DataElementPath path = parameters.getOutputPath();
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, path.getName());
        if(track instanceof DataCollection)
        {
            Object labelProperty = ((DataCollection<?>)track).getInfo().getProperty(SqlTrack.LABEL_PROPERTY);
            if(labelProperty != null)
                properties.put(SqlTrack.LABEL_PROPERTY, labelProperty);
            Object seqCollectionProperty = ((DataCollection<?>)track).getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
            if(seqCollectionProperty != null)
                properties.put(Track.SEQUENCES_COLLECTION_PROPERTY, seqCollectionProperty);
        }

        profiler.startReadingTrack(track.getAllSites().getSize() + (controlTrack == null ? 0 : controlTrack.getAllSites().getSize()));
        initChromosomes(new Track[] {track, controlTrack});
        profiler.endReadingTrack();
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            chroms = null;
            return null;
        }
        
        profiler.startCalcStats();
        calcStats();
        profiler.endCalcStats();
        jobControl.setPreparedness(55);
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            chroms = null;
            return null;
        }
        int i = 0;
        
        profiler.startPeakCall();
        PeakDetect peakDetect = new PeakDetect(options, log);
        log.info("#3 Call peaks...");
        for( Map.Entry<String, ChromosomeMACSTrack> entry : chroms.entrySet() )
        {
            ChromosomeMACSTrack chromosomeMACSTrack = entry.getValue();
            MACSFWTrack control = chromosomeMACSTrack.control;
            MACSFWTrack treat = chromosomeMACSTrack.treat;
            //#3 Call Peaks
            peakDetect.callPeaks(treat, control, entry.getKey());
            jobControl.setPreparedness(55 + 35 * ( ++i ) / chroms.size());
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
            {
                chroms = null;
                return null;
            }
        }
        profiler.endPeakCall();
        
        profiler.startCalculatingFDR();
        if( controlTrack != null )
        {
            log.info("Calculating FDR...");
            peakDetect.addFDR();
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
            {
                chroms = null;
                return null;
            }
        }
        profiler.endCalculatingFDR();

        profiler.startStoringOutput(peakDetect.getPeaks().size());
        log.info("Storing output...");
        WritableTrack output = new SqlTrack(path.optParentCollection(), properties);
        storeOutput(output, peakDetect.getPeaks(), chroms);
        chroms = null;
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            path.getParentCollection().remove( path.getName() );
            return null;
        }
        profiler.endStoringOutput();
        
        return output;
    }

    private void calcStats() throws Exception
    {
        options.treatFiltered = 0;
        options.treatTotal = 0;
        options.controlFiltered = 0;
        options.controlTotal = 0;
        log.info("#1 Calculating stats and filtering...");
        for( ChromosomeMACSTrack track : chroms.values() )
        {
            MACSFWTrack control = track.control;
            if( control != null )
            {
                options.controlTotal += control.getTotal();
            }

            MACSFWTrack treat = track.treat;
            options.treatTotal += treat.getTotal();
        }
        log.info("Total tags in treatment: " + options.treatTotal);
        if(options.controlTotal > 0)
            log.info("Total tags in control: " + options.controlTotal);
        if(options.keepDup.equals("all"))
            options.maxDupTags = -1;
        else if(options.keepDup.equals("auto"))
        {
            log.log(Level.FINE, "Calculate max duplicate tags in single position based on binomal distribution...");
            options.maxDupTags = calMaxDupTags(options.genomeSize, options.treatTotal, 1e-5);
            log.info("Max_dup_tags for treatment based on binomal = " + ( options.maxDupTags ));
            if(options.controlTotal > 0)
            {
                options.maxDupTagsControl = calMaxDupTags(options.genomeSize, options.controlTotal, 1e-5);
                log.info("Max_dup_tags for control based on binomal = " + ( options.maxDupTagsControl ));
            }
        } else
            options.maxDupTags = Integer.parseInt(options.keepDup);
        if(options.maxDupTagsControl == -1) options.maxDupTagsControl = options.maxDupTags;
        for( ChromosomeMACSTrack track : chroms.values() )
        {
            MACSFWTrack control = track.control;
            if( control != null )
            {
                control.filterDup(options.maxDupTagsControl);
                options.controlFiltered += control.getTotalFiltered();
            }
            MACSFWTrack treat = track.treat;
            treat.filterDup(options.maxDupTags);
            options.treatFiltered += treat.getTotalFiltered();
        }
        log.info("Filtered tags in treatment: " + options.treatFiltered);
        log.info("Redundant rate of treatment: " + ((double)options.treatTotal-options.treatFiltered)/options.treatTotal);
        if(options.controlTotal > 0)
        {
            log.info("Filtered tags in control: " + options.controlFiltered);
            log.info("Redundant rate of control: " + ((double)options.controlTotal-options.controlFiltered)/options.controlTotal);
        }
        
        if( !options.noModel )
        {
            log.log(Level.FINE, "#2 Build model...");
            try
            {
                PeakModel peakmodel = new PeakModel(chroms, MAX_PAIRNUM, options, log);
                log.log(Level.FINE, "Model building finished!");
                log.log(Level.FINE, "Model minTags = " + peakmodel.getMinTags()+" / maxTags = " + peakmodel.getMaxTags());
                log.info("Predicted fragment length (d) = " + peakmodel.getD());
                log.log(Level.FINE, "Model scanWindow = " + peakmodel.getScanWindow());

                //TODO:
                //model2r_script(peakmodel,options.modelR,options.name)

                options.d = peakmodel.getD();
                options.scanWindow = 2 * options.d;
                if(!options.offAuto && options.d <= 2*options.tagSize)
                {
                    options.d=options.shiftSize*2;
                    options.scanWindow=2*options.d;
                    log.warning("#2 Since the d calculated from paired-peaks are smaller than 2*tag length, "
                            + "it may be influenced by unknown sequencing problem. MACS will use " + options.shiftSize + " as shiftsize, "
                            + options.d + " as fragment length");
                }
            }
            catch( Exception e )
            {
                if(options.offAuto)
                    throw e;
                log.warning("#2 Skipped: "+e.getMessage());
                options.d = options.shiftSize * 2;
                options.scanWindow = 2 * options.d; //# remove the effect of --bw
                log.warning("MACS will use " + options.shiftSize + " as shiftsize, " + options.d + " as fragment length");
            }
        }
        if( options.noModel || options.d == 0 )
        {
            log.info("#2 Skipped...");
            options.d = options.shiftSize * 2;
            options.scanWindow = 2 * options.d; //# remove the effect of --bw
            log.info("MACS will use " + options.shiftSize + " as shiftsize, " + options.d + " as fragment length");
        }
    }

    public void storeOutput(WritableTrack track, List<Peak> peaks, Map<String, ChromosomeMACSTrack> chroms) throws Exception
    {
        boolean isControl = parameters.getControlTrack() != null;
        for( Peak p : peaks )
        {
            if(p.start < 0)
            {
                p.length += p.start;
                p.start = 0;
            }
            Site s = new SiteImpl(null, p.chrom, SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, p.start + 1, p.length,
                    Site.PRECISION_EXACTLY, StrandType.STRAND_NOT_APPLICABLE, null, null);

            DynamicPropertySet properties = s.getProperties();
            if( parameters.isComputePeakProfile() )
            {
                properties.add(new DynamicProperty(pdProfile, double[].class, getProfileForPeak(chroms.get(p.chrom).treat, p.start, p.end)));
            }
            properties.add(new DynamicProperty(pdSummit, Integer.class, p.summit - p.start));
            properties.add(new DynamicProperty(Site.SCORE_PD, Double.class, p.pvalue));
            properties.add(new DynamicProperty(pdTags, Integer.class, p.numTags));
            properties.add(new DynamicProperty(pdFoldEnrichment, Double.class, p.foldEnrichment));
            if( isControl )
            {
                properties.add(new DynamicProperty(pdFDR, Double.class, p.fdr > 100 ? 100 : p.fdr));
            }
            track.addSite(s);
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
        }
        track.finalizeAddition();
    }

    /**
     * @param track
     * @param start
     * @param end
     * @return
     */
    private double[] getProfileForPeak(MACSFWTrack track, int start, int end)
    {
        int d = options.d;
        IntArray tags = track.getRangesByStrand(StrandType.STRAND_PLUS);
        int startP = start - d;
        int endP = end + d;
        double[] positions = new double[end-start];
        int pos = Arrays.binarySearch(tags.data(), startP);
        if(pos < 0) pos = -pos-1;
        for(;pos < tags.size(); pos++)
        {
            int curOffset = tags.get(pos);
            if(curOffset > endP)
                break;
            int s = curOffset-d/2;
            int e = s+d;
            for(int p = s; p<e; p++)
            {
                if(p-start >= 0 && p-start < positions.length)
                    positions[p-start]++;
            }
        }
        return positions;
    }

    /*
     * Calculate the maximum duplicated tag number based on genome size,
     * total tag number and a p-value based on binomial distribution.
     * Brute force algorithm to calculate reverse CDF no more than MAX_LAMBDA(100000).
     */
    private int calMaxDupTags(double genomeSize, int tagsNumber, double p)
    {
        try
        {
            int tags = Stat.cumulativeBinomialInv(1 - p, tagsNumber, 1.0 / genomeSize);
            return tags;
        }
        catch( Exception e )
        {
            return 1;
        }
    }

    public static class ChromosomeMACSTrack
    {
        MACSFWTrack treat = null, control = null;

        public ChromosomeMACSTrack()
        {
        }
    }

    public static class MACSOptions
    {
        boolean noLambda;
        int bandWidth;
        int scanWindow;
        int shiftSize;
        int d;
        boolean noModel;
        double bgRedundant;
        int maxDupTags, maxDupTagsControl = -1;
        double genomeSize;
        int lmFold;
        int umFold;
        int tagSize;
        double pvalue;
        double logPvalue;
        int treatTotal, treatFiltered, controlTotal, controlFiltered;
        boolean toSmall;
        int lLocal;
        int sLocal;
        String keepDup;
        boolean offAuto;

        MACSOptions(MACS14AnalysisParameters parameters)
        {
            noLambda = parameters.getNolambda();
            bandWidth = parameters.getBw();
            shiftSize = parameters.getShiftsize();
            noModel = parameters.getNomodel();
            genomeSize = parameters.getGsize();
            lmFold = parameters.getMfoldLower();
            umFold = parameters.getMfoldUpper();
            tagSize = parameters.getTsize();
            pvalue = parameters.getPvalue();
            logPvalue = Math.log10(pvalue) * -10;
            offAuto = parameters.isAutoOff();
            keepDup = parameters.getKeepDup();
            sLocal = parameters.getSLocal();
            lLocal = parameters.getLLocal();
            toSmall = parameters.isToSmall();
        }
    }
}
