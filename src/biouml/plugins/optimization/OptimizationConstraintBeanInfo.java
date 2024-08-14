package biouml.plugins.optimization;

import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.BeanInfoEx;

import biouml.plugins.optimization.document.editors.SubdiagramsEditor;

public class OptimizationConstraintBeanInfo extends BeanInfoEx
{
    public OptimizationConstraintBeanInfo()
    {
        super(OptimizationConstraint.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        property("formula").add();
        property("subdiagramPath").hidden("isSubdiagramPathHidden").editor(SubdiagramsEditor.class).add();
        property("initialTime").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        property("completionTime").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        property("experiments").editor(ExperimentsEditor.class).add();
        property("description").add();
    }

    public static class ExperimentsEditor extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ( (OptimizationConstraint)getBean() ).getAvailableExperiments();
        }
    }
}