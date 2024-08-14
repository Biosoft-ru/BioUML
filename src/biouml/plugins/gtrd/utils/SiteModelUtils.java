/* $Id$ */
// 09.06.22
package biouml.plugins.gtrd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixStringConstructor;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.MathUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.LogIPSSiteModel;
import ru.biosoft.bsa.analysis.LogWeightMatrixModelWithModeratePseudocounts;
import ru.biosoft.bsa.analysis.MatchSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 *
 */
public class SiteModelUtils
{
    public static final String WEIGHT_MATRIX_MODEL = "Common additive model";
    public static final String LOG_WEIGHT_MATRIX_MODEL = "Common multiplicative model";
    public static final String MATCH_MODEL = "MATCH model";
    public static final String IPS_MODEL = "IPS model";
    public static final String LOG_IPS_MODEL = "Multiplicative IPS model";

    public static SiteModel createSiteModel(String modelType, String modelName, FrequencyMatrix frequencyMatrix, double threshold, Integer window)
    {
        switch( modelType )
        {
            case MATCH_MODEL             : return new MatchSiteModel(modelName, null, frequencyMatrix, threshold, 0.0);
            case WEIGHT_MATRIX_MODEL     : return new WeightMatrixModel(modelName, null, frequencyMatrix, threshold);
            case IPS_MODEL               : int actualWindow = window == null ? IPSSiteModel.DEFAULT_WINDOW : window;
                                           return new IPSSiteModel(modelName, null, new FrequencyMatrix[]{frequencyMatrix}, threshold, -1, actualWindow);
            case LOG_WEIGHT_MATRIX_MODEL : new LogWeightMatrixModelWithModeratePseudocounts(modelName, null, frequencyMatrix, threshold);
            case LOG_IPS_MODEL           : actualWindow = window == null ? IPSSiteModel.DEFAULT_WINDOW : window;
                                           WeightMatrixModel[] weightMatrixModels = new WeightMatrixModel[]{new LogWeightMatrixModelWithModeratePseudocounts(modelName, null, frequencyMatrix, -Double.MAX_VALUE)};
                                           return new LogIPSSiteModel(modelName, null, weightMatrixModels, threshold, actualWindow, LogIPSSiteModel.DEFAULT_MULTIPLIER);
        }
        return null;
    }
    
    public static String[] getAvailableSiteModelTypes()
    {
        return new String[]{IPS_MODEL, LOG_IPS_MODEL, WEIGHT_MATRIX_MODEL, LOG_WEIGHT_MATRIX_MODEL, MATCH_MODEL};
    }
    
    public static DataMatrixString getSiteModelNamesAndUniprotIds(DataElementPath pathToFolderWithSiteModels)
    {
        DataCollection<DataElement> dc = pathToFolderWithSiteModels.getDataCollection(DataElement.class);
        SiteModel[] siteModels = new SiteModel[dc.getSize()];
        int index = 0;
        for( ru.biosoft.access.core.DataElement de : dc )
            siteModels[index++] = (SiteModel)de;
        return getSiteModelNamesAndUniprotIds(siteModels);
    }
    
