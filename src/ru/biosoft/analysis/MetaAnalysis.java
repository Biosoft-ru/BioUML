package ru.biosoft.analysis;

import org.apache.commons.lang.StringEscapeUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.MicroarrayAnalysisParameters.Threshold;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnGroup;

/**
 * Meta analysis. Must be applied to results of another analysis.
 * It combines all those results in one table and run Hypergeometric analysis on it.
 * @author axec
 *
 */
@ClassIcon ( "resources/meta-analysis.gif" )
public class MetaAnalysis extends HypergeometricAnalysis
{
    private DataElementPath tempPath;
    
    public MetaAnalysis(DataCollection<?> origin, String name) throws Exception
    {
        super(origin, name, new MetaAnalysisParameters());
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        try
        {
            parameters = (MetaAnalysisParameters)params;
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Wrong parameters");
        }
    }
    @Override
    public MetaAnalysisParameters getParameters()
    {
        return (MetaAnalysisParameters)parameters;
    }
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        DataElementPathSet tables = getParameters().getTablePaths();
        if( tables == null || tables.size() < 2 )
            throw new IllegalArgumentException("Two or more experiments should be selected");

        DataElementPath path = parameters.getOutputTablePath();
        if( path == null || path.optParentCollection() == null || path.getParentCollection().getName().isEmpty() )
            throw new IllegalArgumentException("Please specify output collection");

        if( path.getName().isEmpty() )
            throw new IllegalArgumentException("Please specify output name");
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            MetaAnalysisParameters parameters = (MetaAnalysisParameters)parametersObject;

            StringBuffer[] getTableScript = new StringBuffer[] {};
            StringBuffer[] tables = new StringBuffer[] {};

            String[] params = {"", "0.01", "false", "", ""};

            if( parameters.getTablePaths() != null )
            {
                ru.biosoft.access.core.DataElementPath[] tablePaths = parameters.getTablePaths().toArray(new ru.biosoft.access.core.DataElementPath[parameters.getTablePaths().size()]);
                getTableScript = new StringBuffer[tablePaths.length];
                tables = new StringBuffer[tablePaths.length];
                for( int i = 0; i < tables.length; i++ )
                {
                    tables[i] = new StringBuffer("table_").append(i);
                    getTableScript[i] = new StringBuffer("var ").append(tables[i]);
                    getTableScript[i].append(" = data.get('");
                    getTableScript[i].append(StringEscapeUtils.escapeJavaScript(tablePaths[i].toString()));
                    getTableScript[i].append("');\n");
                }
            }

            if( parameters.getOutputType() != null )
                params[0] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputType()) + "'";
            if( parameters.getPvalue() != null )
                params[1] = parameters.getPvalue().toString();
            if( parameters.isFdr() != null )
                params[2] = parameters.isFdr().toString();
            if( parameters.getOutputTablePath() != null )
                params[3] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputTablePath().toString()) + "'";
            if( tables.length != 0 )
                params[4] = String.join(", ", tables);

            String putTableScript = "data.save(result,'" + parameters.getOutputCollection().getCompletePath().toString() + "/');";

            return String.join("", getTableScript) + ( "var result = microarray.meta(" + String.join(", ", params) + ");\n" )
                    + putTableScript;
        }
        catch( Exception ex )
        {
            return "Error during java script generating: " + ex.getMessage();
        }
    }

    /**
     * Analyzing microarray data from file Input results sends to file Output
     * Nexperiment - Number of experiment. result - Clone[]
     */
    @Override
    public TableDataCollection getAnalyzedData() throws Exception
    {
        prepareParameters();
        super.init();
        TableDataCollection result = super.getAnalyzedData();
        tempPath.remove();
        return result;
    }

    private void prepareParameters() throws Exception
    {
        TableDataCollection table = appendExperiments();
        HypergeometricAnalysisParameters params = new HypergeometricAnalysisParameters();
        getParameters().setBv(0.0);
        getParameters().setThreshold(new Threshold(parameters, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        getParameters().setExperimentData(new ColumnGroup(params, table.getCompletePath()));
        getParameters().setDetailed(false);
        super.setParameters(parameters);
    }

    private TableDataCollection appendExperiments() throws Exception
    {
        MultipleTableJoin joinAnalysis = new MultipleTableJoin(null, "");
        MultipleTableJoinParameters parameters = joinAnalysis.getParameters();
        parameters.setJoinType(1);
        parameters.setTablePaths(getParameters().getTablePaths());
        tempPath = createTempPath(parameters.getOutputPath());
        parameters.setOutputPath(tempPath);
        return joinAnalysis.justAnalyzeAndPut();
    }

    private DataElementPath createTempPath(DataElementPath outputPath)
    {
        String pathString = outputPath.toString() + "_temp";
        DataElementPath newPath = DataElementPath.create(pathString);
        int counter = 1;
        while( newPath.exists() )
        {
            newPath = DataElementPath.create(pathString + counter);
            counter++;
        }
        return newPath;
    }

    @Override
    public void init()
    {

    }
}
