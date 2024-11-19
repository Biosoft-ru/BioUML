package biouml.plugins.sbol;

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
import biouml.standard.type.Base;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class SbolDiagramSemanticController extends DefaultSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( isNotificationEnabled );

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
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        Base deBase = de.getKernel();
        Base compartmentBase = compartment.getKernel();

        if( deBase instanceof SequenceFeature )
        {
            return compartmentBase instanceof Backbone;
        }
        else if( deBase instanceof Backbone )
        {
            return compartment instanceof Diagram;
        }
        else if( deBase instanceof MolecularSpecies )
        {
            return compartment instanceof Diagram;
        }

        return false;
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        try
        {
            if( type instanceof Class )
            {
                return ( (Class)type ).getConstructor().newInstance();
            }
        }
        catch( Exception ex )
        {

        }
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
