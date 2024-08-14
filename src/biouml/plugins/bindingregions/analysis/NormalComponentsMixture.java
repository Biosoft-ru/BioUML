package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters.SiteModelTypeEditor;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.SampleComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SitePrediction;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.TableUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * Mixture of normal components for scores of best sites
 */
public class NormalComponentsMixture extends AnalysisMethodSupport<NormalComponentsMixture.NormalComponentsMixtureParameters>
{
    public NormalComponentsMixture(DataCollection<?> origin, String name)
    {
        super(origin, name, new NormalComponentsMixtureParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("Mixture of normal components for scores of best sites");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        boolean isChipSeqPeaks = parameters.isUseChipSeqPeaks();
        String givenTfClass = null;
        if( ! isChipSeqPeaks )
            givenTfClass = parameters.getTfClass();
        DataElementPath pathToTrack = parameters.getTrackPath();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        String siteModelType = parameters.getSiteModelType();
        int numberOfMixtureComponents = parameters.getMixtureComponentsNumber();
        double pvalue = parameters.getPValue();
        int maximalNumberOfIterations = parameters.getMaximalNumberOfIterations();
        DataElementPath pathToTable = parameters.getOutputPath();
        boolean areBothStrands = true;

        // 1.
        log.info("Read sequences of ChIP-Seq peaks (or merged binding regions");
        Sequence[] sequences;
        if( isChipSeqPeaks )
        {
            Map<String, List<ChipSeqPeak>> chromosomeAndPeaks = ChipSeqPeak.readChromosomeAndPeaks(pathToTrack.getDataElement(Track.class));
            sequences = ChipSeqPeak.getLinearSequencesForChipSeqPeaks(chromosomeAndPeaks, pathToSequences, minimalLengthOfSequenceRegion, jobControl, 0, 40);
        }
        else
        {
            Map<String, List<BindingRegion>> selectedBindingRegions = BindingRegion.readBindingRegionsFromTrack(pathToTrack, givenTfClass);
            sequences = BindingRegion.sequencesForBindingRegions( selectedBindingRegions, pathToSequences, minimalLengthOfSequenceRegion )
                    .toArray( Sequence[]::new );
            jobControl.setPreparedness(40);
            if( jobControl.isStopped() ) return null;
        }
        
        // 2.
        log.info("Create site model and find best scores");
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        SitePrediction sp = new SitePrediction(siteModelType, null, null, new FrequencyMatrix[]{matrix}, new double[]{0.0});
        List<Double> scores = new ArrayList<>();
        for( Sequence sequence : sequences )
            scores.add(sp.findBestSite(sequence, areBothStrands).getScore());
        jobControl.setPreparedness(30);
        if( jobControl.isStopped() ) return null;
 
        // 3.
        log.info("Create chart 1 that contains total density");
        Map<Integer, Object[]> indexAndObjects = Stat.DistributionMixture.getNormalMixture(scores, numberOfMixtureComponents, maximalNumberOfIterations, new Random(1));
        Map<String, Chart> namesAndCharts = new HashMap<>();
        double[] array1 = MatrixUtils.fromListToArray(scores);
        Map<String, double[]> map1 = new HashMap<>();
        map1.put("", array1);
        SampleComparison samples1 = new SampleComparison(map1, "Best score");
        Chart chart1 = samples1.chartWithSmoothedDensities(true, null, DensityEstimation.WINDOW_WIDTH_03, null);
        namesAndCharts.put("totalDensity", chart1);
        jobControl.setPreparedness(50);
        if( jobControl.isStopped() ) return null;

        // 4.
        log.info("Create charts2 and chart3. Each chart2 contains refined mormals and outliers for given mixture component");
        log.info("Chart3 contains mixture components");
        Map<String, double[]> map3 = new HashMap<>();
        Map<String, Double> multipliers3 = new HashMap<>();
        map3.put("Total density", array1);
        multipliers3.put("Total density", 1.0);
        for( int j = 0; j < numberOfMixtureComponents; j++ )
        {
            Map<String, double[]> map2 = new HashMap<>();
            Map<String, Double> multipliers2 = new HashMap<>();
            Object[] objects = indexAndObjects.get(j);
            double probabilityOfMixtureComponent = (Double)objects[0];
            double[] meanAndSigmaFromMixture = (double[])objects[1];
            double[] meanAndSigmaFromSimulation = (double[])objects[2];
            List<Double> subsample = (List<Double>)objects[3];
            log.info("mixture component = " + j + " probability of mixture component = " + probabilityOfMixtureComponent);
            log.info("mean from mixture = " + meanAndSigmaFromMixture[0] + " sigma from mixture = " + meanAndSigmaFromMixture[1]);
            log.info("mean from simulation = " + meanAndSigmaFromSimulation[0] + " sigma from simulation = " + meanAndSigmaFromSimulation[1]);
            log.info("size of mixture component = " + subsample.size());
            double[] array3 = MatrixUtils.fromListToArray(subsample);
            map2.put("Density of mixture component " + (j + 1), array3);
            multipliers2.put("Density of mixture component " + (j + 1), 1.0);
            map3.put("Mixture component " + (j + 1), array3);
            multipliers3.put("Mixture component " + (j + 1), probabilityOfMixtureComponent);
            
            // 5.
            log.info("Split each mixture component into refined normal part and outliers");
            Map<String, List<Double>> subsamples = Stat.KolmogorovSmirnovTests.splitIntoNormalSubsampleAndOutliers(subsample, pvalue);
            String splitedSampleName = "normalSubsample";
            String name = "Refined mixture component " + (j + 1);
            for( int i = 0; i < 2; i++ )
            {
                if( i == 1 )
                {
                    splitedSampleName = "outliers";
                    name = "Outliers of mixture component " + (j + 1);
                }
                List<Double> splitedSample = subsamples.get(splitedSampleName);
                if( splitedSample == null || splitedSample.isEmpty() ) continue;
                double[] array2 = MatrixUtils.fromListToArray(splitedSample);
                map2.put(name, array2);
                multipliers2.put(name, (double)splitedSample.size() / (double)subsample.size());
                double[] meanAndSigma = Stat.getMeanAndSigma1(splitedSample);
                log.info("Mixture component = " + j + " " + splitedSampleName + ": mean = " + meanAndSigma[0] + " sigma = " + meanAndSigma[1] + " size = " + splitedSample.size());
            }
            SampleComparison samples2 = new SampleComparison(map2, "Best score");
            Chart chart2 = samples2.chartWithSmoothedDensities(true, multipliers2, DensityEstimation.WINDOW_WIDTH_03, null);
            namesAndCharts.put("subsamplesOfMixtureComponent_" + (j + 1), chart2);
            jobControl.setPreparedness(50 + 50 * (j + 1) / numberOfMixtureComponents);
            if( jobControl.isStopped() ) return null;
        }
        SampleComparison samples3 = new SampleComparison(map3, "Best score");
        Chart chart3 = samples3.chartWithSmoothedDensities(true, multipliers3, DensityEstimation.WINDOW_WIDTH_03, null);
        namesAndCharts.put("normalComponentDensities", chart3);
        TableDataCollection table = TableUtils.writeChartsIntoTable(namesAndCharts, "densitiesOfScore", pathToTable);
        jobControl.setPreparedness(100);
        return table;
    }
    
    public static class NormalComponentsMixtureParameters extends AbstractAnalysisParameters
    {
        public NormalComponentsMixtureParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }
        
        private BasicGenomeSelector dbSelector;
        private boolean useChipSeqPeaks = true;
        private DataElementPath trackPath, matrixPath;
        private String siteModelType = SiteModelsComparison.IPS_SITE_MODEL;
        private String tfClass;
        private DataElementPath outputPath;
        private int mixtureComponentsNumber = 2;
        private int maximalNumberOfIterations = 1000;
        private double pValue = 0.05;
        private int minRegionLength = 300;
        
        @PropertyName(MessageBundle.PN_DB_SELECTOR)
        @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
        public BasicGenomeSelector getDbSelector()
        {
            return dbSelector;
        }
        public void setDbSelector(BasicGenomeSelector dbSelector)
        {
            Object oldValue = this.dbSelector;
            this.dbSelector = dbSelector;
            dbSelector.setParent(this);
            firePropertyChange("dbSelector", oldValue, dbSelector);
        }

        @PropertyName(MessageBundle.PN_IS_CHIP_SEQ_PEAKS)
        @PropertyDescription(MessageBundle.PD_IS_CHIP_SEQ_PEAKS)
        public boolean isUseChipSeqPeaks()
        {
            return useChipSeqPeaks;
        }
        public void setUseChipSeqPeaks(boolean useChipSeqPeaks)
        {
            Object oldValue = this.useChipSeqPeaks;
            this.useChipSeqPeaks = useChipSeqPeaks;
            firePropertyChange("useChipSeqPeaks", oldValue, useChipSeqPeaks);
            firePropertyChange("*", null, null);
        }

        @PropertyName(MessageBundle.PN_MIN_REGION_LENGTH)
        @PropertyDescription(MessageBundle.PD_MIN_REGION_LENGTH)
        public int getMinRegionLength()
        {
            return minRegionLength;
        }
        public void setMinRegionLength(int minRegionLength)
        {
            Object oldValue = this.minRegionLength;
            this.minRegionLength = minRegionLength;
            firePropertyChange("minRegionLength", oldValue, minRegionLength);
        }
        
        @PropertyName(MessageBundle.PN_MAX_ITERATIONS)
        @PropertyDescription(MessageBundle.PD_MAX_ITERATIONS)
        public int getMaximalNumberOfIterations()
        {
            return maximalNumberOfIterations;
        }
        public void setMaximalNumberOfIterations(int maximalNumberOfIterations)
        {
            Object oldValue = this.maximalNumberOfIterations;
            this.maximalNumberOfIterations = maximalNumberOfIterations;
            firePropertyChange("maximalNumberOfIterations", oldValue, maximalNumberOfIterations);
        }


        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_MERGED_OR_NOT)
        public DataElementPath getTrackPath()
        {
            return trackPath;
        }
        public void setTrackPath(DataElementPath trackPath)
        {
            Object oldValue = this.trackPath;
            this.trackPath = trackPath;
            if( !Objects.equals( trackPath, oldValue ) )
            {
                Track track = trackPath.optDataElement( Track.class );
                if( track != null )
                    dbSelector.setFromTrack( track );
            }
            firePropertyChange("trackPath", oldValue, trackPath);
        }

