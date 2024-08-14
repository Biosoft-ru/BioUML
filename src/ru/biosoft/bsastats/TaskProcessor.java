package ru.biosoft.bsastats;

import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public abstract class TaskProcessor extends OptionEx implements JSONBean
{
    private boolean enabled = true;
    
    @PropertyName("Enabled")
    @PropertyDescription("Wheter processor is enabled")
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange( "enabled", oldValue, enabled );
    }
    
    public abstract Task process(Task task);
    
    public void finalizeProcessing() throws Exception {}
}
