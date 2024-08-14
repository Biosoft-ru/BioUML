package biouml.plugins.modelreduction._test;

import java.util.List;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.plugins.simulation.Model;
import biouml.standard.diagram.CompositeModelPreprocessor;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class TestFlatModel extends AbstractBioUMLTest
{
    public TestFlatModel(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestFlatModel.class.getName());
        suite.addTest(new TestFlatModel("test"));
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
    
    private void initModel(double[] value, String[] varpath, Model model, Map<String, Integer> varMapping) throws Exception
    {
        double[] arranged = StreamEx.of(model).mapToDouble(s->value[varMapping.get(s)]).toArray();
        model.setCurrentValues(arranged);
    }
    
}
