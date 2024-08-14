package ru.biosoft.bsa;

import com.developmentontheedge.beans.editors.TagEditorSupport;

/**
 * BasisEditor allows a user select <code>Site</code>
 * basis from the combobox with the
 * predefined values. This values are defined in
 * <code>MessageBundle</code> with key "BASIS_TYPES".
 */
public class BasisEditor extends TagEditorSupport
{
    public BasisEditor()
    {
        super(MessageBundle.class.getName(), MessageBundle.class, "BASIS_TYPES", 0);
    }
}
