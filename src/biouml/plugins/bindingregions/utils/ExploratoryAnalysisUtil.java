/* $Id$ */

package biouml.plugins.bindingregions.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.utils.CistromUtils.CistromConstructor;
import biouml.plugins.gtrd.utils.EnsemblUtils;
//import biouml.plugins.gtrd.TrackSqlTransformer;
//import biouml.model.dynamics.Connection;
import biouml.plugins.gtrd.utils.FunSite;
import biouml.plugins.gtrd.utils.FunSiteUtils;
import biouml.plugins.gtrd.utils.FunSiteUtils.CombinedSites;
import biouml.plugins.gtrd.utils.MetaClusterConsrtruction;
import biouml.plugins.gtrd.utils.SiteModelUtils;
import biouml.plugins.gtrd.utils.SiteModelUtils.RocCurve;
import biouml.plugins.gtrd.utils.SiteModelUtils.SiteModelComposed;
import biouml.plugins.gtrd.utils.SiteUtils;
import biouml.plugins.gtrd.utils.SiteUtils.TransfacSites;
import biouml.plugins.gtrd.utils.TrackInfo;
import biouml.plugins.gtrd.utils.TrackUtils;
import biouml.plugins.machinelearning.classification_models.ClassificationModel;
import biouml.plugins.machinelearning.classification_models.FisherDiscriminantModel;
import biouml.plugins.machinelearning.clustering_models.KmeansModel;
import biouml.plugins.machinelearning.distribution_mixture.NormalMixture;
import biouml.plugins.machinelearning.regression_models.LinearRegressionModel;
import biouml.plugins.machinelearning.regression_models.OrdinaryLeastSquaresRegressionModel;
import biouml.plugins.machinelearning.regression_models.RegressionModel;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrix.DataMatrixConstructor;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixStringConstructor;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.MetaAnalysis.Homogeneity;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.ModelUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.StudentDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.RandomUtils;
import biouml.plugins.machinelearning.utils.StatUtils.SimilaritiesAndDissimilarities;
import biouml.plugins.machinelearning.utils.StatUtils.StatisticalTests.ChiSquaredIndependenceTestForTwoDimensionContingencyTable;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample.DensityEstimation;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSamples;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.Distance;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;
//import biouml.standard.type.Cell;
import biouml.standard.type.Species;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.jobcontrol.JobControl;
//import ru.biosoft.util.ListUtil;
import ru.biosoft.util.TextUtil;

/**
 * @author yura
 *
 */
public class ExploratoryAnalysisUtil
{
    /*****************************************************************************************/
    /********************************************** Quality control **************************/
    /*****************************************************************************************/
    // For quality control article; was used in GtrdAnalysisAdvanced
    public static void getPriorities(DataMatrix dmWithFncms, String[] columnNames, DataElementPath pathToOutputFolder, String tableName)
    {
        // String[] columnNames = new String[]{"FNCM_gem_", "FNCM_macs2_", "FNCM_pics_", "FNCM_sissrs_"};
        DataMatrix dm = dmWithFncms.getSubDataMatrixColumnWise(columnNames);
        double[][] matrix = dm.getMatrix();

                String[] array = new String[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        {
            double[] vector = matrix[i].clone();
            int[] positions = Util.sortHeap(vector);
            StringBuilder builder = new StringBuilder();                
            for( int j = 0; j < positions.length; j++ )
                builder.append(columnNames[positions[j]]).append("_");
            array[i] = builder.toString();
        }
        Object[] objects = PrimitiveOperations.countFrequencies(array);
        dm = new DataMatrix((String[])objects[0], "frequencies", UtilsForArray.transformIntToDouble((int[])objects[1]));
        dm.sortByColumn(0);
        dm.writeDataMatrix(false, pathToOutputFolder, tableName, log);
    }
    
    public static void qualityMetricsComparison(DataElementPath pathToOutputFolder)
    {
        DataElementPath path1 = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Quality_control_01/Merged_macs_FPCM_2.0/quality_control_metrics"),
                        path2 = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Quality_control_01/Overlap_macs2_FPCM_2.0_extended/quality_control_metrics");
        DataMatrix dm1 = new DataMatrix(path1, null), dm2 = new DataMatrix(path2, null);
        String[] columnNames = dm2.getColumnNames();
        for( String s : columnNames )
            dm2.replaceColumnName(s, s + "_");
        DataMatrix dm = DataMatrix.mergeDataMatricesColumnWise(dm1, dm2);
        log.info("dim(dm) = " + dm.getSize());
        dm.removeRowsWithMissingData();
        log.info("dim(dm after removing) = " + dm.getSize());

        // 1. Individual FPCM", "FPCM_", "FPCM2_"
        for( String s : new String[]{"FPCM", "FPCM_", "FPCM2_"} )
        {
            double[] column = dm.getColumn(s);
            double[] meanAndSigma = UnivariateSample.getMeanAndSigma(column);
            log.info("column = " + s + " mean = " + meanAndSigma[0] + " sigma = " + meanAndSigma[1]);

            for( double threshold : new double[]{1.5, 2.0, 3.0} )
            {
                int count = 0;
                for( double x : column )
                    if( x < threshold )
                        count++;
                log.info("column = " + s + " threshold = " + threshold + " freq = " + (double)count / (double)dm.getSize());
            }
        }
            
        // 2. FPCM and new FPCM
        double[] fpcm = dm.getColumn("FPCM"), fpcmNew = dm.getColumn("FPCM_");
        int count = 0;
        for( int i = 0; i < fpcm.length; i++ )
            if( fpcmNew[i] < fpcm[i] )
                count++;
        log.info(" # (fpcmNew[i] < fpcm[i]) = " + count + " " + (double)count / (double)fpcm.length);
        
        // 3. Priorities.
        getPriorities(dm, new String[]{"FNCM_gem", "FNCM_macs", "FNCM_pics", "FNCM_sissrs"}, pathToOutputFolder, "priorities");
        getPriorities(dm, new String[]{"FNCM_gem_", "FNCM_macs2_", "FNCM_pics_", "FNCM_sissrs_"}, pathToOutputFolder, "priorities_new");
        
        // 4. FNCM and new FNCM
        for( String[] names : new String[][]{new String[]{"FNCM_gem", "FNCM_gem_"}, new String[]{"FNCM_macs", "FNCM_macs2_"}, new String[]{"FNCM_pics", "FNCM_pics_"}, new String[]{"FNCM_sissrs", "FNCM_sissrs_"}} )
        {
            DataMatrix datMat = dm.getSubDataMatrixColumnWise(names);
            double[] column1 = datMat.getColumn(names[0]), column2 = datMat.getColumn(names[1]);
            count = 0;
            for( int i = 0; i < column1.length; i++ )
                if( column2[i] > column1[i] )
                    count++;
            log.info(" # (" + names[1] + " > " + names[0] + ") = " + count);
        }
        
        // 5. Data for selection of ROC-curves.
        String[] rowNames = dm.getRowNames();
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"FPCM", "FPCM_"});
        for( int i = 0; i < fpcm.length; i++ )
        {
            if( fpcm[i] > 4.0 && fpcmNew[i] < 1.3 )
                dmc.addRow(rowNames[i], new double[]{fpcm[i], fpcmNew[i]});
        }
        DataMatrix datMat = dmc.getDataMatrix();
        datMat.writeDataMatrix(false, pathToOutputFolder, "fpcm_and_new_fpcm", log);
        
        // 6. ROC-curve construction.
        String[] trackNames = new String[]{"PEAKS039451"
                
//                "PEAKS033567", "PEAKS034898", "PEAKS046703",
//                "PEAKS038243", "PEAKS046652",
                
                };
        String[] siteModelNames = new String[]{"ANDR_HUMAN.H11MO.0.A"
//                "ETS1_HUMAN.H11MO.0.A", "TAL1_HUMAN.H11MO.0.A", "ETS1_HUMAN.H11MO.0.A",
//                "REST_HUMAN.H11MO.0.A", "STAT1_HUMAN.H11MO.0.A",
                
                };
        for( int i = 0; i < trackNames.length; i++ )
            getRocCurvesAndAucs(trackNames[i], siteModelNames[i], pathToOutputFolder);
    }
    
    // Modified version of method from QualityControlAnalysis.
    private static void getRocCurvesAndAucs(String trackName, String siteModelName, DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD/Data/peaks");
        DataElementPath pathToFolderWithSiteModels = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001");
        DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");
        String combinedPeakType1 = CombinedSites.SITE_TYPE_MERGED, combinedPeakType2 = CombinedSites.SITE_TYPE_OVERLAPPED;
        String[] foldersNames1 = new String[]{"macs", "gem", "pics", "sissrs"}, foldersNames2  = new String[]{"macs2", "gem", "pics", "sissrs"};
        int minimalLengthOfPeaks = 20, maximalLengthOfPeaks1 = 300, maximalLengthOfPeaks2 = 1000000;
        boolean doFromDataMatrixToDataMatrices = true;
        
        SiteModel siteModel = pathToFolderWithSiteModels.getChildPath(siteModelName).getDataElement(SiteModel.class);
        int w = 100, lengthOfSequenceRegion = w + siteModel.getLength();
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
        double[][] xValuesForCurves = new double[2][], yValuesForCurves = new double[2][];
        double[] aucs = new double[2];
        for( int i = 0; i < 2; i++ )
        {
            String combinedPeakType = i == 0 ? combinedPeakType1 : combinedPeakType2;
            String[] foldersNames = i == 0 ? foldersNames1 : foldersNames2;
            int maximalLengthOfPeaks = i == 0 ? maximalLengthOfPeaks1 : maximalLengthOfPeaks2;
            CombinedSites css = new CombinedSites(combinedPeakType, pathToFolderWithFolders, foldersNames, trackName, minimalLengthOfPeaks, maximalLengthOfPeaks, doFromDataMatrixToDataMatrices);
            FunSite[] funSites = css.getCombinedSites();
            funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
            Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
            log.info("trackName = " + trackName + " number of sequences = " + sequences.length);
            RocCurve rocCurve = new RocCurve(smc, sequences, 10, 0);
            double[][] curve  = rocCurve.getRocCurve();
            xValuesForCurves[i] = curve[0];
            yValuesForCurves[i] = curve[1];
            aucs[i] = rocCurve.getAuc();
            log.info("i = " + i + " AUC = " + aucs[i]);
        }
        DataMatrix dm = new DataMatrix(new String[]{"Merging with MACS", "Overlapping with MACS2"}, "AUC", aucs);
        dm.writeDataMatrix(false, pathToOutputFolder, "AUCs_" + trackName, log);
        Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, new String[]{"Merging with MACS", "Overlapping with MACS2"}, null, null, null, null, "Specificity", "Sensitivity", true);
        TableAndFileUtils.addChartToTable("chart with ROC-curve", chart, pathToOutputFolder.getChildPath("_chart_with_ROC_curve_" + trackName));
    }
    
    /*****************************************************************************************/
    /********************************************** Rank aggregation *************************/
    /*****************************************************************************************/
    
    public static void fromBedFileToTrack(DataElementPath pathToInputFile, String name, DataElementPath pathToOutputFolder, String trackName)
    {
        Map<String, List<FunSite>> map = FunSiteUtils.readSitesInBedFile(pathToInputFile, 0, 1, 2, null, null, name, 0, 1000000000);
        FunSite[] funSites = FunSiteUtils.transformToArray(map);
        FunSiteUtils.writeSitesToSqlTrack(funSites, null, null, pathToOutputFolder, trackName);
    }
    
    public static void analysisOfSiteMotifsAndRaScores(DataElementPath pathToInputFolder, String[] fileNames, DataElementPath pathToOutputFolder)
    {
        // 1. Initialize some parameters.
        DataElementPath pathToSiteModel = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001/JUN_HUMAN.H11MO.0.A");
        DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");
        int lengthOfSequenceRegion = 80, numberOfGroups = 10;
        double[] siteThresholds = new double[]{3.913, 5.22518, 7.21091};
        
        // 2. Create SiteModelComposed
        SiteModel siteModel = pathToSiteModel.getDataElement(SiteModel.class);
        double threshold = siteModel.getThreshold();
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);

        // 3. Analysis of files.
        for( String fileName : fileNames )
        {
            // 3.1. Read funSites in file and create sequence sample.
            log.info("fileName = " + fileName);
            FunSite[] funSites = readFunSitesInFile(pathToInputFolder.getChildPath(fileName));
            funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
            
            
            
            Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
            log.info("number of sequences = " + sequences.length);
            
            // TODO: Create smc.findBestScoresWithReplacement(sequences)
            // 3.2. Calculate dataMatrix with raScores and siteScores and sort it.
            double[] siteScores = smc.findBestScores(sequences);
            for( int i = 0; i < siteScores.length; i++ )
                if( Double.isNaN(siteScores[i]) )
                    siteScores[i] = threshold;
            DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{RankAggregation.RA_SCORE, "Site_score", "Indicator"});
            for( int i = 0; i < funSites.length; i++ )
            {
                DataMatrix dm = funSites[i].getDataMatrix();
                dmc.addRow("S_" + Integer.toString(i), new double[]{dm.getColumn(RankAggregation.RA_SCORE)[0], siteScores[i], dm.getColumn("Indicator")[0]});                
            }
            DataMatrix dm = dmc.getDataMatrix();
            dm.sortByColumn(0);
            dm = addNewFeatures(dm);
            dm.writeDataMatrix(true, pathToOutputFolder, fileName + "_data_matrix", log);
            
            // TODO: temp
            siteScores = dm.getColumn("Site_score");
            double[] raScores = dm.getColumn(RankAggregation.RA_SCORE);
            double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(raScores, siteScores);
            log.info("corr = " + corr);
            
            // 3.3. Calculate dataMatrix with results: site counts for different thresholds, and Average_site_score_in_group, Aaverage_RA-score_in_group.
            String[] columnNames = new String[siteThresholds.length];
            for( int i = 0; i < siteThresholds.length; i++ )
                columnNames[i] = "Site_score_threshold_" + Double.toString(siteThresholds[i]);
            columnNames = (String[])ArrayUtils.addAll(columnNames, new String[]{"Average_site_score_in_group", "Aaverage_RA-score_in_group"});
            dmc = new DataMatrixConstructor(columnNames);
            int h = funSites.length / numberOfGroups;
            for( int i = 0; i < numberOfGroups; i++ )
            {
                int startIndex = i * h, endIndex = i < numberOfGroups - 1 ? (i + 1) * h : funSites.length;
                DataMatrix datMat = dm.getSubDataMatrixRowWise(startIndex, endIndex);
                siteScores = datMat.getColumn("Site_score");
                double[] row = new double[siteThresholds.length];
                for( int ii = 0; ii < siteThresholds.length; ii++ )
                    for( int j = 0; j < siteScores.length; j++ )
                        if( siteScores[j] >= siteThresholds[ii] )
                            row[ii] += 1.0;
                double averageSiteScore = PrimitiveOperations.getAverage(siteScores), averageRaScore = PrimitiveOperations.getAverage(datMat.getColumn(RankAggregation.RA_SCORE));
                row = VectorOperations.getProductOfVectorAndScalar(row, 1.0 / (double)siteScores.length);
                row = ArrayUtils.addAll(row, new double[]{averageSiteScore, averageRaScore});
                dmc.addRow("Group_" + Integer.toString(i), row);
            }
            dm = dmc.getDataMatrix();
            dm.writeDataMatrix(false, pathToOutputFolder, fileName + "_motifs_and_RA_scores", log);
        }
    }
    
    private static DataMatrix addNewFeatures(DataMatrix dataMatrix)
    {
        double[] siteScores = dataMatrix.getColumn("Site_score"), raScores = dataMatrix.getColumn(RankAggregation.RA_SCORE);
        String[] rowNames = dataMatrix.getRowNames();
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Site_score_squared", RankAggregation.RA_SCORE + "_squared", "Site_score_x_" + RankAggregation.RA_SCORE});
        for( int i = 0; i < siteScores.length; i++ )
            dmc.addRow(rowNames[i], new double[]{siteScores[i] * siteScores[i], raScores[i] * raScores[i], siteScores[i] * raScores[i]});
        DataMatrix dm = dmc.getDataMatrix();
        dm = DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dm, dataMatrix});
        return dm;
    }
    
    private static FunSite[] readFunSitesInFile(DataElementPath pathToFile)
    {
        DataMatrix dm = new DataMatrix(pathToFile, new String[]{"RA-score", "Indicator"});
        //DataMatrixString dms = new DataMatrixString(pathToFile, new String[]{"Indicator"});
        String[] rowNames = dm.getRowNames();
        double[] scores = dm.getColumn(0), indicators = dm.getColumn(1);
        FunSite[] result = new FunSite[rowNames.length];
        for( int i = 0; i < result.length; i++ )
        {
            String[] strings = TextUtil.split(rowNames[i], ':');
            String chromosomeName = strings[0].substring(3);
            strings = TextUtil.split(strings[1], '-');
            Interval coordinates = new Interval(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
            result[i] = new FunSite(chromosomeName, coordinates, 0, new DataMatrix(Integer.toString(i), new String[]{RankAggregation.RA_SCORE, "Indicator"}, new double[]{scores[i], indicators[i]}));
            result[i].setObjects(new Object[]{indicators[i]});
        }
        return result;
    }
    
    /*****************************************************************************************/
    /************************************** Expression intensity paper ***********************/
    /*****************************************************************************************/
    
    public static void crossValidationOfForwardFeatureSelection(DataElementPath pathToOutputFolder, JobControl jobControl)
    {
        // 1.
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended");
        String[] columnNamesForRemoving = new String[]{"Cell_1425_lg", "Cell_1426_lg", "mean_intensity_lg_cell line", "mean_intensity_lg_cell line_predicted_by_K562_HepG2_GM12878_MCF7_HEK293"};
        String responseName = "Cell_1424_lg", cellLine = "HepG2";
        log.info("cellLine = " + cellLine);
        DataElementPath path = pathToOutputFolder.getChildPath(cellLine);
        DataCollectionUtils.createFoldersForPath(path.getChildPath(""));
        crossValidationOfForwardFeatureSelection(pathToDataMatrix, columnNamesForRemoving, responseName, path, jobControl);
        
        // 2.
        pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HEK293_with_QC_3.0_macs2_overlap/data_matrix_extended_2");
        columnNamesForRemoving = new String[]{"mean_intensity_lg_cell line", "mean_intensity_lg_cell line_predicted_by_K562_HepG2_GM12878_MCF7_HEK293"};
        responseName = "Cell_1356";
        cellLine = "HEK293";
        log.info("cellLine = " + cellLine);
        path = pathToOutputFolder.getChildPath(cellLine);
        DataCollectionUtils.createFoldersForPath(path.getChildPath(""));
        crossValidationOfForwardFeatureSelection(pathToDataMatrix, columnNamesForRemoving, responseName, path, jobControl);
        
        // 3.
        pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/K562_with_QC_3.0_macs2_overlap/data_matrix_extended");
        columnNamesForRemoving = new String[]{"Cell_1328", "Cell_1329", "Cell_1330", "mean_intensity_lg_cell line", "mean_intensity_lg_cell line_predicted_by_K562_HepG2_GM12878_MCF7_HEK293"};
        responseName = "Cell_1327";
        cellLine = "K562";
        log.info("cellLine = " + cellLine);
        path = pathToOutputFolder.getChildPath(cellLine);
        DataCollectionUtils.createFoldersForPath(path.getChildPath(""));
        crossValidationOfForwardFeatureSelection(pathToDataMatrix, columnNamesForRemoving, responseName, path, jobControl);
    }

    private static void crossValidationOfForwardFeatureSelection(DataElementPath pathToDataMatrix, String[] columnNamesForRemoving, String responseName, DataElementPath pathToOutputFolder, JobControl jobControl)
    {
        // 1. Read data matrix and response.
        String[] variableNames = TableAndFileUtils.getColumnNames(pathToDataMatrix);
        for( String s : columnNamesForRemoving )
            variableNames = (String[])ArrayUtils.removeElement(variableNames, s);
        String regressionType = RegressionModel.REGRESSION_1_OLS;
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, variableNames);
        if( LinearRegressionModel.doAddInterceptToRegression(regressionType) )
        {
            int interceptIndex = dataMatrix.getColumnNames().length;
            dataMatrix.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dataMatrix.getSize(), 1.0), interceptIndex);
        }
        DataMatrix dm = new DataMatrix(pathToDataMatrix, new String[]{responseName});
        double[] response = dm.getColumn(responseName);
        response = dataMatrix.removeRowsWithMissingData(response);
        dataMatrix.removeColumn(responseName);
        
        // 2. Create regression model for cross-validation.
        int percentageOfDataForTraining = 50, numberOfSelectedVariables = 20;
        Object[] objects = ModelUtils.splitDataSet(dataMatrix, response, response.length * percentageOfDataForTraining / 100, 0), additionalInputParameters = null;
        DataMatrix dataMatrixForFit = (DataMatrix)objects[0], dataMatrixForTest = (DataMatrix)objects[1];
        double[] responseForFit = (double[])objects[2], responseForTest = (double[])objects[3];
        dm = ModelUtils.stepwiseForwardVariableSelectionInRegression(regressionType, responseName, responseForFit, dataMatrixForFit, numberOfSelectedVariables, ModelUtils.PEARSON_CORRELATION_CRITERION, additionalInputParameters, false, jobControl, 0, 100);
        String[] selectedVariablesNames = dm.getRowNames();
        dm = dataMatrixForFit.getSubDataMatrixColumnWise(selectedVariablesNames);
        RegressionModel regressionModel = RegressionModel.createModel(regressionType, responseName, responseForFit, dm, additionalInputParameters, true);
        regressionModel.saveModel(pathToOutputFolder);
        log.info("Fit regression model");
        
        // 3. Test regression model for cross-validation.
        double[] predictedResponse = regressionModel.predict(dm);
        DataMatrix accuracySummary = ModelUtils.getSummaryOnModelAccuracy(responseForFit, predictedResponse, dm.getColumnNames().length);
        accuracySummary.replaceColumnName("Value", "Training set");
        dm = dataMatrixForTest.getSubDataMatrixColumnWise(selectedVariablesNames);
        predictedResponse = regressionModel.predict(dm);
        DataMatrix accuracySummaryForTest = ModelUtils.getSummaryOnModelAccuracy(responseForTest, predictedResponse, dm.getColumnNames().length);
        accuracySummaryForTest.replaceColumnName("Value", "Test set");
        accuracySummary.addAnotherDataMatrixColumnWise(accuracySummaryForTest);
        accuracySummary.writeDataMatrix(false, pathToOutputFolder, "Cross_validation_accuracy", log);
        log.info("Test regression model");
    }
    
    public static void correlationAnalysisForFeatures(DataElementPath pathToOutputFolder)
    {
        int numberOfSelectedCorrelations = 10;
        // 1.
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended");
        String[] columnNamesForRemoving = new String[]{"Cell_1425_lg", "Cell_1426_lg", "mean_intensity_lg_cell line", "mean_intensity_lg_cell line_predicted_by_K562_HepG2_GM12878_MCF7_HEK293"};
        String responseName = "Cell_1424_lg", cellLine = "HepG2";
        log.info("cellLine = " + cellLine);
        
        // 2. Read data matrix and response.
        String[] variableNames = TableAndFileUtils.getColumnNames(pathToDataMatrix);
        for( String s : columnNamesForRemoving )
            variableNames = (String[])ArrayUtils.removeElement(variableNames, s);
        String regressionType = RegressionModel.REGRESSION_1_OLS;
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, variableNames);
        if( LinearRegressionModel.doAddInterceptToRegression(regressionType) )
        {
            int interceptIndex = dataMatrix.getColumnNames().length;
            dataMatrix.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dataMatrix.getSize(), 1.0), interceptIndex);
        }
        DataMatrix dm = new DataMatrix(pathToDataMatrix, new String[]{responseName});
        double[] response = dm.getColumn(responseName);
        response = dataMatrix.removeRowsWithMissingData(response);
        dataMatrix.removeColumn(responseName);
        
        // 3. Correlation between features.
        dataMatrix.transpose();
        variableNames = dataMatrix.getRowNames();
        double[][] matrix = dataMatrix.getMatrix();
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"the_most_correlated_features"});
        for( int i = 0; i < variableNames.length; i++ )
        {
            log.info("i = " + i);
            //dataMatrix.sortByColumn(columnIndex);
            DataMatrixConstructor dmcLocal = new DataMatrixConstructor(new String[]{"correlation"});
            for( int j = 0; j < variableNames.length; j++ )
                if( i != j )
                {
                    double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(matrix[i], matrix[j]);
                    if( ! Double.isNaN(corr) )
                        dmcLocal.addRow(variableNames[j], new double[]{});
                }
            dm = dmcLocal.getDataMatrix();
            dm.sortByColumn(0);
            double[] corrs = dm.getColumn(0);
            String[] names = dm.getRowNames();
            String row = "";
            for( int j = 0; j < numberOfSelectedCorrelations; j++ )
                row += ":" + names[names.length - 1 - j] + "," + Double.toString(corrs[names.length - 1 - j]);
            dmsc.addRow(variableNames[i], new String[]{row});
        }
        DataMatrixString dms = dmsc.getDataMatrixString();
        dms.writeDataMatrixString(true, pathToOutputFolder, "the_most_correlated_features", log);
    }
    
    private static class Feature
    {
        String feature, tfClass, startAndEnd;
        public Feature(String feature)
        {
            this.feature = feature;
            String[] array = TextUtil.split(feature, '_');
            int n = array.length;
            if( n == 3 )
                tfClass = array[0];
            startAndEnd = "[" + array[n - 2] + ", " + array[n - 1] + "]";
            
            log.info("feature = " + feature + " tfClass = " + tfClass + " startAndEnd = " + startAndEnd);////////////////

        }
        
        public String getCanonicalFeature(String[]distinctTfClasses, String[] distinctTfClassesNames)
        {
            String result = tfClass == null ? "Abundance" : distinctTfClassesNames[ArrayUtils.indexOf(distinctTfClasses, tfClass)];
            return result + startAndEnd;
        }
        
        public String getTfClass()
        {
            return tfClass;
        }
        
        public String getFeature()
        {
            return feature;
        }

        public static Feature[] getFeatures(String[] array)
        {
            Feature[] result = new Feature[array.length];
            for( int i = 0; i < array.length; i++ )
                result[i] = new Feature(array[i]);
            return result;
        }

        public static String[] getDistinctTfClasses(Feature[] features)
        {
            Set<String> set = new HashSet<>();
            for( Feature feature : features )
            {
                String tfClass = feature.getTfClass();
                if( tfClass != null )
                    set.add(tfClass);
            }
          return set.toArray(new String[0]);
        }
        
        public static String[] getDistinctTfClasses(Feature[][] features)
        {
            Set<String> set = new HashSet<>();
            for( int i = 0; i < features.length; i++ )
                for( int j = 0; j < features[i].length; j++ )
                {
                    String tfClass = features[i][j].getTfClass();
                    if( tfClass != null )
                        set.add(tfClass);
                }
            return set.toArray(new String[0]);
        }
    }
    
    public static void correlationAnalysisForFeaturesStep2(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToImportantFeatures = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/HepG2/Regression_coefficients");
        DataElementPath pathToFileWithMostCorrelatedFeatures = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB03_correlations_between_features/Ans01/the_most_correlated_features");
        DataElementPath pathToTableWithTfNames = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/summary_on_chip_seq_tracks");

        // 1. importantFeatures
        String[] rowNames = TableAndFileUtils.getRowNamesInTable(pathToImportantFeatures);
        rowNames = (String[])ArrayUtils.removeElement(rowNames, "mean_intensity_lg_cell line_predicted_by_K562_HepG2_GM12878_MCF7_HEK293");
        Feature[] importantFeatures = Feature.getFeatures(rowNames);
        String[] distinctTfClassesInImportantFeatures = Feature.getDistinctTfClasses(importantFeatures);
        
        // 2. correlatedFeatures; correlatedFeatures
        int numberOfCorrelatedFeatures = 5;
        DataMatrixString dms = new DataMatrixString(pathToFileWithMostCorrelatedFeatures, new String[]{"the_most_correlated_features"});
        rowNames = dms.getRowNames();
        String[] column = dms.getColumn(0);
        Feature[][] correlatedFeatures = new Feature[importantFeatures.length][numberOfCorrelatedFeatures];
        // double[][] corr = new double[importantFeatures.length][numberOfCorrelatedFeatures];
        String[][] corr = new String[importantFeatures.length][numberOfCorrelatedFeatures];
        for( int i = 0; i < importantFeatures.length; i++ )
        {
            log.info("i = " + i + " importantFeatures = " + importantFeatures[i].getFeature());
            String name = importantFeatures[i].getFeature();
            int index = ArrayUtils.indexOf(rowNames, name);
            String line = column[index];
            //StringUtils.remove(line, ":I");
            line = line.replace(":I", "II");
            String[] array = TextUtil.split(line, ':');
            for( int j = 0; j < numberOfCorrelatedFeatures; j++ )
            {
                String[] arrayFeatures = TextUtil.split(array[j + 1], ',');
                correlatedFeatures[i][j] = new Feature(arrayFeatures[0]);
                // corr[i][j] = Double.parseDouble(arrayFeatures[1]);
                corr[i][j] = StringUtils.substring(arrayFeatures[1], 0, 5);
            }
        }
        
        // 3. distinctTfClasses
        String[] distinctTfClassesInCorrelatedFeatures = Feature.getDistinctTfClasses(correlatedFeatures);
        String[] distinctTfClasses = (String[])ArrayUtils.addAll(distinctTfClassesInImportantFeatures, distinctTfClassesInCorrelatedFeatures);
        
        // 4. distinctTfClassesNames
        dms = new DataMatrixString(pathToTableWithTfNames, new String[]{"TF-class", "TF-name"});
        String[] tfClasses = dms.getColumn("TF-class"), tfNames = dms.getColumn("TF-name");
        String[] distinctTfClassesNames = new String[distinctTfClasses.length];
        for( int i = 0; i < distinctTfClasses.length; i++ )
        {
            int index = ArrayUtils.indexOf(tfClasses, distinctTfClasses[i]);
            
            log.info("i = " + " distinctTfClasses = " + distinctTfClasses[i] + " index = " + index); //////////////////
            
            distinctTfClassesNames[i] = tfNames[index];
            
            log.info("i = " + distinctTfClassesNames[i]); //////////////////

        }
        
        // 5.
        String[] columnNames = new String[2 * numberOfCorrelatedFeatures];
        for( int i = 0; i < numberOfCorrelatedFeatures; i++ )
        {
            columnNames[2 * i] = "Feature_" + Integer.toString(i);
            columnNames[2 * i + 1] = "Correlation_" + Integer.toString(i);
        }
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(columnNames);
        for( int i = 0; i < importantFeatures.length; i++ )
        {
            String[] row = new String[2 * numberOfCorrelatedFeatures];
            for( int j = 0; j < numberOfCorrelatedFeatures; j++ )
            {
                row[2 * j] = correlatedFeatures[i][j].getCanonicalFeature(distinctTfClasses, distinctTfClassesNames);
                row[2 * j + 1] = corr[i][j];
            }
            dmsc.addRow(importantFeatures[i].getCanonicalFeature(distinctTfClasses, distinctTfClassesNames), row);
        }
        dms = dmsc.getDataMatrixString();
        dms.writeDataMatrixString(true, pathToOutputFolder, "correlated_features", log);
    }

    
    public static void correlationBetween20MostImportantAnd20Next(DataElementPath pathToOutputFolder)
    {
        // 1.
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended");
        DataElementPath path1 = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans00_prediction_without_mean/HepG2_2/selected_variables");
        DataElementPath path2 = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB01_regression_without_most_important/selected_variables");
        DataMatrix dataMatrix = new DataMatrix(path1, null);
        String[] featureNames1 = dataMatrix.getRowNames();
        dataMatrix = new DataMatrix(path2, null);
        String[] featureNames2 = dataMatrix.getRowNames();
        dataMatrix = new DataMatrix(pathToDataMatrix, featureNames1);
        DataMatrix dm = new DataMatrix(pathToDataMatrix, featureNames2);
        
        // 2.
        for( int i = 0; i < featureNames2.length; i++ )
        {
            log.info("featureNames2 = " + featureNames2[i]);
            DataElementPath path = pathToOutputFolder.getChildPath(featureNames2[i]);
            DataCollectionUtils.createFoldersForPath(path.getChildPath(""));
            double[] response = dm.getColumn(featureNames2[i]);
            OrdinaryLeastSquaresRegressionModel regressionModel = (OrdinaryLeastSquaresRegressionModel)RegressionModel.createModel(RegressionModel.REGRESSION_1_OLS, featureNames2[i], response, dataMatrix, null, true);
            regressionModel.saveModel(path);
            //regressionModel
        }
    }
    
    public  static void implementPolinomialRegression()
    {
        DataElementPath[] deps = new DataElementPath[]
        		{
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/summary_on_tracks"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment_/summary_on_tracks"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/summary_on_tracks"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/summary_on_tracks"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/summary_on_tracks"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/summary_on_tracks"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/summary_on_tracks"),
        		};
        DataElementPath[] pathToOutputFolder = new DataElementPath[]
        		{
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Regression_04"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment_/Regression_04"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/Regression_04"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/Regression_04"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Regression_04"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Regression_04"),
                 DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Regression_04"),
        		};

        for( int i = 0; i < deps.length; i++ )
        {
            log.info("i = " + i + " dep = " + deps[i]);
        	implementPolinomialRegression(deps[i], pathToOutputFolder[i]);
        }
    }
    
    public  static void implementPolinomialRegression(DataElementPath pathToDataMatrix, DataElementPath pathToOutputFolder)
    {
        String[] columnNames = new String[]{"IMETARA step", "Number of meta-clusters identified ing given step", "Number of meta-clusters after given step", "Percentage of new meta-clusters after given step"};
        String responseName = "Percentage of new meta-clusters after given step", variableName = "IMETARA step";
        int regressionPower = 2;

        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, columnNames);
        double[] response = dataMatrix.getColumn(responseName);
        
        //////// temp : for lg(response) !!!!
