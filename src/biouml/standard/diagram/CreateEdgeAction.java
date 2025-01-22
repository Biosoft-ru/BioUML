package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.graphics.editor.ViewPaneAdapter;
import ru.biosoft.graphics.editor.ViewPaneEvent;
import ru.biosoft.graphics.editor.ViewPaneLayer;

public class CreateEdgeAction
{
    public static final String EDGE_END_STUB = "edge-end-stub";

    public interface EdgeCreator
    {
        public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary) throws IllegalArgumentException;
    }
    
    private CompositeView layerView;
    private BoxView startBox, endBox;
    private Pen startEndPen = new Pen(3, Color.GREEN);
    private Pen errorPen = new Pen(3, Color.RED);
    
    public void createEdge(Point point, final ViewEditorPane viewEditor, final EdgeCreator creator)
    {
        View startView = viewEditor.getView().getDeepestActive(point);
        if(startView == null)
            return;
        Object model = startView.getModel();
        if(!(model instanceof Node))
            return;
        final Node startNode = (Node)model;
        final Diagram diagram = Diagram.getDiagram(startNode);
        startBox = new BoxView(startEndPen, null, startNode.getView().getBounds());
        final SemanticController controller = diagram.getType().getSemanticController();
        final DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        final ViewPaneLayer layer = new ViewPaneLayer()
        {
            @Override
            public void paintLayer(Graphics2D g2)
            {
                startBox.paint(g2);
                if(endBox != null)
                {
                    endBox.paint(g2);
                }
                layerView.paint(g2);
            }
        };
        
        viewEditor.addLayer(layer);
        
        viewEditor.addViewPaneListener(new ViewPaneAdapter()
        {
            @Override
            public void mousePressed(ViewPaneEvent e)
            {
                View view = e.getViewSource();
                Object model = view == null ? null : view.getModel();
                if( model instanceof Node /*&& model != diagram*/ )
                {
                    Node endNode = (Node)model;
                    try
                    {
                        Edge edge = creator.createEdge(startNode, endNode, false);
                        if(!controller.canAccept(edge.getCompartment(), edge))
                        {
                            throw new IllegalArgumentException("Edge cannot be accepted");
                        } else
                        {
                            viewEditor.add(edge, new Point(0, 0));
                        }
                    }
                    catch( IllegalArgumentException ex )
                    {
                        ApplicationUtils.errorBox(ex);
                    }
                }
                viewEditor.removeLayer(layer);
                viewEditor.removeViewPaneListener(this);
                viewEditor.repaint();
            }

            @Override
            public void mouseMoved(ViewPaneEvent e)
            {
                View view = e.getViewSource();
                Object model = view == null ? null : view.getModel();
                Node endNode;
                if( model instanceof Node && model != diagram )
                {
                    endNode = (Node)model;
                    endBox = new BoxView(startEndPen, null, endNode.getView().getBounds());
                }
                else
                {
                    endNode = new Node(diagram, new Stub(null, EDGE_END_STUB, EDGE_END_STUB));
                    endNode.setLocation(e.getPoint());
                    endNode.setShapeSize(new Dimension(1, 1));
                    endBox = null;
                }
                Edge edge = creator.createEdge(startNode, endNode, true);
                if(endBox != null && !controller.canAccept(edge.getCompartment(), edge))
                {
                    endBox.setPen(errorPen);
                }
                controller.recalculateEdgePath(edge);
                layerView = viewBuilder.createEdgeView(edge, diagram.getViewOptions(), ApplicationUtils.getGraphics());
                viewEditor.repaint();
            }
            
        });
    }
}
