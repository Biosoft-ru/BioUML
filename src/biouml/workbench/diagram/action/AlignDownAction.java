package biouml.workbench.diagram.action;

import java.awt.Dimension;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.util.ImageGenerator;

public class AlignDownAction extends ProcessNodesAction
{
    @Override
    void processNodes(Diagram diagram, List<Node> nodes)
    {
        diagram.setView( null );
        ImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
        double maxPos = nodes.get( 0 ).getView().getBounds().getMaxY();
        for(Node node : nodes)
            maxPos = Math.max( maxPos, node.getView().getBounds().getMaxY() );
        SemanticController sc = diagram.getType().getSemanticController();
        for(Node node : nodes)
        {
            diagram.setView( null );
            ImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
            int offset = (int) ( maxPos - node.getView().getBounds().getMaxY() );
            if(offset != 0)
                try
                {
                    sc.move( node, (Compartment) ( node.getOrigin() ), new Dimension( 0, offset ), node.getView().getBounds() );
                }
                catch( Exception e )
                {
                }
        }
    }
}
