package ru.biosoft.table;

import ru.biosoft.access.core.MessageBundle;
import com.developmentontheedge.beans.BeanInfoEx;

public class RowDataElementBeanInfo extends BeanInfoEx
{
    protected RowDataElementBeanInfo(Class c, String messageBundle)
    {
        super(c, messageBundle );
    }

    public RowDataElementBeanInfo()
    {
        super(RowDataElement.class, MessageBundle.class.getName() );
        
        initResources( "ru.biosoft.access.core.MessageBundle" );
        
        beanDescriptor.setDisplayName( "Data Collection" );
        beanDescriptor.setShortDescription( "Data Collection" );
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources( "ru.biosoft.access.core.MessageBundle" );
        
//        add(new PropertyDescriptorEx("name", beanClass, "getName", null),
//                "ID",
//                "ID");
    }
}
