package biouml.plugins.sbol;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.swing.ImageIcon;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FileUtils;

import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.ArrowView.Tip;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.PathView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.IconUtils;

public class SbolDiagramViewBuilder extends DefaultDiagramViewBuilder
{

    @Override
    public boolean needAddTitle(Node node)
    {
        if( node.getKernel() instanceof InteractionProperties )
            return false;
        return true;
    }

    @Override
    public CompositeView createImageView(Node node, Graphics g)
    {
        CompositeView cView = null;

        BufferedImage image = null;
        String imgPath = SbolUtil.getSbolImage( node );
        Dimension size = node.getShapeSize();

        int width = size.width;//.max( icon.getIconWidth(), size.width );
        int height = size.height;//Math.max( icon.getIconHeight(), size.height );
        int vertShift = 0;
        //        node.setShapeSize( new Dimension( width, height ) );

        if( !imgPath.toString().endsWith( ".png" ) )
        {
            try
            {
                //InputStream settings = getClass().getResourceAsStream( "resources/markup-cropped.svg" );
                InputStream settings = getClass().getResourceAsStream( "resources/" + imgPath + ".svg" );
                image = rasterize( settings, width, height );
                vertShift = SbolUtil.getVerticalShift( imgPath );
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
            }
        }
        else
        {
            ImageIcon icon = IconUtils.getImageIcon( imgPath );
            if( icon != null )
            {
                image = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
                image.getGraphics().drawImage( icon.getImage(), 0, 0, width, height, null );
            }
            else
                return null;
        }
        if( node.getAttributes().getValue( "isReverse" ) != null )
        {
            AffineTransform tx = AffineTransform.getScaleInstance( -1, 1 );
            tx.translate( -image.getWidth( null ), 0 );
            AffineTransformOp op = new AffineTransformOp( tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR );
            image = op.filter( image, null );
        }
        ImageView imageView = new ImageView( image, node.getLocation().x, node.getLocation().y + vertShift, width, height );
        imageView.setPath( "biouml.plugins.sbol:biouml/plugins/sbol/resources/" + imgPath.toString() + ".svg" );

        cView = new CompositeView();
        cView.add( imageView );
        //TODO: make better solution, now we have to enter invisible box for image to be shifted
        BoxView bv = new BoxView( getBorderPen( node, new Pen() ), null, node.getLocation().x, node.getLocation().y, width, vertShift );
        bv.setVisible( false );
        cView.add( bv );
        bv = new BoxView( null, new Brush( new Color( 10, 200, 50, 100 ) ), node.getLocation().x, node.getLocation().y + vertShift, width,
                height );
        //cView.add(bv);
        if( node.getAttributes().getValue( "isComposite" ) != null )
        {
            URL imgCompPath = this.getClass().getResource( "resources/composite.png" );
            ImageIcon iconComp = IconUtils.getImageIcon( imgCompPath );
            Image imageComp = new BufferedImage( iconComp.getIconWidth(), iconComp.getIconHeight(), BufferedImage.TYPE_INT_ARGB );
            imageComp.getGraphics().drawImage( iconComp.getImage(), 0, 0, null );
            Dimension sizeComp = new Dimension( iconComp.getIconWidth(), iconComp.getIconHeight() );
            ImageView compNodeImageView = new ImageView( imageComp, node.getLocation().x, node.getLocation().y + size.height,
                    sizeComp.width, sizeComp.height );
            compNodeImageView.setPath( imgCompPath.toString() );
            cView.add( compNodeImageView );
        }

        cView.setModel( node );
        cView.setActive( true );
        cView.setLocation( node.getLocation() );
        node.setView( cView );
        return cView;
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( ! ( options instanceof SbolDiagramViewOptions ) )
            return super.createCompartmentCoreView( container, compartment, options, g );

        if( ! ( compartment.getKernel() instanceof Backbone ) )
            return super.createCompartmentCoreView( container, compartment, options, g );

        Dimension shapeSize = compartment.getShapeSize();
       Backbone backbone = (Backbone)compartment.getKernel();
        boolean isCircular = backbone.getTopologyType().equals( SbolConstants.TOPOLOGY_CIRCULAR);
        boolean isWithChromLocus = backbone.getTopologyType().equals( SbolConstants.TOPOLOGY_LOCUS);
        boolean hasEnds = isWithChromLocus || isCircular;
        BoxView shapeView = new BoxView( null, getBrush( compartment, new Brush( new Color( 240, 240, 240 ) ) ),
                new Rectangle( 0, 0, shapeSize.width, shapeSize.height ) );
        shapeView.setLocation( compartment.getLocation() );
        Pen backbonePen = ( (SbolDiagramViewOptions)options ).getBackbonePen();
        int vertShift = (int)Math.round( backbonePen.getWidth() ) - 1;
        CompositeView view = new CompositeView();
        int lineWidth = shapeSize.width;//hasEnds ? shapeSize.width - 20 : shapeSize.width;
        int lineStart = 0;// hasEnds ? 10 : 0;
        LineView lineView = new LineView( backbonePen, lineStart, 0, lineWidth, 0 );
        view.add( shapeView );
        view.add( lineView, CompositeView.X_CC | CompositeView.Y_BB, new Point( 0, 14 ) );

        if( isCircular )
        {
            //                InputStream imageStream = getClass().getResourceAsStream("resources/circular-plasmid.svg");
            //                BufferedImage image = rasterize(imageStream, 0, 0);
            //                ImageView imageView = new ImageView(image, compartment.getLocation().x, compartment.getLocation().y, image.getWidth(), image.getHeight());
            //                imageView.setPath("circular-plasmid");
            //view.add(imageView, CompositeView.X_LL | CompositeView.Y_BB);

            GeneralPath path = new GeneralPath();
            path.moveTo( 0, 0 );
            path.quadTo( 30, 0, 30, 8 );
            path.quadTo( 30, 13, 8, 14 );
            view.add( new PathView( backbonePen, path ), CompositeView.X_RL | CompositeView.Y_BB, new Point( 0, vertShift ) );

            GeneralPath path2 = new GeneralPath();
            path2.moveTo( 30, 0 );
            path2.quadTo( 0, 0, 0, 8 );
            path2.quadTo( 0, 13, 22, 14 );
            view.add( new PathView( backbonePen, path2 ), CompositeView.X_LR | CompositeView.Y_BB, new Point( 0, vertShift ) );

        }
        else if( isWithChromLocus )
        {
            GeneralPath path = new GeneralPath();
            path.moveTo( 0, 0 );
            path.quadTo( 30, 0, 30, 6 );
            path.curveTo( 30, 10, 2, 10, 2, 14 );
            path.quadTo( 2, 18, 32, 18 );
            view.add( new PathView( backbonePen, path ), CompositeView.X_RL | CompositeView.Y_BB, new Point( 0, vertShift - 4 ) );

            GeneralPath path2 = new GeneralPath();
            path2.moveTo( 32, 0 );
            path2.quadTo( 2, 0, 2, 6 );
            path2.curveTo( 2, 10, 30, 10, 30, 14 );
            path2.quadTo( 30, 18, 0, 18 );
            view.add( new PathView( backbonePen, path2 ), CompositeView.X_LR | CompositeView.Y_BB );
        }

        container.add( view );
        container.setModel(compartment);
        view.setModel( compartment );

        view.setActive( true );
        return false;
    }

