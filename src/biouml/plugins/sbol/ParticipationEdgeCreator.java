package biouml.plugins.sbol;

import javax.annotation.Nonnull;

import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.DirectionType;
import org.sbolstandard.core2.FunctionalComponent;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.Participation;
import org.sbolstandard.core2.RefinementType;
import org.sbolstandard.core2.SBOLDocument;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
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
            String name = CollectionFactory.getRelativeName( in, diagram ).replace( "/", "_" ) + "_to_" + CollectionFactory.getRelativeName( out, diagram ).replace( "/", "_" );
            name = DefaultSemanticController.generateUniqueName( diagram, name );

            if( temporary )
                return new Edge( new Stub( null, name ), in, out );

            boolean inputInteraction = in.getKernel() instanceof InteractionProperties;
            boolean outputInteraction = out.getKernel() instanceof InteractionProperties;
            if( ! ( inputInteraction ^ outputInteraction ) )
                throw new IllegalArgumentException( "One and only one of selected nodes should be an interaction." );

            SBOLDocument doc = SbolUtil.getDocument( diagram );
            ModuleDefinition moduleDefinition = SbolUtil.checkDefaultModule( doc );

            InteractionProperties interactionProperties = inputInteraction ? (InteractionProperties)in.getKernel()
                    : (InteractionProperties)out.getKernel();
            SbolBase participantKernel = inputInteraction ? (SbolBase)out.getKernel() : (SbolBase)in.getKernel();

            Identified participantDefinition = participantKernel.getSbolObject();
            Interaction interaction = (Interaction)interactionProperties.getSbolObject();
            if( participantKernel instanceof MolecularSpecies )
            {
                
                FunctionalComponent component = SbolUtil.findFunctionalComponent( moduleDefinition, participantDefinition.getDisplayId() );
                Participation participation = interaction.createParticipation( name, component.getDisplayId(),
                        SbolUtil.getParticipationURIByType( SbolConstants.STIMULATOR ) );
                ParticipationProperties kernel = new ParticipationProperties( participation );
                return new Edge( kernel, in, out );
            }
            else if( participantKernel instanceof SequenceFeature )
            {
                Node partNode = inputInteraction ? out : in;
                Compartment parent = partNode.getCompartment();
                Backbone backbone = (Backbone)parent.getKernel();
                ComponentDefinition backBoneDefinition = (ComponentDefinition)backbone.getSbolObject();
                Component backBoneComponentInstance = SbolUtil.findComponent( backBoneDefinition,  participantDefinition.getDisplayId() );
                FunctionalComponent functionalComponentInstance = SbolUtil.findFunctionalComponent( moduleDefinition, participantDefinition.getDisplayId() );
                FunctionalComponent backboneFunctionalComponent = SbolUtil.findFunctionalComponent( moduleDefinition, backBoneDefinition.getDisplayId());
                        
                if (backboneFunctionalComponent == null)
                    backboneFunctionalComponent = moduleDefinition.createFunctionalComponent( backBoneDefinition.getDisplayId() +"_fc", AccessType.PUBLIC,  backBoneDefinition.getDisplayId(), DirectionType.INOUT );
                
                if (functionalComponentInstance == null)
                    functionalComponentInstance = moduleDefinition.createFunctionalComponent( participantDefinition.getDisplayId() +"_fc", AccessType.PUBLIC,  participantDefinition.getDisplayId(), DirectionType.INOUT );
                             
                backboneFunctionalComponent.createMapsTo( name+"map", RefinementType.USELOCAL, functionalComponentInstance.getIdentity(), backBoneComponentInstance.getIdentity() );
     
                Participation participation = interaction.createParticipation( name, functionalComponentInstance.getDisplayId(),
                        SbolUtil.getParticipationURIByType( SbolConstants.STIMULATOR ) );
                ParticipationProperties kernel = new ParticipationProperties( participation );
                return new Edge( kernel, in, out );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return null;
    }

}