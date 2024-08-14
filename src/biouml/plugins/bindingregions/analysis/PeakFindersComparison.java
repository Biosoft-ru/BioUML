package biouml.plugins.bindingregions.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.SampleComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.TrackInfo;
import biouml.standard.type.Species;
import one.util.streamex.MoreCollectors;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

/**
 * @author yura
 *
 */
public class PeakFindersComparison extends AnalysisMethodSupport<PeakFindersComparison.PeakFindersComparisonParameters>
{
    public PeakFindersComparison(DataCollection<?> origin, String name)
    {
        super(origin, name, new PeakFindersComparisonParameters());
    }
    
    private static class SampleInfo
    {
        String commonName;
        String baseName;

        public SampleInfo(String commonName, String baseName)
        {
            this.commonName = commonName;
            this.baseName = baseName;
        }
    }
    
    /////////
    private static final SampleInfo[] infos = new SampleInfo[] {
        new SampleInfo("Number of TF-binding regions", "trackSizes"),
        new SampleInfo("Mean length of TF-binding regions", "meanLengths"),
    };

    private static class PeakFinderInfo
    {
        DataElementPath peakFinderPath;
        double[][] samples;
        
        public PeakFinderInfo(DataElementPath peakFinderPath, int size)
        {
            this.peakFinderPath = peakFinderPath;
            samples = new double[infos.length][size];
        }

        String correctName()
        {
            String result = peakFinderPath.getName();
            if( result.equals("macs") )
                result = ChipSeqPeak.PEAK_FINDER_MACS;
            else if( result.equals("sissrs") )
                result = ChipSeqPeak.PEAK_FINDER_SISSRS;
            return result;
        }
    }
    
    private double[] getTrackSamples(Track track)
    {
        double[] result = new double[2];
        DataCollection<Site> sites = track.getAllSites();
        result[0] = sites.getSize();
        result[1] = 0.0;
        for( Site site : sites )
            result[1] += site.getLength();
        if( result[0] > 1.99 )
            result[1] /= result[0];
        return result;
    }
    
    // create samples for comparison
    private SampleComparison[] getSamplesForComparison(PeakFinderInfo[] peakFinders, boolean[] controlIndicators)
    {
        SampleComparison[] samplesComparison = new SampleComparison[infos.length];
        for( int sampleNum = 0; sampleNum < infos.length; sampleNum++ ) // over sizes and mean lengths
        {
            Map<String, double[]> nameAndSample = new HashMap<>();
            String commonName = infos[sampleNum].commonName;
            for(PeakFinderInfo peakFinder : peakFinders)
            {
                String peakFinderName = peakFinder.correctName();
                nameAndSample.put(peakFinderName, peakFinder.samples[sampleNum]);
            }
            samplesComparison[sampleNum] = new SampleComparison(nameAndSample, commonName, controlIndicators);
        }
        return samplesComparison;
    }
    
