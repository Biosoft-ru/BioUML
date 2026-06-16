package ru.biosoft.bsa.track.big;

import java.awt.Color;

import ru.biosoft.bsa.view.SiteViewOptions;

public class BigWigViewOptions extends SiteViewOptions
{
    private boolean autoScale = true;
    private float scale = 1;
    private boolean showValuesRange = true;
    
    private Color color = Color.BLUE;
    
    public boolean isAutoScale()
    {
        return autoScale;
    }
    public void setAutoScale(boolean autoScale)
    {
        boolean oldValue = this.autoScale;
        this.autoScale = autoScale;
        firePropertyChange( "autoScale", oldValue, autoScale );
    }
    
    public float getScale()
    {
        return scale;
    }
    public void setScale(float scale)
    {
        float oldValue = this.scale;
        this.scale = scale;
        firePropertyChange( "scale", oldValue, scale );
    }
    
    public boolean isShowValuesRange()
    {
        return showValuesRange;
    }
    public void setShowValuesRange(boolean showValuesRange)
    {
        boolean oldValue = this.showValuesRange;
        this.showValuesRange = showValuesRange;
        firePropertyChange( "showValuesRange", oldValue, showValuesRange );
    }
    
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
    
    
}
