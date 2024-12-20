package biouml.plugins.sbol;

import java.net.URI;

import javax.annotation.Nonnull;

import org.sbolstandard.core2.Interaction;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.CollectionFactory;

public class ParticipationEdgeCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        try
        {
            Diagram diagram = Diagram.getDiagram( in );
            String name = CollectionFactory.getRelativeName( in, diagram ) + "_" + CollectionFactory.getRelativeName( out, diagram );

            if( temporary )
                return new Edge( new Stub( null, name ), in, out );

            boolean inputInteraction = in.getKernel() instanceof InteractionProperties;
            boolean outputInteraction = out.getKernel() instanceof InteractionProperties;
            if( ! ( inputInteraction ^ outputInteraction ) )
                throw new IllegalArgumentException( "One and only one of selected nodes should be an interaction." );

            InteractionProperties interactionProperties = inputInteraction ? (InteractionProperties)in.getKernel() : (InteractionProperties)out.getKernel();
            Interaction interaction = (Interaction)interactionProperties.getSbolObject();

            org.sbolstandard.core2.Participation participation = interaction.createParticipation( name, name,
                    SbolUtil.getSpeciesURIByType( SbolUtil.TYPE_STIMULATION ) );

            Participation kernel = new Participation( participation );
            return new Edge( kernel, in, out );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return null;
        }
    }

}