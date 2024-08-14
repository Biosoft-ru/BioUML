package biouml.plugins.bindingregions.analysis;
// 09.06.24

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters;
import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters.SiteModelTypeEditor;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ExploratoryAnalysisUtil;
import biouml.plugins.gtrd.utils.DataBaseUtils.CisBpUtils;
import biouml.plugins.gtrd.utils.EnsemblUtils;
import biouml.plugins.gtrd.utils.EnsemblUtils.OligUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.FrequencyMatrixUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.RocCurve;
import biouml.plugins.machinelearning.distribution_mixture.NormalMixture;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixInteger;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
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
//import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author yura
 * Matrix derivation: sequence sample is loaded as column of given table (TableDataCollection)
 */
public class MatrixDerivation extends AnalysisMethodSupport<MatrixDerivation.MatrixDerivationParameters>
{
    private static final String SUCCESSIVE_NORMAL_MIXTURES = "Matrix derivation : successive algorithm based on 2-component normal mixtures";
    private static final String EM_FOR_SEVERAL_MATRICES = "Matrix derivation : EM-like algorithm for identification of several matrices simultaneously";
    private static final String LIBRARY_CONSTRUCTION = "Library construction for given organism";
    private static final String[] TYPES_OF_INITIAL_MATRICES = {"Matrices from CIS-BP", "Matrices derived from best oligonucleotides"};
    
    private static final String GIVEN_CONSENSUS = "Given consensus";
    private static final String GIVEN_CONSENSUS_FROM_CISBP = "Given consensus from CIS-BP";
    private static final String GIVEN_MATRIX = "Given matrix";
    private static final String GIVEN_MATRIX_FROM_CISBP = "Given matrix from CIS-BP";
    
    private static final int MIN_CLUSTER_SIZE = 5;
    
    public MatrixDerivation(DataCollection<?> origin, String name)
    {
        super(origin, name, new MatrixDerivationParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        String algorithmName = parameters.getNameOfAlgorithm();
        log.info("Matrix derivation : " + algorithmName);
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        String siteModelType = parameters.getSiteModelType();
        Integer window = parameters.getWindow();
        int maxIterations = Math.max(1, parameters.getMaxIterations());
        DataElementPath inputTrackPath = parameters.getTrackPath();
        boolean areBothStrands = parameters.getAreBothStrands();
        int lengthOfEachSequence = parameters.getLengthOfEachSequence();
        int numberOfSequences = parameters.getNumberOfSequences();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // Algorithm implementation
        DataElementPath pathToMatrixLibrary = pathToOutputs.getChildPath("Matrices");
        DataCollection<FrequencyMatrix> outputMatrixLibrary = WeightMatrixCollection.createMatrixLibrary(pathToMatrixLibrary, log);
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));

