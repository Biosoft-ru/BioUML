package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.Util.MatrixElementsStatistics;
import ru.biosoft.analysis.javascript.JavaScriptAnalysis;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.util.BeanUtil;

@ClassIcon ( "resources/hypergeometric-analysis.gif" )
public class HypergeometricAnalysis extends UpDownIdentification
{
    private int repeatedIDs;
    private int unMatchedIDs;
    private HashMap<String, ArrayList<String>> newKeys2OldKeys;

    public HypergeometricAnalysis(DataCollection origin, String name) throws Exception
    {
        super(origin, name, JavaScriptAnalysis.class, new HypergeometricAnalysisParameters());
    }

    protected HypergeometricAnalysis(DataCollection origin, String name, HypergeometricAnalysisParameters parameters) throws Exception
    {
        super(origin, name, JavaScriptAnalysis.class, parameters);
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        try
        {
            parameters = (HypergeometricAnalysisParameters)params;
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Wrong parameters");
        }
    }

    @Override
    public HypergeometricAnalysisParameters getParameters()
    {
        return (HypergeometricAnalysisParameters)parameters;
    }
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        ColumnGroup experimentData = parameters.getExperimentData();
        if( experimentData == null || experimentData.getColumns().length == 0 )
            throw new IllegalArgumentException("Please specify columns");
        if( experimentData.getTable() == null )
            throw new IllegalArgumentException("Please specify input table");

        ColumnGroup controlData = parameters.getControlData();
        if( controlData.getTable() != null && controlData.getColumns().length == 0 )
            throw new IllegalArgumentException("Please specify control columns");

        if( getParameters().getMatchingCollection() != null && getParameters().getNewKeySource() == null )
            throw new IllegalArgumentException("Please specify new key field from matching collection");

        if( !getParameters().isControlAveraged() && parameters.getControl() != null && parameters.getControlData().getColumns() != null
                && parameters.getControlData().getColumns().length != experimentData.getColumns().length )
            throw new IllegalArgumentException(
                    "Can not calculate without averaging control data because experiment columns don't match control columns");
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            HypergeometricAnalysisParameters parameters = (HypergeometricAnalysisParameters)parametersObject;

            StringBuffer getSourceScript = new StringBuffer();
            String[] params = {"null", "null", "", "", "non-logarithmic", "0.01", "all", "-Infinity", "Infinity", "1", "true", "false", "false",
                    "null", "''", ""};