//        for( int i = 0; i < response.length; i++ )
//        esponse[i] = Math.log10(response[i]);
//        responseName = "lg(" + responseName + ")";

        
        double[] variable = dataMatrix.getColumn(variableName);
        
        //////// temp : for lg(variable) !!!!
        for( int i = 0; i < variable.length; i++ )
        	variable[i] = variable[i] < 0.99 ? 0.0 : Math.log10(variable[i]);
        variableName = "lg(" + variableName + ")";

        implementPolinomialRegression(responseName, response, variableName, variable, regressionPower, pathToOutputFolder);
    }
    
    
    public  static void rab_temporary(DataElementPath pathToOutputFolder)
    {
        String responseName = "Percentage of new meta-clusters after given step", variableName = "IMETARA step";
        int regressionPower = 2;
    	double[] variable = new double[]{0, 1.0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
    			25, 26, 27, 28, 29, 30,
    			31, 32, 33, 34, 35, 36, 37, 38, 39, 40};
        double[] response = new double[]{100, 83.15, 97.82, 27.75, 42.88, 83.2, 28.45, 22.57, 25.9, 87.78, 76.25, 69.72, 36.35, 35.89, 12.54, 15.88, 19.01, 56.53, 11.75, 9.653, 8.199, 5.867, 9.628, 8.976,
        								 9, 6, 12, 8, 4, 11,
        								 35, 61, 54, 48, 73, 38, 60, 55, 71, 80};

    	//chart	[[{"data":[[0,100],[1,83.15],[2,97.82],[3,27.75],[4,42.88],[5,83.2],[6,28.45],[7,22.57],[8,25.9],[9,87.78],
        //[10,76.25],[11,69.72],[12,36.35],[13,35.89],[14,12.54],[15,15.88],[16,19.01],[17,56.53],[18,11.75],[19,9.653],[20,8.199],
        //[21,5.867],[22,9.628],[23,8.976]],"color":"rgb(0,0,255)","lines":{"isShapesVisible":true,"show":false}},{"data":[[0,78],[23,3.314]],"color":"rgb(255,0,0)"}],{"xaxis":{"label":"IMETARA step"},"yaxis":{"label":"Percentage of new meta-clusters after given step"}}]
        implementPolinomialRegression(responseName, response, variableName, variable, regressionPower, pathToOutputFolder);

    	
    }

    
    public  static void implementPolinomialRegression(String responseName, double[] response, String variableName, double[] variable, int regressionPower, DataElementPath pathToOutputFolder)
    {
    	int dimForCurve = 100;
        DataElementPath path = pathToOutputFolder.getChildPath("");
    	// 1. Calculation of matrix
        String[] columnNames = new String[1 + regressionPower], rowNames = new String[response.length];
        columnNames[0] = ModelUtils.INTERCEPT;
        double[][] matrix = new double[response.length][];
        for( int i = 0; i < response.length; i++ )
        {
        	double[] row = new double[1 + regressionPower];
        	row[0] = 1.0;
            for( int j = 1; j <= regressionPower; j++ )
            	row[j] = Math.pow(variable[i], (double)j);
        	matrix[i] = row;
        }
        
    	// 2. Calculation of columnNames, rowNames and dataMatrix
        for( int i = 0; i < regressionPower; i++ )
        	columnNames[i + 1] = variableName + "_**_" + Integer.toString(i + 1);
        for( int i = 0; i < response.length; i++ )
        	rowNames[i] = "Row_" + Integer.toString(i);
        DataMatrix dataMatrix = new DataMatrix(rowNames, columnNames, matrix);

        // 3. Create and write regression model.
        // RegressionModel.createModel(String regressionType, String responseName, double[] response, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformationWhenFit)
        OrdinaryLeastSquaresRegressionModel regressionModel = (OrdinaryLeastSquaresRegressionModel)RegressionModel.createModel(RegressionModel.REGRESSION_1_OLS, responseName, response, dataMatrix, null, true);
        regressionModel.saveModel(path);
        
        // 4. Create chart and write it.
        // private static Chart createChartForSimpleOlsRegression(String responseName, double[] response, String variableName, double[] variable)
        //Chart chart = OrdinaryLeastSquaresRegressionModel.createChartForSimpleOlsRegression(responseName, response, variableName, variable);
        //double[] prediction = regressionModel.predict(matrix);
        //Chart chart = ChartUtils.createChart(minAndMax, prediction, null, variable, response, null, null, variableName, responseName, true);
        
        double[] minAndMax = PrimitiveOperations.getMinAndMax(variable);
        double[] xValuesForCurve = new double[dimForCurve];
        double w = (minAndMax[1] - minAndMax[0]) / (double)dimForCurve;
        for( int i = 0; i < dimForCurve; i++ )
        	xValuesForCurve[i] = minAndMax[0] + w * (double)i;
        
        double[][] mat = new double[xValuesForCurve.length][];
        for( int i = 0; i < xValuesForCurve.length; i++ )
        {
        	double[] row = new double[1 + regressionPower];
        	row[0] = 1.0;
            for( int j = 1; j <= regressionPower; j++ )
            	row[j] = Math.pow(xValuesForCurve[i], (double)j);
        	mat[i] = row;
        }
        double[] yValuesForCurve = regressionModel.predict(mat);
        double[] minAndMaxForY = PrimitiveOperations.getMinAndMax(response);
        double[] minAndMaxForXandY = new double[]{minAndMax[0], minAndMax[1], minAndMaxForY[0], minAndMaxForY[1]};
//        public static Chart createChart(double[] xValuesForCurve, double[] yValuesForCurve, String curveName, double[] xValuesForCloud, double[] yValuesForCloud, String cloudName, double[] minAndMaxForXandY, String xName, String yName, boolean doRecalculateCurve);
        Chart chart = ChartUtils.createChart(xValuesForCurve, yValuesForCurve, null, variable, response, null, minAndMaxForXandY, variableName, responseName, true);
        TableAndFileUtils.addChartToTable("chart with predictions", chart, pathToOutputFolder.getChildPath("_chart_with_predictions"));
    }

    /// TEMP!!!!!!!!!!!!! : temporary copy from OrdinaryLeastSquaresRegressionModel !!!!
 // simple linear regression y = a + b * x;
//    public static Chart createChartForSimpleOlsRegression(String responseName, double[] response, String variableName, double[] variable)
//    {
//        double[][] matrix = new double[variable.length][];
//        for( int i = 0; i < variable.length; i++ )
//            matrix[i] = new double[]{1.0, variable[i]};
//        DataMatrix dataMatrix = new DataMatrix(null, new String[]{ModelUtils.INTERCEPT, variableName}, matrix);
//        OrdinaryLeastSquaresRegressionModel olsRegressionModel = new OrdinaryLeastSquaresRegressionModel(responseName, response, dataMatrix, new Object[]{3, 1.0E-10}, false);
//        double[] minAndMax = PrimitiveOperations.getMinAndMax(variable);
//        matrix = new double[][]{new double[]{1.0, minAndMax[0]}, new double[]{1.0, minAndMax[1]}};
//        double[] prediction = olsRegressionModel.predict(matrix);
//        return ChartUtils.createChart(minAndMax, prediction, null, variable, response, null, null, variableName, responseName, true);
//    }
    

    
    
    
    public static void differenceBetweenCells(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended");
        DataElementPath pathHek = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HEK293_with_QC_3.0_macs2_overlap/data_matrix_extended");
        DataElementPath pathK562 = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/K562_with_QC_3.0_macs2_overlap/data_matrix_extended");
        
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, null);
        double[] hepg2 = dataMatrix.getColumn("Cell_1424_lg");
        DataMatrix dm = new DataMatrix(pathHek, new String[]{"Cell_1356"});
        double[] intensities = dm.getColumn(0);
        double[] x = VectorOperations.getSubtractionOfVectors(hepg2, intensities);
        dm = new DataMatrix(pathK562, new String[]{"Cell_1327"});
        intensities = dm.getColumn(0);
        double[] xx = VectorOperations.getSubtractionOfVectors(hepg2, intensities);
        String[] rowNames = dataMatrix.getRowNames();
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Cell_1424_lg - HEK293", "Cell_1424_lg - K562"});
        for( int i = 0; i < rowNames.length; i++ )
            dmc.addRow(rowNames[i], new double[]{x[i], xx[i]});
        dm = dmc.getDataMatrix();
        dataMatrix.addAnotherDataMatrixColumnWise(dm);
        dataMatrix.writeDataMatrix(true, pathToOutputFolder, "data_matix_HepG2_HEK293_K562", log);
    }
    
    public static void reconstructPvalues(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Machine_learning/RAB/RAB_RAB/Rab03/rab1");
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, null);
        double[][] matrix = dataMatrix.getMatrix(), pValues = new double[matrix.length][matrix[0].length];
        double degrees = (double)(209911 - 20);
        for( int i = 0; i < matrix.length; i++ )
            for( int j = 0; j < matrix[0].length; j++ )
                pValues[i][j] = StudentDistribution.getPvalueForAbsStudent(Math.abs(matrix[i][j]), degrees, 80);
        DataMatrix dm = new DataMatrix(dataMatrix.getRowNames(), dataMatrix.getColumnNames(), pValues);
        dm.writeDataMatrix(true, pathToOutputFolder, "p_values", log);
    }
    
    public static void constructMatrixFor3rdStepOfAdvancedRegression(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB07_advanced_model_K562_second_round/data_matrix_for_advanced_2_round");
        DataElementPath pathToCoefficients = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB07_advanced_model_K562_second_round/Regression01/Regression_coefficients");
        DataElementPath pathToPredictions = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB07_advanced_model_K562_second_round/Regression01/Response_predicted___");
        
        // 1. Remove features identified at 2-nd step.
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, null), dm = new DataMatrix(pathToCoefficients, null);
        String[] featureNames = dataMatrix.getColumnNames(), coefficientNames = dm.getRowNames();
        for( int i = 0; i < coefficientNames.length; i++ )
            featureNames = (String[])ArrayUtils.removeElement(featureNames, coefficientNames[i]);
        dm = dataMatrix.getSubDataMatrixColumnWise(featureNames);
        
        // 2. Add prediction obtained at 2-nd step.
        dataMatrix = new DataMatrix(pathToPredictions, new String[]{"Cell_1327_predicted_2nd_round"});
        dm.addAnotherDataMatrixColumnWise(dataMatrix);
        dm.writeDataMatrix(true, pathToOutputFolder, "data_matrix_for_advanced_3_round", log);
    }
    
    public static void constructMatrixFor2ndStepOfAdvancedRegression()
    {
        log.info("HEK293");
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HEK293_with_QC_3.0_macs2_overlap/data_matrix_extended_2");
        DataElementPath pathToCoefficients = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/HEK293/Regression_coefficients");
        DataElementPath pathToPredictions = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/HEK293/Response_predicted_");
        DataElementPath pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB07_advanced_model_HEK293_second_round");
        
        // 1. Remove features identified at 1-st step.
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, null), dm = new DataMatrix(pathToCoefficients, null);
        String[] featureNames = dataMatrix.getColumnNames(), coefficientNames = dm.getRowNames();
        for( int i = 0; i < coefficientNames.length; i++ )
            featureNames = (String[])ArrayUtils.removeElement(featureNames, coefficientNames[i]);
        dm = dataMatrix.getSubDataMatrixColumnWise(featureNames);
        
        // 2. Add prediction obtained at 1-st step.
        dataMatrix = new DataMatrix(pathToPredictions, new String[]{"Cell_1356_predicted"});
        dm.addAnotherDataMatrixColumnWise(dataMatrix);
        dm.writeDataMatrix(true, pathToOutputFolder, "data_matrix_for_advanced_2_round", log);
        log.info("HEK293: O.K.");

        log.info("K562");
        //
        pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/K562/data_matrix");
        pathToCoefficients = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/K562/Regression_coefficients");
        pathToPredictions = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/K562/Response_predicted__");
        pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB07_advanced_model_K562_second_round");
        
        // 3. Remove features identified at 1-st step.
        dataMatrix = new DataMatrix(pathToDataMatrix, null);
        dm = new DataMatrix(pathToCoefficients, null);
        featureNames = dataMatrix.getColumnNames();
        coefficientNames = dm.getRowNames();
        for( int i = 0; i < coefficientNames.length; i++ )
            featureNames = (String[])ArrayUtils.removeElement(featureNames, coefficientNames[i]);
        dm = dataMatrix.getSubDataMatrixColumnWise(featureNames);
        
        // 4. Add prediction obtained at 1-st step.
        dataMatrix = new DataMatrix(pathToPredictions, new String[]{"Cell_1327_predicted"});
        dm.addAnotherDataMatrixColumnWise(dataMatrix);
        dm.writeDataMatrix(true, pathToOutputFolder, "data_matrix_for_advanced_2_round", log);
        log.info("K562: O.K.");
    }
    
 // TODO: temporary; only for article
    public static void createIntensityMatrix(DataElementPath pathToInputFolder, DataElementPath pathToOutputFolder)
    {
        String[] fileNames = pathToInputFolder.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
        DataMatrix dm = new DataMatrix(pathToInputFolder.getChildPath(fileNames[0]), new String[]{"TPM_lg"});
        String[] rowNames = dm.getRowNames(), columnNames = new String[fileNames.length];
        double[][] matrix = new double[rowNames.length][fileNames.length];
        for( int i = 0; i < fileNames.length; i++ )
        {
            log.info("i = " + i + " fileNames[i] = " + fileNames[i]);
            dm = new DataMatrix(pathToInputFolder.getChildPath(fileNames[i]), new String[]{"TPM_lg"});
            columnNames[i] = TextUtil.split(fileNames[i], '_')[0];
            MatrixUtils.fillColumn(matrix, dm.getColumn(0), i);
        }
        dm = new DataMatrix(rowNames, columnNames, matrix);
        dm.writeDataMatrix(true, pathToOutputFolder, "Intensities_lg_RNA_seq", log);
    }
    
    public static void cellLineCorrelation(DataElementPath pathToFileWithIntensities, DataElementPath pathToOutputFolder)
    {
        // 0. indices
        DataElementPath path = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/RNA_seq/ENCODE_26_cell_lines/DATA01_processed/A172_transcripts_ENCFF252PPX_ENCSR580GSX.tsv_processed");
        DataMatrixString dms = new DataMatrixString(path, new String[]{"transcript_type"});
        String[] column = dms.getColumn(0);
        List<Integer> list = new ArrayList<>();
        for( int i = 0; i < column.length; i++ )
            if( column[i].equals("protein_coding") )
                list.add(i);
        int[] indices = UtilsGeneral.fromListIntegerToArray(list);
        
        // 1. All cell lines
        DataMatrix dm = new DataMatrix(pathToFileWithIntensities, null);
        dm = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, indices)[0];
        dm.transpose();
        double[][] matrix = dm.getMatrix();
        String[] cellLineNames = dm.getRowNames();
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"correlations"});
        for( int i = 1; i < cellLineNames.length; i++ )
            for( int j = 0; j < i; j++ )
            {
                double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(matrix[i], matrix[j]);
                dmc.addRow(cellLineNames[i] + "_" + cellLineNames[j], new double[]{corr});
            }
        DataMatrix mat = dmc.getDataMatrix();
        double[] correlations = mat.getColumn("correlations");
        log.info("All cell line");
        
        // 2. Cell lines without GM12878
        dm = new DataMatrix(pathToFileWithIntensities, null);
        dm = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, indices)[0];
        dm.removeColumn("GM12878");
        dm.transpose();
        matrix = dm.getMatrix();
        cellLineNames = dm.getRowNames();
        dmc = new DataMatrixConstructor(new String[]{"correlations"});
        for( int i = 1; i < cellLineNames.length; i++ )
            for( int j = 0; j < i; j++ )
            {
                double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(matrix[i], matrix[j]);
                dmc.addRow(cellLineNames[i] + "_" + cellLineNames[j], new double[]{corr});
            }
        mat = dmc.getDataMatrix();
        double[] correlations2 = mat.getColumn("correlations");
        log.info("Cell lines without GM12878");
        
        // 3.
        String[] sampleNames = new String[]{"26 cell lines", "25 cell lines"};
        double[][] samples = new double[][]{correlations, correlations2};
        UnivariateSamples us = new UnivariateSamples(sampleNames, samples);
        mat = us.getSimpleCharacteristicsOfSamples();
        log.info("Simple characteristics of correlation samples are calculated");
        mat.writeDataMatrix(false, pathToOutputFolder, "correlation_samples_characteristics", log);
        Chart chart = us.createChartWithSmoothedDensities("Correlation coefficient", false, DensityEstimation.WINDOW_WIDTH_01, null);
        TableAndFileUtils.addChartToTable("Densities of correlations", chart, pathToOutputFolder.getChildPath("_chart_correlation_densities"));
        log.info("Chart with correlation densities is created");
    }
    
    public static void represorAnalysis(DataElementPath pathToOutputFolder)
    {
        String[] featureNames = new String[]{"2.3.1.2.10_-100_0", "2.3.1.2.10_501_1000",
                "1.2.4.1.8_-100_0", "1.2.4.1.8_-5000_-1001", "1.2.4.1.8_-1000_-501", "1.2.4.1.8_-500_-201", "1.2.4.1.8_-200_-101",
                "1.2.4.1.8_1_100", "1.2.4.1.8_101_500", "1.2.4.1.8_501_1000"};
        DataElementPath path = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended");
        DataMatrix dm = new DataMatrix(path, featureNames);
        getStatistic("2.3.1.2.10_-100_0", "2.3.1.2.10_501_1000", dm);
        getStatistic("1.2.4.1.8_-100_0", "1.2.4.1.8_-5000_-1001", dm);
        getStatistic("1.2.4.1.8_-100_0", "1.2.4.1.8_-1000_-501", dm);
        getStatistic("1.2.4.1.8_-100_0", "1.2.4.1.8_-500_-201", dm);
        getStatistic("1.2.4.1.8_-100_0", "1.2.4.1.8_-200_-101", dm);
        getStatistic("1.2.4.1.8_-100_0", "1.2.4.1.8_1_100", dm);
        getStatistic("1.2.4.1.8_-100_0", "1.2.4.1.8_101_500", dm);
        getStatistic("1.2.4.1.8_-100_0", "1.2.4.1.8_501_1000", dm);
    }
    
    private static void getStatistic(String featureName1, String featureName2, DataMatrix dm)
    {
        double[] feature1 = dm.getColumn(featureName1), feature2 = dm.getColumn(featureName2);
        int n = dm.getSize(), n11 = 0, n1 = 0, n2 = 0;
        for( int i = 0; i < n; i++ )
        {
            if( feature1[i] > 0.1 && feature2[i] > 0.1 )
                n11++;
            if( feature1[i] > 0.1 )
                n1++;
            if( feature2[i] > 0.1 )
                n2++;
        }
        double pobs = (double)n11 / (double)n, pexp = (double)n1  * (double)n2 / ( (double)n * (double)n), ratio = pobs / pexp;
        log.info("feature1 = " + featureName1 + " feature = " + featureName2 + " observed probability = " + pobs + " expected probability = " + pexp
                + " ratio = " + ratio);
    }
    
    public static void represorAnalysis2(DataElementPath pathToFileWithIntensities, DataElementPath pathToOutputFolder)
    {
        String[] featureNames = new String[]{"2.3.1.2.10_-100_0", "2.3.1.2.10_501_1000",
                "1.2.4.1.8_-100_0", "1.2.4.1.8_-5000_-1001", "1.2.4.1.8_-1000_-501", "1.2.4.1.8_-500_-201", "1.2.4.1.8_-200_-101",
                "1.2.4.1.8_1_100", "1.2.4.1.8_101_500", "1.2.4.1.8_501_1000"};
        DataElementPath path = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended");
        DataMatrix dm = new DataMatrix(path, featureNames);
        DataMatrix datMat = new DataMatrix(path, new String[]{"Cell_1424_lg"});
        double[] response = datMat.getColumn(0);

        for( int i = 0; i < featureNames.length; i++ )
        {
            log.info("i = " + i + " featureNames = " + featureNames[i]);
            getStatistic(featureNames[i], response, dm);
        }
    }
    
    private static void getStatistic(String featureName, double[] response, DataMatrix dm)
    {
        double[] feature = dm.getColumn(featureName);
        List<Double> list1 = new ArrayList<>(), list2 = new ArrayList<>();
        for( int i = 0; i < feature.length; i++ )
        {
            if(feature[i] >0.1 )
                list1.add(response[i]);
            else
                list2.add(response[i]);
        }
        String[] sampleNames = new String[]{"Presence", "Absence"};
        double[][] samples = new double[][]{UtilsGeneral.fromListToArray(list1), UtilsGeneral.fromListToArray(list2)};
        UnivariateSamples us = new UnivariateSamples(sampleNames, samples);
        DataMatrix mat = us.getSimpleCharacteristicsOfSamples();
        log.info(mat.toString());
        double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(feature, response);
        log.info("corr = " + corr);
        
//        mat.writeDataMatrix(false, pathToOutputFolder, "correlation_samples_characteristics", log);
//        Chart chart = us.createChartWithSmoothedDensities("Correlation coefficient", false, DensityEstimation.WINDOW_WIDTH_01, null);
//        TableAndFileUtils.addChartToTable("Densities of correlations", chart, pathToOutputFolder.getChildPath("_chart_correlation_densities"));
    }
    
    public static void primaryRegressionForTFsFromArticle(DataElementPath pathToOutputFolder)
    {
        // 1.
        String[] featureNames = new String[]{"1.2.6.7.5", "3.3.2.1.6", "2.3.1.1.1", "2.3.1.3.1", "1.2.3.1.1", "2.3.3.8.1",
                "5.1.2.0.1", "1.1.2.1.3", "2.9.1.0.1", "1.2.6.2.1", "2.2.1.1.2", "1.1.3.2.3", "2.3.1.1.2", "6.1.2.1.8",
                "1.1.2.1.1", "4.2.1.0.1", "2.3.2.1.12", "3.5.2.5.1", "3.5.2.1.4", "3.1.6.3.2", "1.1.8.1.2", "1.1.1.2.2",
                "2.3.3.0.79", "1.1.1.1.3", "1.1.1.3.1", "1.1.1.2.1", "4.2.1.0.2", "2.2.1.1.1", "2.1.3.4.2", "1.2.6.2.2"};
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/K562_with_QC_3.0_macs2_overlap/data_matrix_extended");
        String[] columNames = TableAndFileUtils.getColumnNames(pathToDataMatrix);
        List<String> list = new ArrayList<>();
        for( String s : featureNames )
        {
            String ss = s + "_-100_0";
            if( ! ArrayUtils.contains(columNames, ss) )
            {
                log.info("feature is absent = " + s);
                continue;
            }
            list.add(ss);
            list.add(s + "_1_100");
        }
        list.add("Cell_1327");
        list.add("mean_intensity_lg_cell line_predicted_by_K562_HepG2_GM12878_MCF7_HEK293");

        String[] featureNamesNew = list.toArray(new String[0]);
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, featureNamesNew);
        dataMatrix.writeDataMatrix(true, pathToOutputFolder, "data_submatrix_with_published_features", log);
    }
    
    public static void primaryRegressionForTFsFromArticle2(DataElementPath pathToOutputFolder)
    {
        // 1.
        String[] featureNames = new String[]{"1.2.6.7.5", "3.3.2.1.6", "2.3.1.1.1", "2.3.1.3.1", "1.2.3.1.1", "2.3.3.8.1",
                "5.1.2.0.1", "1.1.2.1.3", "2.9.1.0.1", "1.2.6.2.1", "2.2.1.1.2", "1.1.3.2.3", "2.3.1.1.2", "6.1.2.1.8",
                "1.1.2.1.1", "4.2.1.0.1", "2.3.2.1.12", "3.5.2.5.1", "3.5.2.1.4", "3.1.6.3.2", "1.1.8.1.2", "1.1.1.2.2",
                "2.3.3.0.79", "1.1.1.1.3", "1.1.1.3.1", "1.1.1.2.1", "4.2.1.0.2", "2.2.1.1.1", "2.1.3.4.2", "1.2.6.2.2"};
        String[] promoterRegions = new String[]{"-5000_-1001", "-1000_-501", "-500_-201", "-200_-101", "-100_0", "1_100", "101_500", "501_1000"};
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/K562_with_QC_3.0_macs2_overlap/data_matrix_extended");
        String[] columNames = TableAndFileUtils.getColumnNames(pathToDataMatrix);
        List<String> list = new ArrayList<>();
        for( String s : featureNames )
        {
            boolean containTfClass = false;
            for( String ss : columNames )
                if( ss.contains(s) )
                {
                    containTfClass = true;
                }
            if( containTfClass )
                for( String ss : promoterRegions )
                    list.add(s + "_" + ss);
        }
        list.add("Cell_1327");
        list.add("mean_intensity_lg_cell line_predicted_by_K562_HepG2_GM12878_MCF7_HEK293");

        String[] featureNamesNew = list.toArray(new String[0]);
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, featureNamesNew);
        dataMatrix.writeDataMatrix(true, pathToOutputFolder, "data_submatrix_with_published_features_extended", log);
    }

    // TODO: For HEY1
    public static void activatorAndRepressor(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToDataMatrixWithStartCoverage = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer04_for_reviewers/RAB09_repressor_HepG2/RAB__01/Indicator_matrix");
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrixWithStartCoverage, new String[]{"1.2.4.1.8_0_0"});
        double[] feature0 = dataMatrix.getColumn("1.2.4.1.8_0_0");
        List<Integer> list = new ArrayList<>();
        for( int i = 0; i < feature0.length; i++ )
            if( feature0[i] < 0.01 )
                list.add(i);
        int[] indices = UtilsGeneral.fromListIntegerToArray(list); 
        
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans00_prediction_without_mean/HepG2_2/selected_data_matrix_extended");
        String feature1 = "1.2.4.1.8_-100_0", feature2 = "1.2.4.1.8_1_100", cellLine = "Cell_1424_lg"; 
        String[] columNames = new String[]{feature1, feature2, cellLine};
        double[][] intervals = new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.5}, new double[]{0.5, 1.0},
                               new double[]{1.0, 10.0}, new double[]{1.0, 2.0}, new double[]{2.0, 3.0}, new double[]{3.0, 10.0}};

        DataMatrix dm = new DataMatrix(pathToDataMatrix, columNames);
        
        dm = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, indices)[0];
        
        for( int i = 0; i < intervals.length; i++ )
        {
            // 1.
            DataMatrix datMat = getSubMatrix2(dm, cellLine, intervals[i]);
            if( datMat == null || datMat.getSize() < 5 ) continue;
            double[] features1 = datMat.getColumn(feature1);
            double[] features2 = datMat.getColumn(feature2);
            double[] intensities = datMat.getColumn(cellLine);
            double meanIntensity = PrimitiveOperations.getAverage(intensities);
            double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(features1, features2);
            log.info("i = " + i + " intervals[i] = " + intervals[i][0] + " " + intervals[i][1] + " n = " + features1.length + " corr = " + corr + " meanIntensity = " + meanIntensity);
            
            // 2.
            printRatio(features1, features2);
        }
    }
    
    public static void activatorAndRepressorForKLF10(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans00_prediction_without_mean/HepG2_2/selected_data_matrix_extended");
        String feature1 = "2.3.1.2.10_-100_0", feature2 = "2.3.1.2.10_501_1000", cellLine = "Cell_1424_lg"; 
        String[] columNames = new String[]{feature1, feature2, cellLine};
        double[][] intervals = new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.5}, new double[]{0.5, 1.0},
                               new double[]{1.0, 10.0}, new double[]{1.0, 2.0}, new double[]{2.0, 3.0}, new double[]{3.0, 10.0}};

        DataMatrix dm = new DataMatrix(pathToDataMatrix, columNames);
        for( int i = 0; i < intervals.length; i++ )
        {
            // 1.
            DataMatrix datMat = getSubMatrix2(dm, cellLine, intervals[i]);
            if( datMat == null || datMat.getSize() < 5 ) continue;
            double[] features1 = datMat.getColumn(feature1);
            double[] features2 = datMat.getColumn(feature2);
            double[] intensities = datMat.getColumn(cellLine);
            double meanIntensity = PrimitiveOperations.getAverage(intensities);
            double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(features1, features2);
            log.info("i = " + i + " intervals[i] = " + intervals[i][0] + " " + intervals[i][1] + " n = " + features1.length + " corr = " + corr + " meanIntensity = " + meanIntensity);
            
            // 2.
            printRatio(features1, features2);
        }
    }
    
    private static void printRatio(double[] binaryFeature1, double[] binaryFeature2)
    {
        int n = binaryFeature1.length, n11 = 0, n1 = 0, n2 = 0;
        for( int i = 0; i < n; i++ )
        {
            if( binaryFeature1[i] > 0.1 && binaryFeature2[i] > 0.1 )
                n11++;
            if( binaryFeature1[i] > 0.1 )
                n1++;
            if( binaryFeature2[i] > 0.1 )
                n2++;
        }
        double pobs = (double)n11 / (double)n, pexp = (double)n1  * (double)n2 / ( (double)n * (double)n);
        double ratio = pexp > 0.0 ? pobs / pexp : Double.NaN;
        log.info("observed probability = " + pobs + " expected probability = " + pexp + " ratio = " + ratio);
    }
   
    private static DataMatrix getSubMatrix2(DataMatrix dm, String columName, double[] interval)
    {
        double[] column = dm.getColumn(columName);
        List<Integer> list = new ArrayList<>();
        for( int i = 0; i < column.length ; i++ )
            if( column[i] >= interval[0] && column[i] <= interval[1] )
                list.add(i);
        if( list.isEmpty() ) return null;
        DataMatrix datMat = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, UtilsGeneral.fromListIntegerToArray(list))[0];
        return datMat;
    }
    
    private static DataMatrix getSubMatrix(DataMatrix dm, String columName, double value)
    {
        double[] column = dm.getColumn(columName);
        List<Integer> list = new ArrayList<>();
        for( int i = 0; i < column.length ; i++ )
            if( column[i] == value )
                list.add(i);
        if( list.isEmpty() ) return null;
        DataMatrix datMat = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, UtilsGeneral.fromListIntegerToArray(list))[0];
        return datMat;
    }
    
    /*****************************************************************************************/
    /************************************* Cistrom2 article  *********************************/
    /*****************************************************************************************/

    
    public static void analysisOfLengthInMetaClusters(DataElementPath pathToOutputFolder)
    {
        // 1.
        ru.biosoft.access.core.DataElementPath[] pathToNewMetaTrack = new  ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372")                
                };
        ru.biosoft.access.core.DataElementPath[] pathToOldMetaTrack = new  ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P05412/meta clusters"),
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P55317/meta clusters"),
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P03372/meta clusters")                
                };
