package biouml.model;

import ru.biosoft.access.support.SetAttributesCommand;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class DiagramElementBeanInfo extends BeanInfoEx2<DiagramElement>
{
    protected DiagramElementBeanInfo(Class<? extends DiagramElement> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("title");
        add("comment");
        addHidden("role", "isRoleHidden");
        add("kernel");
        addWithTags("predefinedStyle", bean -> bean.getAvailableStyles());
        addHidden("customStyle", "isStylePredefined");
        property(new PropertyDescriptorEx("attributes", beanClass.getMethod("getAttributes"), null)).hidden("hasNoAttributes")
                .value("commandClass", SetAttributesCommand.class).add();
    }
}
