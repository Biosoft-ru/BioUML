package biouml.plugins.brain.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class BrainReceptorModelBeanInfo extends BeanInfoEx2<BrainReceptorModel>
{
    public BrainReceptorModelBeanInfo()
    {
        super(BrainReceptorModel.class);
    }

    @Override
    protected void initProperties() throws Exception
    {		
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass);
        pde.setReadOnly(beanClass.getMethod("isCreated"));
        add(pde);
        
        add("comment");
        
        addWithTags("receptorModelType", BrainReceptorModel.availableReceptorModels);
        add("receptorModelProperties");
    }
}