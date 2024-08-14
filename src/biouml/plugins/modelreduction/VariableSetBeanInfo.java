package biouml.plugins.modelreduction;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class VariableSetBeanInfo extends BeanInfoEx2<VariableSet>
{
    public VariableSetBeanInfo()
    {
        super(VariableSet.class);
        setHideChildren(true);
        setCompositeEditor("subdiagramName;variableNames", new java.awt.GridLayout(1, 2));
    }

    @Override
    public void initProperties() throws Exception
    {
        addWithTags("subdiagramName", bean -> bean.getAvailableModules());
        add("variableNames", VariableEditor.class);
    }

    public static class VariableEditor extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            return ((VariableSet)getBean()).getAvailableVariables().toArray(String[]::new);
        }
    }
}

