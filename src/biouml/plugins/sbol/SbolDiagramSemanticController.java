package biouml.plugins.sbol;

import java.awt.Point;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class SbolDiagramSemanticController extends DefaultSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( isNotificationEnabled );
        //
        //            if( PhysicellConstants.TYPE_SECRETION.equals( type ) )
        //            {
        //                new CreateEdgeAction().createEdge( pt, viewEditor, new SecretionCreator() );
        //                return null;
        //            }
        //            if( PhysicellConstants.TYPE_CHEMOTAXIS.equals( type ) )
        //            {
        //                new CreateEdgeAction().createEdge( pt, viewEditor, new ChemotaxisCreator() );
        //                return null;
        //            }
        //            if( PhysicellConstants.TYPE_INTERACTION.equals( type ) )
        //            {
        //                new CreateEdgeAction().createEdge( pt, viewEditor, new InteractionCreator() );
        //                return null;
        //            }
        //            if( PhysicellConstants.TYPE_TRANSFORMATION.equals( type ) )
        //            {
        //                new CreateEdgeAction().createEdge( pt, viewEditor, new TransformationCreator() );
        //                return null;
        //            }
        //            else if( Type.TYPE_NOTE.equals( type ) )
        //                return super.createInstance( parent, type, pt, viewEditor );
        //
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
        if (Backbone.class.equals( type ))
            return new Backbone();
        //            Diagram diagram = Diagram.getDiagram( compartment );
        //            if( PhysicellConstants.TYPE_CELL_DEFINITION.equals( type ) )
        //                return new CellDefinitionProperties( DefaultSemanticController.generateUniqueName( diagram, "CellDefinition" ) );
        //            else if( PhysicellConstants.TYPE_SUBSTRATE.equals( type ) )
        //                return new SubstrateProperties( DefaultSemanticController.generateUniqueName( diagram, "Substrate" ) );
        //            else if( PhysicellConstants.TYPE_EVENT.equals( type ) )
        //                return new EventProperties( DefaultSemanticController.generateUniqueName( diagram, "Event" ) );
        return null;
    }

    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        return false;
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        return de;
    }
}
