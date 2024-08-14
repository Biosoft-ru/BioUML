
package ru.biosoft.bsa.view;

import com.developmentontheedge.beans.editors.TagEditorSupport;

/**
 * BasisEditor allows a user select <code>Site</code>
 * basis from the combobox with the
 * predefined values. This values are defined in
 * <code>SiteMessagesBundle</code> with key "BASIS_TYPES".
 */
public class SiteDisplayNameEditor extends TagEditorSupport
{
    public SiteDisplayNameEditor()
    {
        super(SiteViewOptionsMessageBundle.class.getName(), SiteViewOptionsMessageBundle.class, "SITE_DISPLAY_NAME_TYPES", 0);
    }
}
