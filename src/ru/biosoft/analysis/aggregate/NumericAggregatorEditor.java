package ru.biosoft.analysis.aggregate;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class NumericAggregatorEditor extends GenericComboBoxEditor
{
    @Override
    public Object[] getAvailableValues()
    {
        return NumericAggregator.getAggregators();
    }
}
