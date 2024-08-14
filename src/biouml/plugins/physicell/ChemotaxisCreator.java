package biouml.plugins.physicell;

import javax.annotation.Nonnull;

import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.util.DPSUtils;

public class ChemotaxisCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        Edge result = new Edge( new Stub( null, in.getName() + " chemotaxis " + out.getName(), PhysicellConstants.TYPE_CHEMOTAXIS ), in,
                out );
        ChemotaxisProperties role = new ChemotaxisProperties( result );
        result.setRole( role );
        role.setDiagramElement( result );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( "chemotaxis", ChemotaxisProperties.class, role ) );
        return result;
    }
}