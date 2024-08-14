package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.bsa.analysis.consensustomatrix.ConsensusToMatrix;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters;
import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters.SiteModelTypeEditor;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.FrequencyMatrixUtils;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

/**
 * @author yura
 * Matrix derivation: sequence sample is loaded as column of given table (TableDataCollection)
 */
public class MatrixDerivationAdvanced extends AnalysisMethodSupport<MatrixDerivationAdvanced.MatrixDerivationAdvancedParameters>
{
    private static final String SUCCESSIVE_NORMAL_MIXTURES = "Matrix derivation : successive algorithm based on 2-component normal mixtures";
    private static final String EM_FOR_SEVERAL_MATRICES = "Matrix derivation : EM-like algorithm for identification of several matrices simultaneously";
    private static final String GIVEN_CONSENSUS = "Given consensus";
    private static final String GIVEN_MATRIX = "Given matrix";
    private static final int MIN_CLUSTER_SIZE = 5;
    
    public MatrixDerivationAdvanced(DataCollection<?> origin, String name)
    {
        super(origin, name, new MatrixDerivationAdvancedParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Matrix derivation: sequence sample is loaded as column of given table (TableDataCollection)");
        String algorithmName = parameters.getNameOfAlgorithm();
        String siteModelType = parameters.getSiteModelType();
        Integer window = parameters.getWindow();
        int maxIterations = Math.max(1, parameters.getMaxIterations());
        DataElementPath trackPath = parameters.getTrackPath();
        boolean areBothStrands = parameters.getAreBothStrands();
        NormalMixtureParameters normalMixtureParameters = parameters.getNormalMixtureParameters();
        EmSeveralMatricesParameters emSeveralMatricesParameters = parameters.getEmSeveralMatricesParameters();
        DataElementPath outputPath = parameters.getOutputPath();

        // Algorithm implementation
        //DataCollectionUtils.createFoldersForPath(outputPath.getChildPath(""));
        DataCollection<FrequencyMatrix> matrixLibrary = WeightMatrixCollection.createMatrixLibrary(outputPath, log);
        
        
//        DataElementPath dep = parameters.getOutputPath();
//        DataCollection<FrequencyMatrix> matrixLibrary = WeightMatrixCollection.createMatrixLibrary(dep, log);



        

        
        
        
        //Object[] objects = TableUtils.readGivenColumnInStringTableWithRowNames(pathToTableWithSequenceSample, nameOfTableColumnWithSequenceSample);
        // objects = SequenceSampleUtils.removeMissingDataInSequenceSample((String[])objects[0], (String[])objects[1]);
        //Sequence[] sequences = null;
        DataElementPath pathToFolderWithSequences = DataElementPath.create("databases/EnsemblHuman83_38/Sequences/chromosomes GRCh38");
        int numberOfMostReliableSites = 10000, lengthOfSequenceRegion = 300;
        Sequence[] sequences = null;
        //Sequence[] sequences = EnsemblUtils.getLinearSequencesMostReliableWithGivenLength(trackPath, pathToFolderWithSequences, numberOfMostReliableSites, lengthOfSequenceRegion);

        
        
        // TODO: temp!!!!! print sequences to table
//        log.info(" seq[0] = " + sequences[0].toString());
//        log.info(" seq[1] = " + sequences[1].toString());
//        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"Seq"});
//        for( int jj = 0; jj < 50; jj++ )
//        {
//        	String s = sequences[jj].toString();
//        	dmsc.addRow("seq_" + jj, new String[]{s});
//        }
//        DataMatrixString dms = dmsc.getDataMatrixString();
//		DataElementPath dep = DataElementPath.create("data/Collaboration/yura_project/Data/Files/RAB/RAB_for_CISTROM_03/RAB02_");
//        dms.writeDataMatrixString(false, dep, "sequenceSaqmple", log);

        
        
        
        //public void writeDataMatrixString(boolean doWriteToFile, DataElementPath pathToOutputFolder, String fileOrTableName, Logger log)

        
        switch( algorithmName )
        {
            case SUCCESSIVE_NORMAL_MIXTURES : InitialApproximationForMatrix initialApproximationForMatrix = normalMixtureParameters.getInitialApproximationForMatrix();
                                              FrequencyMatrix frequencyMatrix = getInitialMatrix(initialApproximationForMatrix.getInitialMatrixApproximationType(), initialApproximationForMatrix.getConsensus(), initialApproximationForMatrix.getMatrixName(), initialApproximationForMatrix.getMatrixPath());
                                              //sequences = SequenceSampleUtils.transformSequenceSample((String[])objects[0], (String[])objects[1], frequencyMatrix.getAlphabet());
                                              jobControl.setPreparedness(10);
                                              if( jobControl.isStopped() ) return null;
                                              calculateAndWriteMatrices(sequences, areBothStrands, siteModelType, window, frequencyMatrix, MIN_CLUSTER_SIZE, normalMixtureParameters.getPValue(), maxIterations, Math.max(1, normalMixtureParameters.getNumberOfMatrices()), frequencyMatrix.getName(), matrixLibrary, jobControl, 10, 100); break;
            case EM_FOR_SEVERAL_MATRICES    : double scoreThreshold = emSeveralMatricesParameters.getScoreThreshold();
                                              InitialApproximationForMatrix[] initialApproximationsForMatrices = emSeveralMatricesParameters.getInitialApproximationsForMatrices();
                                              FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[initialApproximationsForMatrices.length];
                                              for( int i = 0; i < initialApproximationsForMatrices.length; i++ )
                                                  frequencyMatrices[i] = getInitialMatrix(initialApproximationsForMatrices[i].getInitialMatrixApproximationType(), initialApproximationsForMatrices[i].getConsensus(), initialApproximationsForMatrices[i].getMatrixName(), initialApproximationsForMatrices[i].getMatrixPath());
                                              //sequences = SequenceSampleUtils.transformSequenceSample((String[])objects[0], (String[])objects[1], frequencyMatrices[0].getAlphabet());
                                              jobControl.setPreparedness(10);
                                              if( jobControl.isStopped() ) return null;
                                              calculateAndWriteMatrices(sequences, areBothStrands, siteModelType, window, frequencyMatrices, MIN_CLUSTER_SIZE, maxIterations, scoreThreshold, matrixLibrary, jobControl, 10, 100); break;
            default                         : throw new Exception("This algorithm '" + algorithmName + "' is not supported in our analysis currently");
        }
        
        return outputPath.getDataCollection();
    }
    
    private FrequencyMatrix getInitialMatrix(String initialMatrixApproximationType, String consensus, String matrixName, DataElementPath pathToMatrixForApproximation)
    {
        switch( initialMatrixApproximationType )
        {
            case GIVEN_MATRIX    : return pathToMatrixForApproximation.getDataElement(FrequencyMatrix.class);
            case GIVEN_CONSENSUS : FrequencyMatrix freqMatrix = ConsensusToMatrix.consensusToMatrix(consensus, null, matrixName, null);
            FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(freqMatrix);
                                   return freqMatrix;
            default              : return null;
        }
    }
    
    private void calculateAndWriteMatrices(Sequence[] sequences, boolean areBothStrands, String siteModelType, Integer window, FrequencyMatrix[] inputMatrices, int minClusterSize, int maxIterations, double scoreThreshold, DataCollection<FrequencyMatrix> outputMatrixLib, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        // 1. Define site models and frequency matrices
        SiteModel[] siteModels = new SiteModel[inputMatrices.length];
        FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[inputMatrices.length];
        for( int i = 0; i < inputMatrices.length; i++ )
        {
            frequencyMatrices[i] = new FrequencyMatrix(outputMatrixLib, inputMatrices[i].getName() + "_revised", inputMatrices[i]);
            siteModels[i] = SiteModelsComparison.getSiteModel(siteModelType, frequencyMatrices[i], 0.01, window);
        }
        
        // 2. Define initial membership indices
        int[] membershipIndices = new int[sequences.length];
        for( int i = 0; i < sequences.length; i++ )
            membershipIndices[i] = siteModels.length;

        // 3. Iterations.
        int iteration = 0, difference = to - from;
        for (;  iteration < maxIterations; iteration++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + (iteration + 1) * difference / maxIterations);
            
            // 4. Estimate new membership
            boolean isChange = false;
            for( int i = 0; i < sequences.length; i++ )
            {
                int membershipIndex = siteModels.length;
                double maxScore = - Double.MAX_VALUE;
                for( int j = 0; j < siteModels.length; j++ )
                    if( siteModels[j] != null )
                    {
                        double score = SiteModelsComparisonUtils.findBestSite(sequences[i], areBothStrands, siteModels[j]).getScore();
                        if( score < scoreThreshold || score < maxScore ) continue;
                        maxScore = score;
                        membershipIndex = j;
                    }
                if( membershipIndex == membershipIndices[i] ) continue;
                membershipIndices[i] = membershipIndex;
                isChange = true;
            }
            if( ! isChange ) break;

            // 5. Update matrices
            for( int i = 0; i < siteModels.length; i++ )
                if( siteModels[i] != null )
                {
                    List<Sequence> seqsForAlignment = new ArrayList<>();
                    for( int j = 0; j < sequences.length; j++ )
                        if( membershipIndices[j] == i )
                            seqsForAlignment.add(sequences[j]);
                    if( seqsForAlignment.size() < minClusterSize )
                        siteModels[i] = null;
                    else
                        SiteModelsComparisonUtils.updateMatrix(seqsForAlignment, frequencyMatrices[i], siteModelType, siteModels[i], 1, areBothStrands);

                }
        }
        log.info("Number of actual iterations = " + iteration);

        // 6. Save derived matrices
        for( int i = 0; i < siteModels.length; i++ )
            if( siteModels[i] != null )
                outputMatrixLib.put(frequencyMatrices[i]);
        CollectionFactory.save(outputMatrixLib);
        if( jobControl != null )
            jobControl.setPreparedness(to);
    }
    
