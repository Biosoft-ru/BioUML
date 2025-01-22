package biouml.plugins.sbol._test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.sbol.SbolImportProperties;
import biouml.plugins.sbol.SbolImporter;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.PathView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.editor.ViewPane;

public class SbolTest extends AbstractBioUMLTest
{
    public static final String filesDir = "../data/test/biouml/plugins/sbol/files";
    public static final String repositoryPath = "../data/test/biouml/plugins/sbol/repo";
    private DataCollection module;

    public SbolTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SbolTest.class.getName());

        suite.addTest(new SbolTest("testReadDiagram"));
        suite.addTest(new SbolTest("testCreateView"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.unregisterAllRoot();
        DataCollection<?> root = CollectionFactory.createRepository(repositoryPath);
        assertNotNull("Can not load repository", root);
        module = (DataCollection<?>) root;
    }

    public void testReadDiagram() throws Exception
    {
        SbolImporter importer = new SbolImporter();
        
        File file = new File(filesDir + "/canv_test1_sbol2.xml");
        SbolImportProperties props = (SbolImportProperties) importer.getProperties(module, file, "canv_test1_sbol2");

        DataElement de = importer.doImport(module, file, props.getDiagramName(), null, null);
        assertNotNull("Diagram was not imported", de);
        assertTrue("Not a diagram", de instanceof Diagram);
        Diagram diagram = (Diagram) de;

        diagram.stream().forEach(elem -> System.out.println(elem.getName() + " " + elem.getKernel().getType()));
        assertEquals(1, diagram.getSize());
    }

    public void testCreateView() throws Exception
    {
        SbolImporter importer = new SbolImporter();

        File file = new File(filesDir + "/chromosomal_and_circular.xml");
        SbolImportProperties props = (SbolImportProperties) importer.getProperties(module, file, "chromosomal_and_circular");

        DataElement de = importer.doImport(module, file, props.getDiagramName(), null, null);
        assertNotNull("Diagram was not imported", de);
        assertTrue("Not a diagram", de instanceof Diagram);
        Diagram diagram = (Diagram) de;

        JFrame frame = new JFrame();
        frame.setSize(600, 400);
        frame.setLocation(800, 500);

        JPanel mainPanel = new JPanel(new GridLayout(0, 2));

        Graphics2D g = ApplicationUtils.getGraphics();

        //        //view
        diagram.setView(null);
        CompositeView view = diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, g);
        //        CompositeView view = new CompositeView();
        //        BoxView shapeView = new BoxView(null, new Brush(Color.cyan), new Rectangle(0, 0, 200, 40));
        //        view.add(shapeView);
        //
        //        GeneralPath path2 = new GeneralPath();
        //        path2.moveTo(20, 0);
        //        path2.quadTo(3, 2, 0, 5);
        //        path2.quadTo(2, 8, 10, 10);
        //        Pen boldPen = new Pen(2, Color.black);
        //        view.add(new PathView(boldPen, path2), CompositeView.X_CC | CompositeView.Y_CC, new Point(-50, 0));
        //
        //
        //        GeneralPath path = new GeneralPath();
        //        path.moveTo(0, 0);
        //        path.quadTo(30, 0, 30, 8);
        //        path.quadTo(30, 13, 8, 14);
        //        view.add(new PathView(boldPen, path), CompositeView.X_CC | CompositeView.Y_CC, new Point(0, 0));
        //
        //        GeneralPath path3 = new GeneralPath();
        //        path3.moveTo(0, 0);
        //        path3.quadTo(30, 0, 30, 6);
        //        path3.curveTo(30, 10, 2, 10, 2, 14);
        //        path3.quadTo(2, 18, 32, 18);
        //        view.add(new PathView(boldPen, path3), CompositeView.X_CC | CompositeView.Y_CC, new Point(50, 0));

        ViewPane pane = new ViewPane();
        pane.setView(view);
        mainPanel.add(pane);

        JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while ( true )
        {
            Thread.sleep(1000);
        }

    }
}
