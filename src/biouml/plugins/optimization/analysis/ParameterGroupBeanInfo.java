package biouml.plugins.optimization.analysis;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.optimization.analysis.ParameterGroup.EstimatedParameter;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ParameterGroupBeanInfo extends BeanInfoEx2<ParameterGroup>
{
    public ParameterGroupBeanInfo()
    {
        super(ParameterGroup.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("parameters", beanClass);
        pde.setChildDisplayName(beanClass.getMethod("calcParameterName", new Class[] {Integer.class, Object.class}));
        pde.setPropertyEditorClass(ParametersEditor.class);
        add(pde);
    }
    
    public static class ParametersEditor extends GenericMultiSelectEditor
    {
        @Override
        protected EstimatedParameter[] getAvailableValues()
        {
            return ( (ParameterGroup)getBean() ).getAvailableParameters();
        }
    }
}