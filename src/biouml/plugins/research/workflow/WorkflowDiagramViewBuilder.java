package biouml.plugins.research.workflow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.util.logging.Level;
import org.apache.commons.lang.StringEscapeUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.engine.SQLElement;
import biouml.plugins.research.workflow.engine.ScriptElement;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.standard.type.Base;
import biouml.standard.type.Type;
import biouml.workbench.graph.InOutFinder;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisParameters;
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
import ru.biosoft.util.TextUtil2;

public class WorkflowDiagramViewBuilder extends DefaultDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new WorkflowDiagramViewOptions( null );
    }

    @Override
    public Icon getIcon(Object type)
    {
        String imageFile = "resources/" + type + ".gif";
        URL url = getIconURL( getClass(), imageFile );

        if( url != null )
            return new ImageIcon( url );

        log.log(Level.SEVERE,  "Image not found for type: " + (String)type );

        return null;
    }

    @Override
    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g)
    {
        /*
        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(false);
        try
        {
            DiagramToGraphTransformer.layoutEdges(diagram);
        }
        finally
        {
            diagram.setNotificationEnabled(notificationEnabled);
        }
        */
        return super.createDiagramView(diagram, g);
    }


    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        WorkflowDiagramViewOptions diagramViewOptions = (WorkflowDiagramViewOptions)viewOptions;
        String type = node.getKernel().getType();

        if( Type.TYPE_DATA_ELEMENT.equals( type ) )
        {
            return createDataElementCoreView( container, node, diagramViewOptions, g );
        }
        else if( Type.TYPE_DATA_ELEMENT_IN.equals( type ) )
        {
            return createInOutCoreView( container, node, diagramViewOptions, g, true );
        }
        else if( Type.TYPE_DATA_ELEMENT_OUT.equals( type ) )
        {
            return createInOutCoreView( container, node, diagramViewOptions, g, false );
        }
        else if( Type.ANALYSIS_PARAMETER.equals( type ) )
        {
            return createPortCoreView( container, node, diagramViewOptions, g );
        }
        else if( Type.ANALYSIS_EXPRESSION.equals( type ) )
        {
            return createExpressionCoreView( container, node, diagramViewOptions, g );
        }
        else if( Type.ANALYSIS_CYCLE_VARIABLE.equals( type ) )
        {
            return createCycleVariableCoreView( container, node, diagramViewOptions, g );
        }
        return super.createNodeCoreView( container, node, viewOptions, g );
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions viewOptions, Graphics g)
    {
        WorkflowDiagramViewOptions diagramViewOptions = (WorkflowDiagramViewOptions)viewOptions;
        String type = compartment.getKernel().getType();
        container.setModel( compartment );

        if( Type.ANALYSIS_METHOD.equals( type ) )
        {
            return createAnalysisCoreView( container, compartment, diagramViewOptions, g );
        }
        else if( Type.ANALYSIS_SCRIPT.equals( type ) )
        {
            return createScriptCoreView( container, compartment, diagramViewOptions, g );
        }
        else if( Type.ANALYSIS_QUERY.equals( type ) )
        {
            return createSQLCoreView( container, compartment, diagramViewOptions, g );
        }
        else if( Type.ANALYSIS_CYCLE.equals( type ) )
        {
            return createCycleCoreView( container, compartment, diagramViewOptions, g );
        }

        return super.createCompartmentCoreView( container, compartment, viewOptions, g );
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        String type = edge.getKernel().getType();
        if( type.equals( Base.TYPE_DIRECTED_LINK ) || type.equals( Base.TYPE_LISTENER_LINK ) )
        {
            CompositeView view = new CompositeView();
            Pen pen = type.equals( Base.TYPE_DIRECTED_LINK ) ? viewOptions.getConnectionPen() : ( (WorkflowDiagramViewOptions)viewOptions )
                    .getListenerPen();
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

    protected boolean createDataElementCoreView(CompositeView container, Node node, WorkflowDiagramViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( DataElementPath.create( (String)node.getAttributes().getValue( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY ) )
                .getName(), diagramOptions.getDefaultFont(), g );
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

        Brush nodeBrush = fillNodeBrush( node, diagramOptions.getDeBrush() );
        BoxView view = new BoxView( diagramOptions.getNodePen(), nodeBrush, r.x - d, r.y - d, r.width + d * 2
                + ( image == null ? 0 : image.getBounds().width ), r.height + d * 2 );

        view.setModel( node );
        container.add( view );
        if( image != null )
            container.add( image, CompositeView.X_LL | CompositeView.Y_CC );
        container.add( text, CompositeView.X_RR | CompositeView.Y_CC );
        view.setActive( true );
        return false;
    }

    protected boolean createAnalysisCoreView(CompositeView container, Compartment compartment, WorkflowDiagramViewOptions diagramOptions,
            Graphics g)
    {
        Dimension size = compartment.getShapeSize();

        View text = new ComplexTextView( StringEscapeUtils.escapeHtml( compartment.getTitle() ), diagramOptions.getDefaultFont(),
                diagramOptions.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, 30, g );
        RectangularShape roundRect = new RoundRectangle2D.Float( 0, 0, size.width, size.height, 5, 5 );
        Brush nodeBrush = fillNodeBrush( compartment, diagramOptions.getAnalysisBrush() );
        BoxView view = new BoxView( diagramOptions.getAnalysisPen(), nodeBrush, roundRect );
        view.setLocation( compartment.getLocation() );
        view.setModel( compartment );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        View progress = getProgress( compartment, diagramOptions, g );
        if( progress != null )
            container.add( progress, CompositeView.X_CC | CompositeView.Y_CC );
        container.setActive( true );
        return false;
    }

    protected boolean createScriptCoreView(CompositeView container, Compartment compartment, WorkflowDiagramViewOptions diagramOptions,
            Graphics g)
    {
        String path = (String)compartment.getAttributes().getValue( ScriptElement.SCRIPT_PATH );
        String source = (String)compartment.getAttributes().getValue( ScriptElement.SCRIPT_SOURCE );
        String textStr = path == null ? source : "[" + ( DataElementPath.create( path ) ).getName() + "]";
        ComplexTextView text = new ComplexTextView( StringEscapeUtils.escapeHtml( textStr ), diagramOptions.getSmallFont(),
                diagramOptions.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_LEFT, 40, g );

        //try cut text so it will be not bigger than 300 px or resized script container height
        Dimension size = compartment.getShapeSize();
        int cutSize = Math.max( size.height, 300 );
        int delta = 30;
        if( text.getBounds().height > cutSize + delta )
        {
            String newStr = textStr.substring( 0, textStr.length() * cutSize / text.getBounds().height ) + " ...";
            text = new ComplexTextView( StringEscapeUtils.escapeHtml( newStr ), diagramOptions.getSmallFont(),
                    diagramOptions.getFontRegistry(),
                    ComplexTextView.TEXT_ALIGN_LEFT, 40, g );
        }

        if( text.getBounds().height > size.height )
        {
            size.height = text.getBounds().height + 5;
        }
        Brush nodeBrush = fillNodeBrush( compartment, diagramOptions.getScriptBrush() );
        RectangularShape roundRect = new RoundRectangle2D.Float( 0, 0, size.width, size.height, 5, 5 );
        BoxView view = new BoxView( diagramOptions.getScriptPen(), nodeBrush, roundRect );
        view.setLocation( compartment.getLocation() );
        view.setModel( compartment );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_TT );

        return false;
    }

    protected boolean createSQLCoreView(CompositeView container, Compartment compartment, WorkflowDiagramViewOptions diagramOptions,
            Graphics g)
    {
        String source = (String)compartment.getAttributes().getValue( SQLElement.SQL_SOURCE );
        ComplexTextView text = new ComplexTextView( StringEscapeUtils.escapeHtml( source ), diagramOptions.getSmallFont(),
                diagramOptions.getFontRegistry(), ComplexTextView.TEXT_ALIGN_LEFT, 30, g );

        Dimension size = compartment.getShapeSize();
        if( text.getBounds().height > size.height )
        {
            size.height = text.getBounds().height + 5;
        }
        Brush nodeBrush = fillNodeBrush( compartment, diagramOptions.getSqlBrush() );
        RectangularShape roundRect = new RoundRectangle2D.Float( 0, 0, size.width, size.height, 5, 5 );
        BoxView view = new BoxView( diagramOptions.getSqlPen(), nodeBrush, roundRect );
        view.setLocation( compartment.getLocation() );
        view.setModel( compartment );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_TT );

        return false;
    }

    protected boolean createInOutCoreView(CompositeView container, Node node, WorkflowDiagramViewOptions diagramOptions, Graphics g,
            boolean isIn)
    {
        Rectangle r = new Rectangle( 0, 0, 16, 16 );

        String description = node.getAttributes().getValueAsString( "description" );
        if(description == null)
            description = node.getTitle();
        container.setDescription( description );

        String iconId = node.getAttributes().getValueAsString( "iconId" );
        if( iconId == null || iconId.isEmpty() )
        {
            try //try to get icon from analysis parameters
            {
                AnalysisParameters parameters = AnalysisDPSUtils.readParametersFromAttributes( ( (Node)node.getParent() ).getAttributes() );
                ComponentModel paramsModel = ComponentFactory.getModel( parameters );
                Property property = paramsModel;
                String propertyName = node.getName().replace( ':', '/' );
                for( String propertyPart : TextUtil2.split( propertyName, '/' ) )
                {
                    property = property.findProperty( propertyPart );
                }
                iconId = DataElementPathEditor.getIconId( property );
                if( iconId != null && !iconId.isEmpty() )
                    node.getAttributes().add( new DynamicProperty( "iconId", String.class, iconId ) );
            }
            catch( Exception e )
            {
            }
        }
        else
        {
            //If icon id is incorrect, IconFactory.getIconById will return ImageIcon with size (-1,-1). Use default connector icon in this case.
            ImageIcon icon = IconFactory.getIconById( iconId );
            if( icon.getIconHeight() < 0 )
                iconId = null;
        }

        if( iconId == null || iconId.isEmpty() )
            iconId = ClassLoading.getResourceLocation( getClass(), "resources/default-connector.gif" );
        ImageView img = new ImageView( IconFactory.getIconById( iconId ).getImage(), 0, 0 );
        img.setPath( iconId );
        container.add( img, CompositeView.X_LL | CompositeView.Y_TT );

        Compartment parent = ( (Compartment)node.getOrigin() );
        Point location = parent.getLocation();
        Dimension dimension = parent.getShapeSize();

        if( isIn )
        {
            node.setLocation( location.x + 2, location.y + (int) ( dimension.height * getPart( node, isIn ) ) - r.height / 2 );
        }
        else
        {
            node.setLocation( location.x + dimension.width - r.width - 2, location.y + (int) ( dimension.height * getPart( node, isIn ) )
                    - r.height / 2 );
        }

        return false;
    }

    protected boolean createPortCoreView(CompositeView container, Node node, WorkflowDiagramViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( node.getName(), diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = fillNodeBrush( node, diagramOptions.getParameterBrush() );
        PolygonView view = new PolygonView( diagramOptions.getParameterPen(), nodeBrush, new int[] { -d, -d, r.width + d * 2,
                r.width + d * 2 + d * 3, r.width + d * 2}, new int[] {r.height + d, -d, -d, r.height / 2, r.height + d} );
        view.setModel( node );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    protected boolean createExpressionCoreView(CompositeView container, Node node, WorkflowDiagramViewOptions diagramOptions, Graphics g)
    {
        View text = new TextView( node.getName(), diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = fillNodeBrush( node, diagramOptions.getExpressionBrush() );
        PolygonView view = new PolygonView( diagramOptions.getExpressionPen(), nodeBrush, new int[] {0, -r.height / 2 - d, -d, r.width + d,
                r.width + r.height / 2 + d, r.width + d}, new int[] {r.height + d, r.height / 2, -d, -d, r.height / 2, r.height + d} );
        view.setModel( node );
        view.setActive( true );
        container.add( view );
        container.add( text, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    protected boolean createCycleCoreView(CompositeView container, Compartment node, WorkflowDiagramViewOptions diagramOptions, Graphics g)
    {
        Brush shadowBrush = new Brush();
        Pen pen = diagramOptions.getDefaultPen();
        Brush mainBrush = getBrush(node, new Brush(Color.white));
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
        SimplePath path = new SimplePath( new int[] {location.x + width, location.x + width + 15, location.x + width + 15,
                location.x - 15, location.x - 15, location.x}, new int[] {location.y + height * 2 / 3,
                location.y + height * 2 / 3, location.y + height + 25, location.y + height + 25,
                location.y + height * 2 / 3, location.y + height * 2 / 3}, 6 );
        View arrow = new ArrowView( pen, null, path, null, ArrowView.createArrowTip( pen, shadowBrush, 8, 12, 5 ) );
        container.add( arrow );
        return false;
    }

    protected boolean createCycleVariableCoreView(CompositeView container, Node node, WorkflowDiagramViewOptions diagramOptions, Graphics g)
    {
        String title = node.getName();
        if( isNodeRunning( node ) )
        {
            WorkflowCycleVariable var = (WorkflowCycleVariable)WorkflowItemFactory.getWorkflowItem( node );
            String value = var.getValueString();
            if( value.length() > 30 )
                value = "..." + value.substring( value.length() - 30 );
            title = title + ": " + value;
        }
        View text = new TextView( title, diagramOptions.getNodeTitleFont(), g );
        int d = 2;
        Rectangle r = text.getBounds();

        Brush nodeBrush = fillNodeBrush( node, diagramOptions.getExpressionBrush() );
        PolygonView view = new PolygonView( diagramOptions.getExpressionPen(), nodeBrush, new int[] {0, -r.height / 2 - d, -d, r.width + d,
                r.width + r.height / 2 + d, r.width + d}, new int[] {r.height + d, r.height / 2, -d, -d, r.height / 2, r.height + d} );
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

    protected boolean isNodeRunning(Node node)
    {
        Object status = node.getAttributes().getValue( WorkflowEngine.NODE_STATUS_PROPERTY );
        if( status != null )
        {
            try
            {
                return Integer.parseInt( status.toString() ) >= 0;
            }
            catch( NumberFormatException e )
            {
            }
        }
        return false;
    }

    protected Brush fillNodeBrush(Node node, Brush defaultBrush)
    {
        return isNodeRunning( node ) ? new Brush( toBlackWhite( defaultBrush.getPaint() ) ) : defaultBrush;
    }

    protected Paint toBlackWhite(Paint paint)
    {
        double gamma = 2.5;
        if( paint instanceof Color )
        {
            Color color = (Color)paint;
            int c = (int) ( Math.pow( ( 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue() ) / 255, gamma ) * 255 );
            return new Color( c, c, c );
        }
        else if( paint instanceof GradientPaint )
        {
            GradientPaint gradient = (GradientPaint)paint;
            int c1 = (int) ( Math.pow( ( 0.299 * gradient.getColor1().getRed() + 0.587 * gradient.getColor1().getGreen() + 0.114 * gradient
                    .getColor1().getBlue() ) / 255, gamma ) * 255 );
            int c2 = (int) ( Math.pow( ( 0.299 * gradient.getColor2().getRed() + 0.587 * gradient.getColor2().getGreen() + 0.114 * gradient
                    .getColor2().getBlue() ) / 255, gamma ) * 255 );
            return new GradientPaint( gradient.getPoint1(), new Color( c1, c1, c1 ), gradient.getPoint2(), new Color( c2, c2, c2 ) );
        }
        return null;
    }

    protected TextView getProgress(Node node, WorkflowDiagramViewOptions diagramOptions, Graphics g)
    {
        Object status = node.getAttributes().getValue( WorkflowEngine.NODE_STATUS_PROPERTY );
        String progressStr = null;
        if( status != null )
        {
            if( Integer.parseInt( status.toString() ) < 0 )
            {
            }
            else if( Integer.parseInt( status.toString() ) <= 100 )
            {
                progressStr = status + "%";
            }
        }
        if( progressStr != null )
            return new TextView( progressStr, diagramOptions.getProgressFont(), g );
        return null;
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        String type = node.getKernel().getType();
        if( type.equals( Type.TYPE_DATA_ELEMENT_IN ) || type.equals( Type.ANALYSIS_PARAMETER ) || type.equals( Type.TYPE_DATA_ELEMENT_OUT ))
            return new InOutFinder( false, getNodeBounds( node ) );
        return super.getPortFinder( node );
    }
}
