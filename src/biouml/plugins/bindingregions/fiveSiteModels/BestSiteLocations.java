package biouml.plugins.bindingregions.fiveSiteModels;

import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.SiteModelTypesSelector;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.TrackInfo;

/**
 * @author lan
 * Best sites locations: locations of best sites in ChIP-Seq peaks; summit(yes/no)
 */
public class BestSiteLocations extends AnalysisMethodSupport<BestSiteLocations.BestSiteLocationsParameters>
{
    public BestSiteLocations(DataCollection<?> origin, String name)
    {
        super(origin, name, new BestSiteLocationsParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Best sites locations: locations of best sites in ChIP-Seq peaks; summit(yes/no)");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        boolean isAroundSummit = parameters.isAroundSummit();
        DataElementPath pathToChipSeqTrack = parameters.getTrackPath();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        String[] siteModelTypes = parameters.getSiteModelTypes();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        boolean areBothStrands = true;
        
        // 1.
        log.info("Read ChIP-Seq peaks from track and their sequences");
        Track track = pathToChipSeqTrack.getDataElement(Track.class);
        jobControl.setPreparedness(5);
        if( jobControl.isStopped() ) return null;
        Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = ChipSeqPeak.readChromosomeAndPeaks(track);
        jobControl.setPreparedness(10);
        if( jobControl.isStopped() ) return null;
        String[] positiveCharacteristicNames = ChipSeqPeak.getPositiveCharacteristicNames(chromosomeAndPeaks);
        jobControl.setPreparedness(15);
        if( jobControl.isStopped() ) return null;
        isAroundSummit = isAroundSummit && ChipSeqPeak.isSummitExist(positiveCharacteristicNames);
        if( isAroundSummit )
            chromosomeAndPeaks = ChipSeqPeak.getPeaksWithSummitsInCenter(chromosomeAndPeaks, minimalLengthOfSequenceRegion, EnsemblUtils.getChromosomeIntervals(pathToSequences));
        jobControl.setPreparedness(10);
        if( jobControl.isStopped() ) return null;
        Sequence[] sequences = ChipSeqPeak.getLinearSequencesForChipSeqPeaks(chromosomeAndPeaks, pathToSequences, minimalLengthOfSequenceRegion, jobControl, 10, 50);
        int[] summits = ! isAroundSummit ? null : ChipSeqPeak.getSummitsCorrectedOnRegionLengthExtention(chromosomeAndPeaks, minimalLengthOfSequenceRegion);
        
        // 2.
        log.info("Create charts and table and write them");
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        SiteModelsComparison smc = new SiteModelsComparison(siteModelTypes, matrix);
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        String tableName = "bestSites_for_" + matrix.getName() + "_in_" + ( new TrackInfo(track) ).getTfClass() + "_" + track.getName();
        if( isAroundSummit )
            tableName += "_summit";
        smc.writeChartsWithBestSitesLocationDistributions(sequences, areBothStrands, summits, pathToOutputs.getChildPath(tableName + "_charts"));
        jobControl.setPreparedness(75);
        if( jobControl.isStopped() ) return null;
        log.info("Create table");
        smc.writeBestSitesIntoTable(chromosomeAndPeaks, sequences, areBothStrands, jobControl, 75, 100, pathToOutputs.getChildPath(tableName + "_locations"));
        return pathToOutputs.getDataCollection();
    }

    public static class BestSiteLocationsParameters extends AbstractFiveSiteModelsParameters
    {}
    
    public static class BestSiteLocationsParametersBeanInfo extends BeanInfoEx2<BestSiteLocationsParameters>
    {
        public BestSiteLocationsParametersBeanInfo()
        {
            super(BestSiteLocationsParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            add("dbSelector");
            add("aroundSummit");
            add("minRegionLength");
            add("siteModelTypes", SiteModelTypesSelector.class);
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
