package biouml.plugins.expression;

import com.developmentontheedge.beans.annot.PropertyName;

public class FluxProperties extends InsideFillProperties
{
    private int maxWidth = 10;

    @PropertyName("Maximum stroke width")
    public int getMaxWidth()
    {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth)
    {
        Object oldValue = this.maxWidth;
        this.maxWidth = maxWidth;
        firePropertyChange( "maxWidth", oldValue, this.maxWidth );
    }

    @Override
    protected void correctMinMax()
    {
        // do not correct for flux edges
    }
}
