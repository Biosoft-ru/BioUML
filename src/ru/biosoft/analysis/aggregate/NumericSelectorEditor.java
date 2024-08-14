package ru.biosoft.analysis.aggregate;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class NumericSelectorEditor extends GenericComboBoxEditor
{
    @Override
    public Object[] getAvailableValues()
    {
        return NumericSelector.getSelectors();
    }
}
