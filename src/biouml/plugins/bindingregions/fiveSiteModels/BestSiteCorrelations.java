package biouml.plugins.bindingregions.fiveSiteModels;

import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.SampleComparison;
import biouml.plugins.bindingregions.utils.SiteModelTypesSelector;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.TrackInfo;

/**
 * @author lan
 * Best sites: correlation between best sites and characteristics of ChIP-Seq peacks; summit(yes/no)
 */
public class BestSiteCorrelations extends AnalysisMethodSupport<BestSiteCorrelations.BestSiteCorrelationsParameters>
{
    public BestSiteCorrelations(DataCollection<?> origin, String name)
    {
        super(origin, name, new BestSiteCorrelationsParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Best sites: correlation between best sites and characteristics of ChIP-Seq peacks; summit(yes/no)");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToChipSeqTrack = parameters.getTrackPath();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        boolean isAroundSummit = parameters.isAroundSummit();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        String[] siteModelTypes = parameters.getSiteModelTypes();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        boolean areBothStrands = true;

        // 1.
        log.info("Read Chip-Seq peaks from track");
        Track track = pathToChipSeqTrack.getDataElement(Track.class);
        Map<String, Interval> nameAndLengthOfChromosomes = EnsemblUtils.getChromosomeIntervals(pathToSequences);
        Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = ChipSeqPeak.readChromosomeAndPeaks(track);
        isAroundSummit = isAroundSummit && ChipSeqPeak.isSummitExist(chromosomeAndPeaks);
        if( isAroundSummit )
            chromosomeAndPeaks = ChipSeqPeak.getPeaksWithSummitsInCenter(chromosomeAndPeaks, minimalLengthOfSequenceRegion, nameAndLengthOfChromosomes);
        jobControl.setPreparedness(5);
        if( jobControl.isStopped() ) return null;
        
        // 2.
        log.info("Calculate site predictions and correlations");
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        SiteModelsComparison smc = new SiteModelsComparison(siteModelTypes, matrix);
        Map<String, double[]> namesAndPredictionsWithCharacteristics = ChipSeqPeak.getNamesAndPredictionsWithChipSeqCharacteristics(chromosomeAndPeaks, pathToSequences, smc, minimalLengthOfSequenceRegion, areBothStrands, jobControl, 5, 80);
        SampleComparison sc = new SampleComparison(namesAndPredictionsWithCharacteristics, "is not used", null);
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        sc.writeTableWithPearsonCorrelationMatrix(pathToOutputs, "correlationMatrix");
        jobControl.setPreparedness(85);
        if( jobControl.isStopped() ) return null;
        sc.writeTableWithAllClouds(pathToOutputs, "clouds_charts");
        jobControl.setPreparedness(100);
        return pathToOutputs.getDataCollection();
    }

    public static class BestSiteCorrelationsParameters extends AbstractFiveSiteModelsParameters
    {
/***
        @PropertyName(MessageBundle.PN_OUTPUT_TABLE_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_TABLE_PATH)
        public DataElementPath getOutputPath()
        {
            return super.getOutputPath();
        }
***/

        ////////////////////////////////////////// may be to delete?
        // No. It is the example that can be used in future
        // it seems likely that this method was used for
        // addHidden(new PropertyDescriptor("outputName", beanClass, "getOutputName", null)); and
        // add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, TableDataCollection.class), "$trackPath/parent$/$outputName$"));
        public String getOutputName()
        {
            try
            {
                String tableName = "bestSites_for_" + getMatrixPath().getName() + "_in_"
                        + new TrackInfo(getTrackPath().getDataElement(Track.class)).getTfClass() + "_" + getTrackPath().getName();
                if( isAroundSummit() )
                    tableName += "_summit";
                return tableName + "_corr";
            }
            catch( Exception e )
            {
                return null;
            }
        }
    }
    
    public static class BestSiteCorrelationsParametersBeanInfo extends BeanInfoEx2<BestSiteCorrelationsParameters>
    {
        public BestSiteCorrelationsParametersBeanInfo()
        {
            super(BestSiteCorrelationsParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("trackPath").inputElement( Track.class ).add();
            add("dbSelector");
            add("aroundSummit");
            add("minRegionLength");
            property("matrixPath").inputElement( FrequencyMatrix.class ).add();
            add("siteModelTypes", SiteModelTypesSelector.class);
//          addHidden(new PropertyDescriptor("outputName", beanClass, "getOutputName", null));
//          property("outputPath").outputElement( TableDataCollection.class ).add();
            property("outputPath").outputElement( FolderCollection.class ).add();
        }
    }
}
