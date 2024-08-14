package biouml.model.xml._test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramViewBuilder;
import junit.framework.TestCase;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;

public class MakeIconsTransparent extends TestCase
{
    public void testMakeIconsTransparent() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        for(XmlDiagramType type : XmlDiagramType.getTypesCollection())
        {
            List<Object> types = new ArrayList<>();
            types.addAll( Arrays.asList( type.getNodeTypes() ) );
            types.addAll( Arrays.asList( type.getEdgeTypes() ) );
            for(Object nodeType : types)
            {
                Icon icon = type.getDiagramViewBuilder().getIcon( nodeType );
                if(icon.getIconWidth() == -1 || icon.getIconHeight() == -1)
                    continue;
                BufferedImage image = new BufferedImage( icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB );
                icon.paintIcon( null, image.getGraphics(), 0, 0);
                int replaceColor = image.getRGB( 0, 0 );
                for(int i=0 ; i<image.getWidth(); i++)
                {
                    for(int j=0; j<image.getHeight(); j++)
                    {
                        if(image.getRGB( i, j ) == replaceColor)
                            image.setRGB( i, j, 0 );
                    }
                }
                Icon newIcon = new ImageIcon( image );
                ((XmlDiagramViewBuilder)type.getDiagramViewBuilder()).setIcon( nodeType, newIcon );
            }
            CollectionFactoryUtils.save( type );
        }
    }
}
