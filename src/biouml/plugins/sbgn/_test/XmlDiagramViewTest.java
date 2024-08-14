package biouml.plugins.sbgn._test;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JFrame;

import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.plugins.sbgn.SBGNXmlReader;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

public class XmlDiagramViewTest extends TestCase
{
    public static final String repositoryPath = "../data_resources";
    public static final String testFileName = "biouml/plugins/sbgn/_test/BIOMD0000000006.sbgn.xml";
    
    protected ViewPane viewPane;

    /** Standart JUnit constructor */
    public XmlDiagramViewTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(XmlDiagramViewTest.class);
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(XmlDiagramViewTest.class.getName());
        suite.addTest(new XmlDiagramViewTest("testXmlDiagram"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testXmlDiagram() throws Exception
    {
        JFrame frame = new JFrame("XmlDiagram test");
        frame.show();

        Container content = frame.getContentPane();
        viewPane = new ViewPane();
        
        CollectionFactory.createRepository(repositoryPath);
        
        SBGNXmlReader reader = new SBGNXmlReader(null, "test", null);
        File testFile = new File(testFileName);
        assertTrue("Can not find test file: "+testFileName, testFile.exists());
        try( FileInputStream inputStream = new FileInputStream( testFile ) )
        {
            Diagram diagram = reader.read( inputStream );

            Graphics g = frame.getGraphics();
            DiagramViewBuilder dvb = diagram.getType().getDiagramViewBuilder();
            CompositeView view = dvb.createDiagramView( diagram, g );

            viewPane.setView( view );
        }

        content.add(viewPane);
        frame.setSize(600, 400);
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                System.exit(0);
            }
        });
        while( true )
        {
            Thread.sleep(100);
        }
    }
}