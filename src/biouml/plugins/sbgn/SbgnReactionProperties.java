package biouml.plugins.sbgn;

import java.awt.Point;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.ReactionInitialProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class SbgnReactionProperties extends ReactionInitialProperties
{

    public SbgnReactionProperties( )
    {

    }

    public SbgnReactionProperties(String reactionName, KineticLaw law, List<SpecieReference> references )
    {
        setKineticlaw(law);
        setSpecieReferences(references);
        setReactionName(reactionName);
    }

    /**
     * Creates reaction node, components nodes and edges. Put them to diagram.
     */
    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        DiagramElementGroup results = super.createElements( compartment, location, viewPane );
        StreamEx.of( results.getElements() ).forEach( de -> SbgnSemanticController.setNeccessaryAttributes( de ) );
        List<DiagramElement> additional = StreamEx.of(results.getElements()).select(Node.class).filter(Util::isReaction)
                .flatCollection(n -> SbgnUtil.generateSourceSink(n, false)).toList();
        results.addAll(additional);
        putResults( additional );
        return results;
    }

    @Override
    public boolean acceptForReaction(Node node)
    {
        return node.getKernel() instanceof Specie && node.getRole() instanceof VariableRole;
    }

    @Override
    public Edge createEdge(SpecieReference sr, Node reactionNode, Node otherNode)
    {
        Edge edge = super.createEdge( sr, reactionNode, otherNode );
        SbgnSemanticController.setNeccessaryAttributes( edge );
        return edge;
    }
}
