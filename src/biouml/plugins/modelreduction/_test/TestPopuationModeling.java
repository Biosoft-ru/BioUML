package biouml.plugins.modelreduction._test;

import java.util.List;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.standard.diagram.CompositeModelPreprocessor;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class TestPopuationModeling extends AbstractBioUMLTest
{
    public TestPopuationModeling(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestPopuationModeling.class.getName());
        suite.addTest(new TestPopuationModeling("test"));
        return suite;
    }

    public void test() throws Exception
    {
        Diagram diagram = getDiagram();
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        Diagram flatVersion = preprocessor.preprocess(diagram);
        List<Edge> edges = flatVersion.recursiveStream().select(Edge.class).toList();
        for( Edge e : edges )
            flatVersion.remove(e.getName());
        diagram.getViewOptions().setDependencyEdges(false);
        diagram.getOrigin().put(flatVersion);
    }

    protected Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection<?> db = CollectionFactory.getDataCollection("data/Collaboration (git)/Cardiovascular system/Solodyannikov 2006/");
        return (Diagram)db.get("Solodyannkiov 2006 Composite");
    }
}