    private void calculateAndWriteMatrices(Sequence[] sequences, boolean areBothStrands, String siteModelType, Integer window, FrequencyMatrix freqMatrix, int minClusterSize, double pvalue, int numberOfIterations, int numberOfCreatingMatrices, String baseNameOfCreatingMatrices, DataCollection<FrequencyMatrix> outputMatrixLib, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from, totalNumberOfSteps = numberOfCreatingMatrices * numberOfIterations;
        log.info("Path to Collection of creating matrices = " + outputMatrixLib.getCompletePath());
        double quantile = Stat.getStandartNormalQuantile(1.0 - pvalue, 0.0001, 100);
        List<Sequence> sequencesList = new ArrayList<>();
        for( Sequence seq : sequences )
            sequencesList.add(seq);
        for( int iMatrix = 1; iMatrix <= numberOfCreatingMatrices; iMatrix++ )
        {
            Sequence[] seqs = sequencesList.toArray(new Sequence[sequencesList.size()]);
            double[] maxSiteScores = new double[sequencesList.size()];
            double scoreThreshold = 0.0;
            FrequencyMatrix frequencyMatrix = new FrequencyMatrix(outputMatrixLib, baseNameOfCreatingMatrices + "_" + iMatrix, freqMatrix);
            Random random = new Random(1);
            for( int j = 0; j < numberOfIterations; j++ )
            {
                if( jobControl != null )
                    jobControl.setPreparedness(from + ((iMatrix - 1) * numberOfIterations + j + 1) * difference / totalNumberOfSteps);
                SiteModel siteModel = SiteModelsComparison.getSiteModel(siteModelType, frequencyMatrix, 0.01, window);
                List<Double> siteScores = new ArrayList<>();
                for( int i = 0; i < sequencesList.size(); i++ )
                {
                    maxSiteScores[i] = SiteModelsComparisonUtils.findBestSite(seqs[i], areBothStrands, siteModel).getScore();
                    siteScores.add(maxSiteScores[i]);
                }
                double[] meanAndSigma0 = Stat.getMeanAndSigma1(siteScores);
                log.info("");
                log.info("initial sample of scores: mean = " + meanAndSigma0[0] + " sigma = " + meanAndSigma0[1] + " size = " + sequencesList.size());
                Map<Integer, Object[]> indexAndObjects = Stat.DistributionMixture.getNormalMixture(siteScores, 2, 1000, random);
                Object[] objects = indexAndObjects.get(0);
                List<Double> subsample = (List<Double>)objects[3];
                Map<String, List<Double>> subsamples = Stat.KolmogorovSmirnovTests.splitIntoNormalSubsampleAndOutliers(subsample, pvalue);
                if( subsamples == null ) return;
                List<Double> normalSubsample = subsamples.get("normalSubsample");
                double[] meanAndSigma = Stat.getMeanAndSigma1(normalSubsample);
                scoreThreshold = meanAndSigma[0] + quantile * meanAndSigma[1];
                
                
                // TODO!!!!!
                ///////////////// Temporary!!!!!
                scoreThreshold = 0.0;
                
                
                List<Sequence> seqsForAlignment = new ArrayList<>();
                for( int i = 0; i < sequencesList.size(); i++ )
                    if( maxSiteScores[i] >= scoreThreshold )
                        seqsForAlignment.add(seqs[i]);
                log.info("Matrix = " + iMatrix + " iteration j = " + (j + 1) + ") Random Component: mean = " + meanAndSigma[0] + " sigma = " + meanAndSigma[1] + " scoreThreshold (for matrix in previous iteration) = " + scoreThreshold + "; size of subsample of actual sites = " + seqsForAlignment.size() + " (" + (100.0 * seqsForAlignment.size()) / sequences.length + "%)");
                SiteModelsComparisonUtils.updateMatrix(seqsForAlignment, frequencyMatrix, siteModelType, siteModel, 3, areBothStrands);
                FrequencyMatrix matrix = new FrequencyMatrix(outputMatrixLib, baseNameOfCreatingMatrices + "_" + iMatrix + "_iteration_" + (j + 1), frequencyMatrix);

                /*******/
                FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(matrix);
                
                outputMatrixLib.put(matrix);
            }
            sequencesList.clear();
            for( int i = 0; i < seqs.length; i++ )
                if( maxSiteScores[i] < scoreThreshold )
                    sequencesList.add(seqs[i]);
            if( sequencesList.size() < minClusterSize ) return;
        }
        CollectionFactory.save(outputMatrixLib);
    }
    
