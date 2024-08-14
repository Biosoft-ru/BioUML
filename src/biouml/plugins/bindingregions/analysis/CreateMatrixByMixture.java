package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.bsa.analysis.consensustomatrix.ConsensusToMatrix;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * Extracted from BindingRegions/mode6
 * 6. Calculation of matrix by using mixture of normal components for maximal IPS scores
 */
public class CreateMatrixByMixture extends AnalysisMethodSupport<CreateMatrixByMixture.CreateMatrixByMixtureParameters>
{
    protected static final int MIN_CLUSTER_SIZE = 5;

    public CreateMatrixByMixture(DataCollection<?> origin, String name)
    {
        super( origin, name, new CreateMatrixByMixtureParameters() );
    }

    @Override
    public CreateMatrixByMixtureParameters getParameters()
    {
        return parameters;
    }

    @Override
    public void setParameters(AnalysisParameters parameters)
    {
        try
        {
            this.parameters = (CreateMatrixByMixtureParameters)parameters;
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException();
        }
    }

    protected void writeCalculatedMatrix(List<Sequence> sequences, String siteModelType, FrequencyMatrix freqMatrix, int minClusterSize, DataCollection<FrequencyMatrix> outputMatrixLib) throws Exception
    {
        double pvalue = parameters.getPValue();
        int numberOfIterations = parameters.getMaxIterations();
        int numberOfCreatingMatrices = parameters.getNumberOfMatrices();
        String baseNameOfCreatingMatrices = parameters.getMatrixBaseName();
        
        log.info("p_value = " + pvalue);
        log.info("Path to Collection of creating matrices = " + outputMatrixLib.getCompletePath());
        log.info("Base Name of Creating Matrices = " + baseNameOfCreatingMatrices);
        log.info("Maximal Number of Iterations = " + numberOfIterations);
        log.info("Number of Matrices Will Be Constructed = " + numberOfCreatingMatrices);
        log.info("SiteModel Type = " + siteModelType);
        
        int n0 = sequences.size();
        double quantile = Stat.getStandartNormalQuantile(1.0 - pvalue, 0.0001, 100);
        List<Sequence> sequences1 = new ArrayList<>();
        for( Sequence seq : sequences )
            sequences1.add(seq);
        for( int iMatrix = 1; iMatrix <= numberOfCreatingMatrices; iMatrix++ )
        {
            int n = sequences1.size();
            Sequence[] seqs = sequences1.toArray(new Sequence[n]);
            double[] maxSiteScores = new double[n];
            double scoreThreshold = 0.0;
            FrequencyMatrix frequencyMatrix = new FrequencyMatrix(outputMatrixLib, baseNameOfCreatingMatrices + "_" + iMatrix, freqMatrix);
            Random random = new Random(1);
            for( int j = 0; j < numberOfIterations; j++ )
            {
                SiteModel siteModel = SiteModelsComparison.getSiteModel(siteModelType, frequencyMatrix, 0.01, null);
                List<Double> siteScores = new ArrayList<>();
                for( int i = 0; i < n; i++ )
                {
                    maxSiteScores[i] = SiteModelsComparisonUtils.findBestSite(seqs[i], siteModel).getScore();
                    siteScores.add(maxSiteScores[i]);
                }
                double[] meanAndSigma0 = Stat.getMeanAndSigma1(siteScores);
                log.info("");
                log.info("initial sample of scores: mean = " + meanAndSigma0[0] + " sigma = " + meanAndSigma0[1] + " size = " + n);
                Map<Integer, Object[]> indexAndObjects = Stat.DistributionMixture.getNormalMixture(siteScores, 2, 1000, random);
                Object[] objects = indexAndObjects.get(0);
                List<Double> subsample = (List<Double>)objects[3];
                Map<String, List<Double>> subsamples = Stat.KolmogorovSmirnovTests.splitIntoNormalSubsampleAndOutliers(subsample, pvalue);
                if( subsamples == null ) return;
                List<Double> normalSubsample = subsamples.get("normalSubsample");
                double[] meanAndSigma = Stat.getMeanAndSigma1(normalSubsample);
                scoreThreshold = meanAndSigma[0] + quantile * meanAndSigma[1];
                List<Sequence> seqsForAlignment = new ArrayList<>();
                for( int i = 0; i < n; i++ )
                    if( maxSiteScores[i] >= scoreThreshold )
                        seqsForAlignment.add(seqs[i]);
                log.info("Matrix = " + iMatrix + " iteration j = " + (j + 1) + ") Random Component: mean = " + meanAndSigma[0] + " sigma = " + meanAndSigma[1] + " scoreThreshold (for matrix in previous iteration) = " + scoreThreshold + "; size of subsample of actual sites = " + seqsForAlignment.size() + " (" + (100.0 * seqsForAlignment.size()) / n0 + "%)");
                SiteModelsComparisonUtils.updateMatrix(seqsForAlignment, frequencyMatrix, siteModelType, siteModel, 3, true);
                FrequencyMatrix matrix = new FrequencyMatrix(outputMatrixLib, baseNameOfCreatingMatrices + "_" + iMatrix + "_iteration_" + (j + 1), frequencyMatrix);
                outputMatrixLib.put(matrix);
                jobControl.setPreparedness(((iMatrix-1) * numberOfIterations + j) * 100 / numberOfCreatingMatrices / numberOfIterations);
                if(jobControl.isStopped()) return;
            }
            sequences1.clear();
            for( int i = 0; i < n; i++ )
                if( maxSiteScores[i] < scoreThreshold )
                    sequences1.add(seqs[i]);
            if( sequences1.size() < minClusterSize ) return;
        }
        CollectionFactoryUtils.save(outputMatrixLib);
    }
    
