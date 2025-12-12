package biouml.plugins.physicell._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.PhysicellDiagramType;
import biouml.plugins.physicell.PhysicellImporter;
import biouml.plugins.physicell.SimulationEngineHelper;
import biouml.plugins.physicell.SubstrateProperties;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class TestSummary extends AbstractBioUMLTest
{
    public static final String repositoryPath = "../data/test/biouml/plugins/physicell/repo";
    private DataCollection module;

    public TestSummary(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestSummary.class.getName() );
        suite.addTest( new TestSummary( "testGeneralSummary" ) );
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.unregisterAllRoot();
        DataCollection<?> root = CollectionFactory.createRepository( repositoryPath );
        assertNotNull( "Can not load repository", root );
        module = (DataCollection<?>)root;
    }


    public static void main(String ... strings) throws Exception
    {
//        testGeneral();
//        testSubstrate();
        testCellDefinition();
    }


    private static void testGeneral() throws Exception
    {
        Diagram diagram = loadDiagram();
        System.out.println( generateSummary( diagram ) );
    }

    private static void testSubstrate() throws Exception
    {
        Diagram diagram = loadDiagram();
        SubstrateProperties substrate = diagram.getRole( MulticellEModel.class ).getSubstrates().get( 0 );
        DiagramElement de = substrate.getDiagramElement();
        System.out.println( generateSummary( de ) );
    }
    
    private static void testCellDefinition() throws Exception
    {
        Diagram diagram = loadDiagram();
        CellDefinitionProperties cellDefinition = diagram.getRole(  MulticellEModel.class  ).getCellDefinitions().get( 2 );
        DiagramElement de = cellDefinition.getDiagramElement();
        System.out.println( generateSummary( de ) );
    }

    private static Diagram loadDiagram() throws Exception
    {
        File f = new File(
                "C:\\Users//Damag\\git\\JPhysiCell\\src\\main\\resources\\ru\\biosoft\\physicell\\sample_projects\\biorobots\\config\\PhysiCell_settings.xml" );
        PhysicellImporter importer = new PhysicellImporter();
        importer.getProperties( null, f, "" );
        return importer.read( f, null, "test" );
    }

    private static String getTemplate() throws Exception
    {
        InputStream inputStream = PhysicellDiagramType.class.getResourceAsStream( "resources/modelSummary.vm" );
        return new BufferedReader(new InputStreamReader(inputStream))
                .lines().collect(Collectors.joining("\n"));
        
//        return PhysicellDiagramType.class.getResource( "resources/modelSummary.vm" ).openStream();
//        File template = new File( "C:\\Users\\Damag\\eclipse_2024_6\\BioUML\\src\\biouml\\plugins\\physicell\\resources\\modelSummary.vm" );
//        return new FileInputStream( template );
    }

    private static String generateSummary(DiagramElement de) throws Exception
    {
        String templateContent = getTemplate();
        System.out.println( templateContent );
        StringReader reader = new StringReader(templateContent);
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        SimpleNode node = runtimeServices.parse( reader, "Summary template" );
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

}
