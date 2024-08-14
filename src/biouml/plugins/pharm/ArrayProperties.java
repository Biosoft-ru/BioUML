package biouml.plugins.pharm;

import java.awt.Dimension;
import java.awt.Point;
import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.SemanticController;
import biouml.standard.type.Stub;

public class ArrayProperties implements InitialElementProperties
{

    public ArrayProperties(String name)
    {
        this.name = name;
    }

    private String name = "array";
    private String index = "i";
    private String from = "1";
    private String upTo = "N";

    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new IllegalArgumentException( "Please specify node name" );

        Compartment node = new Compartment(c, new Stub(null, name, Type.TYPE_ARRAY));
        node.getAttributes().add( new DynamicProperty( "index", String.class, index ) );
        node.getAttributes().add( new DynamicProperty( "from", String.class, from ) );
        node.getAttributes().add( new DynamicProperty( "up to", String.class, upTo ) );

        node.setLocation(location);
        node.setShapeSize( new Dimension(200,200) );
        SemanticController semanticController = Diagram.getDiagram(c).getType().getSemanticController();
        if( semanticController.canAccept(c, node) )
        {
            viewPane.add(node, location);
        }
        return new DiagramElementGroup( node );
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

    @PropertyName("Index")
    public String getIndex()
    {
        return index;
    }

    public void setIndex(String index)
    {
        this.index = index;
    }

    @PropertyName("From")
    public String getFrom()
    {
        return from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    @PropertyName("Up to")
    public String getUpTo()
    {
        return upTo;
    }

    public void setUpTo(String upTo)
    {
        this.upTo = upTo;
    }

}
