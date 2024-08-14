package biouml.model.xml._test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

import junit.framework.TestCase;
import biouml.model.xml.XmlDiagramTypeReader;
import biouml.model.xml.XmlDiagramTypeWriter;

public class ImageBase64CodeTest extends TestCase
{
    public static Image createTestImage()
    {
        int width = 300;
        int height = 200;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        g.drawLine(0, 0, width, height);

        return img;
    }

    public void testImageEncoding() throws Exception
    {
        Image testImage = createTestImage();
        ImageIcon i = new ImageIcon(testImage);
        String base64 = XmlDiagramTypeWriter.encodeBase64(i);
        assertNotNull(base64);

        ImageIcon icon = (ImageIcon)XmlDiagramTypeReader.getIconFromBase64(base64);
        assertNotNull(icon);
        
        // Show pictures
        JFrame frame = new JFrame("Test pictures");
        frame.getContentPane().setLayout(new GridBagLayout());
        frame.getContentPane().add(new JButton(new ImageIcon(testImage)));
        frame.getContentPane().add(new JButton(icon));
        
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                System.exit(0);
            }
        });
        
        frame.setVisible(true);
        while (true)
        {
            Thread.sleep(100);
        }
    }
}
