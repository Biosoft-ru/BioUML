package biouml.plugins.pharm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.net.URL;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.math.view.FormulaViewBuilder;
import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram.PortOrientation;
import biouml.standard.diagram.MathDiagramViewBuilder;
import biouml.standard.diagram.PathwaySimulationDiagramViewBuilder;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;

/**
 *
 * @author Ilya
 *
 */
public class PopulationModelDiagramViewBuilder extends DefaultDiagramViewBuilder
{
    public static final int PORT_SIZE = 20;

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new PopulationModelDiagramViewOptions(null);
    }

    @Override
    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g)
    {
        CompositeView diagramView = super.createDiagramView(diagram, g);

        diagramView.updateBounds();

        return diagramView;
    }

    @Override
    public Icon getIcon(Object type)
    {
        if( type instanceof String )
        {
            String imageFile = "resources/" + (String)type + ".gif";
            URL url = getIconURL( getClass(), imageFile );

            if( url != null )
                return new ImageIcon( url );

            log.log(Level.SEVERE,  "Image not found for type: " + (String)type );
        }
        else if( type instanceof Class )
        {
            return super.getIcon( type );
        }
        return null;
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        PopulationModelDiagramViewOptions options = (PopulationModelDiagramViewOptions)viewOptions;

        Base kernel = node.getKernel();
        String type = kernel.getType();

        if( Type.TYPE_VARIABLE.equals( type ) )
        {
           return createVariableCoreView( container, node, options, g );
        }
        else if( Type.TYPE_TABLE_DATA.equals( type ) )
        {
            return new MathDiagramViewBuilder().createTableEntityCoreView( container, node, options, g );
        }
        else if( Type.TYPE_PORT.equals( type ) )
        {
            return createPort( container, node, options, g );
        }
        return super.createNodeCoreView(container, node, options, g);
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions viewOptions, Graphics g)
    {
        PopulationModelDiagramViewOptions options = (PopulationModelDiagramViewOptions)viewOptions;
        Base kernel = compartment.getKernel();
        String type = kernel.getType();
        if( Type.TYPE_ARRAY.equals( type ) )
        {
           return this.createArrayCoreView( container, compartment, options, g );
        }
        else if (compartment instanceof StructuralModel)
        {
            return createStructuralModelView(container, (StructuralModel)compartment, options, g);
        }
        return super.createCompartmentCoreView(container, compartment, options, g);
    }

    protected boolean createVariableCoreView(CompositeView container, Node node, PopulationModelDiagramViewOptions options, Graphics g)
    {
        PopulationVariable variable = node.getRole( PopulationVariable.class );
        switch(variable.getType())
        {
            case Type.TYPE_STOCHASTIC:
                return createStochasticCoreView(container, node, options, g);
            case Type.TYPE_FUNCTION:
                return createFunctionCoreView(container, node, options, g);
            case Type.TYPE_CONSTANT:
                return createConstantCoreView(container, node, options, g);
            default:
                return createConstantCoreView( container, node, options, g );
        }
    }

    protected boolean createConstantCoreView(CompositeView container, Node node, PopulationModelDiagramViewOptions options, Graphics g)
    {
        container.add(new BoxView(options.getDefaultPen(), options.constantBrush,  0, 0, 80, 40 ));
        container.add( new TextView( node.getTitle(), options.getNodeTitleFont(), g ), CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    protected boolean createStochasticCoreView(CompositeView container, Node node, PopulationModelDiagramViewOptions options, Graphics g)
    {
        container.add(new EllipseView(options.getDefaultPen(), options.stochasticBrush,  0, 0, 80, 40 ));
        container.add( new TextView( node.getTitle(), options.getNodeTitleFont(), g ), CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    protected boolean createFunctionCoreView(CompositeView container, Node node, PopulationModelDiagramViewOptions options, Graphics g)
    {
        container.add(new EllipseView(options.getDefaultPen(), options.fucntionBrush,  0, 0, 80, 40 ));
        container.add( new TextView( node.getTitle(), options.getNodeTitleFont(), g ), CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    protected boolean createArrayCoreView(CompositeView container, Compartment compartment, PopulationModelDiagramViewOptions options, Graphics g)
    {
        Dimension size = compartment.getShapeSize();

        if( size == null )
            size = new Dimension( 200, 200 );

        int width = size.width;
        int height = size.height;
        container.add(new BoxView(options.getDefaultPen(), new Brush(Color.white),  0, 0, width, height ));
        container.add(new BoxView(options.getDefaultPen(), new Brush(Color.white),  0, 0, width - 3, height - 3 ));
        container.add(new BoxView(options.getDefaultPen(), new Brush(Color.white),  0, 0, width - 7, height - 7 ));
        container.add(new BoxView(options.getDefaultPen(), new Brush(Color.white),  0, 0, width - 10, height - 10 ));
        TextView textView = new TextView( getArrayLabel( compartment ), options.getNodeTitleFont(), g );
        container.add( textView, CompositeView.X_LL | CompositeView.Y_BB, new Point(10,10) );

        container.setModel( compartment );
        container.setActive( true );
        container.setLocation( compartment.getLocation() );
        compartment.setView( container );
        return false;
    }

    public static String getArrayLabel(Compartment compartment)
    {
        StringBuilder result = new StringBuilder("for(");
        result.append(compartment.getAttributes().getValueAsString( "index" ));
        result.append(" IN ");
        result.append(compartment.getAttributes().getValueAsString( "from" ));
        result.append(" : ");
        result.append(compartment.getAttributes().getValueAsString( "up to" ));
        result.append(")");

        return result.toString();
    }

    protected boolean createStructuralModelView(CompositeView container, StructuralModel compartment, PopulationModelDiagramViewOptions options, Graphics g)
    {
        String title = compartment.getTitle();
        if( title == null || title.equals( "" ) )
            title = compartment.getName();
        ComplexTextView titleView = new ComplexTextView( title, options.getCompartmentTitleFont(), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );


        Dimension size = compartment.getShapeSize();

        if( size == null )
            size = new Dimension( 250, 250 );

        BoxView boxView = new BoxView( options.getDefaultPen(), options.modelBrush, 0, 0, size.width, size.height );

        container.add( boxView );
        container.add( titleView, CompositeView.X_CC | CompositeView.Y_CC );

        Diagram diagram = compartment.getDiagram();
        ComplexTextView expView = null;
        if( diagram.getCurrentState() != null )
        {
            expView = new ComplexTextView( diagram.getCurrentStateName(), options.stateTitleFont, options.getFontRegistry(),
                    ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
            Point titleLocation = titleView.getLocation();
            int newY = titleLocation.y - ( expView.getBounds().height );
            expView.setLocation( expView.getLocation().x, newY + expView.getBounds().height );
            titleView.setLocation( titleLocation.x, newY );

            container.add( expView, CompositeView.X_CC | CompositeView.Y_CC );
        }

        container.setModel( compartment );
        container.setActive( true );

        container.setLocation( compartment.getLocation() );
        compartment.setView( container );

        return false;
    }

    protected boolean createPort (CompositeView container, Node node, PopulationModelDiagramViewOptions options, Graphics g)
    {
        String type = node.getAttributes().getValueAsString( "type" );
       if (type == null)
           return false;

       PortOrientation orientation = Util.getPortOrientation( node );

        View title =  new FormulaViewBuilder().createTitleView( node.getTitle(), options.getNodeTitleFont(), g );

        if( type.equals( ParameterProperties.OBSERVED_TYPE ) )
        {
            Polygon polygon = PathwaySimulationDiagramViewBuilder.createPolygon( orientation, true, PORT_SIZE );
            container.add( new PolygonView( options.getDefaultPen(), options.observedBrush, polygon ) );
        }
        else if( type.equals( ParameterProperties.PARAMETER_TYPE ) )
        {
            Polygon polygon = PathwaySimulationDiagramViewBuilder.createPolygon( orientation, false, PORT_SIZE );
            container.add( new PolygonView( options.getDefaultPen(), options.parameterBrush, polygon ) );
        }
        else
        {
            container.add( new EllipseView( options.getDefaultPen(), options.doseBrush, 0, 0, 30, 30 ) );
        }

        int x = options.getNodeTitleMargin().x;
        int y = options.getNodeTitleMargin().y;

        switch( orientation )
        {
            case TOP:
                container.add( title, CompositeView.X_CC | CompositeView.Y_BT, new Point( x, y ) );
                break;
            case RIGHT:
                container.add( title, CompositeView.X_LR | CompositeView.Y_CC, new Point( y, x ) );
                break;
            case BOTTOM:
                container.add( title, CompositeView.X_CC | CompositeView.Y_TB, new Point( x, y ) );
                break;
            case LEFT:
                container.add( title, CompositeView.X_RL | CompositeView.Y_CC, new Point( y, x ) );
                break;
            default:
                break;
        }
        return false;
    }

    protected boolean createParameterCoreView(CompositeView container, Node node, PopulationModelDiagramViewOptions options, Graphics g)
    {
        container.add(new EllipseView(options.getDefaultPen(), options.constantBrush,  0, 0, 40, 40 ));
        container.add( new TextView( node.getTitle(), options.getNodeTitleFont(), g ), CompositeView.X_CC | CompositeView.Y_CC );
        return true;
    }


    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = new CompositeView();

        if( edge.getPath() == null )
            Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );

        View arrow = new ArrowView( options.getDefaultPen(), new Brush(Color.black), edge.getSimplePath(), 0, 1 );

        view.add( arrow );
        view.setModel( edge );
        view.setActive( false );

        return view;
    }
}