//        ru.biosoft.access.core.DataElementPath[] pathToOutputFolder = new  ru.biosoft.access.core.DataElementPath[]
//                {
//                DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans01_Length_comparison/P05412"),
//                DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans01_Length_comparison/P55317"),
//                DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans01_Length_comparison/P03372")                
//                };
        double[] raScoreThreshold = new double[]{0.51404, 0.506166, 0.497538};
        
        // 2.
        for( int i = 0; i < pathToNewMetaTrack.length; i++ )
        {
            log.info("i = " + i + " TF = " + pathToNewMetaTrack[i].getName());
            analysisOfLengthInMetaClusters( pathToOldMetaTrack[i], pathToNewMetaTrack[i], raScoreThreshold[i], pathToOutputFolder);
        }
    }

    private static void analysisOfLengthInMetaClusters(DataElementPath pathToOldMetaTrack, DataElementPath pathToNewMetaTrack, double raScoreThreshold, DataElementPath pathToOutputFolder)
    {
        // 1.
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToNewMetaTrack);
        double[] scores = FunSiteUtils.getRaScores(funSites);
        log.info("size of funSites = " + funSites.length + " size of scores = " + scores.length);
        Object[] objects = splitFunSites(funSites, scores, raScoreThreshold);
        FunSite[] funSitesSmall = (FunSite[])objects[0], funSitesLarge = (FunSite[])objects[1];
        double[] lenthgsSmall = getLengths(funSitesSmall), lenthgsLarge = getLengths(funSitesLarge);
        double[] lenthgsOld = getLengths(pathToOldMetaTrack);
        
        // 2.
        String[] samplesNames = new String[]{"Old meta-clusters", "Less reliable meta-clusters", "Most reliable meta-clusters"};
        double[][] samples = new double[][]{lenthgsOld, lenthgsLarge, lenthgsSmall};
        UnivariateSamples us = new UnivariateSamples(samplesNames, samples);
        DataMatrix dm = us.getSimpleCharacteristicsOfSamples();
        log.info("chracteristics = " + dm.toString());
        String name = pathToNewMetaTrack.getName();
        dm.writeDataMatrix(false, pathToOutputFolder, name + "_characteristics", log);
        Chart chart = us.createChartWithSmoothedDensities("Length of meta-clusters", true, DensityEstimation.WINDOW_WIDTH_01, null);
        TableAndFileUtils.addChartToTable("chart with densities", chart, pathToOutputFolder.getChildPath("_chart_with_densities_" + name));
        Homogeneity homogeneity = new Homogeneity(us);
        homogeneity.performPairwiseComparisonOfSamples(null, pathToOutputFolder, name + "_pairwise_comparison");
    }
    
    public static void calculateRocCurvesForOldAndNewMetaClusters(DataElementPath pathToOutputFolder)
    {
        // 1.
        DataElementPath pathToFolderWithSiteModels = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.0001");
        DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");
        String[] siteModelName = new String[]{"ESR1_HUMAN.H11MO.0.A", "JUN_HUMAN.H11MO.0.A", "FOXA1_HUMAN.H11MO.0.A"};
        String[] rocCurveNames = new String[]{"Old meta-clusters", "Less reliable meta-clusters", "Most reliable meta-clusters"};
        ru.biosoft.access.core.DataElementPath[] pathToNewClusters = new ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317")
                };
        ru.biosoft.access.core.DataElementPath[] pathToOldClusters = new ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P03372/meta clusters"),
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P05412/meta clusters"),
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P55317/meta clusters")
                };
        double[] raScoreThreshold = new double[]{0.497538, 0.51404, 0.506166};
        int numberOfMetaClasters = 25000;
        
        // 2.
        for( int i = 0; i < siteModelName.length; i++ )
        {
            log.info("ROC-curve for " + pathToNewClusters[i].getName() + " siteModelName = " + siteModelName[i]);
            FunSite[][] allFunSites = new FunSite[3][];
            
           FunSite[] funSites = getFunSitesFromOldMetaClusters(pathToOldClusters[i]);
           allFunSites[0] = getSubset(funSites, numberOfMetaClasters);
           
           funSites = FunSiteUtils.getFunSites(pathToNewClusters[i]);
           double[] scores = FunSiteUtils.getRaScores(funSites);
           log.info("size of funSites = " + funSites.length + " size of scores = " + scores.length);
           Object[] objects = splitFunSites(funSites, scores, raScoreThreshold[i]);
           FunSite[] funSitesSmall = (FunSite[])objects[0], funSitesLarge = (FunSite[])objects[1];
           allFunSites[1] = getSubset(funSitesLarge, numberOfMetaClasters);
           allFunSites[2] = getSubset(funSitesSmall, numberOfMetaClasters);
           calculateRocCurves(allFunSites, rocCurveNames, pathToNewClusters[i].getName(), pathToFolderWithSiteModels, siteModelName[i], pathToSequences, pathToOutputFolder);
        }
    }

    // It was used the MATCH- site models instead of HOKOMOKO site models
    public static void calculateRocCurvesForOldAndNewMetaClusters2(DataElementPath pathToOutputFolder)
    {
        // 1.
        DataElementPath pathToFolderWithSMatrices = DataElementPath.create("databases/TRANSFAC(R) 2020.3/Data/matrix");
        DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");
        String[] matrixName = new String[]{"V$ESR1_10", "V$JUN_11", "V$FOXA1_14"};
        String[] rocCurveNames = new String[]{"Old meta-clusters", "Less reliable meta-clusters", "Most reliable meta-clusters"};
        ru.biosoft.access.core.DataElementPath[] pathToNewClusters = new ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317")
                };
        ru.biosoft.access.core.DataElementPath[] pathToOldClusters = new ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P03372/meta clusters"),
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P05412/meta clusters"),
                DataElementPath.create("databases/GTRD/Data/clusters/Homo sapiens/By TF/P55317/meta clusters")
                };
        double[] raScoreThreshold = new double[]{0.497538, 0.51404, 0.506166};
        int numberOfMetaClasters = 25000;
        
        // 2.
        for( int i = 0; i < matrixName.length; i++ )
        {
        	
            log.info("ROC-curve for " + pathToNewClusters[i].getName() + " matrixName = " + matrixName[i]);
            FunSite[][] allFunSites = new FunSite[3][];
            FunSite[] funSites = getFunSitesFromOldMetaClusters(pathToOldClusters[i]);
            allFunSites[0] = getSubset(funSites, numberOfMetaClasters);
            funSites = FunSiteUtils.getFunSites(pathToNewClusters[i]);
            double[] scores = FunSiteUtils.getRaScores(funSites);
            log.info("size of funSites = " + funSites.length + " size of scores = " + scores.length);
            Object[] objects = splitFunSites(funSites, scores, raScoreThreshold[i]);
            FunSite[] funSitesSmall = (FunSite[])objects[0], funSitesLarge = (FunSite[])objects[1];
            allFunSites[1] = getSubset(funSitesLarge, numberOfMetaClasters);
            allFunSites[2] = getSubset(funSitesSmall, numberOfMetaClasters);
            calculateRocCurves2(allFunSites, rocCurveNames, pathToNewClusters[i].getName(), pathToFolderWithSMatrices, matrixName[i], pathToSequences, pathToOutputFolder);
        }
    }
    
    private static void calculateRocCurves2(FunSite[][] allFunSites, String[] rocCurveNames, String tfName, DataElementPath pathToFolderWithSMatrices, String matrixName, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
    {
        int w = 50;
        FrequencyMatrix frequencyMatrix = pathToFolderWithSMatrices.getChildPath(matrixName).getDataElement(FrequencyMatrix.class);
        SiteModel siteModel = SiteModelUtils.createSiteModel(SiteModelUtils.MATCH_MODEL, matrixName, frequencyMatrix, 0.0, null);
        //String s = SiteModelUtils.MATCH_MODEL;
        // SiteModel siteModel = pathToFolderWithSMatrices.getChildPath(siteModelName).getDataElement(SiteModel.class);
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
        int lengthOfSequenceRegion = w + siteModel.getLength();
//        Object[] objects = selectBestSites(pathToTrack, numberOfBestSites);
        double[][] xValuesForCurves = new double[rocCurveNames.length][], yValuesForCurves = new double[rocCurveNames.length][];
        double[] aucs = new double[rocCurveNames.length];
        for( int i = 0; i < rocCurveNames.length; i++ )
        {
            FunSite[] funSites = allFunSites[i];
            funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
            Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
            RocCurve rocCurve = new RocCurve(smc, sequences, 10, 0);
            double[][] curve  = rocCurve.getRocCurve();
            xValuesForCurves[i] = curve[0];
            yValuesForCurves[i] = curve[1];
            aucs[i] = rocCurve.getAuc();
            log.info(i + ") AUC = " + aucs[i]);
        }
        DataMatrix dm = new DataMatrix(rocCurveNames, "AUC", aucs);
        dm.writeDataMatrix(false, pathToOutputFolder, "AUCs_" + tfName, log);
        Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, rocCurveNames, null, null, null, null, "Specificity", "Sensitivity", true);
        TableAndFileUtils.addChartToTable("chart with ROC-curve", chart, pathToOutputFolder.getChildPath("_chart_with_ROC_curve_" + tfName));
    }

    private static double[] getLengths(DataElementPath pathToTrack)
    {
        Track track = pathToTrack.getDataElement(Track.class);
        DataCollection<Site> dc = track.getAllSites();
        int n = dc.getSize(), index = 0;
        double[] lengths = new double[n];
        for( Site site : dc )
            lengths[index++] = (double)site.getLength();
        return lengths;
    }

    private static double[] getLengths(FunSite[] funSites)
    {
        double[] lengths = new double[funSites.length];
        for( int i = 0; i < funSites.length; i++ )
            lengths[i] = (double)funSites[i].getLength();
        return lengths;
    }
    
    private static Object[] splitFunSites(FunSite[] funSites, double[] scores, double raScoreThreshold)
    {
        List<FunSite> funSitesSmall = new ArrayList<>(), funSitesLarge = new ArrayList<>();
        for( int i = 0; i < funSites.length; i++ )
        {
            if( scores[i] < raScoreThreshold )
                funSitesSmall.add(funSites[i]);
            else
                funSitesLarge.add(funSites[i]);
        }
        return new Object[]{funSitesSmall.toArray(new FunSite[0]), funSitesLarge.toArray(new FunSite[0])};
    }
    
    /*****************************************************************************************/
    /*********************** For Fedor : properties of removed (induced) sites  **************/
    /*****************************1************************************************************/

    public static void induceMetaFeaturesForTfTracks(DataElementPath pathToOutputFolder)
    {
        // 1.
        DataElementPath pathToFolderWithMetaTracks = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF");
        DataElementPath pathToFolderWithTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans01_data_tracks_for_3_TFs/Combined_peaks");
        DataElementPath pathToTableWithSummary = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans03_sammary_extended_on_tracks/summary_extended_on_ tracks");
        DataMatrixString dms = new DataMatrixString(pathToTableWithSummary, new String[]{ "Uniprot_ID"});

        // 2.
        String[] trackNames = dms.getRowNames(), uniprotIds = dms.getColumn("Uniprot_ID"); // tfClasses = dms.getColumn("TF-class");
        String[] columnNames1 = new String[]{"removed_min", "removed_max", "removed_mean", "frequency_mean"};
        String[] columnNames2 = new String[]{"removed_induced_sites", "frequency_induced_sites"};

        Object[] objects = PrimitiveOperations.countFrequencies(uniprotIds);
        String[] distinctUniprotIds = (String[])objects[0];
        int[] counts = (int[])objects[1];
        
        for( int i = 0; i < trackNames.length; i++ )
        {
            log.info("i = " + i + " track name = " + trackNames[i]);
            DataElementPath pathToMetaTrack = pathToFolderWithMetaTracks.getChildPath(uniprotIds[i]);
            DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(trackNames[i]);
            double raScoreThreshold = 0.0;
            int index = ArrayUtils.indexOf(distinctUniprotIds, uniprotIds[i]);
            int totalNumberOfTracks = counts[index];
            switch( uniprotIds[i] )
            {
                case "P05412" : raScoreThreshold = 0.51404; break;
                case "P55317" : raScoreThreshold = 0.506166; break;
                case "P03372" : raScoreThreshold = 0.497538; break;
            }
            double[] features = induceMetaFeaturesForTfTracks(pathToMetaTrack, pathToTrack, raScoreThreshold, columnNames1, columnNames2, totalNumberOfTracks);
            TableAndFileUtils.addRowToTable(features, null, trackNames[i], (String[])ArrayUtils.addAll(columnNames1, columnNames2), pathToOutputFolder,  "removed_sites_features");
        }
    }
    
    private static double[] induceMetaFeaturesForTfTracks(DataElementPath pathToMetaTrack, DataElementPath pathToTrack, double raScoreThreshold, String[] columnNames1, String[] columnNames2, int totalNumberOfTracks)
    {
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToTrack);
        double[] scores = FunSiteUtils.getRaScores(funSites);
        log.info("size of funSites = " + funSites.length + " size of scores = " + scores.length);
        Track trackWithMetaClusters = pathToMetaTrack.getDataElement(Track.class);
        String[] propertiesNames = new String[]{RankAggregation.RA_SCORE, "Frequency"};
        DataMatrixConstructor dmc1 = new DataMatrixConstructor(columnNames1), dmc2 = new DataMatrixConstructor(columnNames2);
        for( int i = 0; i < funSites.length; i++ )
        {
            DataCollection<Site> dc = trackWithMetaClusters.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
            if( dc.getSize() < 1 ) continue;
            
            //
            DataMatrixConstructor dmcLocal = new DataMatrixConstructor(propertiesNames); 
            int index = 0;
            for( Site site : dc )
            {
                double[] properties = SiteUtils.getProperties(site, propertiesNames);
                dmcLocal.addRow(Integer.toString(index++), properties);
            }
            DataMatrix dmLocal = dmcLocal.getDataMatrix();
            double[] raScoresLocal = dmLocal.getColumn(propertiesNames[0]), frequenciesLocal = dmLocal.getColumn(propertiesNames[1]);
            if( raScoresLocal.length == 1 )
                dmc1.addRow("0", new double[]{raScoresLocal[0], raScoresLocal[0], raScoresLocal[0], frequenciesLocal[0]});
            else
            {
                double[] minAndMax = PrimitiveOperations.getMinAndMax(raScoresLocal);
                dmc1.addRow("0", new double[]{minAndMax[0], minAndMax[1], PrimitiveOperations.getAverage(raScoresLocal), PrimitiveOperations.getAverage(frequenciesLocal)});
            }
            for( int j = 0; j < raScoresLocal.length; j++ )
                dmc2.addRow("0", new double[]{raScoresLocal[j], frequenciesLocal[j]});
        }
        DataMatrix dm1 = dmc1.getDataMatrix(), dm2 = dmc2.getDataMatrix();
        
        //
        double[][] matrix = dm1.getMatrix();
        double[] freq = UtilsForArray.getConstantArray(columnNames1.length + columnNames2.length, 0.0);
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < 3; j++ )
                if( matrix[i][j] >= raScoreThreshold )
                    freq[j] += 1.0;
        }
        for( int j = 0; j < 3; j++ )
            freq[j] /= (double)matrix.length;
        
        //
        double[] array = dm1.getColumn(columnNames1[3]);
        freq[3] = PrimitiveOperations.getAverage(array) / (double)totalNumberOfTracks;
        
        //
        array = dm2.getColumn(columnNames2[0]);
        for( int i = 0; i < dm2.getSize(); i++ )
            if( array[i] >= raScoreThreshold )
                freq[4] += 1.0;
        freq[4] /= (double)dm2.getSize();
        
        //
        array = dm2.getColumn(columnNames2[1]);
        freq[5] = PrimitiveOperations.getAverage(array) / (double)totalNumberOfTracks;
        return freq;
    }
    
    /*****************************************************************************************/
    /************************************** Cistrom paper ************************************/
    /*****************************************************************************************/

    public static void treatWithDnaseSitesClassification()
    {
        // 1
        DataElementPath pathToTrackWithMetaClusters = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412");
        DataElementPath pathToSiteModel = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001/JUN_HUMAN.H11MO.0.A");
        DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");
        
        // 2.
        DataElementPath pathToTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans01_data_tracks_for_3_TFs/Combined_peaks/PEAKS033434");
        DataElementPath pathToTrackWithDnaseSites = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C06_DNase_meta_clusters_merged/HepG2 (hepatoblastoma)");
        DataElementPath pathToOutputFolderWithClassification = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/ANS07_DNase_ckassification/PEAKS033434/classification");
        DataElementPath pathToOutputFolderWithRegression = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/ANS07_DNase_ckassification/PEAKS033434/regression");
        treatWithDnaseSitesClassification(pathToTrack, pathToTrackWithMetaClusters, pathToTrackWithDnaseSites, pathToSiteModel, pathToSequences, pathToOutputFolderWithClassification, pathToOutputFolderWithRegression);
        
        // 3.
        pathToTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans01_data_tracks_for_3_TFs/Combined_peaks/PEAKS033441");
        pathToTrackWithDnaseSites = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C06_DNase_meta_clusters_merged/K562 (myelogenous leukemia)");
        pathToOutputFolderWithClassification = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/ANS07_DNase_ckassification/PEAKS033441/classification");
        pathToOutputFolderWithRegression = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/ANS07_DNase_ckassification/PEAKS033441/regression");
        treatWithDnaseSitesClassification(pathToTrack, pathToTrackWithMetaClusters, pathToTrackWithDnaseSites, pathToSiteModel, pathToSequences, pathToOutputFolderWithClassification, pathToOutputFolderWithRegression);
    }

    private static void treatWithDnaseSitesClassification(DataElementPath pathToTrack, DataElementPath pathToTrackWithMetaClusters, DataElementPath pathToTrackWithDnaseSites, DataElementPath pathToSiteModel, DataElementPath pathToSequences, DataElementPath pathToOutputFolderWithClassification, DataElementPath pathToOutputFolderWithRegression)
    {
        // 0
        int lengthOfSequenceRegion = 100;
        Track trackWithMetaClusters = pathToTrackWithMetaClusters.getDataElement(Track.class);
        Track trackWithDnaseSites = pathToTrackWithDnaseSites.getDataElement(Track.class);
        
        // 1.
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToTrack);
        funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);

        double[] scoresRaInduced = calculateInducedRaScoresAsAverage(funSites, trackWithMetaClusters);
        log.info("track = " + pathToTrack.getName());
        log.info("size of funSites = " + funSites.length + " " + scoresRaInduced.length);
        
        // 2.
        List<FunSite> list1 = new ArrayList<>();
        List<Double> list2 = new ArrayList<>();
        for( int i = 0; i < funSites.length; i++ )
            if( ! Double.isNaN(scoresRaInduced[i]) )
            {
                list1.add(funSites[i]);
                list2.add(scoresRaInduced[i]);
            }
        funSites = list1.toArray(new FunSite[0]);
        scoresRaInduced = UtilsGeneral.fromListToArray(list2);

        // 3.
        boolean[] indicators = areCoveredByTrack(funSites, trackWithDnaseSites);
        String[] isCovered = new String[indicators.length];
        for( int i = 0; i < indicators.length; i++ )
            isCovered[i] = indicators[i] ? "is_covered" : "is_not_covered";
        log.info("isCovered was defined n = " + isCovered.length);

        // 4.
        Object[] objects = getBestScores(funSites, pathToSiteModel, lengthOfSequenceRegion, pathToSequences);
        funSites = (FunSite[])objects[1];
        double[] motifScores = (double[])objects[0];
        log.info("motifScores, n = " + motifScores.length);

        
        // 5.
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"RA_score", "RA_score_squared",
                "motif_score", "motif_score_squared", "interaction_of_RA_score_and_motif_score"});
        for( int i = 0; i < scoresRaInduced.length; i++ )
            dmc.addRow("site_" + Integer.toString(i) , new double[]{scoresRaInduced[i], scoresRaInduced[i] * scoresRaInduced[i],
                motifScores[i], motifScores[i] * motifScores[i], scoresRaInduced[i] * motifScores[i]});
        DataMatrix dm = dmc.getDataMatrix();
        log.info("dim(dm) = " + dm.getSize());
        
        
        // 6.
        Object[] additionalInputParameters = new Object[]{MatrixUtils.DEFAULT_MAX_NUMBER_OF_ROTATIONS, MatrixUtils.DEFAULT_EPS_FOR_ROTATIONS, MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_IN_LYUSTERNIK_METHOD, MatrixUtils.DEFAULT_EPS_IN_LYUSTERNIK_METHOD};
        ClassificationModel classificationModel = new FisherDiscriminantModel("Covered_by_DNAse", isCovered, dm, additionalInputParameters, true);
        classificationModel.saveModel(pathToOutputFolderWithClassification);
        log.info("classification");

        // 7.
        dmc = new DataMatrixConstructor(new String[]{ModelUtils.INTERCEPT, "motif_Scores", "motif_Scoresore_squared"});
        for( int i = 0; i < scoresRaInduced.length; i++ )
            dmc.addRow("site_" + Integer.toString(i) , new double[]{1.0, motifScores[i], motifScores[i] * motifScores[i]});
        dm = dmc.getDataMatrix();
        
        // 8.
        OrdinaryLeastSquaresRegressionModel regressionModel = (OrdinaryLeastSquaresRegressionModel)RegressionModel.createModel(RegressionModel.REGRESSION_1_OLS, RankAggregation.RA_SCORE, scoresRaInduced, dm, null, true);
        regressionModel.saveModel(pathToOutputFolderWithRegression);
        log.info("regression");
    }

    public static void regressionBetweenRAscoresAndSiteScores()
    {
        DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");

        ru.biosoft.access.core.DataElementPath[] pathToMetaTracks = new  ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372")                
                };
        ru.biosoft.access.core.DataElementPath[] pathToSiteModel = new  ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001/JUN_HUMAN.H11MO.0.A"),
                DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001/FOXA1_HUMAN.H11MO.0.A"),
                DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001/ESR1_HUMAN.H11MO.0.A")                
                };
        
        ru.biosoft.access.core.DataElementPath[] pathToOutputFolder = new  ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans06_linearRegression_RA_and_site_scores/most_reliable_sites/Ap1"),
                DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans06_linearRegression_RA_and_site_scores/most_reliable_sites/FoxA1"),
                DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans06_linearRegression_RA_and_site_scores/most_reliable_sites/ESR1")                
                };
        
        double[] threshold = new double[]{0.51404, 0.506166, 0.497538};
        for( int i = 0; i < pathToMetaTracks.length; i++ )
            regressionBetweenRAscoresAndSiteScores(pathToMetaTracks[i], threshold[i], pathToSiteModel[i], pathToSequences, pathToOutputFolder[i]);
    }
    
    private static void regressionBetweenRAscoresAndSiteScores(DataElementPath pathToMetaCusters, double threshold, DataElementPath pathToSiteModel, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
    {
        log.info("pathToMetaCusters = " + pathToMetaCusters.toString());

        // 1.
        int numberOfSelectedIndices = 100000, lengthOfSequenceRegion = 100;
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToMetaCusters);
        log.info("size of funSites = " + funSites.length);
        
        //
        List<FunSite> list = new ArrayList<>();
        for( FunSite fs : funSites )
        {
            DataMatrix dm = fs.getDataMatrix();
            double score  = dm.getColumn(RankAggregation.RA_SCORE)[0];
            if( score <= threshold )
                list.add(fs);
        }
        funSites = list.toArray(new FunSite[0]);
        log.info("size of funSites = " + funSites.length);
        
        //
        funSites = getSubset(funSites, numberOfSelectedIndices);
        Object[] objects = getBestScores(funSites, pathToSiteModel, lengthOfSequenceRegion, pathToSequences);
        funSites = (FunSite[])objects[1];
        double[] siteScoresSites = (double[])objects[0], scoresRA = FunSiteUtils.getRaScores(funSites);
        log.info("size of scores = " + scoresRA.length);
        
        // 2.
//        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{ModelUtils.INTERCEPT, "site_scores", "site_scores_squared", "site_scores_**3", "site_scores_**4", "site_scores_**5"});
//        for( int i = 0; i < siteScoresSites.length; i++ )
//        {
//            double x2 = siteScoresSites[i] * siteScoresSites[i], x3 = x2 * siteScoresSites[i];
//            double x4 = x2 * x2, x5 = x2 * x3;
//            dmc.addRow("site_" + Integer.toString(i) , new double[]{1.0, siteScoresSites[i], x2, x3, x4, x5});
//        }
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{ModelUtils.INTERCEPT, "site_scores"});
        for( int i = 0; i < siteScoresSites.length; i++ )
        {
            dmc.addRow("site_" + Integer.toString(i) , new double[]{1.0, siteScoresSites[i]});
        }
        DataMatrix dm = dmc.getDataMatrix();
        OrdinaryLeastSquaresRegressionModel regressionModel = (OrdinaryLeastSquaresRegressionModel)RegressionModel.createModel(RegressionModel.REGRESSION_1_OLS, RankAggregation.RA_SCORE, scoresRA, dm, null, true);
        regressionModel.saveModel(pathToOutputFolder);
    }
    
    private static Object[] getBestScores(FunSite[] funSites, DataElementPath pathToSiteModel, int lengthOfSequenceRegion, DataElementPath pathToSequences)
    {
        SiteModel siteModel = pathToSiteModel.getDataElement(SiteModel.class);
        return getBestScores(funSites, siteModel, lengthOfSequenceRegion, pathToSequences);
    }
    
    public static Object[] getBestScores(FunSite[] funSites, SiteModel siteModel, int lengthOfSequenceRegion, DataElementPath pathToSequences)
    {
        FunSite[] funSitesNew = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
        double threshold = siteModel.getThreshold();
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
        Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSitesNew, pathToSequences, lengthOfSequenceRegion);
        double[] bestScores = smc.findBestScores(sequences);
        for( int i = 0; i < bestScores.length; i++ )
            if( Double.isNaN(bestScores[i]) )
                bestScores[i] = threshold;
        return new Object[]{bestScores, funSitesNew};
        
    }
    
    public static void countPeakCallers()
    {
        String[] uniprotIds = new String[]{"P05412", "P55317", "P03372"};
        for( String s : uniprotIds )
            countPeakCallers(s);
    }
    
    private static void countPeakCallers(String uniprotId)
    {
        DataElementPath path = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans03_sammary_extended_on_tracks/summary_extended_on traks");
        DataMatrixString dms = new DataMatrixString(path, new String[]{"Uniprot_ID", "Cell_line"});
        String[] uniprotIDs = dms.getColumn("Uniprot_ID"), cellLines = dms.getColumn("Cell_line");
        String[] columnNames = new String[]{"Number_of_sites_gem", "Number_of_sites_macs2", "Number_of_sites_pics", "Number_of_sites_sissrs"};
        DataMatrix dm = new DataMatrix(path, columnNames);
        Set<String> set = new HashSet<>();
        for( int i = 0; i < uniprotIDs.length; i++ )
            if( uniprotIDs[i].equals(uniprotId) )
                set.add(cellLines[i]);
        log.info("uniprotId = " + uniprotId + " distinct cell lines  = " + set.size());
        
        for( int ii = 0; ii < columnNames.length; ii++ )
        {
            double count = 0.0;
            double[] values = dm.getColumn(columnNames[ii]);
            for( int i = 0; i < uniprotIDs.length; i++ )
                if( uniprotIDs[i].equals(uniprotId) )
                    count += values[i];
            log.info("peak calller = " + columnNames[ii] + " count = " + count);
        }
    }

    public static void countMetaClasters(DataElementPath pathToOutputFolder)
    {
        ru.biosoft.access.core.DataElementPath[] pathToMetaTracks = new  ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372")                
                };
        double[] means1 = new double[]{0.48000393014108145, 0.47664461675289943, 0.48524855778512493};
        double[] sigmas1 = new double[]{0.03307997907096753, 0.041583204559474486, 0.022306351720039384};
        double[] means2 = new double[]{0.518896460317819, 0.5117148091421277, 0.5017338632481539};
        double[] sigmas2 = new double[]{0.002951979383968634, 0.003372747881682958, 0.002550357189133859};
        for( int i = 0; i < pathToMetaTracks.length; i++ )
            countMetaClasters(means1[i], sigmas1[i], means2[i], sigmas2[i], pathToMetaTracks[i]);
    }
    
    private static void countMetaClasters(double mean1, double sigma1, double mean2, double sigma2, DataElementPath pathToTrack)
    {
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToTrack);
        double[] scores = FunSiteUtils.getRaScores(funSites);
        double threshold1 = mean2 - 1.645 * sigma2, threshold2 = mean1 + 1.645 * sigma1;
        int count1 = 0, count2 = 0;
        for( double x : scores )
        {
            if( x <= threshold1 )
                count1++;
            if( x <= threshold2 )
                count2++;
        }
        log.info("track = " + pathToTrack.getName() + " total size = " + scores.length + " count1 = " + count1 + " count2 = " + count2);
        log.info("threshold1 = " + threshold1 + " threshold2 = " + threshold2);
    }
    
    public static void treatWithDnaseSitesForAp1(DataElementPath pathToOutputFolder)
    {
        DataElementPath path = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans03_sammary_extended_on_tracks/summary_extended_on traks");
        DataElementPath pathToFolderWithTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans01_data_tracks_for_3_TFs/Combined_peaks");
        DataElementPath pathToMetaTracks = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412");
        Track trackWithMetaClusters = pathToMetaTracks.getDataElement(Track.class);
        DataElementPath pathToDnaseTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C06_DNase_meta_clusters_merged");
        
        // 1.
        DataMatrixString dm = new DataMatrixString(path, new String[]{"TF-class", "Cell_line"});
        String[] tfClasses = dm.getColumn("TF-class"), trackNames = dm.getRowNames(), cellLines = dm.getColumn("Cell_line");
        List<String> list1 = new ArrayList<>(), list2 = new ArrayList<>();
        for( int i = 0; i < tfClasses.length; i++ )
            if( tfClasses[i].equals("1.1.1.1.1"))
            {
                list1.add(trackNames[i]);
                list2.add(cellLines[i]);
            }
        trackNames = list1.toArray(new String[0]);
        cellLines = list2.toArray(new String[0]);
        
        // 2.
        for( int i = 0; i < trackNames.length; i++ )
        {
            log.info("track = " + trackNames[i]);
            DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(trackNames[i]);
            Track trackWithDnaseSites = pathToDnaseTracks.getChildPath(cellLines[i]).getDataElement(Track.class);
            treatWithDnaseSites(pathToTrack, trackWithMetaClusters, trackWithDnaseSites, pathToOutputFolder);
        }
    }

    public static void treatWithDnaseSites(DataElementPath pathToOutputFolder)
    {
        DataElementPath path = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C06_DNase_meta_clusters_merged/MCF7 (Invasive ductal breast carcinoma)");
        Track trackWithDnaseSites = path.getDataElement(Track.class);
        
        String[] trackNames = new String[]{"PEAKS046746", "PEAKS039104", "PEAKS038659"};
        DataElementPath pathToFolderWithTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans01_data_tracks_for_3_TFs/Combined_peaks");

        ru.biosoft.access.core.DataElementPath[] pathToMetaTracks = new  ru.biosoft.access.core.DataElementPath[]
                {
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372")                
                };
        for( int i = 0; i < trackNames.length; i++ )
        {
            log.info("track = " + trackNames[i]);
            DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(trackNames[i]);
            Track trackWithMetaClusters = pathToMetaTracks[i].getDataElement(Track.class);
            treatWithDnaseSites(pathToTrack, trackWithMetaClusters, trackWithDnaseSites, pathToOutputFolder);
        }
    }
    
    private static void treatWithDnaseSites(DataElementPath pathToTrack, Track trackWithMetaClusters, Track trackWithDnaseSites, DataElementPath pathToOutputFolder)
    {
        // 1.
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToTrack);
        double[] scoresInduced = calculateInducedRaScoresAsAverage(funSites, trackWithMetaClusters);
        
        // 2.
        List<FunSite> list1 = new ArrayList<>();
        List<Double> list2 = new ArrayList<>();
        for( int i = 0; i < funSites.length; i++ )
            if( ! Double.isNaN(scoresInduced[i]) )
            {
                list1.add(funSites[i]);
                list2.add(scoresInduced[i]);
            }
        funSites = list1.toArray(new FunSite[0]);
        scoresInduced = UtilsGeneral.fromListToArray(list2);

        // 3.
        boolean[] indicators = areCoveredByTrack(funSites, trackWithDnaseSites);
        String[] samplesNames = new String[indicators.length];
        for( int i = 0; i < indicators.length; i++ )
            samplesNames[i] = indicators[i] ? "is_covered" : "is_not_covered";
        UnivariateSamples us = new UnivariateSamples(samplesNames, scoresInduced);
        String name = pathToTrack.getName();
        DataMatrix dm = us.getSimpleCharacteristicsOfSamples();
        dm.writeDataMatrix(false, pathToOutputFolder, name + "_characteristics", log);
        log.info("track = " + dm.toString());
        Chart chart = us.createChartWithSmoothedDensities(RankAggregation.RA_SCORE, true, DensityEstimation.WINDOW_WIDTH_01, null);
        TableAndFileUtils.addChartToTable("chart with densities", chart, pathToOutputFolder.getChildPath(name + "_chart_with_densities"));

        // 4.
        Homogeneity homogeneity = new Homogeneity(us);
        String[] testNames = Homogeneity.getAvailableTestNames();
        DataMatrix dataMatrix = homogeneity.performTestsOfHomogeneity(testNames);
        dataMatrix.writeDataMatrix(false, pathToOutputFolder, name + "_homogeneity", log);
        log.info("track = " + dataMatrix.toString());
    }

    public static void calculateRocCurves()
    {
        int numberOfBestSites = 20000;
        DataElementPath pathToFolderWithSiteModels = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.0001");
        DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");
        
        // 1.
        DataElementPath pathToTrack = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412");
        String siteModelName = "JUN_HUMAN.H11MO.0.A";
        DataElementPath pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans04_RA_thresholds/ANS03_ROC_curves/Ap1");
        log.info("ROC-curve for Ap1");
        calculateRocCurves(pathToTrack, numberOfBestSites, pathToFolderWithSiteModels, siteModelName, pathToSequences, pathToOutputFolder);

        // 2.
        pathToTrack = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317");
        siteModelName = "FOXA1_HUMAN.H11MO.0.A";
        pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans04_RA_thresholds/ANS03_ROC_curves/FoxA1");
        log.info("ROC-curve for FoxA1");
        calculateRocCurves(pathToTrack, numberOfBestSites, pathToFolderWithSiteModels, siteModelName, pathToSequences, pathToOutputFolder);

        // 3.
        pathToTrack = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372");
        siteModelName = "ESR1_HUMAN.H11MO.0.A";
        pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans04_RA_thresholds/ANS03_ROC_curves/ESR1");
        log.info("ROC-curve for ESR1");
        calculateRocCurves(pathToTrack, numberOfBestSites, pathToFolderWithSiteModels, siteModelName, pathToSequences, pathToOutputFolder);
    }

    private static void calculateRocCurves(DataElementPath pathToTrack, int numberOfBestSites, DataElementPath pathToFolderWithSiteModels, String siteModelName, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
    {
        int w = 100;
        SiteModel siteModel = pathToFolderWithSiteModels.getChildPath(siteModelName).getDataElement(SiteModel.class);
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
        int lengthOfSequenceRegion = w + siteModel.getLength();
        Object[] objects = selectBestSites(pathToTrack, numberOfBestSites);
        double[][] xValuesForCurves = new double[2][], yValuesForCurves = new double[2][];
        double[] aucs = new double[2];
        for( int i = 0; i < 2; i++ )
        {
            FunSite[] funSites = i == 0 ? (FunSite[])objects[0] : (FunSite[])objects[1];
            funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
            Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
            RocCurve rocCurve = new RocCurve(smc, sequences, 10, 0);
            double[][] curve  = rocCurve.getRocCurve();
            xValuesForCurves[i] = curve[0];
            yValuesForCurves[i] = curve[1];
            aucs[i] = rocCurve.getAuc();
            log.info(i + ") AUC = " + aucs[i]);
        }
        DataMatrix dm = new DataMatrix(new String[]{"Low RA-scores", "High RA-scores"}, "AUC", aucs);
        dm.writeDataMatrix(false, pathToOutputFolder, "AUCs", log);
        Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, new String[]{"Low RA-scores", "High RA-scores"}, null, null, null, null, "Specificity", "Sensitivity", true);
        TableAndFileUtils.addChartToTable("chart with ROC-curve", chart, pathToOutputFolder.getChildPath("_chart_with_ROC_curve"));
    }
    
    private static void calculateRocCurves(FunSite[][] allFunSites, String[] rocCurveNames, String tfName, DataElementPath pathToFolderWithSiteModels, String siteModelName, DataElementPath pathToSequences, DataElementPath pathToOutputFolder)
    {
        int w = 50;
        SiteModel siteModel = pathToFolderWithSiteModels.getChildPath(siteModelName).getDataElement(SiteModel.class);
        SiteModelComposed smc = new SiteModelComposed(new SiteModel[]{siteModel}, null, null, true);
        int lengthOfSequenceRegion = w + siteModel.getLength();
//        Object[] objects = selectBestSites(pathToTrack, numberOfBestSites);
        double[][] xValuesForCurves = new double[rocCurveNames.length][], yValuesForCurves = new double[rocCurveNames.length][];
        double[] aucs = new double[rocCurveNames.length];
        for( int i = 0; i < rocCurveNames.length; i++ )
        {
            FunSite[] funSites = allFunSites[i];
            funSites = FunSiteUtils.removeUnusualChromosomes(pathToSequences, funSites);
            Sequence[] sequences = FunSiteUtils.getLinearSequencesWithGivenLength(funSites, pathToSequences, lengthOfSequenceRegion);
            RocCurve rocCurve = new RocCurve(smc, sequences, 10, 0);
            double[][] curve  = rocCurve.getRocCurve();
            xValuesForCurves[i] = curve[0];
            yValuesForCurves[i] = curve[1];
            aucs[i] = rocCurve.getAuc();
            log.info(i + ") AUC = " + aucs[i]);
        }
        DataMatrix dm = new DataMatrix(rocCurveNames, "AUC", aucs);
        dm.writeDataMatrix(false, pathToOutputFolder, "AUCs_" + tfName, log);
        Chart chart = ChartUtils.createChart(xValuesForCurves, yValuesForCurves, rocCurveNames, null, null, null, null, "Specificity", "Sensitivity", true);
        TableAndFileUtils.addChartToTable("chart with ROC-curve", chart, pathToOutputFolder.getChildPath("_chart_with_ROC_curve_" + tfName));
    }
    
    
    // It is transformed into TrackUtils.readBestSitesInTrack()
    private static Object[] selectBestSites(DataElementPath pathToTrack, int numberOfBestSites)
    {
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToTrack);
        double[] scores = FunSiteUtils.getRaScores(funSites), scoresClone = scores.clone();
        UtilsForArray.sortInAscendingOrder(scoresClone);
        double thresholdMin = scoresClone[numberOfBestSites - 1], thresholdMax = scoresClone[funSites.length - numberOfBestSites];
        log.info("thresholdMin = " + thresholdMin + " thresholdMax = " + thresholdMax);
        List<FunSite> listMin = new ArrayList<>(), listMax = new ArrayList<>();
        for( int i = 0; i < funSites.length; i++ )
        {
            if( scores[i] <= thresholdMin )
                listMin.add(funSites[i]);
            if( scores[i] >= thresholdMax )
                listMax.add(funSites[i]);
        }
        log.info(" nMin = " + listMin.size() + " nMax = " + listMax.size());
        return new Object[]{listMin.toArray(new FunSite[0]), listMax.toArray(new FunSite[0])};
    }

    public static void selectBestSites(DataElementPath pathToFolderWithTracks, String inputTrackName, int numberOfBestSites, DataElementPath pathToOutputFolder, String outputTrackName)
    {
        // 1. Read RA-scores and calculate threshold.
        Track track = pathToFolderWithTracks.getChildPath(inputTrackName).getDataElement(Track.class);
        DataCollection<Site> sites = track.getAllSites();
        int n = sites.getSize(), i = 0;
        if( n <= numberOfBestSites ) return;
        String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
        double[] scores = new double[n];
        for( Site site : sites )
            scores[i++] = SiteUtils.getProperties(site, propertiesNames)[0];
        UtilsForArray.sortInAscendingOrder(scores);
        double threshold = scores[numberOfBestSites - 1];
        
        // 2. Select best sites and write them into output track.
        SqlTrack outputTrack = SqlTrack.createTrack(pathToOutputFolder.getChildPath(outputTrackName), null);
        //        Site site = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, getStartPosition(), getLength(), Site.PRECISION_NOT_KNOWN, strand, null, null);

        for( Site site : sites )
            if( SiteUtils.getProperties(site, propertiesNames)[0] <= threshold )
                outputTrack.addSite(site);
        outputTrack.finalizeAddition();
        CollectionFactoryUtils.save(track);
    }
    
    private static FunSite[] getSubset(FunSite[] funSites, int numberOfSelectedIndices)
    {
        int seed = 0;
        int[] randomArray = RandomUtils.selectIndicesRandomly(funSites.length, numberOfSelectedIndices, seed);
        FunSite[] result = new FunSite[numberOfSelectedIndices];
        for( int i = 0; i < numberOfSelectedIndices; i++ )
            result[i] = funSites[randomArray[i]];
        return result;
    }
    
    public static void calculateRaSThresholds(DataElementPath pathToOutputFolder)
    {
        int numberOfSelectedIndices = 100000;
        boolean doGetSubsetOfFunSites = true;
        ru.biosoft.access.core.DataElementPath[] paths = new ru.biosoft.access.core.DataElementPath[]
                {
                
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P55317"),
                DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372")

                };
        
        for( int i = 0; i < paths.length; i++ )
        {
            // 3.1.
            String name = paths[i].getName();
            log.info("i = " + i + " trackNames[i] = " + name);
            FunSite[] funSites = FunSiteUtils.getFunSites(paths[i]);
            log.info("size of funSites = " + funSites.length);
            if( doGetSubsetOfFunSites )
                funSites = getSubset(funSites, numberOfSelectedIndices);
            double[] scores = FunSiteUtils.getRaScores(funSites);
            log.info("size of scores = " + scores.length);
            NormalMixture normalMixture = new NormalMixture(scores, 2, null,null, 300);
            DataMatrix dm = normalMixture.getParametersOfComponents();
            if( dm.getSize() < 2 )
            {
                TableAndFileUtils.addRowToTable(UtilsForArray.getConstantArray(8, Double.NaN), null, name, new String[]{"p1", "mean1", "sigma1", "p2", "mean2", "sigma2", "RA_threshold_mean2-3_sigma2", "RA_threshold_mean2-2_sigma2"}, pathToOutputFolder, "RA_score_thresholds");
                continue;
            }
            if( doGetSubsetOfFunSites )
            {
                Chart chart = normalMixture.createChartWithDensities(RankAggregation.RA_SCORE + "_" + name);
                TableAndFileUtils.addChartToTable("chart with RA-scores", chart, pathToOutputFolder.getChildPath(name + "_chart_mixture"));
            }
            
            // 3.2.
            double[] means = dm.getColumn("Mean value");
            String[] rowNames = dm.getRowNames();
            double[] row = means[0] < means[1] ? ArrayUtils.addAll(dm.getRow(rowNames[0]), dm.getRow(rowNames[1])) : ArrayUtils.addAll(dm.getRow(rowNames[1]), dm.getRow(rowNames[0]));
            double[] rowAdditional = new double[]{row[4] - 3.0 * row[5], row[4] - 2.0 * row[5]}; 
            row = ArrayUtils.addAll(row, rowAdditional);
            TableAndFileUtils.addRowToTable(row, null, name, new String[]{"p1", "mean1", "sigma1", "p2", "mean2", "sigma2", "RA_threshold_mean2-3_sigma2", "RA_threshold_mean2-2_sigma2"}, pathToOutputFolder, "RA_score_thresholds");
            log.info("thresholds = " + row[6] + " " + row[7]);
        }
    }

    // TODO: It must be removed when it will be implemented in 'CistromConstructionAdvanced' analysis.
    public static void calculateRaSThresholds(DataElementPath pathToFolderWithTracks, DataElementPath pathToOutputFolder)
    {
        // 1.
        DataElementPath pathToTableWithSummary = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans03_sammary_extended_on_tracks/summary_extended_on traks");
        DataElementPath path = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P03372");
        Track trackWithMetaClusters = path.getDataElement(Track.class);
        log.info("size of metaClusters = " + trackWithMetaClusters.getAllSites().getSize());
        String tfClass = "2.1.1.2.1";
        
        // 2.
        DataMatrixString dms = new DataMatrixString(pathToTableWithSummary, new String[]{"TF-class"}); 
        String[] trackNamesInTable = dms.getRowNames(), tfCasses = dms.getColumn(0);
        List<String> list = new ArrayList<>();
        for( int i = 0; i < trackNamesInTable.length; i++ )
            if( tfCasses[i].equals(tfClass) )
                list.add(trackNamesInTable[i]);
        String[] trackNames = list.toArray(new String[0]);
        log.info("number of tracks = " + trackNames.length);

        // 3.
        for( int i = 0; i < trackNames.length; i++ )
        {
            // 3.1.
            log.info("i = " + i + " trackNames[i] = " + trackNames[i]);
            DataElementPath pathToTrack = pathToFolderWithTracks.getChildPath(trackNames[i]);
            FunSite[] funSites = FunSiteUtils.getFunSites(pathToTrack);
            log.info("size of funSites = " + funSites.length);
            double[] scoresInduced = calculateInducedRaScores(funSites, trackWithMetaClusters);
            log.info("size of scoresInduced = " + scoresInduced.length);
            NormalMixture normalMixture = new NormalMixture(scoresInduced, 2, null,null, 300);
            DataMatrix dm = normalMixture.getParametersOfComponents();
            if( dm.getSize() < 2 )
            {
                TableAndFileUtils.addRowToTable(UtilsForArray.getConstantArray(8, Double.NaN), null, trackNames[i], new String[]{"p1", "mean1", "sigma1", "p2", "mean2", "sigma2", "RA_threshold_mean2-3_sigma2", "RA_threshold_mean2-2_sigma2"}, pathToOutputFolder, "RA_score_thresholds");
                continue;
            }
            
            // 3.2.
            double[] means = dm.getColumn("Mean value");
            String[] rowNames = dm.getRowNames();
            double[] row = means[0] < means[1] ? ArrayUtils.addAll(dm.getRow(rowNames[0]), dm.getRow(rowNames[1])) : ArrayUtils.addAll(dm.getRow(rowNames[1]), dm.getRow(rowNames[0]));
            double[] rowAdditional = new double[]{row[4] - 3.0 * row[5], row[4] - 2.0 * row[5]}; 
            row = ArrayUtils.addAll(row, rowAdditional);
            TableAndFileUtils.addRowToTable(row, null, trackNames[i], new String[]{"p1", "mean1", "sigma1", "p2", "mean2", "sigma2", "RA_threshold_mean2-3_sigma2", "RA_threshold_mean2-2_sigma2"}, pathToOutputFolder, "RA_score_thresholds");
            log.info("thresholds = " + row[6] + " " + row[7]);
        }
    }

    public static void treatRaScoresAndInducedRaScores(DataElementPath pathToFolderWithTracks, DataElementPath pathToOutputFolder)
    {
        String[] trackNames = new String[]{"PEAKS033434", "PEAKS033494", "PEAKS033616", "PEAKS033665", "PEAKS041349"};
        DataElementPath path = DataElementPath.create("databases/GTRD/Data/new_clusters/Homo sapiens/By TF/P05412");
        Track trackWithMetaClusters = path.getDataElement(Track.class);
        log.info("size of metaClusters = " + trackWithMetaClusters.getAllSites().getSize());
        
        for( int i = 0; i < trackNames.length; i++ )
        {
            log.info("i = " + i + " trackNames[i] = " + trackNames[i]);
            if( ! pathToFolderWithTracks.getChildPath(trackNames[i]).exists() )
                log.info("track is not exist");
            treatRaScoresAndInducedRaScores(pathToFolderWithTracks.getChildPath(trackNames[i]), trackWithMetaClusters, pathToOutputFolder);
        }
    }
    
    private static void treatRaScoresAndInducedRaScores(DataElementPath pathToTrack, Track trackWithMetaClusters, DataElementPath pathToOutputFolder)
    {
        // 1.Calculate scores.
        FunSite[] funSites = FunSiteUtils.getFunSites(pathToTrack);
        log.info("size of funSites = " + funSites.length);
        double[] scores = FunSiteUtils.getRaScores(funSites);
        log.info("size of scores = " + scores.length);
        double[] scoresInduced = calculateInducedRaScores(funSites, trackWithMetaClusters);
        log.info("size of scoresInduced = " + scoresInduced.length);
        
        // 2. Chart for scores and induced scores.
        String trackName = pathToTrack.getName(); 
        Chart chart = DensityEstimation.createChartWithSmoothedDensities(new double[][]{scores, scoresInduced}, new String[]{RankAggregation.RA_SCORE, RankAggregation.RA_SCORE + "_induced"}, "Score", true, null, DensityEstimation.WINDOW_WIDTH_01, null);
        TableAndFileUtils.addChartToTable("chart with RA-scores", chart, pathToOutputFolder.getChildPath(trackName + "_chart_RA_scores"));
        log.info("chart with scores and induced scores is constructed");

        // 3. Normal mixture.
        NormalMixture normalMixture = new NormalMixture(scoresInduced, 2, null,null, 300);
        DataMatrix dm = normalMixture.getParametersOfComponents();
        dm.writeDataMatrix(false, pathToOutputFolder, trackName + "_mixture_parameters", log);
        chart = normalMixture.createChartWithDensities(RankAggregation.RA_SCORE + "_induced");
        TableAndFileUtils.addChartToTable("chart with RA-scores", chart, pathToOutputFolder.getChildPath(trackName + "_chart_mixture"));
        log.info("normal mixture is applied");
    }

