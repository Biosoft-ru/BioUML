package ru.biosoft.analysis.javascript;

import static ru.biosoft.table.TableDataCollectionUtils.parseStringToColumnNames;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysis.CRClusterAnalysis;
import ru.biosoft.analysis.CRClusterAnalysisParameters;
import ru.biosoft.analysis.ClusterAnalysis;
import ru.biosoft.analysis.ClusterAnalysisParameters;
import ru.biosoft.analysis.CorrelationAnalysis;
import ru.biosoft.analysis.CorrelationAnalysisParameters;
import ru.biosoft.analysis.FoldChange;
import ru.biosoft.analysis.FoldChangeParameters;
import ru.biosoft.analysis.HypergeometricAnalysis;
import ru.biosoft.analysis.HypergeometricAnalysisParameters;
import ru.biosoft.analysis.MetaAnalysis;
import ru.biosoft.analysis.MetaAnalysisParameters;
import ru.biosoft.analysis.MicroarrayAnalysisParameters.Threshold;
import ru.biosoft.analysis.PolynomialRegressionAnalysis;
import ru.biosoft.analysis.PolynomialRegressionAnalysisParameters;
import ru.biosoft.analysis.UpDownIdentification;
import ru.biosoft.analysis.UpDownIdentificationParameters;
import ru.biosoft.analysis.VennAnalysis;
import ru.biosoft.analysis.VennAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.javascript.AnalysisFunction;
import ru.biosoft.plugins.javascript.JSAnalysis;
import ru.biosoft.plugins.javascript.JSDescription;
import ru.biosoft.plugins.javascript.JSProperty;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.util.TextUtil;


public class JavaScriptAnalysis extends JavaScriptHostObjectBase
{
    private final static String COLUMNS_DESCRIPTION = "Columns to include in analysis. Example of usage: '1,3,5,6-8,Column10,Groupname5'.";

    public AnalysisFunction getAnalysis(String name)
    {
        return new AnalysisFunction(null, name, AnalysisMethodRegistry.getMethodInfo(name));
    }


    //Hypergeometric analysis versions

    /**Simple hypergeometric analysis
     *@param experiment: experiment table
     *@param experimentColumns: group of columns from experiment table for analysis
     *@param pvalue: threshold for output data (will be outputed only scores with lesser P-value)
     *@param isCalculateFDR: if true, analysis will calculate False Discovery Rate.
     *@param outputPath: output detailed information or brief
     */
    public TableDataCollection hypergeomSimple(TableDataCollection experiment, String experimentColumns, double pvalue, boolean fdr,
            String outputPath)
    {
        return hypergeom(experiment, null, experimentColumns, "", "non-logarithmic", "all", pvalue, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, 0, true, fdr, false, null, "", outputPath);
    }

    /**Common hypergeometric analysis
     *@param experiment: experiment table
     *@param control: control table
     *@param experimentColumns: group of columns from experiment table for analysis
     *@param controlColumns: group of columns from control table for analysis
     *@param outputType: type of elements in result: "Up regulated", "Down regualted" or "Up and down regulated"
     *@param pvalue: threshold for output data (will be outputed only scores with lesser P-value)
     *@param isCalculateFDR: if true, analysis will calculate False Discovery Rate.
     *@param outputPath: output detailed information or brief
     */
    public TableDataCollection hypergeomCommon(TableDataCollection experiment, TableDataCollection control, String experimentColumns,
            String controlColumns, String outputType, double pvalue, boolean fdr, String outputPath)
    {
        double bv = ( control == null || controlColumns == null ) ? 0 : 1;

        return hypergeom(experiment, control, experimentColumns, controlColumns, "non-logarithmic", outputType, pvalue, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, bv, true, fdr, false, null, "", outputPath);
    }

