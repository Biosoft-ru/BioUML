package biouml.plugins.gxl._test;

import junit.framework.TestSuite;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

import biouml.model.DiagramViewBuilder;
import biouml.model._test.ViewTestCase;
import biouml.workbench.graph.DiagramToGraphTransformer;

/** Batch unit test for biouml.model package. */
public class GxlReadWrite extends ViewTestCase
{
    /** Standart JUnit constructor */
    public GxlReadWrite(String name)
    {
        super(name);
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(GxlReadWrite.class.getName());

        suite.addTest(new AutoTest("testReadDiagram"));
        suite.addTest(new GxlReadWrite("testDiagramView") );

        return suite;
    }



    public void testDiagramView()
    {
        ForceDirectedLayouter layouter = new ForceDirectedLayouter();
        // TODO: check if graphics is valid
        DiagramToGraphTransformer.layout(AutoTest.diagram, layouter);

        DiagramViewBuilder builder = AutoTest.diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(AutoTest.diagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView( (CompositeView)AutoTest.diagram.getView() );
        assertView(pane, AutoTest.diagram.getName());

    }

}
