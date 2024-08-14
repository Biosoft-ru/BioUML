package biouml.workbench.diagram;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.PathUtils;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Edge;

public class AddVertexAction extends AbstractAction
{
    public static final String KEY = "Add vertex";

    public static final String EDGE = "edge";
    public static final String POINT = "point";
    public static final String VIEWPANE = "viewPane";

    public AddVertexAction()
    {
        this(true);
    }

    public AddVertexAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Edge edge = (Edge)getValue(EDGE);
        Point point = (Point)getValue(POINT);
        ViewEditorPane viewPane = (ViewEditorPane)getValue(VIEWPANE);

        SimplePath oldPath = edge.getSimplePath();
        if( oldPath != null )
        {
            int pos = PathUtils.getNearestSegment(oldPath, point);

            Path newPath = new Path();

            for( int i = 0; i <= pos; i++ )
            {
                newPath.addPoint(oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i]);
            }
            newPath.addPoint(point.x, point.y);
            for( int i = pos + 1; i < oldPath.npoints; i++ )
            {
                newPath.addPoint(oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i]);
            }

            viewPane.startTransaction("Add vertex");
            edge.setPath(newPath);
            DiagramDocument.updateDiagram(viewPane, edge);
            viewPane.completeTransaction();
        }
    }
}