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

public abstract class WorkflowScriptGenerator
{
    private Template velocityTemplate = null;

    public abstract String getTemplateName();
    public abstract String getTemplatePath();
    public abstract WorkflowVelocityHelper getVelocityHelper(Diagram diagram);
    
    /**
     * Generates Workflow script
     */
    public String generate(Diagram diagram) throws Exception
    {
        diagram = preprocess(diagram);
        
        if( velocityTemplate == null )
            initTemplate( getTemplateName(), getTemplatePath() );

        VelocityContext context = new VelocityContext();
        context.put( "helper", getVelocityHelper(diagram) );
        context.put( "diagram", diagram );
        addTimeStamp( context );
        StringWriter sw = new StringWriter();
        velocityTemplate.merge( context, sw );
        return sw.toString();
    }

    /**
     * Creates velocityTemplate
     */
    private void initTemplate( String templateName, String templatePath) throws Exception
    {
        InputStream inputStream = getClass().getResourceAsStream( templatePath );
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        SimpleNode node = runtimeServices.parse( new InputStreamReader( inputStream ), templateName );
        velocityTemplate = new Template();
        velocityTemplate.setEncoding( "UTF-8" );
        velocityTemplate.setRuntimeServices( runtimeServices );
        velocityTemplate.setData( node );
        velocityTemplate.initDocument();
        Velocity.init();
    }

    /**
     * Adds creation time stamp to velocity context
     */
    private void addTimeStamp(VelocityContext context)
    {
        String pattern = "yyyy.MM.dd HH:mm:ss";
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat( pattern );
        String creationTime = format.format( date );
        context.put( "creationTime", creationTime );
    }
    
    /**
     * Does nothing by default
     * @return changed diagram appropriate for script generation
     */
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        return diagram;
    }
}