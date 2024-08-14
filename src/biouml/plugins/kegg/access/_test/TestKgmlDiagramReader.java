package biouml.plugins.kegg.access._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model._test.ViewTestCase;
import biouml.plugins.kegg.KeggPathwayDiagramViewBuilder;
import biouml.plugins.kegg.access.KgmlDiagramReader;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;


public class TestKgmlDiagramReader extends ViewTestCase
{
    public TestKgmlDiagramReader(String name)
    {
        super(name);
        File configFile = new File( "./biouml/plugins/kegg/access/_test/testKgmlDiagramReader.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestKgmlDiagramReader.class.getName() );

        suite.addTest(new TestKgmlDiagramReader("testLoadModule"));
        suite.addTest(new TestKgmlDiagramReader("testDiagramView_map00031"));
//        suite.addTest(new TestKgmlDiagramReader("testDiagramView_map00010"));
//        suite.addTest(new TestKgmlDiagramReader("testDiagramView_eco00770"));
        //suite.addTest(new TestKgmlDiagramReader("testPerformance"));

        return suite;
    }

    private static Module module;
    public void testLoadModule() throws Exception
    {
        module = (Module)CollectionFactory.createRepository("../data/kegg pathways");
        assertNotNull("Can not load module", module);
    }

    public void testDiagramView_map00010()
    {
      testDiagramView("../data/kegg pathways/diagrams/map00010.xml");
    }

    public void testDiagramView_map00031()
    {
        testDiagramView("../data/kegg pathways/diagrams/map00031.xml");
    }

    public void testDiagramView_map00770()
    {
        testDiagramView("../data/kegg pathways/diagrams/map00770.xml");
    }

    public void testDiagramView_eco00770()
    {
        testDiagramView("../data/kegg pathways/diagrams/eco00770.xml");
    }

    private void testDiagramView(String path)
    {
        try
        {
            KgmlDiagramReader reader = new KgmlDiagramReader(new File(path));

            long time = System.currentTimeMillis();
            Diagram diagram = reader.read(null, null, module);
            System.out.println(path + " parsed in " + (System.currentTimeMillis() - time) + " ms.");

            KeggPathwayDiagramViewBuilder viewBuilder = new KeggPathwayDiagramViewBuilder();
            CompositeView view = viewBuilder.createDiagramView(diagram, getGraphics());
            ViewPane pane = new ViewPane();
            pane.setView(view);
            assertView(pane, path);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void testPerformance() throws Exception
    {
        testSpeed("../data/kegg pathways/diagrams/map00010.xml");
        testSpeed("../data/kegg pathways/diagrams/map00031.xml");
    }

    private void testSpeed(String path) throws Exception
    {
        System.err.println("Diagram: " + path);
        KgmlDiagramReader reader = new KgmlDiagramReader(new File(path));
        for (int i = 0; i < 5; i++)
        {
            Diagram diagram = reader.read(null, null, module);
        }
    }
}
