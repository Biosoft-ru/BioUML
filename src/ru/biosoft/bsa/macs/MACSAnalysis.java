package ru.biosoft.bsa.macs;

import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

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
import ru.biosoft.bsa.macs.PeakDetect.Peak;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.JobControl;

@ClassIcon("resources/macs.gif")
public class MACSAnalysis extends AnalysisMethodSupport<MACSAnalysisParameters>
{
    private static final int MAX_PAIRNUM = 1000;
    /*
    private static final int MAX_LAMBDA = 100000;
    private static final int FESTEP = 20;
    */

    private static final PropertyDescriptor pdSummit = StaticDescriptor.create("summit");
    private static final PropertyDescriptor pdFDR = StaticDescriptor.create("fdr");
    private static final PropertyDescriptor pdFoldEnrichment = StaticDescriptor.create("fold_enrichment");
    private static final PropertyDescriptor pdTags = StaticDescriptor.create("tags");

    private Map<String, ChromosomeMACSTrack> chroms;
    private MACSOptions options;

    public MACSAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new MACSAnalysisParameters());
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        Track output = doMACS();
        if(jobControl.isStopped()) return null;
        return output;
    }

    @Override
    public String generateJavaScript(Object params)
    {
        if( params == null || ! ( params instanceof MACSAnalysisParameters ) )
            return null;
        MACSAnalysisParameters parameters = (MACSAnalysisParameters)params;
        String[] paramList = new String[13];
        DataElementPath track = parameters.getTrackPath();
        paramList[0] = track == null ? "null" : "data.get('" + StringEscapeUtils.escapeJavaScript(track.toString()) + "')";
        DataElementPath controlTrack = parameters.getControlPath();
        paramList[1] = controlTrack == null ? "null" : "data.get('" + StringEscapeUtils.escapeJavaScript(controlTrack.toString()) + "')";

        DataElementPath path = parameters.getOutputPath();
        paramList[2] = path == null ? "null" : "'" + StringEscapeUtils.escapeJavaScript(path.toString()) + "'";

        paramList[3] = parameters.getNolambda().toString();
        int[] lambdaSet = parameters.getLambdaSetArray();
        paramList[4] = lambdaSet[0] + ", " + lambdaSet[1] + ", " + lambdaSet[2];

        paramList[5] = parameters.getNomodel().toString();
        paramList[6] = parameters.getShiftsize().toString();
        paramList[7] = parameters.getBw().toString();
        paramList[8] = parameters.getGsize().toString();
        paramList[9] = parameters.getMfold().toString();
        paramList[10] = parameters.getTsize().toString();
        paramList[11] = parameters.getPvalue().toString();
        paramList[12] = parameters.isFutureFDR().toString();
        return "bsa.MACS(" + StringUtils.join(paramList, ", ") + ");";
    }

    private void initChromosomes(Track[] tracks)
    {
        log.info("Get chromosomes list...");
        jobControl.setPreparedness(1);
        chroms = new HashMap<>();
        for( Track track : tracks )
        {
            if( track == null )
                continue;
            DataCollection<Site> dc = track.getAllSites();
            for(Site s : dc)
            {
                String name = s.getOriginalSequence().getName();
                int len = s.getStart() + s.getLength();
                if( !chroms.containsKey(name) )
                {
                    chroms.put(name, new ChromosomeMACSTrack(len));
                }
                else
                {
                    int oldlen = chroms.get(name).length;
                    if( oldlen < len )
                        chroms.get(name).length = len;
                }
            }
        }
        jobControl.setPreparedness(10);
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
        log.info("Read track...");
        int i = 0;
        for( Map.Entry<String, ChromosomeMACSTrack> entry : chroms.entrySet() )
        {
            String chr = entry.getKey();
            ChromosomeMACSTrack chromosomeMACSTrack = entry.getValue();
            chromosomeMACSTrack.treat = new MACSFWTrack(tracks[0], chr, chromosomeMACSTrack.length);
            if( tracks.length > 1 && tracks[1] != null )
                chromosomeMACSTrack.control = new MACSFWTrack(tracks[1], chr, chromosomeMACSTrack.length);
            jobControl.setPreparedness(10 + 40 * ( ++i ) / chroms.size());
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
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

        initChromosomes(new Track[] {track, controlTrack});
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        calcStats();
        jobControl.setPreparedness(55);
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        int i = 0;
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
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
            // Release unnecessary sets to free memory
            chromosomeMACSTrack.treat = null;
            chromosomeMACSTrack.control = null;
        }
        if( controlTrack != null )
        {
            log.info("Calculating FDR...");
            peakDetect.addFDR();
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
        }
        log.info("Storing output...");
        WritableTrack output = new SqlTrack(path.optParentCollection(), properties);
        storeOutput(output, peakDetect.getPeaks());
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            path.getParentCollection().remove( path.getName() );
            return null;
        }
        return output;
    }

    private void calcStats() throws Exception
    {
        options.treatUnique = 0;
        options.treatTotal = 0;
        options.controlUnique = 0;
        options.controlTotal = 0;
        Map<Integer, Integer> counts = new HashMap<>();
        log.info("Calculating stats...");
        for( ChromosomeMACSTrack chr : chroms.values() )
        {
            MACSFWTrack control = chr.control;
            if( control != null )
            {
                options.controlTotal += control.getTotal();
                options.controlUnique += control.getTotalUnique();
            }

            MACSFWTrack treat = chr.treat;
            options.treatTotal += treat.getTotal();
            options.treatUnique += treat.getTotalUnique();

            for( Integer c : treat.getCommentsByStrand(StrandType.STRAND_PLUS) )
            {
                counts.put(c, counts.containsKey(c) ? counts.get(c) + 1 : 1);
            }
            for( Integer c : treat.getCommentsByStrand(StrandType.STRAND_MINUS) )
            {
                counts.put(c, counts.containsKey(c) ? counts.get(c) + 1 : 1);
            }
        }
        log.info("Unique tags in treatment: " + options.treatUnique);
        log.info("Total tags in treatment: " + options.treatTotal);
        if( !options.noModel )
        {
            log.log(Level.FINE, "Calculate max duplicate tags in single position based on binomal distribution...");
            options.maxDupTags = calMaxDupTags(options.genomeSize, options.treatTotal, options.pvalue);
            log.info("Max_dup_tags based on binomal = " + ( options.maxDupTags ));

            // Calculate background model
            int totalDuplicates = 0; // total duplicated number of tags over maximum dup tag number
            for( Map.Entry<Integer, Integer> entry : counts.entrySet() )
            {
                int c = entry.getKey();
                if( c > options.maxDupTags )
                    totalDuplicates += ( c - options.maxDupTags ) * entry.getValue();
            }
            options.bgRedundant = (double)totalDuplicates / options.treatTotal;
            log.info("Background Redundant rate: " + options.bgRedundant);
            log.log(Level.FINE, "Build model...");
            PeakModel peakmodel = new PeakModel(chroms, MAX_PAIRNUM, options, log);
            log.log(Level.FINE, "Model building finished!");
            log.log(Level.FINE, "Model minTags = " + peakmodel.getMinTags());
            log.log(Level.FINE, "Model d = " + peakmodel.getD());
            log.log(Level.FINE, "Model scanWindow = " + peakmodel.getScanWindow());

            //TODO:
            //model2r_script(peakmodel,options.modelR,options.name)

            options.d = peakmodel.getD();
            options.scanWindow = peakmodel.getScanWindow();
        }
        if( options.noModel || options.d == 0 )
        {
            log.info("#2 Skipped...");
            options.d = options.shiftSize * 2;
            options.scanWindow = Math.max(2 * options.d, 2 * options.bandWidth); //# scan window is no less than 2* bandwidth
        }
    }

    public void storeOutput(WritableTrack track, List<Peak> peaks) throws Exception
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
            s.getProperties().add(new DynamicProperty(pdSummit, Integer.class, p.summit - p.start + 1));
            s.getProperties().add(new DynamicProperty(Site.SCORE_PD, Double.class, p.pvalue));
            s.getProperties().add(new DynamicProperty(pdTags, Integer.class, p.numTags));
            s.getProperties().add(new DynamicProperty(pdFoldEnrichment, Double.class, p.foldEnrichment));
            if( isControl )
            {
                s.getProperties().add(new DynamicProperty(pdFDR, Double.class, p.fdr > 100 ? 100 : p.fdr));
            }
            track.addSite(s);
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return;
        }
        track.finalizeAddition();
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
        int length;
        MACSFWTrack treat = null, control = null;

        public ChromosomeMACSTrack(int length)
        {
            this.length = length;
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
        int maxDupTags;
        double genomeSize;
        int mFold;
        int tagSize;
        double pvalue;
        double logPvalue;
        int[] lambdaRegion;
        boolean futureFDR;
        int treatTotal, treatUnique, controlTotal, controlUnique;

        MACSOptions(MACSAnalysisParameters parameters)
        {
            noLambda = parameters.getNolambda();
            bandWidth = parameters.getBw();
            shiftSize = parameters.getShiftsize();
            noModel = parameters.getNomodel();
            genomeSize = parameters.getGsize();
            mFold = parameters.getMfold();
            tagSize = parameters.getTsize();
            pvalue = parameters.getPvalue();
            logPvalue = Math.log10(pvalue) * -10;
            lambdaRegion = parameters.getLambdaSetArray();
            futureFDR = parameters.isFutureFDR();
        }
    }
}
