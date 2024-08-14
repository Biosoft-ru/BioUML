package biouml.model;

import com.developmentontheedge.beans.editors.TagEditorSupport;

/**
 * DiagramFilterActionEditor allows a user select <code>Site</code>
 * basis from the combobox with the
 * predefined values. This values are defined in
 * <code>SiteMessagesBundle</code> with key "BASIS_TYPES".
 */
public class DiagramFilterActionEditor extends TagEditorSupport
{
    public DiagramFilterActionEditor()
    {
        super("biouml.model.MessageBundle", DiagramFilterActionEditor.class, "FILTER_ACTIONS", 0);
    }
}
