package biouml.standard.diagram;

import java.awt.Point;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;
import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Node;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.TableElement;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import com.developmentontheedge.application.Application;

public class MathDiagramSemanticController extends PathwaySimulationSemanticController implements CreatorElementWithName
{

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if( de.getRole() instanceof TableElement )
            return true;

        return super.canAccept( compartment, de );
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, String name, Point point, Object properties)
    {
        if( type instanceof String )
        {
            if( type.equals( Type.TYPE_BLOCK ) )
            {
                return new DiagramElementGroup(
                        new Compartment( parent, new Stub( null, name, Type.TYPE_BLOCK ) ) );
            }
        }
        
        Class typeClass = (Class)type;
        if( typeClass == Event.class )
        {
            Node node = new Node(parent, new Stub(null, name, Type.MATH_EVENT));
            Event event = new Event(node);
            node.setRole(event);
            return new DiagramElementGroup( node );
        }
        else if( typeClass == Equation.class )
        {
            Node node = new Node(parent, new Stub(null, name, Type.MATH_EQUATION));
            node.setRole(new Equation(node, Equation.TYPE_SCALAR, "unknown", "0"));
            node.setShowTitle(false);
            if (properties instanceof Equation)
                node.getRole(Equation.class).setType(( (Equation)properties ).getType());
            return new DiagramElementGroup( node );
        }
        else if( typeClass == Constraint.class )
        {
            Node node = new Node(parent, new Stub(null, generateUniqueNodeName(parent, Type.MATH_CONSTRAINT), Type.MATH_CONSTRAINT));
            node.setRole(new Constraint(node));
            node.setShowTitle(false);
            return new DiagramElementGroup( node );
        }
        else if( typeClass == Function.class )
        {
            Node node = new Node(parent, new Stub(null, name, Type.MATH_FUNCTION));
            node.setRole(new Function(node));
            node.setShowTitle(false);
            return new DiagramElementGroup( node );
        }
        return DiagramElementGroup.EMPTY_EG;
    }


    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        if( type instanceof String )
        {
            if( type.equals( Type.TYPE_BLOCK ) )
            {
                return new DiagramElementGroup(
                        new Compartment( parent, new Stub( null, generateUniqueNodeName( parent, Type.TYPE_BLOCK ), Type.TYPE_BLOCK ) ) );
            }
        }
        
        Class<?> typeClass = (Class<?>)type;

        if( typeClass == TableElement.class )
        {
            String name = generateUniqueNodeName( parent, Type.TYPE_TABLE_ELEMENT );
            Node node = new Node( parent, new Stub( null, name, Type.TYPE_TABLE_ELEMENT ) );
            TableElement role = new TableElement( node );
            node.setRole( role );

            PropertiesDialog dialog = new PropertiesDialog( Application.getApplicationFrame(), "New " + Type.TYPE_TABLE_ELEMENT, role );
            if( dialog.doModal() )
            {
                if( role.getTable() == null )
                {
                    log.log(Level.SEVERE,  "Please specify table" );
                    return null;
                }
                return new DiagramElementGroup( node );
            }

            return DiagramElementGroup.EMPTY_EG;
        }

        return super.createInstance( parent, type, point, viewEditor );
    }
}
