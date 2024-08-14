package biouml.plugins.pharm._test;

import java.util.List;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.plugins.pharm.analysis.Patient;
import biouml.plugins.pharm.analysis.SimulatePopulationAnalysis;
import biouml.standard.diagram.DiagramGenerator;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestSimulatePopulation extends AbstractBioUMLTest
{
    public TestSimulatePopulation(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestSimulatePopulation.class.getName());
        suite.addTest(new TestSimulatePopulation("simpleTest"));
        return suite;
    }

    public void simpleTest() throws Exception
    {
        SimulatePopulationAnalysis simulatePopulation = new SimulatePopulationAnalysis(null, "");
        List<Patient> patients = simulatePopulation.simulatePopulation(getDiagram(), getTable(), new String[]{"A"}, new String[]{"B"});
        
        for (Patient p: patients)
            assert(p.getInput()[0] - p.getObserved()[0] < 0.1);
    }

    private Diagram getDiagram() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("diagram");
        generator.createEquation("A", "-k*A", Equation.TYPE_RATE);
        generator.createEquation("B", "k*A", Equation.TYPE_RATE);
        Diagram diagram = generator.getDiagram();
        diagram.getRole(EModel.class).declareVariable("k", 1.0);
        return diagram;
    }
    
    private TableDataCollection getTable() throws Exception
    {
        double[][] dataMatrix = new double[10][1];
        for (int i=0; i<dataMatrix.length; i++)
            dataMatrix[i][0] = i;      
        String[] columns = new String[] {"A"};
        return TableDataCollectionUtils.createTable("test", dataMatrix, columns);
    }
}
