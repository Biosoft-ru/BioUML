package biouml.plugins.agentmodeling._test;

import java.awt.Point;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.plugins.agentmodeling.AgentModelDiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.diagram.SubDiagramProperties;
import biouml.standard.type.DiagramInfo;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestChangingStep extends TestCase
{
    public TestChangingStep(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestChangingStep.class.getName());
        suite.addTest(new TestChangingStep("test"));
        return suite;
    }
    public void test() throws Exception
    {
        Diagram agentDiagram = new AgentModelDiagramType().createDiagram(null, "agentModel", new DiagramInfo("agentModel"));
        
        Diagram diagram = new SbgnDiagramType().createDiagram(null, "agentDiagram");
        
        Equation equation = new Equation(null, Equation.TYPE_RATE, "x", "sin(time)");
        diagram.getType().getSemanticController().createInstance( diagram, Equation.class, new Point(0,0), equation );

        Event event = new Event(null);
        event.setTrigger( "time > 100" );
        event.addEventAssignment( new Assignment("__TIME_INCREMENT__", "100", event), true );
        diagram.getType().getSemanticController().createInstance( diagram, Event.class, new Point(0,0), event );
        
//        SubDiagramProperties properties = new SubDiagramProperties(diagram);
//        properties.se
//        agentDiagram.getType().getSemanticController().createInstance( diagram, SubDiagram.class, null, event );
    }
}