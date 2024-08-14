package biouml.workbench.diagram;

import com.developmentontheedge.beans.Option;

import ru.biosoft.graphics.editor.ViewEditorHelper;

/**
 * Base properties for all diagram-related dynamic actions
 * @author lan
 */
public class DiagramDynamicActionProperties extends Option
{
    private ViewEditorHelper helper;

    public void setHelper(ViewEditorHelper helper)
    {
        Object oldValue = this.helper;
        this.helper = helper;
        firePropertyChange("helper", oldValue, helper);
    }

    public ViewEditorHelper getHelper()
    {
        return helper;
    }
}
