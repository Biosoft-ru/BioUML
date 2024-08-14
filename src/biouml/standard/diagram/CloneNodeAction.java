package biouml.standard.diagram;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.VariableRole;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

@SuppressWarnings ( "serial" )
public class CloneNodeAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        return object instanceof Diagram
                && ( (Diagram)object ).getType().getSemanticController() instanceof PathwaySimulationSemanticController;
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        if( !isApplicable(selectedItems) )
            return null;

        return new CloneNodeActionParameters(selectedItems);
    }

    boolean isApplicable(List<DataElement> selectedItems)
    {
        return selectedItems.size() >= 1 && selectedItems.get(0) instanceof Node
                && ( (Node)selectedItems.get(0) ).getRole() instanceof VariableRole;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    if( !isApplicable(selectedItems) )
                        return;

                    CloneNodeActionParameters parameters = (CloneNodeActionParameters)properties;
                    Node node = (Node)selectedItems.get(0);
                    SemanticController controller = Diagram.getDiagram(node).getType().getSemanticController();

                    if( parameters.separateClones )
                    {
                        
                        for( Node reactionNode : parameters.reactions )
                        {
                            Point newLocation = node.getLocation();
                            newLocation.translate( 5, 5 );
                            Node clonedNode = controller.cloneNode( node, parameters.getNodeName() + "_" + reactionNode.getName(), newLocation );
                            clonedNode.save();
                            for( Edge e : reactionNode.getEdges() )
                            {
                                if( e.getOtherEnd(reactionNode) == node )
                                    swapNode(node, clonedNode, e);
                            }
                        }
                    }
                    else
                    {
                        Point newLocation = node.getLocation();
                        newLocation.translate( 5, 5 );
                        Node clonedNode = controller.cloneNode( node, parameters.getNodeName(), newLocation);
                        clonedNode.save();
                        for( Node reactionNode : parameters.reactions )
                        {
                            for( Edge e : reactionNode.getEdges() )
                            {
                                if( e.getOtherEnd(reactionNode) == node )
                                    swapNode(node, clonedNode, e);
                            }
                        }
                    }
                    resultsAreReady( new Object[] {Diagram.getDiagram( node )} );
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    /**
     * Redirects edge e from oldNode to newNode
     * @throws IllegalArgumentException if e is not connected to oldNode
     */
    public static void swapNode(@Nonnull Node oldNode, @Nonnull Node newNode, @Nonnull Edge e)
    {
        if( e.getInput() == oldNode )
        {
//            oldNode.removeEdge(e);
            e.setInput(newNode);
            newNode.addEdge(e);
        }
        else if( e.getOutput() == oldNode )
        {
//            oldNode.removeEdge(e);
            e.setOutput(newNode);
            newNode.addEdge(e);
        }
        else
            throw new IllegalArgumentException(e + ": Supplied node " + oldNode + " is not my input or output");
    }

    public static class CloneNodeActionParameters
    {
        private final Map<String, Node> availableReactions;

        private String nodeName;

        private boolean separateClones = false;

        @PropertyName ( "Separate clone for each reaction" )
        @PropertyDescription ( "Create separate clone for each reaction." )
        public boolean isSeparateClones()
        {
            return separateClones;
        }
        public void setSeparateClones(boolean separateClones)
        {
            this.separateClones = separateClones;
        }

        @PropertyName ( "Name" )
        @PropertyDescription ( "name of the new node." )
        public String getNodeName()
        {
            return nodeName;
        }
        public void setNodeName(String nodeName)
        {
            this.nodeName = nodeName;
        }

        private Node[] reactions = new Node[0];
        @PropertyName ( "Reactions to redirect" )
        @PropertyDescription ( "Reactions which will be redirected to the new node." )
        public String[] getReactions()
        {
            return StreamEx.of(reactions).map(r->r.getName()).toArray(String[]::new);
        }
        public void setReactions(String[] reactions)
        {
            this.reactions = StreamEx.of(reactions).map(r->this.availableReactions.get(r)).toArray(Node[]::new);
        }

        public Set<String> getAvailableReactions()
        {
            return availableReactions.keySet();
        }

        public CloneNodeActionParameters(List<DataElement> elements)
        {
            Node node = (Node)elements.get(0);
            nodeName = DefaultSemanticController.generateUniqueNodeName(node.getCompartment(), node.getName());
            availableReactions = node.edges().map(e -> e.getOtherEnd(node)).filter(Util::isReaction).toMap(n->n.getName(), n->n);
        }
    }

    public static class CloneNodeActionParametersBeanInfo extends BeanInfoEx2<CloneNodeActionParameters>
    {
        public CloneNodeActionParametersBeanInfo()
        {
            super(CloneNodeActionParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("nodeName");
            add("reactions", ReactionSelector.class);
            add("separateClones");
        }
    }

    public static class ReactionSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            Set<String> list = ( (CloneNodeActionParameters)getBean() ).getAvailableReactions();
            return list.toArray(new String[list.size()]);
        }
    }
}
