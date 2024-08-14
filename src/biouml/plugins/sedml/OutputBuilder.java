package biouml.plugins.sedml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

import org.jlibsedml.AbstractTask;
import org.jlibsedml.Parameter;
import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;
import org.jmathml.ASTNode;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Utils;

public abstract class OutputBuilder extends WorkflowBuilder
{
    protected Map<String, Node> simulationResultNodes;
    private Map<String, Diagram> diagrams;
    protected SedML sedml;
    protected Node outputNode;

    public OutputBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }

    public void setSimulationResultNodes(Map<String, Node> nodes)
    {
        this.simulationResultNodes = nodes;
    }

    public void setDiagrams(Map<String, Diagram> diagrams)
    {
        this.diagrams = diagrams;
    }

    public void setSedml(SedML sedml)
    {
        this.sedml = sedml;
    }

    public Node getOutputNode()
    {
        return outputNode;
    }
    
    protected abstract Set<String> getTaskReferences();

    protected String createExpression(org.jlibsedml.DataGenerator sedmlDataGenerator)
    {
        Map<String, Double> parameters = StreamEx.of( sedmlDataGenerator.getListOfParameters() )
                .mapToEntry( Parameter::getId, Parameter::getValue ).toMap();
        
        Set<String> taskReferences = getTaskReferences();
        Map<String, String> variables = new HashMap<>();
        for(org.jlibsedml.Variable sedmlVar : sedmlDataGenerator.getListOfVariables())
        {
            String taskId = sedmlVar.getReference();
            AbstractTask task = sedml.getTaskWithId( taskId );
            String simulationResultName = getTitleForSedmlElement( task );
            
            String modelId = task instanceof RepeatedTask ? SedmlUtils.getModelReference( (RepeatedTask)task, sedml ) : task.getModelReference();
            String nameInModel = SedmlUtils.resolveVariableName( diagrams.get( modelId ), sedmlVar.getTarget() );

            String referenceInExpression = nameInModel;
            if(taskReferences.size() != 1)
                referenceInExpression = simulationResultName + "." + nameInModel;
            variables.put( sedmlVar.getId(), referenceInExpression );
        }
        
        ASTNode math = sedmlDataGenerator.getMath();
        AstStart myAST = MathMLUtils.convertMathML( math );
        List<AstVarNode> varNodes = Utils.deepChildren( myAST ).select( AstVarNode.class ).toList();
        for(AstVarNode var : varNodes)
            if(variables.containsKey( var.getName() ))
                var.setName( variables.get( var.getName() ) );
            else if(parameters.containsKey( var.getName() ))
            {
                Double value = parameters.get( var.getName() );
                var.jjtGetParent().jjtReplaceChild( var, Utils.createConstant( value ) );
            }
        return MathMLUtils.mathMLToExpression( myAST );
    }
}