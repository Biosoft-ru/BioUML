package biouml.plugins.wdl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

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

    public void initParameters(Diagram diagram)
    {
        List<Node> externalParameters = WDLUtil.getExternalParameters( diagram );
        for( Node externalParameter : externalParameters )
        {
            String type = WDLUtil.getType( externalParameter );
            String name = WDLUtil.getName( externalParameter );
            Object value = WDLUtil.getExpression( externalParameter );
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

    public void exportCollections(String outputDir) throws Exception
    {
        if( useJson )
        {
            DataElement de = getJson().getDataElement();
            if( de instanceof TextDataElement )
            {
                DataCollection dc = de.getOrigin();
                String content = ( (TextDataElement)de ).getContent();
                String[] parameters = content.replace( "{", "" ).replace( "}", "" ).replace( "\"", "" ).split( "," );
                for( String parameter : parameters )
                {
                    try
                    {
                        String name = parameter.split( ":" )[1];
                        DataElement parameterDe = dc.get( name );
                        if( parameterDe != null )
                            WDLUtil.export( parameterDe, new File( outputDir ) );
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
                WDLUtil.export( de, new File( outputDir ) );
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
                    value = "\"" + value.toString() + "\"";
                if( !first )
                    bw.write( "," );
                first = false;
                bw.write( "\"" + dp.getName() + "\"" + " : " + value + "\n" );
            }
            bw.write( "}\n" );
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
}