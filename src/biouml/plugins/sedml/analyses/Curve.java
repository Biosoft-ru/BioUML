package biouml.plugins.sedml.analyses;

import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.Option;

@PropertyName("Curve")
@PropertyDescription("Curve.")
public class Curve extends Option implements JSONBean
{
    private String title;
    private boolean logX = false;
    private boolean logY = false;
    
    private String expressionX;
    private String expressionY;


    @PropertyName("Title")
    @PropertyDescription("Title.")
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange( "title", oldValue, title );
    }
    
    @PropertyName("log X")
    @PropertyDescription("Is log X.")
    public boolean isLogX()
    {
        return logX;
    }
    public void setLogX(boolean log)
    {
        boolean oldValue = this.logX;
        this.logX = log;
        firePropertyChange( "logX", oldValue, logX );
    }
    
    @PropertyName("log Y")
    @PropertyDescription("Is log Y.")
    public boolean isLogY()
    {
        return logY;
    }
    public void setLogY(boolean log)
    {
        boolean oldValue = this.logY;
        this.logY = log;
        firePropertyChange( "logY", oldValue, logY );
    }

    public String getExpressionX()
    {
        return expressionX;
    }

    public void setExpressionX(String expressionX)
    {
        String oldValue = this.expressionX;
        this.expressionX = expressionX;
        firePropertyChange( "expressionX", oldValue, expressionX );
    }

    public String getExpressionY()
    {
        return expressionY;
    }

    public void setExpressionY(String expressionY)
    {
        String oldValue = this.expressionY;
        this.expressionY = expressionY;
        firePropertyChange( "expressionY", oldValue, expressionY );
    }

    
}