//    private static FunSite[] getFunSites(DataElementPath pathToTrack)
//    {
//        Track track = pathToTrack.getDataElement(Track.class);
//        Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 1, Integer.MAX_VALUE, new String[]{RankAggregation.RA_SCORE}, track.getName());
//        return FunSiteUtils.transformToArray(sites);
//    }
    
    private static FunSite[] getFunSitesFromOldMetaClusters(DataElementPath pathToTrack)
    {
        Track track = pathToTrack.getDataElement(Track.class);
        Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 1, Integer.MAX_VALUE, new String[]{"peak.count"}, track.getName());
        return FunSiteUtils.transformToArray(sites);
    }

    // It was modified and was moved to FunSiteUtils
    // 10.03.22
//    public static double[] getRaScores(FunSite[] funSites)
//    {
//        DataMatrix dm = FunSiteUtils.getDataMatrixGeneralized(funSites);
//        //return dm.getColumn(RankAggregation.RA_SCORE);
//        return dm.getColumn(0);
//    }

    private static double[] calculateInducedRaScores(FunSite[] funSites, Track trackWithMetaClusters)
    {
        List<Double> list = new ArrayList<>();
        String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
        for( FunSite fs : funSites )
        {
            DataCollection<Site> dc = trackWithMetaClusters.getSites(fs.getChromosomeName(), fs.getStartPosition(), fs.getFinishPosition());
            for( Site site : dc )
            {
                double[] properties = SiteUtils.getProperties(site, propertiesNames);
                // list.add(PrimitiveOperations.getAverage(properties));
                list.add(properties[0]);
            }
        }
        return UtilsGeneral.fromListToArray(list);
    }
    
    private static double[] calculateInducedRaScoresAsAverage(FunSite[] funSites, Track trackWithMetaClusters)
    {
        double[] result = UtilsForArray.getConstantArray(funSites.length, Double.NaN);
        String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
        for( int i = 0; i <  funSites.length; i++ )
        {
            DataCollection<Site> dc = trackWithMetaClusters.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
            if( dc.getSize() >  0 )
                for( Site site : dc )
                {
                    double[] properties = SiteUtils.getProperties(site, propertiesNames);
                    result[i] = PrimitiveOperations.getAverage(properties);
                }
        }
        return result;
    }
    
    private static boolean[] areCoveredByTrack(FunSite[] funSites, Track track)
    {
        boolean[] result = UtilsForArray.getConstantArray(funSites.length, false);
        for( int i = 0; i < funSites.length; i++ )
        {
            DataCollection<Site> dc = track.getSites(funSites[i].getChromosomeName(), funSites[i].getStartPosition(), funSites[i].getFinishPosition());
            if( dc.getSize() > 0 )
                result[i] = true;
        }
        return result;
    }
    
    /********************************************************************/
    /*** Calculate 1-st type error of meta-clusters on TRANSFAC-sites ***/
    /********************************************************************/
    
    public static void parsingFileWithTransfacSites(DataElementPath pathToOutputFolder)
    {
    	// 1. Create linesNew.
        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites");
        String[] lines = TableAndFileUtils.readLinesInFile( pathToFile);
        String[] linesNew = new String[lines.length];
        linesNew[0] = "ID\t" + lines[0];
        for( int i = 1; i < lines.length; i++ )
        	linesNew[i] = "S_" + Integer.toString(i) + "\t" + lines[i];
        
        // 2. Create and write new file.
        String string = linesNew[0];
        for( int i = 1; i < lines.length; i++ )
        	string += "\n" + linesNew[i];
        TableAndFileUtils.writeStringToFile(string, pathToOutputFolder, pathToFile.getName() + "_2", log);
    }
    
    
    //'data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/first_type_errors_w_40
    public static void getDistinctCellLines(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites_3");
        String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
        String[] columnNames = new String[]{"Uniprot", "sources"};
        DataMatrixString dms = new DataMatrixString(pathToFile, columnNames);
        String[] uniprotIdsInTable = dms.getColumn("Uniprot"), cellLinesInTable = dms.getColumn("sources"); 
        //DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Mean_distance", "sigma", "Minimum", "Maximum"});
        Set<String> set = new HashSet<>();
        for( int i = 0; i < uniprotIdsInTable.length; i++ )
        {
        	log.info(" i = " + i + " uniprotIds = " + uniprotIdsInTable[i]);
        	if( uniprotIdsInTable[i] == null || uniprotIdsInTable[i].equals("") ) continue;
        	String[] tokens = TextUtil.split(uniprotIdsInTable[i], ',');
        	boolean doContainUuniprotIds = false;
        	for( String s : tokens )
        		if( ArrayUtils.contains(uniprotIds, s) )
        		{
        			doContainUuniprotIds = true;
        			break;
        		}
        	if( ! doContainUuniprotIds ) continue;
        	if( cellLinesInTable[i] == null || cellLinesInTable[i].equals("") || cellLinesInTable[i].equals("null")) continue;
        	tokens = TextUtil.split(cellLinesInTable[i], ';');
        	for( String s : tokens )
        		set.add(s);
        }
        String[] distinctCellLines = set.toArray(new String[0]);
        dms = new DataMatrixString(distinctCellLines, "distinct_cell_lines", distinctCellLines);
        dms.writeDataMatrixString(false, pathToOutputFolder, "distinct_cell_lines", log);
    }
    
//    private static Object[] calculateDistanceDetweenTransfacSitesAndMetaClusters(String[]chromosomeNames, int[] starts, int[] ends, int w, DataElementPath pathToFolderWithMetaClusterTracks, String trackName)
//    {
//    	int ww = w / 2;
//    	double[] distances = new double[starts.length];
//        for( int i = 0; i < starts.length; i++ )
//        {
//            DataElementPath pathToTrack = pathToFolderWithMetaClusterTracks.getChildPath(trackName);
//            Track track = pathToTrack.getDataElement(Track.class);
//            int center = (starts[i] + ends[i]) / 2, start = center - ww, end = center + ww;  
//            DataCollection<Site> sites = track.getSites(chromosomeNames[i], start, end);
//            int size = sites.getSize();
//            if( size < 1 )
//            	distances[i] = ww;
//            else
//            {
//            	int index = 0;
//            	double[] array = new double[size];
//            	for( Site site : sites )
//            		array[index++] = (double)Math.abs(center - site.getInterval().getCenter());
//            	Object[] objects = PrimitiveOperations.getMin(array);
//            	distances[i] = (double)objects[1];
//            }
//        }
//    	double[] meanAndSigma = UnivariateSample.getMeanAndSigma(distances);
//    	double[] minAndMax = PrimitiveOperations.getMinAndMax(distances);
//        Chart chart = DensityEstimation.createChartWithSmoothedDensities(new double[][]{distances}, new String[]{"Distance between meta-clusters and TRANSFAC sites"}, "Distance", true, null, DensityEstimation.WINDOW_WIDTH_01, null);
//    	return new Object[]{meanAndSigma, minAndMax, chart};
//    }

    private static String[] getDistinctPeakNames(DataElementPath pathToFolderWithMetaClusterTracks, String trackName)
    {
    	ru.biosoft.access.core.DataElementPath pathToTrack = pathToFolderWithMetaClusterTracks.getChildPath(trackName);
        Track track = pathToTrack.getDataElement(Track.class);
        DataCollection<Site> dc = track.getAllSites();
        String propertyName = "Additional_property";
        Set<String> set = new HashSet<>();
        for( Site site : dc )
        {
            DynamicPropertySet dps = site.getProperties();
            String string = dps.getValueAsString(propertyName);
            String[] strings = TextUtil.split(string, ',');
            for( String s : strings )
            	set.add(s);
        }
        return set.toArray(new String[0]);
    }
    
    /********* Move to GTRD utils ******/
    // Example speciesInLatin = "Homo sapiens";
    public static Species transformToGtrdSpecies(String speciesInLatin, DataElementPath pathToGtrdSpecies)
    {
        Species givenSpecie = null;
        for( Species species : pathToGtrdSpecies.getDataCollection(Species.class) )
            if( species.getLatinName().equals(speciesInLatin) )
            	return species;
        return givenSpecie;
    }
    
    /********* Move to GTRD utils ******/
    // Example speciesInLatin = "Homo sapiens";
    public static Species transformToGtrdSpecies(String speciesInLatin)
    {
       return transformToGtrdSpecies(speciesInLatin, DataElementPath.create("databases/Utils/Species"));
    }
    
    private static String[] getDistinctCellLinesInMetaClustes(String givenTfClass, String[] distinctPeakNames, DataElementPath pathToFolderWithGtrdPeakTracks, DataElementPath pathToSequences)
    {
    	
    	// 1.Identification of givenSpecie.
        DataElementPath SPECIES_PATH = DataElementPath.create("databases/Utils/Species");
        DataCollection<Species> dc = SPECIES_PATH.getDataCollection(Species.class);
        Species givenSpecie = null;
        for( Species species : dc )
            if( species.getLatinName().equals("Homo sapiens") )
            {
            	givenSpecie = species;
                break;
            }
        
        // 2.distinctCellLinesInMetaClustes
        TrackInfo[] trackInfos = TrackInfo.getTracksInfo(pathToFolderWithGtrdPeakTracks, givenSpecie, givenTfClass,  pathToSequences);
        Set<String> set = new HashSet<>();
        for( TrackInfo ti : trackInfos )
        {
        	String cellLine = ti.getCellLine(), trackName = ti.getTrackName();
            if( ! ArrayUtils.contains(distinctPeakNames, trackName) || cellLine == null ) continue;
            set.add(cellLine);
        }
        return set.toArray(new String[0]);
//    		    public static TrackInfo[] getTracksInfo(DataElementPath pathToFolderWithTracks, Species givenSpecie, String givenTfClass, DataElementPath pathToSequences)
//    	        //  private Species species = Species.getDefaultSpecies(null);
//    	        DataElementPath SPECIES_PATH = DataElementPath.create("databases/Utils/Species");
//    	        Species givenSpecie = null;
//    	        for(Species species : SPECIES_PATH.getDataCollection(Species.class) )
//    	            if( species.getLatinName().equals("Homo sapiens") )
//    	            {
//    	                givenSpecie = species;
//    	                break;
//    	            }
//    	        log.info("givenSpecie = " + givenSpecie.getLatinName());
   }
    
    // The cell lines in meta-clusters and Transfac sites are considered. 
//    public static void calculateFirstTypeErrorByTransfacSites3(DataElementPath pathToFolderWithMetaClusterTracks, DataElementPath pathToOutputFolder)
//    {
//    	int w = 40;
//    	ru.biosoft.access.core.DataElementPath pathToFolderWithGtrdPeakTracks = DataElementPath.create("databases/GTRD/Data/peaks/macs");
//    	ru.biosoft.access.core.DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38");
//        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites_3");
//        String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
//        String[] tfClasses = new String[]{"2.3.1.1.1", "2.3.1.1.3", "6.1.1.2.1", "1.1.1.1.1", "6.1.1.1.1", "3.3.1.1.1", "2.1.1.2.1"};
//        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Test_sample_size", "1-st_type_error"});
//        for( int i = 0; i < uniprotIds.length; i++ )
//        {
//        	// 1. Identification of distinctPeakNames.
//        	log.info("uniprotID = " + uniprotIds[i] + " tfClass = " + tfClasses[1]);
//        	String[] distinctPeakNames = getDistinctPeakNames(pathToFolderWithMetaClusterTracks, tfClasses[i]);
//        	log.info("distinctPeakNames :");
//            for( int j = 0; j < distinctPeakNames.length; j++ )
//            	log.info("j = " + j + " distinctPeakName = " + distinctPeakNames[j]);
//            
//        	// 2. Identification of distinctCellLinesInMetaClustes.
//            String[] distinctCellLinesInMetaClustes = getDistinctCellLinesInMetaClustes(tfClasses[i], distinctPeakNames, pathToFolderWithGtrdPeakTracks, pathToSequences);
//            for( int j = 0; j < distinctCellLinesInMetaClustes.length;j++ )
//            	log.info("j = " + j + " distinctCellLineInMetaClustes = " + distinctCellLinesInMetaClustes[j]);
//
//
//            //Object[] objects = getTransfacSiteSample(pathToFile, uniprotIds[i]);
//            Object[] objects = getTransfacSiteSampleForGivenCellLines(pathToFile, uniprotIds[i], distinctCellLinesInMetaClustes);
//
//            String[] chromosomeNames = (String[])objects[0];
//            int[] starts = (int[])objects[1], ends = (int[])objects[2];
//            double[] row = calculateFirstTypeErrorByTransfacSites(chromosomeNames, starts, ends, w, pathToFolderWithMetaClusterTracks, tfClasses[i]);
//            dmc.addRow(uniprotIds[i], row);
//        	log.info("uniprotIds = " + uniprotIds[i] + " 1-st type error = " + row[1] + " n = " + starts.length);
//        }
//        DataMatrix dm = dmc.getDataMatrix();
//        dm.writeDataMatrix(false, pathToOutputFolder, "first_type_errors_w_" + Integer.toString(w) + "_in_cells", log);
//    }

//    public static void testOfClassTransfacSites(DataElementPath pathToOutputFolder)
//    {
//    	ru.biosoft.access.core.DataElementPath pathToFileWithTranspathIdAndUniprotId = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/transpath2uniprot.txt");
//    	ru.biosoft.access.core.DataElementPath pathToFileWithTransfacFactors = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/factor.dat");
//    	ru.biosoft.access.core.DataElementPath pathToFileWithTransfacSites = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/site.dat");
//    	String uniprotId = "P08047";
//    	TransfacSites transfacSites = new TransfacSites(pathToFileWithTranspathIdAndUniprotId, pathToFileWithTransfacFactors, pathToFileWithTransfacSites, uniprotId);
//    	String string = transfacSites.toString();
//    	TableAndFileUtils.writeStringToFile(string, pathToOutputFolder, uniprotId + "_sites", log);
//    }
    
    
    
    
    // Test (comparison) of Ivan sites and my Transfac sites
//    public static void testOfClassTransfacSites2(DataElementPath pathToOutputFolder)
//    {
//    	ru.biosoft.access.core.DataElementPath pathToFileWithOldSites = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites_overlapped/Ans_03_3/P08047");
//    	ru.biosoft.access.core.DataElementPath pathToFileWithNewSites = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans05_transfac_test/P08047_sites");
//    	
//    	DataMatrixString dmsOld = new DataMatrixString(pathToFileWithOldSites, new String[]{"chromosome", "end"});
//    	DataMatrixString dmsNew = new DataMatrixString(pathToFileWithNewSites, new String[]{"chromosome", "start", "end"});
//    	String[] chrOld = dmsOld.getColumn("chromosome"), chrNew = dmsNew.getColumn("chromosome"); 
//    	String[]rowNamesNew = dmsNew.getRowNames(), startsNew = dmsNew.getColumn("start"), endsNew = dmsNew.getColumn("end"), endsOld = dmsOld.getColumn("end");
//    	
//        for( int i = 0; i < chrNew.length; i++ )
//        	if( chrNew[i].equals("23") )
//        		chrNew[i] = "X";
//        	else if( chrNew[i].equals("24") )
//        		chrNew[i] = "Y";
//
//
//    	log.info("old size = " + endsOld.length + " new size = " + rowNamesNew.length);
//        DataMatrixStringConstructor dmc = new DataMatrixStringConstructor(new String[]{"chromosome_new", "start_new", "end_new", "extended_name"});
//        for( int i = 0; i < rowNamesNew.length; i++ )
//        {
//        	String newSiteName = "no";
//            for( int j = 0; j < endsOld.length; j++ )
//            	if( chrNew[i].equals(chrOld[j]) && endsNew[i].equals(endsOld[j]) )
//            	{
//            		newSiteName = "yes";
//            		break;
//            		
//            	}
//            if( newSiteName.equals("yes") ) continue;
//            newSiteName = "chr" + chrNew[i] + ":" + startsNew[i] + ".." + endsNew[i];
//            dmc.addRow(rowNamesNew[i], new String[]{chrNew[i], startsNew[i], endsNew[i], newSiteName});
//        }
//        DataMatrixString dms = dmc.getDataMatrixString();
//        dms.writeDataMatrixString(true, pathToOutputFolder, "the_absent_sites", log);
//    }

    public static void tansfacSitesAndMetaClustersAndMotifs(DataElementPath pathToOutputFolder)
    {
    	int minimalLengthOfTransfacSites = 40;
        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites_2");
        DataElementPath pathToFolderWithMetaClusterTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human");
        DataElementPath pathToFolderWithPeakTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human/Combined_peaks");
        DataElementPath pathToFolderWithMotifTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C01_predictions_");
        String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
        String[] tfClasses = new String[]{"2.3.1.1.1", "2.3.1.1.3", "6.1.1.2.1", "1.1.1.1.1", "6.1.1.1.1", "3.3.1.1.1", "2.1.1.2.1"};
        String[] motifTrackNames = new String[]{"Sp1_m", "Sp3_m", "REL_m", "Jun_m", "NFKB1_m", "FoxA1_m", "ESR1_m"};
        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"alfa1_meta", "alfa1_peaks", "corr_meta_motif"});
        for( int i = 0; i < uniprotIds.length; i++ )
        {
        	log.info("i = " + i + " uniprotIds = " + uniprotIds[i]);
            Object[] objects = getTransfacSiteSample(pathToFile, uniprotIds[i]);
            String[] chromosomeNames = (String[])objects[0];
            int[] starts = (int[])objects[1], ends = (int[])objects[2];
            
            // site length correction
            for( int j = 0; j < starts.length; j++ )
            {
            	int length = ends[j] - starts[j] + 1;
            	if( length >= minimalLengthOfTransfacSites) continue;
            	int center = (starts[j] + ends[j]) / 2, halfLength = length / 2;
            	starts[j] = center - halfLength;
            	ends[j] = center + halfLength;
            }
            
            //
            String[] distinctPeakNames = getDistinctPeakNames(pathToFolderWithMetaClusterTracks, tfClasses[i]);
            int[] frequencies = calculateOverlapFrequencies(chromosomeNames, starts, ends, pathToFolderWithPeakTracks, distinctPeakNames);
            
            //
            String[] overlappedWithMetaClustres = areOverlappedWithTrack(chromosomeNames, starts, ends, pathToFolderWithMetaClusterTracks, tfClasses[i]);
            String[] overlappedWithMotifs = areOverlappedWithTrack(chromosomeNames, starts, ends, pathToFolderWithMotifTracks, motifTrackNames[i]);
            
            //
            DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"chromosome", "start", "end", "Is_overlapped_with_meta_clustres", "Is_overlapped_with_motifs", "Frequency", "Frequency-relative"});
            for( int j = 0; j < starts.length; j++ )
            	dmsc.addRow("TRANSFAC_" + Integer.toString(j), new String[]{chromosomeNames[j], Integer.toString(starts[j]), Integer.toString(ends[j]), overlappedWithMetaClustres[j], overlappedWithMotifs[j], Integer.toString(frequencies[j]), Double.toString((double)frequencies[j] / (double)distinctPeakNames.length)});
            DataMatrixString dms = dmsc.getDataMatrixString();
            dms.writeDataMatrixString(true, pathToOutputFolder, uniprotIds[i], log);
                        
        	//
        	double[] x = new double[overlappedWithMetaClustres.length], y = new double[overlappedWithMetaClustres.length];
            for( int j = 0; j < overlappedWithMetaClustres.length; j++ )
            {
            	x[j] = overlappedWithMetaClustres[j].equals("Yes") ? 1.0 : 0.0;
            	y[j] = overlappedWithMotifs[j].equals("Yes") ? 1.0 : 0.0;
            }
            double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(x, y);

            //
            int freq = 0, freq1 = 0;
            for( int j = 0; j < starts.length; j++ )
            {
            	if( overlappedWithMetaClustres[j].equals("No") )
            		freq++;
            	if( frequencies[j] < 1 )
            		freq1++;
            }
            dmc.addRow(uniprotIds[i], new double[]{(double)freq / (double)starts.length, (double)freq1 / (double)starts.length, corr});
        }
        DataMatrix dm = dmc.getDataMatrix();
        dm.writeDataMatrix(false, pathToOutputFolder, "summary", log);
    }

    private static int[] calculateOverlapFrequencies(String[]chromosomeNames, int[] starts, int[] ends, DataElementPath pathToFolderWithTracks, String[] trackNames)
    {
    	int[] result = UtilsForArray.getConstantArray(starts.length, 0);
    	for( int i = 0; i < trackNames.length; i++ )
    	{
            Track track = pathToFolderWithTracks.getChildPath(trackNames[i]).getDataElement(Track.class);
        	for( int j = 0; j < starts.length; j++ )
        		if( track.getSites(chromosomeNames[j], starts[j], ends[j]).getSize() > 0 )
        			result[j]++;
    	}
      	return result;
    }
    
    private static String[] areOverlappedWithTrack(String[]chromosomeNames, int[] starts, int[] ends, DataElementPath pathToFolderTracks, String trackName)
    {
    	String[] result = new String[starts.length];
        for( int i = 0; i < starts.length; i++ )
        {
            Track track = pathToFolderTracks.getChildPath(trackName).getDataElement(Track.class);
            DataCollection<Site> dc = track.getSites(chromosomeNames[i], starts[i], ends[i]);
            result[i] = dc.getSize() > 0 ? "Yes" : "No";
        }
    	return result;
    }

//    public static void calculateFirstTypeErrorByTransfacSites(DataElementPath pathToFolderWithMetaClusterTracks, DataElementPath pathToOutputFolder)
//    {
//    	int w = 40;
//    	
//        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites_2");
//        String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
//        String[] tfClasses = new String[]{"2.3.1.1.1", "2.3.1.1.3", "6.1.1.2.1", "1.1.1.1.1", "6.1.1.1.1", "3.3.1.1.1", "2.1.1.2.1"};
//        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Test_sample_size", "1-st_type_error"});
//        for( int i = 0; i < uniprotIds.length; i++ )
//        {
//            Object[] objects = getTransfacSiteSample(pathToFile, uniprotIds[i]);
//            String[] chromosomeNames = (String[])objects[0];
//            int[] starts = (int[])objects[1], ends = (int[])objects[2];
//            double[] row = calculateFirstTypeErrorByTransfacSites(chromosomeNames, starts, ends, w, pathToFolderWithMetaClusterTracks, tfClasses[i]);
//            dmc.addRow(uniprotIds[i], row);
//        	log.info("uniprotIds = " + uniprotIds[i] + " 1-st type error = " + row[1]);
//        }
//        DataMatrix dm = dmc.getDataMatrix();
//        dm.writeDataMatrix(false, pathToOutputFolder, "first_type_errors_w_" + Integer.toString(w), log);
//    }
    
    public static void saveTransfacSitesToTracks(DataElementPath pathToOutputFolder)
    {
    	//int w = 40;
        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites_2");
        String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
        for( int i = 0; i < uniprotIds.length; i++ )
        {
        	Object[] objects = getTransfacSiteSample(pathToFile, uniprotIds[i]);
            String[] chromosomeNames = (String[])objects[0];
            int[] starts = (int[])objects[1], ends = (int[])objects[2];
            SqlTrack outputTrack = SqlTrack.createTrack(pathToOutputFolder.getChildPath(uniprotIds[i]), null);
            for( int j = 0; j < chromosomeNames.length; j++ )
            {
            	int strand = 0;
            	Site site = new SiteImpl(null, chromosomeNames[j], null, Site.BASIS_USER_ANNOTATED, starts[j], (ends[j] - starts[j] + 1), Site.PRECISION_NOT_KNOWN, strand, null, null);
            	outputTrack.addSite(site);
            }
            outputTrack.finalizeAddition();
            CollectionFactoryUtils.save(outputTrack);
        }
    }
    
    // for old (Ivan) meta-clusters
//    public static void calculateFirstTypeErrorByTransfacSites2(DataElementPath pathToFolderWithMetaClusterTracks, DataElementPath pathToOutputFolder)
//    {
//    	int w = 40;
//        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites_2");
//        String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
//        //String[] tfClasses = new String[]{"2.3.1.1.1", "2.3.1.1.3", "6.1.1.2.1", "1.1.1.1.1", "6.1.1.1.1", "3.3.1.1.1", "2.1.1.2.1"};
//        DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Test_sample_size", "1-st_type_error"});
//        for( int i = 0; i < uniprotIds.length; i++ )
//        {
//            Object[] objects = getTransfacSiteSample(pathToFile, uniprotIds[i]);
//            String[] chromosomeNames = (String[])objects[0];
//            int[] starts = (int[])objects[1], ends = (int[])objects[2];
//            //double[] row = calculateFirstTypeErrorByTransfacSites(chromosomeNames, starts, ends, w, pathToFolderWithMetaClusterTracks, tfClasses[i]);
//            double[] row = calculateFirstTypeErrorByTransfacSites(chromosomeNames, starts, ends, w, pathToFolderWithMetaClusterTracks, uniprotIds[i]);
//            dmc.addRow(uniprotIds[i], row);
//        	log.info("uniprotIds = " + uniprotIds[i] + " 1-st type error = " + row[1]);
//        }
//        DataMatrix dm = dmc.getDataMatrix();
//        dm.writeDataMatrix(false, pathToOutputFolder, "first_type_errors_w_" + Integer.toString(w), log);
//    }
    
//    public static double[] calculateFirstTypeErrorByTransfacSites(String[]chromosomeNames, int[] starts, int[] ends, int w, DataElementPath pathToFolderWithMetaClusterTracks, String trackName)
//    {
//    	int ww = w / 2, coveredNumber = 0;
//        for( int i = 0; i < starts.length; i++ )
//        {
//            DataElementPath pathToTrack = pathToFolderWithMetaClusterTracks.getChildPath(trackName);
//            Track track = pathToTrack.getDataElement(Track.class);
//            int center = (starts[i] + ends[i]) / 2, start = center - ww, end = center + ww;  
//            DataCollection<Site> dc = track.getSites(chromosomeNames[i], start, end);
//            if( dc.getSize() > 0 )
//            	coveredNumber++;
//        }
//    	return new double[]{(double)starts.length, (double)(starts.length - coveredNumber) / (double)starts.length};
//    }

    private static Object[] getTransfacSiteSample(DataElementPath pathToFileWithTransfacSites, String uniprotId)
    {
    	// 1. Get sub-DataMatrixString with given uniprotId.
    	String[] columnNames = new String[]{"#CHROM", "START", "END", "Uniprot"};
        DataMatrixString dms = new DataMatrixString(pathToFileWithTransfacSites, columnNames);
        String[] column = dms.getColumn("Uniprot"), rowNames = dms.getRowNames();
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(columnNames);
        for( int i = 0; i < column.length; i++ )
        {
        	if( column[i] == null || column[i].equals("") ) continue;
        	String[] tokens = TextUtil.split(column[i], ',');
        	if( ! ArrayUtils.contains(tokens, uniprotId) ) continue;
        	DataMatrixString preRow = dms.getRow(i);
        	String[] row = preRow.getRow(rowNames[i]);
        	dmsc.addRow(rowNames[i], row);
        }
        dms = dmsc.getDataMatrixString();

        // 2. Calculate results.
        rowNames = dms.getRowNames();
        int[] starts = new int[rowNames.length], ends = new int[rowNames.length];
        String[] startsString = dms.getColumn(columnNames[1]), endsString = dms.getColumn(columnNames[2]), chromosomes = dms.getColumn(columnNames[0]), chromosomeNames = new String[rowNames.length];
        for( int i = 0; i < rowNames.length; i++ )
        {
        	starts[i] = Integer.parseInt(startsString[i]);
        	ends[i] = Integer.parseInt(endsString[i]);
        	chromosomeNames[i] = chromosomes[i].substring(3, chromosomes[i].length());
        }
    	return new Object[]{chromosomeNames, starts, ends};
    }
    
    private static Object[] getTransfacSiteSampleForGivenCellLines(DataElementPath pathToFileWithTransfacSites, String uniprotId, String[] givenCellLinesInMetaClusters)
    {
    	// 1. Get sub-DataMatrixString with given uniprotId.
        DataElementPath pathToFileWithDistincrCellLines = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/distinct_cell_lines_4");
        DataMatrixString dmsWithDistinctCellLines = new DataMatrixString(pathToFileWithDistincrCellLines, new String[]{"gtrd_name"});
        String[] distinctCellLinesInGtrd = dmsWithDistinctCellLines.getColumn(0), distinctCellLinesInTransfac = dmsWithDistinctCellLines.getRowNames(); 
        

    	String[] columnNames = new String[]{"#CHROM", "START", "END", "Uniprot", "sources"};
        DataMatrixString dms = new DataMatrixString(pathToFileWithTransfacSites, columnNames);
        String[] column = dms.getColumn("Uniprot"), rowNames = dms.getRowNames();
        String[] cellLinesInTransfac = dms.getColumn("sources");
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(columnNames);
        for( int i = 0; i < column.length; i++ )
        {
        	if( column[i] == null || column[i].equals("") ) continue;
        	int index = ArrayUtils.indexOf(distinctCellLinesInTransfac, cellLinesInTransfac[i]);
        	if(index < 1 ) continue;
        	String cellLineInGtrd = distinctCellLinesInGtrd[index];
        	if( ! ArrayUtils.contains(givenCellLinesInMetaClusters, cellLineInGtrd)) continue;
        	
        	String[] tokens = TextUtil.split(column[i], ',');
        	if( ! ArrayUtils.contains(tokens, uniprotId) ) continue;
        	DataMatrixString preRow = dms.getRow(i);
        	String[] row = preRow.getRow(rowNames[i]);
        	dmsc.addRow(rowNames[i], row);
        }
        dms = dmsc.getDataMatrixString();

        // 2. Calculate results.
        rowNames = dms.getRowNames();
        int[] starts = new int[rowNames.length], ends = new int[rowNames.length];
        String[] startsString = dms.getColumn(columnNames[1]), endsString = dms.getColumn(columnNames[2]), chromosomes = dms.getColumn(columnNames[0]), chromosomeNames = new String[rowNames.length];
        for( int i = 0; i < rowNames.length; i++ )
        {
        	starts[i] = Integer.parseInt(startsString[i]);
        	ends[i] = Integer.parseInt(endsString[i]);
        	chromosomeNames[i] = chromosomes[i].substring(3, chromosomes[i].length());
        }
    	return new Object[]{chromosomeNames, starts, ends};
    }


    public static void summaryOnTransfacSites(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToFile = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/Ans04_transfac-sites/human_transfac_2020.3_sites_2");
        DataMatrixString dms = new DataMatrixString(pathToFile, new String[]{"#CHROM", "START", "END", "Uniprot"});
        String[] column = dms.getColumn("Uniprot");
        List<String> list = new ArrayList<>();
        for( int i = 0; i < column.length; i++ )
        {
        	if( column[i] == null || column[i].equals("") ) continue;
        	String[] tokens = TextUtil.split(column[i], ',');
        	for( String s : tokens )
        		list.add(s);
        }
        Object[] objects = PrimitiveOperations.countFrequencies(list.toArray(new String[0]));
        String[] uniprotIds = (String[]) objects[0];
        int[] frequencies = (int[]) objects[1];
        double[] freq = UtilsForArray.transformIntToDouble(frequencies);
        DataMatrix dm = new DataMatrix(uniprotIds, "Frequencies", freq);
        dm.writeDataMatrix(false, pathToOutputFolder, "summary_on_sites", log);
    }

    /******************************************************/
    
    public static void calculateTableWithSummaryOnTracksAndQualityMetric(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToFolderWithTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans01_data_tracks_for_3_TFs/Combined_peaks");
        DataElementPath pathToTableWithQualityMetrics = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C07_for_article/Ans02_quality_metrix_FPCM_3.0_macs/quality_control_metrics");
        DataElementPath pathToTableWithSummary = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/summary_on_chip_seq_tracks");
       
        String[] fileNames = pathToFolderWithTracks.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);

        String[] columnNamesForQuality = new String[]{"FPCM", "FNCM_gem", "FNCM_macs2", "FNCM_pics", "FNCM_sissrs", "FPCM2"};
        DataMatrix dm = new DataMatrix(pathToTableWithQualityMetrics, columnNamesForQuality);

        String[] columnNamesForSummary = new String[]{"Number_of_sites_gem", "Number_of_sites_macs2", "Number_of_sites_pics", "Number_of_sites_sissrs"};
        DataMatrix datMat = new DataMatrix(pathToTableWithSummary, columnNamesForSummary);

        String[] columnNamesForString = new String[]{"TF-class", "TF-name", "Uniprot_ID", "Cell_line",
                                                     "Cell_line_treatment", "is_cell_line_treated", "Control_ID","Do_control_exist", "Antibody", "Matrix_name"};
        DataMatrixString dmSring = new DataMatrixString(pathToTableWithSummary, columnNamesForString);

        //
        DataMatrixConstructor dmc = new DataMatrixConstructor((String[])ArrayUtils.addAll(columnNamesForQuality, columnNamesForSummary));
        DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(columnNamesForString);
        for( int i = 0; i < fileNames.length; i++ )
        {
            double[] row = dm.getRow(fileNames[i]), row2 = datMat.getRow(fileNames[i]);
            dmc.addRow(fileNames[i], ArrayUtils.addAll(row, row2));
            String[] rowString = dmSring.getRow(fileNames[i]);
            dmsc.addRow(fileNames[i], rowString);
        }
        
        //
        dm = dmc.getDataMatrix();
        dmSring = dmsc.getDataMatrixString();
        dm.writeDataMatrix(false, dmSring, pathToOutputFolder, "summary_extended_on traks", log);
    }
    
    /**************************  SqlUsage : start *******************************************/
    public static class SqlUsage
    {
    	// How to get a Connection
        // pathToFolderWithFolders should be replaced by DataElementPath to experiments directory
        
    	//Connection con = null;
    	//ru.biosoft.access.core.DataElementPath pathToFolderWithFolders = null;
        //Connection con = ((SqlConnectionHolder) pathToFolderWithFolders.getDataElement()).getConnection();
        
        public static Connection getConnection(DataElementPath pathToFolderWithFolders)
        {
        	return ((SqlConnectionHolder) pathToFolderWithFolders.getDataElement()).getConnection();
        }

        // Some methods to get info from SQL DB
        public static List<String> getListOfTFUniprotId(Connection c)
        {
        	Query query = new Query("SELECT DISTINCT(tf_uniprot_id) FROM chip_experiments ORDER BY tf_uniprot_id");
        	return SqlUtil.queryStrings(c, query);
        }
        
        public static List<String> getListOfTFUniprotId(Connection c, Species species)
        {
        	Query query;
        	if(species == null)
        		return getListOfTFUniprotId(c);
        	
        	query = new Query("SELECT DISTINCT(tf_uniprot_id) FROM chip_experiments WHERE specie=$species$ ORDER BY tf_uniprot_id");
        	query.str(species.getLatinName());
        	return SqlUtil.queryStrings(c, query);
        }
        
        public static List<String> convertTfClassToUniprotId(Connection c, String tfClassId)
        {
        	Query query = new Query("SELECT id FROM uniprot WHERE cached_tf_class=$tfClassId$");
        	query.str(tfClassId);
        	return SqlUtil.queryStrings(c, query);
        }
        
        public static List<String> convertTfClassToUniprotId(Connection c, String tfClassId, Species species)
        {
        	if(species == null)
        		return convertTfClassToUniprotId(c, tfClassId);
        	
        	Query query = new Query("SELECT id FROM uniprot WHERE cached_tf_class=$tfClassId$ and species=$species$");
        	query.str("tfClassId", tfClassId);
        	query.str("species", species.getLatinName());
        	return SqlUtil.queryStrings(c, query);
        }
        
        public static List<String> getPeakIdsForTF(Connection c, String uniprotId)
        {
        	Query query = new Query("SELECT hub.input FROM chip_experiemnts exps "
        			+ "join hub on hub.output=exps.id WHERE hub.input like '%PEAKS%' and tf_uniprot_id=$uniprotId$");
        	query.str(uniprotId);
        	return SqlUtil.queryStrings(c, query);
        }
        
        public static List<String> getChIPseqExpIdsForTF(Connection c, String uniprotId)
        {
        	Query query = new Query("SELECT id FROM chip_experiemnts WHERE tf_uniprot_id=$uniprotId$");
        	query.str(uniprotId);
        	return SqlUtil.queryStrings(c, query);
        }
        
        //transform tfClass to uniprot_ID
        public static String getUniprotIdByTFClass( String tfClassId )
            {
            	Connection connection = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD/Data/experiments" ).getDataElement() );
            	String uniprotId = SqlUtil.queryString( connection, "SELECT id FROM uniprot WHERE cached_tf_class_id=" + SqlUtil.quoteString( tfClassId ) );
            	return uniprotId;
            }
    }
    
    /**************************  SqlUsage : end ******************************************/


    
    
    //-----------------