    /**
     * Hypergeometric analysis with matching IDs with Ensemble. Results are meta-scores for genes
     *@param experiment: experiment table
     *@param control: control table
     *@param experimentColumns: group of columns from experiment table for analysis
     *@param controlColumns: group of columns from control table for analysis
     *@param outputType: type of elements in result: "Up regulated", "Down regualted" or "Up and down regulated"
     *@param pvalue: threshold for output data (will be outputed only scores with lesser P-value)
     *@param isCalculateFDR: if true, analysis will calculate False Discovery Rate.
     *@param outputPath: output detailed information or brief
     */
    public TableDataCollection hypergeomEnsemble(TableDataCollection experiment, TableDataCollection control, String experimentColumns,
            String controlColumns, String outputType, double pvalue, boolean fdr, String outputPath)
    {
        String repositoryPath = "../data";
        try
        {
            CollectionFactory.createRepository(repositoryPath);
        }
        catch( Exception ex )
        {
            this.setLastError(ex);
            return null;
        }
        DataCollection ensemble = CollectionFactory.getDataCollection("databases/Ensembl/Data/gene");
        double bv = ( control == null || controlColumns == null ) ? 0 : 1;
        return hypergeom(experiment, control, experimentColumns, controlColumns, outputType, "non-logarithmic", pvalue, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, bv, true, fdr, false, ensemble, "title", outputPath);
    }

