package biouml.plugins.pharm._test;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.util.DiagramXmlConstants;
import biouml.plugins.agentmodeling.AgentModelDiagramType;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.modelreduction.SteadyStateAnalysis;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.type.DiagramInfo;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.util.DPSUtils;

public class TestSimulationEngineWrapper extends AbstractBioUMLTest
{
    public TestSimulationEngineWrapper(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( TestSimulationEngineWrapper.class.getName() );
        suite.addTest( new TestSimulationEngineWrapper( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        DiagramType diagramType = new AgentModelDiagramType();
        Diagram diagram = diagramType.createDiagram( null, "agentModel", new DiagramInfo( "agentModel" ) );

        AgentModelSimulationEngine se = new AgentModelSimulationEngine();
        se.setDiagram( diagram );
        diagram.getAttributes()
                .add( DPSUtils.createHiddenReadOnlyTransient( DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se ) );

        SteadyStateAnalysis ssAnalysis = new SteadyStateAnalysis( null, "" );
        ssAnalysis.getParameters().setDiagram( diagram );
    }
}