    public static DataMatrixString getSiteModelNamesAndUniprotIds(SiteModel[] siteModels)
    {
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"uniprot_ID"});
        for( SiteModel siteModel : siteModels )
            dmsc.addRow(siteModel.getName(), new String[]{siteModel.getBindingElement().getFactors()[0].getName()});
        return dmsc.getDataMatrixString();
    }
    
    // 12.03.22 new method
    public static Site findBestSite(Sequence sequence, SiteModel siteModel, boolean areBothStrands)
    {
        Site bestSiteInPositiveStrand = siteModel.findBestSite(sequence);
    	if( ! areBothStrands ) return bestSiteInPositiveStrand;
        Site bestSiteInReversedStrand = siteModel.findBestSite(SequenceRegion.getReversedSequence(sequence));
        return bestSiteInPositiveStrand.getScore() >= bestSiteInReversedStrand.getScore() ? bestSiteInPositiveStrand : bestSiteInReversedStrand;
    }
    
    public static Integer getWindow(SiteModel siteModel)
    {
        switch( siteModel.getName() )
        {
            case IPS_MODEL     : return ((IPSSiteModel)siteModel).getWindow();
            case LOG_IPS_MODEL : return ((LogIPSSiteModel)siteModel).getWindow();
            default            : return null;
        }
    }

    /************************ RocCurve : start *********************/
    public static class RocCurve
    {
        double[][] rocCurve; // rocCurve : rocCurve[0] = double[] fdrs (or specificities), rocCurve[1] = double[] sensitivities;
        SiteModelComposed siteModelComposed;
        
        public RocCurve(SiteModelComposed siteModelComposed, Sequence[] sequences, int numberOfPermutations, long seed)
        {
            this.siteModelComposed = siteModelComposed;
            Sequence[] sequenceClones = new Sequence[sequences.length];
            for( int i = 0; i < sequences.length; i++ )
                sequenceClones[i] = new LinearSequence(sequences[i]);
            double[] scores = siteModelComposed.findBestScores(sequenceClones);
            scores = ArrayUtils.addAll(scores, new double[]{-Double.MAX_VALUE, Double.MAX_VALUE});
            double[] thresholds = UtilsGeneral.getDistinctValues(scores);
            UtilsForArray.sortInAscendingOrder(thresholds);
            double[] sensitivities = getSensitivities(scores, thresholds);
            double[] fdrs = getFdrs(sequenceClones, thresholds, numberOfPermutations, new Random(seed));
            rocCurve = MathUtils.recalculateCurve(fdrs, sensitivities);
        }
        
        public double[][] getRocCurve()
        {
            return rocCurve;
        }
        
        // TODO: To optimize the run-time!!!
        /**
         * For each threshold compute fraction of scores that are greater or equal to the threshold.
         * @param scores
         * @param thresholds
         * @return array of the same length as thresholds
         */
        private double[] getSensitivities(double[] scores, double[] thresholds)
        {
            double[] result = new double[thresholds.length];
            for( int i = 0; i < thresholds.length; i++ )
                for( double score : scores )
                    if( score >= thresholds[i] )
                        result[i] += 1.0 / (double)scores.length;
            return result;
        }
        
        private double[] getFdrs(Sequence[] sequences, double[] thresholds, int numberOfPermutations, Random random)
        {
            double[] fdrs = new double[thresholds.length];
            for( int step = 0; step < numberOfPermutations; step++ )
            {
                for(Sequence s : sequences)
                    shuffle(s, random);
                double[] scores = siteModelComposed.findBestScores(sequences);
                double[] sensitivities = getSensitivities(scores, thresholds);
                for( int i = 0; i < sensitivities.length; i++ )
                    fdrs[i] += sensitivities[i];
            }
            fdrs = VectorOperations.getProductOfVectorAndScalar(fdrs, 1.0 / (double)numberOfPermutations);
            return fdrs;
        }
        
        // TODO: To reduce to StatUtils.shuffleVector(indices, seed);
        // Shuffles letters in sequence.
        private void shuffle(Sequence s, Random random)
        {
            for( int i = s.getLength() + s.getStart() - 1; i > s.getStart(); i-- )
            {
                int j = random.nextInt(i + 1 - s.getStart()) + s.getStart();
                byte tmp = s.getLetterAt(i);
                s.setLetterAt(i, s.getLetterAt(j));
                s.setLetterAt(j, tmp);
            }
        }
        
        /***
         * 
         * @param rocCurve : rocCurve[0] = fdrs, rocCurve[1] = sensitivities;
         * @return AUC-value (Area Under Curve)
         */
        public double getAuc()
        {
            double[] fdrs = rocCurve[0], sensitivities = rocCurve[1];
            double result = 0.0;
            for( int i = 0; i < fdrs.length - 1; i++ )
                result += (fdrs[i + 1] - fdrs[i]) * (sensitivities[i] + sensitivities[i + 1]) / 2.0;
            return result;
        }

        // 14.03.22 new
        // It is modification of function from QualityControlAnalysis
        // ROC-curves are calculated for each FrequencyMatrix separately and for all FrequencyMatrix[].
        public static void getRocCurvesAndAucs(String siteModelType, boolean areBothStrands, FrequencyMatrix[] frequencyMatrices, Integer window, Sequence[] sequences, DataElementPath pathToOutputFolder, String nameTableForAuc, String nameTableForRocCurve)
        {
        	SiteModelComposed[] smcs = new SiteModelComposed[frequencyMatrices.length + 1];
        	
        	// 1. Create siteModels and siteModelNames.
        	SiteModel[] siteModels = new SiteModel[frequencyMatrices.length];
        	String[] siteModelNames = new String[frequencyMatrices.length];
            for( int i = 0; i < frequencyMatrices.length; i++ )
            {
            	siteModelNames[i] = frequencyMatrices[i].getName();
            	siteModels[i] = SiteModelUtils.createSiteModel(siteModelType, siteModelNames[i], frequencyMatrices[i], 0.01, window);
            }
            
            // 2. Create smcs
            for( int i = 0; i < frequencyMatrices.length; i++ )
            {
            	String name = siteModels[i].getName();
            	smcs[i] = new SiteModelComposed(new SiteModel[]{siteModels[i]}, new String[]{siteModelNames[i]}, name, areBothStrands);
            }
            smcs[frequencyMatrices.length] = new SiteModelComposed(siteModels, siteModelNames, "All matrices", areBothStrands);
            
            //3.
            getRocCurvesAndAucs(smcs, sequences, pathToOutputFolder, nameTableForAuc, nameTableForRocCurve);
        }
        
        public static void getRocCurvesAndAucs(String siteModelType, boolean areBothStrands, FrequencyMatrix[] frequencyMatrices, Integer window, Sequence[] sequences, DataElementPath pathToOutputFolder)
        {
        	getRocCurvesAndAucs(siteModelType, areBothStrands, frequencyMatrices, window, sequences, pathToOutputFolder, "AUCs", "_chart_with_ROC_curve");
        }

        // 14.03.22 new
        private static void getRocCurvesAndAucs(SiteModelComposed[] smcs, Sequence[] sequences, DataElementPath pathToOutputFolder, String nameTableForAuc, String nameTableForRocCurve)
        {
        	// 1. Calculate ROC-curves and AUCs.
            double[][] xValuesForCurves = new double[smcs.length][], yValuesForCurves = new double[smcs.length][];
            double[] aucs = new double[smcs.length];
            for( int i = 0; i < smcs.length; i++ )
            {
                RocCurve rocCurve = new RocCurve(smcs[i], sequences, 10, 0);
                double[][] curve  = rocCurve.getRocCurve();
                xValuesForCurves[i] = curve[0];
                yValuesForCurves[i] = curve[1];
                aucs[i] = rocCurve.getAuc();
                log.info("Site model[" + i + "] = " + smcs[i].getSiteModelName() + " AUC = " + aucs[i]);
            }
            //"_chart_with_ROC_curve"
            // 2. Write AUCs and chart with ROC-curves.
            String[] rowNames = new String[smcs.length];
            for( int i = 0; i < smcs.length; i++ )
            	rowNames[i] = smcs[i].getSiteModelName();
            DataMatrix dm = new DataMatrix(rowNames, "AUC", aucs);
            dm.writeDataMatrix(false, pathToOutputFolder, nameTableForAuc, log);
            Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, rowNames, null, null, null, null, "Specificity", "Sensitivity", true);
            TableAndFileUtils.addChartToTable("chart with ROC-curve", chart, pathToOutputFolder.getChildPath(nameTableForRocCurve));
        }
    }
    /****************************** RocCurve : end **************************/

    /********************* FrequencyMatrixUtils : start *********************/
    public static class FrequencyMatrixUtils
    {
    	// new 18.03.22
        public static void updateMatrix(Sequence[] sequences, FrequencyMatrix frequencyMatrix, String siteModelType, SiteModel siteModel, int maxIterations, boolean areBothStrands)
        {
            Integer window = SiteModelUtils.getWindow(siteModel);
            SiteModel newSiteModel = siteModel;
            for( int iter = 0; iter < maxIterations; iter++ )
            {
                List<Sequence> siteSequences = new ArrayList<>();
                for( Sequence seq : sequences )
                	siteSequences.add(SiteModelUtils.findBestSite(seq, newSiteModel, areBothStrands).getSequence());
                frequencyMatrix.updateFromSequences(siteSequences);
                newSiteModel = SiteModelUtils.createSiteModel(siteModelType, siteModel.getName(), frequencyMatrix, 0.01, window);
            }
        }

        // TODO to remove
        // 19.03.22 Temporary test
//        public static void test(Sequence[] sequences, FrequencyMatrix frequencyMatrix, String siteModelType, SiteModel siteModel, int maxIterations, boolean areBothStrands)
//        {
//            int i = 0;
//            for( Sequence seq : sequences )
//            {
//            	Sequence bestSite = findBestSite(seq, areBothStrands, siteModel).getSequence();
//                SequenceRegion reverseSeq = SequenceRegion.getReversedSequence(seq);
//            	boolean strandPlus = seq.toString().contains(bestSite.toString());
//            	boolean strandMinus = reverseSeq.toString().contains(bestSite.toString());
//            	log.info(" ---- TEST for updateMatrix -----");
//            	log.info(" i = " + i + " best site = " + bestSite.toString() + " strandPlus + " + strandPlus + " strandMinus = " + strandMinus);
//            	log.info(" i = " + i + " seq = " + seq.toString());
//            	log.info(" i = " + i + "reverseSeq = " + reverseSeq.toString());
//            	if( ++ i > 9 ) break;
//            }
//        }
        
        public static void printFrequencyMatrixAndConsensus(FrequencyMatrix frequencyMatrix)
        {
        	//frequencyMatrix.getFrequency(position, letterCode)
        	int n = frequencyMatrix.getLength();
        	String[] rowNames = new String[]{"T", "A", "G", "C"}, columnNames = new String[n];
        	String consensus = "";
            for( int i = 0; i < n; i ++ )
            	columnNames[i] = Integer.toString(i);
            double[][] matrix = new double[4][frequencyMatrix.getLength()];
            byte[] letterToCodeMatrix = frequencyMatrix.getAlphabet().letterToCodeMatrix();
            for( int i = 0; i < n; i ++ )
            {
                matrix[0][i] = frequencyMatrix.getFrequency(i, letterToCodeMatrix['T']);
                matrix[1][i] = frequencyMatrix.getFrequency(i, letterToCodeMatrix['A']);
                matrix[2][i] = frequencyMatrix.getFrequency(i, letterToCodeMatrix['G']);
                matrix[3][i] = frequencyMatrix.getFrequency(i, letterToCodeMatrix['C']);
            }

            for( int i = 0; i < n; i ++ )
            {
            	Object[] objects = PrimitiveOperations.getMax(MatrixUtils.getColumn(matrix, i));
            	double max = (double)objects[1];
            	if( max < 0.4 )
            		consensus += 'N';
            	else
            		consensus += rowNames[(int)objects[0]];
            }

            log.info("Frequency matrix = ");
            DataMatrix.printDataMatrix(new DataMatrix(rowNames, columnNames, matrix));
            log.info("Consensus = " + consensus);
        }
        
        // old (incorrect) version
        // DataMatrix dataMatrix contains frequencies for columns {"A", "C", "G", "T"}
//        public static FrequencyMatrix fromDataMatrixToFrequencyMatrix(DataMatrix dataMatrix, DataCollection<?> origin, String name)
//        {
//        	// String consensusTest = "AaTtGgCc";
//            Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();
//            
//            String[] columnNames = dataMatrix.getColumnNames();
//            //byte[] consensusBytes = consensus.getBytes();
//            String str = "";
//            for( int i = 0; i < columnNames.length; i++ ) str += columnNames[i];
//            byte[] nucleotideBytes = str.getBytes();
//
//            double[][] mat = dataMatrix.getMatrix();
//            
//            
//            
//            
//            ///// print nucleotideBytes
////            String s = "nucleotideBytes = ";
////            for( int i = 0; i < nucleotideBytes.length; i++ ) s += " " + nucleotideBytes[i];
////    		log.info(s);
//    		//////////
//
//
////          double[][] matrix = new double[consensusBytes.length - alphabet.codeLength() + 1][alphabet.size()];
//            double[][] matrix = new double[mat.length - alphabet.codeLength() + 1][alphabet.size()];
//            
//            
//            /**************************/
//            log.info("************ Test:   within fromDataMatrixToFrequencyMatrix");
//            DataMatrix.printDataMatrix(dataMatrix);
//            String s = "nucleotideBytes = ";
//            for( int i = 0; i < nucleotideBytes.length; i++ ) s += " " + nucleotideBytes[i];
//            log.info(s);
//            int ii = mat.length - alphabet.codeLength() + 1, jj = alphabet.size();
//            log.info("dim(matrix) = " + ii + " x " + jj);
//            
//            log.info("Test test");
//            for( int i = 0; i < matrix.length; i++ )
//            {
//                byte code = alphabet.lettersToCode(nucleotideBytes, i); // Error because  conflict : (i <= matrix.length - 1)  dim(nucleotideBytes) = 4;
//                log.info("i = " + i + " code = " + code);
//
//                if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR )
//            		log.info("--- nucleotideBytes[i] " + nucleotideBytes[i] + " contains invalid character at position " + i + "");
//                for( byte basicCode : alphabet.basicCodes(code) )
//                {
//            		log.info(" Position = " + i + " code = " + code + " basicCode = " + basicCode);
//                	matrix[i][basicCode] = mat[i][basicCode];
//                }
//            }
//
//            /*********************/
//
//
//            
//            for( int i = 0; i < mat.length; i++ )
//                for( int j = 0; j < nucleotideBytes.length; j++ )
//                {
//                    byte code = alphabet.lettersToCode(nucleotideBytes, j);
//                    if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR )
//                		log.info("--- nucleotideBytes " + nucleotideBytes + " contains invalid character at position " + i + " and " + j);
//                	matrix[i][code] = mat[i][j];
//                }
//            return new FrequencyMatrix(origin, name, alphabet, null, matrix, false);
//        }

        // new version
     // DataMatrix dataMatrix contains frequencies for columns {"A", "C", "G", "T"}
        public static FrequencyMatrix fromDataMatrixToFrequencyMatrix(DataMatrix dataMatrix, DataCollection<?> origin, String name)
        {
        	// String consensusTest = "AaTtGgCc";
            Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();
            
            String[] columnNames = dataMatrix.getColumnNames();
            //byte[] consensusBytes = consensus.getBytes();
            String str = "";
            for( int i = 0; i < columnNames.length; i++ ) str += columnNames[i];
            byte[] nucleotideBytes = str.getBytes();

            double[][] mat = dataMatrix.getMatrix();
            
            
            
            
            ///// print nucleotideBytes
//            String s = "nucleotideBytes = ";
//            for( int i = 0; i < nucleotideBytes.length; i++ ) s += " " + nucleotideBytes[i];
//    		log.info(s);
    		//////////


//          double[][] matrix = new double[consensusBytes.length - alphabet.codeLength() + 1][alphabet.size()];
            double[][] matrix = new double[mat.length - alphabet.codeLength() + 1][alphabet.size()];
    		log.info("--- fromDataMatrixToFrequencyMatrix : dim = " + (mat.length - alphabet.codeLength() + 1) + " x " + alphabet.size());

            
            /**************************/
            log.info("************ Test:   within fromDataMatrixToFrequencyMatrix");
            DataMatrix.printDataMatrix(dataMatrix);
            String s = "nucleotideBytes = ";
            for( int i = 0; i < nucleotideBytes.length; i++ ) s += " " + nucleotideBytes[i];
            log.info(s);
            int ii = mat.length - alphabet.codeLength() + 1, jj = alphabet.size();
            log.info("dim(matrix) = " + ii + " x " + jj);
            
            log.info("Test test");
            for( int i = 0; i < matrix.length; i++ )
            {
                byte code = alphabet.lettersToCode(nucleotideBytes, i); // Error because  conflict : (i <= matrix.length - 1)  dim(nucleotideBytes) = 4;
                log.info("i = " + i + " code = " + code);

                if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR )
            		log.info("--- nucleotideBytes[i] " + nucleotideBytes[i] + " contains invalid character at position " + i + "");
                for( byte basicCode : alphabet.basicCodes(code) )
                {
            		log.info(" Position = " + i + " code = " + code + " basicCode = " + basicCode);
                	matrix[i][basicCode] = mat[i][basicCode];
                }
            }

            /*********************/


            
            for( int i = 0; i < mat.length; i++ )
                for( int j = 0; j < nucleotideBytes.length; j++ )
                {
                    byte code = alphabet.lettersToCode(nucleotideBytes, j);
                    if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR )
                		log.info("--- nucleotideBytes " + nucleotideBytes + " contains invalid character at position " + i + " and " + j);
                	matrix[i][code] = mat[i][j];
                }
            return new FrequencyMatrix(origin, name, alphabet, null, matrix, false);
        }
        
        public static FrequencyMatrix consensusToMatrixTest(String consensus, DataCollection<?> origin, String name)
        {
        	// String consensusTest = "AaTtGgCc";
            Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();
            byte[] consensusBytes = consensus.getBytes();
            
            ///// print consensusBytes
            String s = "consensusBytes = ";
            for( int i = 0; i < consensusBytes.length; i++ ) s += consensusBytes[i];
    		log.info(s);
    		//////////


            double[][] matrix = new double[consensusBytes.length - alphabet.codeLength() + 1][alphabet.size()];
            
    		log.info("--- consensusToMatrixTest : dim = " + (consensusBytes.length - alphabet.codeLength() + 1) + " x " + alphabet.size());

            for( int i = 0; i < matrix.length; i++ )
            {
                byte code = alphabet.lettersToCode(consensusBytes, i);
                if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR )
            		log.info("--- consensus " + consensus + " contains invalid character at position " + i + "");
                for( byte basicCode : alphabet.basicCodes(code) )
                {
            		log.info(" Position = " + i + " code = " + code + " basicCode = " + basicCode);
                	matrix[i][basicCode] = 1;
                }
            }
            return new FrequencyMatrix(origin, name, alphabet, null, matrix, false);
        }
    }
    /********************* FrequencyMatrixUtils : end *********************/

    /****************************** SiteModelComposed : start ***************/
    public static class SiteModelComposed
    {
        protected SiteModel[] siteModels;
        protected String[] siteModelNames;
        protected String name;
        protected boolean areBothStrands;
        
        public SiteModelComposed(SiteModel[] siteModels, String[] siteModelNames, String name, boolean areBothStrands)
        {
            this.siteModels = siteModels;
            this.siteModelNames = siteModelNames;
            this.name = name;
            this.areBothStrands = areBothStrands;
        }
        
        public String getSiteModelName()
        {
        	return name;
        }
        
        // Input sequences must be SequenceRegions, in particular they can be full chromosomes.
        public Track findAllSites(Sequence[] sequences, DataElementPath pathToSequences, DataElementPath pathToOutputFolder, String trackName, AnalysisJobControl jobControl, int from, int to)
        {
            // 1. Install track and create reversed sequences.
            SqlTrack track = SqlTrack.createTrack(pathToOutputFolder.getChildPath(trackName), null, pathToSequences);
            Sequence[] sequencesReversed = areBothStrands ? new Sequence[sequences.length] : null;
            if( areBothStrands )
                for( int i = 0; i < sequences.length; i++ )
                    sequencesReversed[i] = SequenceRegion.getReversedSequence(sequences[i]);
            
            // 2. Find all sites.
            int nJob = siteModels.length * sequences.length, difference = to - from, index = 0;
            for( int i = 0; i < siteModels.length; i++ )
            {
                // 2.1. Find all sites for current site model.
                SqlTrack sqlTrack = SqlTrack.createTrack(pathToOutputFolder.getChildPath(name + ".temporary"), null);
                for( int j = 0; j < sequences.length; j++ )
                {
                    if( jobControl != null )
                        jobControl.setPreparedness(from + index++ * difference / nJob);
                    for( int jj = 0; jj < 2; jj++ )
                    {
                        Sequence seq = jj == 0 ? sequences[j] : null;
                        if( jj == 1 )
                        {
                            if( ! areBothStrands ) break;
                            seq = sequencesReversed[j];
                        }
                        try
                        {
                            siteModels[i].findAllSites(seq, sqlTrack);
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                    sqlTrack.finalizeAddition();
                    CollectionFactoryUtils.save(sqlTrack);
                }
                
                // 2.2. Modify sites for current site model and put them to resulted track.
                for( Site site : sqlTrack.getAllSites() )
                {
                    Site newSite = new SiteImpl(null, siteModelNames[i], siteModelNames[i], Site.BASIS_PREDICTED, site.getStart(), site.getLength(), Site.PRECISION_NOT_KNOWN, site.getStrand(), site.getSequence(), null);
                    DynamicPropertySet dps = newSite.getProperties();
                    dps.add(new DynamicProperty(Site.SCORE_PD, Float.class, site.getScore()));
                    track.addSite(newSite);
                }
                pathToOutputFolder.getChildPath(name + ".temporary").remove();
            }
            
            // 3. Save track.
            track.finalizeAddition();
            CollectionFactoryUtils.save(track);
            return track;
        }

        public double[] findBestScores(Sequence[] sequences)
        {
            double[] result = new double[sequences.length];
            for( int i = 0; i < sequences.length; i++ )
            {
                Site site = findBestSite(sequences[i]);
                result[i] = site != null ? site.getScore() : Double.NaN; 
            }
            return result;
        }
        
        // For site prediction in genome, here it is desirable that sequence is constructed as new SequenceRegion(...);
        // TODO: To use findBestSite(Sequence sequence, SiteModel siteModel, boolean areBothStrands)
        public Site findBestSite(Sequence sequence)
        {
            Site result = null;
            double bestScore = -Double.MAX_VALUE;
            Sequence[] sequences = areBothStrands ? new Sequence[]{sequence, SequenceRegion.getReversedSequence(sequence)} : new Sequence[]{sequence};
            int ii = areBothStrands ? 2 : 1;
            for( int i = 0; i < ii; i++ )
                for( SiteModel siteModel : siteModels )
                {
                    Site site = siteModel.findBestSite(sequences[i]);
                    double score = site.getScore();
                    if( score > bestScore )
                    {
                        result = site;
                        bestScore = score;
                    }
                }
            return result;
        }
    }
    /********** SiteModelComposed : end **********/
    
    private static Logger log = Logger.getLogger(SiteModelUtils.class.getName());
}
