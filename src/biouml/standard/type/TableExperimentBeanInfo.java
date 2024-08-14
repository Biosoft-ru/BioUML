package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;


public class TableExperimentBeanInfo extends ReferrerBeanInfo<TableExperiment>
{
    public TableExperimentBeanInfo()
    {
        super(TableExperiment.class, "MICROARRAY" );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        // reinit message bundle
        initResources("biouml.plugins.microarray.MessageBundle");

        PropertyDescriptorEx pde = new PropertyDescriptorEx("platform", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PL");
        add(2, pde,
            getResourceString("PN_MA_PLATFORM"),
            getResourceString("PD_MA_PLATFORM"));
        
        pde = new PropertyDescriptorEx("species", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PL");
        add(3, pde,
            getResourceString("PN_MA_SPECIES"),
            getResourceString("PD_MA_SPECIES"));
    }
}
