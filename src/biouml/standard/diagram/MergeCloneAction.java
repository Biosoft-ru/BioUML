package biouml.standard.diagram;

import java.util.List;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.VariableRole;

import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ( "serial" )
public class MergeCloneAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        return object instanceof Diagram && ((Diagram)object).getType().getSemanticController() instanceof PathwaySimulationSemanticController;
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
                    if( ! ( selectedItems.get(0) instanceof Node ) )
                        return;

                    Node node = (Node)selectedItems.get(0);

                    Role role = node.getRole();
                    if( ! ( role instanceof VariableRole ) )
                        return;
                    Diagram diagram = Diagram.getDiagram(node);
                    DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
                    Node originalNode = (Node)role.getDiagramElement();
                    if( originalNode != node )
                    {
                        for( Edge e : node.getEdges() )
                        {
                            CloneNodeAction.swapNode(node, originalNode, e);
                            e.setView(null);
                            e.setPath(null);
                        }

                        node.getOrigin().remove(node.getName());

                        VariableRole varRole = originalNode.getRole( VariableRole.class );
                        varRole.removeAssociatedElement(node);

                        if (varRole.getAssociatedElements().length == 1)
                            originalNode.getAttributes().remove("sbgn:cloneMarker");
                    }

                    viewBuilder.createDiagramView(diagram, ApplicationUtils.getGraphics());
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }
}
