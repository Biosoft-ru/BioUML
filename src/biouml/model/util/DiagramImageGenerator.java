package biouml.model.util;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.model.DiagramViewBuilder;
import ru.biosoft.graphics.View;
import ru.biosoft.util.ImageGenerator;

public class DiagramImageGenerator extends ImageGenerator
{
    public static BufferedImage generateDiagramImage(Diagram diagram)
    {
        return generateDiagramImage( diagram, 1, false );
    }

    public static BufferedImage generateDiagramImage(Diagram diagram, double scale, boolean antiAliasing)
    {
        Graphics graphics = ApplicationUtils.getGraphics();

        View view = generateDiagramView( diagram, graphics );
        BufferedImage image = generateImage( view, scale, antiAliasing );

        return image;
    }

    public static View generateDiagramView(Diagram diagram, Graphics graphics)
    {
        View view = diagram.getView();
        if( view == null ) // when diagram is not open
        {
            DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
            view = builder.createDiagramView( diagram, graphics );
            //DiagramFilter filter = diagram.getFilter();
            DiagramFilter[] filterList = diagram.getFilterList();
            for ( DiagramFilter filter : filterList )
            {
                if( filter != null && filter.isEnabled() )
                    filter.apply( diagram );
            }
        }

        // to display diagram with the offset
        view.setLocation( 10, 10 );
        return view;
    }

}
