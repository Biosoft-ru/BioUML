package biouml.plugins.sedml._test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jlibsedml.AbstractIdentifiableElement;
import org.jlibsedml.FunctionalRange;
import org.jlibsedml.Libsedml;
import org.jlibsedml.Parameter;
import org.jlibsedml.Range;
import org.jlibsedml.UniformRange;
import org.jlibsedml.Variable;
import org.jmathml.ASTNode;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.ScriptElement;
import biouml.plugins.research.workflow.items.RangeCycleType;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItem;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.sedml.RangesBuilder;

public class TestRangesBuilder extends TestCase
{
    public void testFunctionalRangeVariables() throws Exception
    {
        WorkflowDiagramType workflowDiagramType = new WorkflowDiagramType();
        Diagram workflow = workflowDiagramType.createDiagram( null, "test", null );
        WorkflowSemanticController controller = (WorkflowSemanticController)workflowDiagramType.getSemanticController();
        Compartment cycle = controller.createCycleNode( workflow );
        
        RangesBuilder builder = new RangesBuilder( cycle, controller );

        Map<String, Range> ranges = new HashMap<>();
        ranges.put( "main", new UniformRange( "main", 0, 1, 10 ) );
        ASTNode mathAsNode = Libsedml.parseFormulaString( "param+var+main" );
        Map<String, AbstractIdentifiableElement> parameters = new HashMap<>();
        parameters.put( "param", new Parameter( "param", "param", 1 ) );
        Map<String, AbstractIdentifiableElement> variables = new HashMap<>();
        variables.put( "var", new Variable( "var", "var", "model", "/s:sbml/s:model/s:listOfParameters/s:parameter[@id='w']" ) );
        ranges.put( "derived", new FunctionalRange( "derived", null, variables, parameters, mathAsNode ));
        builder.setRanges( ranges );
        builder.setMainRange( ranges.get( "main" ) );
        
        Map<String, Node> modelNodes = new HashMap<>();
        Node modelNode = new Node( workflow, "model_node", null );
        modelNodes.put( "model", modelNode  );
        builder.setModelNodes( modelNodes  );
        
        builder.build();
        
        Node cycleNode = (Node)cycle.get( "main" );
        WorkflowItem item = WorkflowItemFactory.getWorkflowItem( cycleNode );
        assertTrue( item instanceof WorkflowCycleVariable );
        WorkflowCycleVariable cycleVariable = (WorkflowCycleVariable)item;
        assertEquals( "0.0,0.1..1.0", cycleVariable.getExpression() );
        assertTrue(cycleVariable.getCycleType() instanceof RangeCycleType);
        assertEquals(VariableType.getType( Double.class ), cycleVariable.getType());
        
        Node paramNode = (Node)cycle.get( "param" );
        WorkflowExpression expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( paramNode );
        assertEquals( "1.0", expression.getExpression() );
        assertEquals(VariableType.getType( Double.class ), expression.getType());
        
        Node varNode = (Node)cycle.get( "var" );
        expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( varNode );
        assertEquals( "$model_node/role/vars/w/initialValue$", expression.getExpression() );
        assertEquals(VariableType.getType( Double.class ), expression.getType());
        
        Node derivedNode = (Node)cycle.get( "derived" );
        assertNotNull(derivedNode);
        assertEquals(VariableType.getType( Double.class ), expression.getType());
        
        assertEquals(1, derivedNode.getEdges().length);
        Edge edge = derivedNode.getEdges()[0];
        assertTrue(edge.getOutput() == derivedNode);
        Node scriptNode = edge.getInput();
        
        assertEquals("math", scriptNode.getAttributes().getValue( ScriptElement.SCRIPT_TYPE ));
        assertEquals("param+var+main", scriptNode.getAttributes().getValue( ScriptElement.SCRIPT_SOURCE ).toString().replace( " ", "" ));
        
        for(Node node : Arrays.asList( cycleNode, paramNode, varNode ))
        {
            Edge[] edges = node.getEdges();
            assertEquals(1, edges.length);
            assertTrue(edges[0].getInput() == node);
            assertTrue(edges[0].getOutput() == scriptNode);
        }
    }
}
