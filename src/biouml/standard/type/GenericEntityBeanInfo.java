package biouml.standard.type;

import ru.biosoft.access.support.SetAttributesCommand;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class GenericEntityBeanInfo<T extends GenericEntity> extends BeanInfoEx2<T>
{
    protected GenericEntityBeanInfo(Class<? extends T> ge)
    {
        super(ge);
    }

    protected GenericEntityBeanInfo(Class beanClass, String key)
    {
        this(beanClass, key, MessageBundle.class.getName());
    }

    /**
     * Note: key can be null. In this case subclass can set up necessary
     * MessageBundle and then initialise beanDescriptor properly.
     */
    protected GenericEntityBeanInfo(Class beanClass, String key, String messageBundle)
    {
        super(beanClass, messageBundle);

        if( key != null && messageBundle != null )
        {
            beanDescriptor.setDisplayName(getResourceString("CN_" + key));
            beanDescriptor.setShortDescription(getResourceString("CD_" + key));
        }
    }

    @Override
    public void initProperties() throws Exception
    {
        property(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null)).htmlDisplayName("ID").add();
        property(new PropertyDescriptorEx("type", beanClass.getMethod("getType"), null)).htmlDisplayName("TY").expert().add();
        property("date").expert().htmlDisplayName("DT").add();
        property("title").expert().htmlDisplayName("TI").add();
        property("comment").expert().htmlDisplayName("CC").add();
        property(new PropertyDescriptorEx("attributes", beanClass.getMethod("getAttributes"), null)).expert()
                .value("commandClass", SetAttributesCommand.class).hidden("hasNoAttributes").htmlDisplayName("AT").add();
    }
}
