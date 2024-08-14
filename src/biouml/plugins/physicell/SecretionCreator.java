package biouml.plugins.physicell;

import javax.annotation.Nonnull;

import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.util.DPSUtils;

public class SecretionCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        Edge result = new Edge( new Stub( null, in.getName() + " -> " + out.getName(), PhysicellConstants.TYPE_SECRETION ), in, out );
        SecretionProperties role = new SecretionProperties( result );
        result.setRole( role );
        role.setDiagramElement( result );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( "secretion", SecretionProperties.class, role ) );
        return result;
    }
}