    private List<TrackInfo> getSelectedTrackInfos()
    {
        List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(parameters.getFirstPathToChipSeqTracks(), parameters.getSpecies());
        jobControl.setPreparedness(10);
        log.info("number of preselected tracks = " + trackInfos.size());
        log.info("Select ChIP-Seq tracks");
        Iterator<TrackInfo> it = trackInfos.iterator();
        while( it.hasNext() )
        {
            TrackInfo trackInfo = it.next();
            if( trackInfo.getNumberOfSites() <= 2 )
                it.remove();
            else
            {
                DataElementPath pathToTrack2 = parameters.getSecondPathToChipSeqTracks().getChildPath(trackInfo.getTrackName());
                if( ! pathToTrack2.exists() )
                    it.remove();
                else if ( pathToTrack2.getDataElement(Track.class).getAllSites().getSize() < 2 )
                    it.remove();
            }
        }
        return trackInfos;
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        
        log.info("Comparison of 2 Peak finders (for example, MACS and SISSRs): lengths and numbers of TF-binding regions are compared");
        // 1-st step: track selection
        log.info("Select ChIP-Seq tracks");
        List<TrackInfo> trackInfos = getSelectedTrackInfos();
        jobControl.setPreparedness(10);
        log.info("number of selected tracks = " + trackInfos.size());
        if( jobControl.isStopped() ) return null;
        
        // 2-nd step: sample creation
        log.info("Create samples with sizes and mean lengths");
        PeakFinderInfo[] peakFinders = new PeakFinderInfo[] {
                new PeakFinderInfo(parameters.getFirstPathToChipSeqTracks(), trackInfos.size()),
                new PeakFinderInfo(parameters.getSecondPathToChipSeqTracks(), trackInfos.size())
        };
        boolean[] controlIndicators = trackInfos.stream().collect( MoreCollectors.toBooleanArray( ti -> ti.getControlId() != null ) );
        int trackNum = 0;
        for( TrackInfo trackInfo : trackInfos )
        {
            for( PeakFinderInfo peakFinder : peakFinders )
            {
                Track track = peakFinder.peakFinderPath.getChildPath(trackInfo.getTrackName()).getDataElement(Track.class);
                double[] samplesForTrack = getTrackSamples(track);
                for( int sampleNum = 0; sampleNum < samplesForTrack.length; sampleNum++ )
                    peakFinder.samples[sampleNum][trackNum] = samplesForTrack[sampleNum];
            }
            
            jobControl.setPreparedness(10 + ++trackNum * 70 / trackInfos.size());
            if( jobControl.isStopped() ) return null;
        }
        SampleComparison[] samplesComparison = getSamplesForComparison(peakFinders, controlIndicators);

        // 3-rd step: sample treatment
        log.info("Comparison: Create and write tables");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        for( int sampleNum = 0; sampleNum < infos.length; sampleNum++ ) // over sizes and mean lengths
        {
            String baseName = infos[sampleNum].baseName;
            for( int j = 0; j < 3; j++ ) // over sample and subsamples
            {
                SampleComparison scForSubsamples = null;
                String subname = "";
                switch( j )
                {
                    case 0: scForSubsamples = samplesComparison[sampleNum]; break;
                    case 1: scForSubsamples = samplesComparison[sampleNum].getFirstSubsamples();
                            subname = "_withControl"; break;
                    case 2: scForSubsamples = samplesComparison[sampleNum].getSecondSubsamples();
                            subname = "_withoutControl"; break;
                    default:
                        continue;
                }
                scForSubsamples.writeTableWithMeanAndSigma(pathToOutputs, baseName + "_meanAndSigma" + subname);
                scForSubsamples.writeTableWithWilcoxonPaired(pathToOutputs, baseName + "_wilcoxon" + subname);
                Chart chart = scForSubsamples.chartWithSmoothedDensities(true, null, DensityEstimation.WINDOW_WIDTH_01, null);
                SiteModelsComparisonUtils.writeChartsIntoTable(scForSubsamples.getCommonName(), chart, "chart", pathToOutputs, baseName + "_chart_densities" + subname);
            }
            jobControl.setPreparedness(80 + (sampleNum + 1) * 10);
        }
        return pathToOutputs.getDataCollection();
    }
    
    @SuppressWarnings ( "serial" )
    public static class PeakFindersComparisonParameters extends AbstractAnalysisParameters
    {
        private Species species = Species.getDefaultSpecies(null);
        private DataElementPath firstPathToChipSeqTracks;
        private DataElementPath secondPathToChipSeqTracks;
        private DataElementPath outputPath;
        
        @PropertyName(MessageBundle.PN_SPECIES)
        @PropertyDescription(MessageBundle.PD_SPECIES)
        public Species getSpecies()
        {
            return species;
        }

        public void setSpecies(Species species)
        {
            Object oldValue = this.species;
            this.species = species;
            firePropertyChange("species", oldValue, species);
        }
        
        @PropertyName(MessageBundle.PN_CHIPSEQ_FOLDER1)
        @PropertyDescription(MessageBundle.PD_CHIPSEQ_FOLDER1)
        public DataElementPath getFirstPathToChipSeqTracks()
        {
            return firstPathToChipSeqTracks;
        }
        
        public void setFirstPathToChipSeqTracks(DataElementPath firstPathToChipSeqTracks)
        {
            Object oldValue = this.firstPathToChipSeqTracks;
            this.firstPathToChipSeqTracks = firstPathToChipSeqTracks;
            firePropertyChange("firstPathToChipSeqTracks", oldValue, firstPathToChipSeqTracks);
        }
        
        @PropertyName(MessageBundle.PN_CHIPSEQ_FOLDER2)
        @PropertyDescription(MessageBundle.PD_CHIPSEQ_FOLDER2)
        public DataElementPath getSecondPathToChipSeqTracks()
        {
            return secondPathToChipSeqTracks;
        }
        
        public void setSecondPathToChipSeqTracks(DataElementPath secondPathToChipSeqTracks)
        {
            Object oldValue = this.secondPathToChipSeqTracks;
            this.secondPathToChipSeqTracks = secondPathToChipSeqTracks;
            firePropertyChange("secondPathToChipSeqTracks", oldValue, secondPathToChipSeqTracks);
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }
    }
    
    public static class PeakFindersComparisonParametersBeanInfo extends BeanInfoEx2<PeakFindersComparisonParameters>
    {
        public PeakFindersComparisonParametersBeanInfo()
        {
            super(PeakFindersComparisonParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(DataElementPathEditor.registerInputChild("firstPathToChipSeqTracks", beanClass, Track.class));
            add(DataElementPathEditor.registerInputChild("secondPathToChipSeqTracks", beanClass, Track.class));
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
