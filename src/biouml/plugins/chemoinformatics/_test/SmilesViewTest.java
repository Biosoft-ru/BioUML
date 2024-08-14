package biouml.plugins.chemoinformatics._test;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Node;
import biouml.model._test.ViewTestCase;
import biouml.plugins.chemoinformatics.SmilesViewBuilder;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Specie;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;

public class SmilesViewTest extends ViewTestCase
{
    public SmilesViewTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SmilesViewTest.class.getName());

        suite.addTest(new SmilesViewTest("test"));

        return suite;
    }

    final static DataElementPath COLLECTION_NAME = DataElementPath.create("databases/test/Diagrams");


    public void test() throws Exception
    {
        CollectionFactory.createRepository("../data");
        Application.setPreferences(new Preferences());
        DataCollection<DataElement> collection = COLLECTION_NAME.getDataCollection();

        DiagramType type = new PathwaySimulationDiagramType();
        Diagram diagram = type.createDiagram(collection, "testSmiles", new DiagramInfo("testSmiles"));

        Node node = new Node(diagram, new Specie(diagram, "gl"));
        node.getAttributes().add(new DynamicProperty(SmilesViewBuilder.SMILES_STRUCTURE, String.class, "c1c(N(=O)=O)cccc1"));

        diagram.put(node);

        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        builder.createDiagramView(diagram, getGraphics());

        ViewPane pane = new ViewPane();
        pane.setView((CompositeView)diagram.getView());
        assertView(pane, diagram.getName());

        collection.put(diagram);
    }

}
