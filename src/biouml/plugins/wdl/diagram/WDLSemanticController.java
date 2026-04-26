package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

/**
 * Semantic controller for workflow diagrams
 */
public class WDLSemanticController extends DefaultSemanticController
{
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        try
        {
            if( type.equals( WDLConstants.LINK_TYPE ) )
            {
                new CreateEdgeAction().createEdge( pt, viewEditor, new LinkCreator() );
                return null;
            }
            else
            {
                Object properties = getPropertiesByType( parent, type, pt );
                if( properties instanceof InitialElementProperties )
                {
                    PropertiesDialog dialog = new PropertiesDialog( Application.getApplicationFrame(), "New element", properties );
                    if( dialog.doModal() )
                        return ( (InitialElementProperties)properties ).createElements( parent, pt, viewEditor );
                    return DiagramElementGroup.EMPTY_EG;
                }
            }
        }
        catch( Throwable t )
        {
            throw ExceptionRegistry.translateException( t );
        }
        return DiagramElementGroup.EMPTY_EG;
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type instanceof String )
        {
            switch( (String)type )
            {
                case WDLConstants.TASK_TYPE:
                    return new TaskProperties();
                case WDLConstants.CALL_TYPE:
                    return new CallProperties(Diagram.getDiagram( compartment ));
                case WDLConstants.EXPRESSION_TYPE:
                    return new ExpressionProperties();
                case WDLConstants.WORKFLOW_INPUT_TYPE:
                    return new WorkflowInputProperties();
                case WDLConstants.WORKFLOW_OUTPUT_TYPE:
                    return new WorkflowOutputProperties();
                case WDLConstants.CONDITION_TYPE:
                    return new ConditionProperties();
                case WDLConstants.CONDITIONAL_TYPE:
                    return new ConditionalProperties();
                case WDLConstants.SCATTER_TYPE:
                    return new CycleProperties();
                case WDLConstants.INPUT_TYPE:
                    return new InputProperties();
                case WDLConstants.OUTPUT_TYPE:
                    return new OutputProperties();
            }
        }
        return null;
    }

    public class LinkCreator implements EdgeCreator
    {
        @Override
        public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
        {
            Edge result = new Edge( new Stub( null, in.getName() + " interact " + out.getName(), WDLConstants.LINK_TYPE ), in, out );
            return result;
        }
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        Dimension result = super.move( de, newParent, offset, oldBounds );
        if( WDLConstants.INPUT_TYPE.equals( de.getKernel().getType() ) || WDLConstants.OUTPUT_TYPE.equals( de.getKernel().getType() )
                && WDLConstants.TASK_TYPE.equals( de.getCompartment().getKernel().getType() ) )
            movePort( (Node)de );

        return result;
    }

    public static void movePort(Node node)
    {
        boolean input = WDLConstants.INPUT_TYPE.equals( node.getKernel().getType() );
        Compartment parent = node.getCompartment();
        Point p = node.getLocation();
        if( input )
            p.x = parent.getLocation().x;
        else
            p.x = parent.getLocation().x + parent.getView().getBounds().width - node.getView().getBounds().width;
        node.setLocation( p );
    }

    public static String uniqName(Compartment parent, String name)
    {
        return DefaultSemanticController.generateUniqueName( parent, name );
    }

    @Override
    public Edge createEdge(Node fromNode, Node toNode, String edgeType, Compartment compartment)
    {
        String name = uniqName( compartment, Base.TYPE_DIRECTED_LINK );
        Stub edgeStub = new Stub( null, name, Base.TYPE_DIRECTED_LINK );
        return new Edge( edgeStub, fromNode, toNode );
    }
}