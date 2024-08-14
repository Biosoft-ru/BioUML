package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Edge;

@SuppressWarnings ( "serial" )
public class StraightenEdgeAction extends AbstractAction
{
    public static final String KEY = "Straighten edge";

    public static final String EDGE = "edge";
    public static final String VIEWPANE = "viewPane";

    public StraightenEdgeAction()
    {
        this( true );
    }

    public StraightenEdgeAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Edge edge = (Edge)getValue( EDGE );
        ViewEditorPane viewPane = (ViewEditorPane)getValue( VIEWPANE );
        Path newPath = new Path();
        newPath.addPoint( edge.getInPort().x, edge.getInPort().y );
        newPath.addPoint( edge.getOutPort().x, edge.getOutPort().y );

        viewPane.startTransaction( "Remove vertex" );
        edge.setPath( newPath );
        DiagramDocument.updateDiagram( viewPane, edge );
        viewPane.completeTransaction();
    }
}