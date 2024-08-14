package biouml.plugins.sedml;

import org.jlibsedml.AbstractIdentifiableElement;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.model.Compartment;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;

public abstract class WorkflowBuilder
{
    protected Compartment parent;
    protected WorkflowSemanticController controller;
    
    public WorkflowBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        this.parent = parent;
        this.controller = controller;
    }

    public abstract void build();
    
    protected Compartment addAnalysis(AnalysisMethod analysis)
    {
        DynamicPropertySet attributes = new DynamicPropertySetAsMap();
        AnalysisDPSUtils.writeParametersToNodeAttributes( analysis.getName(), analysis.getParameters(), attributes );
        Compartment result = controller.createAnalysisNode( parent, analysis.getName(), attributes );
        result = (Compartment)getUniqueNode( parent, result );
        parent.put( result );
        return result;
    }
    
    protected Node addDataElementNode(String nodeName, String expressionString)
    {
        return addWorkflowExpression( nodeName, expressionString, VariableType.getType(DataElementPath.class) );
    }
    
    protected Node addWorkflowExpression(String nodeName, String expressionString, VariableType type)
    {
        WorkflowExpression expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem(parent, Type.ANALYSIS_EXPRESSION);
        expression.setName(nodeName);
        expression.setType(type);
        expression.setExpression(expressionString);
        Node result = expression.getNode();
        result = getUniqueNode( parent, result );
        parent.put( result );
        return result;
    }
    
    protected Node addScript(String content, String type)
    {
        Compartment result;
        try
        {
            result = controller.createScriptNode( parent, content, type );
        }
        catch( Exception e )
        {
            throw new RuntimeException("Can not create script", e);
        }
        parent.put( result );
        return result;
    }
    
    protected String getTitleForSedmlElement(AbstractIdentifiableElement sedmlElement)
    {
        return sedmlElement.getId();
    }
    
    protected Edge addDirectedEdge(Compartment parent, Node in, Node out)
    {
        Edge e = controller.createDirectedLink( parent, in, out );
        controller.annotateEdge( e );
        ((Compartment)e.getOrigin()).put( e );
        return e;
    }
    
    protected Edge bindParameter(Compartment parent, Node in, Node out, String parameterName)
    {
        Edge e = controller.bindParameter( parent, in, out, parameterName, true );
        return e;
    }

    private Node getUniqueNode(Compartment parent, Node node)
    {
        if( parent.get(node.getName()) != null )
        {
            String name = node.getName();
            int i = 2;
            while( parent.get(name) != null )
            {
                name = node.getName() + "(" + i + ")";
                i++;
            }
            node = node.clone(parent, name);
        }
        return node;
    }
}
