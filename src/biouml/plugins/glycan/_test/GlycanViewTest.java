package biouml.plugins.glycan._test;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Node;
import biouml.model._test.ViewTestCase;
import biouml.plugins.glycan.GlycanViewBuilder;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Substance;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

public class GlycanViewTest extends ViewTestCase
{
    public GlycanViewTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(GlycanViewTest.class.getName());

        suite.addTest(new GlycanViewTest("test"));

        return suite;
    }

    final static DataElementPath COLLECTION_NAME = DataElementPath.create("databases/test/Diagrams");

    public void test() throws Exception
    {
        CollectionFactory.createRepository("../data");
        Application.setPreferences(new Preferences());
        DataCollection<DataElement> collection = COLLECTION_NAME.getDataCollection();

        DiagramType type = new PathwaySimulationDiagramType();
        Diagram diagram = type.createDiagram(collection, "testGlu", new DiagramInfo("testGlu"));

        Node node = new Node(diagram, new Substance(diagram, "gl"));
        node.getAttributes().add(
                new DynamicProperty(GlycanViewBuilder.GLYCAN_STRUCTURE, String.class,
                        "Ga3Ma2Ma3(ANa3(NNa6)Ab4Ma2Ma3(Ma2Ma6)Ma6)Mb4GNb4GNa6F"));
        node.getAttributes().add(new DynamicProperty("fullGlycanView", Boolean.class, true));
        diagram.put(node);

        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)diagram.getView());
        assertView(pane, diagram.getName());

        collection.put(diagram);
    }

}
