
package ru.biosoft.bsa.view;

import com.developmentontheedge.beans.editors.TagEditorSupport;

/**
 * Implements TagEditor for sequence view type.
 */
public class SequenceViewTypeEditor extends TagEditorSupport
{
    public SequenceViewTypeEditor()
    {
        super(SequenceViewOptions.class.getName() + "MessageBundle", SequenceViewOptions.class, "VIEW_TYPES", 0);
    }
}