    @Override
    public @Nonnull CompositeView createCompartmentView(Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( ( compartment.getKernel() instanceof Backbone ) )
            return super.createCompartmentView( compartment, options, g );

        boolean notificationEnabled = false;
        if( compartment.isNotificationEnabled() )
        {
            notificationEnabled = true;
            compartment.setNotificationEnabled( false );
        }

        CompositeView result = null;

        try
        {
            if( compartment.isUseCustomImage() )
                result = createImageView( compartment, g );
        }
        catch( Exception ex )
        {
            log.info( "Error during compartment image creation: name = " + compartment.getCompleteNameInDiagram() + ", error: "
                    + ExceptionRegistry.translateException( ex ) );
        }

        if( result == null )
            return super.createCompartmentView( compartment, options, g );

        createNodeTitle( result, compartment, options, g );

        if( notificationEnabled )
            compartment.setNotificationEnabled( true );

        return result;
    }

    @Override
    protected void createNodeTitle(CompositeView view, Node node, DiagramViewOptions options, Graphics g)
    {
        int maxStringSize = options.getNodeTitleLimit();
        Map<String, ColorFont> fontRegistry = options.getFontRegistry();
        String text = node.getTitle();
        String baseText = text;
        int length = text.length();
        ColorFont font = getTitleFont( node, options.getNodeTitleFont() );
        int limit = (int)Math.max( 0, 48 - Math.min( options.getNodeTitleMargin().x, maxStringSize ) );
        ComplexTextView titleView = new ComplexTextView( text, font, fontRegistry, ComplexTextView.TEXT_ALIGN_CENTER, maxStringSize, g );
        while( text.length() > 10 && ( titleView.getBounds().getWidth() > limit ) )
        {
            length--;
            text = baseText.substring( 0, length ).concat( "..." );
            titleView = new ComplexTextView( text, font, fontRegistry, ComplexTextView.TEXT_ALIGN_CENTER, maxStringSize, g );
        }
        view.add( titleView, CompositeView.X_CC | CompositeView.Y_BT, options.getNodeTitleMargin() );
    }

    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getKernel() instanceof MolecularSpecies )
        {
            return createMolecularSpecies( container, node, options, g );
        }
        else if( node.getKernel().getType().equals( SbolConstants.DEGRADATION_PRODUCT ) )
        {
            return createSourceSinkView( container, node, options, g );
        }
        return super.createNodeCoreView( container, node, options, g );
    }

    public boolean createMolecularSpecies(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        MolecularSpecies base = (MolecularSpecies)node.getKernel();

        switch( base.getType() )
        {
            case SbolConstants.COMPLEX:
            {
                createComplexView( container, node, options, g );
                break;
            }
            case SbolConstants.PROTEIN:
            {
                createProteinView( container, node, options, g );
                break;
            }
            case SbolConstants.SIMPLE_CHEMICAL:
            {
                createSimpleChemicalView( container, node, options, g );
                break;
            }
        }
        return true;
    }

    public boolean createSourceSinkView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        int w = Math.max( d.width, 25 );
        int h = Math.max( d.height, 25 );
        Pen pen = getBorderPen( node, options.getNodePen() );
        container.add( new EllipseView( pen, new Brush( Color.white ), 0, 0, w, h ) );
        container.add( new LineView( pen, new Point( 3, h + 3 ), new Point( w - 3, -3 ) ) );
        return false;
    }

    private boolean createProteinView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        d.width = Math.max( d.width, 50 );
        d.height = Math.max( d.height, 25 );
        Pen pen = getBorderPen( node, options.getNodePen() );
        int round = Math.max( Math.min( Math.min( d.width, d.height ) / 3, 20 ), 2 );
        BoxView view = new BoxView( pen, new Brush( Color.white ), new RoundRectangle2D.Float( 0, 0, d.width, d.height, round, round ) );
        container.add( view );
        setView( container, node );
        return false;
    }

    private boolean createSimpleChemicalView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        d.width = Math.max( d.width, 50 );
        d.height = Math.max( d.height, 25 );
        Pen pen = getBorderPen( node, options.getNodePen() );
        int round = Math.max( Math.min( Math.min( d.width, d.height ) / 3, 20 ), 2 );
        BoxView view = new BoxView( pen, new Brush( Color.white ), new RoundRectangle2D.Float( 0, 0, d.width, d.height, round, round ) );
        container.add( view );
        setView( container, node );
        return false;
    }

    private boolean createComplexView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        d.width = Math.max( d.width, 50 );
        d.height = Math.max( d.height, 25 );
        Pen pen = getBorderPen( node, options.getNodePen() );

        int delta = Math.min( Math.min( 15, d.width / 3 ), d.height / 2 );
        int[] x = new int[] {delta, d.width - delta, d.width, d.width, d.width - delta, delta, 0, 0};
        int[] y = new int[] {0, 0, delta, d.height - delta, d.height, d.height, d.height - delta, delta};
        PolygonView polygon = new PolygonView( pen, new Brush( Color.white ), x, y );

        container.add( polygon );
        setView( container, node );
        return true;
    }

    public static void setView(CompositeView view, Node node)
    {
        view.setModel( node );
        view.setLocation( node.getLocation() );
        view.setActive( true );
        node.setView( view );
    }

    /**
     * Transforms SVG file to Buffered Image with given size
     */
    public static BufferedImage rasterize(InputStream svgFile, int width, int height) throws IOException
    {
        final BufferedImage[] imagePointer = new BufferedImage[1];

        String css = "svg {" + "shape-rendering: geometricPrecision;" + "text-rendering:  geometricPrecision;"
                + "color-rendering: optimizeQuality;" + "image-rendering: optimizeQuality;" + "}";
        File cssFile = File.createTempFile( "batik-default-override-", ".css" );
        FileUtils.writeStringToFile( cssFile, css );

        TranscodingHints transcoderHints = new TranscodingHints();
        transcoderHints.put( ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE );
        transcoderHints.put( ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation() );
        transcoderHints.put( ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI );
        transcoderHints.put( ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg" );
        transcoderHints.put( ImageTranscoder.KEY_USER_STYLESHEET_URI, cssFile.toURI().toString() );

        if( width > 0 )
            transcoderHints.put( ImageTranscoder.KEY_WIDTH, (float)width );
        if( height > 0 )
            transcoderHints.put( ImageTranscoder.KEY_HEIGHT, (float)height );

        try
        {
            TranscoderInput input = new TranscoderInput( svgFile );
            ImageTranscoder t = new ImageTranscoder()
            {
                @Override
                public BufferedImage createImage(int w, int h)
                {
                    return new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
                }

                @Override
                public void writeImage(BufferedImage image, TranscoderOutput out) throws TranscoderException
                {
                    imagePointer[0] = image;
                }
            };
            t.setTranscodingHints( transcoderHints );
            t.transcode( input, null );
        }
        catch( TranscoderException ex )
        {
            ex.printStackTrace();
            throw new IOException( "Couldn't convert " + svgFile );
        }
        finally
        {
            cssFile.delete();
        }

        return imagePointer[0];
    }

    @Override
    public CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = new CompositeView();
        Pen pen = getBorderPen( edge, options.getDefaultPen() );

        if( edge.getPath() == null )
            Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );

        SimplePath path = edge.getSimplePath();
        String edgeType = edge.getKernel().getType();
        Brush brush = new Brush( Color.white );
        Tip tip = null;

        switch( edgeType )
        {
            case SbolConstants.CONTROL:
            case SbolConstants.MODIFIED:
                tip = ArrowView.createDiamondTip( pen, brush, 5, 10, 5 );
                break;
            case SbolConstants.STIMULATION:
            case SbolConstants.STIMULATOR:
                tip = ArrowView.createTriangleTip( pen, brush, 15, 5 );
                break;
            //        case Type.TYPE_CATALYSIS:
            //            tip = ArrowView.createEllipseTip(pen, brush, 6);
            //            break;
            case SbolConstants.INHIBITION:
            case SbolConstants.INHIBITOR:
                brush = new Brush( pen.getColor() );
                tip = ArrowView.createLineTip( pen, brush, 3, 8 );
                break;
            case SbolConstants.PROCESS:
            case SbolConstants.PRODUCT:
                tip = ArrowView.createTriangleTip( pen, new Brush(Color.black), 15, 5 );
                break;
            default:
                tip = null;//ArrowView.createSimpleTip(pen, 6, 4);
        }

        View arrow = new ArrowView( pen, null, path, null, tip );

        arrow.setModel( edge );
        arrow.setActive( true );
        view.add( arrow );

        view.setModel( edge );
        view.setActive( false );

        return view;
    }

    @Override
    public boolean calculateInOut(Edge edge, Point in, Point out)
    {
        super.calculateInOut( edge, in, out, 0, 0 );

        Node inNode = edge.getInput();
        Node outNode = edge.getOutput();

        if( inNode.getCompartment().getKernel() instanceof Backbone && inNode.getOrigin() == outNode.getOrigin() )
        {
            Rectangle inBounds = getNodeBounds( edge.getInput() );
            Rectangle outBounds = getNodeBounds( edge.getOutput() );

            in.x = inBounds.x + inBounds.width / 2;
            in.y = inBounds.y;
            out.x = outBounds.x + outBounds.width / 2;
            out.y = outBounds.y;
        }
        return true;
    }

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new SbolDiagramViewOptions( null );
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        if( node.getKernel() instanceof SequenceFeature )
        {
            Rectangle bounds = getNodeBounds( node );
            return new FeaturePortFinder( bounds );
        }
        return null;
    }
}