//    public static void calculate1stStepRAforAp1andFoxa1andESR1(DataElementPath pathToOutputFolder)
//    {
//        // 1. Define species.
//        //  private Species species = Species.getDefaultSpecies(null);
//        DataElementPath SPECIES_PATH = DataElementPath.create("databases/Utils/Species");
//        Species givenSpecie = null;
//        for(Species species : SPECIES_PATH.getDataCollection(Species.class) )
//            if( species.getLatinName().equals("Homo sapiens") )
//            {
//                givenSpecie = species;
//                break;
//            }
//        log.info("givenSpecie = " + givenSpecie.getLatinName());
//        
//        // 2.Construct overlapped tracks for 3 TFs (calculation of 1-st step RA for Ap1, Foxa1 and ESR1).
//        String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
//        String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
//        double fpcmThreshold = 3.0;
//        int siteNumberThreshold = 2000;
//        DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD/Data/peaks");
//        String[] foldersNames = new String[]{"gem", "macs2", "pics", "sissrs"};
//        int minimalLengthOfPeaks = 20;
//        int maximalLengthOfPeaks = 1000000;
//        new CistromConstructor(CistromConstructor.OPTION_02, givenSpecie, null, combinedPeakType, false, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder, null, 0, 100);
//    }
    
    
    /***********************************************************************/
    /******************** FreqMatrixDerivation: start **********************/
    /***********************************************************************/
    
    public static class FreqMatrixDerivation
    {
    	// 15.03.22 new
    	public static void matrixComparison(DataElementPath pathToOutputFolder)
    	{
    		// 1. Identification of frequencyMatrices.
    		DataElementPath[] pathsToMatrices = new DataElementPath[]
    		{
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_03_/ELK2____1_iter_50"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_03_/ELK2____2_iter_50"),
    			DataElementPath.create("databases/HOCOMOCO v11/Data/PCM_HUMAN_mono/ELK1_HUMAN.H11MO.0.B")
    		};
    		FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[pathsToMatrices.length];
            for( int i = 0; i < pathsToMatrices.length; i++ )
            	frequencyMatrices[i] = pathsToMatrices[i].getDataElement(FrequencyMatrix.class);
            
            // 2. ROC-curve calculation.
    		String siteModelType = SiteModelUtils.IPS_MODEL;
    		Sequence[] sequences = getLinearSequencesWithGivenLengthForBestSites();
    		Integer window = 70;
    		RocCurve.getRocCurvesAndAucs(siteModelType, true, frequencyMatrices, window, sequences, pathToOutputFolder);
    	}
    	
        private static Sequence[] getLinearSequencesWithGivenLengthForBestSites()
        {
        	DataElementPath pathToTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX_DERIVETION/RAB/RAB01_meta_clusters/ELK1/Meta_clusters_for_all_peaks");
        	DataElementPath pathToSequences = DataElementPath.create("databases/EnsemblHuman83_38/Sequences/chromosomes GRCh38");
        	int numberOfBestSites = 10000;
        	int lengthOfSequenceRegion = 150;
        	return EnsemblUtils.getLinearSequencesWithGivenLengthForBestSites(pathToTrack, numberOfBestSites, lengthOfSequenceRegion, pathToSequences);
        }
    }
    /*****************************************************************************/
    /************************* FreqMatrixDerivation: end *************************/
    /*****************************************************************************/

    // new 08.04.22
//    public static class SqlUtils
//    {
//        private static Connection connection;
//        
//    	public static Connection getConnection() throws SQLException, ClassNotFoundException
//        {
//    		if( connection == null || connection.isClosed() )
//    		{
//    			connection = Connectors.getConnection( "gtrd_v1903" );
//    		}
//    		return connection;
//        }
//
//        // 09.04.22
//        /************************************* From Mike ***********************/
//        public static String getCisBpID(String uniprotID) throws SQLException, ClassNotFoundException
//        {
//        	String result = null;
//    		String query = "select cis_bp from tf_links where gtrd_uniprot_id=?";
//    		Connection con = getConnection();
//    		try(PreparedStatement ps = con.prepareStatement(query))
//    		{
//    			ps.setString(1, uniprotID);
//    			try (ResultSet rs = ps.executeQuery();)
//    			{
//    				while( rs.next() )
//    				{
//    					result = rs.getString(1);
//    				}
//    			}
//    		}
//    		return result;
//        }
//        
//        // my modification
//        // 10.04.22
////        public static String getCisBpID(String uniprotID)
////        {
////        	String result = null;
////    		String query = "select cis_bp from tf_links where gtrd_uniprot_id=?";
////    		try(Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(query))
////    		{
////    			ps.setString(1, uniprotID);
////    			try (ResultSet rs = ps.executeQuery();)
////    			{
////    				while( rs.next() )
////    				{
////    					result = rs.getString(1);
////    				}
////    			}
////    		}
////    		catch (SQLException e)
////    		{
////    			e.printStackTrace();
////    		}
////			catch (ClassNotFoundException e1)
////    		{
////				e1.printStackTrace();
////    		}
////    		return result;
////        }
//    }

    /*****************************************************************************/
    /******************** SecondArticleOnCistrom: start **************************/
    /*****************************************************************************/
    public static class SecondArticleOnCistrom
    {
    	public static void calculateFirstTypeErrorByTransfacSites(DataElementPath pathToOutputFolder)
        {
        	int w = 40;
        	ru.biosoft.access.core.DataElementPath pathToFileWithTranspathIdAndUniprotId = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/TRANSFAC/transpath2uniprot.txt");
        	ru.biosoft.access.core.DataElementPath pathToFileWithTransfacFactors = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/TRANSFAC/factor.dat");
        	ru.biosoft.access.core.DataElementPath pathToFileWithTransfacSites = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/TRANSFAC/site.dat");
            DataElementPath pathToFolderWithMetaClustersTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human");
            String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
            String[] tfClasses = new String[]{"2.3.1.1.1", "2.3.1.1.3", "6.1.1.2.1", "1.1.1.1.1", "6.1.1.1.1", "3.3.1.1.1", "2.1.1.2.1"};

            DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Test_sample_size", "1-st_type_error"});
            for( int i = 0; i < uniprotIds.length; i++ )
            {
            	TransfacSites transfacSites = new TransfacSites(pathToFileWithTranspathIdAndUniprotId, pathToFileWithTransfacFactors, pathToFileWithTransfacSites, uniprotIds[i]);
            	double[] row = transfacSites.calculateFirstTypeErrorByTransfacSites(w, pathToFolderWithMetaClustersTracks, tfClasses[i]);
            	dmc.addRow(uniprotIds[i], row);
            	log.info("uniprotIds = " + uniprotIds[i] + " 1-st type error = " + row[1]);
            }
            DataMatrix dm = dmc.getDataMatrix();
            dm.writeDataMatrix(false, pathToOutputFolder, "first_type_errors_w_" + Integer.toString(w), log);
        }
    	
    	public static void calculateDistancesBetweenCentersOfMetaClustersAndChipExoPeaks(DataElementPath pathToOutputFolder)
    	{
    		int w = 40;
            DataElementPath pathToFolderWithMetaClustersTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human");
            DataElementPath pathToFolderWithChipExoTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/ChIP-exo_human_peaks");
            DataElementPath pathToFolderWithTablesWithChipExoTrackNames = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/Chip_exo_selected_2");
                        
            String[] tableNames = new String[]{"AR_P10275_`", "CEBPB_P17676_", "CTCF_P49711_",
            		"ELK1_P19419_", "FOXM1_Q08050_", "GABPA_Q06546_", "GRHL2_Q6ISB3_",
            		"KLF16_Q9BXK1_", "NRF1_Q16656_", "STAT3_P40763_", "USF1_P22415_",
            		"VDR_P11473_", "YY1_P25490_", "ZEB2_O60315_"};
            
            String[] tfClasses = new String[]{"2.1.1.1.4", "1.1.8.1.2", "2.3.3.50.1",
            		"3.5.2.2.1", "3.3.1.13.1", "3.5.2.1.4", "6.7.1.1.2",
            		"2.3.1.2.16", "0.0.6.0.1", "6.2.1.0.3", "1.2.6.2.1",
            		"2.1.2.4.1", "2.3.3.9.1", "3.1.8.3.2"};
            
            DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Distance_sample_size", "Mean_distance", "Sigma_of_distance"});
            for( int i = 0; i < tfClasses.length; i++ )
            {
            	log.info("tableNames = " + tableNames[i]);
            	ru.biosoft.access.core.DataElementPath path = pathToFolderWithTablesWithChipExoTrackNames.getChildPath(tableNames[i]);
            	DataMatrixString dms = new DataMatrixString(path, new String[]{"names"});
            	String metaTrackName = dms.getRowNames()[0];
            	Object[] objects = calculateDistancesBetweenCentersOfMetaClustersAndChipExoPeaks(w, pathToFolderWithMetaClustersTracks, tfClasses[i], pathToFolderWithChipExoTracks, metaTrackName);
            	Chart chart = (Chart)objects[0];
            	TableAndFileUtils.addChartToTable("Densities of distances", chart, pathToOutputFolder.getChildPath("_chart_distances_" + tableNames[i]));
            	double[] meanAndSigma = (double[]) objects[2];
            	dmc.addRow(tableNames[i], new double[]{(double)objects[1], meanAndSigma[0], meanAndSigma[1]});
            }
            DataMatrix dm = dmc.getDataMatrix();
            dm.writeDataMatrix(false, pathToOutputFolder, "mean_and_sigma", log);
    	}
    	
    	public static void calculateDistancesBetweenCentersOfMetaClustersAndChipExoPeaks2(DataElementPath pathToOutputFolder)
    	{
    		int w = 40;
            DataElementPath pathToFolderWithMetaClustersTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human");
            DataElementPath pathToFolderWithChipExoTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/ChIP-exo_human_peaks");
            //ru.biosoft.access.core.DataElementPath pathToFolderWithTablesWithChipExoTrackNames = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/Chip_exo_selected_2");
            //data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/ChIP-exo_human_peaks

//            String[] tableNames = new String[]{"AR_P10275_", "CEBPB_P17676_", "CTCF_P49711_",
//            		"ELK1_P19419_", "FOXM1_Q08050_", "GABPA_Q06546_", "GRHL2_Q6ISB3_",
//            		"KLF16_Q9BXK1_", "NRF1_Q16656_", "STAT3_P40763_", "USF1_P22415_",
//            		"VDR_P11473_", "YY1_P25490_", "ZEB2_O60315_"};
            
            String[] tableNames = new String[]{"AR_P10275_", "CEBPB_P17676_", "CTCF_P49711_",
            		"ELK1_P19419_", "FOXM1_Q08050_", "GABPA_Q06546_", "GRHL2_Q6ISB3_",
            		"KLF16_Q9BXK1_", "NRF1_Q16656_", "STAT3_P40763_", "USF1_P22415_",
            		"VDR_P11473_", "YY1_P25490_", "ZEB2_O60315_"};
            
            String[] tfClasses = new String[]{"2.1.1.1.4", "1.1.8.1.2", "2.3.3.50.1",
            		"3.5.2.2.1", "3.3.1.13.1", "3.5.2.1.4", "6.7.1.1.2",
            		"2.3.1.2.16", "0.0.6.0.1", "6.2.1.0.3", "1.2.6.2.1",
            		"2.1.2.4.1", "2.3.3.9.1", "3.1.8.3.2"};
            
            DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Distance_sample_size", "Mean_distance", "Sigma_of_distance"});
            for( int i = 0; i < tfClasses.length; i++ )
            {
            	
            	log.info("tableNames = " + tableNames[i]);
            
            	/*********************************** !!!!!!!!!!!!!!!!!!! **********************/
            	//ru.biosoft.access.core.DataElementPath path = pathToFolderWithTablesWithChipExoTrackNames.getChildPath(tableNames[i]);
            	ru.biosoft.access.core.DataElementPath path = null;
            	
            	DataMatrixString dms = new DataMatrixString(path, new String[]{"names"});
            	String metaTrackName = dms.getRowNames()[0];
            	Object[] objects = calculateDistancesBetweenCentersOfMetaClustersAndChipExoPeaks(w, pathToFolderWithMetaClustersTracks, tfClasses[i], pathToFolderWithChipExoTracks, metaTrackName);
            	Chart chart = (Chart)objects[0];
            	TableAndFileUtils.addChartToTable("Densities of distances", chart, pathToOutputFolder.getChildPath("_chart_distances_" + tableNames[i]));
            	double[] meanAndSigma = (double[]) objects[2];
            	dmc.addRow(tableNames[i], new double[]{(double)objects[1], meanAndSigma[0], meanAndSigma[1]});
            }
            DataMatrix dm = dmc.getDataMatrix();
            dm.writeDataMatrix(false, pathToOutputFolder, "mean_and_sigma", log);
    	}
    	
    	// Only overlapped meta-clusters and chip-exo peaks are used
    	public static Object[] calculateDistancesBetweenCentersOfMetaClustersAndChipExoPeaks(int w, DataElementPath pathToFolderWithMetaClusterTracks, String metaClustersTrackName, DataElementPath pathToFolderWithChipExoTracks, String chipExoTrackName)
    	{
    		int ww = w / 2;
    		ru.biosoft.access.core.DataElementPath pathToTrack = pathToFolderWithChipExoTracks.getChildPath(chipExoTrackName);
    		Track track = pathToTrack.getDataElement(Track.class);
            Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 1, Integer.MAX_VALUE, new String[]{"Fold"}, track.getName());
            FunSite[] chipExoSites = FunSiteUtils.transformToArray(sites);
            track = pathToFolderWithMetaClusterTracks.getChildPath(metaClustersTrackName).getDataElement(Track.class);
            
            List<Integer> list = new ArrayList<>();
            for( int i = 0; i < chipExoSites.length; i++ )
            {
            	int center = (chipExoSites[i].getStartPosition() + chipExoSites[i].getFinishPosition()) / 2, start = center - ww, end = center + ww;
            	DataCollection<Site> dc = track.getSites(chipExoSites[i].getChromosomeName(), start, end);
            	if( dc.getSize() > 0 )
            		for( Site site : dc )
            			list.add(Math.abs(site.getInterval().getCenter() - center));
            }
            double[] distances = UtilsForArray.transformIntToDouble(UtilsGeneral.fromListIntegerToArray(list));
            double[] meanAndSigma = UnivariateSample.getMeanAndSigma(distances);
        	log.info("n = " + distances.length);
        	
            //
            String[] sampleNames = null;
            double[][] samples = new double[][]{distances};
            UnivariateSamples us = new UnivariateSamples(sampleNames, samples);
            Chart chart = us.createChartWithSmoothedDensities("Distances", false, DensityEstimation.WINDOW_WIDTH_01, null);
    		return new Object[]{chart, (double)distances.length, meanAndSigma};
    	}
    	
    	public static void calculateDistancesBetweenBoundariesOfMetaClustersAndChipExoPeaks(DataElementPath pathToOutputFolder)
    	{
    		int w = 40;
            DataElementPath pathToFolderWithMetaClustersTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human");
            DataElementPath pathToFolderWithChipExoTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/ChIP-exo_human_peaks");
            DataElementPath pathToFolderWithTablesWithChipExoTrackNames = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/Chip_exo_selected_2");
                        
            String[] tableNames = new String[]{"AR_P10275_", "CEBPB_P17676_", "CTCF_P49711_",
            		"ELK1_P19419_", "FOXM1_Q08050_", "GABPA_Q06546_", "GRHL2_Q6ISB3_",
            		"KLF16_Q9BXK1_", "NRF1_Q16656_", "STAT3_P40763_", "USF1_P22415_",
            		"VDR_P11473_", "YY1_P25490_", "ZEB2_O60315_"};
            
            String[] tfClasses = new String[]{"2.1.1.1.4", "1.1.8.1.2", "2.3.3.50.1",
            		"3.5.2.2.1", "3.3.1.13.1", "3.5.2.1.4", "6.7.1.1.2",
            		"2.3.1.2.16", "0.0.6.0.1", "6.2.1.0.3", "1.2.6.2.1",
            		"2.1.2.4.1", "2.3.3.9.1", "3.1.8.3.2"};
            
            for( int i = 0; i < tfClasses.length; i++ )
            {
            	log.info("tableNames = " + tableNames[i]);
            	ru.biosoft.access.core.DataElementPath path = pathToFolderWithTablesWithChipExoTrackNames.getChildPath(tableNames[i]);
            	DataMatrixString dms = new DataMatrixString(path, new String[]{"names"});
            	String metaTrackName = dms.getRowNames()[0];
            	Object[] objects = calculateDistancesBetweenBoundariesOfMetaClustersAndChipExoPeaks(w, pathToFolderWithMetaClustersTracks, tfClasses[i], pathToFolderWithChipExoTracks, metaTrackName);
            	Chart chart = (Chart)objects[0];
            	TableAndFileUtils.addChartToTable("Densities of distances", chart, pathToOutputFolder.getChildPath("_chart_distances_" + tableNames[i]));
            	DataMatrix dm = (DataMatrix)objects[1];
            	dm.writeDataMatrix(false, pathToOutputFolder, "simple_characteristics_" + tableNames[i], log);
            }
    	}
    	
    	// Only overlapped meta-clusters and chip-exo peaks are used
    	public static Object[] calculateDistancesBetweenBoundariesOfMetaClustersAndChipExoPeaks(int w, DataElementPath pathToFolderWithMetaClusterTracks, String metaClustersTrackName, DataElementPath pathToFolderWithChipExoTracks, String chipExoTrackName)
    	{
    		int ww = w / 2;
    		ru.biosoft.access.core.DataElementPath pathToTrack = pathToFolderWithChipExoTracks.getChildPath(chipExoTrackName);
    		Track track = pathToTrack.getDataElement(Track.class);
            Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 1, Integer.MAX_VALUE, new String[]{"Fold"}, track.getName());
            FunSite[] chipExoSites = FunSiteUtils.transformToArray(sites);
            track = pathToFolderWithMetaClusterTracks.getChildPath(metaClustersTrackName).getDataElement(Track.class);
            
            // 1. Calculate samples 'distancesLeft' & 'distancesRight'. 
            List<Integer> listForLeft = new ArrayList<>(), listForRight = new ArrayList<>();
            for( int i = 0; i < chipExoSites.length; i++ )
            {
            	int center = (chipExoSites[i].getStartPosition() + chipExoSites[i].getFinishPosition()) / 2, start = center - ww, end = center + ww;
            	DataCollection<Site> dc = track.getSites(chipExoSites[i].getChromosomeName(), start, end);
            	if( dc.getSize() > 0 )
            		for( Site site : dc )
            		{
            			listForLeft.add(Math.abs(site.getFrom() - chipExoSites[i].getStartPosition()));
            			listForRight.add(Math.abs(site.getTo() - chipExoSites[i].getFinishPosition()));
            		}
            }
            double[] distancesLeft = UtilsForArray.transformIntToDouble(UtilsGeneral.fromListIntegerToArray(listForLeft));
            double[] distancesRight = UtilsForArray.transformIntToDouble(UtilsGeneral.fromListIntegerToArray(listForRight));
            
            // 2. Calculate chart and simple characteristics.
            String[] sampleNames = new String[]{"Between left boundaries", "Between right boundaries"};
            double[][] samples = new double[][]{distancesLeft, distancesRight};
            UnivariateSamples us = new UnivariateSamples(sampleNames, samples);
            Chart chart = us.createChartWithSmoothedDensities("Distances", false, DensityEstimation.WINDOW_WIDTH_01, null);
        	log.info("n = " + distancesLeft.length);
            DataMatrix dmCharacteristics = us.getSimpleCharacteristicsOfSamples();
     		return new Object[]{chart, dmCharacteristics};
    	}

    	public static void calculateFirstTypeErrorByChipExoTracks(DataElementPath pathToOutputFolder)
    	{
    		int w = 40;
            DataElementPath pathToFolderWithMetaClustersTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human");
            DataElementPath pathToFolderWithChipExoTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/ChIP-exo_human_peaks");
            DataElementPath pathToFolderWithTablesWithChipExoTrackNames = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/Chip_exo_selected_2");
                        
            String[] tableNames = new String[]{"AR_P10275_", "CEBPB_P17676_", "CTCF_P49711_",
            		"ELK1_P19419_", "FOXM1_Q08050_", "GABPA_Q06546_", "GRHL2_Q6ISB3_",
            		"KLF16_Q9BXK1_", "NRF1_Q16656_", "STAT3_P40763_", "USF1_P22415_",
            		"VDR_P11473_", "YY1_P25490_", "ZEB2_O60315_"};
            
            String[] tfClasses = new String[]{"2.1.1.1.4", "1.1.8.1.2", "2.3.3.50.1",
            		"3.5.2.2.1", "3.3.1.13.1", "3.5.2.1.4", "6.7.1.1.2",
            		"2.3.1.2.16", "0.0.6.0.1", "6.2.1.0.3", "1.2.6.2.1",
            		"2.1.2.4.1", "2.3.3.9.1", "3.1.8.3.2"};
            
            DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Test_sample_size", "1-st_type_error", "size_squared", "size_sqrt", "size_lg"});
            for( int i = 0; i < tfClasses.length; i++ )
            {
            	ru.biosoft.access.core.DataElementPath path = pathToFolderWithTablesWithChipExoTrackNames.getChildPath(tableNames[i]);
            	DataMatrixString dms = new DataMatrixString(path, new String[]{"names"});
            	String metaTrackName = dms.getRowNames()[0];
            	double[] row = calculateFirstTypeErrorByChipExoTracks(w, pathToFolderWithMetaClustersTracks, tfClasses[i], pathToFolderWithChipExoTracks, metaTrackName);
            	row = ArrayUtils.addAll(row, new double[]{row[0] * row[0], Math.sqrt(row[0]), Math.log10(row[0])});
            	dmc.addRow(metaTrackName, row);
            	log.info("tableNames = " + tableNames[i] + " 1-st type error = " + row[1]);
            }
            DataMatrix dm = dmc.getDataMatrix();
            dm.writeDataMatrix(true, pathToOutputFolder, "first_type_errors_w_" + Integer.toString(w), log);
    	}
    	
//    	private double[] calculateFirstTypeErrorByTransfacSites(int w, DataElementPath pathToFolderWithMetaClusterTracks, String trackName)
//        {
//        	int ww = w / 2, coveredNumber = 0;
//            Track track = pathToFolderWithMetaClusterTracks.getChildPath(trackName).getDataElement(Track.class);
//            for( int i = 0; i < transfacSites.length; i++ )
//            {
//            	int center = (transfacSites[i].getFinishPosition() + transfacSites[i].getFinishPosition()) / 2, start = center - ww, end = center + ww;  
//            	DataCollection<Site> dc = track.getSites(transfacSites[i].getChromosomeName(), start, end);
//                if( dc.getSize() > 0 )
//                	coveredNumber++;
//            }
//        	return new double[]{(double)transfacSites.length, (double)(transfacSites.length - coveredNumber) / (double)transfacSites.length};
//        }
//    	
    	public static double[] calculateFirstTypeErrorByChipExoTracks(int w, DataElementPath pathToFolderWithMetaClusterTracks, String metaClustersTrackName, DataElementPath pathToFolderWithChipExoTracks, String chipExoTrackName)
    	{
    		int ww = w / 2, coveredNumber = 0;
    		ru.biosoft.access.core.DataElementPath pathToTrack = pathToFolderWithChipExoTracks.getChildPath(chipExoTrackName);
    		Track track = pathToTrack.getDataElement(Track.class);
            Map<String, List<FunSite>> sites = FunSiteUtils.readSitesInTrack(track, 1, Integer.MAX_VALUE, new String[]{"Fold"}, track.getName());
            FunSite[] chipExoSites = FunSiteUtils.transformToArray(sites);
            
            track = pathToFolderWithMetaClusterTracks.getChildPath(metaClustersTrackName).getDataElement(Track.class);
            for( int i = 0; i < chipExoSites.length; i++ )
            {
            	int center = (chipExoSites[i].getStartPosition() + chipExoSites[i].getFinishPosition()) / 2, start = center - ww, end = center + ww;
            	DataCollection<Site> dc = track.getSites(chipExoSites[i].getChromosomeName(), start, end);
            	if( dc.getSize() > 0 )
            		coveredNumber++;
            }
    		return new double[]{(double)chipExoSites.length, (double)(chipExoSites.length - coveredNumber) / (double)chipExoSites.length};
    	}
    	
