package biouml.plugins.sbol;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;

import javax.swing.ImageIcon;

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
import ru.biosoft.util.IconUtils;

public class SbolDiagramViewBuilder extends DefaultDiagramViewBuilder
{

    @Override
    public CompositeView createImageView(Node node, Graphics g)
    {
        CompositeView cView = null;

        Image image = null;
        //try to load buffered image from repository
        URL imgPath = (URL) node.getAttributes().getValue("node-image");
        Dimension size = node.getShapeSize();
        ImageIcon icon = IconUtils.getImageIcon(imgPath);
        if ( icon != null )
        {
            image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            image.getGraphics().drawImage(icon.getImage(), 0, 0, null);
            if ( size.width == 0 && size.height == 0 )
                size = new Dimension(icon.getIconWidth(), icon.getIconHeight());
        }

        if ( image == null )
            return null;

        ImageView imageView = new ImageView(image, node.getLocation().x, node.getLocation().y, size.width, size.height);
        imageView.setPath(imgPath.toString());

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
        LineView lineView = new LineView(boldPen, 0, 0, (float) compartment.getShapeSize().getWidth(), 0);
        
        view.add( shapeView );
        view.add(lineView, CompositeView.X_CC | CompositeView.Y_BB, new Point(0, 14));
        
        container.add(view);
        view.setModel(compartment);
        view.setActive(true);
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

}
