package biouml.standard.diagram;

import java.awt.Point;

import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.dynamics.SimpleTableElement;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

@PropertyName ( "Table properties" )
@PropertyDescription ( "Table properties." )
public class SimpleTableElementProperties extends InitialElementPropertiesSupport
{
    private SimpleTableElement element;
    private String name;
    
    public SimpleTableElementProperties(String name)
    {
       this.element = new SimpleTableElement( null );
       this.name = name;
    }

    @PropertyName ( "Name" )
    @PropertyDescription ( "Name." )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    
    @PropertyName ( "Properties" )
    @PropertyDescription ( "Properties." )
    public SimpleTableElement getElement()
    {
        return element;
    }
    public void setElement(SimpleTableElement element)
    {
        this.element = element;
    }
    
    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node tableNode = new Node( compartment, new Stub(null, name, Type.TYPE_TABLE) );
        SimpleTableElement role = (SimpleTableElement)element.clone( tableNode );
        tableNode.setRole( role );
        return new DiagramElementGroup( tableNode );
    }
}