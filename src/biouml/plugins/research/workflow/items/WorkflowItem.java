package biouml.plugins.research.workflow.items;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Compartment;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

/**
 * Represents workflow item linked to particular node which can be set up via bean properties of this object
 * @author lan
 */
public abstract class WorkflowItem extends Option
{
    private Node node;
    private boolean canSetName;
    private ViewEditorPane viewEditorPane;

    public WorkflowItem(Node node, Boolean canSetName)
    {
        super();
        this.node = node;
        this.canSetName = canSetName;
    }

    /**
     * Name of workflow node
     */
    public String getName()
    {
        return node.getName();
    }

    /**
     * If name can be set (usually during node creation) then node will be cloned after each name change
     */
    public void setName(String name)
    {
        name = name.replace(".", "").replace("/", "").trim();
        if( isNameReadonly() || name.isEmpty() ) return;
        String oldName = node.getName();
        Node oldNode = node;
        DataCollection origin = node.getOrigin();
        node.setNotificationEnabled(false);
        node = node.clone((Compartment)node.getOrigin(), name);
        if( origin != null && origin.contains(oldName) )
        {
            Edge[] edges = oldNode.getEdges();
            List<Edge> newEdges = new ArrayList<>();
            List<Edge> oldEdges = new ArrayList<>();
            startTransaction("Rename");
            try
            {
                for( Edge edge : edges )
                {
                    Node newInput = edge.getInput();
                    Node newOutput = edge.getOutput();
                    if( newInput == oldNode )
                        newInput = node;
                    if( newOutput == oldNode )
                        newOutput = node;
                    if( newInput != edge.getInput() || newOutput != edge.getOutput() )
                    {
                        Edge newEdge = new Edge(edge.getOrigin(), edge.getKernel(), newInput, newOutput);
                        newEdge.getAttributes().add(
                                new DynamicProperty(WorkflowSemanticController.EDGE_ANALYSIS_PROPERTY, String.class, edge
                                        .getAttributes().getValue(WorkflowSemanticController.EDGE_ANALYSIS_PROPERTY)));
                        newEdge.getAttributes().add(
                                new DynamicProperty(WorkflowSemanticController.EDGE_VARIABLE, String.class, name));
                        newEdges.add(newEdge);
                        oldEdges.add(edge);
                    }
                }
                for( Edge edge : oldEdges )
                {
                    edge.getOrigin().remove(edge.getName());
                }
                origin.remove(oldName);
                origin.put(node);
                for( Edge edge : newEdges )
                {
                    edge.getOrigin().put(edge);
                }
            }
            catch( Exception e )
            {
                // Try to rollback change
                try
                {
                    for( Edge edge : newEdges )
                        origin.remove(edge.getName());
                    node = oldNode;
                    if(!origin.contains(node)) origin.put(node);
                    for( Edge edge : oldEdges )
                        origin.put(edge);
                }
                catch( Exception e1 )
                {
                    
                }
                node.setNotificationEnabled(true);
                return;
            }
            completeTransaction();
        }
        node.setNotificationEnabled(true);
        firePropertyChange("name", oldName, node.getName());
    }

    public boolean isNameReadonly()
    {
        return canSetName == false && getViewEditorPane() == null;
    }

    /**
     * Returns node associated with item. Note that it can differ from one passed to constructor if canSetName is true
     */
    public Node getNode()
    {
        return node;
    }
    
    protected void startTransaction(String name)
    {
        if(getViewEditorPane() != null)
            getViewEditorPane().startTransaction(name);
    }
    
    protected void completeTransaction()
    {
        if(getViewEditorPane() != null)
            getViewEditorPane().completeTransaction();
    }

    protected ViewEditorPane getViewEditorPane()
    {
        return viewEditorPane;
    }

    protected void setViewEditorPane(ViewEditorPane viewEditorPane)
    {
        Object oldValue = this.viewEditorPane;
        this.viewEditorPane = viewEditorPane;
        firePropertyChange("viewEditorPane", oldValue, this.viewEditorPane);
    }
}
