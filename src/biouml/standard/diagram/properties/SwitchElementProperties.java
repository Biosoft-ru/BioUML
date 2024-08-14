package biouml.standard.diagram.properties;

import java.awt.Dimension;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramViewBuilder;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.SubDiagram.PortOrientation;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.Util;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.SwitchElement;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;

@PropertyName("Switch element properties")
@PropertyDescription("Switch element properties.")
public class SwitchElementProperties extends InitialElementPropertiesSupport
{
    protected static final Logger log = Logger.getLogger(SwitchElementProperties.class.getName());
    
    private String name;
    
    public SwitchElementProperties(String name)
    {
        this.name = name;
    }
    
    @PropertyName("Name")
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane)
    {
         return new DiagramElementGroup(createSwitchNode(compartment, location));
    }
    
    /**
     * Create node for switch element
     */
    public Compartment createSwitchNode(Compartment parent, Point pt)
    {
        try
        {
            Diagram diagram = Diagram.getDiagram( parent );
            
            Compartment switchNode = new Compartment(parent, new SwitchElement(parent, DefaultSemanticController.generateUniqueNodeName(diagram, name)));
            switchNode.getAttributes().add(DPSUtils.createHiddenReadOnly( Node.INNER_NODES_PORT_FINDER_ATTR, Boolean.class, true ));
            switchNode.getAttributes().add(new DynamicProperty(Util.CONDITION, String.class, "false"));
            switchNode.setShapeSize(new Dimension(120, 100));

            DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
            viewBuilder.createCompartmentView( switchNode, diagram.getViewOptions(), ApplicationUtils.getGraphics());     
            
            Node defaultPort = new Node(switchNode, new Stub.InputConnectionPort(parent, "default"));
            Util.setOrientation( defaultPort, PortOrientation.LEFT);
            Util.setPortVariable( defaultPort, "default" );
            defaultPort.setLocation(new Point(0, 20));
            switchNode.put(defaultPort);

            Node experimentPort = new Node(switchNode, new Stub.InputConnectionPort(parent, "experiment"));
            Util.setOrientation( experimentPort, PortOrientation.LEFT);
            Util.setPortVariable( experimentPort, "experiment" );
            experimentPort.setLocation(new Point(0, 68));
            switchNode.put(experimentPort);

            Node outPort = new Node(switchNode, new Stub.OutputConnectionPort(parent, "out"));
            Util.setOrientation( outPort, PortOrientation.RIGHT);
            Util.setPortVariable( outPort, "out" );
            View outPortView = viewBuilder.createNodeView( outPort, diagram.getViewOptions(), ApplicationUtils.getGraphics());   
            outPort.setLocation(120 - outPortView.getBounds().width, 44);
            switchNode.put(outPort);
           
            CompositeSemanticController.movePortToEdge( outPort, switchNode, outPort.getLocation(), true);
            return switchNode;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during switch node creation: " + ex.getMessage());
            return null;
        }
    }
}