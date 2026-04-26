package biouml.plugins.wdl.diagram;

import java.awt.Point;
import ru.biosoft.graphics.editor.ViewEditorPane;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.standard.type.Stub;

@SuppressWarnings ( "serial" )
@PropertyName ( "Input properties" )
@PropertyDescription ( "Task input properties." )
public class InputProperties extends ExpressionProperties
{
    public InputProperties()
    {

    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty task input name!" );

        String name = WDLSemanticController.uniqName( parent, this.name );
        Node node = new Node( parent, name, new Stub( null, name, WDLConstants.INPUT_TYPE ) );
        node.setNotificationEnabled( false );

        WorkflowUtil.setName( node, getVariable() );
        WorkflowUtil.setType( node, getType() );
        WorkflowUtil.setExpression( node, getRhs() );
//        node.setShapeSize( new Dimension( 200, 50 ) );

        int position = WorkflowUtil.getInputs( parent ).size();
        WorkflowUtil.setPosition( node, position );
        
        node.setNotificationEnabled( true );
        Diagram diagram = Diagram.getDiagram(parent);
        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept(parent, node) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( node );
    }
}