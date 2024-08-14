package biouml.plugins.physicell;

import javax.annotation.Nonnull;

import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.util.DPSUtils;

public class InteractionCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        Edge result = new Edge( new Stub( null, in.getName() + " interact " + out.getName(), PhysicellConstants.TYPE_INTERACTION ), in,
                out );
        InteractionProperties role = new InteractionProperties( result );
        result.setRole( role );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( "interaction", InteractionProperties.class, role ) );
        return result;
    }
}