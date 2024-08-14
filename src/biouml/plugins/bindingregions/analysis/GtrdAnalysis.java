/* $Id$ */

package biouml.plugins.bindingregions.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.bindingregions.utils.DataMatrix;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.FunSite;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.SequenceSampleUtils;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.StatUtil.DistributionMixture;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;
import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import biouml.plugins.gtrd.ProteinGTRDType;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class GtrdAnalysis extends AnalysisMethodSupport<GtrdAnalysis.GtrdAnalysisParameters>
{
    // public static final String OPTION_01 = "OPTION_01 : Identify coverage of TSSs and promoters by peaks";
    // public static final String OPTION_02 = "OPTION_02 : Calculate frequencies data matrices for cell lines and tfClasses by using coverage matrices";
    public static final String OPTION_03 = "OPTION_03 : Calculate data matrices with means and sigmas of track-properties";
    // public static final String OPTION_04 = "OPTION_04 : Overlapping binding regions from biological replicas (different tracks obtained by the same peak finder)";
    public static final String OPTION_05 = "OPTION_05 : Calculate and write FunSites into tables";
    public static final String OPTION_06 = "OPTION_06 : Correspondence between GTRD tracks and HOCOMOCO matrices";
    public static final String OPTION_07 = "OPTION_07 : Calculate score-thresholds for HOCOMOCO matrices";
    public static final String OPTION_08 = "OPTION_08 : Calculate thresholds for 2 site models: MATCH, HOCOMOCO";
    public static final String OPTION_09 = "OPTION_09 : Calculate table for Fedor (accuracy for 3 site models: IPS, MATCH, HOCOMOCO)";
    public static final String OPTION_10 = "OPTION_10 : Another option (under construction)";

    public static final String MATRIX_NAME = "Matrix_name";

    public GtrdAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new GtrdAnalysisParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        String option = parameters.getOption();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        switch( option )
        {
            case OPTION_03 : // log.info("\n Option 03: \n Calculate data matrices with number of sites and lengths of peaks");
                             log.info("\n Option 03: \n Calculate data matrices with means and sigmas of track-properties");
                             Species givenSpecie = parameters.getParametersForOption03().getSpecies();
                             DataElementPath pathToFolderWithTracks = parameters.getParametersForOption03().getPathToFolderWithTracks();
                             getDataFromTracks(pathToFolderWithTracks, givenSpecie, pathToOutputFolder, 0, 100); break;
            case OPTION_05 : log.info("\n Option 05: \n Calculate and write FunSites into tables");
                             givenSpecie = parameters.getParametersForOption05().getSpecies();
                             DataElementPath pathToFolderWithFolders = parameters.getParametersForOption05().getPathToFolderWithFolders();
                             String[] foldersNames = parameters.getParametersForOption05().getFoldersNames();
                             int minimalLengthOfSite = parameters.getParametersForOption05().getMinimalLengthOfSite();
                             int maximalLengthOfSite = parameters.getParametersForOption05().getMaximalLengthOfSite();
                             
                             String[] distinctTracksNames = getDistinctTracksNamesinSeveralFolders(pathToFolderWithFolders, givenSpecie, foldersNames);
                             log.info("Number of distinct tracks names= " + distinctTracksNames.length);
                             
//                             FunSite.getInformationAboutControl(pathToFolderWithFolders, foldersNames, distinctTracksNames, pathToOutputFolder, "control_IDs");
//                             DataMatrix dataMatrix = FunSite.getTracksPropertiesMeans(pathToFolderWithFolders, foldersNames, distinctTracksNames);
//                             dataMatrix.writeDataMatrix(false, pathToOutputFolder, "tracks_properties_means", log);
                             
                             FunSite.calculateAndWriteFunSitesIntoTables(pathToFolderWithFolders, foldersNames, distinctTracksNames, minimalLengthOfSite, maximalLengthOfSite, pathToOutputFolder, jobControl, 0, 100);
                             break;
            case OPTION_06 : log.info("\n Option 06: \n Correspondence between GTRD tracks and HOCOMOCO matrices");
                             givenSpecie = parameters.getParametersForOption06().getSpecies();
                             pathToFolderWithFolders = parameters.getParametersForOption06().getPathToFolderWithFolders();
                             foldersNames = parameters.getParametersForOption06().getFoldersNames();
                             DataElementPath pathToFolderWithMatrices = parameters.getParametersForOption06().getPathToFolderWithMatrices();
                             gtrdTracksAndHocomocoMatrices(pathToFolderWithFolders, givenSpecie, foldersNames, pathToFolderWithMatrices, pathToOutputFolder);
                             log.info("O.K."); break;
            case OPTION_07 : log.info("\n OPTION_07: \n Calculate score-thresholds for HOCOMOCO matrices \n 1. Read names of matrices in table. \n 2. Read matrices in given folder. \n 3. Read merged FunSites in tables \n 4. Calculate best site scores");
                             DataElementPath pathToSequences = parameters.getParametersForOption07().getDbSelector().getSequenceCollectionPath();
                             DataElementPath pathToInputTable = parameters.getParametersForOption07().getPathToInputTable();
                             pathToFolderWithMatrices = parameters.getParametersForOption07().getPathToFolderWithMatrices();
                             DataElementPath pathToFolderWithTables = parameters.getParametersForOption07().getPathToFolderWithTables();
                             boolean doRemoveOrphans = parameters.getParametersForOption07().getDoRemoveOrphans();
                             boolean doExtractRandomRegions = parameters.getParametersForOption07().getDoExtractRandomRegions();
                             int minimalNumberOfRegions = parameters.getParametersForOption07().getMinimalNumberOfRegions();
                             String siteModelType = parameters.getParametersForOption07().getSiteModelType();
                             boolean doOverlapWithDnaseSites = false;
                             
                             // Dnase-sites
                             Map<String, List<FunSite>> dnaseSitesAsMap = null;
                             if( doOverlapWithDnaseSites )
                             {
                                 DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/GTRD_38/Answer02/False_negative_quality/_DATA/DHSs_hotspots_K562_ENCFF828WSI.broadPeak");
                                 FunSite[] dnaseSites = FunSite.readFunSitesInFileBedLike(pathToFile,  "chrom", "chromStart", "chromEnd", new String[]{"score", "qValue"});
                                 log.info(" number of dnaseSites = " + dnaseSites.length);
                                 dnaseSitesAsMap = FunSite.transformToMap(dnaseSites);
                                 dnaseSitesAsMap = FunSite.removeUnusualChromosomes(pathToSequences, dnaseSitesAsMap);
                                 FunSite.fromDataMatrixToDataMatrices(dnaseSitesAsMap);
                             }

                             // getIpsThresholds(pathToSequences, pathToInputTable, pathToFolderWithMatrices, pathToFolderWithTables, doRemoveOrphans, dnaseSitesAsMap, pathToOutputFolder, "IPS_thresholds", 0, 100);
                             // getIpsThresholdsByMixture(pathToSequences, pathToInputTable, pathToFolderWithMatrices, pathToFolderWithTables, doRemoveOrphans, dnaseSitesAsMap, pathToOutputFolder, "IPS_thresholds_by_mixture", "_chart_IPS_mixtures", 0, 100);
                             
                             getIpsThresholdsByMixtureTemp(pathToSequences, siteModelType, pathToInputTable, pathToFolderWithMatrices, pathToFolderWithTables, doRemoveOrphans, doExtractRandomRegions, minimalNumberOfRegions, dnaseSitesAsMap, pathToOutputFolder, 0, 100);
                             break;
            case OPTION_08 : log.info("\n" + OPTION_08);
                             pathToSequences = parameters.getParametersForOption08().getDbSelector().getSequenceCollectionPath();
                             DataElementPath pathToTableWithTrackAndMatrixNames = parameters.getParametersForOption08().getPathToInputTable();
                             pathToFolderWithMatrices = parameters.getParametersForOption08().getPathToFolderWithMatrices();
                             DataElementPath pathToFolderWithHocomocoSiteModels = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001");

                             // temp: To identify tfClasses and their names
                             DataElementPath pathToInputTable2 = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/GTRD_38/Answer03/Site_predictions_2/Site_models_accuracy");
                             identifyTfClassesForTracks(pathToInputTable2, pathToOutputFolder, "tfClasses_names");
                             log.info("tfClasses_names are identified");
                             // calculateThresholdsForIpsModels(pathToTableWithTrackAndMatrixNames, pathToFolderWithMatrices,  pathToSequences, pathToOutputFolder, "Thresholds_for_IPS_and_IPS_LOG_models");

                             calculateThresholdsForMatchAndHocomocoModels2(pathToTableWithTrackAndMatrixNames, pathToFolderWithMatrices, pathToFolderWithHocomocoSiteModels,  pathToSequences, pathToOutputFolder, "Thresholds_for_MATCH_and_HOCOMOCO_model_both_calculated");
                             break;
            case OPTION_09 : log.info("\n" + OPTION_09);
                             pathToSequences = parameters.getParametersForOption09().getDbSelector().getSequenceCollectionPath();
                             pathToTableWithTrackAndMatrixNames = parameters.getParametersForOption09().getPathToInputTable();
                             pathToFolderWithMatrices = parameters.getParametersForOption09().getPathToFolderWithMatrices();
                             pathToFolderWithTables = parameters.getParametersForOption09().getPathToFolderWithTables();
                             doRemoveOrphans = parameters.getParametersForOption09().getDoRemoveOrphans();
                             minimalNumberOfRegions = parameters.getParametersForOption09().getMinimalNumberOfRegions();
                             DataElementPath pathToMatrixThresholds = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/GTRD_38/Answer03/Site_predictions_2/Thresholds_for_MATCH_and_HOCOMOCO_model_both_calculated");
                             pathToFolderWithHocomocoSiteModels = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.0001");
                             
                             String outputTableName = doRemoveOrphans ? "Site_models_accuracy" : "Site_models_accuracy_with_orphans";

                             String matrixName = "JUN_HUMAN.H11MO.0.A";
                             String[] namesOfTablesWithMergedSites = new String[]{"PEAKS037011", "PEAKS037012", "PEAKS037013", "PEAKS033434", "PEAKS033441", "PEAKS033494"};
                             
//                             String matrixName = "NFYA_HUMAN.H11MO.0.A";
//                             namesOfTablesWithMergedSites = new String[]{"PEAKS034929", "PEAKS034231", "PEAKS033054", "PEAKS034931"};
                             

                             calculateRocCurves(pathToSequences, pathToFolderWithHocomocoSiteModels, pathToFolderWithMatrices, matrixName, pathToFolderWithTables, namesOfTablesWithMergedSites, doRemoveOrphans, minimalNumberOfRegions,  pathToOutputFolder);
                             // calculateTableWithAccuracySiteModelsForFedor(pathToSequences, pathToTableWithTrackAndMatrixNames, pathToMatrixThresholds, pathToFolderWithHocomocoSiteModels, pathToFolderWithMatrices, pathToFolderWithTables, doRemoveOrphans, minimalNumberOfRegions,  pathToOutputFolder, outputTableName, 0, 100);
                             // calculateTableWithAccurasySiteModelsForFedor2(pathToSequences, pathToTableWithTrackAndMatrixNames, pathToMatrixThresholds, pathToFolderWithHocomocoSiteModels, 10, 100000, pathToOutputFolder, "Site_models_accuracy_nonMerged_GEM_and_MACS", 0, 100);
                             // calculateTableWithAccurasySiteModelsForFedor3(pathToSequences, pathToTableWithTrackAndMatrixNames, pathToMatrixThresholds, pathToFolderWithHocomocoSiteModels, pathToFolderWithTables, 10, 100000, pathToOutputFolder, "Site_models_accuracy_nonMerged_GEM_without_orphans", 0, 100);
                             break;
            case OPTION_10 :
            default        : throw new Exception("The selected mode is not supported in our regression analysis currently");
        }
        return pathToOutputFolder.getDataCollection();
    }
    


    //////////////////////////////////////////////////////////////////////////////
    // For OPTION_09
    //////////////////////////////////////////////////////////////////////////////

    // Track names are row names in input table.
    private void identifyTfClassesForTracks(DataElementPath pathToInputTable, DataElementPath pathToOutputFolder, String outputTableName)
    {
        DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD/Data/peaks");
        String[] tracksNames = TableUtils.readRowNamesInTable(pathToInputTable).toArray(new String[0]), foldersNames = new String[]{"gem", "macs", "pics", "sissrs"};
        String[][] tfClassesAndNames = getTfClassesAndNames(pathToFolderWithFolders, foldersNames, tracksNames);
        TableUtils.writeStringTable(tfClassesAndNames, tracksNames, new String[]{"TF-class", "TF-name"}, pathToOutputFolder.getChildPath(outputTableName));
    }

    private void calculateRocCurves(DataElementPath pathToSequences, DataElementPath pathToFolderWithHocomocoSiteModels, DataElementPath pathToFolderWithMatrices, String matrixName, DataElementPath pathToFolderWithTables, String[] namesOfTablesWithMergedSites, boolean doRemoveOrphans, int minimalNumberOfRegions,  DataElementPath pathToOutputFolder)
    {
        // 1. Create site models.
        FrequencyMatrix frequencyMatrix = pathToFolderWithMatrices.getChildPath(matrixName).getDataElement(FrequencyMatrix.class);
        SiteModel[] siteModels = new SiteModel[]{SiteModelsComparison.getSiteModel(SiteModelsComparison.MATCH_SITE_MODEL, frequencyMatrix, 0.01, 0),
                                                 pathToFolderWithHocomocoSiteModels.getChildPath(matrixName).getDataElement(SiteModel.class)};
//        String[] siteModelNames = new String[siteModels.length];
//        for( int i = 0; i < siteModels.length; i++ )
//            siteModelNames[i] = siteModels[i].getName();
            
        int lengthOfSequenceRegion = 100 + frequencyMatrix.getLength();
        for( int i = 0; i < namesOfTablesWithMergedSites.length; i++ )
        {
            // 2. Read binding regions as fun-sites.
            FunSite[] funSites = FunSite.readFunSitesInTable(pathToFolderWithTables.getChildPath(namesOfTablesWithMergedSites[i]));
            log.info("size1 = " + funSites.length);
            if( funSites.length < minimalNumberOfRegions ) continue;
            if( doRemoveOrphans )
                funSites = FunSite.removeOrphans(funSites);
            log.info("size2a = " + funSites.length);
            if( funSites.length < minimalNumberOfRegions ) continue;
            Map<String, List<FunSite>> funSitesAsMap = FunSite.transformToMap(funSites);
            funSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, funSitesAsMap);
            
            // 3. Read sequences.
            Sequence[] sequences = FunSite.getLinearSequencesWithGivenLength(funSitesAsMap, pathToSequences, lengthOfSequenceRegion);
            
            // 4. Calculate ROC-curves and AUCs and write them into tables.
            SiteModelsComparison smc = new SiteModelsComparison(siteModels);
            Object[] objects = smc.getChartWithROCcurves(sequences, true, true, false);
            Chart chart = (Chart)objects[0];
            TableUtils.addChartToTable(namesOfTablesWithMergedSites[i], chart, pathToOutputFolder.getChildPath("chart"));
            Map<String, Double> modelNameAndAuc = (Map<String, Double>)objects[1];
            int index = 0;
            String[] modelNames = new String[modelNameAndAuc.size()];
            double[] aucs = new double[modelNameAndAuc.size()];
            for( Entry<String, Double> entry : modelNameAndAuc.entrySet() )
            {
                modelNames[index] = entry.getKey();
                aucs[index++] = entry.getValue();
            }
            TableUtils.addRowToDoubleTable(aucs, namesOfTablesWithMergedSites[i], modelNames, pathToOutputFolder, "AUC_values");
        }
    }

    // 1. Create merged regions ( MACS, GEM, PICS, SISSRS).
    // 2. remove orphans.
    // 3. Calculate motif frequencies (HOCOMOCO, MATCH, IPS site models).
    private void calculateTableWithAccuracySiteModelsForFedor(DataElementPath pathToSequences, DataElementPath pathToTableWithTrackAndMatrixNames, DataElementPath pathToMatrixThresholds, DataElementPath pathToFolderWithHocomocoSiteModels, DataElementPath pathToFolderWithMatrices, DataElementPath pathToFolderWithTables, boolean doRemoveOrphans, int minimalNumberOfRegions,  DataElementPath pathToOutputFolder, String outputTableName, int from, int to) throws IOException
    {
        // 0. Auxiliary calculations.
        int window = 100, lengthOfSequenceRegion = 200, nForCtcf = 0;
        String[] columnNames = new String[]{"IPS_1000", "IPS_10000", "MATCH_1000", "MATCH_10000", "HOCOMOCO_1000", "HOCOMOCO_10000", "IPS_1000_RANDOM", "IPS_10000_RANDOM", "MATCH_1000_RANDOM", "MATCH_10000_RANDOM", "HOCOMOCO_1000_RANDOM", "HOCOMOCO_10000_RANDOM", "size_with_orphans", "size_without_orphans"};
        DataMatrix dataMatrixWithThresholds = new DataMatrix(pathToMatrixThresholds);
        
        // 1. Read track names and matrix names in table.
        TableDataCollection table = pathToTableWithTrackAndMatrixNames.getDataElement(TableDataCollection.class);
        String[] trackNames = TableUtils.readRowNamesInTable(table);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        
        // 2. Identify distinct names of matrices
        Set<String> set = new HashSet<>();
        for( String s : matrixNames )
            if( s != null && s != "" )
                set.add(s);
        String[] distinctNames = set.toArray(new String[0]);
        
        // 3. Identify row names in output table.
        DataElementPath path = pathToOutputFolder.getChildPath(outputTableName);
        String[] rowNames = ! path.exists() ? null : TableUtils.readRowNamesInTable(path).toArray(new String[0]);
                
        // 4. Calculate output table.
        int difference = to - from;
        for( int i = 0; i < distinctNames.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / distinctNames.length);
            log.info("matrix name = " + distinctNames[i]);
            
            // 4.1 Identify indices of tracks with the same matrix
            List<Integer> list = new ArrayList<>();
            for( int j = 0; j < matrixNames.length; j++ )
                if( matrixNames[j] != null && distinctNames[i].equals(matrixNames[j]) )
                    list.add(j);
            int[] indices = new int[list.size()];
            for( int j = 0 ; j < list.size(); j++ )
                indices[j] = list.get(j);
                
            // 4.2. Set site models
            FrequencyMatrix frequencyMatrix = pathToFolderWithMatrices.getChildPath(distinctNames[i]).getDataElement(FrequencyMatrix.class);
            SiteModel[] siteModels = new SiteModel[]{SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window),
                                                     SiteModelsComparison.getSiteModel(SiteModelsComparison.MATCH_SITE_MODEL, frequencyMatrix, 0.01, window),
                                                     pathToFolderWithHocomocoSiteModels.getChildPath(distinctNames[i]).getDataElement(SiteModel.class)};
            
            // 4.3. Read thresholds in table.
            double[] thresholdsFromTable = dataMatrixWithThresholds.getRow(distinctNames[i]);
            double[][] thresholds = new double[][]{new double[]{3.0, 4.0}, new double[]{thresholdsFromTable[0], thresholdsFromTable[1]}, new double[]{thresholdsFromTable[2], thresholdsFromTable[3]}};

            ///// temp
            MatrixUtils.printMatrix(log, "thresholds:", thresholds);
            
            for( int index : indices )
            {
                log.info("track name = " + trackNames[index] + " matrix name = " + distinctNames[i]);
                if( rowNames != null && ArrayUtils.contains(rowNames, trackNames[index]) ) continue;
                if( distinctNames[i].equals("CTCF_HUMAN.H11MO.0.A") && ++nForCtcf > 26 ) continue;
                
                // 4.4. Read binding regions as fun-sites.
                FunSite[] funSites = FunSite.readFunSitesInTable(pathToFolderWithTables.getChildPath(trackNames[index]));
                log.info("size1 = " + funSites.length);
                double dimWithOrphans = funSites.length;
                if( funSites.length < minimalNumberOfRegions ) continue;
                if( doRemoveOrphans )
                    funSites = FunSite.removeOrphans(funSites);
                double dimWithoutOrphans = doRemoveOrphans ? (double)funSites.length : Double.NaN;
                log.info("size2a = " + funSites.length);
                if( funSites.length < minimalNumberOfRegions ) continue;
                Map<String, List<FunSite>> funSitesAsMap = FunSite.transformToMap(funSites);
                funSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, funSitesAsMap);

                // 4.5. Calculate rows of output table.
                double[] vector = new double[4 * siteModels.length + 2];
                for( int j = 0; j < 2; j++ ) //////////////////////// not-random and random
                {
                    Sequence[] sequences = j == 0 ? FunSite.getLinearSequencesWithGivenLength(funSitesAsMap, pathToSequences, lengthOfSequenceRegion) : FunSite.getRandomLinearSequencesWithGivenLength(funSitesAsMap, pathToSequences, lengthOfSequenceRegion, 0);
                    for( int jj = 0; jj < siteModels.length; jj++ ) ////////////////////////// different site models
                    {
                        if( jj == 1 )
                            sequences = SiteModelsComparison.getTruncatedSequences(sequences, window - frequencyMatrix.getLength());
                        
                        //temp
                        log.info("j = " + j + " jj = " + jj + " length of sequence = " + sequences[0].getLength());
                        
                        int freq1 = 0, freq2 = 0;
                        for( Sequence sequence : sequences )
                        {
                            double score = SiteModelsComparisonUtils.findBestSite(sequence, true, siteModels[jj]).getScore();
                            if( score >= thresholds[jj][0])
                                freq1++;
                            if( score >= thresholds[jj][1])
                                freq2++;
                        }
                        vector[j * siteModels.length * 2 + 2 * jj] = (double)freq1 / (double)sequences.length;
                        vector[j * siteModels.length * 2 + 2 * jj + 1] = (double)freq2 / (double)sequences.length;
                    }
                }
                vector[siteModels.length * 4] = dimWithOrphans;
                vector[siteModels.length * 4 + 1] = dimWithoutOrphans;
                TableUtils.addRowToDoubleTable(vector, trackNames[index], columnNames, pathToOutputFolder, outputTableName);
            }
        }
    }
    
    // 1. Read regions from MACS and GEM tracks separately.
    // 2. Read score thresholds in table.
    // 3. Calculate motif frequencies (HOCOMOCO site model).
    private void calculateTableWithAccurasySiteModelsForFedor2(DataElementPath pathToSequences, DataElementPath pathToTableWithTrackAndMatrixNames, DataElementPath pathToMatrixThresholds, DataElementPath pathToFolderWithHocomocoSiteModels, int minimalNumberOfRegions, int maximalNumberOfRegions, DataElementPath pathToOutputFolder, String outputTableName, int from, int to) throws IOException
    {
        // 0. Auxiliary calculations.
        DataElementPath pathToFolderWithTracks1 = DataElementPath.create("databases/GTRD/Data/peaks/gem");
        DataElementPath pathToFolderWithTracks2 = DataElementPath.create("databases/GTRD/Data/peaks/macs");
        int lengthOfSequenceRegion = 200, nForCtcf = 0;
        Sequence[] sequences = null;
        String[] columnNames = new String[]{"HOCOMOCO_1000_100_GEM", "HOCOMOCO_10000_100_GEM", "HOCOMOCO_1000_200_GEM", "HOCOMOCO_10000_200_GEM", "HOCOMOCO_1000_MACS", "HOCOMOCO_10000_MACS", "size_GEM", "size_MACS"};
        DataMatrix dataMatrixWithThresholds = new DataMatrix(pathToMatrixThresholds);
        
        // 1. Read track names and matrix names in table.
        TableDataCollection table = pathToTableWithTrackAndMatrixNames.getDataElement(TableDataCollection.class);
        String[] trackNames = TableUtils.readRowNamesInTable(table);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        
        // 2. Identify distinct names of matrices
        Set<String> set = new HashSet<>();
        for( String s : matrixNames )
            if( s != null && s != "" )
                set.add(s);
        String[] distinctNames = set.toArray(new String[0]);
        
        // 3. Identify row names in output table.
        DataElementPath path = pathToOutputFolder.getChildPath(outputTableName);
        String[] rowNames = ! path.exists() ? null : TableUtils.readRowNamesInTable(path).toArray(new String[0]);
                
        // 4. Calculate output table.
        int difference = to - from;
        for( int i = 0; i < distinctNames.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / distinctNames.length);
            log.info("matrix name = " + distinctNames[i]);
            
            // 4.1 Identify indices of tracks with the same matrix
            List<Integer> list = new ArrayList<>();
            for( int j = 0; j < matrixNames.length; j++ )
                if( matrixNames[j] != null && distinctNames[i].equals(matrixNames[j]) )
                    list.add(j);
            int[] indices = new int[list.size()];
            for( int j = 0; j < list.size(); j++ )
                indices[j] = list.get(j);
                
            // 4.2. Set site model
            SiteModel siteModel = pathToFolderWithHocomocoSiteModels.getChildPath(distinctNames[i]).getDataElement(SiteModel.class);
            
            // 4.3. Read thresholds in table.
            double[] thresholdsFromTable = dataMatrixWithThresholds.getRow(distinctNames[i]);
            double[] thresholds = new double[]{thresholdsFromTable[2], thresholdsFromTable[3]};

            ///// temp
            MatrixUtils.printVector(log, "thresholds:", thresholds);
            
            for( int index : indices )
            {
                // 4.4. Remove short or long tracks.
                log.info("track name = " + trackNames[index] + " matrix name = " + distinctNames[i]);
                if( rowNames != null && ArrayUtils.contains(rowNames, trackNames[index]) ) continue;
                if( distinctNames[i].equals("CTCF_HUMAN.H11MO.0.A") && ++nForCtcf > 26 ) continue;
                DataElementPath path1 = pathToFolderWithTracks1.getChildPath(trackNames[index]), path2 = pathToFolderWithTracks2.getChildPath(trackNames[index]);
                if( ! path1.exists() || ! path2.exists() ) continue;
                Track track1 = path1.getDataElement(Track.class), track2 = path2.getDataElement(Track.class);
                int n1 = track1.getAllSites().getSize(), n2 = track2.getAllSites().getSize();;
                if( n1 < minimalNumberOfRegions || n1 > maximalNumberOfRegions || n2 < minimalNumberOfRegions || n2 > maximalNumberOfRegions) continue;

                // 4.5. Calculate rows of output table.
                double[] vector = null;
                for( int j = 0; j < 2; j++ ) //////////////////////// GEM or MAC
                {
                    // 4.5.1. Read binding regions as fun-sites and sequences and calculate frequencies.
                    Track track = j == 0 ? track1 : track2;
                    String rowName = j== 0 ? "gem" : "macs";
                    Map<String, List<FunSite>> funSitesAsMap = FunSite.readSitesWithPropertiesInTrack(track, 1, 100000000, rowName);
                    funSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, funSitesAsMap);
                    
                    if( j == 0 )
                        for( int jj = 0; jj < 2; jj++ ) //////////////////////// GEM length of regions = 200 or 100;
                        {
                            sequences = jj == 0 ? FunSite.getLinearSequencesWithGivenLength(funSitesAsMap, pathToSequences, lengthOfSequenceRegion) : SiteModelsComparison.getTruncatedSequences(sequences, 100);
                            double[] frequencies = getRelativeFrequencies(sequences, siteModel, thresholds);
                            vector = jj == 0 ? frequencies : ArrayUtils.addAll(frequencies, vector);
                        }
                    else
                    {
                        sequences = FunSite.getLinearSequences(funSitesAsMap, pathToSequences, 0);
                        double[] frequencies = getRelativeFrequencies(sequences, siteModel, thresholds);
                        vector = ArrayUtils.addAll(vector, frequencies);
                    }
                }
                vector = ArrayUtils.addAll(vector, new double[]{n1, n2});
                TableUtils.addRowToDoubleTable(vector, trackNames[index], columnNames, pathToOutputFolder, outputTableName);
            }
        }
    }
    
    // 1. Read regions from GEM tracks.
    // 2. Read merged regions in table.
    // 3. Identify regions that are non-orphans.
    // 4. Read score thresholds in table.
    // 5. Calculate motif frequencies for non-orphans (HOCOMOCO site model).
    private void calculateTableWithAccurasySiteModelsForFedor3(DataElementPath pathToSequences, DataElementPath pathToTableWithTrackAndMatrixNames, DataElementPath pathToMatrixThresholds, DataElementPath pathToFolderWithHocomocoSiteModels, DataElementPath pathToFolderWithTables, int minimalNumberOfRegions, int maximalNumberOfRegions, DataElementPath pathToOutputFolder, String outputTableName, int from, int to) throws IOException
    {
        // 0. Auxiliary calculations.
        DataElementPath pathToFolderWithTracks = DataElementPath.create("databases/GTRD/Data/peaks/gem");
        int lengthOfSequenceRegion = 200, nForCtcf = 0;
        Sequence[] sequences = null;
        String[] columnNames = new String[]{"HOCOMOCO_1000_100_GEM", "HOCOMOCO_10000_100_GEM", "HOCOMOCO_1000_200_GEM", "HOCOMOCO_10000_200_GEM", "size_GEM", "size_GEM_not_orphans"};
        DataMatrix dataMatrixWithThresholds = new DataMatrix(pathToMatrixThresholds);
        
        // 1. Read track names and matrix names in table.
        TableDataCollection table = pathToTableWithTrackAndMatrixNames.getDataElement(TableDataCollection.class);
        String[] trackNames = TableUtils.readRowNamesInTable(table);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        
        // 2. Identify distinct names of matrices
        Set<String> set = new HashSet<>();
        for( String s : matrixNames )
            if( s != null && s != "" )
                set.add(s);
        String[] distinctNames = set.toArray(new String[0]);
        
        // 3. Identify row names in output table.
        DataElementPath path = pathToOutputFolder.getChildPath(outputTableName);
        String[] rowNames = ! path.exists() ? null : TableUtils.readRowNamesInTable(path).toArray(new String[0]);
                
        // 4. Calculate output table.
        int difference = to - from;
        for( int i = 0; i < distinctNames.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / distinctNames.length);
            log.info("matrix name = " + distinctNames[i]);
            
            // 4.1 Identify indices of tracks with the same matrix
            List<Integer> list = new ArrayList<>();
            for( int j = 0; j < matrixNames.length; j++ )
                if( matrixNames[j] != null && distinctNames[i].equals(matrixNames[j]) )
                    list.add(j);
            int[] indices = new int[list.size()];
            for( int j = 0; j < list.size(); j++ )
                indices[j] = list.get(j);
                
            // 4.2. Set site model
            SiteModel siteModel = pathToFolderWithHocomocoSiteModels.getChildPath(distinctNames[i]).getDataElement(SiteModel.class);
            
            // 4.3. Read thresholds in table.
            double[] thresholdsFromTable = dataMatrixWithThresholds.getRow(distinctNames[i]);
            double[] thresholds = new double[]{thresholdsFromTable[2], thresholdsFromTable[3]};

            ///// temp
            MatrixUtils.printVector(log, "thresholds:", thresholds);
            
            for( int index : indices )
            {
                // 4.4. Remove short or long tracks.
                log.info("track name = " + trackNames[index] + " matrix name = " + distinctNames[i]);
                if( rowNames != null && ArrayUtils.contains(rowNames, trackNames[index]) ) continue;
                if( distinctNames[i].equals("CTCF_HUMAN.H11MO.0.A") && ++nForCtcf > 26 ) continue;
                DataElementPath pathToTrack  = pathToFolderWithTracks.getChildPath(trackNames[index]);
                if( ! pathToTrack.exists() ) continue;
                Track track = pathToTrack.getDataElement(Track.class);
                int n = track.getAllSites().getSize();
                if( n < minimalNumberOfRegions || n > maximalNumberOfRegions ) continue;
                
                // 4.5. Read funSites in track.
                String rowName = pathToFolderWithTracks.getName();
                Map<String, List<FunSite>> funSitesAsMap = FunSite.readSitesWithPropertiesInTrack(track, 1, 100000000, rowName);
                funSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, funSitesAsMap);

                // 4.6. Read merged sites (as funSites) in table and select appropriate sites.
                FunSite[] funSites = FunSite.readFunSitesInTable(pathToFolderWithTables.getChildPath(trackNames[index]));
                log.info("size1 = " + funSites.length);
                if( funSites.length < minimalNumberOfRegions ) continue;
                funSites = FunSite.removeOrphans(funSites);
                log.info("size2a = " + funSites.length);
                if( funSites.length < minimalNumberOfRegions ) continue;
                int indx = ArrayUtils.indexOf(funSites[0].getDataMatrix().getColumnNames(), "Fold_gem");
                if( indx < 0 )
                {
                    log.info("index = 0 !!!");
                    return;
                }
                List<FunSite> siteList = new ArrayList<>();
                for( FunSite fs : funSites )
                    if( ! Double.isNaN(funSites[0].getDataMatrix().getMatrix()[0][indx]) )
                        siteList.add(fs);
                funSites = siteList.toArray(new FunSite[0]);
                if( funSites.length < minimalNumberOfRegions ) continue;
                Map<String, List<FunSite>> mergedFunSitesAsMap = FunSite.transformToMap(funSites);
                mergedFunSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, mergedFunSitesAsMap);
                Map<String, List<FunSite>> fSites = identifyOverlappedFunSites(funSitesAsMap, mergedFunSitesAsMap);
                int nn = FunSite.getNumberOfSites(fSites);
                if( nn < minimalNumberOfRegions ) return;
                
                // 4.7. Calculate row of output table.
                double[] vector = null;
                for( int j = 0; j < 2; j++ )  //////////////////////// GEM length of regions = 200 or 100;
                {
                    sequences = j == 0 ? FunSite.getLinearSequencesWithGivenLength(fSites, pathToSequences, lengthOfSequenceRegion) : SiteModelsComparison.getTruncatedSequences(sequences, 100);
                    double[] frequencies = getRelativeFrequencies(sequences, siteModel, thresholds);
                    vector = j == 0 ? frequencies : ArrayUtils.addAll(frequencies, vector);
                }
                vector = ArrayUtils.addAll(vector, new double[]{n, nn});
                MatrixUtils.printVector(log, "vector = ", vector);
                TableUtils.addRowToDoubleTable(vector, trackNames[index], columnNames, pathToOutputFolder, outputTableName);
            }
        }
    }
    
    // Select all sites from funSites1 that are overlapped with sites from funSites2.
    // Attention: The input funSites2 will be changed!
    private static Map<String, List<FunSite>> identifyOverlappedFunSites(Map<String, List<FunSite>> funSites1, Map<String, List<FunSite>> funSites2)
    {
        Map<String, List<FunSite>> allSites = FunSite.getUnion(funSites2, funSites1);
        ListUtil.sortAll(allSites);
        Map<String, List<FunSite>> result = new HashMap<>();
        for( Entry<String, List<FunSite>> entry : allSites.entrySet() )
            result.put(entry.getKey(), identifyOverlappedFunSites(entry.getValue()));
        return result;
    }
    
    // It is modification of FunSite.getMergedSites()
    public static List<FunSite> identifyOverlappedFunSites(List<FunSite> funSites)
    {
        List<FunSite> result = new ArrayList<>();
        for( int i = 0; i < funSites.size() - 1; i++ )
        {
            FunSite fs1 = funSites.get(i);
            int finishPosition = fs1.getFinishPosition();
            List<FunSite> funSitesOverlapped = new ArrayList<>();
            funSitesOverlapped.add(fs1);
            for( int ii = i + 1; ii < funSites.size(); ii++ )
            {
                FunSite fs2 = funSites.get(ii);
                if( finishPosition < fs2.getStartPosition() )
                    break;
                else
                {
                    finishPosition = Math.max(finishPosition, fs2.getFinishPosition());
                    funSitesOverlapped.add(fs2);
                }
            }
            if( funSitesOverlapped.size() > 0 )
            {
                if( funSitesOverlapped.size() > 1 )
                    for( FunSite fs : funSitesOverlapped )
                        if( fs.getDataMatrix().getRowNames()[0].equals("gem") )
                            result.add(fs);
                i += funSitesOverlapped.size() - 1;
            }
        }
        return result;
    }

    private static double[] getRelativeFrequencies(Sequence[] sequences, SiteModel siteModel, double[] thresholds)
    {
        int freq1 = 0, freq2 = 0;
        for( Sequence sequence : sequences )
        {
            double score = SiteModelsComparisonUtils.findBestSite(sequence, true, siteModel).getScore();
            if( score >= thresholds[0] )
                freq1++;
            if( score >= thresholds[1] )
                freq2++;
        }
        return new double[]{(double)freq1 / (double)sequences.length, (double)freq2 / (double)sequences.length};
    }

    //////////////////////////////////////////////////////////////////////////////
    // For OPTION_08
    //////////////////////////////////////////////////////////////////////////////

    // Here: HOCOMOCO thresholds are read in HOCOMOCO site models
    private void calculateThresholdsForMatchAndHocomocoModels(DataElementPath pathToFolderWithMatrices, DataElementPath pathToFolderWithHocomocoSiteModels1000, DataElementPath pathToFolderWithHocomocoSiteModels10000, DataElementPath pathToSequences, DataElementPath pathToOutputFolder, String tableName)
    {
        // 0. Auxiliary calculations.
        int numberOfFragmentsInEachChromosome = 2174;
        Random randomNumberGenerator = new Random(1);
        String[] chromosomeNames = EnsemblUtils.sequenceNames(pathToSequences).toArray(String[]::new), columnNames = new String[]{"Match_threshold_for_0.001", "Match_threshold_for_0.0001", "Hocomoco_threshold_for_0.001", "Hocomoco_threshold_for_0.0001"};
        int numberOfSequences = numberOfFragmentsInEachChromosome * chromosomeNames.length;
        int index1 = Math.min((int)((1.0 - 0.001) * numberOfSequences), numberOfSequences - 1);
        int index2 = Math.min((int)((1.0 - 0.0001) * numberOfSequences), numberOfSequences - 1);
        DataCollection<DataElement> frequencyMatrices = pathToFolderWithMatrices.getDataCollection(DataElement.class);
        DataElementPath path = pathToOutputFolder.getChildPath(tableName);
        String[] rowNames = ! path.exists() ? null : TableUtils.readRowNamesInTable(path).toArray(new String[0]);
        for( ru.biosoft.access.core.DataElement de: frequencyMatrices )
        {
            // 1. Create site model MATCH.
            if( ! (de instanceof FrequencyMatrix) ) return;
            FrequencyMatrix frequencyMatrix = (FrequencyMatrix)de;
            String matrixName = frequencyMatrix.getName();
            if( rowNames != null && ArrayUtils.contains(rowNames, matrixName) ) continue;
            int length = frequencyMatrix.getLength();
            SiteModel siteModel = SiteModelsComparison.getSiteModel(SiteModelsComparison.MATCH_SITE_MODEL, frequencyMatrix, 0.01, null);
            
            // 2. Read random sequence sample : each sequence has the same length.
            List<Sequence> list = new ArrayList<>();
            for( String name : chromosomeNames )
            {
                Sequence fullChromosome = pathToSequences.getChildPath(name).getDataElement(AnnotatedSequence.class).getSequence();
                int chromosomeLength = fullChromosome.getLength();
                for( int i = 0; i < numberOfFragmentsInEachChromosome; i++ )
                {
                    int start = randomNumberGenerator.nextInt(chromosomeLength - length);
                    FunSite randomSite = new FunSite(name, new Interval(start, start + length - 1), 0, null, null);
                    // list.add(new LinearSequence(randomSite.getSequenceRegion(fullChromosome, 1)));
                    list.add(new LinearSequence(randomSite.getSequenceRegion(fullChromosome, length)));
                }
            }
            Sequence[] sequences = list.toArray(new Sequence[0]);
            
            // 3. Identify best site scores and two score thresholds (for fractions 1/1000 and 1/10000) for MATCH model and score thresholds for HOCOMOCO model.
            List<Double> scores = new ArrayList<>();
            for( Sequence sequence : sequences )
                scores.add(SiteModelsComparisonUtils.findBestSite(sequence, true, siteModel).getScore());
            Collections.sort(scores);
            SiteModel siteModel1 = pathToFolderWithHocomocoSiteModels1000.getChildPath(matrixName).getDataElement(SiteModel.class);
            SiteModel siteModel2 = pathToFolderWithHocomocoSiteModels10000.getChildPath(matrixName).getDataElement(SiteModel.class);
            double[] thresholds = new double[]{scores.get(index1), scores.get(index2), siteModel1.getThreshold(), siteModel2.getThreshold()};
            TableUtils.addRowToDoubleTable(thresholds, matrixName, columnNames, pathToOutputFolder, tableName);
            log.info("matrixName = " + matrixName + " thresholds = " + thresholds[0] + " " + thresholds[1] + " " + thresholds[2] + " " + thresholds[3]);
        }
    }
    
    // Here: HOCOMOCO thresholds are calculated as thresholds for MATCH site models
    private void calculateThresholdsForMatchAndHocomocoModels2(DataElementPath pathToTableWithTrackAndMatrixNames, DataElementPath pathToFolderWithMatrices, DataElementPath pathToFolderWithHocomocoSiteModels,  DataElementPath pathToSequences, DataElementPath pathToOutputFolder, String outputTableName)
    {
        // 0. Auxiliary calculations.
        int numberOfFragmentsInEachChromosome = 2174;
        Random randomNumberGenerator = new Random(1);
        String[] chromosomeNames = EnsemblUtils.sequenceNames(pathToSequences).toArray(String[]::new), columnNames = new String[]{"Match_threshold_for_0.001", "Match_threshold_for_0.0001", "Hocomoco_threshold_for_0.001", "Hocomoco_threshold_for_0.0001"};
        int numberOfSequences = numberOfFragmentsInEachChromosome * chromosomeNames.length;
        int index1 = Math.min((int)((1.0 - 0.001) * numberOfSequences), numberOfSequences - 1);
        int index2 = Math.min((int)((1.0 - 0.0001) * numberOfSequences), numberOfSequences - 1);
        
        // 0.1. Calculate distinctMatrixNames.
        TableDataCollection table = pathToTableWithTrackAndMatrixNames.getDataElement(TableDataCollection.class);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        Set<String> set = new HashSet<>();
        for( String s : matrixNames )
            if( s != null && s != "" )
                set.add(s);
        String[] distinctMatrixNames = set.toArray(new String[0]);
        
        // temp
        log.info("numberOfSequences = " + numberOfSequences + " index1 = " + index1 + " index2 = " + index2 + " # distinctMatrixNames = " + distinctMatrixNames.length);
        
        DataElementPath path = pathToOutputFolder.getChildPath(outputTableName);
        String[] rowNames = ! path.exists() ? null : TableUtils.readRowNamesInTable(path).toArray(new String[0]);
        for( String matrixName : distinctMatrixNames )
        {
            if( rowNames != null && ArrayUtils.contains(rowNames, matrixName) ) continue;
            
            log.info("matrixName = " + matrixName + " path = " + pathToFolderWithMatrices.getChildPath(matrixName).toString());

            // 1. Define site models.
            FrequencyMatrix frequencyMatrix = pathToFolderWithMatrices.getChildPath(matrixName).getDataElement(FrequencyMatrix.class);
            int length = frequencyMatrix.getLength();
            SiteModel[] siteModels = new SiteModel[]{SiteModelsComparison.getSiteModel(SiteModelsComparison.MATCH_SITE_MODEL, frequencyMatrix, 0.01, null),
                                                     pathToFolderWithHocomocoSiteModels.getChildPath(matrixName).getDataElement(SiteModel.class)};
            
            // 2. Read random sequence sample : each sequence has the same length.
            List<Sequence> list = new ArrayList<>();
            for( String name : chromosomeNames )
            {
                Sequence fullChromosome = pathToSequences.getChildPath(name).getDataElement(AnnotatedSequence.class).getSequence();
                int chromosomeLength = fullChromosome.getLength();
                for( int i = 0; i < numberOfFragmentsInEachChromosome; i++ )
                {
                    int start = randomNumberGenerator.nextInt(chromosomeLength - length);
                    FunSite randomSite = new FunSite(name, new Interval(start, start + length - 1), 0, null, null);
                    list.add(new LinearSequence(randomSite.getSequenceRegion(fullChromosome, length)));
                }
            }
            Sequence[] sequences = list.toArray(new Sequence[0]);
            
            // 3. Identify best site scores and two score thresholds (for fractions 1/1000 and 1/10000) for MATCH model and score thresholds for HOCOMOCO model.
            double[] vector = null;
            for( int i = 0; i < siteModels.length; i++ )
            {
                List<Double> scores = new ArrayList<>();
                for( Sequence sequence : sequences )
                    scores.add(SiteModelsComparisonUtils.findBestSite(sequence, true, siteModels[i]).getScore());
                Collections.sort(scores);
                vector = i == 0 ? new double[]{scores.get(index1), scores.get(index2)} : ArrayUtils.addAll(vector, new double[]{scores.get(index1), scores.get(index2)});
                
                // temp
                log.info("score_min = " + scores.get(0) + " score_max = " + scores.get(scores.size() - 1));
                log.info("length = " + length + " sequence length = " + sequences[0].getLength() + " " + sequences[1].getLength());

            }
            TableUtils.addRowToDoubleTable(vector, matrixName, columnNames, pathToOutputFolder, outputTableName);
            log.info("matrixName = " + matrixName + " thresholds = " + vector[0] + " " + vector[1] + " " + vector[2] + " " + vector[3]);
        }
    }

    private void calculateThresholdsForIpsModels(DataElementPath pathToTableWithTrackAndMatrixNames, DataElementPath pathToFolderWithMatrices,  DataElementPath pathToSequences, DataElementPath pathToOutputFolder, String outputTableName)
    {
        // 0. Auxiliary calculations.
        int numberOfFragmentsInEachChromosome = 2174, window = 100;
        Random randomNumberGenerator = new Random(1);
        String[] chromosomeNames = EnsemblUtils.sequenceNames(pathToSequences).toArray(String[]::new), columnNames = new String[]{"IPS_threshold_for_0.001", "IPS_threshold_for_0.0001", "IPS_LOG_threshold_for_0.001", "IPS_LOG_threshold_for_0.0001"};
        int numberOfSequences = numberOfFragmentsInEachChromosome * chromosomeNames.length;
        int index1 = Math.min((int)((1.0 - 0.001) * numberOfSequences), numberOfSequences - 1);
        int index2 = Math.min((int)((1.0 - 0.0001) * numberOfSequences), numberOfSequences - 1);

        // 0.1. Calculate distinctMatrixNames.
        TableDataCollection table = pathToTableWithTrackAndMatrixNames.getDataElement(TableDataCollection.class);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        Set<String> set = new HashSet<>();
        for( String s : matrixNames )
            if( s != null && s != "" )
                set.add(s);
        String[] distinctMatrixNames = set.toArray(new String[0]);
        
        // temp
        log.info("numberOfSequences = " + numberOfSequences + " index1 = " + index1 + " index2 = " + index2 + " # distinctMatrixNames = " + distinctMatrixNames.length);

        // 1. Read random sequence sample : each sequence has the same length.
        List<Sequence> list = new ArrayList<>();
        for( String name : chromosomeNames )
        {
            Sequence fullChromosome = pathToSequences.getChildPath(name).getDataElement(AnnotatedSequence.class).getSequence();
            int chromosomeLength = fullChromosome.getLength();
            for( int i = 0; i < numberOfFragmentsInEachChromosome; i++ )
            {
                int start = randomNumberGenerator.nextInt(chromosomeLength - window);
                FunSite randomSite = new FunSite(name, new Interval(start, start + window - 1), 0, null, null);
                list.add(new LinearSequence(randomSite.getSequenceRegion(fullChromosome, window)));
            }
        }
        Sequence[] sequences = list.toArray(new Sequence[0]);

        DataElementPath path = pathToOutputFolder.getChildPath(outputTableName);
        String[] rowNames = ! path.exists() ? null : TableUtils.readRowNamesInTable(path).toArray(new String[0]);
        for( String matrixName : distinctMatrixNames )
        {
            if( rowNames != null && ArrayUtils.contains(rowNames, matrixName) ) continue;
            log.info("matrixName = " + matrixName + " path = " + pathToFolderWithMatrices.getChildPath(matrixName).toString());

            // 2. Define site models.
            FrequencyMatrix frequencyMatrix = pathToFolderWithMatrices.getChildPath(matrixName).getDataElement(FrequencyMatrix.class);
            int length = frequencyMatrix.getLength();
            SiteModel[] siteModels = new SiteModel[]{SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, null),
                                                     SiteModelsComparison.getSiteModel(SiteModelsComparison.LOG_IPS_SITE_MODEL, frequencyMatrix, 0.01, null)};
            
            // 3. Identify best site scores and two score thresholds (for fractions 1/1000 and 1/10000) for MATCH model and score thresholds for HOCOMOCO model.
            double[] vector = null;
            for( int i = 0; i < siteModels.length; i++ )
            {
                List<Double> scores = new ArrayList<>();
                for( Sequence sequence : sequences )
                    scores.add(SiteModelsComparisonUtils.findBestSite(sequence, true, siteModels[i]).getScore());
                Collections.sort(scores);
                vector = i == 0 ? new double[]{scores.get(index1), scores.get(index2)} : ArrayUtils.addAll(vector, new double[]{scores.get(index1), scores.get(index2)});
                
                // temp
                log.info("score_min = " + scores.get(0) + " score_max = " + scores.get(scores.size() - 1));
            }
            TableUtils.addRowToDoubleTable(vector, matrixName, columnNames, pathToOutputFolder, outputTableName);
            log.info("matrixName = " + matrixName + " thresholds = " + vector[0] + " " + vector[1] + " " + vector[2] + " " + vector[3]);
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    // For OPTION_07
    //////////////////////////////////////////////////////////////////////////////
    private void getIpsThresholds(DataElementPath pathToSequences, DataElementPath pathToInputTable, DataElementPath pathToFolderWithMatrices, DataElementPath pathToFolderWithTables, boolean doRemoveOrphans, int minimalNumberOfRegions, Map<String, List<FunSite>> dnaseSitesAsMap, DataElementPath pathToOutputFolder, String outputTableName, int from, int to)
    {
        int window = 100, minimalLengthOfSequenceRegion = 200;
        // 1. Read track names and matrix names in table.
        TableDataCollection table = pathToInputTable.getDataElement(TableDataCollection.class);
        String[] trackNames = TableUtils.readRowNamesInTable(table);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        
        // 2. Definition of fractions and columnNames.
        double[] fractions = new double[]{0.1, 0.2, 0.3, 0.5, 0.7, 0.75, 0.80, 0.85, 0.90, 0.95, 0.99};
        String[] columnNames = new String[fractions.length];
        for(int i = 0; i < fractions.length; i++ )
            columnNames[i] = "Thresholds_for_" + String.valueOf((int)(fractions[i] * 100.0 + 0.0001));

        // 3. Identify treated track names
        DataElementPath pathToOutputTable = pathToOutputFolder.getChildPath(outputTableName);
        String[] treatedTrackNames = pathToOutputTable.exists() ? TableUtils.readRowNamesInTable(pathToOutputTable.getDataElement(TableDataCollection.class)) : new String[1];
        
        // 4. Calculate output table.
        int difference = to - from;
        for( int i = 0; i < trackNames.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / trackNames.length);
            log.info("track name = " + trackNames[i] + " matrix name[i] = " + matrixNames[i]);
            if( ArrayUtils.contains(treatedTrackNames, trackNames[i]) ) continue;
            
            // 4.1 Treatment when matrix name == null
            if( matrixNames[i] == null )
            {
                TableUtils.addRowToDoubleTable(MatrixUtils.getConstantVector(fractions.length, Double.NaN), trackNames[i], columnNames, pathToOutputFolder, outputTableName);
                continue;
            }
            
            // 4.2. Treatment when matrix name != null
            //4.2.1. Read funSites, correspondence sequence regions and create site model..
            FunSite[] funSites = FunSite.readFunSitesInTable(pathToFolderWithTables.getChildPath(trackNames[i]));
            if( funSites.length < 10 )
            {
                TableUtils.addRowToDoubleTable(MatrixUtils.getConstantVector(fractions.length, Double.NaN), trackNames[i], columnNames, pathToOutputFolder, outputTableName);
                continue;
            }
            log.info("size1 = " + funSites.length);
            if( doRemoveOrphans )
                funSites = FunSite.removeOrphans(funSites);
            log.info("size2a = " + funSites.length);
            Map<String, List<FunSite>> funSitesAsMap = FunSite.transformToMap(funSites);
            funSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, funSitesAsMap);

            // Overlap with DNAse regions.
            if( dnaseSitesAsMap != null )
            {
                FunSite.fromDataMatrixToDataMatrices(funSitesAsMap);
                funSitesAsMap = FunSite.getUnion(funSitesAsMap, dnaseSitesAsMap);
                ListUtil.sortAll(funSitesAsMap);
                funSitesAsMap = FunSite.getMergedSites(funSitesAsMap);
                FunSite[] fss = FunSite.transformToArray(funSitesAsMap);
                fss = FunSite.removeOrphans(fss);
                funSitesAsMap = FunSite.transformToMap(fss);
                log.info("size2b = " + fss.length);
            }
            
            Sequence[] sequences = FunSite.getLinearSequences(funSitesAsMap, pathToSequences, minimalLengthOfSequenceRegion);
            log.info("size3 = " + sequences.length);
            if( sequences.length < 5 )
            {
                TableUtils.addRowToDoubleTable(MatrixUtils.getConstantVector(fractions.length, Double.NaN), trackNames[i], columnNames, pathToOutputFolder, outputTableName);
                continue;
            }
            FrequencyMatrix frequencyMatrix = pathToFolderWithMatrices.getChildPath(matrixNames[i]).getDataElement(FrequencyMatrix.class);
            SiteModel siteModel = SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window);
            
            // 4.2.2. Identify best site scores and IPS-thresholds.
            List<Double> scores = new ArrayList<>();
            for( Sequence sequence : sequences )
                scores.add(SiteModelsComparisonUtils.findBestSite(sequence, true, siteModel).getScore());
            Collections.sort(scores);
            double[] ipsThresholds = new double[fractions.length];
            for( int j = 0; j < fractions.length; j++ )
            {
                int index = (int)((1.0 - fractions[j]) * scores.size());
                index = Math.min(index, scores.size() - 1);
                ipsThresholds[j] = scores.get(index);
            }
            TableUtils.addRowToDoubleTable(ipsThresholds, trackNames[i], columnNames, pathToOutputFolder, outputTableName);
        }
    }
    
    private void getIpsThresholdsByMixture(DataElementPath pathToSequences, DataElementPath pathToInputTable, DataElementPath pathToFolderWithMatrices, DataElementPath pathToFolderWithTables, boolean doRemoveOrphans, Map<String, List<FunSite>> dnaseSitesAsMap, DataElementPath pathToOutputFolder, String outputTableName, String outputChartTableName, int from, int to)
    {
        int window = 100, minimalLengthOfSequenceRegion = 200;
        // 1. Read track names and matrix names in table.
        TableDataCollection table = pathToInputTable.getDataElement(TableDataCollection.class);
        String[] trackNames = TableUtils.readRowNamesInTable(table);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        
        // 2. Definition of columnNames.
        String[] columnNames = new String[]{"Proportion_of_signal_containing_regions", "Sample_size", "IPS_mean_for_signal_not_containing_regions", "IPS_sigma_for_signal_not_containing_regions", "IPS_mean_for_signal_containing_regions", "IPS_sigma_for_signal_containing_regions", "IPS_threshold_from_signal_containing_regions", "IPS_threshold_from_signal_not_containing_regions_95", "IPS_threshold_from_signal_not_containing_regions_99"};

        // 3. Identify treated track names
        DataElementPath pathToOutputTable = pathToOutputFolder.getChildPath(outputTableName);
        String[] treatedTrackNames = pathToOutputTable.exists() ? TableUtils.readRowNamesInTable(pathToOutputTable.getDataElement(TableDataCollection.class)) : new String[1];
        // 4. Calculate output table.
        int difference = to - from;
        for( int i = 0; i < trackNames.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / trackNames.length);
            log.info("track name = " + trackNames[i] + " matrix name[i] = " + matrixNames[i]);
            if( ArrayUtils.contains(treatedTrackNames, trackNames[i]) ) continue;
            
            // 4.1 Treatment when matrix name == null
            if( matrixNames[i] == null )
            {
                TableUtils.addRowToDoubleTable(MatrixUtils.getConstantVector(columnNames.length, Double.NaN), trackNames[i], columnNames, pathToOutputFolder, outputTableName);
                continue;
            }
            
            // 4.2. Treatment when matrix name != null
            //4.2.1. Read funSites, correspondence sequence regions and create site model..
            FunSite[] funSites = FunSite.readFunSitesInTable(pathToFolderWithTables.getChildPath(trackNames[i]));
            if( funSites.length < 10 )
            {
                TableUtils.addRowToDoubleTable(MatrixUtils.getConstantVector(columnNames.length, Double.NaN), trackNames[i], columnNames, pathToOutputFolder, outputTableName);
                continue;
            }
            log.info("size1 = " + funSites.length);
            if( doRemoveOrphans )
                funSites = FunSite.removeOrphans(funSites);
            log.info("size2a = " + funSites.length);
            Map<String, List<FunSite>> funSitesAsMap = FunSite.transformToMap(funSites);
            funSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, funSitesAsMap);

            // 4.2.2. Overlap with DNAse regions.
            if( dnaseSitesAsMap != null )
            {
                FunSite.fromDataMatrixToDataMatrices(funSitesAsMap);
                funSitesAsMap = FunSite.getUnion(funSitesAsMap, dnaseSitesAsMap);
                ListUtil.sortAll(funSitesAsMap);
                funSitesAsMap = FunSite.getMergedSites(funSitesAsMap);
                FunSite[] fss = FunSite.transformToArray(funSitesAsMap);
                fss = FunSite.removeOrphans(fss);
                funSitesAsMap = FunSite.transformToMap(fss);
                log.info("size2b = " + fss.length);
            }
            
            // 4.2.3. Identify sequences and siteModel
            Sequence[] sequences = FunSite.getLinearSequences(funSitesAsMap, pathToSequences, minimalLengthOfSequenceRegion);
            log.info("size3 = " + sequences.length);
            if( sequences.length < 100 )
            {
                TableUtils.addRowToDoubleTable(MatrixUtils.getConstantVector(columnNames.length, Double.NaN), trackNames[i], columnNames, pathToOutputFolder, outputTableName);
                continue;
            }
            FrequencyMatrix frequencyMatrix = pathToFolderWithMatrices.getChildPath(matrixNames[i]).getDataElement(FrequencyMatrix.class);
            SiteModel siteModel = SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window);
            
            // 4.2.4. Calculate IPS-thresholds and charts.
            Object[] objects = estimateThresholdsByNormalMixture(sequences, siteModel, null);
            if( objects == null )
            {
                TableUtils.addRowToDoubleTable(MatrixUtils.getConstantVector(columnNames.length, Double.NaN), trackNames[i], columnNames, pathToOutputFolder, outputTableName);
                continue;
            }
            double[] vector = (double[])objects[0];
            Chart chart = (Chart)objects[1];
            TableUtils.addRowToDoubleTable(vector, trackNames[i], columnNames, pathToOutputFolder, outputTableName);
            TableUtils.addChartToTable(matrixNames[i] + "_" + trackNames[i], chart, pathToOutputFolder.getChildPath(outputChartTableName));
        }
    }

    // To make charts and table only for tracks if matrices exist.
    private void getIpsThresholdsByMixtureTemp(DataElementPath pathToSequences, String siteModelType, DataElementPath pathToInputTable, DataElementPath pathToFolderWithMatrices, DataElementPath pathToFolderWithTables, boolean doRemoveOrphans, boolean doExtractRandomRegions, int minimalNumberOfRegions, Map<String, List<FunSite>> dnaseSitesAsMap, DataElementPath pathToOutputFolder, int from, int to)
    {
        int window = 100, minimalLengthOfSequenceRegion = 200;
        //siteModelType.equals(SiteModelsComparison.IPS_SITE_MODEL) || siteModelType.equals(SiteModelsComparison.LOG_IPS_SITE_MODEL) ? 200 : 200 - window;
        // 1. Read track names and matrix names in table.
        TableDataCollection table = pathToInputTable.getDataElement(TableDataCollection.class);
        String[] trackNames = TableUtils.readRowNamesInTable(table);
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, MATRIX_NAME);
        String[] columnNames = new String[]{"Proportion_of_signal_containing_regions", "Sample_size", "Score_mean_for_signal_not_containing_regions", "Score_sigma_for_signal_not_containing_regions", "Score_mean_for_signal_containing_regions", "Score_sigma_for_signal_containing_regions", "Score_threshold_from_signal_containing_regions", "Score_threshold_from_signal_not_containing_regions_95", "Score_threshold_from_signal_not_containing_regions_99", "Mean_score_for_random", "Sigma_score_for_random"};
        
        // 2. Identify distinct names of matrices
        Set<String> set = new HashSet<>();
        for( String s : matrixNames )
            if( s != null && s != "" )
                set.add(s);
        String[] distinctNames = set.toArray(new String[0]);
        
        // 3. Calculate output charts.
        int difference = to - from;
        for( int i = 0; i < distinctNames.length; i++ )
        {
            jobControl.setPreparedness(from + (i + 1) * difference / distinctNames.length);
            DataElementPath dep = pathToOutputFolder.getChildPath(distinctNames[i]);
            if( dep.exists() ) continue;
            
            // 3.1 Identify indices of tracks with the same matrix
            List<Integer> list = new ArrayList<>();
            for( int j = 0; j < matrixNames.length; j++ )
                if( matrixNames[j] != null && distinctNames[i].equals(matrixNames[j]) )
                    list.add(j);
            int[] indices = new int[list.size()];
            for( int j = 0 ; j < list.size(); j++ )
                indices[j] = list.get(j);
                
            // 3.2. Set site model
            FrequencyMatrix frequencyMatrix = pathToFolderWithMatrices.getChildPath(distinctNames[i]).getDataElement(FrequencyMatrix.class);
//            SiteModel siteModel = SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window);
            SiteModel siteModel = SiteModelsComparison.getSiteModel(siteModelType, frequencyMatrix, 0.01, window);
            
            int nForCtcf = 0;
            for( int index : indices )
            {
                // 3.3. read fun-sites
                log.info("track name = " + trackNames[index] + " matrix name[i] = " + distinctNames[i]);
                FunSite[] funSites = FunSite.readFunSitesInTable(pathToFolderWithTables.getChildPath(trackNames[index]));
                log.info("size1 = " + funSites.length);
                if( funSites.length < minimalNumberOfRegions ) continue;
                if( doRemoveOrphans )
                    funSites = FunSite.removeOrphans(funSites);
                log.info("size2a = " + funSites.length);
                if( funSites.length < minimalNumberOfRegions ) continue;
                Map<String, List<FunSite>> funSitesAsMap = FunSite.transformToMap(funSites);
                funSitesAsMap =  FunSite.removeUnusualChromosomes(pathToSequences, funSitesAsMap);
                
                // 3.4. Overlap with DNAse regions.
                if( dnaseSitesAsMap != null )
                {
                    FunSite.fromDataMatrixToDataMatrices(funSitesAsMap);
                    funSitesAsMap = FunSite.getUnion(funSitesAsMap, dnaseSitesAsMap);
                    ListUtil.sortAll(funSitesAsMap);
                    funSitesAsMap = FunSite.getMergedSites(funSitesAsMap);
                    FunSite[] fss = FunSite.transformToArray(funSitesAsMap);
                    fss = FunSite.removeOrphans(fss);
                    log.info("size2b = " + fss.length);
                    funSitesAsMap = FunSite.transformToMap(fss);
                }
                
                // 3.5. Identify and truncate (if necessary) sequences
                Sequence[] sequences = FunSite.getLinearSequences(funSitesAsMap, pathToSequences, minimalLengthOfSequenceRegion);
                Sequence[] randomSequences = doExtractRandomRegions ? FunSite.getRandomLinearSequences(funSitesAsMap, pathToSequences, minimalLengthOfSequenceRegion, 0) : null;
                if( ! siteModelType.equals(SiteModelsComparison.IPS_SITE_MODEL) && ! siteModelType.equals(SiteModelsComparison.LOG_IPS_SITE_MODEL) )
                {
                    sequences = SiteModelsComparison.getTruncatedSequences(sequences, window - frequencyMatrix.getLength());
                    randomSequences = randomSequences == null ? null : SiteModelsComparison.getTruncatedSequences(randomSequences, window - frequencyMatrix.getLength());
                }
                log.info("size3 = " + sequences.length);
                if( sequences.length < minimalNumberOfRegions ) continue;
                
                // 3.6. Calculate charts.
                Object[] objects = estimateThresholdsByNormalMixture(sequences, siteModel, randomSequences);
                TableUtils.addRowToDoubleTable((double[])objects[0], trackNames[index], columnNames, pathToOutputFolder, "Scores_with_random_sequences");
                Chart chart = (Chart)objects[1];
                if( distinctNames[i].equals("CTCF_HUMAN.H11MO.0.A") && ++nForCtcf > 26 ) continue;
                TableUtils.addChartToTable(trackNames[index], chart, pathToOutputFolder.getChildPath(distinctNames[i]));
            }
        }
    }
    
    private Object[] estimateThresholdsByNormalMixture(Sequence[] sequences, SiteModel siteModel, Sequence[] randomSequences)
    {
        // 1. Identify normal mixture/
        int numberOfMixtureComponents = 2;
        double[] scores = new double[sequences.length];
        for( int i = 0; i < sequences.length; i++ )
            scores[i] = SiteModelsComparisonUtils.findBestSite(sequences[i], true, siteModel).getScore();
        Map<Integer, Object[]> mixture = DistributionMixture.getNormalMixture(scores, numberOfMixtureComponents, 1000, new Random(1));
        
        // 2. Identify isSecondMixtureComponentIdentified.
        Object[] objects = mixture.get(1);
        boolean isSecondMixtureComponentIdentified = objects.length != 5 ? false : true;
        
        // 3. Identify vector and subsample0, subsample1
        double[] vector = null, meanAndSigma0 = (double[])mixture.get(0)[1], meanAndSigma1 = null, subsample0 = null, subsample1 = null;
        if( ! isSecondMixtureComponentIdentified )
        {
            vector = new double[]{0.0, sequences.length, meanAndSigma0[0], meanAndSigma0[1], Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
            subsample0 = scores;
        }
        else
        {
            meanAndSigma1 = (double[])objects[1];
            if( meanAndSigma0[0] > meanAndSigma1[0] )
            {
                Map<Integer, Object[]> mixtureWithNewOrder = new HashMap<>();
                mixtureWithNewOrder.put(0, mixture.get(1));
                mixtureWithNewOrder.put(1, mixture.get(0));
                mixtureWithNewOrder.put(2, mixture.get(2));
                mixture = mixtureWithNewOrder;
            }
            meanAndSigma0 = (double[])mixture.get(0)[1];
            meanAndSigma1 = (double[])mixture.get(1)[1];
            double probabilityOfSignal = (double)mixture.get(1)[0];
            vector = new double[]{probabilityOfSignal, sequences.length, meanAndSigma0[0], meanAndSigma0[1], meanAndSigma1[0], meanAndSigma1[1], meanAndSigma1[0] - 2.0 * meanAndSigma1[1], meanAndSigma0[0] + 2.0 * meanAndSigma0[1], meanAndSigma0[0] + 3.0 * meanAndSigma0[1]};
            subsample0 = (double[])mixture.get(0)[3];
            subsample1 = (double[])mixture.get(1)[3];
        }
        
        // 4. Identify sampleNameAndSample and nameAndMultipliers.
        Map<String, double[]> sampleNameAndSample = new HashMap<>();
        Map<String, Double> nameAndMultipliers = new HashMap<>();
        sampleNameAndSample.put("All scores", scores);
        nameAndMultipliers.put("All scores", 1.0);
        if( isSecondMixtureComponentIdentified )
        {
            sampleNameAndSample.put("Scores for signal-containing regions", subsample1);
            sampleNameAndSample.put("Scores for signal-not-containing regions", subsample0);
            nameAndMultipliers.put("Scores for signal-containing regions", (double)subsample1.length / (double)scores.length);
            nameAndMultipliers.put("Scores for signal-not-containing regions", (double)subsample0.length / (double)scores.length);
        }
        
        // 5. Identify vectorForRandom.
        double[] vectorForRandom = MatrixUtils.getConstantVector(2, Double.NaN);
        if( randomSequences != null )
        {
            double[] randomScores = new double[sequences.length];
            for( int i = 0; i < sequences.length; i++ )
                randomScores[i] = SiteModelsComparisonUtils.findBestSite(randomSequences[i], true, siteModel).getScore();
            sampleNameAndSample.put("Scores for random regions", randomScores);
            nameAndMultipliers.put("Scores for random regions", (double)subsample0.length / (double)scores.length);
            vectorForRandom = Stat.getMeanAndSigma(randomScores);
        }
        
        // 6. Create Chart and final version of vector.
        vector = ArrayUtils.addAll(vector, vectorForRandom);
        Chart chart = DensityEstimation.chartWithSmoothedDensities(sampleNameAndSample, "Scores", true, nameAndMultipliers, DensityEstimation.WINDOW_WIDTH_01, null);
        return new Object[]{vector, chart};
    }

    // For OPTION_06
    private void gtrdTracksAndHocomocoMatrices(DataElementPath pathToFolderWithFolders, Species givenSpecie, String[] foldersNames, DataElementPath pathToFolderWithMatrices, DataElementPath pathToOutputFolder)
    {
        // 1. Identification of distinctTracksNames and tfClasses.
        String[] distinctTracksNames = getDistinctTracksNamesinSeveralFolders(pathToFolderWithFolders, givenSpecie, foldersNames);
        log.info("number of distinctTracks = " + distinctTracksNames.length);
        String[] tfClasses = getTfClasses(pathToFolderWithFolders, foldersNames, distinctTracksNames);
        log.info("number of tfClasses = " + tfClasses.length);

        // 2. Identification of matrixNames and uniprotIds.
        DataCollection<DataElement> frequencyMatrices = pathToFolderWithMatrices.getDataCollection(DataElement.class);
        int n = frequencyMatrices.getSize(), index = 0;
        String[] matrixNames = new String[n], uniprotIds = new String[n];
        for( ru.biosoft.access.core.DataElement de: frequencyMatrices )
        {
            if( ! (de instanceof FrequencyMatrix) ) return;
            FrequencyMatrix frequencyMatrix = (FrequencyMatrix)de;
            matrixNames[index] = frequencyMatrix.getName();
            uniprotIds[index] = frequencyMatrix.getBindingElement().getFactors()[0].getName();
            log.info("matrixName = " + matrixNames[index] + " uniprotId = " + uniprotIds[index]);
            index++;
        }
        
        // 3. Identification of references (i.e. correspondence between tfClasses and uniprotIds).
        Set<String> set = new HashSet<>();
        for( String s : tfClasses )
            set.add(s);
        String[] distinctTfClasses = set.toArray(new String[0]);
        Properties input = BioHubSupport.createProperties("Homo sapiens", ReferenceTypeRegistry.getReferenceType(ProteinGTRDType.class));
        Properties output = BioHubSupport.createProperties("Homo sapiens", ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class));
        Map<String, String[]> references = BioHubRegistry.getReferences(distinctTfClasses, input, output, null);
        
        // 4. Calculate matrix (it contains tfClasses, uniprotIdsCorresponded, matrixNames).
        String[][] matrix = new String[distinctTracksNames.length][];
        for( int i = 0; i < distinctTracksNames.length; i++ )
        {
            matrix[i] = new String[]{tfClasses[i], null, null};
            String[] uniprotIdsCorresponded = references.get(tfClasses[i]);
            if( uniprotIdsCorresponded != null && uniprotIdsCorresponded.length == 1 )
            {
                int indx = ArrayUtils.indexOf(uniprotIds, uniprotIdsCorresponded[0]);
                if( indx >= 0 )
                    matrix[i] = new String[]{tfClasses[i], uniprotIdsCorresponded[0], matrixNames[indx]};
            }
            log.info("i = " + i + " tfClasses, uniprotIdsCorresponded, matrixNames = " + matrix[i][0] + " " + matrix[i][1] + " " + matrix[i][2]);
        }
        TableUtils.writeStringTable(matrix, distinctTracksNames, new String[]{"tfClass", "Uniprot_ID", MATRIX_NAME}, pathToOutputFolder.getChildPath("matrix_names_for_GTRD_tracks"));
    }
    
    // It is copied
    private String[] getTfClasses(DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] distinctTracksNames)
    {
        String[] result = new String[distinctTracksNames.length];
        for( int i = 0; i < distinctTracksNames.length; i++ )
            for( String name: foldersNames )
            {
                DataElementPath pathToTrack = pathToFolderWithFolders.getChildPath(name).getChildPath(distinctTracksNames[i]);
                if( pathToTrack.exists() )
                {
                    TrackInfo ti = new TrackInfo(pathToTrack.getDataElement(Track.class));
                    result[i] = ti.getTfClass();
                    break;
                }
            }
        return result;
    }
    
    private String[][] getTfClassesAndNames(DataElementPath pathToFolderWithFolders, String[] foldersNames, String[] distinctTracksNames)
    {
        String[][] result = new String[distinctTracksNames.length][];
        for( int i = 0; i < distinctTracksNames.length; i++ )
            for( String name: foldersNames )
            {
                DataElementPath pathToTrack = pathToFolderWithFolders.getChildPath(name).getChildPath(distinctTracksNames[i]);
                if( pathToTrack.exists() )               {
                    TrackInfo ti = new TrackInfo(pathToTrack.getDataElement(Track.class));
                    result[i] = new String[]{ti.getTfClass(), ti.getTfName()};
                    break;
                }
            }
        return result;
    }
    
    // For OPTION_05
    // it is copied to new TrackInfo !!!!!
    private String[] getDistinctTracksNamesinSeveralFolders(DataElementPath pathToFolderWithFolders, Species givenSpecie, String[] foldersNames)
    {
        Set<String> result = new HashSet<>();
        for( String folderName : foldersNames )
        {
            log.info("folderName = " + folderName);
            DataElementPath path = pathToFolderWithFolders.getChildPath(folderName);
            List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(path, givenSpecie, null, null);
            for( TrackInfo ti : trackInfos )
                result.add(ti.getTrackName());
        }
        return result.toArray(new String[0]);
    }


    // For OPTION_03
    private void getDataFromTracks(DataElementPath pathToFolderWithTracks, Species givenSpecie, DataElementPath pathToOutputFolder, int from, int to)
    {
        int difference = to - from;
        List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(pathToFolderWithTracks, givenSpecie, null, null);
        Track t = pathToFolderWithTracks.getChildPath(trackInfos.get(0).getTrackName()).getDataElement(Track.class);
        String[] propertiesNames = SequenceSampleUtils.getAvailablePropertiesNames(t);
        propertiesNames = (String[])ArrayUtils.add(propertiesNames, propertiesNames.length, "length");
        
        for( String s : propertiesNames )
            log.info("propertiesNames = " + s);
        
        String[][] data = new String[trackInfos.size()][8];
        double[][] doubleData = new double[trackInfos.size()][2 * propertiesNames.length + 1];
        String[] namesOfColumnsForStringMatrix = new String[]{"control_Id", "control_indicator", "cell_Line", "tf_Class", "tf_Name", "treatment", "treatment_indicator", "antibody"}, trackNames = new String[trackInfos.size()];
        for( int i = 0; i <  trackInfos.size(); i++ )
        {
            //1. Determine String[][] matrix
            jobControl.setPreparedness(from + (i + 1) * difference / trackInfos.size());
            TrackInfo ti =  trackInfos.get(i);
            trackNames[i] = ti.getTrackName();
            log.info("track name = " + trackNames[i]);
            data[i][0] = ti.getControlId();
            data[i][1] = data[i][0] != null && ! data[i][0].equals("") ? "Yes" : "No";
            data[i][2] = ti.getCellLine();
            data[i][3] = ti.getTfClass();
            data[i][4] = ti.getTfName();
            data[i][5] = ti.getTreatment();
            data[i][6] = data[i][5] != null && ! data[i][5].equals("") ? "Yes" : "No";
            data[i][7] = ti.getAntibody();
            
            // 2. Determine double[][] matrix
            Track track = pathToFolderWithTracks.getChildPath(trackNames[i]).getDataElement(Track.class);
            double[][] propertiesValues = getPropertiesValues(track, propertiesNames);
            for( int j = 0; j < propertiesNames.length; j++ )
            {
                double[] meanAndSigma = Stat.getMeanAndSigma(propertiesValues[j]);
                doubleData[i][2 * j] = meanAndSigma[0];
                doubleData[i][2 * j + 1] = meanAndSigma[1];
            }
            doubleData[i][2 * propertiesNames.length] = track.getAllSites().getSize();
            
            //////////////////////////
            String s = trackNames[i] + " : means and sigmas :";
            for( double x : doubleData[i] )
                s += " " + Double.toString(x);
            log.info(s);
            //////////////////////////
            
        }
        String[] namesOfColumnsForDoubleMatrix = new String[2 * propertiesNames.length + 1];
        for( int j = 0; j < propertiesNames.length; j++ )
        {
            namesOfColumnsForDoubleMatrix[2 * j] = propertiesNames[j] + "_mean";
            namesOfColumnsForDoubleMatrix[2 * j + 1] = propertiesNames[j] + "_sigma";
        }
        namesOfColumnsForDoubleMatrix[2 * propertiesNames.length] = "track_size";
        TableUtils.writeDoubleAndString(doubleData, data, trackNames, namesOfColumnsForDoubleMatrix, namesOfColumnsForStringMatrix, pathToOutputFolder, "propertiesInTracks");
    }
    
    private double[][] getPropertiesValues(Track track, String[] propertiesNames)
    {
        DataCollection<Site> sites = track.getAllSites();
        int size = sites.getSize();
        if( size < 1 ) return null;
        double[][] values = new double[propertiesNames.length + 1][size];
        int j = 0;
        for( Site site : sites )
        {
            for( int i = 0; i < propertiesNames.length; i++ )
            {
                if( propertiesNames[i].equals("length") )
                    values[i][j] = site.getLength();
                else
                {
                    DynamicPropertySet properties = site.getProperties();
                    String string = properties.getValueAsString(propertiesNames[i]);
                    values[i][j] = string != null ? Double.parseDouble(string) : -1.0;
                }
            }
            j++;
        }
        // DataMatrix dataMatrix = new DataMatrix(propertiesNamesExtended, null, values);
        return values;
    }

    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Select option (the concrete session of given analysis).";
        
        public static final String PN_DB_SELECTOR = "Sequences collection";
        public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
        
        public static final String PN_SPECIES = "Species";
        public static final String PD_SPECIES = "Select a taxonomical species";
        
        public static final String PN_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
        public static final String PD_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with (ChIP-seq) tracks";
        
        public static final String PN_PATH_TO_FOLDER_WITH_FILES = "Path to folder with files";
        public static final String PD_PATH_TO_FOLDER_WITH_FILES = "Path to folder with files that contain coverage matrices";
        
        public static final String PN_PATH_TO_FOLDER_WITH_TABLES = "Path to folder with tables";
        public static final String PD_PATH_TO_FOLDER_WITH_TABLES = "Path to folder with tables";
        
        public static final String PN_PATH_TO_FOLDER_WITH_MATRICES = "Path to folder with matrices";
        public static final String PD_PATH_TO_FOLDER_WITH_MATRICES = "Path to folder with frequency matrices";
        
        public static final String PN_PATH_TO_FOLDER_WITH_FOLDERS = "Path to folder with folders";
        public static final String PD_PATH_TO_FOLDER_WITH_FOLDERS = "Path to folder with folders (that contain (ChIP-seq) tracks)";
        
        public static final String PN_TRACKS_NAMES = "Tracks names";
        public static final String PD_TRACKS_NAMES = "Select tracks names";
        
        public static final String PN_FOLDERS_NAMES = "Folders names";
        public static final String PD_FOLDERS_NAMES = "Select folders names";

        public static final String PN_THRESHOLD_FOR_NUMBER_OF_SITES = "Track size threshold";
        public static final String PD_THRESHOLD_FOR_NUMBER_OF_SITES = "Track size threshold";
        
        public static final String PN_MINIMAL_LENGTH_OF_SITE = "Minimal length of site";
        public static final String PD_MINIMAL_LENGTH_OF_SITE = "Minimal length of site (or binding region)";
        
        public static final String PN_MAXIMAL_LENGTH_OF_SITE = "Maximal length of site";
        public static final String PD_MAXIMAL_LENGTH_OF_SITE = "Maximal length of site (or binding region)";
        
        public static final String PN_MINIMAL_NUMBER_OF_REGIONS = "Minimal number of regions";
        public static final String PD_MINIMAL_NUMBER_OF_REGIONS = "Minimal number of regions";
        
        public static final String PN_PATH_TO_INPUT_TABLE = "Path to input table";
        public static final String PD_PATH_TO_INPUT_TABLE = "Path to input table";

        public static final String PN_NAME_OF_COLUMN_FOR_CHROMOSOME_NAMES = "Name of column for chromosome names";
        public static final String PD_NAME_OF_COLUMN_FOR_CHROMOSOME_NAMES = "Select name of the table column for chromosome names";
        
        public static final String PN_NAME_OF_COLUMN_FOR_TSS_POSITIONS = "Name of column for TSS positions";
        public static final String PD_NAME_OF_COLUMN_FOR_TSS_POSITIONS = "Select name of the table column for TSS positions";
        
        public static final String PN_NAME_OF_COLUMN_FOR_TSS_STRANDS = "Name of {'+', '-'}-column for TSS strands";
        public static final String PD_NAME_OF_COLUMN_FOR_TSS_STRANDS = "Select name of the table column for TSS strands";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        
        public static final String PN_START_POSITION = "Start position of promoter region";
        public static final String PD_START_POSITION = "Start position of promoter region";
        
        public static final String PN_FINISH_POSITION = "Finish position of promoter region";
        public static final String PD_FINISH_POSITION = "Finish position of promoter region";
        
        public static final String PN_DO_REMOVE_ORPHANS = "Do remove orphans";
        public static final String PD_DO_REMOVE_ORPHANS = "Do remove orphans";
        
        public static final String PN_DO_EXTRACT_RENDOM_REGIONS = "Do extract random regions";
        public static final String PD_DO_EXTRACT_RENDOM_REGIONS = "Do extract random regions";
        
        public static final String PN_SITE_MODEL_TYPE = "Site model type";
        public static final String PD_SITE_MODEL_TYPE = "Select site model type";
          
        public static final String PN_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        public static final String PD_PARAMETERS_FOR_OPTION_01 = "Please, determine parameters for OPTION_01";
        
        public static final String PN_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_2";
        public static final String PD_PARAMETERS_FOR_OPTION_02 = "Please, determine parameters for OPTION_02";
        
        public static final String PN_PARAMETERS_FOR_OPTION_03 = "Parameters for OPTION_03";
        public static final String PD_PARAMETERS_FOR_OPTION_03 = "Please, determine parameters for OPTION_03";
        
        public static final String PN_PARAMETERS_FOR_OPTION_04 = "Parameters for OPTION_04";
        public static final String PD_PARAMETERS_FOR_OPTION_04 = "Please, determine parameters for OPTION_04";
        
        public static final String PN_PARAMETERS_FOR_OPTION_05 = "Parameters for OPTION_05";
        public static final String PD_PARAMETERS_FOR_OPTION_05 = "Please, determine parameters for OPTION_05";
        
        public static final String PN_PARAMETERS_FOR_OPTION_06 = "Parameters for OPTION_06";
        public static final String PD_PARAMETERS_FOR_OPTION_06 = "Please, determine parameters for OPTION_06";
        
        public static final String PN_PARAMETERS_FOR_OPTION_07 = "Parameters for OPTION_07";
        public static final String PD_PARAMETERS_FOR_OPTION_07 = "Please, determine parameters for OPTION_07";
        
        public static final String PN_PARAMETERS_FOR_OPTION_08 = "Parameters for OPTION_08";
        public static final String PD_PARAMETERS_FOR_OPTION_08 = "Please, determine parameters for OPTION_08";
        
        public static final String PN_PARAMETERS_FOR_OPTION_09 = "Parameters for OPTION_09";
        public static final String PD_PARAMETERS_FOR_OPTION_09 = "Please, determine parameters for OPTION_09";
    }
    
    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String option = OPTION_03;
        private BasicGenomeSelector dbSelector;
        private Species species = Species.getDefaultSpecies(null);
        private DataElementPath pathToFolderWithTracks;
        private DataElementPath pathToFolderWithFiles;
        private DataElementPath pathToFolderWithTables;
        private DataElementPath pathToFolderWithMatrices;
        private DataElementPath pathToFolderWithFolders;
        private String[] tracksNames;
        private String[] foldersNames;
        private int trackSizeThreshold = 500;
        private int minimalLengthOfSite = 20;
        private int maximalLengthOfSite = 300;
        private int minimalNumberOfRegions = 100;
        private DataElementPath pathToInputTable;
        private String nameOfColumnForChromosomeNames;
        private String nameOfColumnForTssPositions;
        private String nameOfColumnForTssStrands;
        private boolean doRemoveOrphans = true;
        private boolean doExtractRandomRegions = true;
        private String siteModelType = SiteModelsComparison.IPS_SITE_MODEL;
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_FILES)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_FILES)
        public DataElementPath getPathToFolderWithFiles()
        {
            return pathToFolderWithFiles;
        }
        public void setPathToFolderWithFiles(DataElementPath pathToFolderWithFiles)
        {
            Object oldValue = this.pathToFolderWithFiles;
            this.pathToFolderWithFiles = pathToFolderWithFiles;
            firePropertyChange("*", oldValue, pathToFolderWithFiles);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_TABLES)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_TABLES)
        public DataElementPath getPathToFolderWithTables()
        {
            return pathToFolderWithTables;
        }
        public void setPathToFolderWithTables(DataElementPath pathToFolderWithTables)
        {
            Object oldValue = this.pathToFolderWithTables;
            this.pathToFolderWithTables = pathToFolderWithTables;
            firePropertyChange("pathToFolderWithTables", oldValue, pathToFolderWithTables);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_MATRICES)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_MATRICES)
        public DataElementPath getPathToFolderWithMatrices()
        {
            return pathToFolderWithMatrices;
        }
        public void setPathToFolderWithMatrices(DataElementPath pathToFolderWithMatrices)
        {
            Object oldValue = this.pathToFolderWithMatrices;
            this.pathToFolderWithMatrices = pathToFolderWithMatrices;
            firePropertyChange("pathToFolderWithMatrices", oldValue, pathToFolderWithMatrices);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_FOLDERS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_FOLDERS)
        public DataElementPath getPathToFolderWithFolders()
        {
            return pathToFolderWithFolders;
        }
        public void setPathToFolderWithFolders(DataElementPath pathToFolderWithFolders)
        {
            Object oldValue = this.pathToFolderWithFolders;
            this.pathToFolderWithFolders = pathToFolderWithFolders;
            firePropertyChange("*", oldValue, pathToFolderWithFolders);
        }
        
        @PropertyName(MessageBundle.PN_TRACKS_NAMES)
        @PropertyDescription(MessageBundle.PD_TRACKS_NAMES)
        public String[] getTracksNames()
        {
            return tracksNames;
        }
        public void setTracksNames(String[] tracksNames)
        {
            Object oldValue = this.tracksNames;
            this.tracksNames = tracksNames;
            firePropertyChange("tracksNames", oldValue, tracksNames);
        }
        
        @PropertyName(MessageBundle.PN_FOLDERS_NAMES)
        @PropertyDescription(MessageBundle.PD_FOLDERS_NAMES)
        public String[] getFoldersNames()
        {
            return foldersNames;
        }
        public void setFoldersNames(String[] foldersNames)
        {
            Object oldValue = this.foldersNames;
            this.foldersNames = foldersNames;
            firePropertyChange("foldersNames", oldValue, foldersNames);
        }
        
        @PropertyName(MessageBundle.PN_THRESHOLD_FOR_NUMBER_OF_SITES)
        @PropertyDescription(MessageBundle.PD_THRESHOLD_FOR_NUMBER_OF_SITES)
        public int getTrackSizeThreshold()
        {
            return trackSizeThreshold;
        }
        public void setTrackSizeThreshold(int trackSizeThreshold)
        {
            Object oldValue = this.trackSizeThreshold;
            this.trackSizeThreshold = trackSizeThreshold;
            firePropertyChange("trackSizeThreshold", oldValue, trackSizeThreshold);
        }
        
        @PropertyName(MessageBundle.PN_MINIMAL_LENGTH_OF_SITE)
        @PropertyDescription(MessageBundle.PD_MINIMAL_LENGTH_OF_SITE)
        public int getMinimalLengthOfSite()
        {
            return minimalLengthOfSite;
        }
        public void setMinimalLengthOfSite(int minimalLengthOfSite)
        {
            Object oldValue = this.minimalLengthOfSite;
            this.minimalLengthOfSite = minimalLengthOfSite;
            firePropertyChange("minimalLengthOfSite", oldValue, minimalLengthOfSite);
        }
        
        @PropertyName(MessageBundle.PN_MAXIMAL_LENGTH_OF_SITE)
        @PropertyDescription(MessageBundle.PD_MAXIMAL_LENGTH_OF_SITE)
        public int getMaximalLengthOfSite()
        {
            return maximalLengthOfSite;
        }
        public void setMaximalLengthOfSite(int maximalLengthOfSite)
        {
            Object oldValue = this.maximalLengthOfSite;
            this.maximalLengthOfSite = maximalLengthOfSite;
            firePropertyChange("maximalLengthOfSite", oldValue, maximalLengthOfSite);
        }
        
        @PropertyName(MessageBundle.PN_MINIMAL_NUMBER_OF_REGIONS)
        @PropertyDescription(MessageBundle.PD_MINIMAL_NUMBER_OF_REGIONS)
        public int getMinimalNumberOfRegions()
        {
            return minimalNumberOfRegions;
        }
        public void setMinimalNumberOfRegions(int minimalNumberOfRegions)
        {
            Object oldValue = this.minimalNumberOfRegions;
            this.minimalNumberOfRegions = minimalNumberOfRegions;
            firePropertyChange("minimalNumberOfRegions", oldValue, minimalNumberOfRegions);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_INPUT_TABLE)
        @PropertyDescription(MessageBundle.PD_PATH_TO_INPUT_TABLE)
        public DataElementPath getPathToInputTable()
        {
            return pathToInputTable;
        }
        public void setPathToInputTable(DataElementPath pathToInputTable)
        {
            Object oldValue = this.pathToInputTable;
            this.pathToInputTable = pathToInputTable;
            firePropertyChange("pathToInputTable", oldValue, pathToInputTable);
        }

        @PropertyName(MessageBundle.PN_NAME_OF_COLUMN_FOR_CHROMOSOME_NAMES)
        @PropertyDescription(MessageBundle.PD_NAME_OF_COLUMN_FOR_CHROMOSOME_NAMES)
        public String getNameOfColumnForChromosomeNames()
        {
            return nameOfColumnForChromosomeNames;
        }
        public void setNameOfColumnForChromosomeNames(String nameOfColumnForChromosomeNames)
        {
            Object oldValue = this.nameOfColumnForChromosomeNames;
            this.nameOfColumnForChromosomeNames = nameOfColumnForChromosomeNames;
            firePropertyChange("*", oldValue, nameOfColumnForChromosomeNames);
        }
        
        @PropertyName(MessageBundle.PN_NAME_OF_COLUMN_FOR_TSS_POSITIONS)
        @PropertyDescription(MessageBundle.PD_NAME_OF_COLUMN_FOR_TSS_POSITIONS)
        public String getNameOfColumnForTssPositions()
        {
            return nameOfColumnForTssPositions;
        }
        public void setNameOfColumnForTssPositions(String nameOfColumnForTssPositions)
        {
            Object oldValue = this.nameOfColumnForTssPositions;
            this.nameOfColumnForTssPositions = nameOfColumnForTssPositions;
            firePropertyChange("*", oldValue, nameOfColumnForTssPositions);
        }
        
        @PropertyName(MessageBundle.PN_NAME_OF_COLUMN_FOR_TSS_STRANDS)
        @PropertyDescription(MessageBundle.PD_NAME_OF_COLUMN_FOR_TSS_STRANDS)
        public String getNameOfColumnForTssStrands()
        {
            return nameOfColumnForTssStrands;
        }
        public void setNameOfColumnForTssStrands(String nameOfColumnForTssStrands)
        {
            Object oldValue = this.nameOfColumnForTssStrands;
            this.nameOfColumnForTssStrands = nameOfColumnForTssStrands;
            firePropertyChange("*", oldValue, nameOfColumnForTssStrands);
        }
        
        @PropertyName(MessageBundle.PN_DO_REMOVE_ORPHANS)
        @PropertyDescription(MessageBundle.PD_DO_REMOVE_ORPHANS)
        public boolean getDoRemoveOrphans()
        {
            return doRemoveOrphans;
        }
        public void setDoRemoveOrphans(boolean doRemoveOrphans)
        {
            Object oldValue = this.doRemoveOrphans;
            this.doRemoveOrphans = doRemoveOrphans;
            firePropertyChange("doRemoveOrphans", oldValue, doRemoveOrphans);
        }
      
        @PropertyName(MessageBundle.PN_DO_EXTRACT_RENDOM_REGIONS)
        @PropertyDescription(MessageBundle.PD_DO_EXTRACT_RENDOM_REGIONS)
        public boolean getDoExtractRandomRegions()
        {
            return doExtractRandomRegions;
        }
        public void setDoExtractRandomRegions(boolean doExtractRandomRegions)
        {
            Object oldValue = this.doExtractRandomRegions;
            this.doExtractRandomRegions = doExtractRandomRegions;
            firePropertyChange("doExtractRandomRegions", oldValue, doExtractRandomRegions);
        }
        
        @PropertyName(MessageBundle.PN_SITE_MODEL_TYPE)
        @PropertyDescription(MessageBundle.PD_SITE_MODEL_TYPE)
        public String getSiteModelType()
        {
            return siteModelType;
        }
        public void setSiteModelType(String siteModelType)
        {
            Object oldValue = this.siteModelType;
            this.siteModelType = siteModelType;
            firePropertyChange("siteModelType", oldValue, siteModelType);
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
            add("dbSelector");
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
            add("trackSizeThreshold");
            add(DataElementPathEditor.registerInput("pathToInputTable", beanClass, TableDataCollection.class, true));
            add(new PropertyDescriptorEx("nameOfColumnForChromosomeNames", beanClass), ColumnNameSelector.class);
            add(new PropertyDescriptorEx("nameOfColumnForTssPositions", beanClass), ColumnNameSelector.class);
            add(new PropertyDescriptorEx("nameOfColumnForTssStrands", beanClass), ColumnNameSelector.class);
            add("promoterRegions");
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
            add(DataElementPathEditor.registerInputChild("pathToFolderWithFiles", beanClass, FolderCollection.class, true));
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
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
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
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
            add("tracksNames", TracksNamesSelector.class);
            add("minimalLengthOfSite");
            add("maximalLengthOfSite");
        }
    }
    
    public static class ParametersForOption05 extends AllParameters
    {}
    
    public static class ParametersForOption05BeanInfo extends BeanInfoEx2<ParametersForOption05>
    {
        public ParametersForOption05BeanInfo()
        {
            super(ParametersForOption05.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
            add("foldersNames", FoldersNamesSelectorForOption05.class);
            add("minimalLengthOfSite");
            add("maximalLengthOfSite");
        }
    }
    
    public static class ParametersForOption06 extends AllParameters
    {}
    
    public static class ParametersForOption06BeanInfo extends BeanInfoEx2<ParametersForOption06>
    {
        public ParametersForOption06BeanInfo()
        {
            super(ParametersForOption06.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(DataElementPathEditor.registerOutput("pathToFolderWithFolders", beanClass, FolderCollection.class, true));
            add("foldersNames", FoldersNamesSelectorForOption06.class);
            add(DataElementPathEditor.registerInputChild("pathToFolderWithMatrices", beanClass, FrequencyMatrix.class, true));
        }
    }
    
    public static class ParametersForOption07 extends AllParameters
    {}
    
    public static class ParametersForOption07BeanInfo extends BeanInfoEx2<ParametersForOption07>
    {
        public ParametersForOption07BeanInfo()
        {
            super(ParametersForOption07.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInput("pathToInputTable", beanClass, TableDataCollection.class, true));
            add(DataElementPathEditor.registerInputChild("pathToFolderWithMatrices", beanClass, FrequencyMatrix.class, true));
            add(new PropertyDescriptorEx("siteModelType", beanClass), SiteModelTypeEditor.class);
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTables", beanClass, TableDataCollection.class, true));
            add("doRemoveOrphans");
            add("doExtractRandomRegions");
            add("minimalNumberOfRegions");
        }
    }
    
    public static class ParametersForOption08 extends AllParameters
    {}
    
    public static class ParametersForOption08BeanInfo extends BeanInfoEx2<ParametersForOption08>
    {
        public ParametersForOption08BeanInfo()
        {
            super(ParametersForOption08.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInput("pathToInputTable", beanClass, TableDataCollection.class, true));
            add(DataElementPathEditor.registerInputChild("pathToFolderWithMatrices", beanClass, FrequencyMatrix.class, true));
//            add(DataElementPathEditor.registerInputChild("pathToFolderWithTables", beanClass, TableDataCollection.class, true));
//            add("doRemoveOrphans");
//            add("minimalNumberOfRegions");
        }
    }

    public static class ParametersForOption09 extends AllParameters
    {}
    
    public static class ParametersForOption09BeanInfo extends BeanInfoEx2<ParametersForOption09>
    {
        public ParametersForOption09BeanInfo()
        {
            super(ParametersForOption09.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInput("pathToInputTable", beanClass, TableDataCollection.class, true));
            add(DataElementPathEditor.registerInputChild("pathToFolderWithMatrices", beanClass, FrequencyMatrix.class, true));
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTables", beanClass, TableDataCollection.class, true));
            add("doRemoveOrphans");
            add("minimalNumberOfRegions");
        }
    }
    
    public static class SiteModelTypeEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return SiteModelsComparison.getSiteModelTypes();
        }
    }

    public static class ColumnNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                TableDataCollection table = ((ParametersForOption01)getBean()).getPathToInputTable().getDataElement(TableDataCollection.class);
                return table.columns().map(TableColumn::getName).toArray(String[]::new);
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table doesn't contain the columns)"};
            }
        }
    }
    
    public static class TracksNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> tracks = ((ParametersForOption04)getBean()).getPathToFolderWithTracks().getDataCollection(DataElement.class);
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
    
    public static class FoldersNamesSelectorForOption05 extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> folders = ((ParametersForOption05)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
                String[] foldersNames = folders.getNameList().toArray(new String[0]);
                Arrays.sort(foldersNames, String.CASE_INSENSITIVE_ORDER);
                return foldersNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with folders)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the folders)"};
            }
        }
    }
    
    public static class FoldersNamesSelectorForOption06 extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> tracks = ((ParametersForOption06)getBean()).getPathToFolderWithFolders().getDataCollection(DataElement.class);
                String[] foldersNames = tracks.getNameList().toArray(new String[0]);
                Arrays.sort(foldersNames, String.CASE_INSENSITIVE_ORDER);
                return foldersNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with folders)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the folders)"};
            }
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
    
    public class SiteModelTypesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return SiteModelsComparison.getSiteModelTypes();
        }
    }
    
    public static String[] getAvailableOptions()
    {
        return new String[]{OPTION_03, OPTION_05, OPTION_06, OPTION_07, OPTION_08, OPTION_09, OPTION_10};
    }
    
    public static class GtrdAnalysisParameters extends AllParameters
    {
        ParametersForOption01 parametersForOption01;
        ParametersForOption02 parametersForOption02;
        ParametersForOption03 parametersForOption03;
        ParametersForOption04 parametersForOption04;
        ParametersForOption05 parametersForOption05;
        ParametersForOption06 parametersForOption06;
        ParametersForOption07 parametersForOption07;
        ParametersForOption08 parametersForOption08;
        ParametersForOption09 parametersForOption09;
        
        public GtrdAnalysisParameters()
        {
            setParametersForOption01(new ParametersForOption01());
            setParametersForOption02(new ParametersForOption02());
            setParametersForOption03(new ParametersForOption03());
            setParametersForOption04(new ParametersForOption04());
            setParametersForOption05(new ParametersForOption05());
            setParametersForOption06(new ParametersForOption06());
            setParametersForOption07(new ParametersForOption07());
            setParametersForOption08(new ParametersForOption08());
            setParametersForOption09(new ParametersForOption09());
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
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_05)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_05)
        public ParametersForOption05 getParametersForOption05()
        {
            return parametersForOption05;
        }
        public void setParametersForOption05(ParametersForOption05 parametersForOption05)
        {
            Object oldValue = this.parametersForOption05;
            this.parametersForOption05 = parametersForOption05;
            firePropertyChange("parametersForOption05", oldValue, parametersForOption05);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_06)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_06)
        public ParametersForOption06 getParametersForOption06()
        {
            return parametersForOption06;
        }
        public void setParametersForOption06(ParametersForOption06 parametersForOption06)
        {
            Object oldValue = this.parametersForOption06;
            this.parametersForOption06 = parametersForOption06;
            firePropertyChange("parametersForOption06", oldValue, parametersForOption06);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_07)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_07)
        public ParametersForOption07 getParametersForOption07()
        {
            return parametersForOption07;
        }
        public void setParametersForOption07(ParametersForOption07 parametersForOption07)
        {
            Object oldValue = this.parametersForOption07;
            this.parametersForOption07 = parametersForOption07;
            firePropertyChange("parametersForOption07", oldValue, parametersForOption07);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_08)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_08)
        public ParametersForOption08 getParametersForOption08()
        {
            return parametersForOption08;
        }
        public void setParametersForOption08(ParametersForOption08 parametersForOption08)
        {
            Object oldValue = this.parametersForOption08;
            this.parametersForOption08 = parametersForOption08;
            firePropertyChange("parametersForOption08", oldValue, parametersForOption08);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_09)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_09)
        public ParametersForOption09 getParametersForOption09()
        {
            return parametersForOption09;
        }
        public void setParametersForOption09(ParametersForOption09 parametersForOption09)
        {
            Object oldValue = this.parametersForOption09;
            this.parametersForOption09 = parametersForOption09;
            firePropertyChange("parametersForOption09", oldValue, parametersForOption09);
        }

        public boolean isParametersForOption01Hidden()
        {
            return true;
        }
        
        public boolean isParametersForOption02Hidden()
        {
            return true;
        }
        
        public boolean isParametersForOption03Hidden()
        {
            return( ! getOption().equals(OPTION_03) );
        }
        
        public boolean isParametersForOption04Hidden()
        {
            return(true);
        }
        
        public boolean isParametersForOption05Hidden()
        {
            return( ! getOption().equals(OPTION_05) );
        }
        
        public boolean isParametersForOption06Hidden()
        {
            return( ! getOption().equals(OPTION_06) );
        }
        
        public boolean isParametersForOption07Hidden()
        {
            return( ! getOption().equals(OPTION_07) );
        }
        
        public boolean isParametersForOption08Hidden()
        {
            return( ! getOption().equals(OPTION_08) );
        }
        
        public boolean isParametersForOption09Hidden()
        {
            return( ! getOption().equals(OPTION_09) );
        }
    }

    public static class GtrdAnalysisParametersBeanInfo extends BeanInfoEx2<GtrdAnalysisParameters>
    {
        public GtrdAnalysisParametersBeanInfo()
        {
            super(GtrdAnalysisParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            addHidden("parametersForOption01", "isParametersForOption01Hidden");
            addHidden("parametersForOption02", "isParametersForOption02Hidden");
            addHidden("parametersForOption03", "isParametersForOption03Hidden");
            addHidden("parametersForOption04", "isParametersForOption04Hidden");
            addHidden("parametersForOption05", "isParametersForOption05Hidden");
            addHidden("parametersForOption06", "isParametersForOption06Hidden");
            addHidden("parametersForOption07", "isParametersForOption07Hidden");
            addHidden("parametersForOption08", "isParametersForOption08Hidden");
            addHidden("parametersForOption09", "isParametersForOption09Hidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}
