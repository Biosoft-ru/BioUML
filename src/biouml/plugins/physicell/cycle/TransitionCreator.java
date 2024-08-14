package biouml.plugins.physicell.cycle;

import javax.annotation.Nonnull;

import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.physicell.TransitionProperties;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.util.DPSUtils;

public class TransitionCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        Edge result = new Edge( new Stub( null, in.getName() + " -> " + out.getName(), CycleConstants.TYPE_TRANSITION ), in, out );
        TransitionProperties role = new TransitionProperties( result );
        result.setRole( role );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( CycleConstants.TYPE_TRANSITION, TransitionProperties.class, role ) );
        return result;
    }
}