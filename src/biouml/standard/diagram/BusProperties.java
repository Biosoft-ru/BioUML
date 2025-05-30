package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Point;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Stub.Bus;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
@PropertyName("Bus properties")
@PropertyDescription("Bus properties.")
public class BusProperties extends Option implements InitialElementProperties
{

    public static final String NEW_VARIABLE_CONSTANT = "<new>";

    protected String name;
    protected VariableRole variable;
    protected EModel model;

    public BusProperties(EModel model)
    {
        this.model = model;
    }

    public BusProperties(EModel model, String name)
    {
        this( model );
        setName( name );
    }


    @PropertyName("Name")
    @PropertyDescription("New variable name")
    public String getName()
    {
        return name;
    }

    //TODO: validate name
    public void setName(String name)
    {
        Variable var = model.getVariable( name );
        this.name = name;
        this.variable = ( var instanceof VariableRole ) ? (VariableRole)var : null;
        this.firePropertyChange( "*", null, null );
    }

    public void setVariable(String variable)
    {
        if( variable.equals( NEW_VARIABLE_CONSTANT ) )
        {
            if( this.variable != null )
            {
                this.name = "";
                this.variable = null;
            }
        }
        else if( variable.startsWith( "$" ) )
        {
            setName( variable.substring( 1 ) );
        }
    }

    @PropertyName("Variable")
    @PropertyDescription("Variable, if <new> is chosen then new variable name will be specified by name field")
    public String getVariable()
    {
        if( variable == null )
            return NEW_VARIABLE_CONSTANT;
        return variable.getName();
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty bus name" );

        String nodeName = generateUniqueBusName( compartment, name );
        Node node = new Node( compartment, nodeName, new Bus( null, nodeName ) );
        node.setNotificationEnabled( false );
        if( variable == null )
            variable = new VariableRole( node, 0.0 );

        DiagramElement[] nodes = variable.getAssociatedElements();
        Node otherNode = StreamEx.of(nodes).select(Node.class).without(node).findAny().orElse(null);
        if( otherNode != null && otherNode.getAttributes().getProperty("color") != null)
            node.getAttributes().add(otherNode.getAttributes().getProperty("color"));
        else
            node.getAttributes().add( new DynamicProperty( "color", Brush.class, new Brush( Color.gray ) ) );

        variable.addAssociatedElement( node );
        node.setRole( variable );
        node.setTitle( variable.getName().replace( VariableRole.PREFIX, "" ) );

        Diagram diagram = Diagram.getDiagram(compartment);
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept(compartment, node) )
            return new DiagramElementGroup();
        
        if( viewPane != null )
        {
            node.setNotificationEnabled(true);
            boolean isNotificationEnabled = compartment.isNotificationEnabled();
            compartment.setNotificationEnabled(true);
            viewPane.add(node, location);
            compartment.setNotificationEnabled(isNotificationEnabled);
        }
        //TODO: check if singletone was meaningful
        return new DiagramElementGroup( node );
    }

    public static String generateUniqueBusName(Compartment parent, String name)
    {
        String result = name;
        int index = 0;
        while( parent.get(result) != null )
        {
            index++;
            result = name + "(" + index + ")";
        }
        return result;
    }
}