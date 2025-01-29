package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.net.URI;

import javax.annotation.Nonnull;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.FunctionalComponent;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.MapsTo;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.Participation;
import org.sbolstandard.core2.RefinementType;
import org.sbolstandard.core2.SBOLDocument;

import biouml.model.Compartment;
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

            if( temporary )
            {
                String name = CollectionFactory.getRelativeName( in, diagram ).replace( "/", "_" ) + "_to_"
                        + CollectionFactory.getRelativeName( out, diagram ).replace( "/", "_" );
                name = SbolUtil.generateUniqueName( diagram, name );
                return new Edge( new Stub( null, name ), in, out );
            }
            else if( in.getKernel() instanceof MolecularSpecies && out instanceof Diagram )
            {
                return createDegradation( in, diagram );
            }
            else if( in.getKernel() instanceof MolecularSpecies && out.getKernel() instanceof MolecularSpecies )
            {
                return createSpeciesInteraction( diagram, in, out );
            }
            else if( in.getKernel() instanceof MolecularSpecies )
            {
                if( out.getKernel() instanceof InteractionProperties )
                    return createSpeciesParticipant( diagram, in, out );
                else if( out.getKernel() instanceof SequenceFeature )
                    return createSpeciesFeaturInteraction( diagram, in, out );
            }
            else if( out.getKernel() instanceof MolecularSpecies )
            {
                if( in.getKernel() instanceof InteractionProperties )
                    return createSpeciesParticipant( diagram, in, out );
                else if( in.getKernel() instanceof SequenceFeature )
                    return createSpeciesFeaturInteraction( diagram, in, out );
            }          
            else if( in.getKernel() instanceof SequenceFeature || out.getKernel() instanceof SequenceFeature )
            {
                return createFeatureParticipant( diagram, in, out );
            }
            else
                throw new IllegalArgumentException(
                        "Can not create participation edge for nodes " + in.getName() + " and " + out.getName() );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Creates interaction between two species and all neccessary edges between them
     */
    private Edge createSpeciesInteraction(Diagram diagram, Node in, Node out) throws Exception
    {
        SBOLDocument doc = SbolUtil.getDocument( diagram );

        //create interaction
        InteractionProperties properties = new InteractionProperties( SbolUtil.generateUniqueName( diagram, "Process" ) );
        properties.setType( SbolConstants.PROCESS );
        int x = ( in.getLocation().x + out.getLocation().x ) / 2;
        int y = ( in.getLocation().y + out.getLocation().y ) / 2;
        Node reaction = properties.doCreateInteraction( diagram, doc, new Point( x, y ) );
        diagram.put( reaction );

        Edge inputEdge = createSpeciesParticipant( diagram, in, reaction );
        diagram.put( inputEdge );
        Edge outputEdge = createSpeciesParticipant( diagram, reaction, out );
        return outputEdge;
    }
    
    private Edge createSpeciesFeaturInteraction(Diagram diagram, Node in, Node out) throws Exception
    {
        SBOLDocument doc = SbolUtil.getDocument( diagram );

        //create interaction
        InteractionProperties properties = new InteractionProperties( SbolUtil.generateUniqueName( diagram, "Process" ) );
        properties.setType( SbolConstants.PROCESS );
        int x = ( in.getLocation().x + out.getLocation().x ) / 2;
        int y = ( in.getLocation().y + out.getLocation().y ) / 2;
        Node reaction = properties.doCreateInteraction( diagram, doc, new Point( x, y ) );
        diagram.put( reaction );

        if( in.getKernel() instanceof MolecularSpecies )
        {
            diagram.put( createSpeciesParticipant( diagram, in, reaction ) );
            return  createFeatureParticipant( diagram, reaction, out );
        }
        else
        {
            diagram.put( createFeatureParticipant( diagram, in, reaction ) );
            return createSpeciesParticipant( diagram, reaction, out ) ;
        }
    }

    /**
     * Creates reaction participant for molecular species
     */
    private Edge createSpeciesParticipant(Diagram diagram, Node in, Node out) throws Exception
    {
        String name = CollectionFactory.getRelativeName( in, diagram ).replace( "/", "_" ) + "_to_"
                + CollectionFactory.getRelativeName( out, diagram ).replace( "/", "_" );
        name = SbolUtil.generateUniqueName( diagram, name );

        SBOLDocument doc = SbolUtil.getDocument( diagram );
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( doc );
        boolean inputInteraction = in.getKernel() instanceof InteractionProperties;
        Node interactionNode = inputInteraction ? in : out;
        Node partNode = inputInteraction ? out : in;
        Interaction interaction = SbolUtil.getSbolObject( interactionNode, Interaction.class );
        URI type = inputInteraction ? SbolUtil.getParticipationURIByType( SbolConstants.PRODUCT )
                : SbolUtil.getParticipationURIByType( SbolConstants.REACTANT );
        FunctionalComponent component = SbolUtil.findFunctionalComponent( moduleDefinition, SbolUtil.getDisplayId( partNode ) );
        Participation participation = interaction.createParticipation( name, component.getDisplayId(), type );
        return new Edge( new ParticipationProperties( participation ), in, out );
    }

    /**
     * Creates reaction participant for sequence feature on a back bone
     */
    private Edge createFeatureParticipant(Diagram diagram, Node in, Node out) throws Exception
    {
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( doc );
        boolean inputInteraction = in.getKernel() instanceof InteractionProperties;
        Node interactionNode = inputInteraction ? in : out;
        Node partNode = inputInteraction ? out : in;
        Interaction interaction = SbolUtil.getSbolObject( interactionNode, Interaction.class );
        ComponentDefinition participantDef = SbolUtil.getSbolObject( partNode, ComponentDefinition.class );

        Compartment parent = partNode.getCompartment();
        Backbone backbone = (Backbone)parent.getKernel();
        ComponentDefinition backBoneDef = (ComponentDefinition)backbone.getSbolObject();
        Component backBoneComponent = SbolUtil.findComponent( backBoneDef, participantDef.getDisplayId() );

        FunctionalComponent backboneFunc = SbolUtil.findFunctionalComponent( moduleDefinition, backBoneDef.getDisplayId() );
        if( backboneFunc == null )
            backboneFunc = SbolUtil.createFunctionalComponent( moduleDefinition, backBoneDef );

        FunctionalComponent participantFunc = SbolUtil.findFunctionalComponent( moduleDefinition, participantDef.getDisplayId() );
        if( participantFunc == null )
            participantFunc = SbolUtil.createFunctionalComponent( moduleDefinition, participantDef );

        boolean alreadyExists = false;
        for( MapsTo maps : backboneFunc.getMapsTos() )
        {
            URI identityRemote = maps.getRemoteIdentity();
            URI identityLocal = maps.getLocalIdentity();
            if( identityRemote.equals( backBoneComponent.getIdentity() ) && identityLocal.equals( participantFunc.getIdentity() ) )
                alreadyExists = true;
            break;
        }

        String name = CollectionFactory.getRelativeName( in, diagram ).replace( "/", "_" ) + "_to_"
                + CollectionFactory.getRelativeName( out, diagram ).replace( "/", "_" );
        name = SbolUtil.generateUniqueName( diagram, name );

        if( !alreadyExists )
            backboneFunc.createMapsTo( name + "map", RefinementType.USELOCAL, participantFunc.getIdentity(),
                    backBoneComponent.getIdentity() );
        URI type = inputInteraction ? SbolUtil.getParticipationURIByType( SbolConstants.PRODUCT )
                : SbolUtil.getParticipationURIByType( SbolConstants.REACTANT );
        Participation participation = interaction.createParticipation( name, participantFunc.getDisplayId(), type );
        return new Edge( new ParticipationProperties( participation ), in, out );
    }

    /**
     * Creates degradation reaction for given node, empty set and edges
     */
    private Edge createDegradation(Node node, Diagram diagram) throws Exception
    {
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( doc );

        //create interaction
        InteractionProperties properties = new InteractionProperties(
                SbolUtil.generateUniqueName( diagram, node.getName() + "_degradation" ) );
        properties.setType( SbolConstants.DEGRADATION );
        int x = node.getLocation().x + node.getShapeSize().width + 20;
        int y = node.getLocation().y + node.getShapeSize().height / 2;
        Node reaction = properties.doCreateInteraction( diagram, doc, new Point( x, y ) );
        diagram.put( reaction );

        //create empty set node
        x = node.getLocation().x + node.getShapeSize().width + 55;
        String emptyName = SbolUtil.generateUniqueName( diagram, node.getName() + "_degradation_product" );
        Node emptyNode = new Node( diagram, new Stub( null, emptyName, SbolConstants.DEGRADATION ) );
        emptyNode.setShapeSize( new Dimension( 25, 25 ) );
        emptyNode.setLocation( new Point( x, y ) );
        diagram.put( emptyNode );

        //create participant edge
        String definitionID = SbolUtil.getDisplayId( node );
        FunctionalComponent component = SbolUtil.findFunctionalComponent( moduleDefinition, definitionID );
        Interaction interaction = (Interaction)properties.getSbolObject();
        Participation participation = interaction.createParticipation( node.getName() + "_to_" + reaction.getName(),
                component.getDisplayId(), SbolUtil.getParticipationURIByType( SbolConstants.PRODUCT ) );
        ParticipationProperties participant = new ParticipationProperties( participation );
        participant.setType( SbolConstants.REACTANT );
        Edge supportEdge = new Edge( participant, node, reaction );
        diagram.put( supportEdge );

        //create degradation edge
        ParticipationProperties kernel = new ParticipationProperties();
        kernel.setName( reaction.getName() + "_to_" + emptyName );
        kernel.setType( SbolConstants.PRODUCT );
        return new Edge( kernel, reaction, emptyNode );
    }

}