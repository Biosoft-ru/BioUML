package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
@PropertyName ( "Simple Bus properties" )
@PropertyDescription ( "Simple Bus properties." )
public class SimpleBusProperties extends Option implements InitialElementProperties
{
    /**
     * Name for bus (it can be different from node name)
     */
    protected String name;
    
    /**
     * If true then new bus for this node will be created, otherwise new node for existing bus will be created
     */
    private boolean newBus = true;
    private boolean directed = false;

    private Color color = Color.RED;
    
    /**
     * Map between names and buses on the diagram, note that one bus corresponds to several nodes
     */
    private Map<String, Bus> nameToBus = new HashMap<>();

    public SimpleBusProperties(Diagram diagram)
    {
        name = DefaultSemanticController.generateUniqueName( diagram, "Bus" );
        nameToBus = diagram.recursiveStream().map( n -> n.getRole() ).select( Bus.class ).distinct().toSortedMap( b -> b.getName(),
                b -> b );
    }

    @PropertyName ( "Name" )
    @PropertyDescription ( "New bus name" )
    public String getName()
    {
        return name;
    }

    //TODO: validate name
    public void setName(String name)
    {
        Object oldValue = this.name;
        this.name = name;
        this.firePropertyChange( "name", oldValue, name );
    }
    
    @PropertyName ( "Existing bus" )
    @PropertyDescription ( "Existing bus name" )
    public String getExistingName()
    {
        return name;
    }

    //TODO: validate name
    public void setExistingName(String name)
    {
        Object oldValue = this.name;
        this.name = name;
        this.firePropertyChange( "name", oldValue, name );
    }

    public void setNewBus(boolean newBus)
    {
        Object oldValue = this.newBus;
        this.newBus = newBus;
        this.setName( ( newBus )? "": getExistingBuses().findAny().orElse( "" ));
        this.firePropertyChange( "newBus", oldValue, newBus );   
        this.firePropertyChange("*", null, null);
    }

    @PropertyName ( "New bus" )
    @PropertyDescription ( "If true then this bus will be new." )
    public boolean isNewBus()
    {
        return newBus;
    }
    
    public boolean isExistingBus()
    {
        return !newBus;
    }

    public Stream<String> getExistingBuses()
    {
        return nameToBus.keySet().stream();
    }

    public Bus findBus(String name)
    {
        return nameToBus.get( name );
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Can not create new Bus node with empty bus name" );

        String nodeName = DefaultSemanticController.generateUniqueName( Diagram.getDiagram( compartment ), name );
        Node node = new Node( compartment, nodeName, new Stub( null, nodeName, Type.TYPE_CONNECTION_BUS ) );
        Bus bus = newBus ? new Bus( name, directed ) : findBus( name );
        if (bus == null)
            throw new Exception("Can not create new Bus node for Bus "+name+". Can not find it.");

        if( newBus )
            bus.setColor( color );
        bus.addNode( node );
        node.setRole( bus );
        node.setTitle( bus.getName() );

        if( viewPane != null )
        {
            node.setNotificationEnabled( true );
            boolean isNotificationEnabled = compartment.isNotificationEnabled();
            compartment.setNotificationEnabled( true );
            viewPane.add( node, location );
            compartment.setNotificationEnabled( isNotificationEnabled );
        }
        //TODO: check if singletone was meaningful
        return new DiagramElementGroup( node );
    }

    @PropertyName("Directed")
    public boolean isDirected()
    {
        return directed;
    }
    public void setDirected(boolean directed)
    {
        this.directed = directed;
    }

    @PropertyName("Color")
    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }
}