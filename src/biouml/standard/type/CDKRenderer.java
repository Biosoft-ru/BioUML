package biouml.standard.type;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.openscience.cdk.Molecule;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.ArrowElement;
import org.openscience.cdk.renderer.elements.AtomSymbolElement;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.elements.LineElement;
import org.openscience.cdk.renderer.elements.OvalElement;
import org.openscience.cdk.renderer.elements.PathElement;
import org.openscience.cdk.renderer.elements.RectangleElement;
import org.openscience.cdk.renderer.elements.TextElement;
import org.openscience.cdk.renderer.elements.TextGroupElement;
import org.openscience.cdk.renderer.elements.WedgeLineElement;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.font.IFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator.Scale;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;

import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.ArrowView.Tip;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.PathView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

public class CDKRenderer
{

    public static class CompositeViewDrawVisitor implements IDrawVisitor
    {
        /**
         * The font manager cannot be set by the constructor as it needs to
         * be managed by the Renderer.
         */
        private AWTFontManager fontManager;

        protected CompositeView parentView;

        protected BasicStroke stroke;
        protected Graphics2D g;

        public CompositeViewDrawVisitor(CompositeView parentView, Graphics2D g)
        {
            this.parentView = parentView;
            this.g = g;
            this.fontManager = null;
            this.stroke = new BasicStroke( 0.5f );
        }

        @Override
        public void visit(IRenderingElement element)
        {
            if( element instanceof ElementGroup )
                visit( (ElementGroup)element );
            else if( element instanceof WedgeLineElement )
                visit( (WedgeLineElement)element );
            else if( element instanceof LineElement )
                visit( (LineElement)element );
            else if( element instanceof ArrowElement )
                visit( (ArrowElement)element );
            else if( element instanceof OvalElement )
                visit( (OvalElement)element );
            else if( element instanceof TextGroupElement )
                visit( (TextGroupElement)element );
            else if( element instanceof AtomSymbolElement )
                visit( (AtomSymbolElement)element );
            else if( element instanceof TextElement )
                visit( (TextElement)element );
            else if( element instanceof RectangleElement )
                visit( (RectangleElement)element );
            else if( element instanceof PathElement )
                visit( (PathElement)element );
            else
                System.err.println( "Visitor method for " + element.getClass().getName() + " is not implemented" );
        }

        @Override
        public void setFontManager(IFontManager fontManager)
        {
            this.fontManager = (AWTFontManager)fontManager;
        }

        @Override
        public void setRendererModel(RendererModel rendererModel)
        {
        }

        @Override
        public void setTransform(AffineTransform transform)
        {
            this.transform = transform;
        }

        /**
         * This is initially null, and must be set in the setTransform method!
         */
        protected AffineTransform transform = null;

        public Point transformPoint(double x, double y)
        {
            double[] src = new double[] {x, y};
            double[] dest = new double[2];
            this.transform.transform( src, 0, dest, 0, 1 );
            return new Point( (int)dest[0], (int)dest[1] );
        }

        protected void visit(ElementGroup elementGroup)
        {
            elementGroup.visitChildren( this );
        }

        protected void visit(ArrowElement line)
        {
            Pen pen = new Pen( (float)line.width, line.color );
            Brush brush = new Brush( line.color );
            Point start = transformPoint( line.x1, line.y1 );
            Point end = transformPoint( line.x2, line.y2 );
            Tip startTip = null;
            Tip endTip = null;
            if( line.direction )
            {
                startTip = ArrowView.createArrowTip( pen, brush, 6, 6, 4 );
            }
            else
            {
                endTip = ArrowView.createArrowTip( pen, brush, 6, 6, 4 );
            }
            ArrowView view = new ArrowView( pen, brush, start.x, start.y, end.x, end.y, startTip, endTip );
            parentView.add( view );
        }


        protected void visit(LineElement line)
        {
            Pen pen = new Pen( stroke, line.color );
            Brush brush = new Brush( line.color );
            Point start = transformPoint( line.x1, line.y1 );
            Point end = transformPoint( line.x2, line.y2 );
            ArrowView view = new ArrowView( pen, brush, start.x, start.y, end.x, end.y, null, null );
            parentView.add( view );
        }

