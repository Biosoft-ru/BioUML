package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Edge;

@SuppressWarnings ( "serial" )
public class RemoveVertexAction extends AbstractAction
{
    public static final String KEY = "Remove vertex";

    public static final String EDGE = "edge";
    public static final String POINT = "point";
    public static final String VIEWPANE = "viewPane";

    public RemoveVertexAction()
    {
        this( true );
    }

    public RemoveVertexAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Edge edge = (Edge)getValue( EDGE );
        Integer point = (Integer)getValue( POINT );
        ViewEditorPane viewPane = (ViewEditorPane)getValue( VIEWPANE );

        Path oldPath = edge.getPath();
        if( oldPath != null && oldPath.npoints > point )
        {
            Path newPath = new Path();
            for( int i = 0; i < point; i++ )
            {
                newPath.addPoint( oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i] );
            }
            for( int i = point + 1; i < oldPath.npoints; i++ )
            {
                newPath.addPoint( oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i] );
            }

            viewPane.startTransaction( "Remove vertex" );
            edge.setPath( newPath );
            DiagramDocument.updateDiagram( viewPane, edge );
            viewPane.completeTransaction();
        }
    }
}
