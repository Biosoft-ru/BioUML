package ru.biosoft.analysis;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.table.export.TableElementExporter.TableExporterProperties;
import ru.biosoft.util.TempFiles;

@ClassIcon("resources/cluster-analysis.gif")
public class ClusterAnalysis extends MicroarrayAnalysis<ClusterAnalysisParameters>
{
    public ClusterAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new ClusterAnalysisParameters());
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            ClusterAnalysisParameters parameters = (ClusterAnalysisParameters)parametersObject;

            StringBuffer getSourceScript = new StringBuffer();
            String[] params = {"null", "", "", "2", ""};

            if( parameters.getExperiment() != null )
            {
                getSourceScript.append("var experiment = data.get('"
                        + StringEscapeUtils.escapeJavaScript(parameters.getExperiment().getCompletePath().toString()) + "');\n");
                params[0] = "experiment";
            }

            if( parameters.getExperimentData().getColumns() != null )
                params[1] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getExperimentData().getNamesDescription()) + "'";
            if( parameters.getMethod() != null )
                params[2] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getMethod()) + "'";
            if( parameters.getClusterCount() != null )
                params[3] = parameters.getClusterCount().toString();
            if( parameters.getOutputTablePath() != null )
                params[4] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputTablePath().toString()) + "'";

            String putTableScript = "data.save(result,'" + parameters.getOutputCollection().getCompletePath().toString() + "/');";

            return getSourceScript.append("var result = microarray.cluster(" + String.join(", ", params) + ");\n").append(
                    putTableScript).toString();
        }
        catch( Exception ex )
        {
            return "Error during java script generating" + ex.getMessage();
        }
    }
    @Override
    public void validateParameters()
    {
        ColumnGroup experimentData = parameters.getExperimentData();
        if( experimentData == null || experimentData.getColumns().length == 0 )
            throw new IllegalArgumentException("Please specify experiment columns");
        if( experimentData.getTable() == null )
            throw new IllegalArgumentException("Please specify experiment table");

        DataElementPath path = parameters.getOutputTablePath();
        if( path == null || path.optParentCollection() == null || path.getParentCollection().getName().equals( "" ) )
            throw new IllegalArgumentException("Please specify output collection");

        if( path.getName().equals("") )
            throw new IllegalArgumentException("Please specify output name");
        if( parameters.getExperimentData().getTablePath().equals(parameters.getOutputTablePath()))
            throw new IllegalArgumentException("Output is the same as the input. Please specify different output name.");
    }

    @Override
    public TableDataCollection getAnalyzedData() throws Exception
    {
        TableDataCollection table = parameters.getExperimentData().getTable();
        String[] columnNames = parameters.getExperimentData().getNames();

        File rInput = exportTable( table, columnNames );
        incPreparedness(5);
        ScriptEnvironment env = new LogScriptEnvironment( log );
        Map<String, Object> inputVars = new HashMap<>();
        inputVars.put( "path", rInput.getAbsolutePath() );
        Map<String, Object> outVars = new HashMap<>();
        outVars.put( "cluster", null );
        
        String rCode = generateRcode();
        SecurityManager.runPrivileged( () -> {
            ScriptDataElement script = ScriptTypeRegistry.createScript( "R", null, rCode );
            script.execute( rCode, env, inputVars, outVars, false );
            return null;
        } );
        incPreparedness(70);
        int[] clusters = (int[])outVars.get( "cluster" );
        
        TableDataCollection result = assignClusters(clusters, table, columnNames);
        incPreparedness(100);
        return result; 
    }

    protected File exportTable(TableDataCollection table, String[] columnNames) throws Exception
    {
        File rInput = TempFiles.file("Rinput.txt");
        TableElementExporter exporter = new TableElementExporter();
        Properties prop = new Properties();
        prop.put(DataElementExporterRegistry.SUFFIX, "txt");
        exporter.init(prop);
        
        TableExporterProperties exportProperties = (TableExporterProperties)exporter.getProperties(table, rInput);
        Column[] columns = StreamEx.of(columnNames).map( name -> new Column(null, name) ).toArray( Column[]::new );
        exportProperties.setColumns(columns);
        
        exporter.doExport(table, rInput);
        return rInput;
    }
    
    protected String generateRcode() throws Exception
    {
        return "data <- read.table(path, header=TRUE, sep='\\t')\n" +
               "data <- data[,-1]\n" +
               "kmeans <- kmeans(data," + parameters.getClusterCount() + ",1000,1,\"" + parameters.getMethod() + "\")\n" +
               "cluster <- kmeans$cluster\n";
    }

    private TableDataCollection assignClusters(int[] clusterAssignments, TableDataCollection table, String[] columnNames)
    {
        TableDataCollection result = parameters.getOutputTable();//TableDataCollectionUtils.createTableDataCollection(parameters.getOutputCollection(), parameters.getOutputName());

        result.getColumnModel().addColumn("Cluster", Integer.class);
        ColumnModel columnModel = table.getColumnModel();
        for( String columnName: columnNames )
        {
            TableColumn column = columnModel.getColumn(columnName);
            result.getColumnModel().addColumn(column.getName(), column.getType());
        }
        
        int[] colIndices = StreamEx.of( columnNames ).mapToInt( columnModel::getColumnIndex ).toArray();

        for( int i = 0; i < table.getSize(); i++ )
        {
            String key = table.getName(i);
            Object[] rowValues = TableDataCollectionUtils.getRowValues(table, key);
            
            Object[] values = IntStreamEx.of( colIndices ).elements( rowValues ).prepend( clusterAssignments[i] ).toArray();

            TableDataCollectionUtils.addRow(result, key, values);
        }
        return result;
    }
}
