package biouml.workbench;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.imageio.ImageIO;

import one.util.streamex.StreamEx;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.font.ColorFont;

class GenerateSplash
{
    public static final int WIDTH = 405;
    public static final int HEIGHT = 247;

    public static void main(String[] args) throws IOException
    {
        if( args.length < 3 )
            return;

        String version = args[0];
        String fileName = args[1];

        String paths;

        boolean expanded = false;
        if (args.length == 4)
        {
            expanded = args[2].equals("true");
            paths = args[3];
        }
        else
            paths = args[2];

        int height = expanded? HEIGHT + 21: HEIGHT;
        int textLocation = HEIGHT - 10;
        BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        CompositeView result = new CompositeView();
        result.add(new ImageView(ImageIO.read(GenerateSplash.class.getResource("resources/splash_base.png").openStream()), 0, 0, WIDTH,
                height));
        Font font = new Font("Verdana", Font.BOLD, 26);
        result.add(new TextView("Version " + version, new Point(WIDTH / 2 + 2, 142), TextView.CENTER, new ColorFont(font, new Color(200,
                200, 200)), graphics));
        result.add(new TextView("Version " + version, new Point(WIDTH / 2, 140), TextView.CENTER,
                new ColorFont(font, new Color(0, 166, 81)), graphics));
        result.add(new TextView("\u00A9 2002-" + Calendar.getInstance().get(Calendar.YEAR)
                + " Institute of Systems Biology, Novosibirsk, Russia", new Point(WIDTH / 2, textLocation), TextView.CENTER, new ColorFont(
                new Font("Tahoma", Font.BOLD, 12), new Color(24, 2, 104)), graphics));


        Rectangle shape = new Rectangle(new Point(0, 0), new Dimension(WIDTH, height));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setClip(shape);
        graphics.setColor(Color.WHITE);
        graphics.fill(shape);
        result.paint(graphics);

        for( String s : StreamEx.of( paths ).flatMap( s -> StreamEx.split( s, ';' ) ) )
            ImageIO.write(image, "PNG", new FileOutputStream(new File(s + fileName)));
    }
}