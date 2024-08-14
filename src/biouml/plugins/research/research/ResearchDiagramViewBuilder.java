package biouml.plugins.research.research;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.net.URL;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.ArrowView.Tip;
import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Type;

public class ResearchDiagramViewBuilder extends DefaultDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new ResearchDiagramViewOptions(null);
    }

    @Override
    public Icon getIcon(Object type)
    {
        String imageFile = "resources/" + (String)type + ".gif";
        URL url = getIconURL(getClass(), imageFile);

        if( url != null )
            return new ImageIcon(url);
        log.log(Level.SEVERE,  "Image not found for type: " +  (String)type );
        return null;
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        ResearchDiagramViewOptions diagramViewOptions = (ResearchDiagramViewOptions)viewOptions;
        String type = node.getKernel().getType();

        if( Type.ANALYSIS_METHOD.equals(type) )
        {
            return createAnalysisCoreView(container, node, diagramViewOptions, g);
        }
        else if( Type.TYPE_DATA_ELEMENT.equals(type) )
        {
            return createDataElementCoreView(container, node, diagramViewOptions, g);
        }
        else if( Type.TYPE_EXPERIMENT.equals(type) )
        {
            return createExperimentCoreView(container, node, diagramViewOptions, g);
        }
        else if( Type.TYPE_SIMULATION_RESULT.equals(type) )
        {
            return createSimulationResultCoreView(container, node, diagramViewOptions, g);
        }
        else if( Type.ANALYSIS_TABLE.equals(type) )
        {
            return createAnalysisTableCoreView(container, node, diagramViewOptions, g);
        }
        else if( Type.DIAGRAM_INFO.equals(type) )
        {
            return createColorBoxCoreView(container, node, diagramViewOptions, g, diagramViewOptions.getDiagramBrush());
        }

        return super.createNodeCoreView(container, node, viewOptions, g);
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions viewOptions, Graphics g)
    {
        ResearchDiagramViewOptions diagramViewOptions = (ResearchDiagramViewOptions)viewOptions;
        String type = compartment.getKernel().getType();
        container.setModel(compartment);

        if( Type.ANALYSIS_SCRIPT.equals(type) )
        {
            return createScriptCoreView(container, compartment, diagramViewOptions, g);
        }
        else if( Type.ANALYSIS_QUERY.equals(type) )
        {
            return createSQLCoreView(container, compartment, diagramViewOptions, g);
        }

        return super.createCompartmentCoreView(container, compartment, viewOptions, g);
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        String type = edge.getKernel().getType();
        if( type.equals(Base.TYPE_DIRECTED_LINK) || type.equals(Base.TYPE_UNDIRECTED_LINK) )
        {
            CompositeView view = new CompositeView();
            Pen pen = viewOptions.getConnectionPen();
            Brush brush = viewOptions.getConnectionBrush();

            Path path = edge.getPath();

            Point in = new Point();
            Point out = new Point();
            if( !calculateInOut(edge, in, out) )
                return view;
            if( path == null || path.npoints < 2 )
            {
                path = new Path();
                path.addPoint(in.x, in.y);
                path.addPoint(out.x, out.y);

                edge.setPath(path);
            }
            else
            {
                path.xpoints[0] = in.x;
                path.ypoints[0] = in.y;
                path.xpoints[path.npoints - 1] = out.x;
                path.ypoints[path.npoints - 1] = out.y;
            }

            Tip tip = null;
            if( type.equals(Base.TYPE_DIRECTED_LINK) )
            {
                tip = ArrowView.createArrowTip(pen, brush, 6, 6, 4);
            }
            View arrow = new ArrowView(pen, brush, edge.getSimplePath(), null, tip);
            arrow.setModel(edge);
            arrow.setActive(true);
            view.add(arrow);

            view.setModel(edge);
            view.setActive(false);

            return view;
        }
        return super.createEdgeView(edge, viewOptions, g);
    }

    protected boolean createDataElementCoreView(CompositeView container, Node node, ResearchDiagramViewOptions diagramOptions, Graphics g)
    {
        return createColorBoxCoreView(container, node, diagramOptions, g, diagramOptions.getDeBrush());
    }

    protected boolean createExperimentCoreView(CompositeView container, Node node, ResearchDiagramViewOptions diagramOptions, Graphics g)
    {
        return createColorBoxCoreView(container, node, diagramOptions, g, diagramOptions.getExperimentBrush());
    }

    protected boolean createSimulationResultCoreView(CompositeView container, Node node, ResearchDiagramViewOptions diagramOptions,
            Graphics g)
    {
        return createColorBoxCoreView(container, node, diagramOptions, g, diagramOptions.getResultBrush());
    }

    protected boolean createAnalysisTableCoreView(CompositeView container, Node node, ResearchDiagramViewOptions diagramOptions,
            Graphics g)
    {
        return createColorBoxCoreView(container, node, diagramOptions, g, diagramOptions.getAnalysisTableBrush());
    }

    protected boolean createAnalysisCoreView(CompositeView container, Node node, ResearchDiagramViewOptions diagramOptions,
            Graphics g)
    {
        return createColorBoxCoreView(container, node, diagramOptions, g, diagramOptions.getAnalysisBrush());
    }

    protected boolean createColorBoxCoreView(CompositeView container, Node node, ResearchDiagramViewOptions diagramOptions,
            Graphics g, Brush brush)
    {
        View text = new TextView(node.getTitle(), diagramOptions.getDefaultFont(), g);
        int d = 3;
        Rectangle r = text.getBounds();

        BoxView view = new BoxView(diagramOptions.getNodePen(), brush, r.x - d, r.y - d, r.width + d * 2,
                r.height + d * 2);

        view.setModel(node);
        container.add(view);
        container.add(text, CompositeView.X_CC | CompositeView.Y_CC);
        view.setActive(true);
        return false;
    }

    protected boolean createScriptCoreView(CompositeView container, Compartment compartment, ResearchDiagramViewOptions diagramOptions,
            Graphics g)
    {
        View text = new TextView("Script", diagramOptions.getDefaultFont(), g);
        int d = 3;
        Rectangle r = text.getBounds();
        BoxView view = new BoxView(diagramOptions.getNodePen(), diagramOptions.getSqlBrush(), r.x - d, r.y - d, r.width + d * 2,
                r.height + d * 2);
        view.setLocation(compartment.getLocation());
        view.setModel(compartment);
        view.setActive(true);
        container.add(view);
        container.add(text, CompositeView.X_CC | CompositeView.Y_CC);

        return false;
    }

    protected boolean createSQLCoreView(CompositeView container, Compartment compartment, ResearchDiagramViewOptions diagramOptions,
            Graphics g)
    {
        View text = new TextView("SQL query", diagramOptions.getDefaultFont(), g);
        int d = 3;
        Rectangle r = text.getBounds();
        BoxView view = new BoxView(diagramOptions.getNodePen(), diagramOptions.getSqlBrush(), r.x - d, r.y - d, r.width + d * 2,
                r.height + d * 2);
        view.setLocation(compartment.getLocation());
        view.setModel(compartment);
        view.setActive(true);
        container.add(view);
        container.add(text, CompositeView.X_CC | CompositeView.Y_CC);

        return false;
    }
}
