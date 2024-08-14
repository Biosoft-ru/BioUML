package biouml.plugins.biopax.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class BioSourceBeanInfo extends ConceptBeanInfo
{
    public BioSourceBeanInfo()
    {
        this(BioSource.class, "BIOSOURCE" );
    }

    protected BioSourceBeanInfo(Class beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        initResources("biouml.plugins.biopax.model.MessageBundle");
        
        int index = findPropertyIndex("comment");

        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("celltype", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "CT");
        add(index, pde,
                getResourceString("PN_BIOSOURCE_CELLTYPE"),
                getResourceString("PD_BIOSOURCE_CELLTYPE"));
        
        pde = new PropertyDescriptorEx("tissue", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TS");
        add(index+1, pde,
                getResourceString("PN_BIOSOURCE_TISSUE"),
                getResourceString("PD_BIOSOURCE_TISSUE"));
        
    }

}
