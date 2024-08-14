package biouml.plugins.physicell;

import javax.annotation.Nonnull;

import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.util.DPSUtils;

public class TransformationCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        Edge result = new Edge( new Stub( null, in.getName() + " transforms " + out.getName(), PhysicellConstants.TYPE_TRANSFORMATION ), in,
                out );
        TransformationProperties role = new TransformationProperties( result );
        result.setRole( role );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( "transformation", TransformationProperties.class, role ) );
        return result;
    }
}