        @PropertyName(MessageBundle.PN_OUTPUT_CHART_TABLE_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_CHART_TABLE_PATH)
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
        
        @PropertyName(MessageBundle.PN_MATRIX_PATH)
        @PropertyDescription(MessageBundle.PD_MATRIX_PATH)
        public DataElementPath getMatrixPath()
        {
            return matrixPath;
        }
        
        public void setMatrixPath(DataElementPath matrixPath)
        {
            Object oldValue = this.matrixPath;
            this.matrixPath = matrixPath;
            firePropertyChange("matrixPath", oldValue, matrixPath);
        }

        @PropertyName(MessageBundle.PN_SITE_MODEL_TYPE)
        @PropertyDescription(MessageBundle.PD_SITE_MODEL_TYPE_GENERAL)
        public String getSiteModelType()
        {
            return siteModelType;
        }
        public void setSiteModelType(String siteModelType)
        {
            Object oldValue = this.siteModelType;
            this.siteModelType = siteModelType;
            firePropertyChange("*", oldValue, siteModelType);
        }

        @PropertyName(MessageBundle.PN_TF_CLASS)
        @PropertyDescription(MessageBundle.PD_TF_CLASS)
        public String getTfClass()
        {
            return tfClass;
        }
        public void setTfClass(String tfClass)
        {
            Object oldValue = this.tfClass;
            this.tfClass = tfClass;
            firePropertyChange("tfClass", oldValue, tfClass);
        }
        
