package biouml.plugins.physicell.cycle;

import java.awt.Point;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.plugins.physicell.PhaseProperties;
import biouml.plugins.physicell.PhysicellUtil;
import biouml.plugins.physicell.TransitionProperties;
import biouml.standard.diagram.CreateEdgeAction;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class CycleDiagramSemanticController extends DefaultSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( isNotificationEnabled );

        if( CycleConstants.TYPE_TRANSITION.equals( type ) )
        {
            new CreateEdgeAction().createEdge( pt, viewEditor, new TransitionCreator() );
            return null;
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
        if( type.equals( CycleConstants.TYPE_PHASE ) )
            return new PhaseProperties( false, Diagram.getDiagram( compartment ).getRole( CycleEModel.class ).isDeathModel() );
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

        if( CycleConstants.TYPE_PHASE.equals( de.getKernel().getType() ) )
        {
            PhysicellUtil.validateRole( de, PhaseProperties.class, CycleConstants.TYPE_PHASE );
        }
        else if( CycleConstants.TYPE_TRANSITION.equals( de.getKernel().getType() ) )
        {
            PhysicellUtil.validateRole( de, TransitionProperties.class, CycleConstants.TYPE_TRANSITION );
        }
        return de;
    }
}