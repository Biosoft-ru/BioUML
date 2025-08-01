package biouml.plugins.wdl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import biouml.model.Diagram;

public class NextFlowGenerator
{
    protected static String TEMPLATE_PATH = "resources/nextflow.vm";
    private Template velocityTemplate;

    public String generateNextFlow(Diagram diagram) throws Exception
    {
        try
        {
            InputStream inputStream = getClass().getResourceAsStream( TEMPLATE_PATH );

            if( velocityTemplate == null )
            {
                RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
                SimpleNode node = runtimeServices.parse( new InputStreamReader( inputStream ), "Next Flow template" );
                velocityTemplate = new Template();
                velocityTemplate.setEncoding( "UTF-8" );
                velocityTemplate.setRuntimeServices( runtimeServices );
                velocityTemplate.setData( node );
                velocityTemplate.initDocument();
                Velocity.init();
            }
            VelocityContext context = new VelocityContext();

            context.put( "helper", new NextFlowVelocityHelper( diagram ) );

            //Creation time
            String pattern = "yyyy.MM.dd HH:mm:ss";
            Calendar calendar = Calendar.getInstance();
            Date date = calendar.getTime();
            SimpleDateFormat format = new SimpleDateFormat( pattern );
            String creationTime = format.format( date );

            context.put( "creationTime", creationTime );
            context.put( "diagram", diagram );

            StringWriter sw = new StringWriter();
            velocityTemplate.merge( context, sw );
            return sw.toString();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return "";
        }

    }
}