        protected void visit(OvalElement oval)
        {
            Pen pen = new Pen( stroke, oval.color );
            Brush brush = null;
            if( oval.fill )
            {
                brush = new Brush( oval.color );
            }
            Point min = transformPoint( oval.x - oval.radius, oval.y - oval.radius );
            Point max = transformPoint( oval.x + oval.radius, oval.y + oval.radius );
            int w = max.x - min.x;
            int h = max.y - min.y;

            EllipseView view = new EllipseView( pen, brush, min.x + w / 2, min.y + h / 2, w, h );
            parentView.add( view );
        }

        protected void visit(TextElement textElement)
        {
            Point location = transformPoint( textElement.x, textElement.y );
            CompositeView text = new CompositeView();

            ColorFont colorFont = new ColorFont( fontManager.getFont(), textElement.color );
            View textView = new TextView( textElement.text, colorFont, g );
            Rectangle bounds = textView.getBounds();
            location = new Point( location.x - bounds.width / 2, location.y - bounds.height / 2 );

            BoxView back = new BoxView( null, new Brush( Color.white ), bounds );
            text.add( back );
            text.add( textView );
            text.setLocation( location );

            parentView.add( text );
        }
        protected void visit(WedgeLineElement wedge)
        {
            Brush brush = new Brush( wedge.color );
            Point start = transformPoint( wedge.x1, wedge.y1 );
            Point end = transformPoint( wedge.x2, wedge.y2 );
            int length = (int)Math.sqrt( ( ( start.x - end.x ) * ( start.x - end.x ) ) + ( ( start.y - end.y ) * ( start.y - end.y ) ) );
            View view = null;
            if( wedge.isDashed )
            {
                BasicStroke stroke2 = new BasicStroke( 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {3.0f, 3.0f},
                        0.0f );
                Pen pen = new Pen( stroke2, wedge.color );
                view = new LineView( pen, start.x, start.y, end.x, end.y );
            }
            else
            {
                Pen pen = new Pen( stroke, wedge.color );
                view = new ArrowView( pen, brush, start.x, start.y, end.x, end.y, ArrowView.createArrowTip( pen, brush, length, length, 2 ),
                        null );
            }
            parentView.add( view );
        }

        protected void visit(AtomSymbolElement atomSymbol)
        {
            Point location = transformPoint( atomSymbol.x, atomSymbol.y );
            CompositeView text = new CompositeView();

            ColorFont colorFont = new ColorFont( fontManager.getFont(), atomSymbol.color );
            View textView = new TextView( atomSymbol.text, colorFont, g );
            Rectangle bounds = textView.getBounds();
            location = new Point( location.x - bounds.width / 2, location.y - bounds.height / 2 );

            BoxView back = new BoxView( null, new Brush( Color.white ), bounds );
            text.add( back );
            text.add( textView );
            text.setLocation( location );
            parentView.add( text );

            /*int offset = 10; // XXX
            String chargeString = null;
            if( atomSymbol.formalCharge == 0 )
            {
                return;
            }
            else if( atomSymbol.formalCharge == 1 )
            {
                chargeString = "+";
            }
            else if( atomSymbol.formalCharge > 1 )
            {
                chargeString = atomSymbol.formalCharge + "+";
            }
            else if( atomSymbol.formalCharge == -1 )
            {
                chargeString = "-";
            }
            else if( atomSymbol.formalCharge < -1 )
            {
                int absCharge = Math.abs(atomSymbol.formalCharge);
                chargeString = absCharge + "-";
            }

            if( chargeString != null )
            {
                TextView chargeView = new TextView(chargeString, colorFont, g);
                if( atomSymbol.alignment == 1 )
                { // RIGHT
                    text.add(chargeView, CompositeView.REL, new Point(p.x + offset, min.x));
                }
                else if( atomSymbol.alignment == -1 )
                { // LEFT
                    text.add(chargeView, CompositeView.REL, new Point(p.x - offset, min.y));
                }
                else if( atomSymbol.alignment == 2 )
                { // TOP
                    text.add(chargeView, CompositeView.REL, new Point(p.x, p.y - offset));
                }
                else if( atomSymbol.alignment == -2 )
                { // BOT
                    text.add(chargeView, CompositeView.REL, new Point(p.x, p.y + offset));
                }
            }*/
        }