//    	 public double[] calculateFirstTypeErrorByTransfacSites(int w, DataElementPath pathToFolderWithMetaClusterTracks, String trackName)
//         {
//         	int ww = w / 2, coveredNumber = 0;
//         	Track track = pathToFolderWithMetaClusterTracks.getChildPath(trackName).getDataElement(Track.class);
//             for( int i = 0; i < transfacSites.length; i++ )
//             {
//             	int center = (transfacSites[i].getFinishPosition() + transfacSites[i].getFinishPosition()) / 2, start = center - ww, end = center + ww;  
//             	DataCollection<Site> dc = track.getSites(transfacSites[i].getChromosomeName(), start, end);
//                 if( dc.getSize() > 0 )
//                 	coveredNumber++;
//             }
//         	return new double[]{(double)transfacSites.length, (double)(transfacSites.length - coveredNumber) / (double)transfacSites.length};
//         }
    	
    	public static void calculateDistanceBetweenTransfacSitesAndMetaClusters(DataElementPath pathToOutputFolder)
        {
        	int w = 3000;
        	ru.biosoft.access.core.DataElementPath pathToFileWithTranspathIdAndUniprotId = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/TRANSFAC/transpath2uniprot.txt");
        	ru.biosoft.access.core.DataElementPath pathToFileWithTransfacFactors = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/TRANSFAC/factor.dat");
        	ru.biosoft.access.core.DataElementPath pathToFileWithTransfacSites = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/TRANSFAC/site.dat");
            DataElementPath pathToFolderWithMetaClustersTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human");
            String[] uniprotIds = new String[]{"P08047", "Q02447", "Q04206", "P05412", "P19838", "P55317", "P03372"};
            String[] tfClasses = new String[]{"2.3.1.1.1", "2.3.1.1.3", "6.1.1.2.1", "1.1.1.1.1", "6.1.1.1.1", "3.3.1.1.1", "2.1.1.2.1"};
            DataMatrixConstructor dmc = new DataMatrixConstructor(new String[]{"Mean_distance", "sigma", "Minimum", "Maximum"});
            for( int i = 0; i < uniprotIds.length; i++ )
            {
            	TransfacSites transfacSites = new TransfacSites(pathToFileWithTranspathIdAndUniprotId, pathToFileWithTransfacFactors, pathToFileWithTransfacSites, uniprotIds[i]);
                Object[] objects = transfacSites.calculateDistanceBetweenTransfacSitesAndMetaClusters(w, pathToFolderWithMetaClustersTracks, tfClasses[i]);
                double[] meanAndSigma = (double[]) objects[0], minAndMax = (double[]) objects[1];
                Chart chart = (Chart) objects[2];
                dmc.addRow(uniprotIds[i], new double[]{meanAndSigma[0], meanAndSigma[1], minAndMax[0], minAndMax[1]});
            	log.info("uniprotIds = " + uniprotIds[i]);
            	ru.biosoft.access.core.DataElementPath pathToTable = pathToOutputFolder.getChildPath("_density_charts");
                TableAndFileUtils.addChartToTable(uniprotIds[i], chart, pathToTable);
            }
            DataMatrix dm = dmc.getDataMatrix();
            dm.writeDataMatrix(false, pathToOutputFolder, "simple_characteristics_w_" + Integer.toString(w), log);
        }
    	
    	// TODO: under construction
    	//data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/ChIP-exo_human_peaks
    	public static void metaAnalysisOfChipExoData(DataElementPath pathToOutputFolder)
    	{
            DataElementPath pathToFolderWithChipExoTracks = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C10_cistrom2_article/ChIP-exo_human_peaks");
            DataElementPath pathToFolderWithChipExoNamesTables = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/DATA01/Chip_exo_selected");
            String[] namesOfTablesWithNamesOfChipExoData =  new String[]{"AR_P10275", "CEBPB_P17676", "CTCF_P49711", "ELK1_P19419", "FOXM1_Q08050", "GABPA_Q06546", "GRHL2_Q6ISB3", "HMGB1_P09429", "KLF16_Q9BXK1", "NRF1_Q16656", "STAT3_P40763", "USF1_P22415", "VDR_P11473", "YY1_P25490", "ZEB2_O60315"};
            for( int i = 0; i < namesOfTablesWithNamesOfChipExoData.length; i++ )
            {
            	ru.biosoft.access.core.DataElementPath pathToChipExoNamesTable = pathToFolderWithChipExoNamesTables.getChildPath(namesOfTablesWithNamesOfChipExoData[i]);
            	DataMatrixString dms = new DataMatrixString(pathToChipExoNamesTable, new String[]{"names"});
            	String[] trackNames = dms.getColumn("names");
            	
            	// DataElementPath pathToChipExoTrack = pathToFolderWithChipExoTracks.getChildPath(namesOfTablesWithNamesOfChipExoData);
            }
    	}
    	
        public static void identifyNotOverlappedSitesInOldAndNewMetaClusters(DataElementPath pathToOutputFolder)
        {
//            DataElementPath pathToOldMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human/6.1.1.1.1");
//            DataElementPath pathToNewMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/Ans_05_meta_clusters_with_site_motifs/NFKB1/6.1.1.1.1");
        	
            DataElementPath pathToOldMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_01/C11_metara_clusters_gtrd2006/human/1.1.1.1.1");
            DataElementPath pathToNewMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_02/Ans_05_meta_clusters_with_site_motifs/Jun_only_part_mtifs/1.1.1.1.1");


            identifyNotOverlappedSites(pathToOldMetaClusters, pathToNewMetaClusters, pathToOutputFolder, "no_in_old", "no_in_old_table");
            identifyNotOverlappedSites(pathToNewMetaClusters, pathToOldMetaClusters, pathToOutputFolder, "no_in_new", "no_in_new_table");
        }

        private static void identifyNotOverlappedSites(DataElementPath pathToFirstTrack, DataElementPath pathToSecondTrack, DataElementPath pathToOutputFolder, String newTrackName, String fileName)
        {
        	FunSite[] funSites = FunSiteUtils.getFunSites(pathToFirstTrack);
        	log.info("funSites.length = " + funSites.length);

        	Track track = pathToSecondTrack.getDataElement(Track.class);
        	
        	log.info("track.length = " + track.getAllSites().getSize());
        	
        	boolean[] indicators = areCoveredByTrack(funSites, track);
        	List<FunSite> list = new ArrayList<>();
            for( int i = 0; i < funSites.length; i++ )
            	if( indicators[i] == false )
            		list.add(funSites[i]);
            FunSite[] funSitesNew = list.toArray(new FunSite[0]);
            
        	log.info("funSitesNew.length = " + funSitesNew.length);
            FunSiteUtils.writeSitesToSqlTrack(funSitesNew, null, null, pathToOutputFolder, newTrackName);
            FunSiteUtils.writeSitesToBedFile(funSitesNew, pathToOutputFolder, fileName);
        }
    }

    /*****************************************************************************/
    /******************** SecondArticleOnCistrom: end ****************************/
    /*****************************************************************************/
    
    /*****************************************************************************/
    /******************** ThirdArticleOnCistrom: start ***************************/
    /*****************************************************************************/


    public static class ThirdArticleOnCistrom
    {

    	/***********************************************************/
    	/****************** HistoneUtils: start ********************/
    	/***********************************************************/

        public static class HistoneUtils
        {
        	public static void getHistonesTest()
        	{
        		// !!!!!!!!!!!!!!!!!!!!!!!!! Attention Temporery replacement           !!!!!!!!!!!!!!!!!!!!!!!!!
        		// !!!!!!!!!!!!!!!!!!!!!!!!! In class HistonesExperimentSQLTransformer !!!!!!!!!!!!!!!!!!!!!!!!!
        		// public static final String DEFAULT_GTRD_ROOT = "databases/GTRD/Data";  ->  public static final String DEFAULT_GTRD_ROOT = "databases/GTRD_20.06/Data";
        		
        		DataElementPath pathToFolderWithHistoneExperiments = DataElementPath.create("databases/GTRD_20.06/Data/ChIP-seq HM experiments");
        		DataElementPath pathToFolderWithHistonePeaks = DataElementPath.create("databases/GTRD_20.06/Data/peaks/Histone Modifications/macs2");

        		//temp
        		int index = 0;
        		
        		DataCollection<HistonesExperiment> histDataCollection = pathToFolderWithHistoneExperiments.getDataCollection(HistonesExperiment.class);
        		for( HistonesExperiment exp : histDataCollection )
        		{
        			// 1.
        			if( exp.isControlExperiment() ) continue;
    				String peaksId = exp.getMacsPeaks().getName(); // peaksId is also name of track with peacks of histone modifications.
    				DataElementPath pathToTrackWithPeaks = pathToFolderWithHistonePeaks.getChildPath(peaksId);
    				if( ! pathToTrackWithPeaks.exists() ) continue;
    				
        			Species species = exp.getSpecie();
    				String speciesName = species.getLatinName();
    				CellLine cell = exp.getCell();
    				String cellName = cell.toString(), histoneType = exp.getTarget();
    				
    				log.info("speciesName = " + speciesName + " cellName = " + cellName + " histoneType = " + histoneType + " peaksId = " + peaksId);
    	    		if( ++index >= 100 ) break;
        		}
        	}

        	//*** Method 1 ***//
        	public static void identifyHistoneModificationsInGivenGenomeFragment(DataElementPath pathToOutputFolder)
        	{
        		DataElementPath pathToFolderWithHistoneExperiments = DataElementPath.create("databases/GTRD_20.06/Data/ChIP-seq HM experiments");
        		DataElementPath pathToFolderWithHistonePeaks = DataElementPath.create("databases/GTRD_20.06/Data/peaks/Histone Modifications/macs2");
        		String speciesNameGiven = "Homo sapiens";
        		String chromosomeName = "22";
        		int from = 17120484, to = 17120514;

        		// speciesName = Homo sapiens cellName = 1132 histoneType = H3K9me3 peaksId = HPEAKS000042 isTrackExist = true

    			// 1. Calculate and data matrix: peaksId <-> histoneType
        		int index = 0;
        		DataCollection<HistonesExperiment> histDataCollection = pathToFolderWithHistoneExperiments.getDataCollection(HistonesExperiment.class);
        		DataMatrixStringConstructor dmsc = new DataMatrixStringConstructor(new String[]{"histoneType"});
        		for( HistonesExperiment exp : histDataCollection )
        		{
        			if( exp.isControlExperiment() ) continue;
    				String peaksId = exp.getMacsPeaks().getName(); // peaksId is also name of track with peacks of histone modifications.
    				DataElementPath pathToTrackWithPeaks = pathToFolderWithHistonePeaks.getChildPath(peaksId);
    				if( ! pathToTrackWithPeaks.exists() ) continue;
    				String speciesName = exp.getSpecie().getLatinName();
    				if( ! speciesName.equals(speciesNameGiven) ) continue;
    				String histoneType = exp.getTarget();
    				if( isOverlapped(chromosomeName, from, to, pathToTrackWithPeaks) )
    				{
    					dmsc.addRow(peaksId, new String[]{histoneType});
    					log.info("index = " + index++ + " histoneType = " + histoneType + " peaksId = " + peaksId);
    				}
        		}
        		DataMatrixString dm = dmsc.getDataMatrixString();
        		dm.writeDataMatrixString(false, pathToOutputFolder, "histone_types", log);
        		
        		// 2. Create and write table: distinct histoneType <-> some peaksId
        		String[] histoneTypes = dm.getColumn("histoneType"), peaksIds = dm.getRowNames(); 
        		Map<String, String> map = new HashMap<>();
            	for( int i = 0; i < histoneTypes.length; i++ )
            		map.put(histoneTypes[i], peaksIds[i]);
        		String[] histoneTypesDistinct = new String[map.size()], peaksIdsDistinct = new String[map.size()];
        		index = 0;
            	for( Entry<String, String> entry : map.entrySet() )
            	{
            		histoneTypesDistinct[index] = entry.getKey();
            		peaksIdsDistinct[index++] = entry.getValue();
            	}
            	dm = new DataMatrixString(histoneTypesDistinct, "peaksIds", peaksIdsDistinct);
        		dm.writeDataMatrixString(false, pathToOutputFolder, "histone_types_distinct", log);
        	}
        	
        	private static boolean isOverlapped(String chromosomeName, int from, int to, DataElementPath pathToTrack)
        	{
        		Track trackWithMetaClusters = (Track)pathToTrack.getDataElement();
        		DataCollection<Site> sites = trackWithMetaClusters.getSites(chromosomeName, from, to);
    			return sites.getSize() > 0 ? true : false;
        	}
        	
        	//*** Method 3 ***//
        	public static void analysisOfHistonePairsWithinMetaClusters(DataElementPath pathToOutputFolder)
        	{
        		DataElementPath pathToTrackWithMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans05_histone_patterns/pattern_01_/REST_no_treatment/histones_in_meta_clusters");
        		DataElementPath pathToTableWithHistoneNames = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans05_histone_patterns/pattern_01_/histone_types_distinct");
        		int coverageHistoneFrequencyThreshold = 10;
        		
        		// 1. Read distinctHistoneNames in table.
        		DataMatrixString dms = new DataMatrixString(pathToTableWithHistoneNames, new String[]{"peaksIds"});
        		String[] distinctHistoneNames = dms.getRowNames(), trackNamesWithDistinctHistones = dms.getColumn("peaksIds");
        		
        		// 2. Calclulate indicatorMatrix.
        		List<boolean[]> list = new ArrayList<>();
    			Track track = (Track)pathToTrackWithMetaClusters.getDataElement();
                for( Site site : track.getAllSites() )
                {
                	// 2.1.
        			DynamicPropertySet dps = site.getProperties();
        			String string = dps.getValueAsString("Coverage histone frequency");
        			int coverageHistoneFrequency = Integer.parseInt(string);
        			if( coverageHistoneFrequency < coverageHistoneFrequencyThreshold ) continue;
    				log.info("coverageHistoneFrequency = " + coverageHistoneFrequency);
        			
    				// 2.2.
        			String histoneTracksNames = dps.getValueAsString("Histone track names");
        			boolean[] histoneIndicators = UtilsForArray.getConstantArray(trackNamesWithDistinctHistones.length, false);
        			for( int i = 0; i < trackNamesWithDistinctHistones.length; i++ )
        				if( histoneTracksNames.contains(trackNamesWithDistinctHistones[i]) )
        					histoneIndicators[i] = true;
        			list.add(histoneIndicators);
                }
                boolean[][] indicatorMatrix = UtilsGeneral.fromListBooleanToMatrix(list);
				log.info("***** O.K.: indicatorMatrix is constructed *****");
                
                // 3.Analisys of histon pairs.
                String[] columnNames = new String[]{"Chi-squared statistic", "p-value", "Corr-coeffitient",
                		"Probability_of_1-st_histone", "Probability_of_2-nd_histone", "Probability_of_histone_pair",
                		"Theoretical_probability_of_histone_pair", "Ratio_of_probabilities_for_pair"};
                DataMatrixConstructor dmc = new DataMatrixConstructor(columnNames);
                boolean[][] indicatorMatrixTransposed = MatrixUtils.getTransposedMatrix(indicatorMatrix);
    			for( int i = 0; i < distinctHistoneNames.length - 1; i++ )
        			for( int ii = i + 1; ii < distinctHistoneNames.length; ii++ )
        			{
        				Object[] objects = ChiSquaredIndependenceTestForTwoDimensionContingencyTable.performTest(indicatorMatrixTransposed[i], indicatorMatrixTransposed[ii]);
        				double statistic = (double)objects[0], pValue = (double)objects[1];
        	    		double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(indicatorMatrixTransposed[i], indicatorMatrixTransposed[ii]);
        	    		double[] observedprobabilities = getObservedProbabilities(indicatorMatrixTransposed[i], indicatorMatrixTransposed[ii]);
        	    		double probabilityObservedInSampleOne = observedprobabilities[0],
        	    				probabilityObservedInSampleTwo = observedprobabilities[1], 
        	    				probabilityObservedInBothSamples = observedprobabilities[2],
        	    				ratioOfProbabilities = observedprobabilities[3];
        	    		double theoreticalProbabilityOfPair = probabilityObservedInSampleOne * probabilityObservedInSampleTwo;
        	    		double[] rowValues = new double[]{statistic, pValue, corr,
        	    				probabilityObservedInSampleOne, probabilityObservedInSampleTwo, probabilityObservedInBothSamples,
        	    				theoreticalProbabilityOfPair, ratioOfProbabilities};
        	    		dmc.addRow(distinctHistoneNames[i] + "_" + distinctHistoneNames[ii], rowValues);
        			}
    			DataMatrix dm = dmc.getDataMatrix();
    			dm.writeDataMatrix(false, pathToOutputFolder, "pairs_of_histones", log);
        	}

        	private static double[] getObservedProbabilities(boolean[] sampleOne, boolean[] sampleTwo)
        	{
        		int numberOfObservedInSampleOne = 0, numberOfObservedInSampleTwo = 0, numberOfObservedTogether = 0;
    			for( int i = 0; i < sampleOne.length; i++ )
    			{
    				if( sampleOne[i] )
    					numberOfObservedInSampleOne++;
    				if( sampleTwo[i] )
    					numberOfObservedInSampleTwo++;
    				if( sampleOne[i] && sampleTwo[i] )
    					numberOfObservedTogether++;
    			}
    			double probabilityOservedInSampleOne = (double)numberOfObservedInSampleOne / (double)sampleOne.length;
    			double probabilityOservedInSampleTwo = (double)numberOfObservedInSampleTwo / (double)sampleOne.length;
    			double probabilityOservedInBothSamples = (double)numberOfObservedTogether / (double)sampleOne.length;
    			double probabilityExpectedInBothSamples = probabilityOservedInSampleOne * probabilityOservedInSampleTwo;
    			double ratioOfProbabilities = probabilityOservedInBothSamples / probabilityExpectedInBothSamples;
        		return new double[]{probabilityOservedInSampleOne, probabilityOservedInSampleTwo, probabilityOservedInBothSamples, ratioOfProbabilities};
        	}
        	
        	//*** Method 2 ***//
        	private static Object[] getFrequenciesAndGistoneNamesFromTrack(DataElementPath pathToTrack)
        	{
    			Track track = (Track)pathToTrack.getDataElement();
    			DataCollection<Site> sites = track.getAllSites();
    			int n = sites.getSize(), index = 0;
    			int[] coverageHistoneFrequencies = new int[n];
    			String[] histoneNames = new String[n];
                for( Site site : sites )
                {
        			DynamicPropertySet dps = site.getProperties();
        			String stringForFreq = dps.getValueAsString("Coverage histone frequency");
        			String names = dps.getValueAsString("Histone track names");
        			coverageHistoneFrequencies[index] = Integer.parseInt(stringForFreq);
        			histoneNames[index++] = names;
                }
    			return new Object[]{coverageHistoneFrequencies, histoneNames};
        	}

        	public static void analysisOfHistonePatternsWithinMetaClusters(DataElementPath pathToOutputFolder)
        	{
        		DataElementPath pathToTrackWithMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans05_histone_patterns/pattern_01_/REST_no_treatment/histones_in_meta_clusters");
        		int freqForZero = 55033;
        		
        		// 1.
        		Object[] objects = getFrequenciesAndGistoneNamesFromTrack(pathToTrackWithMetaClusters);
    			int[] coverageHistoneFrequencies = (int[])objects[0];
    			String[] histoneNames = (String[])objects[1];
    			int max = (int)PrimitiveOperations.getMax(coverageHistoneFrequencies)[1];
    			
    			// 2. Calculate probability density for coverageHistoneFrequency.
    			double[] xValues = new double[max + 1], yValues = UtilsForArray.getConstantArray(max + 1, 0.0);
    			for( int i = 0; i <= max; i++ )
    				xValues[i] = (double)i;
    			yValues[0] = (double)freqForZero;
            	for( int i = 0; i < coverageHistoneFrequencies.length; i++ )
            		yValues[coverageHistoneFrequencies[i]] += 1.0;
            	double sum = PrimitiveOperations.getSum(yValues);
            	yValues = VectorOperations.getProductOfVectorAndScalar(yValues, 1.0 / sum);
            	Chart chart = ChartUtils.createChart(xValues, yValues, "density of histone occurrence", null, null, null, null, "Number of histone modifications", "Probability", true);
            	TableAndFileUtils.addChartToTable("chart with density", chart, pathToOutputFolder.getChildPath("_chart_with_densities_of_histone_frequencies"));
        	}

        	/*-------------------------*/
        	
        	//*** Method 2 ***//
        	public static void getHistonePatternsWithinMetaClusters(DataElementPath pathToOutputFolder)
        	{
        		DataElementPath pathToTrackWithMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Metara_27");
        		DataElementPath pathToFolderWithHistoneTracks = DataElementPath.create("databases/GTRD_20.06/Data/peaks/Histone Modifications/macs2");
        		DataElementPath pathToTableWithHistoneTypes = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans05_histone_patterns/pattern_01_/histone_types_distinct");
        		int frequencyThreshold = 1, thresholdOfNumberOfOverlappedTracks = 1; //int frequencyThreshold = 3, thresholdOfNumberOfOverlappedTracks = 3;

        		// 1. Initialization of some variables.
        		DataMatrixString dms = new DataMatrixString(pathToTableWithHistoneTypes, new String[]{"peaksIds"});
        		String[] histoneTrackNames = dms.getColumn(0);
        		Track[] tracksWithHistones = new Track[histoneTrackNames.length];
            	for( int i = 0; i < histoneTrackNames.length; i++ )
            		tracksWithHistones[i] = (Track)pathToFolderWithHistoneTracks.getChildPath(histoneTrackNames[i]).getDataElement();

            	// 2.
            	int index = 0;
            	DataElementPath pathToResultedTrack = pathToOutputFolder.getChildPath("histones_in_meta_clusters");
        		SqlTrack track = SqlTrack.createTrack(pathToResultedTrack, null);
    			Track trackWithMetaClusters = (Track)pathToTrackWithMetaClusters.getDataElement();
                for( Site site : trackWithMetaClusters.getAllSites() )
            	{
                	// 2.1. Get information about meta-clusters.
        			DynamicPropertySet dps = site.getProperties();
        			String frequencyOfPeakTracks = dps.getValueAsString("Frequency"), raScore = dps.getValueAsString(RankAggregation.RA_SCORE);
//        			int frequency = Integer.parseInt(dps.getValueAsString("Coverage frequency"));
        			if( Integer.parseInt(frequencyOfPeakTracks) < frequencyThreshold ) continue;
        			String chromosome = site.getSequence().getName();

//        			Interval coordinates = site.getInterval();
//        			int from = coordinates.getFrom(), to = coordinates.getTo(), length = to - from + 1;
        			int from = site.getFrom(), to = site.getTo(), length = to - from + 1;

        			// 2.2. Identify list of histoneTrackNames
        			List<String> list = new ArrayList<>();
                	for( int i = 0; i < tracksWithHistones.length; i++ )
                	{
            			DataCollection<Site> sites = tracksWithHistones[i].getSites(chromosome, from, to);
            			if( sites.getSize() > 0 )
            				list.add(histoneTrackNames[i]);
        			}
        			if( list.size() < thresholdOfNumberOfOverlappedTracks ) continue;
        			
        			// 2.3. Add new site.
        			String s = list.size() < 1 ? "N/A" : list.toString();
        			Site newSite = new SiteImpl(null, chromosome, null, Site.BASIS_USER_ANNOTATED, from, length, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
                    dps = newSite.getProperties();
                    dps.add(new DynamicProperty("Frequency of peak tracks", String.class, frequencyOfPeakTracks));
                    dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, raScore));
                    dps.add(new DynamicProperty("Coverage histone frequency", String.class, Integer.toString(list.size())));
                    dps.add(new DynamicProperty("Histone track names", String.class, s));
                    track.addSite(newSite);
        			log.info("index = " + index++ + " Meta-cluster: chr = " + chromosome + " from = " + from + " to = " + to + " Frequency of peak tracks = " + frequencyOfPeakTracks + " Coverage histone frequency = " + list.size() + " histones = " + s);
            	}
        		track.finalizeAddition();
                CollectionFactory.save(track);
        	}
        	
        	//*** Method 4 ***//
        	// GOOD!!! It also can be applied to site motifs!!! 
        	public static void analysisOfRaScoresAndHistoneModifications(DataElementPath pathToOutputFolder)
        	{
        		DataElementPath pathToTrackWithMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Metara_5");
        		//DataElementPath pathToTrackWithMetaClusters = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Metara_23");
        		
        		//DataElementPath pathToTrackWithHistoneModifications = DataElementPath.create("databases/GTRD_20.06/Data/peaks/Histone Modifications/macs2/HPEAKS002082");
        		DataElementPath pathToTrackWithHistoneModifications = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans02_site_motifs/REST_IPS_4.0");

        		analysisOfRAscoresAndHistoneModifications(pathToTrackWithMetaClusters, pathToTrackWithHistoneModifications, pathToOutputFolder);
        	}

        	private static void analysisOfRAscoresAndHistoneModifications(DataElementPath pathToTrackWithMetaClusters, DataElementPath pathToTrackWithHistoneModifications, DataElementPath pathToOutputFolder)
        	{
        		int subsampleSize = 100000; //subsampleSize = 50000

        		// 1. Calculate raScores and isCoveredByHistone
        		Track track = (Track)pathToTrackWithMetaClusters.getDataElement();
        		Track trackWithHistoneModifications = (Track)pathToTrackWithHistoneModifications.getDataElement();
        		String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
        		DataMatrix dataMatrix = SiteUtils.getProperties(track, propertiesNames);
        		double[] raScores = dataMatrix.getColumn(RankAggregation.RA_SCORE);
        		boolean[] isCoveredByHistone = new boolean[raScores.length];
        		int index = 0;
        		DataCollection<Site> dc = track.getAllSites();
        		for( Site site : dc )
        		{
        			String chromosomeName = site.getSequence().getName();
                    Interval coordinates = site.getInterval();
        			int from = coordinates.getFrom(), to = coordinates.getTo(); // length = to - from + 1;
        			DataCollection<Site> sitesInHistones = trackWithHistoneModifications.getSites(chromosomeName, from, to);
        			isCoveredByHistone[index++] = sitesInHistones.isEmpty() ? false : true;
        		}
    			log.info("O.K.1");

        		// 2. Calculate subsamples: raScoresSubsample and isCoveredByHistoneSubsample
        		double[] raScoresSubsample = new double[subsampleSize];
        		boolean[] isCoveredByHistoneSubsample = new boolean[subsampleSize];
            	int[] randomArray = RandomUtils.selectIndicesRandomly(raScores.length, subsampleSize, 0);
            	for( int i = 0; i < subsampleSize; i++ )
            	{
            		raScoresSubsample[i] = raScores[randomArray[i]];
            		isCoveredByHistoneSubsample[i] = isCoveredByHistone[randomArray[i]];
            	}
    			log.info("O.K.2");
            	
        		// 3. Calculate subsamples: raScoresSubsampleCovered and raScoresSubsampleNotCovered
            	List<Double> listForCovered = new ArrayList<>(), listForNotCovered = new ArrayList<>();
            	for( int i = 0; i < raScoresSubsample.length; i++ )
            	{
            		if( isCoveredByHistoneSubsample[i] )
            			listForCovered.add(raScoresSubsample[i]);
            		else
            			listForNotCovered.add(raScoresSubsample[i]);
            	}
            	double[] raScoresSubsampleCovered = UtilsGeneral.fromListToArray(listForCovered);
            	double[] raScoresSubsampleNotCovered = UtilsGeneral.fromListToArray(listForNotCovered);
    			log.info("O.K.3");
    			
            	// 4. Output results.
    			DataMatrix dm = getSimpleCharacteristicsOfSamples(raScoresSubsampleCovered, raScoresSubsampleNotCovered, raScoresSubsample);
    			dm.writeDataMatrix(false, pathToOutputFolder, "characteristics", log);
            	createAndWriteChartWithDensities(raScoresSubsample, raScoresSubsampleCovered, raScoresSubsampleNotCovered, pathToOutputFolder);
    			log.info("O.K.4");
        	}

            private static DataMatrix getSimpleCharacteristicsOfSamples(double[] raScoresSubsampleCovered, double[] raScoresSubsampleNotCovered, double[] raScoresSubsample)
            {
            	String[] sampleNames = new String[]{"RA-scores for meta-clusters overlapped with histones", "RA-scores for meta-clusters not overlapped with histones", "RA-scores for all meta-clusters"};
                double[][] matrix = new double[3][];
                double[] meanAndSigma = UnivariateSample.getMeanAndSigma(raScoresSubsampleCovered);
                double[] minAndMax = PrimitiveOperations.getMinAndMax(raScoresSubsampleCovered);
                matrix[0] = new double[]{raScoresSubsampleCovered.length, meanAndSigma[0], meanAndSigma[1], minAndMax[0], minAndMax[1]};
                
                meanAndSigma = UnivariateSample.getMeanAndSigma(raScoresSubsampleNotCovered);
                minAndMax = PrimitiveOperations.getMinAndMax(raScoresSubsampleNotCovered);
                matrix[1] = new double[]{raScoresSubsampleNotCovered.length, meanAndSigma[0], meanAndSigma[1], minAndMax[0], minAndMax[1]};
                
                meanAndSigma = UnivariateSample.getMeanAndSigma(raScoresSubsample);
                minAndMax = PrimitiveOperations.getMinAndMax(raScoresSubsample);
                matrix[2] = new double[]{raScoresSubsample.length, meanAndSigma[0], meanAndSigma[1], minAndMax[0], minAndMax[1]};
                return new DataMatrix(sampleNames, new String[]{"Size", "Mean", "Sigma", "Minimum", "Maximum"}, matrix);
            }
            
        	public static void createAndWriteChartWithDensities(double[] wholeSample, double[] raScoresSubsampleCovered, double[] raScoresSubsampleNotCovered, DataElementPath pathToOutputFolder)
            {
            	String nameOfChart = "RA-scores";
                int m = 3;
                String[] sampleNames = new String[]{"All meta-clusters", "Meta-clusters overlapped with histones", "Meta-clusters not overlapped with histones"};
                double[] multipliers = new double[]{1.0, 0.5, 0.5};
                double[][] xValuesForCurves = new double[m][], yValuesForCurves = new double[m][];
                double window = DensityEstimation.getWindow(wholeSample, DensityEstimation.WINDOW_WIDTH_01, null);

                DensityEstimation de = new DensityEstimation(wholeSample, window, true);
                double[][] curve = de.getCurve();
                xValuesForCurves[0] = curve[0];
                yValuesForCurves[0] = curve[1];

                de = new DensityEstimation(raScoresSubsampleCovered, window, true);
                curve = de.getCurve();
                xValuesForCurves[1] = curve[0];
                yValuesForCurves[1] = curve[1];
                
                de = new DensityEstimation(raScoresSubsampleNotCovered, window, true);
                curve = de.getCurve();
                xValuesForCurves[2] = curve[0];
                yValuesForCurves[2] = curve[1];
                
                Chart chart = DensityEstimation.createChartWithSmoothedDensities(xValuesForCurves, yValuesForCurves, sampleNames, nameOfChart,  multipliers);
            	TableAndFileUtils.addChartToTable("chart with " + nameOfChart, chart, pathToOutputFolder.getChildPath("_chart_" + nameOfChart));
            }
        }
    	        
    	/*********************************************************/
    	/****************** HistoneUtils: end ********************/
    	/*********************************************************/
        
        public static void comparisonOfMetaraAndImetaraMetaClusterTracks()
    	{
    		DataElementPath[] pathToFoldersFirst = new DataElementPath[]
    		{
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Meta_clusters_for_all_peaks"),
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Meta_clusters_for_all_peaks"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/Meta_clusters_for_all_peaks"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/Meta_clusters_for_all_peaks"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Meta_clusters_for_all_peaks"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment/Meta_clusters_for_all_peaks"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Meta_clusters_for_all_peaks")
    		};

    		DataElementPath[] pathToFoldersSecond = new DataElementPath[]
    		{
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Metara_5_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Metara_23_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/Metara_25_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/Metara_27_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Metara_12_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment/Metara_3_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Metara_27_merged_")
    		};
    		
        	for( int i = 0; i < pathToFoldersFirst.length; i++ )
        		TrackUtils.comparisonOfTwoTracksByOverlapsFrequencies(pathToFoldersFirst[i], pathToFoldersSecond[i]);
    	}
        
        public static void comparisonOfTreatmentAndNotTreatmentforMetaClusters()
    	{
    		DataElementPath[] pathToFoldersFirst = new DataElementPath[]
    		{
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Metara_23_merged_"),
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/Metara_25_merged_"),
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Metara_12_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Metara_27_merged_"),
    		};

    		DataElementPath[] pathToFoldersSecond = new DataElementPath[]
    		{
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment_/Metara_20_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/Metara_27_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment/Metara_3_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Metara_5_merged_"),
    		};
    		
        	for( int i = 0; i < pathToFoldersFirst.length; i++ )
        		TrackUtils.comparisonOfTwoTracksByOverlapsFrequencies(pathToFoldersFirst[i], pathToFoldersSecond[i]);
    	}
        
        public static void comparisonOfMetaClustersForDistinctTf()
    	{
    		DataElementPath[] pathToFoldersFirst = new DataElementPath[]
    		{
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Metara_23_merged_"),
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/Metara_25_merged_"),
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Metara_12_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Metara_27_merged_"),
    		};

    		DataElementPath[] pathToFoldersSecond = new DataElementPath[]
    		{
   				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment_/Metara_20_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/Metara_27_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment/Metara_3_merged_"),
    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Metara_5_merged_"),
    		};
    		
        	for( int i = 0; i < pathToFoldersFirst.length - 1; i++ )
        		for( int j = i + 1; j < pathToFoldersFirst.length; j++ )
        			TrackUtils.comparisonOfTwoTracksByOverlapsFrequencies(pathToFoldersFirst[i], pathToFoldersFirst[j]);
        	
        	for( int i = 0; i < pathToFoldersSecond.length - 1; i++ )
        		for( int j = i + 1; j < pathToFoldersSecond.length; j++ )
        			TrackUtils.comparisonOfTwoTracksByOverlapsFrequencies(pathToFoldersSecond[i], pathToFoldersSecond[j]);
    	}

    	public static void comparisonOfRaScoresAndFrequenciesOfMetaClusters(DataElementPath pathToOutputFolder)
    	{
    		DataElementPath[] pathToTracksWithMetaClusters = new DataElementPath[]
    	    		{
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Metara_23"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment_/Metara_20"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/Metara_25"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/Metara_27"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Metara_12"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment/Metara_3"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Metara_27"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Metara_5")
    	    		};
    		String[] namesOfCharts = new String[]
    				{
    					"ESR1_no_treat", "ESR1_yes_treat", "FOXA1_no_treat", "FOXA1_yes_treat", "JUND_no_treat", "JUND_yes_treat", "REST_no_treat", "REST_yes_treat"
    				};
    		
        	for( int i = 0; i < namesOfCharts.length; i++ )
        		comparisonOfRaScoresAndFrequenciesOfMetaClusters(pathToTracksWithMetaClusters[i], namesOfCharts[i], pathToOutputFolder);
    	}
    	
    	// To drow figure!!!
    	public static void getOverlappedIntervals()
    	{
    		String chromosome = "10";
    		int from = 6713139, to = 6713900;
    		DataElementPath pathToFolderWithTracks = DataElementPath.create("databases/GTRD_20.06/Data/peaks/Histone Modifications/macs2");
            String[] trackNames = pathToFolderWithTracks.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
    		getOverlappedIntervals(chromosome, from, to, pathToFolderWithTracks, trackNames);
    	}
    	
    	public static void getOverlappedIntervals(String chromosome, int from, int to, DataElementPath pathToFolderWithTracks, String[] trackNames)
    	{
    		for( String trackName : trackNames )
    		{
            	log.info("trackName = " + trackName);
    			Track track = (Track)pathToFolderWithTracks.getChildPath(trackName).getDataElement();

    			// 1.
    			DataCollectionInfo info = ((DataCollection<?>)track).getInfo();
    	        String pathToSequenceCollection = info.getProperty("Sequence collection");
            	log.info("pathToSequenceCollection = " + pathToSequenceCollection);

            	// 2.    	        
    			DataCollection<Site> sites = track.getSites(chromosome, from, to);
    			if( sites.isEmpty() ) continue;
    			int index = 0;
    			for( Site site : sites )
    			{
        			DynamicPropertySet dps = site.getProperties();
        			String histoneModification = dps.getValueAsString("name");
        			int start = site.getFrom(), end = site.getTo();
                	log.info("*** index = " + index++ + " trackName = " + trackName + " histoneModification = " + histoneModification + " start = " + start + " end = " + end);
    			}
    			if( index >= 10 ) break;
    		}
    	}
    	
    	// Temp; Implement of METARA after IMETARA for TEST
    	public static void implementMetaraForSelectedTrackNames()
    	{
    		// 1.
//        	log.info("\n METARA for FOXA1_yes_treatment");
//        	String nameOfImetaraResultedTrack = "Metara_27";
//        	DataElementPath pathToFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment");
//    		implementMetaraForSelectedTrackNames(nameOfImetaraResultedTrack,  pathToFolder);

    		// 2.
        	log.info("\n METARA for ESR1_yes_treatment");
        	String nameOfImetaraResultedTrack = "Metara_20";
        	DataElementPath pathToFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment_");
    		implementMetaraForSelectedTrackNames(nameOfImetaraResultedTrack,  pathToFolder);
    	}
    	
    	private static void implementMetaraForSelectedTrackNames(String nameOfImetaraResultedTrack, DataElementPath pathToFolder)
    	{
    		// 01.04.22
        	//String cistromType = CistromConstructor.OPTION_02;
    		String cistromType = MetaClusterConsrtruction.OPTION_02;
    		
        	String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
        	String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
        	double fpcmThreshold = 1000.0;
        	int siteNumberThreshold = 2000;
        	int maximalLengthOfPeaks = 1000000;
        	int minimalLengthOfPeaks = 30;
        	String trackNameOfMetaClustersForAllPeaks = "Meta_clusters_for_all_peaks";
        	DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD_20.06/Data/peaks");
        	
    		Track track = (Track)pathToFolder.getChildPath(nameOfImetaraResultedTrack).getDataElement();
    		String[] allTrackNames = getDistinctTrackNames(track);
        	String[] foldersNames = new String[]{"gem", "macs2", "pics", "sissrs"};
        	new CistromConstructor(cistromType, combinedPeakType, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, allTrackNames, trackNameOfMetaClustersForAllPeaks, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToFolder);
    	}
    	
    	public static void implementMetaraForESR1()
    	{
    		// REMARK: not enough memory !!!
        	// It is for ESR1
    		//DataElementPath pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment");
    																   //data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment
    		
        	// It is for ELK1
    		DataElementPath pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX_DERIVETION/RAB/RAB01_meta_clusters/ELK1");

    		
        	DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD_20.06/Data/peaks");
//    		String[] allTrackNames = new String[]
//    		{		"PEAKS033069", "PEAKS033070", "PEAKS033084", "PEAKS033093", "PEAKS033094",
//    				"PEAKS033119", "PEAKS033120", "PEAKS033121", "PEAKS033122", "PEAKS033127",
//    				"PEAKS033129", "PEAKS033134", "PEAKS033137", "PEAKS033145", "PEAKS033401",
//    				"PEAKS033403", "PEAKS034510", "PEAKS034511", "PEAKS034512", "PEAKS034513",
//    				"PEAKS034514", "PEAKS034596", "PEAKS034597", "PEAKS034658", "PEAKS034911"
//        	};
        	
        	// TODO: Temporary! Trancated allTrackNames !!! 
//        	String[] allTrackNames = new String[]
//            		{		"PEAKS033070", "PEAKS033084", "PEAKS033093"
//                	};
        	
        	// It is for ELK1
        	String[] allTrackNames = new String[]{"PEAKS033061", "PEAKS041424", "PEAKS041450", "PEAKS041467", "PEAKS041509"};

    		
        	// It is temporary for FOXA1
//    		DataElementPath pathToOutputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/FOXA1_RAB");
//        	String[] allTrackNames = new String[]
//        	  {"PEAKS039820", "PEAKS040142", "PEAKS033056", "PEAKS033194", "PEAKS033357",
//			   "PEAKS033546", "PEAKS039104", "PEAKS038433", "PEAKS039334", "PEAKS033097",
//			   "PEAKS033251", "PEAKS033367", "PEAKS033452", "PEAKS041024", "PEAKS033530",
//			   "PEAKS033793", "PEAKS034828", "PEAKS035019", "PEAKS038447", "PEAKS039335",
//			   "PEAKS039418", "PEAKS040684", "PEAKS041015", "PEAKS041016", "PEAKS041017",
//			   "PEAKS041018", "PEAKS041638", "PEAKS041662", "PEAKS041726"};


    		
        	
        	// 01.04.22
        	// String cistromType = CistromConstructor.OPTION_02;
        	String cistromType = MetaClusterConsrtruction.OPTION_02;
        	String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
        	String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
        	double fpcmThreshold = 3.0; // fpcmThreshold = 3.0; fpcmThreshold = 7.0;
        	int siteNumberThreshold = 2000;
        	String[] foldersNames = new String[]{"gem", "macs2", "pics", "sissrs"};
        	String trackNameOfMetaClustersForAllPeaks = "Meta_clusters_for_all_peaks";
        	int maximalLengthOfPeaks = 1000000;
        	int minimalLengthOfPeaks = 30;
        	new CistromConstructor(cistromType, combinedPeakType, rankAggregationMethod, fpcmThreshold, siteNumberThreshold,
        						   pathToFolderWithFolders, foldersNames, allTrackNames, trackNameOfMetaClustersForAllPeaks,
        						   minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
    	}

    	public static void implementImetaraForGivenTf(DataElementPath pathToOutputFolder)
        {
    		// 01.04.22
        	// String cistromType = CistromConstructor.OPTION_02;
    		String cistromType = MetaClusterConsrtruction.OPTION_02;
        	String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
        	String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
        	double fpcmThreshold = 3.0; // fpcmThreshold = 3.0; fpcmThreshold = 7.0;
        	int siteNumberThreshold = 2000;
        	int maximalLengthOfPeaks = 1000000;
        	int minimalLengthOfPeaks = 30;
        	int numberOfAllTracksForAnalysis = 16; //numberOfAllTracksForAnalysis = 30; number of tracks for selection; it can be changed!!!.
        	String tfName = "JUND"; // "REST", "ESR1", "FOXA1", "JUND"
        	boolean doWithoutTreatedExperiments = true; // if doRemoveTreatedExperiments = false then only treated experiments are analized 
        	DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD_20.06/Data/peaks");
        	String[] foldersNames = new String[]{"gem", "macs2", "pics", "sissrs"};
        	DataElementPath pathToTableWithSummaryOnGtrdPeaks = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/summary_on_chip_seq_tracks");
        	
        	// 1. Calculate trackNames (names of selected tracks).
        	//String[] trackNames = identifyTrackNames(pathToTableWithSummaryOnGtrdPeaks, tfName, doWithoutTreatedExperiments, numberOfAllTracksForAnalysis);

        	/////////////////////////////// TEMP ////////////////
//        	trackNames = (String[]) ArrayUtils.removeElement(trackNames, "PEAKS040702"); ////// !!!!!!!!!!!!!!!!!! To remove after run.dat !!!!!!!!!!!
//        	numberOfAllTracksForAnalysis -= 2;	////// !!!!!!!!!!!!!!!!!! To remove after run.dat !!!!!!!!!!!
        	
        	/////////////////////////////// TEMP ////////////////
        	// FOXA1
//        	String[] trackNames = new String[]{"PEAKS039820", "PEAKS040142", "PEAKS033056", "PEAKS033194", "PEAKS033357",
//        									   "PEAKS033546", "PEAKS039104", "PEAKS038433", "PEAKS039334", "PEAKS033097",
//        									   "PEAKS033251", "PEAKS033367", "PEAKS033452", "PEAKS041024", "PEAKS033530",
//        									   "PEAKS033793", "PEAKS034828", "PEAKS035019", "PEAKS038447", "PEAKS039335",
//        									   "PEAKS039418", "PEAKS040684", "PEAKS041015", "PEAKS041016", "PEAKS041017",
//        									   "PEAKS041018", "PEAKS041638", "PEAKS041662", "PEAKS041726"};

        	// ELK1
        	String[] trackNames = new String[]{"PEAKS033061", "PEAKS041424", "PEAKS041450", "PEAKS041467", "PEAKS041509"};

        	
        	
        	// JUND treatment
        	// "PEAKS033980", "PEAKS033890", "PEAKS034559", "PEAKS038842"
        	
        	
        	// TEMP: for test
        	// String[][] trackNamesForEverySteps = new String[][]{new String[]{"PEAKS033980"}, new String[]{"PEAKS033890"}, new String[]{"PEAKS034559"}, new String[]{"PEAKS038842"}};
        
        	// 2. Calculate trackNamesForEverySteps and allTrackNames.
        	// 09.03.22
        	//int typeOfStructureOfOutputArray = 0;
        	// String[][] trackNamesForEverySteps = getTrackNamesForEverySteps(numberOfAllTracksForAnalysis, trackNames, typeOfStructureOfOutputArray);
        	String[][] trackNamesForEverySteps = new String[trackNames.length][];
        	for( int jj = 0; jj < trackNames.length; jj++ )
        		trackNamesForEverySteps[jj] = new String[]{trackNames[jj]};
        		
        	
        	String[] allTrackNames = trackNamesForEverySteps[0];
        	for( int i = 1; i < trackNamesForEverySteps.length; i++ )
            	allTrackNames = (String[]) ArrayUtils.addAll(allTrackNames, trackNamesForEverySteps[i]);
        	log.info("O.K.1 : determination of track names for every steps");
        	
        	// 3. Apply METARA to tracks in each step of IMETARA.
        	log.info("trackNamesForEverySteps.length = " + trackNamesForEverySteps.length);
        	for( int i = 0; i < trackNamesForEverySteps.length; i++ )
        	{
            	log.info("METARA in i = " + Integer.toString(i) + " step");
        		String nameOfTemporaryTrack = "_temporary_track", nameOfFinalTrack = "MetaClusters_step_" + Integer.toString(i);
            	new CistromConstructor(cistromType, combinedPeakType, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, trackNamesForEverySteps[i], nameOfTemporaryTrack, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
            	log.info("Step " + i + " CistromConstructor: O.K.");

            	// new : extend the site boundaries
            	changeSiteLengths(nameOfTemporaryTrack, nameOfFinalTrack, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
            	log.info("Step " + i + " changeSiteLengths: O.K.");

            	DataElementPath path = pathToOutputFolder.getChildPath(nameOfTemporaryTrack);
            	path.remove();
        	}
        	log.info("O.K.2 : Application of METARA to tracks in each step of IMETARA");
        	
        	// 4.Implement the increment steps of IMETARA.
        	for( int i = 1; i < trackNamesForEverySteps.length; i++ )
    		{
            	log.info("*** IMETARA *** : step = " + i);
        		String nameOfOldTrack =  i == 1 ? "MetaClusters_step_0" : "Metara_" + Integer.toString(i - 1);
        		String nameOfAdditionalSitesTrack = "MetaClusters_step_" + Integer.toString(i);
        		String nameOfResultedTrack = "Metara_" + Integer.toString(i);
            	implementOneStepOfImetara(nameOfOldTrack, nameOfAdditionalSitesTrack, nameOfResultedTrack, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
    		}
        	
        	// 5. Calculate meta-clusters for all tracks available for METARA.
        	String trackNameOfMetaClustersForAllPeaks = "Meta_clusters_for_all_peaks";
        	
    		//////
        	new CistromConstructor(cistromType, combinedPeakType, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, allTrackNames, trackNameOfMetaClustersForAllPeaks, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
            DataElementPath pathToTrack = pathToOutputFolder.getChildPath(trackNameOfMetaClustersForAllPeaks);
        	Track track = pathToTrack.getDataElement(Track.class);
        	log.info("**** size of track with meta-clusters for all available tracks = " + track.getAllSites().getSize());
        	
        	// 6. Compare two tracks.
        	String nameOld = "Meta_clusters_for_all_peaks";
        	String nameNew = "Metara_" + Integer.toString(trackNamesForEverySteps.length - 1);
    		DataElementPath pathToOldTrack = pathToOutputFolder.getChildPath(nameOld);
    		DataElementPath pathToNewTrack = pathToOutputFolder.getChildPath(nameNew);
    		
    		//compareTwoTracks(pathToOldTrack, pathToNewTrack);
    		TrackUtils.comparisonOfTwoTracksByOverlapsFrequencies(pathToOldTrack, pathToNewTrack);
        }

    	///////////////////////////////////////////////////
    	///////////////////////////////////////////////////
    	///////////////////////////////////////////////////
    	
    	
    	

    	public static void createMatrixLibrary() throws Exception
    	{
    		DataElementPath dataElementPathToMatrixLibrary1 = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_01_");
    		DataElementPath dataElementPathToMatrixLibrary2 = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_02_");
    		DataElementPath dataElementPathToMatrixLibrary3 = DataElementPath.create("data/Collaboration/yura_project/Data/Files/MATRIX/MATRIX_03_");

    		WeightMatrixCollection.createMatrixLibrary(dataElementPathToMatrixLibrary1, log);
    		WeightMatrixCollection.createMatrixLibrary(dataElementPathToMatrixLibrary2, log);
    		WeightMatrixCollection.createMatrixLibrary(dataElementPathToMatrixLibrary3, log);

    	}
    	
        public void printFrequencyMatrixAndConsensus(FrequencyMatrix frequencyMatrix)
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
            	consensus += rowNames[(int) PrimitiveOperations.getMax(MatrixUtils.getColumn(matrix, i))[0]];
            log.info("Initial approximation:\nFrequency matrix =");
            DataMatrix.printDataMatrix(new DataMatrix(rowNames, columnNames, matrix));
            log.info("Consensus = " + consensus);
        }


    	private static Interval getMergedInterval(DataCollection<Site> dataCollection)
    	{
    		int fromTotal = Integer.MAX_VALUE, toTotal = -1;
    		for( Site site : dataCollection )
    		{
    			fromTotal = Math.min(fromTotal, site.getFrom());
    			toTotal = Math.max(toTotal, site.getTo());
    		}
    		return new Interval(fromTotal, toTotal);
    	}
    	
    	private static Interval getMergedInterval(Track track, String chromosomeName, int fromInitial, int toInitial)
    	{
    		int fromOld = fromInitial, toOld = toInitial;
    		while( true )
    		{
            	DataCollection<Site> dataCollection = track.getSites(chromosomeName, fromOld, toOld);
            	Interval interval = getMergedInterval(dataCollection);
        		int fromNew = interval.getFrom(), toNew = interval.getTo();
        		if( fromNew == fromOld && toNew == toOld ) return interval;
        		fromOld = fromNew;
        		toOld = toNew;
    		}
    	}
    	
    	// TODO: To test this method !!!!!!!!!!!!!!!!!!!!!!!
    	private static Site getMergedSite(DataCollection<Site> dataCollection)
    	{
    		// 1. Calculate chromosomeName and initial from and to.
    		String chromosomeName = null;
    		Interval coordinates = null;
    		for( Site site : dataCollection )
    		{
                chromosomeName = site.getSequence().getName();
                coordinates = site.getInterval();
                break;
    		}
    		
    		// 2. Create merged site.
    		int fromNew = Integer.MAX_VALUE, toNew = 0;
    		String trackNamesNew = "";
			double rankAggregationScoreNew = Double.MAX_VALUE;

    		for( Site site : dataCollection )
    		{
    			// 2.1. Calculate fromNew and toNew.
                coordinates = site.getInterval();
    			fromNew = Math.min(coordinates.getFrom(), fromNew);
    			toNew = Math.max(coordinates.getTo(), toNew);

                // 2.2. Calculate trackNamesNew as String.
    			DynamicPropertySet dps = site.getProperties();
    			String string = dps.getValueAsString(RankAggregation.RA_SCORE);
    			double rankAggregationScore = string != null ? Double.parseDouble(string) : Double.NaN;
    			rankAggregationScoreNew = Math.min(rankAggregationScoreNew, rankAggregationScore);

    			String trackNames = dps.getValueAsString("Additional_property");
    			// if( trackNames.length() > 0 )
    			if( trackNames.length() > 0 && trackNamesNew.length() > 0 )
    				trackNamesNew += ",";
    			trackNamesNew += trackNames;
    		}
    		
    		// 2.3. Calculate distinctTrackNames and trackNamesNew as String. 
            String[] strings = TextUtil.split(trackNamesNew, ',');
            String[] distinctTrackNames = UtilsGeneral.getDistinctValues(strings);
            
            String distinctTrackNamesAsString = "";
            for( int j = 0; j < distinctTrackNames.length; j++ )
            {
            	distinctTrackNamesAsString += distinctTrackNames[j];
            	if( j < distinctTrackNames.length - 1 )
            		distinctTrackNamesAsString += ",";
            }
            int frequency = distinctTrackNames.length;
            
            if ( frequency >= 4 )
            {
                log.info("chromosomeName = " + chromosomeName + " fromNew = " + fromNew + " toNew = " + toNew + " frequency = " + frequency);
            }
            
            // 2.4. Add DynamicPropertySet to siteNew.
            Site siteNew = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, fromNew, toNew - fromNew + 1, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
            DynamicPropertySet dps = siteNew.getProperties();
            dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(frequency)));
            dps.add(new DynamicProperty("Additional_property", String.class, distinctTrackNamesAsString));
            dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, Double.toString(rankAggregationScoreNew)));
            
//			String trackNames = trackNamesFirst;
//			int frequency = 1;
//            for( int i = 0; i < trackNames.length(); i++ )
//            	if( trackNames.charAt(i) == ',')
//            		frequency++;
//            // double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks(trackFirst, trackSecond);
//            double rankAggregationScoreNew = recalculateRAscore(rankAggregationScoreFirst, 0.0, 0, someCharacteristicsOfTwoTracks) / someCharacteristicsOfTwoTracks[3];
//            DynamicPropertySet dps = newSite.getProperties();
//            dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(frequency)));
//            dps.add(new DynamicProperty("Additional_property", String.class, trackNames));
//            dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, Double.toString(rankAggregationScoreNew)));

    		return siteNew;
    	}

    	////////////////// TEMPORARY COPY ; it is modified!!!!////////////////
        private static FunSite[] getSites(String chromosomeName, int from, int to, Map<String, List<FunSite>> allSites)
        {
        	List<FunSite> result = new ArrayList<>();
        	Interval interval = new Interval(from, to);
        	List<FunSite> funSites = allSites.get(chromosomeName);
        	for( FunSite fs : funSites )
        	{
        		Interval coordinates = fs.getCoordinates();
        		if( coordinates.intersects(interval) )
        			result.add(fs);
        		//if( to < fs.getFinishPosition() ) break;
        	}
        	return result.toArray(new FunSite[0]);
        }
        
    	public static void testForKmeansModel(DataElementPath pathToOutputFolder)
    	{
    		int numberOfClusters = 5;
    		DataElementPath pathToMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Machine_learning/Data_test/scor");
    		log.info("Distance = " + Distance.DISTANSCE_1_MANHATTAN);
    		Object[] additionalInputParameters = new Object[]{Distance.DISTANSCE_1_MANHATTAN};    		
    		DataMatrix dataMatrix = new DataMatrix(pathToMatrix, null);
    		boolean doCalculateAccompaniedInformation = true;
    		KmeansModel kmeans = new KmeansModel(numberOfClusters, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformation);
    		kmeans.saveResults(pathToOutputFolder);
    	}

    	public static void mergeOverlappedSites(AnalysisJobControl jobControl)
    	{
    		DataElementPath[] paths = new DataElementPath[]
    				{
    				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_"),
    				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_"),
    				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment"),
    				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment"),
    				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment"),
    				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_"),
    				DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_")
    				};
    		
    		String[] namesOfInputTracks = new String[]{"Metara_23", "Metara_25", "Metara_27", "Metara_12", "Metara_3", "Metara_27", "Metara_5"};
    		
            for( int i = 0; i < paths.length; i++ )
            {
                log.info("i = " + i + " path = " + paths[i].toString());
                TrackUtils.mergeOverlappedSites(paths[i], namesOfInputTracks[i], jobControl, 0, 100);
            }
    	}

