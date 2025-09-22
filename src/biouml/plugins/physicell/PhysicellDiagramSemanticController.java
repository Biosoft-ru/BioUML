package biouml.plugins.physicell;

import java.awt.Point;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.diagram.NoteLinkEdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class PhysicellDiagramSemanticController extends DefaultSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( isNotificationEnabled );

        if( PhysicellConstants.TYPE_SECRETION.equals( type ) )
        {
            new CreateEdgeAction().createEdge( pt, viewEditor, new SecretionCreator() );
            return null;
        }
        if( PhysicellConstants.TYPE_CHEMOTAXIS.equals( type ) )
        {
            new CreateEdgeAction().createEdge( pt, viewEditor, new ChemotaxisCreator() );
            return null;
        }
        if( PhysicellConstants.TYPE_INTERACTION.equals( type ) )
        {
            new CreateEdgeAction().createEdge( pt, viewEditor, new InteractionCreator() );
            return null;
        }
        if( PhysicellConstants.TYPE_TRANSFORMATION.equals( type ) )
        {
            new CreateEdgeAction().createEdge( pt, viewEditor, new TransformationCreator() );
            return null;
        }
        else if( PhysicellConstants.TYPE_NOTE.equals( type ) )
        {
            String id = generateUniqueNodeName(parent, PhysicellConstants.TYPE_NOTE);
            return new DiagramElementGroup( new Node( parent, new Stub.Note( null, id ) ) );
        }
        else if( PhysicellConstants.TYPE_NOTE_LINK.equals( type ) )
        {
            new CreateEdgeAction().createEdge(pt, viewEditor, new NoteLinkEdgeCreator());
            return DiagramElementGroup.EMPTY_EG;
        }
        
        try
        {
            Object properties = getPropertiesByType( parent, type, pt );
            if( properties instanceof InitialElementProperties )
            {
                if( new PropertiesDialog( Application.getApplicationFrame(), "New element", properties ).doModal() )
                    ( (InitialElementProperties)properties ).createElements( parent, pt, viewEditor );
                return DiagramElementGroup.EMPTY_EG;
            }
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Error during element creation", ex );
            return DiagramElementGroup.EMPTY_EG;
        }
        finally
        {
            parent.setNotificationEnabled( isNotificationEnabled );
        }
        return super.createInstance( parent, type, pt, viewEditor );
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        if( PhysicellConstants.TYPE_CELL_DEFINITION.equals( type ) )
            return new CellDefinitionProperties( DefaultSemanticController.generateUniqueName( diagram, "CellDefinition" ) );
        else if( PhysicellConstants.TYPE_SUBSTRATE.equals( type ) )
            return new SubstrateProperties( DefaultSemanticController.generateUniqueName( diagram, "Substrate" ) );
        else if( PhysicellConstants.TYPE_EVENT.equals( type ) )
            return new EventProperties( DefaultSemanticController.generateUniqueName( diagram, "Event" ) );
        return null;
    }

    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        return true;
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        if( de.getRole() != null || de.getKernel() == null )
            return de;

        if( PhysicellConstants.TYPE_SUBSTRATE.equals( de.getKernel().getType() ) )
        {
            SubstrateProperties sp = PhysicellUtil.validateRole( de, SubstrateProperties.class, PhysicellConstants.TYPE_SUBSTRATE );
            sp.setDiagramElement( de );
        }
        else if( PhysicellConstants.TYPE_CELL_DEFINITION.equals( de.getKernel().getType() ) )
        {
            CellDefinitionProperties cdp = PhysicellUtil.validateRole( de, CellDefinitionProperties.class, "cellDefinition" );
            fixPhases(cdp);
            cdp.setDiagramElement( de );
        }
        else if( PhysicellConstants.TYPE_EVENT.equals( de.getKernel().getType() ) )
        {
            EventProperties ep = PhysicellUtil.validateRole( de, EventProperties.class, "event" );
            ep.setDiagramElement( de );
        }
        else if( PhysicellConstants.TYPE_SECRETION.equals( de.getKernel().getType() ) )
        {
            SecretionProperties role = PhysicellUtil.validateRole( de, SecretionProperties.class, PhysicellConstants.TYPE_SECRETION );
            role.setDiagramElement( de );
            if( de instanceof Edge )
            {
                CellDefinitionProperties cdp = PhysicellUtil.findNode( (Edge)de, CellDefinitionProperties.class );
                if( cdp != null )
                    cdp.getSecretionsProperties().addSecretion( role );
            }
        }
        else if( PhysicellConstants.TYPE_CHEMOTAXIS.equals( de.getKernel().getType() ) )
        {
            ChemotaxisProperties role = PhysicellUtil.validateRole( de, ChemotaxisProperties.class, PhysicellConstants.TYPE_CHEMOTAXIS );
            role.setDiagramElement( de );
            if( de instanceof Edge )
            {
                CellDefinitionProperties cdp = PhysicellUtil.findNode( (Edge)de, CellDefinitionProperties.class );
                if( cdp != null )
                    cdp.getMotilityProperties().addChemotaxis( role );
            }
        }
        else if( PhysicellConstants.TYPE_INTERACTION.equals( de.getKernel().getType() ) )
        {
            InteractionProperties role = PhysicellUtil.validateRole( de, InteractionProperties.class, PhysicellConstants.TYPE_INTERACTION );
            role.setDiagramElement( de );
            if( de instanceof Edge )
            {
                CellDefinitionProperties cdp = PhysicellUtil.findNode( (Edge)de, CellDefinitionProperties.class );
                if( cdp != null )
                    cdp.getInteractionsProperties().addInteraction( role );
            }
        }
        else if( PhysicellConstants.TYPE_TRANSFORMATION.equals( de.getKernel().getType() ) )
        {
            TransformationProperties role = PhysicellUtil.validateRole( de, TransformationProperties.class,
                    PhysicellConstants.TYPE_TRANSFORMATION );
            role.setDiagramElement( de );
            if( de instanceof Edge )
            {
                CellDefinitionProperties cdp = PhysicellUtil.findNode( (Edge)de, CellDefinitionProperties.class );
                if( cdp != null )
                    cdp.getTransformationsProperties().addTransformation( role );
            }
        }
        return de;
    }
    
    private void fixPhases(CellDefinitionProperties cd)
    {
        for (DeathModelProperties dmp: cd.getDeathProperties().getDeathModels())
        {
            for (PhaseProperties pp: dmp.getCycle().getPhases())
                pp.setDeathPhase( true );
        }
            
    }
    
}