    /**
     * Expert hypergeometric analysis version with full set of parameters
     *@param experiment: experiment table.
     *@param control: control table.
     *@param experimentColumns: group of columns from experiment table for analysis.
     *@param controlColumns: group of columns from control table for analysis.
     *@param outputType: type of elements in result: "Up regulated", "Down regualted" or "Up and down regulated".
     *@param pvalue: threshold for output data (will be outputed only scores with lesser P-value).
     *@param thresholdDown: threshold for input data values lesser then thresholdDown will be ignored.
     *@param thresholdUp: threshold for input data values greater then thresholdUp will be ignored.
     *@param boundaryvalue: Experiments value will be compared to that parameter. If experiment is compared with control than this value should be set to 1.
     *@param isControlAveraged: if false then each experiment value will be compared to corresponding control value (columns should agree).
     *@param isCalculateFDR: if true, analysis will calculate False Discovery Rate.
     *@param isDetailed: if true then result will contain detailed information otherwise - only scores.
     *@param matchingCollection: if this parameter is not null, experiment IDs will be matched to elements of this collection, and result will contain meta-scores for those elements.
     *@param outputPath: field from elements of matchingCollection which will be used as new keys.
     */
    @JSAnalysis ( HypergeometricAnalysis.class )
    public TableDataCollection hypergeom(
            @JSProperty ( "experimentData/tablePath" ) TableDataCollection experiment,
            @JSProperty ( "controlData/tablePath" ) TableDataCollection control,
            @JSProperty ( "experimentData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String experimentColumns,
            @JSProperty ( "controlData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String controlColumns,
            @JSProperty ( "inputLogarithmBase" ) @JSDescription ( "input data logarithm base: non-logarithmic, log2 , log10, logE" ) String logarithmBase,
            @JSProperty ( "outputType" ) String outputType, @JSProperty ( "pvalue" ) double pvalue,
            @JSProperty ( "threshold/thresholdDown" ) double thresholdDown, @JSProperty ( "threshold/thresholdUp" ) double thresholdUp,
            @JSProperty ( "bv" ) double bv, @JSProperty ( "isControlAveraged" ) boolean isControlAveraged,
            @JSProperty ( "fdr" ) boolean fdr, @JSProperty ( "detailed" ) boolean isDetailed,
            @JSProperty ( "matchingCollectionPath" ) DataCollection matchingCollection, @JSProperty ( "newKeySource" ) String keySource,
            @JSProperty ( "outputTablePath" ) String outputTablePath)
    {
        try
        {
            HypergeometricAnalysisParameters parameters = new HypergeometricAnalysisParameters();
            parameters.setBv(bv);
            parameters.setControlAveraged(isControlAveraged);
            parameters.setFdr(fdr);
            parameters.setDetailed(isDetailed);
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));
            parameters.setPvalue(pvalue);
            parameters.setThreshold(new Threshold(parameters, thresholdDown, thresholdUp));
            parameters.setOutputType(outputType);
            parameters.setInputLogarithmBase(logarithmBase);

            if( matchingCollection != null )
            {
                parameters.setMatchingCollection(matchingCollection);
                parameters.setNewKeySource(keySource);
            }

            String[] columnNames = parseStringToColumnNames(experimentColumns, experiment);
            parameters.setExperimentData(new ColumnGroup(parameters, columnNames, DataElementPath.create(experiment)));

            if( control != null && controlColumns != null )
            {
                String[] controlNames = parseStringToColumnNames(controlColumns, control);
                parameters.setControlData(new ColumnGroup(parameters, controlNames, DataElementPath.create(control)));
            }

            HypergeometricAnalysis analysis = new HypergeometricAnalysis(null, "Hypergeometric analysis");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    /**
     *@param outputName: name of output TableExperiment
     *@param ouputType: type of output data (up regulated genes, down regulated genes or both (default))
     *@param pvalue: threshold for output data (will be outputed only scores with lesser P-value)
     */
    @JSAnalysis ( MetaAnalysis.class )
    public TableDataCollection meta(@JSProperty ( "outputType" ) String outputType, @JSProperty ( "pvalue" ) double pvalue,
            @JSProperty ( "fdr" ) boolean fdr, @JSProperty ( "outputTablePath" ) String outputTablePath,
            @JSProperty ( "experiments" ) TableDataCollection ... experiments)
    {
        try
        {
            MetaAnalysisParameters parameters = new MetaAnalysisParameters();
            DataElementPathSet tables = new DataElementPathSet();
            for(TableDataCollection experiment: experiments)
            {
                tables.add(DataElementPath.create(experiment));
            }
            parameters.setTablePaths(tables);
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));
            parameters.setPvalue(pvalue);
            parameters.setOutputType(outputType);
            parameters.setFdr(fdr);
            MetaAnalysis analysis = new MetaAnalysis(null, "Meta analysis");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    /**
     * Up and down regulated genes identification
     *@param experimentTable: experiment table.
     *@param controlTable: control table.
     *@param experimentColumns: group of columns from experiment table for analysis.
     *@param controlColumns: group of columns from control table for analysis.
     *@param method: up and down identification method
     *@param outputType: type of elements in result: "Up regulated", "Down regulated" or "Up and down regulated".
     *@param pvalue: threshold for output data (will be outputed only scores with lesser P-value).
     *@param thresholdDown: threshold for input data values lesser then thresholdDown will be ignored.
     *@param thresholdUp: threshold for input data values greater then thresholdUp will be ignored.
     *@param isCalculateFDR: if true, analysis will calculate False Discovery Rate.
      those elements.
     *@param outputPath: field from elements of matchingCollection which will be used as new keys.
     */
    @JSAnalysis ( UpDownIdentification.class )
    public TableDataCollection updown(
            @JSProperty ( "experimentData/tablePath" ) TableDataCollection experiment,
            @JSProperty ( "controlData/tablePath" ) TableDataCollection control,
            @JSProperty ( "experimentData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String experimentColumns,
            @JSProperty ( "controlData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String controlColumns,
            @JSProperty ( "method" ) String method,
            @JSProperty ( "inputLogarithmBase" ) @JSDescription ( "input data logarithm base: non-logarithmic, log2 , log10, logE" ) String logarithmBase,
            @JSProperty ( "outputType" ) String outputType, @JSProperty ( "pvalue" ) double pvalue,
            @JSProperty ( "threshold/thresholdDown" ) double thresholdDown, @JSProperty ( "threshold/thresholdUp" ) double thresholdUp,
            @JSProperty ( "fdr" ) boolean fdr, @JSProperty ( "outputTablePath" ) String outputTablePath)
    {
        try
        {
            UpDownIdentificationParameters parameters = new UpDownIdentificationParameters();
            parameters.setFdr(fdr);
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));
            parameters.setPvalue(pvalue);
            parameters.setThreshold(new Threshold(parameters, thresholdDown, thresholdUp));
            parameters.setOutputType(outputType);
            parameters.setMethod(method);
            parameters.setInputLogarithmBase(logarithmBase);
            String[] experimentNames = TextUtil.isEmpty(experimentColumns) ? TableDataCollectionUtils.getColumnNames(experiment)
                    : parseStringToColumnNames(experimentColumns, experiment);

            String[] controlNames = TextUtil.isEmpty(controlColumns) ? TableDataCollectionUtils.getColumnNames(control)
                    : parseStringToColumnNames(controlColumns, control);

            parameters.setExperimentData(new ColumnGroup(parameters, experimentNames, DataElementPath.create(experiment)));
            parameters.setControlData(new ColumnGroup(parameters, controlNames, DataElementPath.create(control)));

            UpDownIdentification analysis = new UpDownIdentification(null, "Standard Up and Down Identification");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    /**
     * Fold change calculation
     *@param experimentTable: experiment table.
     *@param controlTable: control table.
     *@param experimentColumns: group of columns from experiment table for analysis.
     *@param controlColumns: group of columns from control table for analysis.
     *@param type: type of fold-change: AVERAGE_ALL, AVERAGE_CONTROL, AVERAGE_EXPERIMENT, AVERAGE_NONE, ONE_TO_ONE
     *@param logarithmBase: base of logarithm of result, usually: 2, 10. 1 means no logarithm/
     *@param thresholdDown: threshold for input data values lesser then thresholdDown will be ignored.
     *@param thresholdUp: threshold for input data values greater then thresholdUp will be ignored.
     *@param isCalculateFDR: if true, analysis will calculate False Discovery Rate.
      those elements.
     *@param outputPath: field from elements of matchingCollection which will be used as new keys.
     */
    @JSAnalysis ( FoldChange.class )
    public TableDataCollection foldchange(
            @JSProperty ( "experimentData/tablePath" ) TableDataCollection experiment,
            @JSProperty ( "controlData/tablePath" ) TableDataCollection control,
            @JSProperty ( "experimentData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String experimentColumns,
            @JSProperty ( "controlData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String controlColumns,
            @JSProperty ( "type" ) @JSDescription ( "Type of fold-change: 0 (average none), 1 (average control), 2 (average experiment), 3 (average all), 4 (one to one)." ) String type,
            @JSProperty ( "inputLogarithmBase" ) @JSDescription ( "input data logarithm base: non-logarithmic, log2 , log10, logE" ) String inputLogarithmBase,
            @JSProperty ( "outputLogarithmBase" ) @JSDescription ( "logarithm base for result: non-logarithmic, log2 , log10, logE" ) String outputLogarithmBase,
            @JSProperty ( "threshold/thresholdDown" ) double thresholdDown, @JSProperty ( "threshold/thresholdUp" ) double thresholdUp,
            @JSProperty ( "outputTablePath" ) String outputTablePath)
    {
        try
        {
            FoldChangeParameters parameters = new FoldChangeParameters();
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));
            parameters.setThreshold(new Threshold(parameters, thresholdDown, thresholdUp));
            parameters.setType(type);
            parameters.setInputLogarithmBase( inputLogarithmBase );
            parameters.setOutputLogarithmBase( outputLogarithmBase );

            String[] experimentNames = TextUtil.isEmpty(experimentColumns) ? TableDataCollectionUtils.getColumnNames(experiment)
                    : parseStringToColumnNames(experimentColumns, experiment);

            String[] controlNames = TextUtil.isEmpty(controlColumns) ? TableDataCollectionUtils.getColumnNames(control)
                    : parseStringToColumnNames(controlColumns, control);

            parameters.setExperimentData(new ColumnGroup(parameters, experimentNames, DataElementPath.create(experiment)));
            parameters.setControlData(new ColumnGroup(parameters, controlNames, DataElementPath.create(control)));

            FoldChange analysis = new FoldChange(null, "Fold-Change");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    /**
     *@param outputName: name of output TableExperiment
     *@param columns1: first group of columns from experiment for analysis
     *@param timePoints: time points for analysis
     */
    @JSAnalysis ( ClusterAnalysis.class )
    public TableDataCollection cluster(
            @JSProperty ( "experimentData/tablePath" ) TableDataCollection experiment,
            @JSProperty ( "experimentData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String experimentColumns,
            @JSProperty ( "method" ) @JSDescription ( "Analysis method. Possible values: 'Hartigan-Wong', 'Forgy', 'Lloyd', 'MacQueen'." ) String method,
            @JSProperty ( "clusterCount" ) int clusterCount, @JSProperty ( "outputTablePath" ) String outputTablePath)
    {
        try
        {
            ClusterAnalysisParameters parameters = new ClusterAnalysisParameters();
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));

            String[] experimentNames = TextUtil.isEmpty(experimentColumns) ? TableDataCollectionUtils.getColumnNames(experiment)
                    : parseStringToColumnNames(experimentColumns, experiment);
            parameters.setExperimentData(new ColumnGroup(parameters, experimentNames, DataElementPath.create(experiment)));
            parameters.setMethod(method);
            parameters.setClusterCount(clusterCount);
            ClusterAnalysis analysis = new ClusterAnalysis(null, "Cluster analysis");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    @JSAnalysis ( CRClusterAnalysis.class )
    public TableDataCollection crc(@JSProperty ( "experimentData/tablePath" ) TableDataCollection experiment,
            @JSProperty ( "experimentData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String experimentColumns,
            @JSProperty ( "cutoff" ) double cutoff, @JSProperty ( "chainsCount" ) int chainsCount,
            @JSProperty ( "cycleCount" ) int cycleCount, @JSProperty ( "invert" ) boolean invert,
            @JSProperty ( "threshold/thresholdDown" ) double thresholdDown, @JSProperty ( "threshold/thresholdUp" ) double thresholdUp,
            @JSProperty ( "outputTablePath" ) String outputTablePath)
    {
        try
        {
            CRClusterAnalysisParameters parameters = new CRClusterAnalysisParameters();
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));
            String[] experimentNames = TextUtil.isEmpty(experimentColumns) ? TableDataCollectionUtils.getColumnNames(experiment)
                    : parseStringToColumnNames(experimentColumns, experiment);
            parameters.setExperimentData(new ColumnGroup(parameters, experimentNames, DataElementPath.create(experiment)));
            parameters.setChainsCount(chainsCount);
            parameters.setCutoff(cutoff);
            parameters.setCycleCount(cycleCount);
            parameters.setInvert(invert);
            parameters.setThreshold(new Threshold(parameters, thresholdDown, thresholdUp));
            CRClusterAnalysis analysis = new CRClusterAnalysis(null, "CRCluster analysis");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    /**
     *@param outputName: name of output TableExperiment
     *@param columns1: first group of columns from experiment for analysis
     *@param timePoints: time points for analysis
     */
    @JSAnalysis ( PolynomialRegressionAnalysis.class )
    public TableDataCollection regression(
            @JSProperty ( "experimentData/tablePath" ) TableDataCollection experiment,
            @JSProperty ( "experimentData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String experimentColumns,
            @JSProperty ( "timePoints" ) @JSDescription ( "time points for regression. Amount of time points must be equal to amount of columns. Example: '2,4,6,8,10,12'." ) String timePoints,
            @JSProperty ( "regressionPower" ) int regressionPower, @JSProperty ( "pvalue" ) double pvalue,
            @JSProperty ( "threshold/thresholdDown" ) double thresholdDown, @JSProperty ( "threshold/thresholdUp" ) double thresholdUp,
            @JSProperty ( "fdr" ) boolean fdr, @JSProperty ( "outputTablePath" ) String outputTablePath)
    {
        try
        {
            PolynomialRegressionAnalysisParameters parameters = new PolynomialRegressionAnalysisParameters();
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));
            parameters.setPvalue(pvalue);
            parameters.setThreshold(new Threshold(parameters, thresholdDown, thresholdUp));
            String[] experimentNames = TextUtil.isEmpty(experimentColumns) ? TableDataCollectionUtils.getColumnNames(experiment)
                    : parseStringToColumnNames(experimentColumns, experiment);
            parameters.setExperimentData(new ColumnGroup(parameters, experimentNames, DataElementPath.create(experiment)));
            parameters.setFdr(fdr);
            parameters.setRegressionPower(regressionPower);
            PolynomialRegressionAnalysis analysis = new PolynomialRegressionAnalysis(null, "Regression analysis");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }
    /**
     *@param outputName: name of output TableExperiment
     *@param columns1: first group of columns from experiment for analysis
     *@param timePoints: time points for analysis
     */
    public TableDataCollection linearRegress(TableDataCollection experiment, String columns, String timePoints, String outputTablePath)
    {
        try
        {
            return regression(experiment, columns, timePoints, 1, 0.01, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
                    outputTablePath);
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }


    /**
     *@param outputName: name of output TableExperiment
     *@param columns1: first group of columns from experiment for analysis
     *@param timePoints: time points for analysis
     */
    public TableDataCollection squareRegress(TableDataCollection experiment, String columns, String timePoints, String outputTablePath)
    {
        try
        {
            return regression(experiment, columns, timePoints, 2, 0.01, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
                    outputTablePath);
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    /**
     *@param outputName: name of output TableDataCollection
     *@param columns: columns for analysis
     *@param control: control group
     */
    @JSAnalysis ( CorrelationAnalysis.class )
    public TableDataCollection correlation(@JSProperty ( "experimentData/tablePath" ) TableDataCollection experiment,
            @JSProperty ( "controlData/tablePath" ) TableDataCollection control,
            @JSProperty ( "experimentData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String experimentColumns,
            @JSProperty ( "controlData/namesDescription" ) @JSDescription ( COLUMNS_DESCRIPTION ) String controlColumns,
            @JSProperty ( "dataSource" ) @JSDescription ( "Data from input tables, could be 'Columns' or 'Rows'." ) String dataSource,
            @JSProperty ( "resultType" ) @JSDescription ( "Result data format, could be 'Column' or 'Matrix'." ) String resultType,
            @JSProperty ( "correlationType" ) @JSDescription ( "Correlation, could be 'Pearson' or 'Spearman'." ) String correlationType,
            @JSProperty ( "pvalue" ) double pvalue, @JSProperty ( "threshold/thresholdDown" ) double thresholdDown,
            @JSProperty ( "threshold/thresholdUp" ) double thresholdUp, @JSProperty ( "fdr" ) boolean fdr,
            @JSProperty ( "outputTablePath" ) String outputTablePath)
    {
        try
        {
            CorrelationAnalysisParameters parameters = new CorrelationAnalysisParameters();
            parameters.setOutputTablePath(DataElementPath.create(outputTablePath));

            String[] experimentNames = TextUtil.isEmpty(experimentColumns) ? TableDataCollectionUtils.getColumnNames(experiment)
                    : parseStringToColumnNames(experimentColumns, experiment);

            String[] controlNames = TextUtil.isEmpty(controlColumns) ? TableDataCollectionUtils.getColumnNames(control)
                    : parseStringToColumnNames(controlColumns, control);

            parameters.setExperimentData(new ColumnGroup(parameters, experimentNames, DataElementPath.create(experiment)));
            parameters.setControlData(new ColumnGroup(parameters, controlNames, DataElementPath.create(control)));
            parameters.setResultTypeName(resultType);
            parameters.setCorrelationTypeName(correlationType);
            parameters.setDataSourceName(dataSource);
            parameters.setPvalue(pvalue);
            parameters.setThreshold(new Threshold(parameters, thresholdDown, thresholdUp));
            parameters.setFdr(fdr);
            CorrelationAnalysis analysis = new CorrelationAnalysis(null, "Correlation analysis");
            analysis.setParameters(parameters);
            return analysis.justAnalyze();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
        return null;
    }

    /**
     *@param outputName: name of output TableDataCollection
     *@param columns: columns for analysis
     *@param control: control group
     */
    @JSAnalysis ( VennAnalysis.class )
    public void venn(@JSProperty ( "table1Path" ) TableDataCollection table1,
            @JSProperty ( "table2Path" ) TableDataCollection table2,
            @JSProperty ( "table3Path" ) TableDataCollection table3, @JSProperty ( "simple" ) boolean simple,
            @JSProperty ( "output" ) String outputPath)
    {
        try
        {
            VennAnalysisParameters parameters = new VennAnalysisParameters();

            parameters.setTable1Path(DataElementPath.create(table1));
            parameters.setTable2Path(DataElementPath.create(table2));
            parameters.setTable3Path(DataElementPath.create(table3));
            parameters.setSimple(simple);
            parameters.setOutput(DataElementPath.create(outputPath));
            VennAnalysis analysis = new VennAnalysis(null, "");
            analysis.setParameters(parameters);
            analysis.getJobControl();
            analysis.justAnalyzeAndPut();
        }
        catch( Exception e )
        {
            setLastError(e);
        }
    }
}
