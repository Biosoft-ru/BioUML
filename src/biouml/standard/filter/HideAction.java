package biouml.standard.filter;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Type;

/**
 * If diagram element satisfies to the filter condition it will be highlighted.
 */
public class HideAction implements Action
{
    public static final HideAction instance = new HideAction();

    /** @todo */
    @Override
    public void apply(DiagramElement de)
    {
        if( de instanceof Node )
            hideNode( (Node)de );
    }

    protected void hideNode(Node node)
    {
        if( node.getView().isVisible() )
        {
            node.getView().setVisible(false);
            hideNodeEdges(node);
        }
    }

    protected void hideNodeEdges(Node node)
    {
        // hide also all related reactions
        node.edges().peek( edge -> edge.getView().setVisible( false ) )
            .flatMap( Edge::nodes ).filter( n -> isReaction( n.getKernel() ) )
            .forEach( this::hideNode );
    }

    protected boolean isReaction(Base kernel)
    {
        return kernel != null &&
            ( Type.TYPE_REACTION.equals ( kernel.getType() ) || kernel.getType().startsWith ( Type.TYPE_RELATION ) );
    }
}


