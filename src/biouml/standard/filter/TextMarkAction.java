package biouml.standard.filter;

import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import biouml.model.DiagramElement;

/**
 * @author lan
 *
 */
public class TextMarkAction implements Action
{
    private String mark;
    private String description;
    
    public TextMarkAction(String mark, String description)
    {
        this.mark = mark;
        this.description = description;
    }
    
    private static class AppliedException extends Exception
    {
    }
    
    private void applyToView(View view) throws AppliedException
    {
        if(view instanceof TextView)
        {
            //TODO: ((TextView)view).setText(((TextView)view).getText()+this.mark);
            throw new AppliedException();
        } else if(view instanceof CompositeView)
        {
            for(View child: (CompositeView)view)
            {
                applyToView(child);
            }
        }
    }

    @Override
    public void apply(DiagramElement de)
    {
        View view = de.getView();
        try
        {
            applyToView(view);
        }
        catch( AppliedException e )
        {
        }
        if(description != null)
            view.setDescription(view.getDescription()==null?description:view.getDescription()+": "+description);
    }

}