        protected void visit(RectangleElement rectangle)
        {
            Point p1 = this.transformPoint( rectangle.x, rectangle.y );
            Point p2 = this.transformPoint( rectangle.x + rectangle.width, rectangle.y + rectangle.height );
            Pen pen = new Pen( stroke, rectangle.color );
            Brush brush = null;
            if( rectangle.filled )
            {
                brush = new Brush( rectangle.color );
            }

            BoxView view = new BoxView( pen, brush, p1.x, p1.y, p2.x - p1.x, p2.y - p1.y );
            parentView.add( view );
        }

        protected void visit(PathElement path)
        {
            Pen pen = new Pen( stroke, path.color );
            List points = path.points;
            if( points.size() > 0 )
            {
                GeneralPath generalPath = new GeneralPath();
                Point start = (Point)points.get( 0 );
                generalPath.moveTo( start.x, start.y );
                for( int i = 1; i < points.size(); i++ )
                {
                    Point p = (Point)points.get( i );
                    generalPath.lineTo( p.x, p.y );
                }
                PathView view = new PathView( pen, generalPath );
                parentView.add( view );
            }
        }

        protected void visit(TextGroupElement textGroup)
        {
            Point location = transformPoint( textGroup.x, textGroup.y );
            CompositeView text = new CompositeView();

            ColorFont colorFont = new ColorFont( fontManager.getFont(), textGroup.color );
            View textView = new TextView( textGroup.text, colorFont, g );
            Rectangle bounds = textView.getBounds();
            location = new Point( location.x - bounds.width / 2, location.y - bounds.height / 2 );

            BoxView back = new BoxView( null, new Brush( Color.white ), bounds );
            text.add( back );
            text.add( textView );
            text.setLocation( location );
            parentView.add( text );

            //TODO: add textGroup.children
            /*
            for( TextGroupElement.Child child : textGroup.children )
            {
                //First we calculate the child bounds just to find width and height
                Rectangle2D childBounds = getTextBounds(child.text, 0, 0, g, rendererModel.getZoomFactor());
                int oW = (int)childBounds.getWidth();
                int oH = (int)childBounds.getHeight();

                //use that to actually calculate the position
                int cx;
                int cy;

                switch( child.position )
                {
                    case NE:
                        cx = x2;
                        cy = y1;
                        break;
                    case N:
                        cx = x1;
                        cy = y1;
                        break;
                    case NW:
                        cx = x1 - oW;
                        cy = y1;
                        break;
                    case W:
                        cx = x1 - oW;
                        cy = p.y;
                        break;
                    case SW:
                        cx = x1 - oW;
                        cy = y1 + oH;
                        break;
                    case S:
                        cx = x1;
                        cy = y2 + oH;
                        break;
                    case SE:
                        cx = x2;
                        cy = y2 + oH;
                        break;
                    case E:
                        cx = x2;
                        cy = p.y;
                        break;
                    default:
                        cx = x;
                        cy = y;
                        break;
                }
                //for deleting background of child
                //we need the bounds at the actual positions
                childBounds = getTextBounds(child.text, cx, cy, g, rendererModel.getZoomFactor());
                this.g.setColor(textGroup.backColor != null ? textGroup.backColor : this.rendererModel.getBackColor());
                Rectangle2D childBackground = new Rectangle2D.Double(cx, cy - childBounds.getHeight(), childBounds.getWidth(), childBounds
                        .getHeight());
                this.g.fill(childBackground);
                this.g.setColor(textGroup.color);
                //write child
                this.g.drawString(child.text, cx, cy);
                if( child.subscript != null )
                {
                    int scx = (int) ( cx + ( childBounds.getWidth() * 0.75 ) );
                    int scy = (int) ( cy + ( childBounds.getHeight() / 3 ) );
                    Font f = this.g.getFont(); // TODO : move to font manager
                    Font subscriptFont = f.deriveFont(f.getStyle(), f.getSize() - 2);
                    this.g.setFont(subscriptFont);
                    this.g.setColor(textGroup.color);
                    //write subscript
                    this.g.drawString(child.subscript, scx, scy);
                }
            }
            if( textGroup.isNotTypeableUnderlined )
            {
                this.g.setColor(Color.RED);
                this.g.drawLine(x1, y2, x2, y2);
            }*/
        }
    }

    public static double[] getBounds(IAtomContainer molecule)
    {
        double[] minMax = GeometryTools.getMinMax( molecule );
        if( minMax[2] == minMax[0] )
            minMax[2]++;
        if( minMax[3] == minMax[1] )
            minMax[3]++;
        return new double[] {minMax[2] - minMax[0], minMax[3] - minMax[1]};
    }