    public static class NormalMixtureParameters extends AbstractAnalysisParameters
    {
        private double pValue = 0.05;
        private int numberOfMatrices = 5;
        private InitialApproximationForMatrix initialApproximationForMatrix;
        
        public NormalMixtureParameters()
        {
            setInitialApproximationForMatrix(new InitialApproximationForMatrix());
        }
        
        @PropertyName(MessageBundle.PN_INITIAL_APPROXIMATION_FOR_MATRIX)
        @PropertyDescription(MessageBundle.PD_INITIAL_APPROXIMATION_FOR_MATRIX)
        public InitialApproximationForMatrix getInitialApproximationForMatrix()
        {
            return initialApproximationForMatrix;
        }
        public void setInitialApproximationForMatrix(InitialApproximationForMatrix initialApproximationForMatrix)
        {
            Object oldValue = this.initialApproximationForMatrix;
            this.initialApproximationForMatrix = withPropagation(this.initialApproximationForMatrix, initialApproximationForMatrix);
            firePropertyChange("initialApproximationForMatrix", oldValue, initialApproximationForMatrix);
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
    }
    
    public static class NormalMixtureParametersBeanInfo extends BeanInfoEx2<NormalMixtureParameters>
    {
        public NormalMixtureParametersBeanInfo()
        {
            super(NormalMixtureParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("pValue");
            add("numberOfMatrices");
            add("initialApproximationForMatrix");
        }
    }
    
