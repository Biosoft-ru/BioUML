package biouml.plugins.wdl.diagram;

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
import biouml.plugins.wdl.WorkflowUtil;
import biouml.standard.type.Stub;
import com.developmentontheedge.beans.Option;

@SuppressWarnings ( "serial" )
@PropertyName ( "Condition properties" )
@PropertyDescription ( "Condition properties." )
public class ConditionProperties extends Option implements InitialElementProperties
{
    protected String name = "condition_1";
    private String expression = "true";

    public ConditionProperties()
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
        this.name = name;
    }

    @PropertyName ( "Expression" )
    @PropertyDescription ( "Expression" )
    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty condition name!" );

        String name = WDLSemanticController.uniqName( parent, this.name );
        Node node = new Node( parent, name, new Stub( null, name, WDLConstants.CONDITION_TYPE ) );
        node.setNotificationEnabled( false );

        WorkflowUtil.setExpression( node, this.expression );
//        node.setShapeSize( new Dimension( 200, 50 ) );

        node.setNotificationEnabled( true );
        Diagram diagram = Diagram.getDiagram(parent);
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept(parent, node) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( node );
    }
}