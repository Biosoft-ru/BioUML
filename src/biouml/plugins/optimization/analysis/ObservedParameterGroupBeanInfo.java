package biouml.plugins.optimization.analysis;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.optimization.analysis.ObservedParameterGroup.ObservedParameter;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ObservedParameterGroupBeanInfo extends BeanInfoEx2<ObservedParameterGroup>
{
    public ObservedParameterGroupBeanInfo()
    {
        super(ObservedParameterGroup.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("parameters", beanClass);
        pde.setChildDisplayName(beanClass.getMethod("calcParameterName", new Class[] {Integer.class, Object.class}));
        pde.setPropertyEditorClass(ObservedParametersEditor.class);
        add(pde);
    }
    
    public static class ObservedParametersEditor extends GenericMultiSelectEditor
    {
        @Override
        protected ObservedParameter[] getAvailableValues()
        {
            return ( (ObservedParameterGroup)getBean() ).getAvailableParameters();
        }
    }
}