    public static class EmSeveralMatricesParameters extends AbstractAnalysisParameters
    {
        private double scoreThreshold;
        private InitialApproximationForMatrix[] initialApproximationsForMatrices;
        
        public EmSeveralMatricesParameters()
        {
            setInitialApproximationsForMatrices(new InitialApproximationForMatrix[]{new InitialApproximationForMatrix()});
        }
        
        @PropertyName(MessageBundle.PN_SCORE_THRESHOLD)
        @PropertyDescription(MessageBundle.PD_SCORE_THRESHOLD)
        public double getScoreThreshold()
        {
            return scoreThreshold;
        }
        public void setScoreThreshold(double scoreThreshold)
        {
            Object oldValue = this.scoreThreshold;
            this.scoreThreshold = scoreThreshold;
            firePropertyChange("scoreThreshold", oldValue, scoreThreshold);
        }
        
        @PropertyName(MessageBundle.PN_INITIAL_APPROXIMATIONS_FOR_MATRICES)
        @PropertyDescription(MessageBundle.PD_INITIAL_APPROXIMATIONS_FOR_MATRICES)
        public InitialApproximationForMatrix[] getInitialApproximationsForMatrices()
        {
            return initialApproximationsForMatrices;
        }
        public void setInitialApproximationsForMatrices(InitialApproximationForMatrix[] initialApproximationsForMatrices)
        {
            Object oldValue = this.initialApproximationsForMatrices;
            this.initialApproximationsForMatrices = withPropagation(this.initialApproximationsForMatrices, initialApproximationsForMatrices);
            firePropertyChange("initialApproximationsForMatrices", oldValue, initialApproximationsForMatrices);
        }
    }
    
