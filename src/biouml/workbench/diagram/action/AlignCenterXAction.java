package biouml.workbench.diagram.action;

import java.awt.Dimension;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.util.DiagramImageGenerator;

public class AlignCenterXAction extends ProcessNodesAction
{
    @Override
    void processNodes(Diagram diagram, List<Node> nodes)
    {
        diagram.setView( null );
        DiagramImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
        double minPos = nodes.get( 0 ).getView().getBounds().getMinX();
        double maxPos = nodes.get( 0 ).getView().getBounds().getMaxX();
        for(Node node : nodes)
        {
            minPos = Math.min( minPos, node.getView().getBounds().getMinX() );
            maxPos = Math.max( maxPos, node.getView().getBounds().getMaxX() );
        }
        double midPos = (maxPos+minPos)/2;
        SemanticController sc = diagram.getType().getSemanticController();
        for(Node node : nodes)
        {
            diagram.setView(null);
            DiagramImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
            int offset = (int) ( midPos - node.getView().getBounds().getCenterX() );
            if(offset != 0)
                try
                {
                    sc.move( node, (Compartment) ( node.getOrigin() ), new Dimension( offset, 0 ), node.getView().getBounds() );
                }
                catch( Exception e )
                {
                }
        }
    }
}
