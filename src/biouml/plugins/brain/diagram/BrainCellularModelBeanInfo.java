package biouml.plugins.brain.diagram;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class BrainCellularModelBeanInfo extends BeanInfoEx2<BrainCellularModel>
{
    public BrainCellularModelBeanInfo()
    {
        super(BrainCellularModel.class);
    }

    @Override
    protected void initProperties() throws Exception
    {		
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass);
        pde.setReadOnly(beanClass.getMethod("isCreated"));
        add(pde);
        
        add("comment");
        
        addWithTags("cellularModelType", BrainCellularModel.availableCellularModels);
        add("cellularModelProperties");
    }
}