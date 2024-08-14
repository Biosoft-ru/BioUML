package biouml.standard.state;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class StateBeanInfo extends BeanInfoEx
{
    public StateBeanInfo()
    {
        this(State.class, "STATE", "biouml.standard.type.MessageBundle");
    }

    protected StateBeanInfo(Class beanClass, String key, String messageBundle)
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
        super.initProperties();

        initResources("biouml.standard.state.MessageBundle");

        PropertyDescriptorEx pde;

        pde = new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null);
        HtmlPropertyInspector.setDisplayName(pde, "ID");
        add(pde, getResourceString("PN_IDENTIFIER"), getResourceString("PD_IDENTIFIER"));

        pde = new PropertyDescriptorEx("title", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TI");
        add(pde, getResourceString("PN_TITLE"), getResourceString("PD_TITLE"));

        pde = new PropertyDescriptorEx("description", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "DE");
        add(pde, getResourceString("PN_DESCRIPTION"), getResourceString("PD_DESCRIPTION"));

        pde = new PropertyDescriptorEx("version", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "VS");
        add(pde, getResourceString("PN_STATE_VERSION"), getResourceString("PD_STATE_VERSION"));
    }
}