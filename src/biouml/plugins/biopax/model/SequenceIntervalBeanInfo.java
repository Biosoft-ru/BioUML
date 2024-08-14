package biouml.plugins.biopax.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class SequenceIntervalBeanInfo extends ConceptBeanInfo
{
    public SequenceIntervalBeanInfo()
    {
        this(SequenceInterval.class, "SEQUENCEINTERVAL" );
    }

    protected SequenceIntervalBeanInfo(Class beanClass, String key)
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
        pde = new PropertyDescriptorEx("begin", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "BG");
        add(index, pde,
                getResourceString("PN_SEQUENCEINTERVAL_BEGIN"),
                getResourceString("PD_SEQUENCEINTERVAL_BEGIN"));
        
        pde = new PropertyDescriptorEx("end", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "EN");
        add(index+1, pde,
                getResourceString("PN_SEQUENCEINTERVAL_END"),
                getResourceString("PD_SEQUENCEINTERVAL_END"));
        
    }

}
