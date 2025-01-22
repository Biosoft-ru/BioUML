package biouml.plugins.sbol;

import java.awt.Point;

import javax.annotation.Nonnull;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
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


            String name = CollectionFactory.getRelativeName( in, diagram ).replace( "/", "_" ) + "_to_"
                    + CollectionFactory.getRelativeName( out, diagram ).replace( "/", "_" );
            name = DefaultSemanticController.generateUniqueName( diagram, name );

            if( temporary )
            {
                return new Edge( new Stub( null, name ), in, out );
            }
            else if( in.getKernel() instanceof MolecularSpecies && out instanceof Diagram )
            {
                return createDegradation( name, in, diagram );
            }
            else if( in.getKernel() instanceof MolecularSpecies && out.getKernel() instanceof MolecularSpecies )
            {
                return createSpeciesParticipant( name, diagram, in, out );
            }
            else if( in.getKernel() instanceof MolecularSpecies || out.getKernel() instanceof MolecularSpecies )
            {
                return createSpeciesParticipant( name, diagram, in, out );
            }
            else if( in.getKernel() instanceof SequenceFeature || out.getKernel() instanceof SequenceFeature )
            {
                return createFeatureParticipant( name, diagram, in, out );
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
//    private Edge createSpeciesInteraction(String name, Diagram diagram, Node in, Node out) throws Exception
//    {
//        SBOLDocument doc = SbolUtil.getDocument( diagram );
//        InteractionProperties properties = new InteractionProperties();
//        properties.setName( DefaultSemanticController.generateUniqueName( diagram, "Interaction" ) );
//        Point location = new Point( ( in.getLocation().x + out.getLocation().x ) / 2, ( in.getLocation().y + out.getLocation().y ) / 2 );
//        Node interactionNode = properties.doCreateInteraction( diagram, doc, location );
//    }

    /**
     * Creates reaction participant for molecular species
     */
    private Edge createSpeciesParticipant(String name, Diagram diagram, Node in, Node out) throws Exception
    {
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( doc );
        boolean inputInteraction = in.getKernel() instanceof InteractionProperties;
        InteractionProperties interactionProperties = inputInteraction ? (InteractionProperties)in.getKernel()
                : (InteractionProperties)out.getKernel();
        SbolBase participantKernel = inputInteraction ? (SbolBase)out.getKernel() : (SbolBase)in.getKernel();

        Identified participantDefinition = participantKernel.getSbolObject();
        Interaction interaction = (Interaction)interactionProperties.getSbolObject();
        FunctionalComponent component = SbolUtil.findFunctionalComponent( moduleDefinition, participantDefinition.getDisplayId() );
        Participation participation = interaction.createParticipation( name, component.getDisplayId(),
                SbolUtil.getParticipationURIByType( SbolConstants.STIMULATOR ) );
        return new Edge( new ParticipationProperties( participation ), in, out );
    }

    /**
     * Creates reaction participant for sequence feature on a back bone
     */
    private Edge createFeatureParticipant(String name, Diagram diagram, Node in, Node out) throws Exception
    {
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( doc );
        boolean inputInteraction = in.getKernel() instanceof InteractionProperties;
        Node interactionNode = inputInteraction ? in : out;
        Node partNode = inputInteraction ? out : in;
        Interaction interaction = (Interaction) ( (InteractionProperties)interactionNode.getKernel() ).getSbolObject();
        ComponentDefinition participantDef = (ComponentDefinition) ( (SbolBase)partNode.getKernel() ).getSbolObject();

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

        backboneFunc.createMapsTo( name + "map", RefinementType.USELOCAL, participantFunc.getIdentity(), backBoneComponent.getIdentity() );
        Participation participation = interaction.createParticipation( name, participantFunc.getDisplayId(),
                SbolUtil.getParticipationURIByType( SbolConstants.STIMULATOR ) );
        return new Edge( new ParticipationProperties( participation ), in, out );
    }

    /**
     * Creates degradation reaction for given node, empty set and edges
     */
    private Edge createDegradation(String name, Node node, Diagram diagram) throws Exception
    {
        SBOLDocument doc = SbolUtil.getDocument( diagram );
        ModuleDefinition moduleDefinition = SbolUtil.getDefaultModuleDefinition( doc );

        //create interaction
        InteractionProperties properties = new InteractionProperties("Degradation");
        properties.setType( SbolConstants.DEGRADATION );
        properties.setName( DefaultSemanticController.generateUniqueName( diagram, "Degradation" ) );
        int x = node.getLocation().x + node.getShapeSize().width + 20;
        int y = node.getLocation().y + node.getShapeSize().height / 2;
        Node reaction = properties.doCreateInteraction( diagram, doc, new Point( x, y ) );
        diagram.put( reaction );

        //create empty set node
        x = node.getLocation().x + node.getShapeSize().width + 55;
        String emptyName = DefaultSemanticController.generateUniqueName( diagram, "empty" );
        Node emptyNode = new Node( diagram, new Stub( null, emptyName, SbolUtil.TYPE_DEGRADATION_PRODUCT ) );
        emptyNode.setLocation( new Point( x, y ) );
        diagram.put( emptyNode );

        //create participant edge
        String definitionID = ( (SbolBase)node.getKernel() ).getSbolObject().getDisplayId();
        FunctionalComponent component = SbolUtil.findFunctionalComponent( moduleDefinition, definitionID );
        Interaction interaction = (Interaction)properties.getSbolObject();
        Participation participation = interaction.createParticipation( name, component.getDisplayId(),
                SbolUtil.getParticipationURIByType( SbolConstants.PRODUCT ) );
        ParticipationProperties participant = new ParticipationProperties( participation );
        Edge supportEdge = new Edge( participant, node, reaction );
        diagram.put( supportEdge );

        //create degradation edge
        ParticipationProperties kernel = new ParticipationProperties();
        kernel.setType( SbolConstants.DEGRADATION );
        return new Edge( kernel, reaction, emptyNode );
    }

}