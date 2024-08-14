package biouml.workbench.diagram;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Diagram;
import biouml.model.Edge;

public class SnapEdgeAction extends AbstractAction
{
    public static final String KEY = "Snap edge";

    public static final String EDGE = "edge";
    public static final String VIEWPANE = "viewPane";

    public SnapEdgeAction()
    {
        this( true );
    }

    public SnapEdgeAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Edge edge = (Edge)getValue( EDGE );
        ViewEditorPane viewPane = (ViewEditorPane)getValue( VIEWPANE );
        Diagram diagram = Diagram.getDiagram( edge );
        int snapSize = diagram.getViewOptions().getGridOptions().getCellSize();
        Path oldPath = edge.getPath();
        Path newPath = new Path();
        newPath.npoints = oldPath.npoints;
        newPath.xpoints = oldPath.xpoints.clone();
        newPath.ypoints = oldPath.ypoints.clone();
        newPath.pointTypes = oldPath.pointTypes.clone();
        for(int i=1; i<newPath.npoints-1; i++)
        {
            newPath.xpoints[i] = snap( newPath.xpoints[i], snapSize ); 
            newPath.ypoints[i] = snap( newPath.ypoints[i], snapSize ); 
        }
        Point in = new Point(), out = new Point();

        viewPane.startTransaction( "Snap edge" );
        edge.setPath( newPath );
        diagram.getType().getDiagramViewBuilder().calculateInOut( edge, in, out );
        newPath.xpoints[0] = in.x;
        newPath.ypoints[0] = in.y;
        newPath.xpoints[newPath.npoints-1] = out.x; 
        newPath.ypoints[newPath.npoints-1] = out.y; 
        edge.setPath( newPath );
        
        DiagramDocument.updateDiagram( viewPane, edge );
        viewPane.completeTransaction();
    }

    private int snap(int prev, int snapSize)
    {
        return (prev+snapSize/2*(prev>0?1:-1))/snapSize*snapSize;
    }
}