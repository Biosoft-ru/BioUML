package biouml.plugins.riboseq.db.editors;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import biouml.plugins.riboseq.db.model.SequenceData.Format;

public class FormatSelector extends GenericComboBoxEditor
{
    @Override
    protected Object[] getAvailableValues()
    {
        return Format.values();
    }
}