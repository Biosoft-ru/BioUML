package biouml.plugins.psimi.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.plugins.biopax.model.Confidence;
import biouml.standard.type.ConceptBeanInfo;

public class ConfidenceBeanInfo extends ConceptBeanInfo
{
    public ConfidenceBeanInfo()
    {
        this(Confidence.class, "CONFIDENCE" );
    }

    protected ConfidenceBeanInfo(Class<? extends Confidence> beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        initResources("biouml.plugins.psimi.MessageBundle");
        
        int index = findPropertyIndex("comment");

        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("unit", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "UN");
        add(index, pde,
                getResourceString("PN_CONFIDENCE_UNIT"),
                getResourceString("PD_CONFIDENCE_UNIT"));
        
        pde = new PropertyDescriptorEx("value", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "VA");
        add(index, pde,
                getResourceString("PN_CONFIDENCE_VALUE"),
                getResourceString("PD_CONFIDENCE_VALUE"));
        
        pde = new PropertyDescriptorEx("experimentRefList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "RL");
        add(index+1, pde,
                getResourceString("PN_CONFIDENCE_REFLIST"),
                getResourceString("PD_CONFIDENCE_REFLIST"));
        
    }

}
