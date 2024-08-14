package biouml.standard.filter;

import java.awt.Color;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ShapeView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import biouml.model.DiagramElement;

/**
 * Highlight DiagramElement internals
 * This will find the biggest shape and substitute its brush
 * @author lan
 */
public class InteriorHighlightAction implements Action
{
    private Brush brush;
    private String description;
    
    /**
     * @param brush - brush to highlight with
     * @param description - description for highlighting if applicable
     */
    public InteriorHighlightAction(Brush brush, String description)
    {
        this.brush = brush;
        this.description = description;
    }

    /**
     * @param brush - brush to highlight with
     */
    public InteriorHighlightAction(Brush brush)
    {
        this(brush, null);
    }

    @Override
    public void apply(DiagramElement de)
    {
        View view = de.getView();
        if( view instanceof ShapeView )
        {
            ( (ShapeView)view ).setBrush( brush );
        }
        else if( view instanceof CompositeView )
        {
            ShapeView biggestView = null;
            InnerShapeView innerShape = InnerShapeView.findBiggestInnerShape( (CompositeView)view, -1 );
            if( innerShape != null )
                biggestView = innerShape.getShapeView();

            TextView textView = null;
            for(View childView: (CompositeView)view)
            {
                if(childView instanceof TextView)
                {
                    textView = (TextView)childView;
                }
            }

            if(biggestView != null)
            {
                biggestView.setBrush(brush);
            } else if(textView != null && brush.getPaint() instanceof Color)
            {
                //TODO: textView.setColor((Color)brush.getPaint());
            }
        }
        if(description != null)
            view.setDescription(view.getDescription()==null?description:view.getDescription()+": "+description);
    }
}
