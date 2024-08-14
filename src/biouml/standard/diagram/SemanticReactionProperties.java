package biouml.standard.diagram;

import biouml.model.Compartment;
import biouml.model.Node;

public class SemanticReactionProperties extends ReactionInitialProperties
{
    @Override
    public boolean acceptForReaction(Node node)
    {
        return node != null && ! ( node instanceof Compartment );
    }

}
