package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class EModelBeanInfo extends BeanInfoEx2<EModel>
{
    public EModelBeanInfo(Class<? extends EModel> beanClass)
    {
        super(beanClass);
    }
    
    public EModelBeanInfo()
    {
        super(EModel.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("comment");
        addHidden(new PropertyDescriptorEx("vars", beanClass, "getVars", null));
    }
}
