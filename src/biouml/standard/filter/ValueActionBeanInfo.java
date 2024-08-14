package biouml.standard.filter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.GridLayout2;

public class ValueActionBeanInfo extends BeanInfoEx
{
    /**
     * We not set up DisplayName while it will be redefined by parent.
     *
     * @pending #1 how to set up short description
     */
    public ValueActionBeanInfo()
    {
        super(ValueAction.class, MessageBundle.class.getName());

        beanDescriptor.setShortDescription(""); // #1
        setCompositeEditor("enabled;action", new GridLayout2());
    }

    @Override
    protected void initProperties() throws Exception
    {
        addHidden(new PropertyDescriptorEx("enabled", beanClass));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("action", beanClass);
        pde.setPropertyEditorClass(ActionEditor.class);
        addHidden(pde);
    }
}


