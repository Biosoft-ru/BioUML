package biouml.standard.diagram;

import java.util.List;
import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;

import biouml.standard.type.Stub.ConnectionPort;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ( "serial" )
public class ChangePortTypeAction extends BackgroundDynamicAction
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

        return new ChangePortTypeActionParameters(selectedItems);
    }

    boolean isApplicable(List<DataElement> selectedItems)
    {
        return selectedItems.size() == 1 && selectedItems.get(0) instanceof DiagramElement
                && Util.isPort((DiagramElement)selectedItems.get(0));
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
                    Diagram diagram = ( (Diagram)model );

                    ChangePortTypeActionParameters parameters = (ChangePortTypeActionParameters)properties;
                    Node node = (Node)selectedItems.get(0);

                    PathwaySimulationSemanticController controller = (PathwaySimulationSemanticController)diagram.getType()
                            .getSemanticController();

                    PortProperties properties = new PortProperties(diagram, ConnectionPort.class);
                    properties.setVarName(Util.getPortVariable(node));
                    properties.setAccessType(Util.getAccessType(node));
                    properties.setName(node.getName());
                    properties.setPortType(parameters.getPortType());
                    for( Edge e : node.edges() )
                        e.getCompartment().remove(e.getName());
                    Compartment c = node.getCompartment();
                    c.remove(node.getName());

                    Node newNode = (Node)controller.createInstance( c, ConnectionPort.class, node.getLocation(), properties ).getElement();
                    Util.setPortOrientation(newNode, Util.getPortOrientation(node));
                    diagram.put(newNode);

                    for( Edge e : node.getEdges() )
                        swapNode(node, newNode, e);
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
            oldNode.removeEdge(e);
            e.setInput(newNode);
            newNode.addEdge(e);
        }
        else if( e.getOutput() == oldNode )
        {
            oldNode.removeEdge(e);
            e.setOutput(newNode);
            newNode.addEdge(e);
        }
        else
            throw new IllegalArgumentException(e + ": Supplied node " + oldNode + " is not my input or output");
    }

    public static class ChangePortTypeActionParameters
    {
        private String portType = "contact";

        @PropertyName ( "Port type" )
        @PropertyDescription ( "Port type." )
        public String getPortType()
        {
            return portType;
        }
        public void setPortType(String portType)
        {
            this.portType = portType;
        }

        public ChangePortTypeActionParameters(List<DataElement> elements)
        {
        }
    }

    public static class ChangePortTypeActionParametersBeanInfo extends BeanInfoEx2<ChangePortTypeActionParameters>
    {
        public ChangePortTypeActionParametersBeanInfo()
        {
            super(ChangePortTypeActionParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            addWithTags("portType", "input", "output", "contact");
        }
    }
}