        switch( algorithmName )
        {
            case SUCCESSIVE_NORMAL_MIXTURES : // Sequence[] sequences = getLinearSequencesWithGivenLengthForBestSites(inputTrackPath, pathToSequences, lengthOfEachSequence, numberOfSequences);
            								  Sequence[] sequences = EnsemblUtils.getLinearSequencesWithGivenLengthForBestSites(inputTrackPath, numberOfSequences, lengthOfEachSequence, pathToSequences);
            								  NormalMixtureParameters normalMixtureParameters = parameters.getNormalMixtureParameters();
            								  InitialApproximationForMatrix initialApproximationForMatrix = normalMixtureParameters.getInitialApproximationForMatrix();
                                              FrequencyMatrix frequencyMatrix = getInitialMatrix(initialApproximationForMatrix.getInitialMatrixApproximationType(), initialApproximationForMatrix.getConsensus(), initialApproximationForMatrix.getMatrixName(), initialApproximationForMatrix.getMatrixPath());
                                              jobControl.setPreparedness(10);
                                              int maxNumberOfCreatingMatrices = Math.max(1, parameters.getMaxNumberOfCreatingMatrices());
                                          	  // 29.04.22
                                              // calculateAndWriteMatrices(sequences, areBothStrands, siteModelType, window, frequencyMatrix, normalMixtureParameters.getPValue(), maxIterations, maxNumberOfMatrices, frequencyMatrix.getName(), outputMatrixLibrary, jobControl, 10, 100);
                                              calculateAndWriteMatrices(sequences, areBothStrands, siteModelType, window, frequencyMatrix, maxIterations, maxNumberOfCreatingMatrices, frequencyMatrix.getName(), outputMatrixLibrary, jobControl, 10, 100);

                                              CollectionFactory.save(outputMatrixLibrary);
                                              break;
            case EM_FOR_SEVERAL_MATRICES    : // sequences = getLinearSequencesWithGivenLengthForBestSites(inputTrackPath, pathToSequences, lengthOfEachSequence, numberOfSequences);
			  								  sequences = EnsemblUtils.getLinearSequencesWithGivenLengthForBestSites(inputTrackPath, numberOfSequences, lengthOfEachSequence, pathToSequences);

            								  EmSeveralMatricesParameters emSeveralMatricesParameters = parameters.getEmSeveralMatricesParameters();
            								  double scoreThreshold = emSeveralMatricesParameters.getScoreThreshold();
                                              InitialApproximationForMatrix[] initialApproximationsForMatrices = emSeveralMatricesParameters.getInitialApproximationsForMatrices();
                                              FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[initialApproximationsForMatrices.length];
                                              for( int i = 0; i < initialApproximationsForMatrices.length; i++ )
                                                  frequencyMatrices[i] = getInitialMatrix(initialApproximationsForMatrices[i].getInitialMatrixApproximationType(), initialApproximationsForMatrices[i].getConsensus(), initialApproximationsForMatrices[i].getMatrixName(), initialApproximationsForMatrices[i].getMatrixPath());
                                              for( int i = 0; i < frequencyMatrices.length; i++ )
                                              {
                                            	  log.info("Initial matrix No " + i + " = ");
                                            	  FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(frequencyMatrices[i]);
                                              }
                                              jobControl.setPreparedness(10);
                                              if( jobControl.isStopped() ) return null;
                                              calculateAndWriteMatrices(sequences, areBothStrands, siteModelType, window, frequencyMatrices, maxIterations, scoreThreshold, outputMatrixLibrary, jobControl, 10, 100); break;
            case LIBRARY_CONSTRUCTION		: DataElementPath pathToInputFolder = parameters.getPathToInputFolder();
            								  LibraryDerivationParameters libraryDerivationParameters = parameters.getLibraryDerivationParameters();
            								  DataElementPath pathToTextFileWithCisBpInformation = libraryDerivationParameters.getPathToTextFileWithCisBpInformation();
            								  DataElementPath pathToFolderWithCisBpMatrices = libraryDerivationParameters.getPathToFolderWithCisBpMatrices();
            								  int lengthOfBestOligonucleotides = libraryDerivationParameters.getLengthOfBestOligonucleotides();
            								  int numberOfBestOligonucleotides = libraryDerivationParameters.getNumberOfBestOligonucleotides();
            								  maxNumberOfCreatingMatrices = Math.max(1, parameters.getMaxNumberOfCreatingMatrices());

            								  String typeOfInitialMatrices = parameters.getLibraryDerivationParameters().getTypeOfInitialMatrices();
            								  log.info("typeOfInitialMatrices = " + typeOfInitialMatrices);
            								  // calculateAndWriteAucTest(); break;
            								  ExploratoryAnalysisUtil.rab_temporary(pathToOutputs);
            								  transformCisBpMatrixToFrequencyMatrixTest(); break;
            								  //calculateMatrixLibraryForGivenOrganism(pathToMatrixLibrary, outputMatrixLibrary, typeOfInitialMatrices, pathToInputFolder, pathToSequences, lengthOfEachSequence, numberOfSequences, lengthOfBestOligonucleotides, numberOfBestOligonucleotides, maxNumberOfCreatingMatrices, pathToTextFileWithCisBpInformation, pathToFolderWithCisBpMatrices, siteModelType, window, maxIterations, jobControl, 0, 100); break;
            default                         : log.info("This algorithm '" + algorithmName + "' is not supported in our analysis currently");
        }
        //return (DataCollection<?>) pathToOutputs;
        return pathToOutputs.getDataCollection();

    }
    
    // 19.04.22
    // Name of each metaclusterTrack is UniProt ID.
    // All tracks within input folder 'pathToFolderWithTracksWithMetaCusters' must be previously calculated for given organism.
    private void calculateMatrixLibraryForGivenOrganism(DataElementPath pathToMatrixLibrary, DataCollection<FrequencyMatrix> outputMatrixLibrary, String typeOfInitialMatrices, DataElementPath pathToFolderWithTracksWithMetaClusters, DataElementPath pathToSequences, int lengthOfEachSequence, int numberOfSequences, int lengthOfBestOligonucleotides, int numberOfBestOligonucleotides, int maxNumberOfCreatingMatrices, DataElementPath pathToTextFileWithCisBpInformation, DataElementPath pathToFolderWithCisBpMatrices, String siteModelType, Integer window, int maxNumberOfIterations, AnalysisJobControl jobControl, int from, int to)
    {
    	String[] uniprotIds = pathToFolderWithTracksWithMetaClusters.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
    	log.info("dim(uniprotIds) = " + uniprotIds.length);

    	// 06.05.22
//    	DataMatrixString dms = new DataMatrixString(pathToTextFileWithCisBpInformation, new String[]{"Motif_ID"});
//    	String[] cisBpIds = dms.getRowNames(), cisBpMatrixNames = dms.getColumn("Motif_ID");
    	
        int difference = to - from;
        
//TODO : temporary /////////////////////////////////////////////////////////////        
        int temporaryIndex = 0;

        
        for( int i = 0; i < uniprotIds.length; i++ )
        {
        	log.info("uniprotIds = " + uniprotIds[i]);
        	if( uniprotIds[i].equals("Combined_peaks") ) continue;

            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / uniprotIds.length);

        	// 1. Calculate sequence sample and consensus
        	// 06.05.22
            
        	log.info("pathToFolderWithTracksWithMetaClusters = " + pathToFolderWithTracksWithMetaClusters);

        	DataElementPath pathToTrackWithMetaClusters = pathToFolderWithTracksWithMetaClusters.getChildPath(uniprotIds[i]);
        	

        	
        	Sequence[] sequences = EnsemblUtils.getLinearSequencesWithGivenLengthForBestSites(pathToTrackWithMetaClusters, numberOfSequences, lengthOfEachSequence, pathToSequences);
        	log.info("sequences.length = " + sequences.length);

        	String consensus = getConsensusFromCisBpMatrixOrOligs(typeOfInitialMatrices, pathToTextFileWithCisBpInformation, pathToFolderWithCisBpMatrices, uniprotIds[i], lengthOfBestOligonucleotides, sequences);
        	log.info("i = " + i + " uniprotIds = " + uniprotIds[i] + " consensus = " + consensus);
            if( consensus == null ) continue;
        	consensus = "NNN" + consensus + "NNN";
        	
        	// 23.05.22aa
        	// 2. Calculate initial matrix approximation of frequencyMatrix (GIVEN_CONSENSUS is used)
        	//DataElementPath pathToMatrixForApproximation = null;
        	// FrequencyMatrix frequencyMatrix = getInitialMatrix(GIVEN_CONSENSUS_FROM_CISBP, consensus, uniprotIds[i], pathToMatrixForApproximation);
        	//FrequencyMatrix frequencyMatrix = getInitialMatrixFromCisbp(GIVEN_MATRIX_FROM_CISBP, outputMatrixLibrary, pathToFolderWithCisBpMatrices, consensus, uniprotIds[i]);
        	
        	FrequencyMatrix frequencyMatrix = getInitialMatrixFromCisbp(GIVEN_MATRIX_FROM_CISBP, outputMatrixLibrary,
            		pathToTextFileWithCisBpInformation, pathToFolderWithCisBpMatrices, consensus, uniprotIds[i]);
        	
        	log.info("iinitial matrix = ");
        	FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(frequencyMatrix);
        	
//        	String[] cisBpMatrixNames = new String[]{"M00008_2.00.txt", "M00153_2.00.txt"};
//        	DataElementPath pathToMatrix = pathToFolderWithCisBpMatrices.getChildPath(cisBpMatrixNames[i]);
//            FrequencyMatrix freqMat = FrequencyMatrixUtils.fromDataMatrixToFrequencyMatrix(dataMatrix, matrixLibrary, cisBpMatrixNames[i]);

        	
        	// 3. Calculate and write matrices
        	String baseNameOfCreatingMatrices = frequencyMatrix.getName();
        	calculateAndWriteMatrices(sequences, true, siteModelType, window, frequencyMatrix, maxNumberOfIterations, maxNumberOfCreatingMatrices, baseNameOfCreatingMatrices, outputMatrixLibrary, jobControl, from, to);
        	

        	// 4. Calculate and write revised matrices
        	// 13.05.22
        	boolean areBothStrands = true;
        	double scoreThreshold = getScoreThreshold(siteModelType);
        	calculateAndWriteRevisedMatrices(pathToMatrixLibrary, sequences, uniprotIds[i], areBothStrands, siteModelType, window, maxNumberOfIterations, scoreThreshold, outputMatrixLibrary, jobControl, from, to);

        	
 //TODO : temporary /////////////////////////////////////////////////////////////        
        	if( ++ temporaryIndex >= 3 ) break;
        	
        	
        }
        CollectionFactory.save(outputMatrixLibrary);
    }
    
    ////FrequencyMatrix getFrequencyMatrixFromCisBp(DataCollection<FrequencyMatrix> matrixLibrary, DataElementPath pathToFolderWithCisBpMatrices, String cisBpMatrixName)
    private FrequencyMatrix getInitialMatrixFromCisbp(String initialMatrixApproximationType, DataCollection<FrequencyMatrix> matrixLibrary,
    		DataElementPath pathToTextFileWithCisBpInformation, DataElementPath pathToFolderWithTextMatricesFiles, String consensus, String uniprotId)
    {
        switch( initialMatrixApproximationType )
        {
            //case GIVEN_MATRIX_FROM_CISBP    : return  getFrequencyMatrixFromCisBp(matrixLibrary, pathToFolderWithCisBpMatrices, matrixName);
            case GIVEN_MATRIX_FROM_CISBP    : DataMatrix dataMatrix = CisBpUtils.getDataMatrix(pathToTextFileWithCisBpInformation, pathToFolderWithTextMatricesFiles, uniprotId);
            					              return FrequencyMatrixUtils.fromDataMatrixToFrequencyMatrix(dataMatrix, matrixLibrary, uniprotId);
            					              
            case GIVEN_CONSENSUS_FROM_CISBP : // TODO: FrequencyMatrix freqMatrix = ConsensusToMatrix.consensusToMatrix(consensus, null, matrixName, null);
//            					   			  log.info(" Initial matrix approximation :");
//            					   			  FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(freqMatrix);
//            					   			  return freqMatrix;
            default				            : return null;
        }
    }
    
    private FrequencyMatrix getInitialMatrix(String initialMatrixApproximationType, String consensus, String matrixName,
    										 DataElementPath pathToMatrixForApproximation)
    {
        switch( initialMatrixApproximationType )
        {
            case GIVEN_MATRIX    : return pathToMatrixForApproximation.getDataElement(FrequencyMatrix.class);
            case GIVEN_CONSENSUS : FrequencyMatrix freqMatrix = ConsensusToMatrix.consensusToMatrix(consensus, null, matrixName, null);
            					   log.info(" Initial matrix approximation :");
            					   FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(freqMatrix);
                                   return freqMatrix;
            default              : return null;
        }
    }

    private void transformCisBpMatrixToFrequencyMatrixTest() throws Exception
    {
    	
    	DataElementPath pathToFolderWithCisBpMatrices = DataElementPath.create("data/Collaboration/yura_project/Data/123/PWMs");
    	DataElementPath pathToFolderWithFrequencyMatrices = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_03_RAB/MATRIX_03_RAB_COMMON/Matrices");
        DataCollection<FrequencyMatrix> matrixLibrary = WeightMatrixCollection.createMatrixLibrary(pathToFolderWithFrequencyMatrices, log);


        // 1.
    	String consensus = "AaTtGgCc";
    	FrequencyMatrix freqMatrix = FrequencyMatrixUtils.consensusToMatrixTest(consensus, matrixLibrary, "test_matrix");
    	FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(freqMatrix);

//    	String[] cisBpMatrixNames = new String[]{"M00008_2.00.txt", "M00153_2.00.txt"};
//    	DataElementPath pathToMatrix = pathToFolderWithCisBpMatrices.getChildPath(cisBpMatrixNames[i]);
//        FrequencyMatrix freqMat = FrequencyMatrixUtils.fromDataMatrixToFrequencyMatrix(dataMatrix, matrixLibrary, cisBpMatrixNames[i]);

        
        // 2.
    	String[] cisBpMatrixNames = new String[]{"M00008_2.00.txt", "M00153_2.00.txt"};
        for( int i = 0; i < cisBpMatrixNames.length; i++ )
        {
        	
        	
        	DataElementPath pathToMatrix = pathToFolderWithCisBpMatrices.getChildPath(cisBpMatrixNames[i]);
        	DataMatrix dataMatrix = new DataMatrix(pathToMatrix, new String[]{"A", "C", "G", "T"});
        	log.info("i = " + i + " cisBpMatrixNames[i] = " + cisBpMatrixNames[i] + " dataMatrix = ");
        	DataMatrix.printDataMatrix(dataMatrix);


            FrequencyMatrix freqMat = FrequencyMatrixUtils.fromDataMatrixToFrequencyMatrix(dataMatrix, matrixLibrary, cisBpMatrixNames[i]);
        	log.info("i = " + i + " cisBpMatrixNames[i] = " + cisBpMatrixNames[i]);
        	FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(freqMat);

        	
        	
//        	FrequencyMatrix frequencyMatrix = CisBpUtils.getFrequencyMatrixFromTextFile(matrixLibrary, pathToFolderWithCisBpMatrices, cisBpMatrixNames[i]);
//        	FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(frequencyMatrix);
        }
        CollectionFactory.save(matrixLibrary);
    }

    private double getScoreThreshold(String siteModelType)
    {
    	switch(siteModelType)
    	{
    		case SiteModelUtils.IPS_MODEL			: return 4.3;
    		case SiteModelUtils.MATCH_MODEL			:
    		case SiteModelUtils.WEIGHT_MATRIX_MODEL : return 3.0;
    	}
    	return 0.01;
    }

    private FrequencyMatrix getFrequencyMatrixFromCisBp(DataCollection<FrequencyMatrix> matrixLibrary, DataElementPath pathToFolderWithCisBpMatrices, String cisBpMatrixName)
    {
    	DataElementPath pathToMatrix = pathToFolderWithCisBpMatrices.getChildPath(cisBpMatrixName);
    	DataMatrix dataMatrix = new DataMatrix(pathToMatrix, new String[]{"A", "C", "G", "T"});
    	log.info("cisBpMatrixName = " + cisBpMatrixName);
    	DataMatrix.printDataMatrix(dataMatrix);
        FrequencyMatrix frequencyMatrix = FrequencyMatrixUtils.fromDataMatrixToFrequencyMatrix(dataMatrix, matrixLibrary, cisBpMatrixName);
        return frequencyMatrix;
    }

    

    private void calculateAndWriteAucTest()
    {
    	
    	DataElementPath pathToFolderWithFrequencyMatrices = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_03_RAB/MATRIX_03_RAB_IPS_cisbp/Matrices");
    	DataElementPath pathToFolderWithTracksWithMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX_DERIVETION/META_CLUSTERS/Drosophila/FPCM_3.0_treated");
    	DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblFruitfly91/Sequences/chromosomes BDGP6");
    	
    	
    	DataElementPath pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_03_RAB/MATRIX_03_RAB_IPS_cisbp/AUC_and_ROC");

    	// String[] uniProtIds = new String[] {"A0A0B4K6W1", "A4V1Y6"};
    	String[] uniProtIds = new String[] {"O62609", "A4V1Y6"};
    	//data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_03_RAB/MATRIX_03_RAB_IPS_cisbp/Matrices/O62609_2_iter_8_revised_3
    	int numberOfSequences = 3000, lengthOfEachSequence = 150;
    	String siteModelType = SiteModelUtils.IPS_MODEL;
    	boolean doRevised = true;


        for( int i = 0; i < uniProtIds.length; i++ )
        {
        	log.info("i = " + i + " uniProtIds[i] = " + uniProtIds[i]);

        	DataElementPath path = pathToFolderWithTracksWithMetaClusters.getChildPath(uniProtIds[i]);
            Sequence[] sequences = EnsemblUtils.getLinearSequencesWithGivenLengthForBestSites(path, numberOfSequences, lengthOfEachSequence, pathToSequences);
        	calculateAucsAndRocCurves(pathToFolderWithFrequencyMatrices, doRevised, sequences, uniProtIds[i], true, siteModelType, pathToOutputFolder, jobControl, 0, 100);
        }

    	
    	


    	
    	
    	
//    	DataElementPath pathToFolderWithFrequencyMatrices = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_RAB_9_Test");
//    	DataElementPath pathToFolderWithTracksWithMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX_DERIVETION/META_CLUSTERS/Drosophila/FPCM_3.0_treated");
//    	DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblFruitfly91/Sequences/chromosomes BDGP6");
//
//    	String uniProtId = "A0A021WW32";
//    	int numberOfSequences = 3000, lengthOfEachSequence = 150;
//    	boolean areBothStrands = true;
//    	String siteModelType = SiteModelUtils.IPS_MODEL;
//    	Integer window = 50;
//    	int maxIterations = 10;
//    	double scoreThreshold = 4.3;
//    	DataCollection<FrequencyMatrix> outputMatrixLibrary = null;
//		try
//		{
//			outputMatrixLibrary = WeightMatrixCollection.createMatrixLibrary(pathToFolderWithFrequencyMatrices, log);
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//        DataElementPath path = pathToFolderWithTracksWithMetaClusters.getChildPath(uniProtId);
//        Sequence[] sequences = EnsemblUtils.getLinearSequencesWithGivenLengthForBestSites(path, numberOfSequences, lengthOfEachSequence, pathToSequences);

    	
//        calculateAndWriteRevisedMatrices(pathToFolderWithFrequencyMatrices, sequences, uniProtId, areBothStrands, siteModelType,
//        								 window, maxIterations, scoreThreshold, outputMatrixLibrary, jobControl, 0, 100);
//        CollectionFactoryUtils.save(outputMatrixLibrary);
    }

    private void calculateAucsAndRocCurves(DataElementPath pathToFolderWithFrequencyMatrices, boolean doRevised, Sequence[] sequences,
    			String uniProtId, boolean areBothStrands, String siteModelType, DataElementPath pathToOutputFolder, AnalysisJobControl jobControl, int from, int to)
    {
    	Integer window = 50;
    	// 1. Calculate matrixNames.
    	String[] allMatrixNames = pathToFolderWithFrequencyMatrices.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
    	List<String> list = new ArrayList<>();
    	for( String name : allMatrixNames )
    		if( name.contains(uniProtId) )
    		{
    			if( doRevised && ! name.contains("revised") ) continue; 
    			if( ! doRevised && name.contains("revised") ) continue;
    			list.add(name);
    		}
    	if( list.isEmpty() ) return;
    	String[] matrixNames = list.toArray(new String[0]);

    	// 2. Read frequency matrices.
    	FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[matrixNames.length];
        for( int i = 0; i < matrixNames.length; i++ )
        {
        	DataElementPath path = pathToFolderWithFrequencyMatrices.getChildPath(matrixNames[i]);
        	frequencyMatrices[i] = path.getDataElement(FrequencyMatrix.class);
        }
        
        String nameTableForAuc = "AUCs_" + uniProtId, nameTableForRocCurve = "_chart_ROC_" + uniProtId;
        RocCurve.getRocCurvesAndAucs(siteModelType, true, frequencyMatrices, window, sequences, pathToOutputFolder, nameTableForAuc, nameTableForRocCurve);
        
        





        
//        int w = 50;
//        //FrequencyMatrix frequencyMatrix = pathToFolderWithSMatrices.getChildPath(matrixName).getDataElement(FrequencyMatrix.class);
//        SiteModel siteModel = SiteModelUtils.createSiteModel(SiteModelUtils.MATCH_MODEL, matrixName, frequencyMatrix, 0.0, null);
//        //String s = SiteModelUtils.MATCH_MODEL;
//        // SiteModel siteModel = pathToFolderWithSMatrices.getChildPath(siteModelName).getDataElement(SiteModel.class);
//        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
//        int lengthOfSequenceRegion = w + siteModel.getLength();
////        Object[] objects = selectBestSites(pathToTrack, numberOfBestSites);
//        double[][] xValuesForCurves = new double[rocCurveNames.length][], yValuesForCurves = new double[rocCurveNames.length][];
//        double[] aucs = new double[rocCurveNames.length];
//        for( int i = 0; i < rocCurveNames.length; i++ )
//        {
////            FunSite[] funSites = allFunSites[i];
////            funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
////            Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
//            RocCurve rocCurve = new RocCurve(smc, sequences, 10, 0);
//            double[][] curve  = rocCurve.getRocCurve();
//            xValuesForCurves[i] = curve[0];
//            yValuesForCurves[i] = curve[1];
//            aucs[i] = rocCurve.getAuc();
//            log.info(i + ") AUC = " + aucs[i]);
//        }
//        DataMatrix dm = new DataMatrix(rocCurveNames, "AUC", aucs);
//        dm.writeDataMatrix(false, pathToOutputFolder, "AUCs_" + tfName, log);
//        Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, rocCurveNames, null, null, null, null, "Specificity", "Sensitivity", true);
//        TableAndFileUtils.addChartToTable("chart with ROC-curve", chart, pathToOutputFolder.getChildPath("_chart_with_ROC_curve_" + tfName));

    }

    private void calculateAndWriteRevisedMatrices(DataElementPath pathToFolderWithFrequencyMatrices, Sequence[] sequences, String uniProtId, boolean areBothStrands, String siteModelType, Integer window, int maxIterations, double scoreThreshold, DataCollection<FrequencyMatrix> outputMatrixLibrary, AnalysisJobControl jobControl, int from, int to)
    {
    	// 1. Calculate matrixNames.
    	String[] allMatrixNames = pathToFolderWithFrequencyMatrices.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
    	List<String> list = new ArrayList<>();
    	for( String name : allMatrixNames )
    		if( name.contains(uniProtId) && ! name.contains("revised") )
    			list.add(name);
    	if( list.isEmpty() ) return;
    	String[] matrixNames = list.toArray(new String[0]);

    	// 2. Read frequency matrices.
    	FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[matrixNames.length];
        for( int i = 0; i < matrixNames.length; i++ )
        {
        	DataElementPath path = pathToFolderWithFrequencyMatrices.getChildPath(matrixNames[i]);
        	frequencyMatrices[i] = path.getDataElement(FrequencyMatrix.class);
        }
        calculateAndWriteMatrices(sequences, areBothStrands, siteModelType, window, frequencyMatrices, maxIterations, scoreThreshold, outputMatrixLibrary, jobControl, 10, 100);
        // CollectionFactoryUtils.save(outputMatrixLib);
    }
 
	// 06.05.22
    private String getConsensusFromCisBpMatrixOrOligs(String typeOfInitialMatrices, DataElementPath pathToTextFileWithCisBpInformation, DataElementPath pathToFolderWithCisBpMatrices, String uniprotId, int lengthOfBestOligonucleotides, Sequence[] sequences)
    {
    	if( typeOfInitialMatrices.equals(TYPES_OF_INITIAL_MATRICES[0]) )
    		return CisBpUtils.getConsensusFromMatrix(pathToTextFileWithCisBpInformation, pathToFolderWithCisBpMatrices, uniprotId);
    	else if( typeOfInitialMatrices.equals(TYPES_OF_INITIAL_MATRICES[1]) )
    	{
        	// TODO: TEST
        	int freqThreshold = 3;
            Object[] objects = OligUtils.selectNotZeroOligsAndSortThem(sequences, lengthOfBestOligonucleotides, freqThreshold);
            String[] oligs = (String[])objects[0];
            int[] frequencies = (int[])objects[1];
            for( int j = 0; j < 100; j++ )
            	log.info(" result: oligs[] = " + oligs[oligs.length - 1 - j] + " frequencies[] = " + frequencies[frequencies.length - 1 - j]);
            
            
            return oligs[oligs.length - 1];
    	}
    	return null;
    }
    
    private void calculateAndWriteMatrices(Sequence[] sequences, boolean areBothStrands, String siteModelType, Integer window, FrequencyMatrix[] inputMatrices, int maxIterations, double scoreThreshold, DataCollection<FrequencyMatrix> outputMatrixLib, AnalysisJobControl jobControl, int from, int to)
    {
    	// 0.
    	int maxNumberOfIterationForMatrixUpdate = 3;
    	
        // 1. Define site models and frequency matrices
        SiteModel[] siteModels = new SiteModel[inputMatrices.length];
        FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[inputMatrices.length];
        for( int i = 0; i < inputMatrices.length; i++ )
        {
            frequencyMatrices[i] = new FrequencyMatrix(outputMatrixLib, inputMatrices[i].getName() + "_revised_3", inputMatrices[i]);
            siteModels[i] = SiteModelUtils.createSiteModel(siteModelType, frequencyMatrices[i].getName(), frequencyMatrices[i], 0.01, window);
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
                        double score = SiteModelUtils.findBestSite(sequences[i], siteModels[j], areBothStrands).getScore();
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
                    if( seqsForAlignment.size() < MIN_CLUSTER_SIZE )
                        siteModels[i] = null;
                    else
                    	FrequencyMatrixUtils.updateMatrix(seqsForAlignment.toArray(new Sequence[0]), frequencyMatrices[i], siteModelType, siteModels[i], maxNumberOfIterationForMatrixUpdate, areBothStrands);
                }
        }
        log.info("Number of actual iterations = " + iteration);

        // 6. Save derived matrices
        for( int i = 0; i < siteModels.length; i++ )
            if( siteModels[i] != null )
                outputMatrixLib.put(frequencyMatrices[i]);
        CollectionFactoryUtils.save(outputMatrixLib);
        if( jobControl != null )
            jobControl.setPreparedness(to);
    }


	// 29.04.22
    // private void calculateAndWriteMatrices(Sequence[] sequences, boolean areBothStrands, String siteModelType, Integer window, FrequencyMatrix freqMatrix, double pvalue, int numberOfIterations, int numberOfCreatingMatrices, String baseNameOfCreatingMatrices, DataCollection<FrequencyMatrix> outputMatrixLibrary, AnalysisJobControl jobControl, int from, int to)
    private void calculateAndWriteMatrices(Sequence[] sequences, boolean areBothStrands, String siteModelType, Integer window, FrequencyMatrix freqMatrix, int numberOfIterations, int maxNumberOfCreatingMatrices, String baseNameOfCreatingMatrices, DataCollection<FrequencyMatrix> outputMatrixLibrary, AnalysisJobControl jobControl, int from, int to)
    {
    	int maxNumberOfIterationForMatrixUpdate = 3;
        int difference = to - from, totalNumberOfSteps = maxNumberOfCreatingMatrices * numberOfIterations;
        Sequence[] seqs = new Sequence[sequences.length];
        for( int i = 0; i < sequences.length; i++ )
        	seqs[i] = sequences[i];

        for( int iMatrix = 1; iMatrix <= maxNumberOfCreatingMatrices; iMatrix++ )
        {
        	Sequence[] seqsForNextMatrix = null;
            double[] maxSiteScores = new double[seqs.length];
            FrequencyMatrix frequencyMatrix = new FrequencyMatrix(outputMatrixLibrary, baseNameOfCreatingMatrices + "_" + iMatrix, freqMatrix);
            FrequencyMatrix matrix = null;
            for( int j = 0; j < numberOfIterations; j++ )
            {
                //log.info("iMatrix = " + iMatrix + " j (for iterations) = " + j);
                if( jobControl != null )
                    jobControl.setPreparedness(from + ((iMatrix - 1) * numberOfIterations + j + 1) * difference / totalNumberOfSteps);
                SiteModel siteModel = SiteModelUtils.createSiteModel(siteModelType, frequencyMatrix.getName(), frequencyMatrix, 0.01, window);
                for( int i = 0; i < seqs.length; i++ )
                	maxSiteScores[i] = SiteModelUtils.findBestSite(seqs[i], siteModel, areBothStrands).getScore();
                NormalMixture nm = new NormalMixture(maxSiteScores, 2, null, null, 10);
                DataMatrix parametersOfComponents = nm.getParametersOfComponents();
                int numberOfRealComponents = parametersOfComponents.getSize();
                Sequence[] seqsForAlignment = null;
                if( numberOfRealComponents == 1 )
                {
                	seqsForAlignment = seqs;
                	seqsForNextMatrix = null;
                }
                else
                {
                    DataMatrixInteger datMatInt = nm.getMembershipIndices();
                	int[] membershipIndices = datMatInt.getColumn(0);
                	double[] meanValuesOfComponents = parametersOfComponents.getColumn("Mean value");
                	int indexOfMax = meanValuesOfComponents[0] > meanValuesOfComponents[1] ? 0 : 1;
                	List<Sequence> list = new ArrayList<>(), listForNextMatrix = new ArrayList<>();
                	for( int i = 0; i < membershipIndices.length; i++ )
                		if( membershipIndices[i] == indexOfMax )
                			list.add(seqs[i]);
                		else
                			listForNextMatrix.add(seqs[i]);
                	seqsForAlignment = list.toArray(new Sequence[0]);
                	seqsForNextMatrix = listForNextMatrix.toArray(new Sequence[0]);
                }
                
                // Update FrequencyMatrix
                log.info("***** Matrix = " + iMatrix + " iteration j = " + (j + 1) + "; size of subsample of actual sites = " + seqsForAlignment.length + " (" + (100.0 * seqsForAlignment.length) / sequences.length + "%)");
                FrequencyMatrixUtils.updateMatrix(seqsForAlignment, frequencyMatrix, siteModelType, siteModel, maxNumberOfIterationForMatrixUpdate, areBothStrands);
                matrix = new FrequencyMatrix(outputMatrixLibrary, baseNameOfCreatingMatrices + "_" + iMatrix + "_iter_" + (j + 1), frequencyMatrix);
                FrequencyMatrixUtils.printFrequencyMatrixAndConsensus(matrix);
                if( j == numberOfIterations - 1 )
                {
                    log.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxx Output matrix : iMatrix = " + iMatrix + " j = " + j + " dim(seqs) = " + seqs.length + " dim(seqsForNextMatrix) = " + seqsForNextMatrix.length);
                    outputMatrixLibrary.put(matrix);
                }
            }
        	seqs = seqsForNextMatrix;
            /*******************/
            log.info("seqsForNextMatrix.length = " + seqsForNextMatrix.length + " minClusterSize = " + MIN_CLUSTER_SIZE);
            /*******************/
            if( seqsForNextMatrix.length < MIN_CLUSTER_SIZE ) return;
        }
        
        // 21.04.22
        // CollectionFactory.save(outputMatrixLibrary);
    }
    
    public static class NormalMixtureParameters extends AbstractAnalysisParameters
    {
    	// 29.04.22
    	// private double pValue = 0.05;
        // private int maxNumberOfCreatingMatrices = 5;
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
        
        // 29.04.22
//        @PropertyName(MessageBundle.PN_P_VALUE_THRESHOLD)
//        @PropertyDescription(MessageBundle.PD_P_VALUE_THRESHOLD)
//        public double getPValue()
//        {
//            return pValue;
//        }
//        public void setPValue(double pValue)
//        {
//            Object oldValue = this.pValue;
//            this.pValue = pValue;
//            firePropertyChange("pValue", oldValue, pValue);
//        }

//        private int maxNumberOfCreatingMatrices = 5;
//
//        @PropertyName(MessageBundle.PD_MAX_NUMBER_OF_CREATING_MATRICES)
//        @PropertyDescription(MessageBundle.PN_MAX_NUMBER_OF_CREATING_MATRICES)
//        public int getMaxNumberOfCreatingMatrices()
//        {
//            return maxNumberOfCreatingMatrices;
//        }
//        public void setMaxNumberOfMatrices(int maxNumberOfCreatingMatrices)
//        {
//            Object oldValue = this.maxNumberOfCreatingMatrices;
//            this.maxNumberOfCreatingMatrices = maxNumberOfCreatingMatrices;
//            firePropertyChange("maxNumberOfCreatingMatrices", oldValue, maxNumberOfCreatingMatrices);
//        }
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
        	// 29.04.22
            // add("pValue");
            add("numberOfMatrices");
            add("initialApproximationForMatrix");
        }
    }

    // 29.04.22
    public static class LibraryDerivationParameters extends AbstractAnalysisParameters
    {
        private DataElementPath pathToTextFileWithCisBpInformation;
        private DataElementPath pathToFolderWithCisBpMatrices;
        private int lengthOfBestOligonucleotides = 3;
        private int numberOfBestOligonucleotides = 3000;
        private String typeOfInitialMatrices = TYPES_OF_INITIAL_MATRICES[0];

        
        @PropertyName(MessageBundle.PN_NUMBER_OF_BEST_OLIGONUCLEOTIDES)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_BEST_OLIGONUCLEOTIDES)
        public int getNumberOfBestOligonucleotides()
        {
            return numberOfBestOligonucleotides;
        }
        public void setNumberOfBestOligonucleotides(int numberOfBestOligonucleotides)
        {
            Object oldValue = this.numberOfBestOligonucleotides;
            this.numberOfBestOligonucleotides = numberOfBestOligonucleotides;
            firePropertyChange("numberOfBestOligonucleotides", oldValue, numberOfBestOligonucleotides);
        }

        @PropertyName(MessageBundle.PN_LENGTH_OF_BEST_OLIGONUCLEOTIDES)
        @PropertyDescription(MessageBundle.PD_LENGTH_OF_BEST_OLIGONUCLEOTIDES)
        public int getLengthOfBestOligonucleotides()
        {
            return lengthOfBestOligonucleotides;
        }
        public void setLengthOfBestOligonucleotides(int lengthOfBestOligonucleotides)
        {
            Object oldValue = this.lengthOfBestOligonucleotides;
            this.lengthOfBestOligonucleotides = lengthOfBestOligonucleotides;
            firePropertyChange("lengthOfBestOligonucleotides", oldValue, lengthOfBestOligonucleotides);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_TEXT_FILE_WITH_CIS_BP_INFORMATION)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TEXT_FILE_WITH_CIS_BP_INFORMATION)
        public DataElementPath getPathToTextFileWithCisBpInformation()
        {
            return pathToTextFileWithCisBpInformation;
        }
        public void setPathToTextFileWithCisBpInformation(DataElementPath pathToTextFileWithCisBpInformation)
        {
            Object oldValue = this.pathToTextFileWithCisBpInformation;
            this.pathToTextFileWithCisBpInformation = pathToTextFileWithCisBpInformation;
            firePropertyChange("pathToTextFileWithCisBpInformation", oldValue, pathToTextFileWithCisBpInformation);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_CISBP_MATRICES)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_CISBP_MATRICES)
        public DataElementPath getPathToFolderWithCisBpMatrices()
        {
            return pathToFolderWithCisBpMatrices;
        }
        public void setPathToFolderWithCisBpMatrices(DataElementPath pathToFolderWithCisBpMatrices)
        {
            Object oldValue = this.pathToFolderWithCisBpMatrices;
            this.pathToFolderWithCisBpMatrices = pathToFolderWithCisBpMatrices;
            firePropertyChange("pathToFolderWithCisBpMatrices", oldValue, pathToFolderWithCisBpMatrices);
        }
        
        //////// new 05.05.22
        @PropertyName(MessageBundle.PN_TYPE_OF_INITIAL_MATRICES)
        @PropertyDescription(MessageBundle.PD_TYPE_OF_INITIAL_MATRICES)
        public String getTypeOfInitialMatrices()
        {
            return typeOfInitialMatrices;
        }
        public void setTypeOfInitialMatrices(String typeOfInitialMatrices)
        {
            Object oldValue = this.typeOfInitialMatrices;
            this.typeOfInitialMatrices = typeOfInitialMatrices;
            //firePropertyChange("typeOfInitialMatrices", oldValue, typeOfInitialMatrices);
            firePropertyChange("*", oldValue, typeOfInitialMatrices);
        }
        
        public boolean areOligonucleotidesHidden()
        {
        	return ! getTypeOfInitialMatrices().equals(TYPES_OF_INITIAL_MATRICES[1]);
        }
        
        public boolean areCisBpHidden()
        {
        	return ! getTypeOfInitialMatrices().equals(TYPES_OF_INITIAL_MATRICES[0]);
        }
    }
    
    public static class LibraryDerivationParametersBeanInfo extends BeanInfoEx2<LibraryDerivationParameters>
    {
        public LibraryDerivationParametersBeanInfo()
        {
            super(LibraryDerivationParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            
            //////// new 05.05.22
            add(new PropertyDescriptorEx("typeOfInitialMatrices", beanClass), TypeOfInitialMatricesSelector.class);

            //////// new 05.05.22
            // add(DataElementPathEditor.registerInput("pathToTextFileWithCisBpInformation", beanClass, FileDataElement.class, true));
            // add(DataElementPathEditor.registerInputChild("pathToFolderWithCisBpMatrices", beanClass, DataCollection.class, true));
            // add("lengthOfBestOligonucleotides");
            // add("numberOfBestOligonucleotides");

            addHidden(DataElementPathEditor.registerInput("pathToTextFileWithCisBpInformation", beanClass, FileDataElement.class, true), "areCisBpHidden");
            addHidden(DataElementPathEditor.registerInputChild("pathToFolderWithCisBpMatrices", beanClass, DataCollection.class, true), "areCisBpHidden");
            addHidden("lengthOfBestOligonucleotides", "areOligonucleotidesHidden");
            addHidden("numberOfBestOligonucleotides", "areOligonucleotidesHidden");
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

    public static class MatrixDerivationParameters extends AbstractFiveSiteModelsParameters
    {
        private NormalMixtureParameters normalMixtureParameters;
        private EmSeveralMatricesParameters emSeveralMatricesParameters;
        // 29.04.22
        private LibraryDerivationParameters libraryDerivationParameters;
        private String nameOfAlgorithm = SUCCESSIVE_NORMAL_MIXTURES;
        private int lengthOfEachSequence = 150;
        private int numberOfSequences = 3000;
        private int maxIterations = 15;
        private DataElementPath pathToInputFolder;
        // private String typeOfInitialMatrices = TYPES_OF_INITIAL_MATRICES[0];
        private int maxNumberOfCreatingMatrices = 5;

        @PropertyName(MessageBundle.PN_MAX_NUMBER_OF_CREATING_MATRICES)
        @PropertyDescription(MessageBundle.PD_MAX_NUMBER_OF_CREATING_MATRICES)
        public int getMaxNumberOfCreatingMatrices()
        {
            return maxNumberOfCreatingMatrices;
        }
        public void setMaxNumberOfCreatingMatrices(int maxNumberOfCreatingMatrices)
        {
            Object oldValue = this.maxNumberOfCreatingMatrices;
            this.maxNumberOfCreatingMatrices = maxNumberOfCreatingMatrices;
            firePropertyChange("maxNumberOfCreatingMatrices", oldValue, maxNumberOfCreatingMatrices);
        }

//        @PropertyName(MessageBundle.PN_TYPE_OF_INITIAL_MATRICES)
//        @PropertyDescription(MessageBundle.PD_TYPE_OF_INITIAL_MATRICES)
//        public String getTypeOfInitialMatrices()
//        {
//            return typeOfInitialMatrices;
//        }
//        public void setTypeOfInitialMatrices(String typeOfInitialMatrices)
//        {
//            Object oldValue = this.typeOfInitialMatrices;
//            this.typeOfInitialMatrices = typeOfInitialMatrices;
//            firePropertyChange("typeOfInitialMatrices", oldValue, typeOfInitialMatrices);
//        }

        public MatrixDerivationParameters()
        {
            setNormalMixtureParameters(new NormalMixtureParameters());
            setEmSeveralMatricesParameters(new EmSeveralMatricesParameters());
            // 29.04.22
            setLibraryDerivationParameters(new LibraryDerivationParameters());
        }
        
        @PropertyName(MessageBundle.PN_LENGTH_OF_EACH_SEQUENCE)
        @PropertyDescription(MessageBundle.PD_LENGTH_OF_EACH_SEQUENCE)
        public int getLengthOfEachSequence()
        {
            return lengthOfEachSequence;
        }
        public void setLengthOfEachSequence(int lengthOfEachSequence)
        {
            Object oldValue = this.lengthOfEachSequence;
            this.lengthOfEachSequence = lengthOfEachSequence;
            firePropertyChange("lengthOfEachSequence", oldValue, lengthOfEachSequence);
        }

        @PropertyName(MessageBundle.PN_NUMBER_OF_SEQUENCES)
        @PropertyDescription(MessageBundle.PD_NUMBER_OF_SEQUENCES)
        public int getNumberOfSequences()
        {
            return numberOfSequences;
        }
        public void setNumberOfSequences(int numberOfSequences)
        {
            Object oldValue = this.numberOfSequences;
            this.numberOfSequences = numberOfSequences;
            firePropertyChange("numberOfSequences", oldValue, numberOfSequences);
        }
        
        // 29.04.22
        @PropertyName("Parameters specific for '" + LIBRARY_CONSTRUCTION + "'")
        public LibraryDerivationParameters getLibraryDerivationParameters()
        {
            return libraryDerivationParameters;
        }
        public void setLibraryDerivationParameters(LibraryDerivationParameters libraryDerivationParameters)
        {
            Object oldValue = this.libraryDerivationParameters;
            this.libraryDerivationParameters = withPropagation(this.libraryDerivationParameters, libraryDerivationParameters);
            firePropertyChange("libraryDerivationParameters", oldValue, libraryDerivationParameters);
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_INPUT_FOLDER)
        @PropertyDescription(MessageBundle.PD_PATH_TO_INPUT_FOLDER)
        public DataElementPath getPathToInputFolder()
        {
            return pathToInputFolder;
        }
        public void setPathToInputFolder(DataElementPath pathToInputFolder)
        {
            Object oldValue = this.pathToInputFolder;
            this.pathToInputFolder = pathToInputFolder;
            firePropertyChange("pathToInputFolder", oldValue, pathToInputFolder);
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
        
        // 29.04.22
        public boolean areLibraryDerivationParametersHidden()
        {
            return ! getNameOfAlgorithm().equals(LIBRARY_CONSTRUCTION);
        }
        
        // 27.04.22
        public boolean isTrackPathHidden()
        {
        	return getNameOfAlgorithm().equals(LIBRARY_CONSTRUCTION);
        }
        
        // 27.04.22
        public boolean isPathToInputFolderHidden()
        {
        	return ! getNameOfAlgorithm().equals(LIBRARY_CONSTRUCTION);
        }
        
        public boolean isTypeOfInitialMatricesHidden()
        {
        	return ! getNameOfAlgorithm().equals(LIBRARY_CONSTRUCTION);
        }
        
        public boolean isMaxNumberOfCreatingMatricesHidden()
        {
        	return getNameOfAlgorithm().equals(EM_FOR_SEVERAL_MATRICES);
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
            return new String[]{SUCCESSIVE_NORMAL_MIXTURES, EM_FOR_SEVERAL_MATRICES, LIBRARY_CONSTRUCTION};
        }
    }
    
    public static class TypeOfInitialMatricesSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return TYPES_OF_INITIAL_MATRICES;
        }
    }
    
    public static class MatrixDerivationParametersBeanInfo extends BeanInfoEx2<MatrixDerivationParameters>
    {
        public MatrixDerivationParametersBeanInfo()
        {
            super(MatrixDerivationParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("nameOfAlgorithm", beanClass), AlgorithmSelector.class);
            add("dbSelector");
            add("lengthOfEachSequence");
            add("numberOfSequences");
            // addHidden("typeOfInitialMatrices", "isTypeOfInitialMatricesHidden");
            // addHidden(new PropertyDescriptorEx("typeOfInitialMatrices", beanClass), TypeOfInitialMatricesSelector.class, "isTypeOfInitialMatricesHidden");
            addHidden("maxNumberOfCreatingMatrices", "isMaxNumberOfCreatingMatricesHidden");

            
            //27.04.22
            //add(DataElementPathEditor.registerInputChild("pathToInputFolder", beanClass, DataCollection.class, true));
            addHidden(DataElementPathEditor.registerInputChild("pathToInputFolder", beanClass, DataCollection.class, true), "isPathToInputFolderHidden");
            
            add(new PropertyDescriptorEx("siteModelType", beanClass), SiteModelTypeEditor.class);
            addHidden("window", "isWindowHidden");
            add("areBothStrands");
            //property("trackPath").inputElement(Track.class).add();
            //27.04.22
            //add(DataElementPathEditor.registerInput("trackPath", beanClass, Track.class));
            addHidden(DataElementPathEditor.registerInput("trackPath", beanClass, Track.class), "isTrackPathHidden");

            addHidden("normalMixtureParameters", "areNormalMixtureParametersHidden");
            addHidden("emSeveralMatricesParameters", "areEmSeveralMatricesParametersHidden");
            addHidden("libraryDerivationParameters", "areLibraryDerivationParametersHidden");
            
            add("maxIterations");
            
            // 29.04.22
            // add(DataElementPathEditor.registerInput("pathToTextFileWithCisBpInformation", beanClass, FileDataElement.class, true));
            // add(DataElementPathEditor.registerInputChild("pathToFolderWithCisBpMatrices", beanClass, DataCollection.class, true));

            // add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, WeightMatrixCollection.class), ""));
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, false));

        }
    }
}
