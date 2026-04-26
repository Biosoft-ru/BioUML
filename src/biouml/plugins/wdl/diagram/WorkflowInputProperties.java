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
@PropertyName ( "Workflow input properties" )
@PropertyDescription ( "Workflow input properties." )
public class WorkflowInputProperties extends ExpressionProperties
{

    public WorkflowInputProperties()
    {
        this.setName( "input_1" );
    }

    @Override
    public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( name.isEmpty() )
            throw new Exception( "Empty workflow input name!" );
        Diagram diagram = Diagram.getDiagram( parent );

        String name = WDLSemanticController.uniqName( parent, this.name );
        Node node = new Node( parent, name, new Stub( null, name, WDLConstants.WORKFLOW_INPUT_TYPE ) );
        node.setNotificationEnabled( false );

        WorkflowUtil.setName( node, this.getVariable() );
        WorkflowUtil.setType( node, this.getType() );
        WorkflowUtil.setExpression( node, this.getRhs() );

        int position = WorkflowUtil.getExternalParameters( diagram ).size();
        WorkflowUtil.setPosition( node, position );

        //        node.setShapeSize( new Dimension( 200, 50 ) );

        node.setNotificationEnabled( true );

        SemanticController controller = diagram.getType().getSemanticController();
        if( !controller.canAccept( parent, node ) )
            return new DiagramElementGroup();

        return new DiagramElementGroup( node );
    }
}