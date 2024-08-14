package biouml.standard.filter;

import com.developmentontheedge.beans.Option;

/**
 * Returns some action
 */
public class ValueAction extends Option
{
    public ValueAction(Option parent, String value)
    {
        super(parent);
        this.value = value;
    }

    public ValueAction(String value)
    {
        this.value = value;
    }

    private final String value;
    public String getValue()
    {
        return value;
    }

    private Action action = HideAction.instance;
    public Action getAction()
    {
        return action;
    }
    public void setAction(Action action)
    {
        Action oldValue = this.action;
        this.action = action;
        firePropertyChange("action", oldValue, action);
    }

    private boolean enabled = false;
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }
}


