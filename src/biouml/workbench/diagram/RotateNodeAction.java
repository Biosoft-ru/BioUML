package biouml.workbench.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram.PortOrientation;
import biouml.standard.type.Stub.ConnectionPort;

@SuppressWarnings ( "serial" )
public class RotateNodeAction extends AbstractAction
{
    public static final String KEY = "Rotate clockwise";

    public static final String NODE = "node";
    public static final String VIEWPANE = "viewPane";

    public RotateNodeAction()
    {
        this(true);
    }

    public RotateNodeAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Node node = (Node)getValue(NODE);
        ViewEditorPane viewPane = (ViewEditorPane)getValue(VIEWPANE);
        DynamicProperty dp = node.getAttributes().getProperty(ConnectionPort.PORT_ORIENTATION);
        if( dp == null )
            return;

        Object value = dp.getValue();

        if( orientationClockwise.containsKey(value) )
            dp.setValue(orientationClockwise.get(value));
        else if( value instanceof PortOrientation )
            dp.setValue(((PortOrientation)value).clockwise());

        Dimension shapeSize = node.getShapeSize();
        int w = shapeSize != null ? shapeSize.width : 0;
        int h = shapeSize != null ? shapeSize.height : 0;
        int x = node.getLocation().x + (w-h)/2;
        int y = node.getLocation().y + (h-w)/2;
        node.setLocation(new Point(x,y));
        node.setShapeSize(new Dimension(h, w));
        
        SemanticController controller = Diagram.getDiagram(node).getType().getSemanticController();

        viewPane.startTransaction("Rotate node");
        node.edges().forEach( controller::recalculateEdgePath );

        DiagramDocument.updateDiagram(viewPane, node);
        viewPane.completeTransaction();
    }
    
    private static final Map<String, String> orientationClockwise = new HashMap<String, String>()
    {
        {
            put("right", "bottom");
            put("bottom", "left");
            put("left", "top");
            put("top", "right");
        }
    };
}