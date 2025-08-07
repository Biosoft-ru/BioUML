package biouml.plugins.wdl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.Node;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

public class WorkflowSettings
{
    private DataElementPath outputPath;
    private DynamicPropertySet parameters = new DynamicPropertySetSupport();

    public WorkflowSettings()
    {
        //System.out.println( "Load" );
    }

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
        for( DynamicProperty dp : parameters )
        {
            if( dp.getValue() instanceof DataElementPath )
            {
                DataElement de = ( (DataElementPath)dp.getValue() ).getDataElement();
                WDLUtil.export( de, new File( outputDir ) );
            }
        }
    }

    public File generateParametersJSON(String outputDir) throws IOException
    {
        File json = new File( outputDir, "parameters.json" );
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
        this.parameters = parameters;
    }

    @PropertyName ( "Output path" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        this.outputPath = outputPath;
    }
}