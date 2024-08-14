package biouml.workbench.diagram.action;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.util.ImageGenerator;

public class DistributeVerticalAction extends ProcessNodesAction
{
    @Override
    void processNodes(Diagram diagram, List<Node> nodes)
    {
        if(nodes.size() <= 2)
            return;
        diagram.setView( null );
        ImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );

        Collections.sort( nodes, Comparator.comparingDouble( node -> node.getView().getBounds().getCenterY() ) );
        double totalLength = 0;
        double minPos = nodes.get( 0 ).getView().getBounds().getMinY();
        double maxPos = nodes.get( 0 ).getView().getBounds().getMaxY();
        for(Node node : nodes)
        {
            Rectangle bounds = node.getView().getBounds();
            minPos = Math.min( minPos, bounds.getMinY() );
            maxPos = Math.max( maxPos, bounds.getMaxY() );
            totalLength+=bounds.getHeight();
        }
        double delta = (maxPos-minPos-totalLength)/(nodes.size()-1);
        double pos = nodes.get( 0 ).getView().getBounds().getMaxY();
        SemanticController sc = diagram.getType().getSemanticController();
        for(int i=1; i<nodes.size()-1; i++)
        {
            diagram.setView(null);
            ImageGenerator.generateDiagramView(diagram, ApplicationUtils.getGraphics());
            pos+=delta;
            Node node = nodes.get( i );
            int offset = (int) ( pos - node.getView().getBounds().getMinY() );
            pos+=node.getView().getBounds().getHeight();
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
