package biouml.plugins.research.workflow.items;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class VariableTypeEditor extends GenericComboBoxEditor
{
    @Override
    protected Object[] getAvailableValues()
    {
        return VariableType.VARIABLE_TYPES;
    }
}