        public boolean isTfClassHidden()
        {
            return isUseChipSeqPeaks();
        }
        
        @PropertyName(MessageBundle.PN_MIXTURE_COMPONENTS_NUMBER)
        @PropertyDescription(MessageBundle.PD_MIXTURE_COMPONENTS_NUMBER)
        public int getMixtureComponentsNumber()
        {
            return mixtureComponentsNumber;
        }
        public void setMixtureComponentsNumber(int mixtureComponentsNumber)
        {
            Object oldValue = this.mixtureComponentsNumber;
            this.mixtureComponentsNumber = mixtureComponentsNumber;
            firePropertyChange("mixtureComponentsNumber", oldValue, mixtureComponentsNumber);
        }
        
        @PropertyName(MessageBundle.PN_P_VALUE_THRESHOLD)
        @PropertyDescription(MessageBundle.PD_P_VALUE_THRESHOLD)
        public double getPValue()
        {
            return pValue;
        }
        public void setPValue(double pValue)
        {
            Object oldValue = this.pValue;
            this.pValue = pValue;
            firePropertyChange("pValue", oldValue, pValue);
        }
    }
    
    public static class NormalComponentsMixtureParametersBeanInfo extends BeanInfoEx2<NormalComponentsMixtureParameters>
    {
        public NormalComponentsMixtureParametersBeanInfo()
        {
            super(NormalComponentsMixtureParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add("useChipSeqPeaks");
            addHidden("tfClass", "isTfClassHidden");
            add("minRegionLength");
//          add(OptionEx.makeAutoProperty(new PropertyDescriptor("tfClass", beanClass), "$matrixPath/element/classReference$"));
            property( "trackPath" ).inputElement( Track.class ).add();
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            property( "siteModelType" ).editor( SiteModelTypeEditor.class ).add();
            add( "mixtureComponentsNumber" );
            add("pValue");
            add("maximalNumberOfIterations");
            property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$trackPath$ charts" ).add();
        }
    }
}
