package biouml.plugins.sedml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlibsedml.ComputeChange;
import org.jlibsedml.Parameter;
import org.jlibsedml.Variable;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.state.analyses.ChangeDiagram;
import biouml.plugins.state.analyses.ChangeDiagramParameters;
import biouml.plugins.state.analyses.StateChange;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.Utils;

public class ComputeChangesBuilder extends WorkflowBuilder
{
    private List<ComputeChange> changes;
    private Node modelNode;
    private Diagram model;
    private Map<String, Node> referencedModelNodes;
    private Map<String, Diagram> referencedModels;

    private Compartment changeDiagramNode;

    public ComputeChangesBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }

    public void setChanges(List<ComputeChange> changes)
    {
        this.changes = changes;
    }

    public void setModelNode(Node modelNode)
    {
        this.modelNode = modelNode;
    }

    public void setModel(Diagram model)
    {
        this.model = model;
    }


    public void setReferencedModelNodes(Map<String, Node> referencedModelNodes)
    {
        this.referencedModelNodes = referencedModelNodes;
    }

    public void setReferencedModels(Map<String, Diagram> referencedModels)
    {
        this.referencedModels = referencedModels;
    }

    public Compartment getChangeDiagramNode()
    {
        return changeDiagramNode;
    }

    @Override
    public void build()
    {
        ChangeDiagram analysis = AnalysisMethodRegistry.getAnalysisMethod( ChangeDiagram.class );
        ChangeDiagramParameters parameters = analysis.getParameters();

        StateChange[] stateChanges = new StateChange[changes.size()];
        for( int i = 0; i < changes.size(); i++ )
        {
            ComputeChange change = changes.get( i );
            StateChange stateChange = new StateChange();
            String varName = SedmlUtils.resolveVariableName( model, change.getTargetXPath().getTargetAsString() );
            stateChange.setElementId( "" );//change diagram itself
            stateChange.setElementProperty( "role/vars/" + varName + "/initialValue" );
            stateChanges[i] = stateChange;
        }
        parameters.setChanges( stateChanges );

        changeDiagramNode =  addAnalysis( analysis );

        for( int i = 0; i < changes.size(); i++ )
        {
            ComputeChange change = changes.get( i );

            Map<String, Node> variableNodes = new HashMap<>();
            for(Variable var : change.getListOfVariables())
                variableNodes.put( var.getId(), buildVariableNode(var) );
            for(Parameter p : change.getListOfParameters())
                variableNodes.put( p.getId(), buildParameterNode(p) );

            AstStart ast = MathMLUtils.convertMathML( change.getMath() );
            Utils.pruneFunctions( ast );
            String expr = MathMLUtils.mathMLToExpression( ast );

            Node nodeToBind;
            if( variableNodes.containsKey( expr ) )
                nodeToBind = variableNodes.get( expr );
            else
            {
                Node scriptNode = addScript( expr, "math" );
                for(Node varNode : variableNodes.values())
                    addDirectedEdge( parent, varNode, scriptNode );
                nodeToBind = addWorkflowExpression( "change" + (i+1), "", VariableType.getType( Double.class ) );
                addDirectedEdge( parent, scriptNode, nodeToBind );
            }

            controller.bindParameter( nodeToBind, changeDiagramNode, "changes/[" + i + "]/propertyValue", true );
        }

        addDirectedEdge( parent, modelNode, (Node)changeDiagramNode.get( "diagramPath" ) );
    }

    private Node buildParameterNode(Parameter p)
    {
        return addWorkflowExpression( p.getId(), String.valueOf( p.getValue() ), VariableType.getType( Double.class ) );
    }

    private Node buildVariableNode(Variable var)
    {
        if(var.getReference() == null || var.getReference().isEmpty() )
            throw new IllegalArgumentException("Variable should have reference attribute when used in ComputeChange");
        Node referenceModelNode = referencedModelNodes.get( var.getReference() );
        if(referenceModelNode == null)
            throw new IllegalArgumentException("Unknown model reference '" + var.getReference() + "'");
        Diagram referenceModel = referencedModels.get( var.getReference() );
        String nameInModel = SedmlUtils.resolveVariableName( referenceModel, var.getTarget() );
        //TODO: reference top level model even if this.parent is cycle.
        String expression = "$" + WorkflowExpression.escape( referenceModelNode.getName() + "/element/role/vars/" + nameInModel + "/initialValue" ) + "$";
        return addWorkflowExpression( var.getId(), expression, VariableType.getType( Double.class ) );
    }

}
