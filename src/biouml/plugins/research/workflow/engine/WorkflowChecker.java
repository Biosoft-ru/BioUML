package biouml.plugins.research.workflow.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisParameters;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Type;

/**
 * Checker for workflow diagrams
 */
public class WorkflowChecker
{
    protected Diagram workflow;
    protected StringBuffer errors = null;

    public WorkflowChecker(Diagram workflow)
    {
        this.workflow = workflow;
    }

    /**
     * Check workflow diagram
     */
    public boolean check()
    {
        errors = new StringBuffer();
        checkElements(workflow);
        checkRelations();
        return errors.length() == 0;
    }

    /**
     * Return string with errors description if errors exist
     */
    public String getErrors()
    {
        if( errors == null )
            return null;
        return errors.toString();
    }

    /**
     * Add error message
     */
    protected void addError(String error)
    {
        errors.append("* ").append(error).append('\n');
    }

    /**
     * Check workflow elements for parameters
     */
    protected void checkElements(Compartment compartment)
    {
        for(DiagramElement de: compartment)
        {
            if( de.getKernel().getType().equals(Type.ANALYSIS_METHOD) )
            {
                AnalysisParameters parameters = WorkflowEngine.getAnalysisParametersByNode( (Node) de );
                if(parameters == null)
                {
                    addError("Workflow node '" + de.getCompleteNameInDiagram() + "' refers to unknown analysis '"
                            + de.getAttributes().getValueAsString( AnalysisDPSUtils.PARAMETER_ANALYSIS_FULLNAME )+"'");
                    continue;
                }
                ComponentModel model = ComponentFactory.getModel(parameters);
                for( String inputName : parameters.getInputNames() )
                {
                    Property property = model.findProperty(inputName);
                    Node n = (Node) ( (Compartment)de ).get(inputName.replace('/', ':'));
                    if(n == null)
                    {
                        //Workaround for modified analysis properties: do not fail if can be null
                        if( property.getBooleanAttribute( BeanInfoConstants.CAN_BE_NULL ) )
                            continue;

                        addError("Invalid analysis '" + de.getName()+"': parameter "+inputName+" absent");
                        continue;
                    }
                    Edge[] edges = n.getEdges();
                    if( ( edges.length > 1 && !property.getValueClass().equals(DataElementPathSet.class) )
                            || ( edges.length == 0 && !(property.getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL) || !property.isVisible(Property.SHOW_EXPERT)) )
                            || ( edges.length > 0 && edges[0].getOutput() != n ) )
                    {
                        addError("Incorrect input edges for analysis '" + de.getName()+"', parameter '"+inputName+"': "
                                +((edges.length == 0)?"no edge connected":edges.length>1?"multiple edges connected":"invalid edge"));
                        continue;
                    }
                }
                for( String outputName : parameters.getOutputNames() )
                {
                    Property property = model.findProperty(outputName);
                    Node n = (Node) ( (Compartment)de ).get(outputName.replace('/', ':'));
                    if(n == null)
                    {
                        //Workaround for modified analysis properties: do not fail if can be null
                        if( property.getBooleanAttribute( BeanInfoConstants.CAN_BE_NULL ) )
                            continue;
                        addError("Invalid analysis '" + de.getName()+"': parameter "+outputName+" absent");
                        continue;
                    }
                    Edge[] edges = n.getEdges();
                    if( ( edges.length > 1 )
                            || ( edges.length == 0 && !property.getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL) )
                            || ( edges.length > 0 && edges[0].getInput() != n ) )
                    {
                        addError("Incorrect output edges for analysis '" + de.getName()+"', parameter '"+outputName+"': "
                                +((edges.length == 0)?"no edge connected":edges.length>1?"multiple edges connected":"invalid edge"));
                        continue;
                    }
                }
            }
            else if( de.getKernel().getType().equals(Type.ANALYSIS_CYCLE) )
            {
                checkElements( (Compartment)de );
            }
            else if( de.getKernel().getType().equals(Type.TYPE_PLOT) )
            {
                if( de.getAttributes().getValue(PlotElement.PLOT_PATH) == null )
                {
                    addError("Incorrect plot path for plot '" + de.getName() + "'");
                    continue;
                }
            }
        }
    }

    /**
     * Check relations for cycles
     */
    protected void checkRelations()
    {
        Map<ru.biosoft.access.core.DataElement, GraphNode> relations = new HashMap<>();
        //build dependencies
        for(DiagramElement de: workflow)
        {
            if( de.getKernel().getType().equals(Type.ANALYSIS_METHOD) )
            {
                AnalysisParameters parameters = AnalysisDPSUtils.getAnalysisMethodByNode( de.getAttributes())
                        .getParameters();
                for( String inputName : parameters.getInputNames() )
                {
                    DataElement input = ( (Compartment)de ).get(inputName.replace('/', ':'));
                    if( input != null )
                    {
                        for( String outputName : parameters.getOutputNames() )
                        {
                            DataElement output = ( (Compartment)de ).get(outputName.replace('/', ':'));
                            if( output != null )
                            {
                                addGraphNode(relations, input, output);
                            }
                        }
                    }
                }
            }
            else if( de.getKernel().getType().equals(Type.TYPE_DIRECTED_LINK) )
            {
                addGraphNode(relations, ( (Edge)de ).getInput(), ( (Edge)de ).getOutput());
            }
        }
        //check for cycles
        for( GraphNode gNode : relations.values() )
        {
            if( ( gNode != null ) && ( !gNode.marked ) )
            {
                if( findCycles(relations, gNode) )
                {
                    addError("Cycle detected: workflow can not be executed");
                }
            }
        }
    }

    protected void addGraphNode(Map<ru.biosoft.access.core.DataElement, GraphNode> relations, DataElement input, DataElement output)
    {
        GraphNode gNode = relations.get(input);
        if( gNode == null )
        {
            gNode = new GraphNode();
            gNode.input = input;
            gNode.output.add(output);
            relations.put(input, gNode);
        }
        else
        {
            gNode.output.add(output);
        }
    }

    /**
     * Return true if cycle detected
     */
    protected boolean findCycles(Map<ru.biosoft.access.core.DataElement, GraphNode> relations, GraphNode node)
    {
        node.marked = true;
        node.picked = true;

        for( ru.biosoft.access.core.DataElement output : node.output )
        {
            GraphNode gOut = relations.get(output);
            if( gOut != null )
            {
                if( gOut.picked )
                {
                    return true;
                }
                else if( !gOut.marked && findCycles(relations, gOut) )
                {
                    return true;
                }
            }
        }

        node.picked = false;
        return false;
    }

    protected static class GraphNode
    {
        public DataElement input;
        public List<DataElement> output;
        public boolean picked;
        public boolean marked;

        public GraphNode()
        {
            output = new ArrayList<>();
            picked = false;
            marked = false;
        }
    }
}
