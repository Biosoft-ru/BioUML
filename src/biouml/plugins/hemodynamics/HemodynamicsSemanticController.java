package biouml.plugins.hemodynamics;

import java.awt.Point;
import javax.annotation.Nonnull;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.standard.diagram.PathwaySimulationSemanticController;
import biouml.standard.diagram.PortProperties;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

import com.developmentontheedge.application.Application;

public class HemodynamicsSemanticController extends PathwaySimulationSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        try
        {
            Object properties = getPropertiesByType(parent, type, point);
            if( properties != null )
            {
                PropertiesDialog dialog = new PropertiesDialog(Application.getApplicationFrame(), "New element", properties);
                if( dialog.doModal() )
                {
                    if( properties instanceof InitialElementProperties )
                        ( (InitialElementProperties)properties ).createElements(parent, point, viewEditor);
                    return null;
                }
            }
        }
        catch( Throwable t )
        {
            throw ExceptionRegistry.translateException(t);
        }
        return super.createInstance(parent, type, point, viewEditor);
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        try
        {
            if( HemodynamicsType.BIFURCATION.equals(type) )
                return new BifurcationProperties(Diagram.getDiagram(compartment));
            else if( HemodynamicsType.CONTROL_POINT.equals(type) )
                return new ControlPointProperties(Diagram.getDiagram(compartment));
            else if( Type.TYPE_PORT.equals(type) )
                return new PortProperties(Diagram.getDiagram(compartment), ConnectionPort.class);

            return super.getPropertiesByType(compartment, type, point);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        return true;
    }
}