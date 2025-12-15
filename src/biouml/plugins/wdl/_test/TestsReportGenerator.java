package biouml.plugins.wdl._test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import biouml.plugins.wdl._test.TestNextflow.TestResult;

public class TestsReportGenerator
{
    private Template velocityTemplate = null;
    private static String TEMPLATE_PATH = "../resources/testsReport.vm";
    private static String TEMPLATE_NAME = "Report template";

    public String getTemplateName()
    {
        return TEMPLATE_NAME;
    }

    public String getTemplatePath()
    {
        return TEMPLATE_PATH;
    }

    /**
     * Generates HTML report for one model
     */
    public String generate(List<TestResult> results, File testDir) throws Exception
    {
        if( velocityTemplate == null )
            initTemplate( getTemplateName(), getTemplatePath() );
        StringWriter sw = new StringWriter();
        velocityTemplate.merge( createContext( results ), sw );
        return sw.toString();
    }

    private VelocityContext createContext(List<TestResult> results) throws IOException
    {
        VelocityContext context = new VelocityContext();
        context.put( "results", results );
        return context;
    }

    /**
     * Creates velocityTemplate
     */
    private void initTemplate(String templateName, String templatePath) throws Exception
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
}