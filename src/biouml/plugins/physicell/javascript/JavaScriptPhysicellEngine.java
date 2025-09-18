package biouml.plugins.physicell.javascript;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.util.DiagramImageGenerator;
import biouml.plugins.physicell.PhysicellDiagramType;
import biouml.plugins.physicell.SimulationEngineHelper;
import biouml.plugins.physicell.document.PhysicellSimulationResult;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;

public class JavaScriptPhysicellEngine  extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger( JavaScriptPhysicellEngine.class.getName() );

    Diagram diagram;

    public void loadDiagram(String path)
    {
        try
        {
            diagram = DataElementPath.create( path ).getDataElement( Diagram.class );
        }
        catch( Exception ex )
        {
            log.info( "Error during loading diagram " + path + ": " + ex.getMessage() );
        }
    }

    public JavaScriptPhysicellResult loadPhysicellResult(String path)
    {
        DataCollection dc = DataElementPath.create( path ).getDataCollection();
        return loadPhysicellResult( dc );
    }

    public JavaScriptPhysicellResult loadPhysicellResult(DataCollection dc)
    {
        try
        {
            return new JavaScriptPhysicellResult( new PhysicellSimulationResult( "temp", dc ) );
        }
        catch( Exception ex )
        {
            log.info( "Error during result laoding " + ex.getMessage() );
            return null;
        }
    }

    public void showDiagram(Diagram diagram)
    {
        BufferedImage image = DiagramImageGenerator.generateDiagramImage( diagram );
        ScriptEnvironment environment = Global.getEnvironment();
        if( environment != null )
            environment.showGraphics( image );
    }

    public String showSummary(DiagramElement de) throws Exception
    {
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        SimpleNode node = runtimeServices.parse( new InputStreamReader( getTemplate() ), "Summary template" );
        Template velocityTemplate = new Template();
        velocityTemplate.setRuntimeServices( runtimeServices );
        velocityTemplate.setData( node );
        velocityTemplate.initDocument();
        Velocity.init();
        VelocityContext context = new VelocityContext();
        context.put( "physicellEngine", new SimulationEngineHelper() );
        context.put( "de", de );
        StringWriter sw = new StringWriter();
        velocityTemplate.merge( context, sw );
        return sw.toString();
    }

    private InputStream getTemplate() throws Exception
    {
        return PhysicellDiagramType.class.getResource( "resources/modelSummary.vm" ).openStream();
    }
}