//    	public static void mergeOverlappedSites(DataElementPath pathToFolder, String nameOfInputTrack, AnalysisJobControl jobControl, int fromJobControl, int toJobControl)
//    	{
//    		DataElementPath pathToInputTrack = pathToFolder.getChildPath(nameOfInputTrack);
//    		DataElementPath pathToOutputTrack = pathToFolder.getChildPath(nameOfInputTrack + "_merged__");
//
//    		// 1. Read FunSite in input track.
//    		Track inputTrack = (Track)pathToInputTrack.getDataElement();
//    		SqlTrack resultedTrack = SqlTrack.createTrack(pathToOutputTrack, null);
//            Map<String, List<FunSite>> allOverlappedSites = new HashMap<>();
//            DataMatrix dataMatrix = null;
//        	log.info("Read sites in track");
//    		for( Site site : inputTrack.getAllSites() )
//    		{
//    			String chromosomeName = site.getSequence().getName();
//                Interval coordinates = site.getInterval();
//    			FunSite funSite = new FunSite(chromosomeName, coordinates, site.getStrand(), dataMatrix);
//    			funSite.setObjects(new Object[]{site});
//    			allOverlappedSites.computeIfAbsent(chromosomeName, key -> new ArrayList<>()).add(funSite);
//    		}
//        	log.info("O.K.1");
//    		
//    		// 2. Select a single site (with maximal frequency) from set of overlapped sites and write it.
//        	log.info("*** all overlapped sites are sorting ***");
//    		ListUtil.sortAll(allOverlappedSites);
//        	log.info("*** all overlapped sites were sorted ***");
//            int index = 0, difference = toJobControl - fromJobControl; 
//            List<FunSite> list = new ArrayList<>();
//            for( Entry<String, List<FunSite>> entry : allOverlappedSites.entrySet() )
//            {
//                if( jobControl != null )
//                    jobControl.setPreparedness(fromJobControl + (++index) * difference / allOverlappedSites.size());
//                FunSite[] funSites = entry.getValue().toArray(new FunSite[0]);
//                for( int i = 0; i < funSites.length; i++ )
//                {
//                	if( list.isEmpty() )
//                	{
//                		list.add(funSites[i]);
//                		continue;
//                	}
//                	Interval coordinates = funSites[i].getCoordinates(), coordinatesPrevious = funSites[i - 1].getCoordinates();
//                	if( coordinates.intersects(coordinatesPrevious) )
//                		list.add(funSites[i]);
//                	else
//                	{
//                		selectSiteAndWriteItToTrack(list, resultedTrack);
//                		list.add(funSites[i]);
//                	}
//                }
//            	if( ! list.isEmpty() )
//            		selectSiteAndWriteItToTrack(list, resultedTrack);
//            }
//            jobControl.setPreparedness(toJobControl);
//    		resultedTrack.finalizeAddition();
//    		CollectionFactory.save(resultedTrack);
//    	}
    	
//    	private static void selectSiteAndWriteItToTrack(List<FunSite> list, SqlTrack track)
//    	{
//        	log.info("dim(list) = " + list.size());
//    		double frequencySelected = -1.0;
//    		Site siteSelected = null;
//    		for( FunSite funSite : list )
//    		{
//    			Site site = (Site)funSite.getObjects()[0];
//    			DynamicPropertySet dpsFirst = site.getProperties();
//    			double frequency = Double.parseDouble(dpsFirst.getValueAsString("Frequency"));
//    			if( frequency > frequencySelected )
//    			{
//    				frequencySelected = frequency;
//    				siteSelected = site;
//    			}
//    		}
//			track.addSite(siteSelected);
//			list.clear();
//    	}

    	//Site newSite = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, start, length, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
    	//String trackNames = trackNamesFirst;
    	//int frequency = 1;
    	//for( int i = 0; i < trackNames.length(); i++ )
    	//if( trackNames.charAt(i) == ',')
    	//frequency++;
    	//// double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks(trackFirst, trackSecond);
    	//double rankAggregationScoreNew = recalculateRAscore(rankAggregationScoreFirst, 0.0, 0, someCharacteristicsOfTwoTracks) / someCharacteristicsOfTwoTracks[3];
    	//DynamicPropertySet dps = newSite.getProperties();
    	//dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(frequency)));
    	//dps.add(new DynamicProperty("Additional_property", String.class, trackNames));
    	//dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, Double.toString(rankAggregationScoreNew)));
    	//resultedTrackTemporary.addSite(newSite);
    	
    	// new version 3 of implementOneStepOfImetara()
    	private static void implementOneStepOfImetara(String nameOfOldTrack, String nameOfAdditionalSitesTrack, String nameOfResultedTrack, int minimalLengthOfSite, int maximalLengthOfSite, DataElementPath pathToOutputFolder)
    	{
    		// 1. Open 2 input tracks and resulted track.
    		DataElementPath pathToFirstTrack = pathToOutputFolder.getChildPath(nameOfOldTrack); 
    		Track trackFirst = (Track)pathToFirstTrack.getDataElement();
    		DataElementPath pathToSecondTrack = pathToOutputFolder.getChildPath(nameOfAdditionalSitesTrack); 
    		Track trackSecond = (Track)pathToSecondTrack.getDataElement();
    		String nameOfResultedTrackTemporary = nameOfResultedTrack;
    		DataElementPath pathToResultedTrackTemporary = pathToOutputFolder.getChildPath(nameOfResultedTrackTemporary); 
    		SqlTrack resultedTrackTemporary = SqlTrack.createTrack(pathToResultedTrackTemporary, null);
    		
            double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks(trackFirst, trackSecond);

    		// TODO: temp
//        	log.info("nameOfOldTrack = " + nameOfOldTrack + " nameOfAdditionalSitesTrack = " + nameOfAdditionalSitesTrack + " nameOfResultedTrack = " + nameOfResultedTrack);
//        	log.info("trackFirst = " + trackFirst + " n = " + trackFirst.getAllSites().getSize() + " trackSecond = " + trackSecond + " n = " + trackSecond.getAllSites().getSize());
        	//

        	// 2. Analysis of sites from 1-st track.
    		for( Site site : trackFirst.getAllSites() )
            {
    			// 2.1 Calculate rankAggregationScoreFirst and trackNamesFirst.
                String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();
                coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    			DynamicPropertySet dpsFirst = site.getProperties();
    			String string = dpsFirst.getValueAsString(RankAggregation.RA_SCORE);
    			double rankAggregationScoreFirst = string != null ? Double.parseDouble(string) : Double.NaN;
    			String trackNamesFirst = dpsFirst.getValueAsString("Additional_property");

    			// 2.2. Add sites that are in : 1-st track - yes and 2-nd track - no
                DataCollection<Site> dc = trackSecond.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo());
                if( dc.isEmpty() )
                {
                	// TODO: To recalculate RA-scores !!!!
                	
                	DataCollection<Site> dcTemp = resultedTrackTemporary.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo()); 
                	if( ! dcTemp.isEmpty() )
                    {
                    	log.info("WWWWWWWW Warning (due to site extention) of type 1 : (yes, no) WWWWWWWWWW");
                    	log.info("WWWWWWWW Site: chromosomeName = " + chromosomeName + " from = " + coordinates.getFrom() + " to = " + coordinates.getTo() + " WWWWWWWWWW");
                    	for( Site siteTepm : dcTemp )
                    	{
                            Interval coordinatesTemp = siteTepm.getInterval();
                        	log.info("WWWWWWWW siteTepm: chromosomeName = " + siteTepm.getSequence().getName() + " from = " + coordinatesTemp.getFrom() + " to = " + coordinatesTemp.getTo() + " WWWWWWWWWW");
                    	}
                    	continue;
                    }

                	// create and write newSite 
        			int start = coordinates.getFrom(), end = coordinates.getTo(), length = end - start + 1;
        			Site newSite = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, start, length, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
        			String trackNames = trackNamesFirst;
        			int frequency = 1;
                    for( int i = 0; i < trackNames.length(); i++ )
                    	if( trackNames.charAt(i) == ',')
                    		frequency++;
                    // double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks(trackFirst, trackSecond);
                    double rankAggregationScoreNew = recalculateRAscore(rankAggregationScoreFirst, 0.0, 0, someCharacteristicsOfTwoTracks) / someCharacteristicsOfTwoTracks[3];
                    DynamicPropertySet dps = newSite.getProperties();
                    dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(frequency)));
                    dps.add(new DynamicProperty("Additional_property", String.class, trackNames));
                    dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, Double.toString(rankAggregationScoreNew)));
                    resultedTrackTemporary.addSite(newSite);
                	continue;
                }
                
    			// 2.3. Add sites that are in : 1-st track - yes and 2-nd track - yes
        		for( Site siteAdditional : dc )
        		{
        			Interval coordinatesAdditional = siteAdditional.getInterval();
        			DynamicPropertySet dps = siteAdditional.getProperties();
        			string = dps.getValueAsString(RankAggregation.RA_SCORE);
        			double rankAggregationScoreSecond = string != null ? Double.parseDouble(string) : Double.NaN;
        			String trackNamesSecond = dps.getValueAsString("Additional_property");

        			// 2.3.1. Add sites that are in : 1-st track - yes and 2-nd track - yes
        			Interval coordinatesNew = coordinates.intersect(coordinatesAdditional);
        			coordinatesNew = SiteUtils.changeInterval(coordinatesNew, minimalLengthOfSite, maximalLengthOfSite);
        			int start = coordinatesNew.getFrom(), end = coordinatesNew.getTo(), length = end - start + 1;
                	DataCollection<Site> dcTemp = resultedTrackTemporary.getSites(chromosomeName, start, end); 
                	if( ! dcTemp.isEmpty() )
                    {
                    	log.info("WWWWWWWW Warning (due to site extention) of type 2 : (yes, yes) WWWWWWWWWW");
                    	log.info("WWWWWWWW Site: chromosomeName = " + chromosomeName + " from = " + coordinates.getFrom() + " to = " + coordinates.getTo() + " WWWWWWWWWW");
                    	for( Site siteTepm : dcTemp )
                    	{
                            Interval coordinatesTemp = siteTepm.getInterval();
                        	log.info("WWWWWWWW siteTepm: chromosomeName = " + siteTepm.getSequence().getName() + " from = " + coordinatesTemp.getFrom() + " to = " + coordinatesTemp.getTo() + "WWWWWWWWWW");
                    	}
                    	continue;
                    }
                	
                	// create and write newSite 
        			Site newSite = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, start, length, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
        			String trackNames = trackNamesFirst + "," + trackNamesSecond;
        			int frequency = 1;
                    for( int i = 0; i < trackNames.length(); i++ )
                    	if( trackNames.charAt(i) == ',')
                    		frequency++;
                    //double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks(trackFirst, trackSecond);
                    double rankAggregationScoreNew = (recalculateRAscore(rankAggregationScoreFirst, rankAggregationScoreSecond, 1, someCharacteristicsOfTwoTracks)) / someCharacteristicsOfTwoTracks[3];
                    dps = newSite.getProperties();
                    dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(frequency)));
                    dps.add(new DynamicProperty("Additional_property", String.class, trackNames));
                    dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, Double.toString(rankAggregationScoreNew)));
                    resultedTrackTemporary.addSite(newSite);
        		}
            }
        	log.info("O.K.1"); 

    		// 3. Add sites that are in : 1-st track - no and 2-nd track - yes
    		for( Site site : trackSecond.getAllSites() )
            {
                String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();
                coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    			DynamicPropertySet dps = site.getProperties();
    			String string = dps.getValueAsString(RankAggregation.RA_SCORE);
    			double rankAggregationScoreSecond = string != null ? Double.parseDouble(string) : Double.NaN;
    			String trackNamesSecond = dps.getValueAsString("Additional_property");
    			int start = coordinates.getFrom(), end = coordinates.getTo(), length = end - start + 1;
    			
                DataCollection<Site> dc = trackFirst.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo());
                if( dc.isEmpty() )
                {
                	DataCollection<Site> dcTemp = resultedTrackTemporary.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo()); 
                	if( ! dcTemp.isEmpty() )
                    {
                    	log.info("WWWWWWWW Warning (due to site extention) of type 3 : (no, yes) WWWWWWWWWW");
                    	log.info("WWWWWWWW Site: chromosomeName = " + chromosomeName + " from = " + coordinates.getFrom() + " to = " + coordinates.getTo() + "WWWWWWWWWW");
                    	for( Site siteTepm : dcTemp )
                    	{
                            Interval coordinatesTemp = siteTepm.getInterval();
                        	log.info("WWWWWWWW siteTepm: chromosomeName = " + siteTepm.getSequence().getName() + " from = " + coordinatesTemp.getFrom() + " to = " + coordinatesTemp.getTo() + " WWWWWWWWWW");
                    	}
                    	continue;
                    }
                	
                	// create and write newSite 
        			Site newSite = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, start, length, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
        			String trackNames = trackNamesSecond;
        			int frequency = 1;
                    for( int i = 0; i < trackNames.length(); i++ )
                    	if( trackNames.charAt(i) == ',')
                    		frequency++;
                    //double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks(trackFirst, trackSecond);
                    double rankAggregationScoreNew = (recalculateRAscore(0.0, rankAggregationScoreSecond, 2, someCharacteristicsOfTwoTracks)) / someCharacteristicsOfTwoTracks[3];
                    dps = newSite.getProperties();
                    dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(frequency)));
                    dps.add(new DynamicProperty("Additional_property", String.class, trackNames));
                    dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, Double.toString(rankAggregationScoreNew)));
                    resultedTrackTemporary.addSite(newSite);
                }
            }
        	log.info("O.K.2");
    		
    		// TODO:
        	log.info("nameOfResultedTrackTemporary = " + nameOfResultedTrackTemporary + "; its size, n = " + resultedTrackTemporary.getAllSites().getSize());
        	//
        	
        	// 4. Save temporary track.
        	resultedTrackTemporary.finalizeAddition();
            CollectionFactory.save(resultedTrackTemporary);
        	
//            // 5. Remove some duplicated sites and track 'nameOfResultedTrackTemporary'.
//            removeMarkedSites(nameOfResultedTrackTemporary, nameOfResultedTrack, pathToOutputFolder);
    	}
    	
    	// typeOfSiteOverlaps = 0 if siteFirst - yes and siteSecond - no; 
    	//						1 if siteFirst - yes and siteSecond - yes;
    	//						2 if siteFirst - no  and siteSecond - yes;
    	private static double recalculateRAscore(double rankAggregationScoreFirst, double rankAggregationScoreSecond, int typeOfSiteOverlaps, double[] someCharacteristicsOfTwoTracks)
    	{
    		double rankFirst = rankAggregationScoreFirst * someCharacteristicsOfTwoTracks[0];
    		double rankSecond = rankAggregationScoreSecond * someCharacteristicsOfTwoTracks[1];

    		switch( typeOfSiteOverlaps )
    		{
//    		 case 0 : return (rankFirst * someCharacteristicsOfTwoTracks[4] + 
//    				 rankSecond * someCharacteristicsOfTwoTracks[5])/ (someCharacteristicsOfTwoTracks[4] + someCharacteristicsOfTwoTracks[5]);
//    		 case 1 : double rank = 0.5 * (2.0 * someCharacteristicsOfTwoTracks[0] + someCharacteristicsOfTwoTracks[1] - someCharacteristicsOfTwoTracks[2] + 1.0);
//    		 		  return (rankFirst * someCharacteristicsOfTwoTracks[4] + rank * someCharacteristicsOfTwoTracks[5]) / (someCharacteristicsOfTwoTracks[4] + someCharacteristicsOfTwoTracks[5]);
//    		 case 2 : rank = 0.5 * (2.0 * someCharacteristicsOfTwoTracks[1] + someCharacteristicsOfTwoTracks[0] - someCharacteristicsOfTwoTracks[2] + 1.0);
//    		 		  return (rank * someCharacteristicsOfTwoTracks[4] + rankSecond * someCharacteristicsOfTwoTracks[5]) / (someCharacteristicsOfTwoTracks[4] + someCharacteristicsOfTwoTracks[5]);

    		 case 0 : double rank = 0.5 * (2.0 * someCharacteristicsOfTwoTracks[0] + someCharacteristicsOfTwoTracks[1] - someCharacteristicsOfTwoTracks[2] + 1.0);
    		 		  return (rankFirst * someCharacteristicsOfTwoTracks[4] + rank * someCharacteristicsOfTwoTracks[5]) / (someCharacteristicsOfTwoTracks[4] + someCharacteristicsOfTwoTracks[5]);
    		 case 1 : return (rankFirst * someCharacteristicsOfTwoTracks[4] + rankSecond * someCharacteristicsOfTwoTracks[5])/ (someCharacteristicsOfTwoTracks[4] + someCharacteristicsOfTwoTracks[5]);
    		 case 2 : rank = 0.5 * (2.0 * someCharacteristicsOfTwoTracks[1] + someCharacteristicsOfTwoTracks[0] - someCharacteristicsOfTwoTracks[2] + 1.0);
    		 		 return (rank * someCharacteristicsOfTwoTracks[4] + rankSecond * someCharacteristicsOfTwoTracks[5]) / (someCharacteristicsOfTwoTracks[4] + someCharacteristicsOfTwoTracks[5]);
    		}
    		return Double.NaN;
    	}

    	// old
//    	private static void compareTwoTracks(String nameOfFirstTrack, String nameOfSecondTrack, DataElementPath pathToFolder)
//    	{
//    		DataElementPath pathToFirstTrack = pathToFolder.getChildPath(nameOfFirstTrack); 
//    		DataElement de = pathToFirstTrack.getDataElement();
//    		Track trackFirst = (Track)de;
//
//    		DataElementPath pathToSecondTrack = pathToFolder.getChildPath(nameOfSecondTrack); 
//    		de = pathToSecondTrack.getDataElement();
//    		Track trackSecond = (Track)de;
//    		
//    		int n1 = trackFirst.getAllSites().getSize(), n2 = trackSecond.getAllSites().getSize(), n = 0;
//    		for( Site site : trackSecond.getAllSites() )
//            {
//                String chromosomeName = site.getSequence().getName();
//                Interval coordinates = site.getInterval();
//                DataCollection<Site> dc = trackFirst.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo());
//                if( ! dc.isEmpty() )
//                	n++;
//            }
//    		log.info("*** Comparison of two tracks ***");
//    		log.info("nameOfFirstTrack = " + nameOfFirstTrack +  ", n1 = " + n1 + "; nameOfSecondTrack = " + nameOfSecondTrack + ", n2 = " + n2 + " numberOfOvelapped = " + n);
//    	}
    	
    	// new
    	// TODO: To replace it by int[] frequecies = TrackUtils.getOverlapsFrequencies(pathToFirstTrack, pathToSecondTrack);
//    	private static int[] compareTwoTracks(DataElementPath pathToFirstTrack, DataElementPath pathToSecondTrack)
//    	{
//    		// 1. Initial determinations.
//    		Track trackFirst = (Track)pathToFirstTrack.getDataElement(), trackSecond = (Track)pathToSecondTrack.getDataElement();
//    		int sizeOverlapForFirst = 0, sizeOverlapForSecond = 0;
//
//    		// 2. Calculate sizeOverlapForFirst.
//    		log.info("*** Analisys of 1-st track ***");
//    		DataCollection<Site> allSites = trackFirst.getAllSites();
//    		int sizeFirst = allSites.getSize();
//    		for( Site site : allSites )
//            {
//                Interval coordinates = site.getInterval();
//                if( ! trackSecond.getSites(site.getSequence().getName(), coordinates.getFrom(), coordinates.getTo()).isEmpty() )
//                	sizeOverlapForFirst++;
//            }
//    		
//    		// 3. Calculate sizeOverlapForSecond.
//    		log.info("*** Analisys of 2-nd track ***");
//    		allSites = trackSecond.getAllSites();
//    		int sizeSecond = allSites.getSize();
//    		for( Site site : allSites )
//            {
//                Interval coordinates = site.getInterval();
//                if( ! trackFirst.getSites(site.getSequence().getName(), coordinates.getFrom(), coordinates.getTo()).isEmpty() )
//                	sizeOverlapForSecond++;
//            }
//    		return new int[]{sizeFirst, sizeSecond, sizeOverlapForFirst, sizeOverlapForSecond};
//    	}
    	
    	private static String[] identifyTrackNames(DataElementPath pathToTableWithSummaryOnGtrdPeaks, String tfName, boolean doWithoutTreatedExperiments, int numberOfAllTracksForAnalysis)
    	{
        	DataMatrixString dms = new DataMatrixString(pathToTableWithSummaryOnGtrdPeaks, new String[]{"TF-name", "is_cell_line_treated"}); 
        	String[] trackNames  = dms.getRowNames(), tfNames = dms.getColumn("TF-name"), areTreated = dms.getColumn("is_cell_line_treated");
        	DataMatrix dm = new DataMatrix(pathToTableWithSummaryOnGtrdPeaks, new String[]{"Number_of_sites_gem", "Number_of_sites_macs2", "Number_of_sites_pics", "Number_of_sites_sissrs"});

        	double[][] matrix = dm.getMatrix();
        	List<String> list = new ArrayList<>();
        	for( int i = 0; i < dms.getSize(); i++ )
        	{

        		////////////////////////// Temp
        		if( tfNames[i].equals(tfName) )
        			log.info("*** i = " + i + " tfNames[i] = " + tfNames[i] + " " + matrix[i][0] + " " + matrix[i][1] + " " + matrix[i][2] + " " + matrix[i][3] + " areTreated = " + areTreated[i]);
        		////////////////////////// Temp
        		
        		if( ! tfNames[i].equals(tfName) ) continue;
        		if( doWithoutTreatedExperiments )
        			if( areTreated[i] != null && ( areTreated[i].equals("Yes")) ) continue;
        		boolean doSelect = true;
            	for( int j = 0; j < matrix[0].length; j++ )
                	if( matrix[i][j] > 800000.0 || matrix[i][j] < 300.0 )
                	{
                		doSelect = false;
            			break;
                	}
            	if( doSelect == false ) continue;
            	list.add(trackNames[i]);
            	if( list.size() >= numberOfAllTracksForAnalysis + 2 ) break;
        	}
        	log.info("Number of experiments = " + list.size());
        	if( list.size() != numberOfAllTracksForAnalysis )
        	{
            	log.info("!!! Attention !!! number of selected experiments < numberOfAllTracksForAnalysis");
            	numberOfAllTracksForAnalysis = list.size();
        	}
        	return list.toArray(new String[0]);
    	}

    	private static void changeSiteLengths(String nameOfInitialTrack, String nameOfResultedTrack, int minimalLengthOfSite, int maximalLengthOfSite, DataElementPath pathToFolder)
    	{
    		Track trackInitial = (Track)pathToFolder.getChildPath(nameOfInitialTrack).getDataElement();
    		SqlTrack resultedTrack = SqlTrack.createTrack(pathToFolder.getChildPath(nameOfResultedTrack), null);
    		for( Site site : trackInitial.getAllSites() )
    		{
    			// 1. Read site in trackInitial.
    			DynamicPropertySet dps = site.getProperties();
    			String raScoreAsString = dps.getValueAsString(RankAggregation.RA_SCORE);
    			String frequencyAsString = dps.getValueAsString("Frequency");
    			String trackNames = dps.getValueAsString("Additional_property");
    		    String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();

    			// 2. Create and write new site into resulted track.
                coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    			int start = coordinates.getFrom(), end = coordinates.getTo(), length = end - start + 1;
    			
    			//StrandType.STRAND_NOT_KNOWN
    			Site newSite = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, start, length, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
    			dps = newSite.getProperties();
                dps.add(new DynamicProperty("Frequency", String.class, frequencyAsString));
                dps.add(new DynamicProperty("Additional_property", String.class, trackNames));
                dps.add(new DynamicProperty(RankAggregation.RA_SCORE, String.class, raScoreAsString));
            	resultedTrack.addSite(newSite);
    		}
    		resultedTrack.finalizeAddition();
            CollectionFactory.save(resultedTrack);
    	}

    	/**********************************************************************************************************************/
    	/**********************************************************************************************************************/
    	/**********************************************************************************************************************/

    	public static void recalculateRAscore_Test()
    	{
        	DataElementPath pathToFirstTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment/Metara_26");
        	DataElementPath pathToSecondTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment/MetaClusters_step_27");
    		Track trackFirst = (Track)pathToFirstTrack.getDataElement();
    		Track trackSecond = (Track)pathToSecondTrack.getDataElement();
    		//double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks(trackFirst, trackSecond);
    		double[] someCharacteristicsOfTwoTracks = getSomeCharacteristicsOfTwoTracks_For_TEST(trackFirst, trackSecond);
        	log.info(" someCharacteristicsOfTwoTracks : ");
        	for( int i = 0; i < someCharacteristicsOfTwoTracks.length; i++ )
            	log.info("i = " + i + " someCharacteristicsOfTwoTracks[i] = " + someCharacteristicsOfTwoTracks[i]);

        	double rankAggregationScoreFirst = 0.0;
    		double rankAggregationScoreSecond = 0.691062929666842;
    		int typeOfSiteOverlaps = 2;
    		
    		double rankFirst = rankAggregationScoreFirst * someCharacteristicsOfTwoTracks[0];
    		double rankSecond = rankAggregationScoreSecond * someCharacteristicsOfTwoTracks[1];
    		double rank = 0.5 * (2.0 * someCharacteristicsOfTwoTracks[1] + someCharacteristicsOfTwoTracks[0] - someCharacteristicsOfTwoTracks[2] + 1.0);
    		double raScore = (rank * someCharacteristicsOfTwoTracks[4] + rankSecond * someCharacteristicsOfTwoTracks[5]) / (someCharacteristicsOfTwoTracks[4] + someCharacteristicsOfTwoTracks[5]);
        	log.info("rank = " + rank + " raScore = " + raScore);
    	}
    	
    	//////////////////////////// For Test /////////////////////
    	private static double[] getSomeCharacteristicsOfTwoTracks_For_TEST(Track trackFirst, Track trackSecond)
    	{
    		int sizeFirst = trackFirst.getAllSites().getSize(), sizeSecond = trackSecond.getAllSites().getSize(), sizeOverlap = 0;
    		for ( Site site : trackFirst.getAllSites() )
    		{
    			String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();
    			int start = coordinates.getFrom(), end = coordinates.getTo();
            	DataCollection<Site> dc = trackSecond.getSites(chromosomeName, start, end);
            	if( ! dc.isEmpty() )
            	{
            		sizeOverlap++;
                	log.info("sizeOverlap = " + sizeOverlap + " chromosomeName = " + chromosomeName + " start = " + start + " end = " + end);
                	for( Site siteFromTrack2 : dc )
                	{
                		String chromosomeName1 = siteFromTrack2.getSequence().getName();
                        Interval coordinates1 = siteFromTrack2.getInterval();
            			int start1 = coordinates1.getFrom(), end1 = coordinates1.getTo();
                		log.info("siteFromTrack2 : " + sizeOverlap + " chromosomeName1 = " + chromosomeName1
                    			+ " start1 = " + start1 + " end1 = " + end1);
                	}
            	}
    		}
    		int sizeUnion = sizeFirst + sizeSecond - sizeOverlap;
    		int n = getNumberOfDistinctTrackNames(trackFirst), k = getNumberOfDistinctTrackNames(trackSecond); 
    		return new double[]{(double)sizeFirst, (double)sizeSecond, (double)sizeOverlap, (double)sizeUnion, (double)n, (double)k};
    	}


    	public static void compareRAscoresInTwoTracks()
    	{
        	DataElementPath pathToFirstTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment/Metara_27");
        	DataElementPath pathToSecondTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment/Meta_clusters_for_all_peaks");
        	compareRAscoresInTwoTracks(pathToFirstTrack, pathToSecondTrack);
    	}

    	
    	// ????
    	private static void compareRAscoresInTwoTracks(DataElementPath pathToFirstTrack, DataElementPath pathToSecondTrack)
    	{
    		Track trackFirst = (Track)pathToFirstTrack.getDataElement(), trackSecond = (Track)pathToSecondTrack.getDataElement();
    		List<double[]> list = new ArrayList<>();
    		
    		for( Site site : trackFirst.getAllSites() )
            {
                String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();
                DataCollection<Site> dc = trackSecond.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo());
                if( dc.isEmpty() ) continue;
                
                //
                DynamicPropertySet dps = site.getProperties();
    			String raScoreAsString = dps.getValueAsString(RankAggregation.RA_SCORE), frequencyAsString = dps.getValueAsString("Frequency");
    			double rankAggregationScoreFirst = Double.parseDouble(raScoreAsString), frequencyFirst = Double.parseDouble(frequencyAsString);
        		for( Site siteSecond : dc )
        		{
                    dps = siteSecond.getProperties();
        			raScoreAsString = dps.getValueAsString(RankAggregation.RA_SCORE);
        			frequencyAsString = dps.getValueAsString("Frequency");
        			double rankAggregationScoreSecond = Double.parseDouble(raScoreAsString), frequencySecond = Double.parseDouble(frequencyAsString);
        			list.add(new double[]{rankAggregationScoreFirst, rankAggregationScoreSecond});
        			break;
        		}
            }
    		double[][] array = list.toArray(new double[list.size()][]);
    		double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(MatrixUtils.getColumn(array, 0), MatrixUtils.getColumn(array, 1));
        	log.info("n1 = " + trackFirst.getAllSites().getSize() + " n2 = " + trackSecond.getAllSites().getSize());
        	log.info("size of overlap, n = " + array.length + " corr = " + corr);
    	}

    	// 
        public static void performComparison()
        {
//            String[] samplesNames = new String[]{"Old meta-clusters", "Less reliable meta-clusters", "Most reliable meta-clusters"};
//            double[][] samples = new double[][]{lenthgsOld, lenthgsLarge, lenthgsSmall};
//            UnivariateSamples us = new UnivariateSamples(samplesNames, samples);
//            DataMatrix dm = us.getSimpleCharacteristicsOfSamples();
//            log.info("chracteristics = " + dm.toString());
//            String name = pathToNewMetaTrack.getName();
//            dm.writeDataMatrix(false, pathToOutputFolder, name + "_characteristics", log);
//            Chart chart = us.createChartWithSmoothedDensities("Length of meta-clusters", true, DensityEstimation.WINDOW_WIDTH_01, null);
//            TableAndFileUtils.addChartToTable("chart with densities", chart, pathToOutputFolder.getChildPath("_chart_with_densities_" + name));
//            Homogeneity homogeneity = new Homogeneity(us);
//            homogeneity.performPairwiseComparisonOfSamples(null, pathToOutputFolder, name + "_pairwise_comparison");
        }
        
    	private static void comparisonOfRaScoresAndFrequenciesOfMetaClusters(DataElementPath pathToTrackWithMetaClusters, String nameOfChart, DataElementPath pathToOutputFolder)
    	{
    		int subsampleSize = 100000;
    		
    		// 1. Calculate dataMatrix.
    		Track track = (Track)pathToTrackWithMetaClusters.getDataElement();
    		DataMatrix dataMatrix = SiteUtils.getProperties(track, new String[]{RankAggregation.RA_SCORE, "Frequency"}), dataSubMatrix = null;
    		int n = dataMatrix.getSize();
    		
    		// 2. Calculate dataSubMatrix.
    		if( n <= subsampleSize )
    			dataSubMatrix = dataMatrix;
    		else
    		{
            	int[] randomArray = RandomUtils.selectIndicesRandomly(n, subsampleSize, 0);
        		boolean[] doIncludeRow = UtilsForArray.getConstantArray(n, false);
        		for( int i = 0; i < randomArray.length; i++ )
        			doIncludeRow[randomArray[i]] = true;
        		dataSubMatrix = dataMatrix.getSubDataMatrixRowWise(doIncludeRow);
    		}
    		
    		// 3. Result creation ant output.
    		double[] raScores = dataSubMatrix.getColumn(RankAggregation.RA_SCORE), frequencies = dataSubMatrix.getColumn("Frequency");
            double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(raScores, frequencies);
            log.info(nameOfChart + " corr = " + corr);
    		Chart chart = OrdinaryLeastSquaresRegressionModel.createChartForSimpleOlsRegression(RankAggregation.RA_SCORE, raScores, "Frequency", frequencies);
    		TableAndFileUtils.addChartToTable("_chart_correlation", chart, pathToOutputFolder.getChildPath("_" + nameOfChart));
    	}
        
    	public static void performNormalMixtureAnalysisOfRAscores()
    	{
    		DataElementPath[] pathToTracks = new DataElementPath[]
    				{
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_no_treatment_/Metara_23_merged_"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/ESR1_yes_treatment_/Metara_20_merged_"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment_/Metara_25_merged_"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_yes_treatment/Metara_27_merged_"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Metara_12_merged_"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_yes_treatment/Metara_3_merged_"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_no_treatment_/Metara_27_merged_"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Metara_5_merged_")
    	    		};
    		DataElementPath[] pathToOutputFolders = new DataElementPath[]
    	    		{
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/ESR1_no_treatment"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/ESR1_yes_treatment"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/FOXA1_no_treatment"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/FOXA1_yes_treatment"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/JUND_no_treatment"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/JUND_yes_treatment"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/REST_no_treatment"),
    	    			DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_2/REST_yes_treatment")
    	    		};

    		int numberOfComponents = 3;
    		for( int i = 0; i < pathToTracks.length; i++ )
    		{
                log.info("*** i = " + i + " Input track = " + pathToTracks[i].toString());
                performNormalMixtureAnalysisOfRAscores(numberOfComponents, pathToTracks[i], pathToOutputFolders[i]);
    		}

    		// TEMP!!!
