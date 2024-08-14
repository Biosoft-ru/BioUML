package biouml.plugins.bindingregions.fiveSiteModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.Clusterization;
import biouml.plugins.bindingregions.utils.Clusterization.KMeansAlgorithm;
import biouml.plugins.bindingregions.utils.MatrixUtils.Distance;
import biouml.plugins.bindingregions.utils.MultivariateSample.Transformation;
import biouml.plugins.bindingregions.utils.SiteModelTypesSelector;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;

/**
 * @author yura
 *
 */
public class ROCCurvesInClusterizedPeaks extends AnalysisMethodSupport<ROCCurvesInClusterizedPeaks.ROCCurvesInClusterizedPeaksParameters>
{
    public ROCCurvesInClusterizedPeaks(DataCollection<?> origin, String name)
    {
        super(origin, name, new ROCCurvesInClusterizedPeaksParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("ROC-curves in clusterized peaks; summit(yes/no)");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToChipSeqTrack = parameters.getTrackPath();
        String transformationType = parameters.getDataTransformationType();
        int numberOfClusters = Math.max(2, parameters.getNumberOfClusters());
        boolean isNumberOfOverlaps = parameters.getDoAddNumberOfOverlaps();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        boolean isAroundSummit = parameters.isAroundSummit();
        String[] siteModelTypes = parameters.getSiteModelTypes();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        boolean areBothStrands = true;

        // 1.
        log.info("Read ChIP-Seq peaks and their characteristics");
        Track track = pathToChipSeqTrack.getDataElement(Track.class);
        Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = ChipSeqPeak.readChromosomeAndPeaks(track);
        jobControl.setPreparedness(5);
        if( jobControl.isStopped() ) return null;
        String[] characteristicNames = ChipSeqPeak.getPositiveCharacteristicNames(chromosomeAndPeaks);
        isAroundSummit = isAroundSummit && ChipSeqPeak.isSummitExist(characteristicNames);
        characteristicNames = (String[])ArrayUtils.removeElement(characteristicNames, ChipSeqPeak.SUMMIT);
        characteristicNames = (String[])ArrayUtils.add(characteristicNames, characteristicNames.length, ChipSeqPeak.LENGTH);
        double[][] dataMatrix = ChipSeqPeak.getValuesOfGivenCharacteristics(chromosomeAndPeaks, characteristicNames);
        if( isNumberOfOverlaps )
        {
            Object[] objects = ChipSeqPeak.insertNumbersOfOverlaps(pathToChipSeqTrack, chromosomeAndPeaks, characteristicNames, dataMatrix);
            characteristicNames = (String[])objects[0];
            dataMatrix = (double[][])objects[1];
        }
        jobControl.setPreparedness(15);
        if( jobControl.isStopped() ) return null;

        // 2.
        log.info("Clusterization of peaks : k-means algorithm");
        double[][] transformedDataMatrix = Transformation.transformData(dataMatrix, transformationType);
        KMeansAlgorithm kmAlgorithm = new KMeansAlgorithm(transformedDataMatrix, null, numberOfClusters, Distance.EUCLIDEAN);
        kmAlgorithm.implementKmeansAlgorithm();
        int[] indicesOfClusters = kmAlgorithm.getIndicesOfClusters();
        String commonSubname = numberOfClusters + "_" + track.getName() + "_" + transformationType;
        Clusterization.writeTableWithMeansAndSigmas(transformedDataMatrix, characteristicNames, indicesOfClusters, pathToOutputs, "meansAndSigmas_" + commonSubname);
        Clusterization.writeTableWithDistancesBetweenClusterCenters(Distance.EUCLIDEAN, transformedDataMatrix, indicesOfClusters, pathToOutputs, "distancesBetweenClusterCenters_" + commonSubname);
        jobControl.setPreparedness(40);
        if( jobControl.isStopped() ) return null;

        // 3.
        log.info("Creation of ROC-curves and calculation of AUCs for peak clusters");
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        SiteModelsComparison smc = new SiteModelsComparison(siteModelTypes, matrix);
        Map<String, Map<String, List<ChipSeqPeak>>> nameOfGroupAndPeaksOfGroup = getClusterizedPeaks(chromosomeAndPeaks, indicesOfClusters);
        String commonSubNameOfTables = "_for_" + matrix.getName() + "_in_" + (new TrackInfo(track)).getTfClass() + "_" + pathToChipSeqTrack.getName() + "_clustersNumber_" + numberOfClusters;
        if( isAroundSummit )
            commonSubNameOfTables += "_summit_" + minimalLengthOfSequenceRegion;
        SiteModelsComparisonUtils.writeROCcurvesAndAUCsForGroupedPeaks(characteristicNames, nameOfGroupAndPeaksOfGroup, isAroundSummit, pathToSequences, areBothStrands, minimalLengthOfSequenceRegion, smc, pathToOutputs, commonSubNameOfTables, jobControl, 40, 100);
        return pathToOutputs.getDataCollection();
    }

    private Map<String, Map<String, List<ChipSeqPeak>>> getClusterizedPeaks(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, int[] indicesOfClusters)
    {
        Map<String, Map<String, List<ChipSeqPeak>>> result = new HashMap<>();
        List<ChipSeqPeak> peaksList = new ArrayList<>();
        for( List<ChipSeqPeak> peaks : chromosomeAndPeaks.values() )
            for( ChipSeqPeak peak : peaks )
                peaksList.add(peak);
        for( int i = 0; i < indicesOfClusters.length; i++ )
        {
            String clusterName = Integer.toString(indicesOfClusters[i]);
            ChipSeqPeak peak = peaksList.get(i);
            String chromosome = peak.getChromosome();
            Map<String, List<ChipSeqPeak>> peaksCluster = result.containsKey(clusterName) ? result.get(clusterName) : new HashMap<>();
            List<ChipSeqPeak> peaks = peaksCluster.containsKey(chromosome) ? peaksCluster.get(chromosome) : new ArrayList<>();
            peaks.add(peak);
            peaksCluster.put(chromosome, peaks);
            result.put(clusterName, peaksCluster);
        }
        return result;
    }

    public static class ROCCurvesInClusterizedPeaksParameters extends AbstractFiveSiteModelsParameters
    {
        private String dataTransformationType = Transformation.NO_TRANSFORMATION;
        boolean doAddNumberOfOverlaps = true;
        private int numberOfClusters = 5;

        @PropertyName(MessageBundle.PN_TRANSFORMATION_TYPE)
        @PropertyDescription(MessageBundle.PD_TRANSFORMATION_TYPE)
        public String getDataTransformationType()
        {
            return dataTransformationType;
        }
        public void setDataTransformationType(String dataTransformationType)
        {
            Object oldValue = this.dataTransformationType;
            this.dataTransformationType = dataTransformationType;
            firePropertyChange("dataTransformationType", oldValue, dataTransformationType);
            firePropertyChange("*", null, null);
        }

        @PropertyName(MessageBundle.PN_ADD_NUMBERS_OF_OVERLAPS)
        @PropertyDescription(MessageBundle.PD_ADD_NUMBERS_OF_OVERLAPS)
        public boolean getDoAddNumberOfOverlaps()
        {
            return doAddNumberOfOverlaps;
        }
        public void setDoAddNumberOfOverlaps(boolean doAddNumberOfOverlaps)
        {
            Object oldValue = this.doAddNumberOfOverlaps;
            this.doAddNumberOfOverlaps = doAddNumberOfOverlaps;
            firePropertyChange("doAddNumberOfOverlaps", oldValue, doAddNumberOfOverlaps);
        }

        @PropertyName(MessageBundle.PN_NUMBER_OF_CLUSTERS)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_CLUSTERS)
        public int getNumberOfClusters()
        {
            return numberOfClusters;
        }
        public void setNumberOfClusters(int numberOfClusters)
        {
            Object oldValue = this.numberOfClusters;
            this.numberOfClusters = numberOfClusters;
            firePropertyChange("numberOfClusters", oldValue, numberOfClusters);
        }
    }

    public static class ROCCurvesInClusterizedPeaksParametersBeanInfo extends BeanInfoEx2<ROCCurvesInClusterizedPeaksParameters>
    {
        public ROCCurvesInClusterizedPeaksParametersBeanInfo()
        {
            super(ROCCurvesInClusterizedPeaksParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            property( "trackPath" ).inputElement( Track.class ).add();
            add("aroundSummit");
            add("minRegionLength");
            property("dataTransformationType").tags(Transformation.getTransformationTypes()).add();
            add("doAddNumberOfOverlaps");
            add("numberOfClusters");
            add("siteModelTypes", SiteModelTypesSelector.class);
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
