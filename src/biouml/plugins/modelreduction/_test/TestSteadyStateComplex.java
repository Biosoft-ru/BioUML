package biouml.plugins.modelreduction._test;

import java.util.Map;
import java.util.Map.Entry;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.modelreduction.SteadyStateAnalysis;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

public class TestSteadyStateComplex extends AbstractBioUMLTest
{
    public TestSteadyStateComplex(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSteadyStateComplex.class);
        return suite;
    }

    public void test() throws Exception
    {
        Diagram diagram = getDiagram("data/Collaboration (git)/Cardiovascular system/Complex model/", "Complex model new");
        SteadyStateAnalysis analysis = new SteadyStateAnalysis(null, "");
        
        analysis.getParameters().setDiagram(diagram);
        analysis.getParameters().setStartSearchTime(1E5);
        analysis.getParameters().setAbsoluteTolerance(10);
        analysis.getParameters().setValidationSize(1);
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime(1E5+100);
        VariableSet observed = new VariableSet();
        analysis.getParameters().setVariableNames(observed);
        observed.setSubdiagramName("Heart");
        observed.setVariableNames(new String[]{"P_S"});
        
        SimulationEngine engine = analysis.getParameters().getEngineWrapper().getEngine();
        
        Model model = engine.createModel();
        analysis.findSteadyState(model);
        
        double[] currentValues = model.getCurrentValues();

        Map<String, Diagram> diagrams = StreamEx.of(Util.getSubDiagrams(diagram)).toMap(s->s.getName(),s->(Diagram)DataElementPath.create(s.getDiagramPath()).getDataElement());
        
        for (Diagram d: diagrams.values())
        {
        	d.addState(new State(d, "steady"));
        	d.setCurrentStateName("steady");
        }
        
        for( Entry<String, Integer> e : engine.getVarPathIndexMapping().entrySet() )
        {
            double val = currentValues[e.getValue()];
            String path = e.getKey();
            
            
			if (path.contains("\\")) {
				String name = path.substring(path.lastIndexOf("\\") + 1,
						path.length());
				String subName = path.substring(0, path.lastIndexOf("\\"));

				Diagram innerDiagram = diagrams.get(subName);
				Variable var = innerDiagram.getRole(EModel.class).getVariable(
						name);
				var.setInitialValue(val);
			}
        }
        
        for (Diagram d: diagrams.values())
        {
        	d.restore();
        	d.save();
//        	System.out.println(d.getName());
        }
        
    }

    private Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository("../data_resources");
        DataCollection<?> collection = CollectionFactory.getDataCollection( path );
        DataElement de = collection.get(name);
        return (Diagram)de;
    }
}
