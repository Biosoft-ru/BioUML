package biouml.plugins.sbol;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import javax.swing.ImageIcon;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FileUtils;

import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.util.IconUtils;

public class SbolDiagramViewBuilder extends DefaultDiagramViewBuilder
{

    @Override
    public CompositeView createImageView(Node node, Graphics g)
    {
        CompositeView cView = null;

        Image image = null;
        //try to load buffered image from repository
        URL imgPath = (URL)node.getAttributes().getValue( "node-image" );
        Dimension size = node.getShapeSize();
      
        int width = size.width;//.max( icon.getIconWidth(), size.width );
        int height = size.height;//Math.max( icon.getIconHeight(), size.height );

//        node.setShapeSize( new Dimension( width, height ) );


        if( imgPath.toString().endsWith( ".svg" ) )
        {
            try
            {
                InputStream settings = getClass().getResourceAsStream( "resources/markup-cropped.svg" );
                image = rasterize( settings, width, height );
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
        ImageView imageView = new ImageView( image, node.getLocation().x, node.getLocation().y, width, height );
        imageView.setPath( imgPath.toString() );

        cView = new CompositeView();
        cView.add(imageView);

        if ( node.getAttributes().getValue("isComposite") != null )
        {
            URL imgCompPath = this.getClass().getResource("resources/composite.png");
            ImageIcon iconComp = IconUtils.getImageIcon(imgCompPath);
            Image imageComp = new BufferedImage(iconComp.getIconWidth(), iconComp.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            imageComp.getGraphics().drawImage(iconComp.getImage(), 0, 0, null);
            Dimension sizeComp = new Dimension(iconComp.getIconWidth(), iconComp.getIconHeight());
            ImageView compNodeImageView = new ImageView(imageComp, node.getLocation().x, node.getLocation().y + size.height, sizeComp.width,
                    sizeComp.height);
            compNodeImageView.setPath(imgCompPath.toString());
            cView.add(compNodeImageView);
        }

        cView.setModel(node);
        cView.setActive(true);
        cView.setLocation(node.getLocation());
        node.setView(cView);
        return cView;
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        Dimension shapeSize = compartment.getShapeSize();
        BoxView shapeView = new BoxView(null, getBrush(compartment, new Brush(Color.yellow.brighter())),
                new Rectangle(0, 0, shapeSize.width, shapeSize.height));
        shapeView.setLocation(compartment.getLocation());
        Pen boldPen = new Pen(2, Color.black);
        CompositeView view = new CompositeView();
        LineView lineView = new LineView( boldPen, 0, 0, (float)compartment.getShapeSize().getWidth(), 0 );

        view.add( shapeView );
        view.add( lineView, CompositeView.X_CC | CompositeView.Y_BB, new Point( 0, 14 ) );

        container.add( view );
        view.setModel( compartment );

        view.setActive( true );
        return false;
    }

    @Override
    protected void createNodeTitle(CompositeView view, Node node, DiagramViewOptions options, Graphics g)
    {
        int maxStringSize = options.getNodeTitleLimit();
        Map<String, ColorFont> fontRegistry = options.getFontRegistry();
        String text = node.getTitle();
        String baseText = text;
        int length = text.length();
        ColorFont font = getTitleFont(node, options.getNodeTitleFont());
        int limit = (int) Math.max(0, 48 - Math.min(options.getNodeTitleMargin().x, maxStringSize));
        ComplexTextView titleView = new ComplexTextView(text, font, fontRegistry, ComplexTextView.TEXT_ALIGN_CENTER, maxStringSize, g);
        while ( text.length() > 3 && (titleView.getBounds().getWidth() > limit) )
        {
            length--;
            text = baseText.substring(0, length).concat("...");
            titleView = new ComplexTextView(text, font, fontRegistry, ComplexTextView.TEXT_ALIGN_CENTER, maxStringSize, g);
        }
        view.add(titleView, CompositeView.X_CC | CompositeView.Y_BT, options.getNodeTitleMargin());
    }

    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getKernel() instanceof MolecularSpecies )
        {
            return createMolecularSpecies( container, node, options, g );
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

    private boolean createProteinView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        d.width = Math.max( d.width, 50 );
        d.height = Math.max( d.height, 25 );
        //        Brush brush = DefaultDiagramViewBuilder.getBrush( node, null );
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
        //        Brush brush = DefaultDiagramViewBuilder.getBrush( node, null );
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
        //        Brush brush = DefaultDiagramViewBuilder.getBrush( node, null );
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


    public static BufferedImage rasterize(InputStream svgFile, int width, int height) throws IOException
    {

        final BufferedImage[] imagePointer = new BufferedImage[1];

        String css = "svg {" + "shape-rendering: geometricPrecision;" + "text-rendering:  geometricPrecision;"
                + "color-rendering: optimizeQuality;" + "image-rendering: optimizeQuality;" + "}";
        File cssFile = File.createTempFile( "batik-default-override-", ".css" );
        FileUtils.writeStringToFile( cssFile, css );

        TranscodingHints transcoderHints = new TranscodingHints();
        transcoderHints.put( ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE );
        transcoderHints.put( ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDomImplementation.getDOMImplementation() );
        transcoderHints.put( ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI );
        transcoderHints.put( ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg" );
        transcoderHints.put( ImageTranscoder.KEY_USER_STYLESHEET_URI, cssFile.toURI().toString() );
        transcoderHints.put( ImageTranscoder.KEY_WIDTH, (float)width );
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
            // Requires Java 6
            ex.printStackTrace();
            throw new IOException( "Couldn't convert " + svgFile );
        }
        finally
        {
            cssFile.delete();
        }

        return imagePointer[0];
    }
}
