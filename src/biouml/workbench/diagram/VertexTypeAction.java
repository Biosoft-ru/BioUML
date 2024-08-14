package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewPane;

import biouml.model.Edge;

abstract public class VertexTypeAction extends AbstractAction
{
    public static final String KEY = "Vertex type";

    public static final String EDGE = "edge";
    public static final String POINT = "point";
    public static final String TYPE = "type";
    public static final String VIEWPANE = "viewPane";

    public VertexTypeAction()
    {
        this(true);
    }

    public VertexTypeAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    protected Edge edge;
    protected Integer point;
    protected Integer type;
    protected ViewPane viewPane;

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        edge = (Edge)getValue(EDGE);
        point = (Integer)getValue(POINT);
        type = (Integer)getValue(TYPE);
        viewPane = (ViewPane)getValue(VIEWPANE);
    }

    protected void setVertexType(int type)
    {
        Path path = edge.getPath();
        path.pointTypes[point] = type;

        DiagramDocument.updateDiagram(viewPane, edge);
    }
}