    protected static void renderImage(Structure structure, Dimension size, Graphics2D g, CompositeView result)
    {
        Rectangle drawArea = new Rectangle( size.width, size.height );

        IMolecule molecule = loadMolecule( structure );

        if( size.width <= 0 || size.height <= 0 )
        {
            size.setSize( GeometryTools.get2DDimension( molecule ) );
        }

        double[] bounds = getBounds( molecule );
        double scaleX = size.getWidth() * 0.9 / bounds[0];
        double scaleY = size.getHeight() * 0.9 / bounds[1];
        GeometryTools.scaleMolecule( molecule, Math.min( scaleX, scaleY ) );
        GeometryTools.center( molecule, size );

        // generators make the image elements
        List<IGenerator<IAtomContainer>> generators = new ArrayList<>();
        generators.add( new BasicSceneGenerator() );
        generators.add( new BasicBondGenerator() );
        generators.add( new BasicAtomGenerator() );

        // the renderer needs to have a toolkit-specific font manager
        AtomContainerRenderer renderer = new AtomContainerRenderer( generators, new AWTFontManager() );

        // the call to 'setup' only needs to be done on the first paint
        renderer.setDrawCenter( drawArea.getCenterX(), drawArea.getCenterY() );
        renderer.setModelCenter( drawArea.getCenterX(), drawArea.getCenterY() );
        renderer.getRenderer2DModel().getParameter( Scale.class ).setValue( 1.0 );

        // the paint method also needs a toolkit-specific renderer
        renderer.paint( molecule, result == null ? new AWTDrawVisitor( g ) : new CompositeViewDrawVisitor( result, g ) );
    }

    public static IMolecule loadMolecule(Structure structure) throws BiosoftParseException
    {
        try
        {
            String structureData = convertDataForCDKReader( structure.getData() );

            PrintStream oldError = System.err;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream newError = new PrintStream( baos );
            IMolecule molecule = null;
            try
            {
                molecule = (IMolecule)SecurityManager.runPrivileged( () -> {

                    System.setErr( newError );
                    IMolecule mol;
                    try
                    {
                        MDLReader mdl = new MDLReader( new StringReader( structureData ) );
                        mol = (Molecule)mdl.read( new Molecule() );

                        //try to generate 2D coordinates
                        if( !GeometryTools.has2DCoordinates( mol ) )
                            try
                            {
                                StructureDiagramGenerator sdg = new StructureDiagramGenerator();
                                sdg.setMolecule( mol );
                                sdg.generateCoordinates();
                                mol = sdg.getMolecule();
                            }
                            catch( Exception e )
                            {
                            }
                    }
                    finally
                    {
                        System.setErr( oldError );
                    }
                    return mol;
                } );
            }
            catch( Exception e1 )
            {
            }

            if( baos.size() > 0 )
            {
                throw new CDKException( new String( baos.toByteArray(), StandardCharsets.ISO_8859_1 ) );
            }
            return molecule;
        }
        catch( CDKException e )
        {
            throw new BiosoftParseException( e, structure.getName() );
        }
    }

    private static String convertDataForCDKReader(String data)
    {
        String structureData = data;
        if( structureData.startsWith( "  " ) )
        {
            structureData = "\n" + structureData;
        }
        //        else
        //        {
        //            int ind = structureData.indexOf( "\n  " );
        //            if( ind != -1 )
        //            {
        //                structureData = structureData.substring( ind );
        //            }
        //        }
        return structureData;
    }

    /**
     * Generate structure image
     */
    public static BufferedImage createStructureImage(Structure structure, Dimension size) throws Exception
    {
        BufferedImage image = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_ARGB );

        // paint the background
        Graphics2D g2 = image.createGraphics();
        g2.setColor( Color.WHITE );
        g2.fillRect( 0, 0, size.width, size.height );

        renderImage( structure, size, g2, null );
        return image;
    }

    /**
     * Create structure view as {@link CompositeView}
     */
    public static @Nonnull CompositeView createStructureView(Structure structure, Dimension size, Graphics2D g) throws Exception
    {
        CompositeView result = new CompositeView();
        renderImage( structure, size, g, result );
        Rectangle bounds = result.getBounds();
        result.move( -bounds.x, -bounds.y );
        return result;
    }
}