//    		DataElementPath path1 = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/REST_yes_treatment_/Metara_5");
//    		DataElementPath path2 = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans03_normal_mixture_of_RA_scores_/REST_yes_treatment_4_components");
//    		int numberOfComponents = 4;
//          performNormalMixtureAnalysisOfRAscores(numberOfComponents, path1, path2);
    	}

    	private static void performNormalMixtureAnalysisOfRAscores(int numberOfComponents, DataElementPath pathToTrackWithMetaClusters, DataElementPath pathToOutputFolder)
    	{
    		//DataElementPath pathToTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/FOXA1_no_treatment/Metara_10");
    		int subsampleSize = 50000;
    		int maximalNumberOfIterations = 30;
    		
    		Track track = (Track)pathToTrackWithMetaClusters.getDataElement();
    		String[] propertiesNames = new String[]{RankAggregation.RA_SCORE};
    		DataMatrix dataMatrix = SiteUtils.getProperties(track, propertiesNames);
    		double[] raScores = dataMatrix.getColumn(RankAggregation.RA_SCORE);
            log.info("size of raScores = " + raScores.length);
            double[] raScoresSubsample = raScores.length <= subsampleSize ? raScores : RandomUtils.selectSubsampleRandomly(raScores, subsampleSize);
            NormalMixture normalMixture = new NormalMixture(raScoresSubsample, numberOfComponents, null,null, maximalNumberOfIterations);
            normalMixture.getResults(RankAggregation.RA_SCORE, pathToOutputFolder);
            log.info("normal mixture : O.K.");
    	}
    	
    	public static void getSummaryOnMetaraSteps(DataElementPath pathToFolder)
    	{
    		// 1. Calculate numbet of METARA steps.
    		int nMetaraSteps = 0;
        	for( int i = 1; i < Integer.MAX_VALUE; i++ )
        	{
        		DataElementPath pathToTrack = pathToFolder.getChildPath("Metara_" + i);
        		if( ! pathToTrack.exists() ) break;
        		else nMetaraSteps = i;
        	}
        	nMetaraSteps++;
        	log.info("number of METARA steps = " + nMetaraSteps);

        	// 2. Calculate and write data matrix for summary.
        	double[][] matrix = new double[nMetaraSteps][];
        	String[] rowNames = new String[nMetaraSteps];
        	String[] columnNames = new String[]{"IMETARA step", "Number of meta-clusters identified in given step", "Number of meta-clusters after given step", "Percentage of new meta-clusters after given step"};
        	for( int i = 0; i < nMetaraSteps; i++ )
        	{
    			rowNames[i] = "Meta_clusters_step_" + i;
        		DataElementPath pathToTrack = pathToFolder.getChildPath("MetaClusters_step_" + i);
        		Track track = (Track)pathToTrack.getDataElement();
        		int numberOfMetaClustersIdentifiedInGivenStep = track.getAllSites().getSize();
        		if( i == 0 )
        		{
        			matrix[i] = new double[]{(double)i, (double)numberOfMetaClustersIdentifiedInGivenStep, (double)numberOfMetaClustersIdentifiedInGivenStep, 100.0};
        			continue;
        		}
        		pathToTrack = pathToFolder.getChildPath("Metara_" + i);
        		track = (Track)pathToTrack.getDataElement();
        		int numberOfMetaClustersAfterGivenStep = track.getAllSites().getSize();
        		double persentageOfNewMetaClustersAfterGivenStep = 100.0 * ((double)numberOfMetaClustersAfterGivenStep - matrix[i - 1][2]) / (double)numberOfMetaClustersIdentifiedInGivenStep; 
    			matrix[i] = new double[]{(double)i, (double)numberOfMetaClustersIdentifiedInGivenStep, (double)numberOfMetaClustersAfterGivenStep, persentageOfNewMetaClustersAfterGivenStep};
        	}
        	log.info("Calculation of summary : O.K");

        	DataMatrix dmSummary = new DataMatrix(rowNames, columnNames, matrix);
        	dmSummary.writeDataMatrix(false, pathToFolder, "summary_on_tracks", log);
    	}

//    	public static void implementImetara(DataElementPath pathToOutputFolder)
//        {
//        	String cistromType = CistromConstructor.OPTION_02;
//        	String combinedPeakType = CombinedSites.SITE_TYPE_OVERLAPPED;
//        	String rankAggregationMethod = RankAggregation.METHOD_AR_MEAN;
//        	double fpcmThreshold = 3.0;
//        	int siteNumberThreshold = 2000;
//        	int maximalLengthOfPeaks = 1000000;
//        	int minimalLengthOfPeaks = 20;
//        	
//        	DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD_20.06/Data/peaks");
//        	String[] foldersNames = new String[]{"gem", "macs2", "pics", "sissrs"};
//
//        	// for JunD
////        	String[][] trackNamesForEverySteps = new String [][]{new String []{"PEAKS033143", "PEAKS033233"},
////				     							 new String []{"PEAKS033690", "PEAKS033791"},
////				     							 new String []{"PEAKS033873"},
////				     							 new String []{"PEAKS033890"},
////				     							 new String []{"PEAKS033899"},
////				     							 new String []{"PEAKS034552"},
////				     							 new String []{"PEAKS041939"}};
//
//        	// for STAT1
////        	String[][] trackNamesForEverySteps = new String [][]{new String []{"PEAKS033050", "PEAKS033076"},
////					 new String []{"PEAKS033077", "PEAKS033308"},
////					 new String []{"PEAKS033724"},
////					 new String []{"PEAKS034603"},
////					 new String []{"PEAKS033608"},
////					 new String []{"PEAKS034609"},
////					 new String []{"PEAKS033693"}};
//
//        	// for FOXA1
//        	String[][] trackNamesForEverySteps = new String [][]{new String []{"PEAKS033251", "PEAKS033452"},
//					 new String []{"PEAKS033793", "PEAKS035075"},
//					 new String []{"PEAKS037202"},
//					 new String []{"PEAKS037729"},
//					 new String []{"PEAKS038448"},
//					 new String []{"PEAKS039282"},
//					 new String []{"PEAKS041789"}};
//        	
//        	// 1. Calculate meta-clusters for all tracks available for METARA.
//        	String[] allTrackNames = trackNamesForEverySteps[0];
//        	for( int i = 1; i < trackNamesForEverySteps.length; i++ )
//            	allTrackNames = (String[]) ArrayUtils.addAll(allTrackNames, trackNamesForEverySteps[i]);
//        	String trackNameOfMetaClustersForAllPeaks = "Meta_clusters_for_all_peaks";
//        	new CistromConstructor(cistromType, combinedPeakType, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, allTrackNames, trackNameOfMetaClustersForAllPeaks, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
//            DataElementPath pathToTrack = pathToOutputFolder.getChildPath(trackNameOfMetaClustersForAllPeaks);
//        	Track track = pathToTrack.getDataElement(Track.class);
//        	log.info("size of track with metaclusters for all available tracks = " + track.getAllSites().getSize());
//        	
//        	// 2.Implement METARA for each step of IMETARA.
//        	for( int i = 0; i < trackNamesForEverySteps.length; i++ )
//        	{
//            	log.info("METARA in i = " + Integer.toString(i) + " step");
//        		String name = "MetaClusters_step_" + Integer.toString(i);
//            	new CistromConstructor(cistromType, combinedPeakType, rankAggregationMethod, fpcmThreshold, siteNumberThreshold, pathToFolderWithFolders, foldersNames, trackNamesForEverySteps[i], name, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
//        	}
//        	
//        	// 3.Implement IMETARA.
//        	for( int i = 1; i < trackNamesForEverySteps.length; i++ )
//    		{
//            	log.info("*** IMETARA *** : step = " + i);
//        		String nameOfOldTrack =  i == 1 ? "MetaClusters_step_0" : "Metara_" + Integer.toString(i - 1);
//        		String nameOfAdditionalSitesTrack = "MetaClusters_step_" + Integer.toString(i);
//        		String nameOfResultedTrack = "Metara_" + Integer.toString(i);
//        		
//        		// TODO:
//            	log.info("i = " + i + " nameOfOldTrack = " + nameOfOldTrack +  " nameOfAdditionalSitesTrack = " + nameOfAdditionalSitesTrack + " nameOfResultedTrack = " + nameOfResultedTrack);
//            	//
//
//        		implementOneStepOfImetaraTwo(nameOfOldTrack, nameOfAdditionalSitesTrack, nameOfResultedTrack, minimalLengthOfPeaks, maximalLengthOfPeaks, pathToOutputFolder);
//    		}
//        	
//        	// 3. Compare two tracks
//        	String nameOld = "Meta_clusters_for_all_peaks", nameNew = "Metara_" + Integer.toString(trackNamesForEverySteps.length - 1);
//        	compareTwoTracks(nameOld, nameNew, pathToOutputFolder);
//        }
    	
    	public static void getChartWithPeaksFrequenciesInChromosomeFragmentPositions(DataElementPath pathToOutputFolder)
    	{
        	DataElementPath pathToFolderWithFolders = DataElementPath.create("databases/GTRD_20.06/Data/peaks");
        	DataElementPath pathToTableWithSummaryOnGtrdPeaks = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/summary_on_chip_seq_tracks");
        	
        	String[] foldersNames = new String[]{"gem", "macs2", "pics", "sissrs"};
        	String chromosomeName = "22";
        	int startInChromosome = 17050000, endInChromosome = 17100000;
        	String tfName = "REST"; // "ESR1", "JUND", "FOXA1", "STAT1"
        	boolean doRemoveTreatedExperiments = true;
        	int numberOfAllTracksForAnalysis = 30; //numberOfAllTracksForAnalysis = 8000; numberOfAllTracksForAnalysis = 30 // number of tracks for selection; it can be changed!!!.
        	
        	// 1. Calculate frequencies.
        	String[] trackNames = identifyTrackNames(pathToTableWithSummaryOnGtrdPeaks, tfName, doRemoveTreatedExperiments, numberOfAllTracksForAnalysis);
        	int length = endInChromosome - startInChromosome + 1;
        	double[][] frequencies = new double[foldersNames.length][];
        	for( int i = 0; i < foldersNames.length; i++ )
        	{
    			log.info("i = " + i + " folderName = " + foldersNames[i]);
            	// 1. Calculate frequencies.
        		frequencies[i] = UtilsForArray.getConstantArray(length, 0.0);
            	DataElementPath pathToFolder = pathToFolderWithFolders.getChildPath(foldersNames[i]);
            	for( int j = 0; j < trackNames.length; j++ )
            	{
            		DataElementPath pathToTrack = pathToFolder.getChildPath(trackNames[j]);
            		Track track = (Track)pathToTrack.getDataElement();
            		DataCollection<Site> sites = track.getSites(chromosomeName, startInChromosome, endInChromosome);
            		for( Site site : sites )
            		{
            			int fromInSite = site.getFrom(), toInSite = site.getTo();
            			int start = Math.max(fromInSite, startInChromosome), end = Math.min(toInSite, endInChromosome);
                    	for( int jj = start; jj <= end; jj++ )
                    		frequencies[i][jj - startInChromosome] += 1.0;
                    	
                    	//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! temp
                		// temp : test
                		//****//
//            			log.info("i = " + i + " foldersName = " + foldersNames[i] + " trackNames = " + trackNames[j] + " number_of_peaks = " + sites.getSize() +
//            					 " fromInSite = " + fromInSite + " toInSite = " + toInSite);
                		//**//
                    	//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! temp
            			
            		}
            	}
        	}
			log.info("O.K.1");
        	
        	// 2. Create and write chart.
        	double[] xValues = new double[length];
        	for( int i = startInChromosome; i <= endInChromosome; i++ )
        		xValues[i - startInChromosome] = i;
        	double[][] xValuesForCurves = new double[foldersNames.length][];
        	for( int i = 0; i < foldersNames.length; i++ )
        		xValuesForCurves[i] = xValues;
        	Chart chart = ChartUtils.createChart(xValuesForCurves, frequencies, foldersNames, null, null, null, new double[]{(double)startInChromosome, (double)endInChromosome, 0.0, (double)(trackNames.length + 1)}, "Chromosome positions", "Frequencies", false);
            TableAndFileUtils.addChartToTable("chart with frequencies", chart, pathToOutputFolder.getChildPath("_chart_with_frequencies"));
			log.info("O.K.2");
    	}
    	
    	// Remark: numberOfAllTracksForAnalysis can be < dim(trackNmaes).
    	private static String[][] getTrackNamesForEverySteps(int numberOfAllTracksForAnalysis, String[] trackNames, int typeOfStructureOfOutputArray)
    	{
    		switch( typeOfStructureOfOutputArray )
    		{
    		 case 0  : String[][] result = new String [numberOfAllTracksForAnalysis - 2][];
    		 		   result[0] = new String[]{trackNames[0], trackNames[1]};
    		 		   result[1] = new String[]{trackNames[2], trackNames[3]};
    		 		   for( int i = 2; i < numberOfAllTracksForAnalysis - 2; i++ )
    		 			   result[i] = new String[]{trackNames[i + 2]};
    		 		   return result;
    		 case 1  : int dim =  numberOfAllTracksForAnalysis / 2;
    		 		   result = new String[numberOfAllTracksForAnalysis - dim + 1][];
    		 		   result[0] = (String[])ArrayUtils.subarray(trackNames, 0, dim);
    		 		   for( int i = 0; i < numberOfAllTracksForAnalysis - dim; i++ )
    		 			   result[1 + i] = new String[]{trackNames[dim + i]};
    		 		   return result;
  		 	 default : return null;
    		}
    	}
    	
    	private static double[] getSomeCharacteristicsOfTwoTracks(Track trackFirst, Track trackSecond)
    	{
    		int sizeFirst = trackFirst.getAllSites().getSize(), sizeSecond = trackSecond.getAllSites().getSize(), sizeOverlap = 0;
    		for ( Site site : trackFirst.getAllSites() )
    		{
    			String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();
    			int start = coordinates.getFrom(), end = coordinates.getTo();
            	DataCollection<Site> dc = trackSecond.getSites(chromosomeName, start, end);
            	if( ! dc.isEmpty() )
            		sizeOverlap++;
    		}
    		int sizeUnion = sizeFirst + sizeSecond - sizeOverlap;
    		int n = getNumberOfDistinctTrackNames(trackFirst), k = getNumberOfDistinctTrackNames(trackSecond); 
    		return new double[]{(double)sizeFirst, (double)sizeSecond, (double)sizeOverlap, (double)sizeUnion, (double)n, (double)k};
    	}
    	
    	private static String[] getDistinctTrackNames(Track track)
    	{
    		Set<String> distinctTrackNames = new HashSet<>();
    		for ( Site site : track.getAllSites() )
    		{
    			DynamicPropertySet dps = site.getProperties();
    			String trackNames = dps.getValueAsString("Additional_property");
    			String[] trackNamesAsArray = trackNames.split(",");
    			for( String s : trackNamesAsArray )
    				distinctTrackNames.add(s);
    		}
    		return distinctTrackNames.toArray(new String[0]);
    	}

    	// old
//    	private static int getNumberOfDistinctTrackNames(Track track)
//    	{
//    		Set<String> distinctTrackNames = new HashSet<>();
//    		for ( Site site : track.getAllSites() )
//    		{
//    			DynamicPropertySet dps = site.getProperties();
//    			String trackNames = dps.getValueAsString("Additional_property");
//    			String[] trackNamesAsArray = trackNames.split(",");
//    			for( String s : trackNamesAsArray )
//    				distinctTrackNames.add(s);
//    		}
//    		return distinctTrackNames.size();
//		}
    		
    	// new
    	private static int getNumberOfDistinctTrackNames(Track track)
    	{
    		return getDistinctTrackNames(track).length;
    	}
    	
    	/* -------------------*/
    	
    	// new version No 2 of implementOneStepOfImetara()
    	private static void implementOneStepOfImetaraTwo(String nameOfOldTrack, String nameOfAdditionalSitesTrack, String nameOfResultedTrack, int minimalLengthOfSite, int maximalLengthOfSite, DataElementPath pathToOutputFolder)
    	{
    		// 1. Open 2 input tracks and resulted track.
    		DataElementPath pathToFirstTrack = pathToOutputFolder.getChildPath(nameOfOldTrack); 
    		Track trackFirst = (Track)pathToFirstTrack.getDataElement();
    		DataElementPath pathToSecondTrack = pathToOutputFolder.getChildPath(nameOfAdditionalSitesTrack); 
    		Track trackSecond = (Track)pathToSecondTrack.getDataElement();

    		String nameOfResultedTrackTemporary = nameOfResultedTrack + "_temporary";
    		DataElementPath pathToResultedTrackTemporary = pathToOutputFolder.getChildPath(nameOfResultedTrackTemporary); 
    		SqlTrack resultedTrackTemporary = SqlTrack.createTrack(pathToResultedTrackTemporary, null);
    		
    		// TODO: temp
        	log.info("nameOfOldTrack = " + nameOfOldTrack + " nameOfAdditionalSitesTrack = " + nameOfAdditionalSitesTrack + " nameOfResultedTrack = " + nameOfResultedTrack);
        	log.info("trackFirst = " + trackFirst + " n = " + trackFirst.getAllSites().getSize() + " trackSecond = " + trackSecond + " n = " + trackSecond.getAllSites().getSize());
        	//

        	// 2. Analysis of sites from 1-st track.
    		for( Site site : trackFirst.getAllSites() )
            {
    			// 2.1 Calculate rankAggregationScoreFirst and trackNamesFirst.
                String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();
                coordinates = SiteUtils.changeInterval(coordinates, minimalLengthOfSite, maximalLengthOfSite);
    			DynamicPropertySet dpsFirst = site.getProperties();
    			String string = dpsFirst.getValueAsString(RankAggregation.RA_SCORE);
    			double rankAggregationScoreFirst = string != null ? Double.parseDouble(string) : Double.NaN;
    			String trackNamesFirst = dpsFirst.getValueAsString("Additional_property");

    			// 2.2. Add sites that are in : 1-st track - yes and 2-nd track - no
                DataCollection<Site> dc = trackSecond.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo());
                if( dc.isEmpty() )
                {
                	// TODO: To recalculate RA-scores !!!!
                	resultedTrackTemporary.addSite(site);
                	continue;
                }
                
    			// 2.3. Add sites that are in : 1-st track - yes and 2-nd track - yes
        		for( Site siteAdditional : dc )
        		{
        			Interval coordinatesAdditional = siteAdditional.getInterval();
        			// coordinatesAdditional = SiteUtils.changeInterval(coordinatesAdditional, minimalLengthOfSite, maximalLengthOfSite);
        			DynamicPropertySet dps = siteAdditional.getProperties();
        			string = dps.getValueAsString(RankAggregation.RA_SCORE);
        			double rankAggregationScoreSecond = string != null ? Double.parseDouble(string) : Double.NaN;
        			String trackNamesSecond = dps.getValueAsString("Additional_property");

        			// TODO: To recalculate RA-scores !!!!

        			// 2.3.1. Add sites that are in : 1-st track - yes and 2-nd track - yes
        			Interval coordinatesNew = coordinates.intersect(coordinatesAdditional);
        			coordinatesNew = SiteUtils.changeInterval(coordinatesNew, minimalLengthOfSite, maximalLengthOfSite);
        			int start = coordinatesNew.getFrom(), end = coordinatesNew.getTo(), length = end - start + 1;
        			Site newSite = new SiteImpl(null, chromosomeName, null, Site.BASIS_USER_ANNOTATED, start, length, Site.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, null, null);
        			String trackNames = trackNamesFirst + "," + trackNamesSecond;
        			int frequency = 1;
                    for( int i = 0; i < trackNames.length(); i++ )
                    {
                    	char charInTrackNames = trackNames.charAt(i);
                    	if( charInTrackNames == ',')
                    		frequency++;
                    }
                    dps.add(new DynamicProperty("Frequency", String.class, Integer.toString(frequency)));
                    dps.add(new DynamicProperty("Additional_property", String.class, trackNames));
                    resultedTrackTemporary.addSite(newSite);
                    dpsFirst.add(new DynamicProperty("Do_remove", String.class, "Yes"));
        		}
            }

    		// 3. Add sites that are in : 1-st track - no and 2-nd track - yes
    		for( Site site : trackSecond.getAllSites() )
            {
                String chromosomeName = site.getSequence().getName();
                Interval coordinates = site.getInterval();
                DataCollection<Site> dc = resultedTrackTemporary.getSites(chromosomeName, coordinates.getFrom(), coordinates.getTo());
                if( dc.isEmpty() )
                	resultedTrackTemporary.addSite(site);
                else if( getSitesAfterRemoving(dc).isEmpty() )
        			resultedTrackTemporary.addSite(site);
            }
        	
        	// 4. Save temporary track.
        	resultedTrackTemporary.finalizeAddition();
            CollectionFactory.save(resultedTrackTemporary);
            
            // 5. Remove some duplicated sites and track 'nameOfResultedTrackTemporary'.
            removeMarkedSites(nameOfResultedTrackTemporary, nameOfResultedTrack, pathToOutputFolder);
    	}


    	private static void removeMarkedSites(String nameOfInputTrack, String nameOfOutputTrack, DataElementPath pathToFolder)
    	{
    		Track trackInput = (Track)pathToFolder.getChildPath(nameOfInputTrack).getDataElement();
    		SqlTrack outputTrack = SqlTrack.createTrack(pathToFolder.getChildPath(nameOfOutputTrack), null);
    		for( Site site : trackInput.getAllSites() )
    		{
    			DynamicPropertySet dps = site.getProperties();
    			String s = dps.getValueAsString("Do_remove");
    			if( s != null && ! s.equals("Yes") )
    				outputTrack.addSite(site);
    		}
    		outputTrack.finalizeAddition();
            CollectionFactory.save(outputTrack);
        	log.info("--- remove marked sites ---");
        	log.info("--- nameOfInputTrack = " + nameOfInputTrack + " InputTrack size = " + trackInput.getAllSites().getSize() + " nameOfOutputTrack = " + nameOfOutputTrack + " OutputTrack size = " + outputTrack.getAllSites().getSize() + " ---");
    	}

    	private static List<Site> getSitesAfterRemoving(DataCollection<Site> dataCollection)
    	{
    		List<Site> list = new ArrayList<>();
    		for( Site site : dataCollection )
    		{
    			DynamicPropertySet dps = site.getProperties();
    			String s = dps.getValueAsString("Do_remove");
    			if( s != null && ! s.equals("Yes") )
    				list.add(site);
    		}
    		return list;
    	}
    }
    
    /*****************************************************************************/
    /******************** ThirdArticleOnCistrom: end *****************************/
    /*****************************************************************************/
    
    
    
    /*****************************************************************************************/
    /************************************ Reviewers Intensity paper **************************/
    /*****************************************************************************************/
    
    public static void predictDifferesialExpression()
    {
        ru.biosoft.access.core.DataElementPath[] pathToMatrices = new ru.biosoft.access.core.DataElementPath[]{
                DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended"),
                DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/K562_with_QC_3.0_macs2_overlap/data_matrix_extended"),
                DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HEK293_with_QC_3.0_macs2_overlap/data_matrix_extended_2")
        };
        
        ru.biosoft.access.core.DataElementPath[] pathToModels = new ru.biosoft.access.core.DataElementPath[]{
                DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/HepG2"),
                DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/K562"),
                DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer03/Ans02_prediction_with_predicted_mean/HEK293")
        };
        String[] columnNamesForExpressions = new String[]{"Cell_1424_lg","Cell_1327", "Cell_1356"};
        String[] cellLineNames = new String[]{"HepG2","K562", "HEK293"};
        
        double[][] responses = new double[pathToModels.length][], predictions = new double[pathToModels.length][];
        for( int i = 0; i < pathToModels.length; i++ )
        {
            log.info("i = " + cellLineNames[i]);
            DataMatrix dm = new DataMatrix(pathToMatrices[i], new String[]{columnNamesForExpressions[i]});
            responses[i] = dm.getColumn(0);
            log.info("i = " + cellLineNames[i] + " responses : O.K.");
            predictions[i] = readModelAndPredictlExpression(pathToMatrices[i], pathToModels[i]);
            log.info("i = " + cellLineNames[i] + " predictions : O.K.");

        }
        
        for( int i = 0; i < pathToModels.length; i++ )
            for( int j = 0; j < pathToModels.length; j++ )
                if( i != j )
                    treatDifferences(cellLineNames[i], cellLineNames[j], responses[i], responses[j], predictions[i], predictions[j]);

    }
    
    private static void treatDifferences(String cellLine1, String cellLine2, double[] response1, double[] response2, double[] prediction1, double[] prediction2)
    {
        double[] diffResp = VectorOperations.getSubtractionOfVectors(response1, response2);
        double[] diffPred = VectorOperations.getSubtractionOfVectors(prediction1, prediction2);
        double corr = SimilaritiesAndDissimilarities.getPearsonCorrelation(diffResp, diffPred);
        log.info("cellLine1 = " + cellLine1 + " cellLine2 = " + cellLine2 + " corr = " + corr);
    }
    
    private static double[] readModelAndPredictlExpression(DataElementPath pathToDataMatrix, DataElementPath pathToModel)
    {
        RegressionModel regressionModel = RegressionModel.loadModel(pathToModel);
        String[] variableNames = regressionModel.getVariableNames();
        int interceptIndex =  ArrayUtils.indexOf(variableNames, ModelUtils.INTERCEPT);
        String[] names = interceptIndex < 0 ? variableNames : (String[])ArrayUtils.remove(variableNames, interceptIndex);
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, names);
        if( interceptIndex >= 0 )
            dataMatrix.addColumn(ModelUtils.INTERCEPT, UtilsForArray.getConstantArray(dataMatrix.getSize(), 1.0), interceptIndex);
        dataMatrix.removeRowsWithMissingData();
        return regressionModel.predict(dataMatrix);
    }
    
    public static void constructDmsForDifferentialExpression(DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToFileWithHepg2 = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap/data_matrix_extended");
        DataElementPath pathToFileWithK562 = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/K562_with_QC_3.0_macs2_overlap/data_matrix_extended");
        DataElementPath pathToFileWithHek = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HEK293_with_QC_3.0_macs2_overlap/data_matrix_extended_2");
        String columnNameForExpressionHepg2 = "Cell_1424_lg";
        String columnNameForExpressionK562 = "Cell_1327";
        String columnNameForExpressionHek293 = "Cell_1356";
        DataMatrix dm = new DataMatrix(pathToFileWithHepg2, new String[]{columnNameForExpressionHepg2});
        double[] expressionHepG2 = dm.getColumn(0);
        dm = new DataMatrix(pathToFileWithK562, new String[]{columnNameForExpressionK562});
        double[] expressionK562 = dm.getColumn(0);
        dm = new DataMatrix(pathToFileWithHek, new String[]{columnNameForExpressionHek293});
        double[] expressionHek293 = dm.getColumn(0);
        log.info("O.K.1");
        
        constructDmsForDifferentialExpression(pathToFileWithK562, columnNameForExpressionK562, expressionHepG2, expressionHek293, "HepG2", "HEK293", pathToOutputFolder, "data_matrix_K562_");
        log.info("O.K.2");
        constructDmsForDifferentialExpression(pathToFileWithHek, columnNameForExpressionHek293, expressionHepG2, expressionK562, "HepG2", "K562", pathToOutputFolder, "data_matrix_HEK293_");
        log.info("O.K.3");
    }
    
    private static void constructDmsForDifferentialExpression(DataElementPath pathToDataMatrix, String columnNameWithExpression, double[] expression1, double[] expression2, String columnName1, String columnName2, DataElementPath pathToOutputFolder, String fileName)
    {
        DataMatrix dm = new DataMatrix(pathToDataMatrix, null);
        double[] expression = dm.getColumn(columnNameWithExpression);
        double[][] matrix = new double[expression.length][2];
        for( int i = 0; i < expression.length; i++ )
        {
            matrix[i][0] = expression[i] - expression1[i];
            matrix[i][1] = expression[i] - expression2[i];
        }
        DataMatrix datMat = new DataMatrix(dm.getRowNames(), new String[]{columnNameWithExpression + "-" + columnName1, columnNameWithExpression + "-" + columnName2}, matrix);
        dm = DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dm, datMat});
        dm.writeDataMatrix(true, pathToOutputFolder, fileName, log);
    }

    public static void comparePearsonCorrelations()
    {
        double[] confidenceLevels = new double[]{0.95, 0.99, 0.999, 0.9999, 0.99999, 0.999999,  1.0 - 1.0E-10, 1.0 - 1.0E-15, 1.0 - 1.0E-20};
        double correlationCoefficient1 = 0.726, correlationCoefficient2 = 0.681;
        int sampleSize1 = 209911, sampleSize2 = 209911;
        double[] result = SimilaritiesAndDissimilarities.comparePearsonCorrelations(correlationCoefficient1, sampleSize1, correlationCoefficient2, sampleSize2);
        log.info("Z-score = " +  result[0] + " p-value = " + result[1]);
        
        for( int i = 0; i < confidenceLevels.length; i++ )
        {
            result = SimilaritiesAndDissimilarities.confidenceIntervalForPearsonCorrelation(correlationCoefficient1, sampleSize1, confidenceLevels[i]);
            double[] result2 = SimilaritiesAndDissimilarities.confidenceIntervalForPearsonCorrelation(correlationCoefficient2, sampleSize2, confidenceLevels[i]);
            log.info("i = " + i + "| confidenceLevels = " + confidenceLevels[i] + " CI1 = " + result[0] + " " + result[1] + " CI2 = " + result2[0] + " " + result2[1]);
        }
    }
    
    static Logger log = Logger.getLogger(ExploratoryAnalysisUtil.class.getName());
}
