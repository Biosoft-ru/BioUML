package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;

import ru.biosoft.graphics.editor.ViewEditorPane;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.standard.type.Stub;
import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
@PropertyName ( "Conditional block properties" )
@PropertyDescription ( "Conditional block properties." )
public class ConditionalProperties extends Option implements InitialElementProperties
{
    protected String name = "conditional_1";

    public ConditionalProperties()
    {

    }

    @PropertyName ( "Name" )
    @PropertyDescription ( "Name" )
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        this.firePropertyChange( "name", oldValue, name );
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty conditional block name!" );

        String name = WDLSemanticController.uniqName( parent, this.name );
        Compartment compartment = new Compartment( parent, name, new Stub( null, name, WDLConstants.CONDITIONAL_TYPE ) );
        Node node = new Node( parent, "port", new Stub( null, "port",  WDLConstants.CONDITIONAL_PORT_TYPE ) );
        compartment.put( node );

        compartment.setShapeSize( new Dimension( 350, 150 ) );
        Diagram diagram = Diagram.getDiagram(parent);
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept(parent, compartment) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( compartment );
    }
}