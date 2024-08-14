package ru.biosoft.math.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.math.parser.Parser;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author lan
 *
 */
public class FormulaConnection extends URLConnection
{
    private BufferedImage image;

    protected FormulaConnection(URL url)
    {
        super(url);
        String formula = url.getPath().substring(1);
        Parser parser = new Parser();
        int status = parser.parse(formula);
        View view = null;
        if(status != 0)
        {
            view = new ComplexTextView(parser.getMessages().isEmpty() ? "Formula parse error" : parser.getMessages().get(0),
                    new ColorFont(), null, ComplexTextView.LEFT, ApplicationUtils.getGraphics(), 300);
        } else
        {
            FormulaViewBuilder formulaViewBuilder = new FormulaViewBuilder();
            view = formulaViewBuilder.createView(parser.getStartNode(), ApplicationUtils.getGraphics());
        }
        Rectangle r = view.getBounds();
        image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(1, 1, 1, 0.f));
        graphics.fill(new Rectangle(0, 0, r.width, r.height));
        view.move(-r.x, -r.y);
        view.paint(graphics);
    }

    @Override
    public void connect() throws IOException
    {
    }

    @Override
    public String getContentType()
    {
        return "image/png";
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
