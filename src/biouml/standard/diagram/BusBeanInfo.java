package biouml.standard.diagram;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class BusBeanInfo extends BeanInfoEx2<Bus>
{
    public BusBeanInfo()
    {
        super( Bus.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null)).add();        
        add("color");
    }
}