    public static class EmSeveralMatricesParametersBeanInfo extends BeanInfoEx2<EmSeveralMatricesParameters>
    {
        public EmSeveralMatricesParametersBeanInfo()
        {
            super(EmSeveralMatricesParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("scoreThreshold");
            add("initialApproximationsForMatrices");
        }
    }
    
    public static class InitialApproximationForMatrix extends OptionEx
    {
        private String initialMatrixApproximationType = GIVEN_CONSENSUS;
        private String consensus;
        private String matrixName;
        private DataElementPath matrixPath;
        
        @PropertyName(MessageBundle.PN_INITIAL_MATRIX_APPROXIMATION_TYPE)
        @PropertyDescription(MessageBundle.PD_INITIAL_MATRIX_APPROXIMATION_TYPE)
        public String getInitialMatrixApproximationType()
        {
            return initialMatrixApproximationType;
        }
        public void setInitialMatrixApproximationType(String initialMatrixApproximationType)
        {
            Object oldValue = this.initialMatrixApproximationType;
            this.initialMatrixApproximationType = initialMatrixApproximationType;
            firePropertyChange("initialMatrixApproximationType", oldValue, initialMatrixApproximationType);
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
        
        @PropertyName(MessageBundle.PN_MATRIX_NAME)
        @PropertyDescription(MessageBundle.PD_MATRIX_NAME)
        public String getMatrixName()
        {
            return matrixName;
        }
        public void setMatrixName(String matrixName)
        {
            Object oldValue = this.matrixName;
            this.matrixName = matrixName;
            firePropertyChange("matrixName", oldValue, matrixName);
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
        
        public boolean isConsensusHidden()
        {
            return ! getInitialMatrixApproximationType().equals(GIVEN_CONSENSUS);
        }
        
        public boolean isPathToMatrixForApproximationHidden()
        {
            return ! getInitialMatrixApproximationType().equals(GIVEN_MATRIX);
        }
    }
    
    public static class InitialApproximationForMatrixBeanInfo extends BeanInfoEx2<InitialApproximationForMatrix>
    {
        public InitialApproximationForMatrixBeanInfo()
        {
            super(InitialApproximationForMatrix.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("initialMatrixApproximationType", beanClass), InitialMatrixApproximationTypeSelector.class);
            addHidden("consensus", "isConsensusHidden");
            addHidden("matrixName", "isConsensusHidden");
            addHidden(DataElementPathEditor.registerInput("matrixPath", beanClass, FrequencyMatrix.class, true), "isPathToMatrixForApproximationHidden");
        }
    }

    public static class MatrixDerivationAdvancedParameters extends AbstractFiveSiteModelsParameters
    {
        private NormalMixtureParameters normalMixtureParameters;
        private EmSeveralMatricesParameters emSeveralMatricesParameters;
        private String nameOfAlgorithm = SUCCESSIVE_NORMAL_MIXTURES;
        private int maxIterations = 15;

        public MatrixDerivationAdvancedParameters()
        {
            setNormalMixtureParameters(new NormalMixtureParameters());
            setEmSeveralMatricesParameters(new EmSeveralMatricesParameters());
        }
        
        @PropertyName("Parameters specific for '" + SUCCESSIVE_NORMAL_MIXTURES + "'")
        public NormalMixtureParameters getNormalMixtureParameters()
        {
            return normalMixtureParameters;
        }
        public void setNormalMixtureParameters(NormalMixtureParameters normalMixtureParameters)
        {
            Object oldValue = this.normalMixtureParameters;
            this.normalMixtureParameters = withPropagation(this.normalMixtureParameters, normalMixtureParameters);
            firePropertyChange("normalMixtureParameters", oldValue, normalMixtureParameters);
        }

        @PropertyName("Parameters specific for '" + EM_FOR_SEVERAL_MATRICES + "'")
        public EmSeveralMatricesParameters getEmSeveralMatricesParameters()
        {
            return emSeveralMatricesParameters;
        }
        public void setEmSeveralMatricesParameters(EmSeveralMatricesParameters emSeveralMatricesParameters)
        {
            Object oldValue = this.emSeveralMatricesParameters;
            this.emSeveralMatricesParameters = withPropagation(this.emSeveralMatricesParameters, emSeveralMatricesParameters);
            firePropertyChange("emSeveralMatricesParameters", oldValue, emSeveralMatricesParameters);
        }

        @PropertyName(MessageBundle.PN_NAME_OF_ALGORITHM)
        @PropertyDescription(MessageBundle.PD_NAME_OF_ALGORITHM)
        public String getNameOfAlgorithm()
        {
            return nameOfAlgorithm;
        }
        public void setNameOfAlgorithm(String nameOfAlgorithm)
        {
            Object oldValue = this.nameOfAlgorithm;
            this.nameOfAlgorithm = nameOfAlgorithm;
            firePropertyChange("*", oldValue, nameOfAlgorithm);
            firePropertyChange("*", null, null);
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

        public boolean areNormalMixtureParametersHidden()
        {
            return ! getNameOfAlgorithm().equals(SUCCESSIVE_NORMAL_MIXTURES);
        }
        
        public boolean areEmSeveralMatricesParametersHidden()
        {
            return ! getNameOfAlgorithm().equals(EM_FOR_SEVERAL_MATRICES);
        }
    }
    
    public static class InitialMatrixApproximationTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{GIVEN_MATRIX, GIVEN_CONSENSUS};
        }
    }
    
