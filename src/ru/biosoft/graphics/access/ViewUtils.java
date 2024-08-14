package ru.biosoft.graphics.access;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringEscapeUtils;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.application.ApplicationUtils;

public class ViewUtils
{
    private static final @Nonnull PolygonView errorSymbol = new PolygonView( null, new Brush( Color.RED ),
            new int[] {0, 7, 20, 33, 40, 27, 40, 33, 20,  7,  0, 13},
            new int[] {7, 0, 13,  0,  7, 20, 33, 40, 27, 40, 33, 20} );

    public static BufferedImage paintException(Throwable t)
    {
        String message = "Error rendering image:<br>"+StringEscapeUtils.escapeHtml( ExceptionRegistry.log( t ) ).replace( "\n", "<br>" );
        CompositeView view = new CompositeView();
        view.add( errorSymbol );
        ComplexTextView textView = new ComplexTextView( message, new ColorFont( new Font(Font.SANS_SERIF, 0, 12), Color.BLACK ), null, ComplexTextView.TEXT_ALIGN_LEFT, ApplicationUtils.getGraphics(), 500 );
        view.add( textView, CompositeView.X_RL | CompositeView.Y_TT, new Point(5, 0) );
        BufferedImage image = new BufferedImage(view.getBounds().width, view.getBounds().height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        view.paint( graphics );
        return image;
    }

}