            if( parameters.getExperiment() != null )
            {
                getSourceScript.append("var experiment = data.get('"
                        + StringEscapeUtils.escapeJavaScript(parameters.getExperiment().getCompletePath().toString()) + "');\n");
                params[0] = "experiment";
            }
            if( parameters.getControl() != null )
            {
                if( parameters.getExperiment() != null && parameters.getExperiment().equals(parameters.getControl()) )
                    params[1] = "experiment";
                else
                {
                    getSourceScript.append("var control = data.get('"
                            + StringEscapeUtils.escapeJavaScript(parameters.getControl().getCompletePath().toString()) + "');\n");
                    params[1] = "control";
                }
            }
            if( parameters.getExperimentData().getColumns() != null )
                params[2] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getExperimentData().getNamesDescription()) + "'";
            if( parameters.getControlData().getColumns() != null )
                params[3] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getControlData().getNamesDescription()) + "'";
            if( parameters.getInputLogarithmBase() != null )
                params[4] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getInputLogarithmBase()) + "'";
            if( parameters.getOutputType() != null )
                params[5] = "'" + StringEscapeUtils.escapeJavaScript( parameters.getOutputType() ) + "'";
            if( parameters.getPvalue() != null )
                params[6] = parameters.getPvalue().toString();
            if( parameters.getThresholdDown() != null )
                params[7] = parameters.getThresholdDown().toString();
            if( parameters.getThresholdUp() != null )
                params[8] = parameters.getThresholdUp().toString();
            if( parameters.getBv() != null )
                params[9] = parameters.getBv().toString();
            if( parameters.isControlAveraged() != null )
                params[10] = parameters.isControlAveraged().toString();
            if( parameters.isFdr() != null )
                params[11] = parameters.isFdr().toString();
            if( parameters.isDetailed() != null )
                params[12] = parameters.isDetailed().toString();
            if( parameters.getMatchingCollection() != null )
            {
                getSourceScript.append("var matchingCollection = data.get('"
                        + StringEscapeUtils.escapeJavaScript(parameters.getMatchingCollection().getCompletePath().toString()) + "');\n");
                params[13] = "matchingCollection";
                if( parameters.getNewKeySource() != null )
                {
                    params[14] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getNewKeySource()) + "'";
                }
            }
            if( parameters.getOutputTablePath() != null )
                params[15] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputTablePath().toString()) + "'";

            String putTableScript = "data.save(result,'" + parameters.getOutputCollection().getCompletePath().toString() + "/');";

            return getSourceScript.append("var result = microarray.hypergeom(" + String.join(", ", params) + ");\n").append(
                    putTableScript).toString();
        }
        catch( Exception ex )
        {
            return "Error during java script generating" + ex.getMessage();
        }
    }
    @Override
    public TableDataCollection getAnalyzedData() throws Exception
    {
        validateParameters();
        int outputTypeCode = parameters.getOutputTypeCode();
        boolean upRegulated = ( outputTypeCode & HypergeometricAnalysisParameters.UP_REGULATED ) == HypergeometricAnalysisParameters.UP_REGULATED;
        boolean downRegulated = ( outputTypeCode & HypergeometricAnalysisParameters.DOWN_REGULATED ) == HypergeometricAnalysisParameters.DOWN_REGULATED;

        upRegulatedFound = 0;
        downRegulatedFound = 0;

        initData();
        if( data == null )
            throw new Exception("Problems during data initialization");

        TableDataCollection output = calculate(data);

        if( upRegulated )
            log.info("Up regulated objects found: " + upRegulatedFound);
        if( downRegulated )
            log.info("Down regulated objects found: " + downRegulatedFound);

        //calculate FDR
        if( parameters.isFdr() )
        {
            int totalEntries = pvalues.size();
            upFoundByMistake = new int[totalEntries];
            downFoundByMistake = new int[totalEntries];
            
            for( int niter = 0; niter < 50; niter++ )
            {
                double[][] permutatedMatrix = Stat.permutationComplicatedMatrix(data);
                calculateFDR(permutatedMatrix, pvalues);
            }

            for( int i = 0; i < totalEntries; i++ )
            {
                output.getAt( i ).setValue( "FDR UP", upFoundByMistake[i] / ( 50d * data.length ) );
                output.getAt( i ).setValue( "FDR DOWN", downFoundByMistake[i] / ( 50d * data.length ) );
            }
        }
        return output;
    }

    private List<Double> pvalues;

    private TableDataCollection calculate(double[][] matrix) throws Exception
    {
        boolean detailed = getParameters().isDetailed();
        boolean meta = ( getParameters().getMatchingCollection() != null && getParameters().getNewKeySource() != null );
        int outputTypeCode = parameters.getOutputTypeCode();
        double cutOff = parameters.getPvalue();
        boolean upRegulated = ( outputTypeCode & HypergeometricAnalysisParameters.UP_REGULATED ) == HypergeometricAnalysisParameters.UP_REGULATED;
        boolean downRegulated = ( outputTypeCode & HypergeometricAnalysisParameters.DOWN_REGULATED ) == HypergeometricAnalysisParameters.DOWN_REGULATED;

        TableDataCollection result = createOutputTable( meta, detailed, upRegulated, downRegulated );

        double barrierValue = getParameters().getBv();

        MatrixElementsStatistics statistics = new MatrixElementsStatistics( matrix );
        int totalData = statistics.getSize();
        int rowCount = matrix.length;
        pvalues = new ArrayList<>();
        for( int i = 0; i < rowCount && go; i++ )
        {
            incPreparedness( step++ );
            double[] row = Arrays.copyOf( matrix[i], matrix[i].length );
            Result resultRow = calculate( row, statistics, barrierValue, totalData, i );
           
            if( resultRow.pValue > cutOff )
                continue;
            pvalues.add( resultRow.pValue );
            if( resultRow.statistic <= 0 )
            {
                if( !upRegulated )
                    continue;
                upRegulatedFound++;
            }
            else
            {
                if( !downRegulated )
                    continue;
                downRegulatedFound++;
            }

            Vector<Object> rowBuffer = new Vector<>();

            double score = Math.log10( resultRow.pValue ) * Math.signum( resultRow.statistic );
            rowBuffer.add( score );

            if( meta )
            {
                ArrayList<String> keyList = newKeys2OldKeys.get( keys[i] );
                rowBuffer.add( keyList );
                rowBuffer.add( keyList.size() );
            }

            if( upRegulated && detailed )
            {
                rowBuffer.add( resultRow.pvalueUp );
                rowBuffer.add( resultRow.pvalueUp1 );
                rowBuffer.add( resultRow.greaterThenBvDataInRow );
                rowBuffer.add( resultRow.greaterThenCritical );
                rowBuffer.add( resultRow.criticalElementUp );
            }

            if( downRegulated && detailed )
            {
                rowBuffer.add( resultRow.pvalueDown );
                rowBuffer.add( resultRow.pvalueDown1 );
                rowBuffer.add( resultRow.smallerThenBvDataInRow );
                rowBuffer.add( resultRow.smallerThenCritical );
                rowBuffer.add( resultRow.criticalElementDown );
            }
            
            if (parameters.isFdr())
            {
                rowBuffer.add( 0 );
                rowBuffer.add( 0 );
            }
            TableDataCollectionUtils.addRow( result, keys[i], rowBuffer.toArray() );

        }
        return result;
    }
    
    private static class Result
    {
        double pValue;
        double statistic;
        double pvalueUp;
        double pvalueUp1;
        int greaterThenBvDataInRow;
        int greaterThenCritical;
        double pvalueDown;
        double pvalueDown1;
        int smallerThenBvDataInRow;
        int smallerThenCritical;
        double criticalElementDown;
        double criticalElementUp;
    }
    
    private Result calculate(double[] row, MatrixElementsStatistics statistics, double barrierValue, int totalData, int i)
    {
        int dataInRow = row.length;
        int[] pos = Util.sortHeap(row);
        int notEmptyDataInRow = 0;
        int equalToBvDataInRow = 0;
        int greaterThenBvDataInRow = 0;
        for( int j = dataInRow - 1; j >= 0; j-- )
        {
            notEmptyDataInRow++;
            if( row[j] == barrierValue )
                equalToBvDataInRow++;
            if( row[j] > barrierValue )
                greaterThenBvDataInRow++;
        }
        if( notEmptyDataInRow == 0 )
            return null;

        int smallerThenBvDataInRow = notEmptyDataInRow - greaterThenBvDataInRow - equalToBvDataInRow;

        //Calculate for UP_REGULATED
        double pvalueUp = 1;
        int greaterThenCritical = 0;
        double criticalElementUp = 0;

        double pvaluetemp = 0;

        for( int j = 1; j <= greaterThenBvDataInRow; j++ )
        {
            try
            {
                int greaterThenThisData = statistics.getGreaterElementsCount(i, pos[dataInRow - j]);

                int m = Math.min(notEmptyDataInRow, greaterThenThisData);

                pvaluetemp = 0;
                for( int k = j; k <= m; k++ )
                    pvaluetemp += Math.exp(Stat.logHyperDistribution(totalData, notEmptyDataInRow, greaterThenThisData, k));

                if( pvaluetemp < pvalueUp )
                {
                    pvalueUp = pvaluetemp;
                    greaterThenCritical = j;
                    criticalElementUp = row[dataInRow - j];
                }
            }
            catch( Exception ex )
            {
                //In some cases when we lack of rows incorrect parameters may be passed to logHyperDistribution
                //For example: totalData == notEmptyDataInRow (we have only one row) but j < greaterThenThisData because of repeating values
                //We consider analysis to be useless in such cases so return bad pvalue.
            }
        }
        double pvalueUp1 = 1;
        if( notEmptyDataInRow == equalToBvDataInRow )
            pvalueUp1 = 1;
        else if( notEmptyDataInRow == greaterThenBvDataInRow )
            pvalueUp1 = Math.pow(0.5, notEmptyDataInRow);
        else if( greaterThenBvDataInRow != 0 )
            pvalueUp1 = Stat.betaDistribution(0.5, greaterThenBvDataInRow, notEmptyDataInRow - greaterThenBvDataInRow + 1, 400)[0];

        //DOWN_REGULATED
        double pvalueDown = 1;
        int smallerThenCritical = 0;
        double criticalElementDown = 0;

        for( int j = 1; j <= smallerThenBvDataInRow; j++ )
        {
            try
            {
                int index = dataInRow - notEmptyDataInRow + j - 1;
                int smallerThenThisData = statistics.getSmallerElementsCount(i, pos[index]);

                int m = Math.min(notEmptyDataInRow, smallerThenThisData);

                pvaluetemp = 0;
                for( int k = j; k <= m; k++ )
                    pvaluetemp += Math.exp(Stat.logHyperDistribution(totalData, notEmptyDataInRow, smallerThenThisData, k));

                if( pvaluetemp < pvalueDown )
                {
                    pvalueDown = pvaluetemp;
                    smallerThenCritical = j - 1;
                    criticalElementDown = row[index];
                }
            }
            catch( Exception ex )
            {
                //In some cases when we lack of rows incorrect parameters may be passed to logHyperDistribution
                //For example: totalData == notEmptyDataInRow (we have only one row) but k < smallerThenThisData because of repeating values
                //We consider analysis to be useless in such cases so return bad pvalue.
            }
        }

        double pvalueDown1 = 1;
        if( notEmptyDataInRow == equalToBvDataInRow )
            pvalueDown1 = 1;
        else if( notEmptyDataInRow == smallerThenBvDataInRow )
            pvalueDown1 = Math.pow(0.5, notEmptyDataInRow);
        else if( smallerThenBvDataInRow != 0 )
            pvalueDown1 = Stat.betaDistribution(0.5, smallerThenBvDataInRow, notEmptyDataInRow - smallerThenBvDataInRow + 1, 400)[0];

        double pvalueUpMin = Math.min(pvalueUp, pvalueUp1);
        double pvalueDownMin = Math.min(pvalueDown, pvalueDown1);

        Result result = new Result();
        result.pValue = Math.min(pvalueUpMin, pvalueDownMin);
        result.statistic = pvalueUpMin - pvalueDownMin;
        result.pvalueUp = pvalueUp;
        result.pvalueUp1 = pvalueUp1;
        result.greaterThenBvDataInRow = greaterThenBvDataInRow;
        result.greaterThenCritical = greaterThenCritical;
        result.criticalElementUp = criticalElementUp;
        result.pvalueDown = pvalueDown;
        result.pvalueDown1 = pvalueDown1;
        result.smallerThenBvDataInRow = smallerThenBvDataInRow;
        result.smallerThenCritical = smallerThenCritical;
        result.criticalElementDown = criticalElementDown;

        return result;
    }
    
    
    public void addUpColumns(TableDataCollection te) throws Exception
    {
        te.getColumnModel().addColumn("P-value UP", String.class);
        te.getColumnModel().addColumn("Binomial P-value UP", String.class);
        te.getColumnModel().addColumn("Not empty data UP", String.class);
        te.getColumnModel().addColumn("Nontypical elements UP", String.class);
        te.getColumnModel().addColumn("Critical element UP", String.class);
    }

    public void addDownColumns(TableDataCollection te) throws Exception
    {
        te.getColumnModel().addColumn("P-value DOWN", String.class);
        te.getColumnModel().addColumn("Binomial P-value DOWN", String.class);
        te.getColumnModel().addColumn("Not empty data DOWN", String.class);
        te.getColumnModel().addColumn("Nontypical elements DOWN", String.class);
        te.getColumnModel().addColumn("Critical element DOWN", String.class);
    }

    private void divideByAveragedControl(double[][] matrix, double[][] controlMatrix)
    {
        if( controlMatrix == null || controlMatrix.length == 0 )
            return;
        double[] control = new double[controlMatrix.length];

        for( int i = 0; i < matrix.length; i++ )
        {
            control[i] = Stat.mean(controlMatrix[i]);
            for( int j = 0; j < matrix[i].length; j++ )
            {
                matrix[i][j] /= control[i];
            }
            matrix[i] = Util.avoidNaNs(matrix[i]);
        }
    }

    private void divideByControl(double[][] matrix, double[][] controlMatrix) throws Exception
    {
        try
        {
            for( int i = 0; i < matrix.length; i++ )
            {
                for( int j = 0; j < matrix[i].length; j++ )
                {
                    matrix[i][j] /= controlMatrix[i][j];
                }
                matrix[i] = Util.avoidNaNs(matrix[i]);
            }
        }
        catch( Exception ex )
        {
            throw new Exception("Experiment and control data does not agree");
        }
    }

    /**
     * Prepare experiment and control matrices
     * note: into result matrices
     * @return
     */
    protected void initData() throws Exception
    {
        TableDataCollection experimentTable = parameters.getExperimentData().getTable();
        String[] experimentColumns = parameters.getExperimentData().getNames();
        int[] experimentIndices = TableDataCollectionUtils.getColumnIndexes(experimentTable, experimentColumns);

        double thresholdDown = parameters.getThresholdDown();
        double thresholdUp = parameters.getThresholdUp();

        double logarithmBase = Util.getLogarithmBase(parameters.getInputLogarithmBaseCode());

        if( parameters.getControlData() == null || parameters.getControlData().getTable() == null
                || parameters.getControlData().getColumns() == null )
        {
            keys = TableDataCollectionUtils.getKeysUnsorted(experimentTable);
            data = TableDataCollectionUtils.getComplicatedMatrix(experimentTable, experimentIndices, thresholdDown, thresholdUp);
            if( logarithmBase != 1 )
                Util.pow(logarithmBase, data);
        }
        else
        {
            TableDataCollection controlTable = parameters.getControlData().getTable();
            if( controlTable.isEmpty() )
                log.info("Control table is empty or loaded with errors.");

            String[] controlColumns = parameters.getControlData().getNames();
            int[] controlIndices = TableDataCollectionUtils.getColumnIndexes(controlTable, controlColumns);

            if( !experimentTable.equals(controlTable) )
            {
                experimentTable = TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, experimentTable, controlTable, null,
                        experimentColumns, controlColumns);

                //we assume that after join all columns will be added consequently so new indices are just {1,2,...}
                int n = experimentIndices.length;
                experimentIndices = new int[n];
                for( int i = 0; i < n; i++ )
                    experimentIndices[i] = i;

                int offset = experimentIndices.length;//from this point experiment indices ends and starts control indices
                n = controlIndices.length;
                controlIndices = new int[n];
                for( int i = 0; i < n; i++ )
                    controlIndices[i] = i + offset;
            }

            keys = TableDataCollectionUtils.getKeysUnsorted(experimentTable);

            if( getParameters().isControlAveraged() )
            {
                data = TableDataCollectionUtils.getComplicatedMatrix(experimentTable, experimentIndices, thresholdDown, thresholdUp);
                control = TableDataCollectionUtils.getComplicatedMatrix(experimentTable, controlIndices, thresholdDown, thresholdUp);

                if( logarithmBase != 1 )
                {
                    Util.pow(logarithmBase, data);
                    Util.pow(logarithmBase, control);
                }
                divideByAveragedControl(data, control);
            }
            else
            {
                data = TableDataCollectionUtils.getMatrix(experimentTable, experimentIndices, thresholdDown, thresholdUp);
                control = TableDataCollectionUtils.getMatrix(experimentTable, controlIndices, thresholdDown, thresholdUp);

                if( logarithmBase != 1 )
                {
                    Util.pow(logarithmBase, data);
                    Util.pow(logarithmBase, control);
                }

                divideByControl(data, control);
            }
            control = null;
        }
        //if we conduct meta-analysis
        DataCollection matchingCollection = getParameters().getMatchingCollection();
        if( matchingCollection != null )
        {
            generateNewData(matchingCollection, getParameters().getNewKeySource());
        }
    }


    /**
     * In case of meta-analysis we append data rows which corresponds to the same value from <b>field</b><br>
     * ( Matching table IDs through BioHUB )<br>
     * @param annotationCollection
     * @param field
     * @throws Exception
     */
    private void generateNewData(DataCollection annotationCollection, String field) throws Exception
    {
        HashMap<String, ArrayList<Double>> newKeys2NewData = new HashMap<>();
        newKeys2OldKeys = new HashMap<>();

        //loading biohub
        String dataBaseName = annotationCollection.getCompletePath().getPathComponents()[1];
        CollectionRecord collection = new CollectionRecord(annotationCollection.getCompletePath(), true);
        TargetOptions dbOptions = new TargetOptions(collection);
        BioHub hub = BioHubRegistry.getBioHub(dbOptions);

        if( hub == null )
            throw new Exception("Failed to load BioHub");

        Element[] startElements = new Element[keys.length];
        for( int i = 0; i < keys.length; i++ )
            startElements[i] = new Element("stub/%//" + keys[i]);//Util.avoidCOPY(keys[i])); //magic?

        log.info("Loading data from BioHub, please wait...");
        Map<Element, Element[]> references = hub.getReferences(startElements, dbOptions, null, 1, -1);

        if( references == null || references.size() == 0 )
            throw new Exception("No data were found.");

        repeatedIDs = 0; //ids from table with more than one reference
        unMatchedIDs = 0; //ids from table without references

        for( int i = 0; i < keys.length && go; i++ )
        {
            String oldKey = keys[i];
            Element[] refs = references.get(startElements[i]);
            if( refs == null || refs.length == 0 ) //check if there are no references - we preserve old key and data
            {
                unMatchedIDs++;
                ArrayList<String> key = new ArrayList<>();
                key.add(keys[i]);
                ArrayList<Double> newData = new ArrayList<>();
                for( double dataElement : data[i] )
                    newData.add(dataElement);
                newKeys2OldKeys.put(oldKey, key);
                newKeys2NewData.put(oldKey, newData);
                continue;
            }
            if( refs.length > 1 ) //check if there is more than one reference
            {
                repeatedIDs++;
            }
            double[] oldData = data[i];

            for( Element ref : refs )
            {
                //getting new Key from reference
                String idFromCollection = ref.getAccession();
                DataElement element = annotationCollection.get(idFromCollection);
                String newKey = BeanUtil.getBeanPropertyValue(element, field).toString();

                ArrayList<Double> newData = ( newKeys2NewData.containsKey(newKey) ) ? newKeys2NewData.get(newKey) : new ArrayList<>();
                for( double dataElement : oldData )
                    newData.add(dataElement);
                newKeys2NewData.put(newKey, newData);

                ArrayList<String> oldKeys = ( newKeys2OldKeys.containsKey(newKey) ) ? newKeys2OldKeys.get(newKey) : new ArrayList<>();
                oldKeys.add(oldKey);
                newKeys2OldKeys.put(newKey, oldKeys);
            }
        }

        //get String[] keys and double[][] data from new mapping for further analysis
        keys = newKeys2NewData.keySet().toArray(new String[newKeys2NewData.size()]);
        data = StreamEx.of( keys ).map( key -> StreamEx.of( newKeys2NewData.get( key ) ).mapToDouble( x -> x ).toArray() )
                .toArray( double[][]::new );
        log.info(unMatchedIDs + " IDs from input data were not matched (initial IDs preserverd)");
        log.info(repeatedIDs + " IDs from input data were mathed to multiple objects");
    }

    protected TableDataCollection createOutputTable(boolean meta, boolean detailed, boolean upRegulated, boolean downRegulated)
            throws Exception
    {
        TableDataCollection result = parameters.getOutputTable();
        result.getColumnModel().addColumn("-log(P-value)", Double.class);

        //if conduct meta-analysis output set of old keys matched to new one
        if( meta )
        {
            result.getColumnModel().addColumn("Hits", StringSet.class);
            result.getColumnModel().addColumn("Number of hits", Integer.class);
        }

        if( detailed )
        {
            if( upRegulated )
            {
                addUpColumns(result);
            }
            if( downRegulated )
            {
                addDownColumns(result);
            }
        }
        
        if (parameters.isFdr())
        {
            result.getColumnModel().addColumn("FDR UP", Double.class);
            result.getColumnModel().addColumn("FDR DOWN", Double.class);
        }

        return result;
    }
    
    private int[] upFoundByMistake;
    private int[] downFoundByMistake;
    
    private void calculateFDR(double[][] matrix, List<Double> pValues) throws Exception
    {
        double barrierValue = getParameters().getBv();
        MatrixElementsStatistics statistics = new MatrixElementsStatistics( matrix );
        int totalData = statistics.getSize();
        int rowCount = matrix.length;
        for( int i = 0; i < rowCount && go; i++ )
        {
            incPreparedness( step++ );
            double[] row = Arrays.copyOf( matrix[i], matrix[i].length );
            Result result = calculate( row, statistics, barrierValue, totalData, i );

            for (int j=0; j< pValues.size(); j++)
            {
                if( result.pValue <= pValues.get(j) )
                {
                    if( result.statistic <= 0 )
                        upFoundByMistake[j]++;
                    else
                        downFoundByMistake[j]++;
                }
            }
        }
    }
}
