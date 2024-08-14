package biouml.plugins.brain.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class BrainRegionalModelBeanInfo extends BeanInfoEx2<BrainRegionalModel>
{
    public BrainRegionalModelBeanInfo()
    {
        super(BrainRegionalModel.class);
    }

    @Override
    protected void initProperties() throws Exception
    {		
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass);
        pde.setReadOnly(beanClass.getMethod("isCreated"));
        add(pde);
        
        add("comment");
        
        addWithTags("regionalModelType", BrainRegionalModel.availableRegionalModels);
        add("regionalModelProperties");
        
    }
}