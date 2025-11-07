package biouml.plugins.wdl.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;

import javax.annotation.Nonnull;
import javax.swing.ImageIcon;

import org.apache.commons.text.StringEscapeUtils;

import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.standard.type.Type;
import biouml.workbench.graph.InOutFinder;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;

public class WDLViewBuilder extends DefaultDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new WDLViewOptions( null );
    }

    @Override
    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g)
    {
        return super.createDiagramView( diagram, g );
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        WDLViewOptions diagramViewOptions = (WDLViewOptions)viewOptions;
        String type = node.getKernel().getType();

        if( WDLConstants.INPUT_TYPE.equals( type ) || WDLConstants.OUTPUT_TYPE.equals( type ) )
        {
            return createInOutCoreView( container, node, diagramViewOptions, g, true );
        }
        else if( WDLConstants.EXTERNAL_PARAMETER_TYPE.equals( type ) )
        {
            return createExternalParameter( container, node, diagramViewOptions, g );
        }
        else if( WDLConstants.WORKFLOW_OUTPUT_TYPE.equals( type ) )
        {
            return createWorkflowOutput( container, node, diagramViewOptions, g );
        }
        else if( WDLConstants.EXPRESSION_TYPE.equals( type ) )
        {
            return createExpressionCoreView( container, node, diagramViewOptions, g );
        }
        else if( WDLConstants.CONDITION_TYPE.equals( type ) )
        {
            return createConditionCoreView( container, node, diagramViewOptions, g );
        }
        else if( WDLConstants.SCATTER_VARIABLE_TYPE.equals( type ) )
        {
            return createCycleVariableCoreView( container, node, diagramViewOptions, g );
        }
        else if( WDLConstants.STRUCT_TYPE.equals( type ) )
        {
            return createStructView( container, node, diagramViewOptions, g );
        }
        return super.createNodeCoreView( container, node, viewOptions, g );
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions viewOptions, Graphics g)
    {
        WDLViewOptions diagramViewOptions = (WDLViewOptions)viewOptions;
        String type = compartment.getKernel().getType();
        container.setModel( compartment );

        if( WDLConstants.TASK_TYPE.equals( type ) )
        {
            return createTaskView( container, compartment, diagramViewOptions, g );
        }
        else if( WDLConstants.CALL_TYPE.equals( type ) )
        {
            return createCommandCallView( container, compartment, diagramViewOptions, g );
        }
        else if( WDLConstants.SCATTER_TYPE.equals( type ) )
        {
            return createScatterView( container, compartment, diagramViewOptions, g );
        }
        else if( WDLConstants.CONDITIONAL_TYPE.equals( type ) )
        {
            return createConditionalBlockView( container, compartment, diagramViewOptions, g );
        }


        return super.createCompartmentCoreView( container, compartment, viewOptions, g );
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        String type = edge.getKernel().getType();
        if(WDLConstants.LINK_TYPE.equals( type ) )
        {
            CompositeView view = new CompositeView();
            Pen pen = viewOptions.getConnectionPen();
            Brush brush = viewOptions.getConnectionBrush();

            if( edge.getPath() == null )
            {
                Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );
            }

            SimplePath path = edge.getSimplePath();
            View arrow = new ArrowView( pen, brush, path, null, ArrowView.createArrowTip( pen, brush, 6, 6, 4 ) );
            arrow.setModel( edge );
            arrow.setActive( true );
            view.add( arrow );

            view.setModel( edge );
            view.setActive( false );

            return view;
        }
        return super.createEdgeView( edge, viewOptions, g );
    }

    protected boolean createInputView(CompositeView container, Node node, WDLViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( DataElementPath
                .create( (String)node.getAttributes().getValue( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY ) ).getName(),
                diagramOptions.getDefaultFont(), g );
        int d = 3;

        ImageView image = null;

        try
        {
            DataElementPath imgPath = ru.biosoft.access.core.DataElementPath
                    .create( (String)node.getAttributes().getValue( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY ) );
            image = new ImageView( IconFactory.getIcon( imgPath ).getImage(), d, 0 );
            image.setPath( IconFactory.getIconId( imgPath ) );
        }
        catch( Exception e )
        {
        }

        Rectangle r = text.getBounds();

        Brush nodeBrush = getBrush( node, diagramOptions.getDeBrush() );
        BoxView view = new BoxView( diagramOptions.getNodePen(), nodeBrush, r.x - d, r.y - d,
                r.width + d * 2 + ( image == null ? 0 : image.getBounds().width ), r.height + d * 2 );

        view.setModel( node );
        container.add( view );
        if( image != null )
            container.add( image, CompositeView.X_LL | CompositeView.Y_CC );
        container.add( text, CompositeView.X_RR | CompositeView.Y_CC );
        view.setActive( true );
        return false;
    }

    protected boolean createStructView(CompositeView container, Node node, WDLViewOptions viewOptions, Graphics g)
    {
        Dimension size = node.getShapeSize();

        View text = new ComplexTextView( StringEscapeUtils.escapeHtml4( node.getTitle() ), viewOptions.getDefaultFont(),
                viewOptions.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, 30, g );
        RectangularShape roundRect = new RoundRectangle2D.Float( 0, 0, size.width, size.height, 5, 5 );
        Brush nodeBrush = getBrush( node, viewOptions.getStructBrush() );
        BoxView view = new BoxView( viewOptions.getAnalysisPen(), nodeBrush, roundRect );
        view.setLocation( node.getLocation() );
        view.setModel( node );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        container.setActive( true );
        return false;
    }
    
    protected boolean createTaskView(CompositeView container, Compartment compartment, WDLViewOptions viewOptions, Graphics g)
    {
        Dimension size = compartment.getShapeSize();

        View text = new ComplexTextView( StringEscapeUtils.escapeHtml4( compartment.getTitle() ), viewOptions.getDefaultFont(),
                viewOptions.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, 30, g );
        RectangularShape roundRect = new RoundRectangle2D.Float( 0, 0, size.width, size.height, 5, 5 );
        Brush nodeBrush = getBrush( compartment, viewOptions.getTaskBrush() );
        BoxView view = new BoxView( viewOptions.getAnalysisPen(), nodeBrush, roundRect );
        view.setLocation( compartment.getLocation() );
        view.setModel( compartment );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        container.setActive( true );
        return false;
    }

    protected boolean createCommandCallView(CompositeView container, Compartment compartment, WDLViewOptions viewOptions, Graphics g)
    {
        Dimension size = compartment.getShapeSize();

        View text = new ComplexTextView( StringEscapeUtils.escapeHtml4( compartment.getTitle() ), viewOptions.getDefaultFont(),
                viewOptions.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, 30, g );
        RectangularShape roundRect = new RoundRectangle2D.Float( 0, 0, size.width, size.height, 5, 5 );
        Brush nodeBrush = getBrush( compartment, viewOptions.getAnalysisBrush() );
        BoxView view = new BoxView( viewOptions.getAnalysisPen(), nodeBrush, roundRect );
        view.setLocation( compartment.getLocation() );
        view.setModel( compartment );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        container.setActive( true );
        return false;
    }

    protected boolean createInOutCoreView(CompositeView container, Node node, WDLViewOptions diagramOptions, Graphics g, boolean isIn)
    {
        Rectangle r = new Rectangle( 0, 0, 16, 16 );

        String description = node.getAttributes().getValueAsString( "description" );
        if( description == null )
            description = node.getTitle();
        container.setDescription( description );

        String iconId = node.getAttributes().getValueAsString( "iconId" );

        //If icon id is incorrect, IconFactory.getIconById will return ImageIcon with size (-1,-1). Use default connector icon in this case.
        ImageIcon icon = IconFactory.getIconById( iconId );
        if( iconId != null && icon.getIconHeight() < 0 )
            iconId = null;

        if( iconId == null || iconId.isEmpty() )
            iconId = ClassLoading.getResourceLocation( getClass(), "resources/default-connector.gif" );
        
//        String iconName = null;
//        switch( WDLUtil.getType( node ) )
//        {
//            case "String":
//                iconName = "string.gif";
//                break;
//            case "File":
//                iconName = "file.gif";
//                break;
//            case "Array[File]":
//                iconName = "files.gif";
//                break;
//            case "Int":
//                iconName = "integer.gif";
//                break;
//        }
//        if( iconName != null )
//            iconId = ClassLoading.getResourceLocation( getClass(), "resources/" + iconName );
        ImageView img = new ImageView( IconFactory.getIconById( iconId ).getImage(), 0, 0 );
        img.setPath( iconId );
        container.add( img, CompositeView.X_LL | CompositeView.Y_TT );

        return false;
    }

    protected boolean createExpressionCoreView(CompositeView container, Node node, WDLViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( node.getName(), diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = getBrush( node, diagramOptions.getExpressionBrush() );
        PolygonView view = new PolygonView( diagramOptions.getExpressionPen(), nodeBrush,
                new int[] {0, -r.height / 2 - d, -d, r.width + d, r.width + r.height / 2 + d, r.width + d},
                new int[] {r.height + d, r.height / 2, -d, -d, r.height / 2, r.height + d} );
        view.setModel( node );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }
    
    protected boolean createConditionCoreView(CompositeView container, Node node, WDLViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( WorkflowUtil.getExpression( node ), diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = getBrush( node, diagramOptions.getConditionBrush() );
        PolygonView view = new PolygonView( diagramOptions.getConditionPen(), nodeBrush,
                new int[] {0, -r.height / 2 - d, -d, r.width + d, r.width + r.height / 2 + d, r.width + d},
                new int[] {r.height + d, r.height / 2, -d, -d, r.height / 2, r.height + d} );
        view.setModel( node );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    protected boolean createExternalParameter(CompositeView container, Node node, WDLViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( node.getName(), diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = getBrush( node, diagramOptions.getParameterBrush() );
        PolygonView view = new PolygonView( diagramOptions.getParameterPen(), nodeBrush,
                new int[] { -d, -d, r.width + d * 2, r.width + d * 2 + d * 3, r.width + d * 2},
                new int[] {r.height + d, -d, -d, r.height / 2, r.height + d} );
        view.setModel( node );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }
    
    protected boolean createWorkflowOutput(CompositeView container, Node node, WDLViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( node.getName(), diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = getBrush( node,  diagramOptions.getOutputBrush() );
        PolygonView view = new PolygonView( diagramOptions.getOutputPen(), nodeBrush,
                new int[] {r.width + d * 2, r.width + d * 2, -d, -4 * d, -d},
                new int[] { -d, r.height + d, r.height + d, r.height / 2, -d} );
        view.setModel( node );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }
    
    protected boolean createConditionalBlockView(CompositeView container, Compartment node, WDLViewOptions diagramOptions, Graphics g)
    {
        Brush shadowBrush = new Brush();
        Pen pen = getBorderPen(node, diagramOptions.getDefaultPen());
        Brush mainBrush = getBrush( node, diagramOptions.getConditionalBrush() );
        Point location = node.getLocation();
        Dimension size = node.getShapeSize();
        int width = size.width - 31;
        int height = size.height - 26;
        View shadowView = new BoxView( pen, shadowBrush, location.x + 1, location.y + 1, width, height );
        View boxView = new BoxView( pen, mainBrush, location.x, location.y, width, height );
        boxView.setModel( node );
        boxView.setActive( true );
        container.add( shadowView );
        container.add( boxView );
        return false;
    }
    
    protected boolean createScatterView(CompositeView container, Compartment node, WDLViewOptions diagramOptions, Graphics g)
    {
        Brush shadowBrush = new Brush();
        Pen pen = getBorderPen(node, diagramOptions.getDefaultPen());
        Brush mainBrush = getBrush( node, new Brush( Color.white ) );
        Point location = node.getLocation();
        Dimension size = node.getShapeSize();
        int width = size.width - 31;
        int height = size.height - 26;
        View shadowView = new BoxView( pen, shadowBrush, location.x + 1, location.y + 1, width, height );
        View boxView = new BoxView( pen, mainBrush, location.x, location.y, width, height );
        boxView.setModel( node );
        boxView.setActive( true );
        container.add( shadowView );
        container.add( boxView );
        SimplePath path = new SimplePath(
                new int[] {location.x + width, location.x + width + 15, location.x + width + 15, location.x - 15, location.x - 15,
                        location.x},
                new int[] {location.y + height * 2 / 3, location.y + height * 2 / 3, location.y + height + 25, location.y + height + 25,
                        location.y + height * 2 / 3, location.y + height * 2 / 3},
                6 );
        View arrow = new ArrowView( pen, null, path, null, ArrowView.createArrowTip( pen, shadowBrush, 8, 12, 5 ) );
        container.add( arrow );
        return false;
    }

    protected boolean createCycleVariableCoreView(CompositeView container, Node node, WDLViewOptions diagramOptions, Graphics g)
    {
        String title = node.getName();
        View text = new TextView( title, diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = getBrush( node, diagramOptions.getExpressionBrush() );
        PolygonView view = new PolygonView( diagramOptions.getExpressionPen(), nodeBrush,
                new int[] {0, -r.height / 2 - d, -d, r.width + d, r.width + r.height / 2 + d, r.width + d},
                new int[] {r.height + d, r.height / 2, -d, -d, r.height / 2, r.height + d} );
        view.setModel( node );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );

        node.setLocation( ( (Compartment)node.getOrigin() ).getLocation() );

        return false;
    }

    /**
     * Returns vertical position of the node
     * @param node
     * @param isIn
     * @return
     */
    protected double getPart(Node node, boolean isIn)
    {
        Compartment parent = ( (Compartment)node.getOrigin() );
        Object positionObj = node.getAttributes().getValue( "position" );

        // Count total number of input or output parameters
        int count = 0;
        for( DiagramElement de : parent )
        {
            if( ( de.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_IN ) && isIn )
                    || ( de.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_OUT ) && !isIn ) )
                count++;
        }

        int pos = 0;
        if( positionObj instanceof Number )
        {
            pos = ( (Number)positionObj ).intValue() + 1;
            return count == 0 ? 0.0 : ( pos - 0.5 ) / ( count );
        }
        // To support older workflows
        AnalysisMethod analysisMethod = AnalysisDPSUtils.getAnalysisMethodByNode( parent.getAttributes() );
        if( analysisMethod != null )
        {
            String[] names = isIn ? analysisMethod.getParameters().getInputNames() : analysisMethod.getParameters().getOutputNames();
            if( count == 0 )
                return 0.0;
            for( pos = 0; pos < names.length; pos++ )
            {
                if( names[pos].equals( node.getName().replace( ':', '/' ) ) )
                {
                    return ( pos + 0.5 ) / ( count );
                }
            }
        }
        pos = 0;
        for( DiagramElement de : parent )
        {
            if( ( de.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_IN ) && isIn )
                    || ( de.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_OUT ) && !isIn ) )
            {
                pos++;
                if( de == node )
                {
                    return count == 0 ? 0.0 : ( pos - 0.5 ) / ( count );
                }
            }
        }
        return 0.0;
    }

    protected Paint toBlackWhite(Paint paint)
    {
        double gamma = 2.5;
        if( paint instanceof Color )
        {
            Color color = (Color)paint;
            int c = (int) ( Math.pow( ( 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue() ) / 255, gamma )
                    * 255 );
            return new Color( c, c, c );
        }
        else if( paint instanceof GradientPaint )
        {
            GradientPaint gradient = (GradientPaint)paint;
            int c1 = (int) ( Math.pow( ( 0.299 * gradient.getColor1().getRed() + 0.587 * gradient.getColor1().getGreen()
                    + 0.114 * gradient.getColor1().getBlue() ) / 255, gamma ) * 255 );
            int c2 = (int) ( Math.pow( ( 0.299 * gradient.getColor2().getRed() + 0.587 * gradient.getColor2().getGreen()
                    + 0.114 * gradient.getColor2().getBlue() ) / 255, gamma ) * 255 );
            return new GradientPaint( gradient.getPoint1(), new Color( c1, c1, c1 ), gradient.getPoint2(), new Color( c2, c2, c2 ) );
        }
        return null;
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        String type = node.getKernel().getType();
        if( type.equals( Type.TYPE_DATA_ELEMENT_IN ) || type.equals( Type.ANALYSIS_PARAMETER )
                || type.equals( Type.TYPE_DATA_ELEMENT_OUT ) )
            return new InOutFinder( false, getNodeBounds( node ) );
        return super.getPortFinder( node );
    }
    
}
