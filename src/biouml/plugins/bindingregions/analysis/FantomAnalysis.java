/* $Id$ */

package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.bindingregions.utils.ExploratoryAnalysisUtil;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils.TranscriptionStartSites;
import biouml.plugins.gtrd.utils.GeneActivityUtils;
import biouml.plugins.gtrd.utils.GeneActivityUtils.PromoterRegion;
import biouml.plugins.gtrd.utils.MetaClusterConsrtruction.Metara;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrix.DataMatrixConstructor;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixChar;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.MatrixUtils.MatrixTransformation;
import biouml.plugins.machinelearning.utils.MetaAnalysis.Homogeneity;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.RandomUtils;
import biouml.plugins.machinelearning.utils.StatUtils.SimilaritiesAndDissimilarities;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample.DensityEstimation;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSamples;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class FantomAnalysis extends AnalysisMethodSupport<FantomAnalysis.FantomAnalysisParameters> 
{
    public static final String OPTION_01 = "OPTION_01 : Conversion of initial FANTOM5's files (such files as hg38_fair+new_CAGE_peaks_phase1and2.bed) to track and tab-delimited files";
    public static final String OPTION_02 = "OPTION_02 : Create indicator {0, 1}-matrix indicating the overlapping of binding regions and promoter regions and write it to file. FANTOM5' TSSs are used for determination of promoter regions.";
    public static final String OPTION_03 = "OPTION_03 : Analysis of similarity and homogeneity of cell lines.";
    public static final String OPTION_04 = "OPTION_04 : Analysis of correlation between Fantom and RNA-Seq.";

    public FantomAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new FantomAnalysisParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info(" ****************************************************************************************************");
        log.info(" *                FANTOM5 analysis. There are 4 options :                                           *");
        log.info(" * 1. Conversion of initial FANTOM5's files (such files as hg38_fair+new_CAGE_peaks_phase1and2.bed) *");
        log.info(" *    to track and tab-delimited files.                                                             *");
        log.info(" * 2. Create indicator {0, 1}-matrix indicating the overlapping of binding regions and promoter     *");
        log.info(" *    regions and write it to file.                                                                 *");
        log.info(" * 3. Analysis of similarity and homogeneity of cell lines.                                         *");
        log.info(" * 4. Analysis of correlation between FANTOM5 and RNA-Seq.                                          *");
        log.info(" ****************************************************************************************************");

        String option = parameters.getOption();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        switch( option )
        {
            case OPTION_01 : log.info("Your option is " + OPTION_01);
                             DataElementPath pathToInputFolder = parameters.getParametersForOption01().getPathToInputFolder();
                             
                             // TODO: To create OPTION05
                             // to create sums for closed TSSs
//                             int distanceThreshold = 100;
//                             ExploratoryUtils.modifyDataMatrices(distanceThreshold);
                             
                             //ExploratoryUtils.treatRankAggregationScoresByNormalMixture(pathToInputFolder, new String[]{"PEAKS033100.514.sorted.w_dnase", "PEAKS033434.430.sorted.w_dnase", "PEAKS033494.423.sorted.w_dnase"}, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.analysisOfSiteMotifsAndRaScores(pathToInputFolder, new String[]{"PEAKS033100.514.sorted.w_dnase", "PEAKS033434.430.sorted.w_dnase", "PEAKS033494.423.sorted.w_dnase"}, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.qualityMetricsComparison(pathToOutputFolder);
                             
                             /*** For article on intensities.  ***/
                             //ExploratoryAnalysisUtil.crossValidationOfForwardFeatureSelection(pathToOutputFolder, jobControl);
                             //ExploratoryAnalysisUtil.correlationAnalysisForFeatures( pathToOutputFolder);
                             //ExploratoryAnalysisUtil.correlationBetween20MostImportantAnd20Next(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.differenceBetweenCells(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.reconstructPvalues(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.constructMatrixFor2ndStepOfAdvancedRegression();
                             //ExploratoryAnalysisUtil.constructMatrixFor3rdStepOfAdvancedRegression(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.createIntensityMatrix(pathToInputFolder, pathToOutputFolder);

                             //ru.biosoft.access.core.DataElementPath pathToFileWithIntensities = pathToInputFolder.getChildPath("Intensities_lg_RNA_seq");
                             //ExploratoryAnalysisUtil.cellLineCorrelation(pathToFileWithIntensities, pathToOutputFolder);
                             
                             //ExploratoryAnalysisUtil.represorAnalysis( pathToOutputFolder);
                             //ExploratoryAnalysisUtil.represorAnalysis2(pathToInputFolder, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.primaryRegressionForTFsFromArticle(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.primaryRegressionForTFsFromArticle2(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.activatorAndRepressor(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.activatorAndRepressorForKLF10(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.correlationAnalysisForFeaturesStep2(pathToOutputFolder);
                             
                             /*** For reviewers of article on intensities.  ***/

                             //ExploratoryAnalysisUtil.comparePearsonCorrelations();
                             //ExploratoryAnalysisUtil.constructDmsForDifferentialExpression(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.predictDifferesialExpression();
                             
                             /*** For article on cistrom.  ***/
                             //ExploratoryAnalysisUtil.calculate1stStepRAforAp1andFoxa1andESR1(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.calculateTableWithSummaryOnTracksAndQualityMetric(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.treatRaScoresAndInducedRaScores(pathToInputFolder, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.calculateRaSThresholds(pathToInputFolder, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.calculateRocCurves();
                             //ExploratoryAnalysisUtil.calculateRaSThresholds(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.treatWithDnaseSites(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.treatWithDnaseSitesForAp1(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.countMetaClasters(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.countPeakCallers();
                             //ExploratoryAnalysisUtil.regressionBetweenRAscoresAndSiteScores();
                             //ExploratoryAnalysisUtil.treatWithDnaseSitesClassification();
                             //ExploratoryAnalysisUtil.calculateRocCurvesForOldAndNewMetaClusters2(pathToOutputFolder);
                             
                             
                             //ExploratoryAnalysisUtil.parsingFileWithTransfacSites(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.summaryOnTransfacSites(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.calculateFirstTypeErrorByTransfacSites(pathToInputFolder, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.calculateFirstTypeErrorByTransfacSites2(pathToInputFolder, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.calculateDistanceDetweenTransfacSitesAndMetaClusters(pathToInputFolder, pathToOutputFolder);
                             //ExploratoryAnalysisUtil.getDistinctCellLines(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.calculateFirstTypeErrorByTransfacSites3(pathToInputFolder, pathToOutputFolder);
                             // ExploratoryAnalysisUtil.saveTransfacSitesToTracks(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.tansfacSitesAndMetaClustersAndMotifs(pathToOutputFolder);
                             
                             //ExploratoryAnalysisUtil.testOfClassTransfacSites( pathToOutputFolder);
                             //////////////////////ExploratoryAnalysisUtil.testOfClassTransfacSites2(pathToOutputFolder);
                             
                             //ExploratoryAnalysisUtil.SecondArticleOnCistrom.calculateFirstTypeErrorByTransfacSites(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.SecondArticleOnCistrom.calculateDistanceBetweenTransfacSitesAndMetaClusters(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.SecondArticleOnCistrom.calculateFirstTypeErrorByChipExoTracks(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.SecondArticleOnCistrom.calculateDistancesBetweenCentersOfMetaClustersAndChipExoPeaks(pathToOutputFolder);
                             
                             
                             //ExploratoryAnalysisUtil.SecondArticleOnCistrom.identifyNotOverlappedSitesInOldAndNewMetaClusters(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.SecondArticleOnCistrom.calculateDistancesBetweenBoundariesOfMetaClustersAndChipExoPeaks(pathToOutputFolder);

                             /*** For Cistrom2 article ***/
                             // ExploratoryAnalysisUtil.analysisOfLengthInMetaClusters(pathToOutputFolder);

                             /*** For Cistrom3 article ***/

                             
                             // !!!!! ExploratoryAnalysisUtil.ThirdArticleOnCistrom.mergeOverlappedSites(jobControl);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.testForKmeansModel(pathToOutputFolder);	k-means
                             // ExploratoryAnalysisUtil.ThirdArticleOnCistrom.implementMetaraForESR1();
//Metara metara = new Metara();
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.implementImetaraForGivenTf(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.createMatrixLibrary();
                             
                             
                             
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.comparisonOfMetaraAndImetaraMetaClusterTracks();
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.comparisonOfTreatmentAndNotTreatmentforMetaClusters();
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.comparisonOfMetaClustersForDistinctTf();
                             
                             // !!!!!
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.performNormalMixtureAnalysisOfRAscores();                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.implementImetaraForGivenTf(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.implementImetara(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.implementPolinomialRegression();
                             // ExploratoryAnalysisUtil.ThirdArticleOnCistrom.performNormalMixtureAnalysisOfRAscores();
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.comparisonOfRaScoresAndFrequenciesOfMetaClusters(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.implementMetaraForSelectedTrackNames(); /***/
                             
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.getSummaryOnMetaraSteps(pathToOutputFolder);
//                             pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_");
//                             ExploratoryAnalysisUtil.ThirdArticleOnCistrom.getSummaryOnMetaraSteps(pathToOutputFolder);
//                             pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment");
//                             ExploratoryAnalysisUtil.ThirdArticleOnCistrom.getSummaryOnMetaraSteps(pathToOutputFolder);
//                             pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment");
//                             ExploratoryAnalysisUtil.ThirdArticleOnCistrom.getSummaryOnMetaraSteps(pathToOutputFolder);
                             
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.getOverlappedIntervals();
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.HistoneUtils.getHistonePatternsWithinMetaClusters(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.HistoneUtils.analysisOfHistonePatternsWithinMetaClusters(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.HistoneUtils.analysisOfHistonePairsWithinMetaClusters(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.HistoneUtils.analysisOfRaScoresAndHistoneModifications(pathToOutputFolder);
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.HistoneUtils.getHistonesTest();
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.HistoneUtils.identifyHistoneModificationsInGivenGenomeFragment(pathToOutputFolder);
                             
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.getChartWithPeaksFrequenciesInChromosomeFragmentPositions(pathToOutputFolder);
                             
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.compareRAscoresInTwoTracks();
                             //ExploratoryAnalysisUtil.ThirdArticleOnCistrom.comparisonOfTwoMetaClusterTracks();
                             
                             // ExploratoryAnalysisUtil.ThirdArticleOnCistrom.recalculateRAscore_Test();
                             /*********************************************************************************************/                             

                             /*** For Frequency Matrix Analysis ***/
                             ExploratoryAnalysisUtil.FreqMatrixDerivation.matrixComparison(pathToOutputFolder);

                             //implementOption01(pathToInputFolder, pathToOutputFolder);
                             break;
            case OPTION_02 : log.info("Your option is " + OPTION_02);
                             DataElementPath pathToSequences = parameters.getParametersForOption02().getDbSelector().getSequenceCollectionPath();
                             PromoterRegion[] promoterRegions = parameters.getParametersForOption02().getPromoterRegions();
                             DataElementPath pathToFolderWithTracks = parameters.getParametersForOption02().getPathToFolderWithTracks();
                             String[] trackNames = parameters.getParametersForOption02().getTrackNames();
                             DataElementPath pathToFileWithTsss = parameters.getParametersForOption02().getPathToFile();
                             
                             // temp for article
                             //rabOverlapEnsemblAndFantomTsss(pathToSequences, pathToFileWithTsss, pathToOutputFolder);
                             
                             implementOption02(pathToSequences, pathToFolderWithTracks, trackNames,  promoterRegions, pathToFileWithTsss, pathToOutputFolder);
                             break;
            case OPTION_03 : log.info("Your option is " + OPTION_03);
                             pathToInputFolder = parameters.getParametersForOption03().getPathToInputFolder();
                             implementOption03(pathToInputFolder, pathToOutputFolder); break;
            case OPTION_04 : log.info("Your option is " + OPTION_04);
                             DataElementPath pathToRnaSeqFile = parameters.getParametersForOption04().getPathToFile();
                             pathToInputFolder = parameters.getParametersForOption04().getPathToInputFolder();
                             String cellLine = parameters.getParametersForOption04().getCellLine();
                             implementOption04(pathToRnaSeqFile, pathToInputFolder, cellLine, pathToOutputFolder); break;
        }
        return pathToOutputFolder.getDataCollection();
    }
    
    /************************************************************************/
    /************************ For  OPTION_01 ********************************/
    /************************************************************************/
    
    private void implementOption01(DataElementPath pathToInputFolder, DataElementPath pathToOutputFolder)
    {
        // 1.
        log.info("1. Rewrite Cage peaks from Fantom5's file to new file and to track");
        DataElementPath pathToFile = pathToInputFolder.getChildPath("hg38_fair+new_CAGE_peaks_phase1and2.bed");
        DataElementPath pathToNewFile = pathToOutputFolder.getChildPath("TSSs_CAGE_peaks");
        Object[] objects = rewriteCagePeaksFromFileToFile(pathToFile, pathToOutputFolder, "TSSs_CAGE_peaks", 0, 10);
        rewriteCagePeaksFromNewFileToTrack(pathToNewFile, pathToOutputFolder, "Fantom5", 10, 20);
        
        // 2.
        log.info("2. Re-write expression levels from Fantom5's file 'hg38_fair+new_CAGE_peaks_phase1and2_counts.osc.txt' to file.  Lg-transformation is elso implemented");
        String[] cagePeakNamesOld = (String[])objects[0], cagePeakNamesNew = (String[])objects[1];
        pathToFile = pathToInputFolder.getChildPath("hg38_fair+new_CAGE_peaks_phase1and2_counts.osc.txt");
        rewriteExpressionLevelsFromFileToFile(pathToFile, cagePeakNamesOld, cagePeakNamesNew, pathToOutputFolder, "transcription_levels_counts", "cell_names_counts", true, 20, 40);
        jobControl.setPreparedness(60);
        System.gc();

        // 3.
        log.info("3. Re-write expression levels from Fantom5's file 'hg38_fair+new_CAGE_peaks_phase1and2_tpm.osc.txt' to file");
        pathToFile = pathToInputFolder.getChildPath("hg38_fair+new_CAGE_peaks_phase1and2_tpm.osc.txt");
        rewriteExpressionLevelsFromFileToFile(pathToFile, cagePeakNamesOld, cagePeakNamesNew, pathToOutputFolder, "transcription_levels_tmp", "cell_names_tmp", false, 60, 80);
        
        // 4.
        log.info("4. Transform tables with cell line subtypes from article");
        transformTableWithCellLineSubtypes(pathToInputFolder.getChildPath("TableS1"), pathToOutputFolder.getChildPath("cell_names_counts"), pathToOutputFolder);
        transformTableWithCellLineSubtypes(pathToInputFolder.getChildPath("TableS10"), pathToOutputFolder.getChildPath("cell_names_counts"), pathToOutputFolder);
        transformTableWithCellLineSubtypes(pathToInputFolder.getChildPath("TableS11"), pathToOutputFolder.getChildPath("cell_names_counts"), pathToOutputFolder);
        
        // 5.
        log.info("5. Calculate meanIntensities");
        DataElementPath pathToTable = pathToOutputFolder.getChildPath("TableS1_");
        pathToFile = pathToOutputFolder.getChildPath("transcription_levels_counts_lg");
        calculateMeanIntensities(pathToTable, "Type", pathToFile, pathToOutputFolder, "mean_transcription_levels_counts_lg");
        jobControl.setPreparedness(100);
        
        // 6.
        log.info("6. Relation between CAGE peaks and Gene IDs");
        pathToFile = pathToInputFolder.getChildPath("hg38_fair+new_CAGE_peaks_phase1and2_ann");
        relationBetweenCagePeaksAndGeneIds(pathToFile, pathToOutputFolder);
    }
    
    /************* Translator : start *********/
    private static class Translator
    {
        String[][] vocabularyFrom;
        String[] vocabularyTo;
        
        private Translator(DataMatrixString dataMatrixString, String columnNameForVocabularyFrom)
        {
            vocabularyTo = dataMatrixString.getRowNames();
            String[] column = dataMatrixString.getColumn(columnNameForVocabularyFrom);
            vocabularyFrom = convertIntoTwoDimensions(column);
        }
        
        public String translate(String wordForTranslation)
        {
            if( wordForTranslation.equals("") ) return "";
            for( int i = 0; i < vocabularyFrom.length; i++  )
                for( int j = 0; j < vocabularyFrom[i].length; j++ )
                    if( vocabularyFrom[i][j].equals(wordForTranslation) )
                        return vocabularyTo[i];
            return "";
        }
        
        public static String[][] convertIntoTwoDimensions(String[] column)
        {
            String[][] vocabularyFrom = new String[column.length][];
            for( int i = 0; i < column.length; i++ )
                vocabularyFrom[i] = column[i].equals("") ? new String[]{""} : TextUtil2.split(column[i], ',');
            return vocabularyFrom;
        }
    }
    /************* Translator : end *********/

    private void relationBetweenCagePeaksAndGeneIds(DataElementPath pathToFile, DataElementPath pathToOutputFolder)
    {
        String[] columnNames = new String[]{"entrezgene_id", "hgnc_id", "uniprot_id"};
        
        // 1. Check correctness of FANTOM peaks names.
        DataMatrixString dms = new DataMatrixString(pathToOutputFolder.getChildPath("TSSs_CAGE_peaks"), new String[]{"CAGE_peak_name_in_FANTOM"});
        String[] oldNames = dms.getColumn(0), newNames = dms.getRowNames();
        dms = new DataMatrixString(pathToFile, columnNames);
        String[] rowNames = dms.getRowNames();
        for( int i = 0; i < rowNames.length; i++ )
        {
            if( ! rowNames[i].equals(oldNames[i]) )
            {
                log.info("!!!Error in row " + i + " of tables!!!");
                return;
            }
        }
        
        // 2. Transform to standard format.
        String[][] matrix = dms.getMatrix();
        for( int i = 0; i < matrix.length; i++ )
            for( int j = 0; j < matrix[0].length; j++ )
            {
                matrix[i][j] = matrix[i][j].replace(' ', ',');
                if( matrix[i][j].equals("NA") )
                    matrix[i][j] = "";
            }
        dms = new DataMatrixString(newNames, columnNames, matrix);
        
        // 3. Calculate Ensembl IDs.
        DataMatrixString dmsForTranslator = new DataMatrixString(pathToOutputFolder.getChildPath("Ensembl_HGNC_Uniprot_Entrez"), columnNames); 
        for( int i = 0; i < columnNames.length; i++ )
        {
            log.info("Calculate Ensembl IDs for " + columnNames[i]);
            Translator translator = new Translator(dmsForTranslator, columnNames[i]);
            String[] column = dms.getColumn(columnNames[i]), columnTranslated = UtilsForArray.getConstantArray(column.length, "");
            for( int ii = 0; ii < column.length; ii++ )
            {
                if( column[ii].equals("") ) continue;
                String[] words = TextUtil2.split(column[ii], ',');
                Set<String> set = new HashSet<>();
                for( String s : words )
                {
                    String translatedWord = translator.translate(s);
                    if( ! translatedWord.equals("") )
                        set.add(translatedWord);
                }
                columnTranslated[ii] = set.isEmpty() ? "" : UtilsForArray.toString(set.toArray(new String[0]), ",");
            }
            
            // TODO: temp
            if( i == 0 )
                for( int ii = 0; ii < columnTranslated.length; ii++ )
                    log.info("columnTranslated[ii] = " + columnTranslated[ii]);
            
            dms.addColumn("Ensembl_IDs_from_" + columnNames[i], columnTranslated, dms.getColumnNames().length);
        }
        dms.writeDataMatrixString(true, pathToOutputFolder, "relation_between_Cage_peaks_and_gene_IDs", log);
    }
    
    private Object[] rewriteCagePeaksFromFileToFile(DataElementPath pathToFile, DataElementPath pathToOutputFolder, String fileName, int from, int to)
    {
        List<String> fantomCagePeakNamesOld = new ArrayList<>(), fantomCagePeakNamesNew = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        builder.append("ID\tchromosome\tstrand\tCAGE_peak_start\tCAGE_peak_end\tTSS_start\tTSS_start_+1\tscore\tCAGE_peak_name_in_FANTOM\trgb_string_for_color_coding");
        String[] fileLines = TableAndFileUtils.readLinesInFile(pathToFile);
        int index = 0, fromNew = (from + to) / 2, difference = to - fromNew;
        jobControl.setPreparedness((from + to) / 2);
        for( int i = 0; i < fileLines.length; i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(fromNew + (i + 1) * difference / fileLines.length);
            String[] tokens = TextUtil2.split(fileLines[i], '\t');
            if( tokens.length < 9 || tokens[0].length() < 4 || ! tokens[0].substring(0, 3).equals("chr") ) continue;
            String chromosome = tokens[0].substring(3), newName = "F_" + Integer.toString(index++);
            chromosome = chromosome.equals("M") ? "MT" : chromosome;
            int tssBegin = Integer.parseInt(tokens[6]), strand = tokens[5].equals("+") ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
            builder.append("\n").append(newName).append("\t").append(chromosome).append("\t").append(Integer.toString(strand)).append("\t").append(tokens[1]).append("\t").append(tokens[2]).append("\t").append(tokens[6]).append("\t").append(Integer.toString(tssBegin + 1)).append("\t").append(tokens[4]).append("\t").append(tokens[3]).append("\t").append(tokens[8]);
            fantomCagePeakNamesOld.add(tokens[3]);
            fantomCagePeakNamesNew.add(newName);
        }
        TableAndFileUtils.writeStringToFile(builder.toString(), pathToOutputFolder, fileName, log);
        return new Object[]{fantomCagePeakNamesOld.toArray(new String[0]), fantomCagePeakNamesNew.toArray(new String[0])};
    }
    
    private void rewriteCagePeaksFromNewFileToTrack(DataElementPath pathToNewFile, DataElementPath pathToOutputFolder, String trackName, int from, int to)
    {
        // 1. Read matrices in file.
        Object[] objects = TableAndFileUtils.readMatrixOrSubmatixInFile(pathToNewFile, new String[]{"chromosome", "CAGE_peak_name_in_FANTOM"}, TableAndFileUtils.STRING_TYPE);
        String[][] matrixString = (String[][])objects[2];
        objects = TableAndFileUtils.readMatrixOrSubmatixInFile(pathToNewFile, new String[]{"CAGE_peak_start", "CAGE_peak_end", "TSS_start", "TSS_start_+1", "strand"}, TableAndFileUtils.INT_TYPE);
        int[][] matrixInteger = (int[][])objects[2];
        objects = TableAndFileUtils.readMatrixOrSubmatixInFile(pathToNewFile, new String[]{"score"}, TableAndFileUtils.DOUBLE_TYPE);
        double[][] matrixDouble = (double[][])objects[2];

        // 2. Create and write sites into track.
        SqlTrack track = SqlTrack.createTrack(pathToOutputFolder.getChildPath(trackName), null);
        String siteName = "FANTOM5";
        int difference = to - from;
        for( int i = 0; i < matrixString.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / matrixString.length);
            Site site = new SiteImpl(null, matrixString[i][0], siteName, Site.BASIS_USER_ANNOTATED, matrixInteger[i][2], matrixInteger[i][3] - matrixInteger[i][2] + 1, Site.PRECISION_NOT_KNOWN, matrixInteger[i][4], null, null);
            DynamicPropertySet dps = site.getProperties();
            dps.add(new DynamicProperty("CAGE_start", String.class, Integer.toString(matrixInteger[i][0])));
            dps.add(new DynamicProperty("CAGE_end", String.class, Integer.toString(matrixInteger[i][1])));
            dps.add(new DynamicProperty("score", String.class, Double.toString(matrixDouble[i][0])));
            dps.add(new DynamicProperty("FANTOM_NAME", String.class, matrixString[i][1]));
            track.addSite(site);
        }
        track.finalizeAddition();
        CollectionFactoryUtils.save(track);
    }
    
    private void rewriteExpressionLevelsFromFileToFile(DataElementPath pathToFile, String[] cagePeakNamesOld, String[] cagePeakNamesNew, DataElementPath pathToOutputFolder, String outputFileName, String outputTableName, boolean implementLgTransformation, int from, int to)
    {
        int index = 0, numberOfCells = 0, difference = to - from;
        String[] fileLines = TableAndFileUtils.readLinesInFile(pathToFile), cellNamesOld = null;
        double[][] matrix = new double[cagePeakNamesOld.length][];
        for( int i = 0; i < fileLines.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / fileLines.length);
            String[] tokens = TextUtil2.split(fileLines[i], '\t');
            if( tokens.length < 10 ) continue;
            if( tokens[0].equals(cagePeakNamesOld[index]) )
            {
                double[] row = new double[numberOfCells];
                for( int j = 0; j < numberOfCells; j++ )
                    row[j] = tokens[1 + j].equals("NA") ? Double.NaN : Double.parseDouble(tokens[1 + j]);
                matrix[index++] = row;
            }
            else if( tokens[0].equals("00Annotation") )
            {
                numberOfCells = tokens.length - 1;
                cellNamesOld = (String[])ArrayUtils.subarray(tokens, 1, tokens.length);
            }
        }
        // TODO: To generalized it and create something like class DataMatrixTransformation
        String[] cellNamesNew = new String[numberOfCells];
        for( int i = 0; i < numberOfCells; i++ )
            cellNamesNew[i] = "Cell_" + Integer.toString(i);
        if( implementLgTransformation )
        {
            double[][] matrixLg = MatrixTransformation.getLgMatrixWithReplacement(matrix);
            DataMatrix dm = new DataMatrix(cagePeakNamesNew, cellNamesNew, matrixLg);
            dm.writeDataMatrix(true, pathToOutputFolder, outputFileName + "_lg", log);
            dm = null;
        }
        DataMatrixString dms = new DataMatrixString(cellNamesNew, "cell_names_in_FANTOM", cellNamesOld);
        dms.writeDataMatrixString(false, pathToOutputFolder, outputTableName, log);
        DataMatrix dm = new DataMatrix(cagePeakNamesNew, cellNamesNew, matrix);
        dm.addColumn("mean_intensity", getMeanValues(matrix), matrix[0].length);
        dm.writeDataMatrix(true, pathToOutputFolder, outputFileName, log);
    }
    
    // TODO: To move to appropriate Class!!!
    private static double[] getMeanValues(double[][] matrix)
    {
        double[] result = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++)
            result[i] = PrimitiveOperations.getAverage(matrix[i]);
        return result;
    }
    
    private void transformTableWithCellLineSubtypes(DataElementPath pathToInputTable, DataElementPath pathToTableWithCellNames, DataElementPath pathToOutputFolder)
    {
        DataMatrixString dms = new DataMatrixString(pathToInputTable, null), dmsWithCellNames = new DataMatrixString(pathToTableWithCellNames, null);
        String[] sampleNames = dms.getRowNames(), sampleIds = dms.getColumn("Sample ID"), cellNamesInFantom = dmsWithCellNames.getColumn("cell_names_in_FANTOM"), cellNames = dmsWithCellNames.getRowNames();
        dms.addColumn("Sample name",  sampleNames, dms.getColumnNames().length);
        String[] rowNames = new String[sampleNames.length];
        for( int i = 0; i < rowNames.length; i++)
        {
            int j = 0;
            for( ; j < cellNamesInFantom.length; j++ )
                if(cellNamesInFantom[j].contains(sampleIds[i]) ) break;
            rowNames[i] = cellNames[j];
        }
        DataMatrixString dmsNew = new DataMatrixString(rowNames, dms.getColumnNames(), dms.getMatrix());
        dmsNew.writeDataMatrixString(false, pathToOutputFolder, pathToInputTable.getName() + "_", log);
    }
    
    private void calculateMeanIntensities(DataElementPath pathToInputTable, String columnName, DataElementPath pathToInputFile, DataElementPath pathToOutputFolder, String outputFileName)
    {
        DataMatrixString dms = new DataMatrixString(pathToInputTable, new String[]{columnName});
        String[] cellNames = dms.getRowNames(), types = dms.getColumn(columnName), distinctTypes = UtilsGeneral.getDistinctValues(types), rowNames = TableAndFileUtils.getRowNamesInFile(pathToInputFile);
        DataMatrix dataMatrix = new DataMatrix(pathToInputFile, null);
        double[][] matrix = new double[rowNames.length][distinctTypes.length];
        for( int j = 0; j < distinctTypes.length; j++ )
        {
            List<String> list = new ArrayList<>();
            for( int jj = 0; jj < types.length; jj++ )
                if( types[jj].equals(distinctTypes[j]) )
                    list.add(cellNames[jj]);
            String[] cellNamesSubset = list.toArray(new String[0]);
            DataMatrix dm = dataMatrix.getSubDataMatrixColumnWise(cellNamesSubset);
            double[] means = getMeanValues(dm.getMatrix());
            MatrixUtils.fillColumn(matrix, means, j);
        }
        for( int i = 0; i < distinctTypes.length; i++ )
            distinctTypes[i] = "mean_intensity_lg_" + distinctTypes[i];
        DataMatrix dm = new DataMatrix(rowNames, distinctTypes, matrix);
        dm.addColumn("mean_intensity_lg", getMeanValues(dataMatrix.getMatrix()),distinctTypes.length);
        dm.writeDataMatrix(true, pathToOutputFolder, outputFileName, log);
    }

    /************************************************************************/
    /************************** For OPTION_02 *******************************/
    /************************************************************************/
    
    private void implementOption02(DataElementPath pathToSequences, DataElementPath pathToFolderWithTracks, String[] trackNames, PromoterRegion[] promoterRegions, DataElementPath pathToFileWithTsss, DataElementPath pathToOutputFolder)
    {
        Object[] objects = GeneActivityUtils.calculateIndicatorAndFrequencyMatrices(pathToSequences, pathToFolderWithTracks, trackNames, promoterRegions, pathToFileWithTsss, jobControl);
        DataMatrixChar indicatorMatrix = (DataMatrixChar)objects[0];
        indicatorMatrix.writeDataMatrixChar(pathToOutputFolder, "Indicator_matrix", log);
        DataMatrix frequencyMatrix = (DataMatrix)objects[1];
        frequencyMatrix.writeDataMatrix(true, pathToOutputFolder, "Frequency_matrix", log);
    }
    
    /************************************************************************/
    /************************ For  OPTION_03 ********************************/
    /************************************************************************/
    
    private void implementOption03(DataElementPath pathToInputFolder, DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToTableWithCellLineTypes = pathToInputFolder.getChildPath("TableS1_");
        DataElementPath pathToFileWithIntensities = pathToInputFolder.getChildPath("transcription_levels_counts_lg");
        cellLineCorrelation(pathToTableWithCellLineTypes, "Type", pathToFileWithIntensities, pathToOutputFolder);
        cellLineHomogeneity(pathToTableWithCellLineTypes, "Type", pathToFileWithIntensities, pathToOutputFolder);
    }
    
    private void cellLineCorrelation(DataElementPath pathToTableWithCellLineTypes, String nameOfColumnWithCellLineTypes, DataElementPath pathToFileWithIntensities, DataElementPath pathToOutputFolder)
    {
        String additionalType = "randomly selected sample";
        int randomSampleSize = 300;
        
        // 1. Calculate distinctTypes, cellLineTypes, cellLineNames; dim(cellLineNames) = dim(cellLineTypes);
        DataMatrixString dms = new DataMatrixString(pathToTableWithCellLineTypes, new String[]{nameOfColumnWithCellLineTypes});
        String[] cellLineTypes = dms.getColumn(0), cellLineNames = dms.getRowNames();
        String[] distinctTypes = (String[])UtilsForArray.getDistinctStringsAndIndices(cellLineTypes)[0], namesOfSampleCellLines;
        distinctTypes = (String[])ArrayUtils.add(distinctTypes, additionalType);
        
        // 2. Create samples.
        double[][] samples = new double[distinctTypes.length][];
        for( int i = 0; i < distinctTypes.length; i++ )
        {
            log.info("Correlation sample construction: " + distinctTypes[i]);
            
            // 2.1. Create namesOfSampleCellLines.
            if( distinctTypes[i].equals(additionalType) )
            {
                int[] indices = RandomUtils.selectIndicesRandomly(cellLineNames.length, randomSampleSize, 1);
                namesOfSampleCellLines = new String[indices.length];
                for( int j = 0; j < indices.length; j++ )
                    namesOfSampleCellLines[j] = cellLineNames[indices[j]];
            }
            else
            {
                List<String> list = new ArrayList<>();
                for( int j = 0; j < cellLineNames.length; j++ )
                    if( distinctTypes[i].equals(cellLineTypes[j]) )
                        list.add(cellLineNames[j]);
                namesOfSampleCellLines = list.toArray(new String[0]);
            }
            
            // 2. Create double[][] samples.
            DataMatrix dm = new DataMatrix(pathToFileWithIntensities, namesOfSampleCellLines);
            double[][] mat = MatrixUtils.getTransposedMatrix(dm.getMatrix());
            List<Double> list = new ArrayList<>();
            for( int j = 0; j < mat.length - 1; j++ )
                for( int jj = j + 1; jj < mat.length; jj++ )
                {
                    double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(mat[j], mat[jj]);
                    if( ! Double.isNaN(corr) )
                        list.add(corr);
                }
            samples[i] = UtilsGeneral.fromListToArray(list);
            log.info("Correlation sample construction: " + distinctTypes[i] + " sample size = " + samples[i].length);
        }
        
        // 3. Analysis of samples and output results.
        UnivariateSamples us = new UnivariateSamples(distinctTypes, samples);
        DataMatrix dm = us.getSimpleCharacteristicsOfSamples();
        log.info("Simple characteristics of correlation samples are calculated");
        dm.writeDataMatrix(false, pathToOutputFolder, "correlation_samples_characteristics", log);
        String[] testNames = Homogeneity.getAvailableTestNames();
        Homogeneity homogeneity = new Homogeneity(us);
        dm = homogeneity.performTestsOfHomogeneity(testNames);
        dm.writeDataMatrix(false, pathToOutputFolder, "correlation_homogeneity_tests", log);
        log.info("Correlation homogeneity tests are performed");
        Chart chart = us.createChartWithSmoothedDensities("Correlation", false, DensityEstimation.WINDOW_WIDTH_01, null);
        TableAndFileUtils.addChartToTable("Densities of correlations", chart, pathToOutputFolder.getChildPath("_chart_correlation_densities"));
        log.info("Chart with correlation densities is created");
    }
    
    private void cellLineHomogeneity(DataElementPath pathToTableWithCellLineTypes, String nameOfColumnWithCellLineTypes, DataElementPath pathToFileWithIntensities, DataElementPath pathToOutputFolder)
    {
        double pValue = 0.05;
        
        // 1. Calculate distinctTypes, cellLineTypes, cellLineNames; dim(cellLineNames) = dim(cellLineTypes);
        DataMatrixString dms = new DataMatrixString(pathToTableWithCellLineTypes, new String[]{nameOfColumnWithCellLineTypes});
        String[] cellLineTypes = dms.getColumn(0), cellLineNames = dms.getRowNames();
        String[] distinctTypes = (String[])UtilsForArray.getDistinctStringsAndIndices(cellLineTypes)[0];
        
        // 2. Create array of matrices with intensities for distinctTypes.
        double[][][] matricesForDistinctTypes = new double[distinctTypes.length][][];
        DataMatrix dm = null;
        for( int i = 0; i < distinctTypes.length; i++ )
        {
            List<String> list = new ArrayList<>();
            for( int j = 0; j < cellLineNames.length; j++ )
                if( distinctTypes[i].equals(cellLineTypes[j]) )
                    list.add(cellLineNames[j]);
            dm = new DataMatrix(pathToFileWithIntensities, list.toArray(new String[0])); 
            matricesForDistinctTypes[i] = dm.getMatrix();
        }
        log.info("Matrices with intensities were created");
        
        // 3. Create output dataMatrix.
        String[] rowNames = dm.getRowNames(), testNames = Homogeneity.getAvailableTestNames(), columnNames = new String[0];
        for( String s : testNames )
            columnNames = (String[])ArrayUtils.addAll(columnNames, new String[]{"statistic_" + s, "p-value_" + s});
        columnNames = (String[])ArrayUtils.add(columnNames, "number_of_significant_tests");
        double[][] matrix = new double[rowNames.length][];
        for( int i = 0; i < matrix.length; i++ )
        {
            double[][] samples = new double[distinctTypes.length][];
            for( int j = 0; j < samples.length; j++ )
                samples[j] = matricesForDistinctTypes[j][i];
            UnivariateSamples us = new UnivariateSamples(distinctTypes, samples);
            Homogeneity homogeneity = new Homogeneity(us);
            DataMatrix dataMatrix = homogeneity.performTestsOfHomogeneity(testNames);
            double[] row = MatrixUtils.concatinateRows(dataMatrix.getMatrix());
            int numberOfSignificantTests = 0;
            double[] pValues = dataMatrix.getColumn("p-value");
            for( double x : pValues )
                if( ! Double.isNaN(x) && x <= pValue )
                    numberOfSignificantTests++;
            matrix[i] = ArrayUtils.add(row, (double)numberOfSignificantTests);
            log.info("i = " + i + " numberOfSignificantTests = " + numberOfSignificantTests);
        }
        dm = new DataMatrix(rowNames, columnNames, matrix);
        dm.writeDataMatrix(true, pathToOutputFolder, "homogeneity_tests", log);
    }
    
    /************************************************************************/
    /************************ For  OPTION_04 ********************************/
    /************************************************************************/
    
    private void implementOption04(DataElementPath pathToRnaSeqFile, DataElementPath pathToInputFolder, String cellLine, DataElementPath pathToOutputFolder)
    {
        char letter = TableAndFileUtils.getRowNamesInFile(pathToRnaSeqFile)[0].charAt(3);
        boolean isGene = letter == 'G' ? true : false;
        if( isGene )
            correlationBetweenFantomAndRnaSeqThroughGenes(pathToRnaSeqFile, pathToInputFolder, cellLine, pathToOutputFolder);
        else
            correlationBetweenFantomAndRnaSeqThroughTranscripts(pathToRnaSeqFile, pathToInputFolder, cellLine, pathToOutputFolder);
    }
    
    // It is based on analysis of nearest TSSs.
    private void correlationBetweenFantomAndRnaSeqThroughTranscripts(DataElementPath pathToRnaSeqFile, DataElementPath pathToInputFolder, String cellLine, DataElementPath pathToOutputFolder)
    {
        // 1. Calculate FunSite[] tssFantom.
        TranscriptionStartSites tsssInRnaSeq = new TranscriptionStartSites(pathToRnaSeqFile, pathToRnaSeqFile, "chromosome", "strand", "TSS_start", new String[]{"FPKM_lg"});
        DataElementPath pathToFileWithFantomSites = pathToInputFolder.getChildPath("TSSs_CAGE_peaks");
        DataElementPath pathToFileWithFantomIntensitiesLg = pathToInputFolder.getChildPath("transcription_levels_counts_lg");
        TranscriptionStartSites tsssInFantom = new TranscriptionStartSites(pathToFileWithFantomSites, pathToFileWithFantomIntensitiesLg, "chromosome", "strand", "TSS_start", new String[]{cellLine});
        tsssInRnaSeq.identifyNearestTsss(tsssInFantom.getTsss());
        FunSite[] tsssRnaSeq = tsssInRnaSeq.getTsss();
        
        // 2. Look at nearest sites for test.
        for( int i = 0; i < 30; i++ )
        {
            FunSite fsFantom = (FunSite)tsssRnaSeq[i].getObjects()[0];
            DataMatrix dmRnaSeq = tsssRnaSeq[i].getDataMatrix(), dmFantom = fsFantom.getDataMatrix(); 
            log.info("i = " + i + " Name = " + dmRnaSeq.getRowNames()[0] + " chr = " + tsssRnaSeq[i].getChromosomeName() + " strand = " + tsssRnaSeq[i].getStrand()
                    + " pos = " + tsssRnaSeq[i].getStartPosition() + " intensity = " + dmRnaSeq.getColumn(0)[0]);
            log.info("i = " + i + " Name = " + dmFantom.getRowNames()[0] + " chr = " + fsFantom.getChromosomeName() + " strand = " + fsFantom.getStrand()
                    + " pos = " + fsFantom.getStartPosition() + " intensity = " + dmFantom.getColumn(0)[0]);
            log.info("distanse = " + Math.abs(tsssRnaSeq[i].getStartPosition() - fsFantom.getStartPosition()));
        }
        
        // 3. Analysis of similarity of Fantom and RNA-Seq.
        similarityAnalysis(tsssRnaSeq);
    }
    
    private void similarityAnalysis(FunSite[] tsssRnaSeq)
    {
        int[] distanceThreshold = new int[]{5, 10, 20, 50, 75, 100, 150, 200, 250, 300, 500, 1000, 100000000};
        // 1. Calculation of distances, intensitiesRna , intensitiesFantom.
        int[] distances = new int[tsssRnaSeq.length];
        double[] intensitiesRna = new double[tsssRnaSeq.length], intensitiesFantom = new double[tsssRnaSeq.length]; 
        for( int i = 0; i < tsssRnaSeq.length; i++ )
        {
            FunSite fsFantom = (FunSite)tsssRnaSeq[i].getObjects()[0];
            distances[i] = Math.abs(tsssRnaSeq[i].getStartPosition() - fsFantom.getStartPosition());
            DataMatrix dmRnaSeq = tsssRnaSeq[i].getDataMatrix(), dmFantom = fsFantom.getDataMatrix();
            intensitiesRna[i] = dmRnaSeq.getColumn(0)[0];
            intensitiesFantom[i] = dmFantom.getColumn(0)[0];
        }
        
        // 2. Similarity analysis.
        for( int i = 0; i < distanceThreshold.length; i++ )
        {
            List<Double> list1 = new ArrayList<>(), list2 = new ArrayList<>();
            for( int j = 0; j < distances.length; j++ )
                if( distances[j] <= distanceThreshold[i] )
                {
                    list1.add(intensitiesRna[j]);
                    list2.add(intensitiesFantom[j]);
                }
            double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(UtilsGeneral.fromListToArray(list1), UtilsGeneral.fromListToArray(list2));
            log.info("i = " + i + " n = " + list1.size() + " corr = " + corr + " distanceThreshold = " + distanceThreshold[i]);
        }
    }

    // It is based on summation of Fantom intensities for individual gene.
    private void correlationBetweenFantomAndRnaSeqThroughGenes(DataElementPath pathToRnaSeqFile, DataElementPath pathToInputFolder, String cellLine, DataElementPath pathToOutputFolder)
    {
        // 1. Hard code
        //ru.biosoft.access.core.DataElementPath pathToFileWithIntensities = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Data/Converted_from_initial/transcription_levels_counts");
        //String cellLine = "Cell_1424";
        DataElementPath pathToFileWithIntensities = pathToInputFolder.getChildPath("transcription_levels_counts");
        DataElementPath pathToFantomFileWithGeneIds = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Data/Converted_from_initial/relation_between_Cage_peaks_and_gene_IDs");
        String columnWithGeneIdsInFantom = "Ensembl_IDs_from_entrezgene_id";
        
        DataMatrix rnaIntensitiesDm = new DataMatrix(pathToRnaSeqFile, new String[]{"FPKM_lg"});
        double[] rnaIntensities = rnaIntensitiesDm.getColumn(0);
        String[] geneIdsInRnaSeq = rnaIntensitiesDm.getRowNames();
        DataMatrixString geneIdsInFantomDm = new DataMatrixString(pathToFantomFileWithGeneIds, new String[]{columnWithGeneIdsInFantom});
        String[] geneIdsInFantom = geneIdsInFantomDm.getColumn(columnWithGeneIdsInFantom);
        String[][] geneIdsInFantomInTwoDimensions = Translator.convertIntoTwoDimensions(geneIdsInFantom);
        DataMatrix fantomCellLine = new DataMatrix(pathToFileWithIntensities, new String[]{cellLine});
        double[] fantomIntensities = fantomCellLine.getColumn(0);
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"FPKM_lg", "sum_intensity_in_Fantom_lg", "number_of_Fantom_TSSs"});
        for( int i = 0; i < geneIdsInRnaSeq.length; i++ )
        {
            List<Integer> list = new ArrayList<>();
            for( int j = 0; j < geneIdsInFantomInTwoDimensions.length; j++ )
            {
                if( geneIdsInFantomInTwoDimensions[j].length == 1 && geneIdsInFantomInTwoDimensions[j][0].equals("") ) continue;
                for( int jj = 0; jj < geneIdsInFantomInTwoDimensions[j].length; jj++ )
                    if( geneIdsInFantomInTwoDimensions[j][jj].equals(geneIdsInRnaSeq[i]) )
                        list.add(j);
            }
            if( list.isEmpty() ) continue;
            log.info("i = " + i + " geneIdsInRnaSeq = " + geneIdsInRnaSeq[i] + " number_of_Fantom_TSSs = " + list.size());
            double sumIntensity = 0.0;
            for( int index : list )
                sumIntensity += fantomIntensities[index];
            sumIntensity = sumIntensity >= 1.0 ? Math.log10(sumIntensity) : 0.0;
            dmc.addRow(geneIdsInRnaSeq[i], new double[]{rnaIntensities[i], sumIntensity, (double)list.size()});
        }
        DataMatrix dm = dmc.getDataMatrix();
        dm.writeDataMatrix(false, pathToOutputFolder, "correlation_between_RNA_Seq_and_Fantom", log);
        double[] rnaIntensity = dm.getColumn("FPKM_lg"), fantomIntensity = dm.getColumn("sum_intensity_in_Fantom_lg");
        double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(rnaIntensity, fantomIntensity);
        log.info("n = " + rnaIntensity.length + " corr = " + corr);
    }

    public static String[] getAvailableOptions()
    {
        return new String[]{OPTION_01, OPTION_02, OPTION_03, OPTION_04};
    }
    
    /************************************************************************/
    /************************** Utils for AnalysisMethodSupport *************/
    /************************************************************************/
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Please, select option (i.e. select the concrete session of given analysis).";
        
        public static final String PN_DB_SELECTOR = "Sequences collection";
        public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
        
        public static final String PN_PATH_TO_INPUT_FOLDER = "Path to input folder";
        public static final String PD_PATH_TO_INPUT_FOLDER = "Path to input folder";
        
        public static final String PN_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
        public static final String PD_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
        
        public static final String PN_TRACK_NAMES = "Track names";
        public static final String PD_TRACK_NAMES = "Please, select track names";
        
        public static final String PN_PATH_TO_FILE = "Path to file";
        public static final String PD_PATH_TO_FILE = "Path to file";
        
        public static final String PN_CELL_LINE = "Cell line";
        public static final String PD_CELL_LINE = "Cell line";

        public static final String PN_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        public static final String PD_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        
        public static final String PN_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_02";
        public static final String PD_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_02";
        
        public static final String PN_PARAMETERS_FOR_OPTION_03 = "Parameters for OPTION_03";
        public static final String PD_PARAMETERS_FOR_OPTION_03 = "Parameters for OPTION_03";
        
        public static final String PN_PARAMETERS_FOR_OPTION_04 = "Parameters for OPTION_04";
        public static final String PD_PARAMETERS_FOR_OPTION_04 = "Parameters for OPTION_04";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private String option = OPTION_01;
        private DataElementPath pathToInputFolder;
        private PromoterRegion[] promoterRegions = new PromoterRegion[]{new PromoterRegion()};
        private DataElementPath pathToFolderWithTracks;
        private String[] trackNames;
        private DataElementPath pathToFile;
        private String cellLine;
        private DataElementPath pathToOutputFolder;
        
        public AllParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }
        
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
        
        @PropertyName(MessageBundle.PN_OPTION)
        @PropertyDescription(MessageBundle.PD_OPTION)
        public String getOption()
        {
            return option;
        }
        public void setOption(String option)
        {
            Object oldValue = this.option;
            this.option = option;
            firePropertyChange("*", oldValue, option);
        }
        
        @PropertyName("Promoter regions")
        public PromoterRegion[] getPromoterRegions()
        {
            return promoterRegions;
        }
        public void setPromoterRegions(PromoterRegion[] promoterRegions)
        {
            Object oldValue = this.promoterRegions;
            this.promoterRegions = promoterRegions;
            firePropertyChange("promoterRegions", oldValue, promoterRegions);
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_TRACKS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_TRACKS)
        public DataElementPath getPathToFolderWithTracks()
        {
            return pathToFolderWithTracks;
        }
        public void setPathToFolderWithTracks(DataElementPath pathToFolderWithTracks)
        {
            Object oldValue = this.pathToFolderWithTracks;
            this.pathToFolderWithTracks = pathToFolderWithTracks;
            firePropertyChange("pathToFolderWithTracks", oldValue, pathToFolderWithTracks);
        }
        
        @PropertyName(MessageBundle.PN_TRACK_NAMES)
        @PropertyDescription(MessageBundle.PD_TRACK_NAMES)
        public String[] getTrackNames()
        {
            return trackNames;
        }
        public void setTrackNames(String[] trackNames)
        {
            Object oldValue = this.trackNames;
            this.trackNames = trackNames;
            firePropertyChange("trackNames", oldValue, trackNames);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FILE)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FILE)
        public DataElementPath getPathToFile()
        {
            return pathToFile;
        }
        public void setPathToFile(DataElementPath pathToFile)
        {
            Object oldValue = this.pathToFile;
            this.pathToFile = pathToFile;
            firePropertyChange("pathToFile", oldValue, pathToFile);
        }
        
        @PropertyName(MessageBundle.PN_CELL_LINE)
        @PropertyDescription(MessageBundle.PD_CELL_LINE)
        public String getCellLine()
        {
            return cellLine;
        }
        public void setCellLine(String cellLine)
        {
            Object oldValue = this.cellLine;
            this.cellLine = cellLine;
            firePropertyChange("cellLine*", oldValue, cellLine);
        }

        @PropertyName(MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
        @PropertyDescription(MessageBundle.PD_PATH_TO_OUTPUT_FOLDER)
        public DataElementPath getPathToOutputFolder()
        {
            return pathToOutputFolder;
        }
        public void setPathToOutputFolder(DataElementPath pathToOutputFolder)
        {
            Object oldValue = this.pathToOutputFolder;
            this.pathToOutputFolder = pathToOutputFolder;
            firePropertyChange("pathToOutputFolder", oldValue, pathToOutputFolder);
        }
    }
    
    public static class ParametersForOption01 extends AllParameters
    {}
    
    public static class ParametersForOption01BeanInfo extends BeanInfoEx2<ParametersForOption01>
    {
        public ParametersForOption01BeanInfo()
        {
            super(ParametersForOption01.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToInputFolder", beanClass, DataCollection.class, true));
        }
    }
    
    public static class ParametersForOption02 extends AllParameters
    {}
    
    public static class ParametersForOption02BeanInfo extends BeanInfoEx2<ParametersForOption02>
    {
        public ParametersForOption02BeanInfo()
        {
            super(ParametersForOption02.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
            add("trackNames", TrackNamesSelector.class);
            add(DataElementPathEditor.registerInput("pathToFile", beanClass, FileDataElement.class, true));
            add("dbSelector");
            add("promoterRegions");
        }
    }
    
    public static class ParametersForOption03 extends AllParameters
    {}
    
    public static class ParametersForOption03BeanInfo extends BeanInfoEx2<ParametersForOption03>
    {
        public ParametersForOption03BeanInfo()
        {
            super(ParametersForOption03.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToInputFolder", beanClass, DataCollection.class, true));
        }
    }
    
    public static class ParametersForOption04 extends AllParameters
    {}
    
    public static class ParametersForOption04BeanInfo extends BeanInfoEx2<ParametersForOption04>
    {
        public ParametersForOption04BeanInfo()
        {
            super(ParametersForOption04.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToFile", beanClass, FileDataElement.class, true));
            add(DataElementPathEditor.registerInputChild("pathToInputFolder", beanClass, DataCollection.class, true));
            add("cellLine");
        }
    }
    
    public static class FantomAnalysisParameters extends AllParameters
    {
        ParametersForOption01 parametersForOption01;
        ParametersForOption02 parametersForOption02;
        ParametersForOption03 parametersForOption03;
        ParametersForOption04 parametersForOption04;

        public FantomAnalysisParameters()
        {
            setParametersForOption01(new ParametersForOption01());
            setParametersForOption02(new ParametersForOption02());
            setParametersForOption03(new ParametersForOption03());
            setParametersForOption04(new ParametersForOption04());
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_01)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_01)
        public ParametersForOption01 getParametersForOption01()
        {
            return parametersForOption01;
        }
        public void setParametersForOption01(ParametersForOption01 parametersForOption01)
        {
            Object oldValue = this.parametersForOption01;
            this.parametersForOption01 = parametersForOption01;
            firePropertyChange("parametersForOption01", oldValue, parametersForOption01);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_02)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_02)
        public ParametersForOption02 getParametersForOption02()
        {
            return parametersForOption02;
        }
        public void setParametersForOption02(ParametersForOption02 parametersForOption02)
        {
            Object oldValue = this.parametersForOption02;
            this.parametersForOption02 = parametersForOption02;
            firePropertyChange("parametersForOption02", oldValue, parametersForOption02);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_03)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_03)
        public ParametersForOption03 getParametersForOption03()
        {
            return parametersForOption03;
        }
        public void setParametersForOption03(ParametersForOption03 parametersForOption03)
        {
            Object oldValue = this.parametersForOption03;
            this.parametersForOption03 = parametersForOption03;
            firePropertyChange("parametersForOption03", oldValue, parametersForOption03);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_04)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_04)
        public ParametersForOption04 getParametersForOption04()
        {
            return parametersForOption04;
        }
        public void setParametersForOption04(ParametersForOption04 parametersForOption04)
        {
            Object oldValue = this.parametersForOption04;
            this.parametersForOption04 = parametersForOption04;
            firePropertyChange("parametersForOption04", oldValue, parametersForOption04);
        }
        
        public boolean areParametersForOption01Hidden()
        {
            return( ! getOption().equals(OPTION_01) );
        }
        
        public boolean areParametersForOption02Hidden()
        {
            return( ! getOption().equals(OPTION_02) );
        }
        
        public boolean areParametersForOption03Hidden()
        {
            return( ! getOption().equals(OPTION_03) );
        }
        
        public boolean areParametersForOption04Hidden()
        {
            return( ! getOption().equals(OPTION_04) );
        }
    }
    
    public static class OptionSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableOptions();
        }
    }
    
    public static class TrackNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> tracks = ((ParametersForOption02)getBean()).getPathToFolderWithTracks().getDataCollection(DataElement.class);
                String[] trackNames = tracks.getNameList().toArray(new String[0]);
                Arrays.sort(trackNames, String.CASE_INSENSITIVE_ORDER);
                return trackNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with tracks)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the tracks)"};
            }
        }
    }

    public static class FantomAnalysisParametersBeanInfo extends BeanInfoEx2<FantomAnalysisParameters>
    {
        public FantomAnalysisParametersBeanInfo()
        {
            super(FantomAnalysisParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            addHidden("parametersForOption01", "areParametersForOption01Hidden");
            addHidden("parametersForOption02", "areParametersForOption02Hidden");
            addHidden("parametersForOption03", "areParametersForOption03Hidden");
            addHidden("parametersForOption04", "areParametersForOption04Hidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));

            // old parameters
            //add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, DataCollection.class, true));
            //add(DataElementPathEditor.registerInputChild("chipSeqTracksPath", beanClass, Track.class));
        }
    }
}
