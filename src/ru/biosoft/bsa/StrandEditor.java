package ru.biosoft.bsa;

import com.developmentontheedge.beans.editors.TagEditorSupport;

/**
 * StrandTypeEditor allows a user select <code>Site</code>
 * strand property from the combobox with the
 * predefined values. This values are defined in
 * <code>MessageBundle</code> with key "STRAND_TYPES".
 */
public class StrandEditor extends TagEditorSupport
{
    public StrandEditor()
    {
        super(MessageBundle.class.getName(), MessageBundle.class, "STRAND_TYPES", 0);
    }
}
