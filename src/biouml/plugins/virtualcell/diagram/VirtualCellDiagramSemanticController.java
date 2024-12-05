package biouml.plugins.virtualcell.diagram;

import java.awt.Point;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.standard.diagram.CreateEdgeAction;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class VirtualCellDiagramSemanticController extends DefaultSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( false );

        if( "Connection".equals( type ) )
        {
            new CreateEdgeAction().createEdge( pt, viewEditor, new ConnectionCreator() );
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
            //            log.log( Level.SEVERE, "Error during element creation", ex );
            return DiagramElementGroup.EMPTY_EG;
        }
        finally
        {
            parent.setNotificationEnabled( isNotificationEnabled );
        }
        return super.createInstance( parent, type, pt, viewEditor );
        //        return super.createInstance( parent, type, pt, viewEditor );
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type.equals( "Pool" ) )
            return new TableCollectionPoolProperties( DefaultSemanticController.generateUniqueName( compartment, "Pool" ) );
        else if( type.equals( "Process" ) )
            return new ProcessProperties( DefaultSemanticController.generateUniqueName( compartment, "Process" ) );
        else if( type.equals( "Translation" ) )
            return new TranslationProperties( DefaultSemanticController.generateUniqueName( compartment, "Translation" ) );
        else if( type.equals( "ProteinDegradation" ) )
            return new ProteinDegradationProperties( DefaultSemanticController.generateUniqueName( compartment, "Protein Degradation" ) );
        else if( type.equals( "Population" ) )
            return new PopulationProperties( DefaultSemanticController.generateUniqueName( compartment, "Population" ) );
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
        return de;
    }
}