    public static class AlgorithmSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{SUCCESSIVE_NORMAL_MIXTURES, EM_FOR_SEVERAL_MATRICES};
        }
    }
    
    public static class MatrixDerivationAdvancedParametersBeanInfo extends BeanInfoEx2<MatrixDerivationAdvancedParameters>
    {
        public MatrixDerivationAdvancedParametersBeanInfo()
        {
            super(MatrixDerivationAdvancedParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("nameOfAlgorithm", beanClass), AlgorithmSelector.class);
            add(new PropertyDescriptorEx("siteModelType", beanClass), SiteModelTypeEditor.class);
            addHidden("window", "isWindowHidden");
            add("areBothStrands");
            
            //add(DataElementPathEditor.registerInputChild("trackPath", beanClass, Track.class));
            property("trackPath").inputElement(Track.class).add();


            //add(ColumnNameSelector.registerSelector("nameOfTableColumnWithSequenceSample", beanClass, "pathToTableWithSequenceSample", false));
            addHidden("normalMixtureParameters", "areNormalMixtureParametersHidden");
            addHidden("emSeveralMatricesParameters", "areEmSeveralMatricesParametersHidden");
            add("maxIterations");

            add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, WeightMatrixCollection.class), ""));
            //add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, true));

            // add(DataElementPathEditor.registerOutput("outputPath", beanClass, TableDataCollection.class, false));
//            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}