    protected FrequencyMatrix getInitialMatrix()
    {
        boolean isInitialMatrixGivenByConsensus = parameters.isUseConsensus();
        FrequencyMatrix freqMatrix;
        if( isInitialMatrixGivenByConsensus == false )
        {
            DataElementPath pathToMatrix = parameters.getMatrixPath();
            freqMatrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
            log.info("pathToMatrix = " + pathToMatrix);
        }
        else
        {
            String consensus = parameters.getConsensus();
            log.info("Consensus for initial approximation of matrix = " + consensus);
            freqMatrix = ConsensusToMatrix.consensusToMatrix(consensus, null, "From consensus", null);
            //test_begin: to look at matrix approximation
            double[][] matrix = new double[4][freqMatrix.getLength()];
            byte[] letterToCodeMatrix = freqMatrix.getAlphabet().letterToCodeMatrix();
            for( int i = 0; i < freqMatrix.getLength(); i ++ )
            {
                matrix[0][i] = freqMatrix.getFrequency(i, letterToCodeMatrix['T']);
                matrix[1][i] = freqMatrix.getFrequency(i, letterToCodeMatrix['A']);
                matrix[2][i] = freqMatrix.getFrequency(i, letterToCodeMatrix['G']);
                matrix[3][i] = freqMatrix.getFrequency(i, letterToCodeMatrix['C']);
            }
            for( int i = 0; i < 4; i++ )
            {
                StringBuilder ss = new StringBuilder();
                for( int j = 0; j < freqMatrix.getLength(); j++ )
                    ss.append(matrix[i][j]).append(" ");
                log.info(i + "-th row = " + ss.toString());
            }
            //test_end: to look at matrix approximation
        }
        return freqMatrix;
    }

    @Override
    public DataCollection<FrequencyMatrix> justAnalyzeAndPut() throws Exception
    {
        DataElementPath pathToSingleTrack = parameters.getTrackPath();
        log.info("Calculation of matrix by using mixture of normal components for best sites (sites with maximal scores");
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        String givenTfClass = parameters.getTfClass();
        FrequencyMatrix freqMatrix = getInitialMatrix();
        log.info("Path to Single Input Track = " + pathToSingleTrack);
        log.info("tfClass = " + givenTfClass);
        DataElementPath pathToSequences = TrackInfo.getPathToSequences(pathToSingleTrack);
        log.info("Read binding regions of tfClass = " + givenTfClass + " from track "+pathToSingleTrack);
        jobControl.pushProgress(5, 100);
        Map<String, List<BindingRegion>> selectedBindingRegions = BindingRegion.readBindingRegionsFromTrack(pathToSingleTrack, givenTfClass);
        List<Sequence> sequences = BindingRegion.getLinearSequencesForBindingRegions(selectedBindingRegions, pathToSequences, minimalLengthOfSequenceRegion);
        DataCollection<FrequencyMatrix> matrixLibrary = WeightMatrixCollection.createMatrixLibrary(parameters.getOutputPath(), log);
        String siteModelType = SiteModelsComparison.IPS_SITE_MODEL; //
        writeCalculatedMatrix(sequences, siteModelType, freqMatrix, MIN_CLUSTER_SIZE, matrixLibrary);
        jobControl.popProgress();
        return matrixLibrary;
    }

    public static class CreateMatrixByMixtureParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
        private String tfClass;
        private DataElementPath outputPath;
        private int maxIterations = 15;
        private int numberOfMatrices = 5;
        private String matrixBaseName;
        private double pValue = 0.05;
        private String consensus;
        private DataElementPath matrixPath;
        private boolean useConsensus;
        private int minRegionLength = 300;
        
        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_MERGED)
        public DataElementPath getTrackPath()
        {
            return trackPath;
        }
        public void setTrackPath(DataElementPath trackPath)
        {
            Object oldValue = this.trackPath;
            this.trackPath = trackPath;
            firePropertyChange("trackPath", oldValue, trackPath);
        }

        @PropertyName(MessageBundle.PN_OUTPUT_MATRIX_LIBRARY_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_MATRIX_LIBRARY_PATH)
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
        
        @PropertyName(MessageBundle.PN_MAX_ITERATIONS)
        @PropertyDescription(MessageBundle.PD_MAX_ITERATIONS)
        public int getMaxIterations()
        {
            return maxIterations;
        }
        public void setMaxIterations(int maxIterations)
        {
            Object oldValue = this.maxIterations;
            this.maxIterations = maxIterations;
            firePropertyChange("maxIterations", oldValue, maxIterations);
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_MATRIX_BASE_NAME)
        @PropertyDescription(MessageBundle.PD_OUTPUT_MATRIX_BASE_NAME)
        public String getMatrixBaseName()
        {
            return matrixBaseName;
        }
        public void setMatrixBaseName(String matrixBaseName)
        {
            Object oldValue = this.matrixBaseName;
            this.matrixBaseName = matrixBaseName;
            firePropertyChange("matrixBaseName", oldValue, matrixBaseName);
        }
        
        @PropertyName(MessageBundle.PN_INITIAL_AS_CONSENSUS)
        @PropertyDescription(MessageBundle.PD_INITIAL_AS_CONSENSUS)
        public boolean isUseConsensus()
        {
            return useConsensus;
        }
        public void setUseConsensus(boolean useConsensus)
        {
            Object oldValue = this.useConsensus;
            this.useConsensus = useConsensus;
            firePropertyChange("useConsensus", oldValue, useConsensus);
            firePropertyChange("*", null, null);
        }

        @PropertyName(MessageBundle.PN_CONSENSUS)
        @PropertyDescription(MessageBundle.PD_CONSENSUS)
        public String getConsensus()
        {
            return consensus;
        }
        public void setConsensus(String consensus)
        {
            Object oldValue = this.consensus;
            this.consensus = consensus;
            firePropertyChange("consensus", oldValue, consensus);
        }
        
        public boolean isConsensusHidden()
        {
            return ! isUseConsensus();
        }
        
        @PropertyName(MessageBundle.PN_INITIAL_MATRIX)
        @PropertyDescription(MessageBundle.PD_INITIAL_MATRIX)
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
        
        @PropertyName(MessageBundle.PN_NUMBER_OF_MATRICES)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_MATRICES)
        public int getNumberOfMatrices()
        {
            return numberOfMatrices;
        }
        public void setNumberOfMatrices(int numberOfMatrices)
        {
            Object oldValue = this.numberOfMatrices;
            this.numberOfMatrices = numberOfMatrices;
            firePropertyChange("numberOfMatrices", oldValue, numberOfMatrices);
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
    }

    public static class CreateMatrixByMixtureParametersBeanInfo extends BeanInfoEx2<CreateMatrixByMixtureParameters>
    {
        public CreateMatrixByMixtureParametersBeanInfo()
        {
            super(CreateMatrixByMixtureParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            add("tfClass");
            add("minRegionLength");
            add("pValue");
            add("maxIterations");
            add("numberOfMatrices");
            add("useConsensus");
            addHidden("consensus", "isConsensusHidden");
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).hidden( "isUseConsensus" ).add();
            property( "outputPath" ).outputElement( WeightMatrixCollection.class ).auto( "$trackPath$ lib" ).add();
            property( "matrixBaseName" ).auto( "$trackPath/name$" ).add();
        }
    }
}
