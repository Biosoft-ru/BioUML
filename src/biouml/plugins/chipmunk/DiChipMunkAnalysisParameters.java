package biouml.plugins.chipmunk;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class DiChipMunkAnalysisParameters extends AbstractChipMunkParameters
{
    private boolean localBackground;

    @PropertyName("Local background")
    @PropertyDescription("If checked, local background estimation is used. Otherwise uniform background estimation is used")
    public boolean isLocalBackground()
    {
        return localBackground;
    }

    public void setLocalBackground(boolean localBackground)
    {
        Object oldValue = this.localBackground;
        this.localBackground = localBackground;
        firePropertyChange("localBackground", oldValue, localBackground);
    }
    
    
}
