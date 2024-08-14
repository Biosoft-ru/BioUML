package biouml.standard.filter;

import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ShapeView;
import ru.biosoft.graphics.View;

public class InnerShapeView
{
    private final ShapeView view;
    private final double area;
    private InnerShapeView(ShapeView view, double area)
    {
        this.view = view;
        this.area = area;
    }

    public ShapeView getShapeView()
    {
        return view;
    }

    //TODO: if we have 2 or more shapes with same area, we should highlight all of them
    static InnerShapeView findBiggestInnerShape(CompositeView container, int depth)
    {
        InnerShapeView result = null;
        for( View childView : container )
        {
            if( childView instanceof ShapeView )
            {
                double area = childView.getBounds().getWidth() * childView.getBounds().getHeight();
                if( result == null || area > result.area )
                    result = new InnerShapeView( (ShapeView)childView, area );
            }
            else if( depth != 0 && childView instanceof CompositeView )
            {
                int nextDepth = depth > 0 ? depth - 1 : depth;
                InnerShapeView innerResult = findBiggestInnerShape( (CompositeView)childView, nextDepth );
                if( innerResult == null )
                    continue;
                if( result == null || innerResult.area > result.area )
                    result = innerResult;
            }
        }
        return result;
    }
}
