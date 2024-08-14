package biouml.standard.diagram.properties;

import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.standard.diagram.Util;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.editor.ViewEditorPane;

@PropertyName("Constant element properties")
@PropertyDescription("Constant element properties.")
public class ConstantElementProperties extends InitialElementPropertiesSupport
{
    protected static final Logger log = Logger.getLogger(SwitchElementProperties.class.getName());
    
    private String name;
    private double value = 0;
    
    public ConstantElementProperties( String name)
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
    
    @PropertyName("Value")
    public double getValue()
    {
        return value;
    }
    public void setValue(double value)
    {
        this.value = value;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane)
    {
         return new DiagramElementGroup(createConstantNode(compartment, name, location));
    }

    public Node createConstantNode(Compartment parent, String name, Point location)
    {
        try
        {
            Diagram diagram = Diagram.getDiagram( parent );
            Node node = new Node(parent, new Stub.Constant(parent, DefaultSemanticController.generateUniqueNodeName(diagram, name)));
            node.getAttributes().add(new DynamicProperty(Util.INITIAL_VALUE, Double.class, value));
            node.setLocation( location );
            return node;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during constant node creation: " + ex.getMessage());
            return null;
        }
    }
}