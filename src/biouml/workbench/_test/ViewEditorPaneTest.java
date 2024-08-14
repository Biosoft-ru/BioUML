package biouml.workbench._test;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.workbench.diagram.DiagramDocument;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.editor.DefaultViewEditorHelper;
import ru.biosoft.graphics.editor.ViewEditorPane;

/**
 * Batch unit test for biouml.model package.
 */
public class ViewEditorPaneTest extends TestCase
{
    public static final String repositoryPath = "./data";

    /** Standart JUnit constructor */
    public ViewEditorPaneTest( String name )
    {
        super(name);
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main( String[] args )
    {
        if ( args != null && args.length>0 && args[0].startsWith( "text" ) )
            { junit.textui.TestRunner.run( suite() ); }
        else { junit.swingui.TestRunner.run( ViewEditorPaneTest.class ); }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite ( ViewEditorPaneTest.class.getName() );
        //suite.addTest( AutoTest.suite() );

        suite.addTest(new ViewEditorPaneTest("testViewEditorPane"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //



    private CompositeView getSimpleView ()
    {
        CompositeView compositeA = new CompositeView();
        BoxView rectA = new BoxView(new Pen(2, Color.black), new Brush(Color.yellow), 0, 0, 800, 400);
        CompositeView compositeB = new CompositeView();
        BoxView rectB = new BoxView(new Pen(2, Color.black), new Brush(Color.red), 100, 100, 300, 200);
        EllipseView ellC = new EllipseView(new Pen(2, Color.black), new Brush(Color.blue), 120, 120, 150, 100);
        EllipseView ellD = new EllipseView(new Pen(2, Color.black), new Brush(Color.green), 500, 200, 150, 100);
        ArrowView arrowE = new ArrowView(new Pen(2, Color.black), new Brush(Color.black), 270, 170, 500, 250, 0, ArrowView.ARROW_TIP);

        rectA.setActive(true);
        rectB.setActive(true);
        ellC.setActive(true);
        ellD.setActive(true);
        arrowE.setActive(true);

        compositeB.add(rectB);
        compositeB.add(ellC);
        compositeA.add(rectA);
        compositeA.add(ellD);
        compositeA.add(compositeB);
        compositeA.add(arrowE);
        return compositeA;
    }



    public void testViewEditorPane() throws Exception
    {
        JFrame frame = new JFrame ("ViewEditorPane test");
        frame.show();

        ViewEditorPane viewEditorPane = new ViewEditorPane(new DefaultViewEditorHelper());
        viewEditorPane.setView( getSimpleView() );
        viewEditorPane.scale(1.5, 1.5);

        frame.setContentPane(viewEditorPane);
        frame.pack();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed( WindowEvent e )
            {
                System.exit(0);
            }
        });
        while(true)
        {
            Thread.sleep(100);
        }
    }

    //boolean close;
    public void testViewEditorPaneTabacco() throws Exception
    {
        JFrame frame = new JFrame ("ViewEditorPane test");
        frame.show();

        CollectionFactory.createRepository(repositoryPath);

        String diagramPath = "databases/GeneNet/Diagrams/tobacco";

        DiagramDocument diagramDocument;
        Diagram diagram = (Diagram)CollectionFactory.getDataCollection(diagramPath);

        ViewEditorPane viewEditorPane = new ViewEditorPane(new DefaultViewEditorHelper());

        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        CompositeView diagramView = builder.createDiagramView( diagram , frame.getGraphics() );

        viewEditorPane.setView( diagramView , new Point( 10 , 10 ) );

        frame.setContentPane(viewEditorPane);
        frame.pack();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed( WindowEvent e )
            {
                //close = true;
                System.exit(0);
            }
        });
        while(true)
        {
            Thread.sleep(100);
        }
/*
        while( !close )
        {
            Thread.sleep(100);
        }
*/
    }
}