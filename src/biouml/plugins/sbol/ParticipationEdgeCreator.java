package biouml.plugins.sbol;

import java.awt.Point;

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
            SBOLDocument doc = SbolUtil.getDocument( diagram );

            String name = CollectionFactory.getRelativeName( in, diagram ).replace( "/", "_" ) + "_to_"
                    + CollectionFactory.getRelativeName( out, diagram ).replace( "/", "_" );
            name = DefaultSemanticController.generateUniqueName( diagram, name );
            ModuleDefinition moduleDefinition = SbolUtil.checkDefaultModule( doc );

            if( temporary )
            {
                return new Edge( new Stub( null, name ), in, out );
            }
            else if( out instanceof Diagram )//create degradation reaction
            {
                InteractionProperties properties = new InteractionProperties();
                properties.setType( SbolConstants.DEGRADATION );
                properties.setName( DefaultSemanticController.generateUniqueName( diagram, "Degradation" ) );
                Node reaction = properties.doCreateInteraction( diagram, doc,
                        new Point( in.getLocation().x + in.getShapeSize().width + 20, in.getLocation().y + in.getShapeSize().height / 2 ) );
                diagram.put( reaction );
                String emptyName = DefaultSemanticController.generateUniqueName( diagram, "empty" );
                Node emptyNode = new Node( diagram, new Stub( null, emptyName, SbolUtil.TYPE_DEGRADATION_PRODUCT ) );
                emptyNode.setLocation(
                        new Point( in.getLocation().x + in.getShapeSize().width + 55, in.getLocation().y + in.getShapeSize().height / 2 ) );
                diagram.put( emptyNode );

                FunctionalComponent component = SbolUtil.findFunctionalComponent( moduleDefinition,
                        ( (SbolBase)in.getKernel() ).getSbolObject().getDisplayId() );
                Interaction interaction = (Interaction)properties.getSbolObject();
                Participation participation = interaction.createParticipation( name, component.getDisplayId(),
                        SbolUtil.getParticipationURIByType( SbolConstants.PRODUCT ) );
                ParticipationProperties participant = new ParticipationProperties( participation );
                Edge supportEdge = new Edge( participant, in, reaction );
                diagram.put( supportEdge );

                ParticipationProperties kernel = new ParticipationProperties();
                kernel.setType( SbolConstants.DEGRADATION );
                return new Edge( kernel, reaction, emptyNode );
            }

            boolean inputInteraction = in.getKernel() instanceof InteractionProperties;
            boolean outputInteraction = out.getKernel() instanceof InteractionProperties;
            if( ! ( inputInteraction ^ outputInteraction ) )
                throw new IllegalArgumentException( "One and only one of selected nodes should be an interaction." );

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
                Component backBoneComponentInstance = SbolUtil.findComponent( backBoneDefinition, participantDefinition.getDisplayId() );
                FunctionalComponent functionalComponentInstance = SbolUtil.findFunctionalComponent( moduleDefinition,
                        participantDefinition.getDisplayId() );
                FunctionalComponent backboneFunctionalComponent = SbolUtil.findFunctionalComponent( moduleDefinition,
                        backBoneDefinition.getDisplayId() );

                if( backboneFunctionalComponent == null )
                    backboneFunctionalComponent = moduleDefinition.createFunctionalComponent( backBoneDefinition.getDisplayId() + "_fc",
                            AccessType.PUBLIC, backBoneDefinition.getDisplayId(), DirectionType.INOUT );

                if( functionalComponentInstance == null )
                    functionalComponentInstance = moduleDefinition.createFunctionalComponent( participantDefinition.getDisplayId() + "_fc",
                            AccessType.PUBLIC, participantDefinition.getDisplayId(), DirectionType.INOUT );

                backboneFunctionalComponent.createMapsTo( name + "map", RefinementType.USELOCAL, functionalComponentInstance.getIdentity(),
                        backBoneComponentInstance.getIdentity() );

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