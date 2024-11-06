package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Node;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.util.ApplicationUtils;

public class SbolDiagramViewBuilder extends DefaultDiagramViewBuilder
{

    @Override
    public CompositeView createImageView(Node node, Graphics g)
    {
        CompositeView cView = null;

        Image image = null;
        //try to load buffered image from repository
        String imgPath = node.getAttributes().getValueAsString("node-image");
        Dimension size = node.getShapeSize();
        ImageIcon icon = ApplicationUtils.getImageIcon(imgPath);
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
        imageView.setPath(imgPath);

        cView = new CompositeView();
        cView.add(imageView);
        cView.setModel(node);
        cView.setActive(true);
        cView.setLocation(node.getLocation());
        node.setView(cView);
        return cView;
    }

}
