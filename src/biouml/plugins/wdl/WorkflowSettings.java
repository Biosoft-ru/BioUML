package biouml.plugins.wdl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.Node;
import ru.biosoft.access.FileExporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TextDataElement;

public class WorkflowSettings extends Option
{
    private DataElementPath outputPath;
    private boolean useJson = false;
    private DataElementPath json;
    private DynamicPropertySet parameters = new DynamicPropertySetSupport();

    public static String NEXTFLOW_TYPE = "Nextflow";
    public static String CWL_TYPE = "CWL";

    private String executionType = NEXTFLOW_TYPE;

    public void initParameters(Diagram diagram)
    {
        List<Node> externalParameters = WorkflowUtil.getExternalParameters( diagram );
        for( Node externalParameter : externalParameters )
        {
            String type = WorkflowUtil.getType( externalParameter );
            String name = WorkflowUtil.getName( externalParameter );
            Object value = WorkflowUtil.getExpression( externalParameter );
            Class clazz = String.class;
            if( type.equals( "File" ) || type.equals( "Array[File]" ) )
            {
                if( value != null )
                    value = DataElementPath.create( value.toString() );
                clazz = DataElementPath.class;
            }
            DynamicProperty dp = new DynamicProperty( name, clazz, value );
            parameters.add( dp );
        }
    }

    public static Set<String> getFileInputs(String json)
    {
        Set<String> result = new HashSet<>();
        JSONObject map = new JSONObject( json );
        for( String name : map.keySet() )
        {
            Object object = map.get( name );
            if( object instanceof JSONObject )
            {
                Object cls = ( (JSONObject)object ).get( "class" );
                if( cls.equals( "File" ) )
                {
                    Object path = ( (JSONObject)object ).get( "path" );
                    result.add( path.toString() );
                }
            }
        }
        return result;
    }

    public void exportCollections(String outputDir) throws Exception
    {
        if( useJson )
        {
            DataElement de = getJson().getDataElement();
            if( de instanceof TextDataElement )
            {
                DataCollection dc = de.getOrigin();
                String content = ( (TextDataElement)de ).getContent();

                Set<String> fileInputs = getFileInputs( content );
                for( String fileInput : fileInputs )
                {
                    DataElement parameterDe = dc.get( fileInput );
                    if( parameterDe != null )
                    {
                        System.out.println( "Exporting " + fileInput );
                        WorkflowUtil.export( parameterDe, new File( outputDir ) );
                    }
                }

                String[] parameters = content.replace( "{", "" ).replace( "}", "" ).replace( "\"", "" ).split( "," );
                for( String parameter : parameters )
                {
                    try
                    {
                        String name = parameter.split( ":" )[1];
                        name = name.trim().replace( "\n", "").replace( ",", "");
                        DataElement parameterDe = dc.get( name );
                        if( parameterDe != null )
                            WorkflowUtil.export( parameterDe, new File( outputDir ) );
                    }
                    catch( Exception ex )
                    {

                    }
                }
            }
            return;
        }
        for( DynamicProperty dp : parameters )
        {
            if( dp.getValue() instanceof DataElementPath )
            {
                DataElement de = ( (DataElementPath)dp.getValue() ).getDataElement();
                WorkflowUtil.export( de, new File( outputDir ) );
            }
        }
    }

    public File generateParametersJSON(String outputDir) throws Exception
    {
        File json = new File( outputDir, "parameters.json" );
        if( isUseJson() )
        {
            DataElement de = getJson().getDataElement();
            FileExporter exporter = new FileExporter();
            exporter.doExport( de, json );
            return json;
        }

        try (BufferedWriter bw = new BufferedWriter( new FileWriter( json ) ))
        {
            bw.write( "{\n" );
            boolean first = true;
            for( DynamicProperty dp : parameters )
            {
                Object value = dp.getValue();
                if( value instanceof DataElementPath dep )
                    value = "\"" + dep.getName() + "\"";
                else
                {
                    String valueStr = value.toString();
                    if( valueStr.startsWith( "\"" ) && valueStr.endsWith( "\"" ) )
                        value = valueStr;
                    else
                        value = "\"" + valueStr + "\"";
                }
                if( !first )
                    bw.write( "," );
                first = false;
                bw.write( "\"" + dp.getName() + "\"" + " : " + value + "\n" );
            }
            bw.write( "}\n" );
        }
        return json;
    }

    public File generateParametersJSON2(String outputDir) throws Exception
    {
        File json = new File( outputDir, "parameters.json" );
        if( isUseJson() )
        {
            DataElement de = getJson().getDataElement();
            FileExporter exporter = new FileExporter();
            exporter.doExport( de, json );
            return json;
        }
        return json;
    }

    @PropertyName ( "Parameters" )
    public DynamicPropertySet getParameters()
    {
        return parameters;
    }

    public void setParameters(DynamicPropertySet parameters)
    {
        Object oldValue = this.parameters;
        this.parameters = parameters;
        firePropertyChange( "parameters", oldValue, parameters );
    }

    @PropertyName ( "Output path" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange( "outputPath", oldValue, outputPath );
    }

    @PropertyName ( "From json" )
    public boolean isUseJson()
    {
        return useJson;
    }

    public boolean isNotJson()
    {
        return !useJson;
    }

    public void setUseJson(boolean useJson)
    {
        boolean oldValue = this.useJson;
        this.useJson = useJson;
        firePropertyChange( "useJson", oldValue, useJson );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Parameters json" )
    public DataElementPath getJson()
    {
        return json;
    }

    public void setJson(DataElementPath json)
    {
        Object oldValue = this.json;
        this.json = json;
        firePropertyChange( "json", oldValue, json );
    }

    @PropertyName ( "Execution Type" )
    public String getExecutionType()
    {
        return executionType;
    }

    public void setExecutionType(String executionType)
    {
        Object oldValue = this.executionType;
        this.executionType = executionType;
        firePropertyChange( "executionType", oldValue, executionType );
    }
}