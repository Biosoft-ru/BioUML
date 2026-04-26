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
@PropertyName ( "Expression properties" )
@PropertyDescription ( "Expression properties." )
public class ExpressionProperties extends Option implements InitialElementProperties
{
    protected String name = "expression_1";
    private String variable = "";
    private String type = "";
    private String rhs = "";

    public ExpressionProperties()
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

    @PropertyName ( "Variable" )
    @PropertyDescription ( "Variable" )
    public String getVariable()
    {
        return variable;
    }

    public void setVariable(String variable)
    {
        this.variable = variable;
    }

    @PropertyName ( "Right Hand Side" )
    @PropertyDescription ( "Right Hand Side" )
    public String getRhs()
    {
        return rhs;
    }

    public void setRhs(String rhs)
    {
        this.rhs = rhs;
    }

    @PropertyName ( "Type" )
    @PropertyDescription ( "Type" )
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty task name!" );

        String name = WDLSemanticController.uniqName( parent, this.name );
        Node node = new Node( parent, name, new Stub( null, name, WDLConstants.EXPRESSION_TYPE ) );
        node.setNotificationEnabled( false );

        WorkflowUtil.setName( node, this.variable );
        WorkflowUtil.setType( node, this.type );
        WorkflowUtil.setExpression( node, this.rhs );
//        node.setShapeSize( new Dimension( 200, 50 ) );

        node.setNotificationEnabled( true );
        Diagram diagram = Diagram.getDiagram(parent);
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept(parent, node) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( node );
    }
}