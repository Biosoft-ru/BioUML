package biouml.plugins.biopax.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

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

        initResources("biouml.plugins.biopax.model.MessageBundle");
        
        int index = findPropertyIndex("comment");

        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("confidenceValue", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "CV");
        add(index, pde,
                getResourceString("PN_CONFIDENCE_VALUE"),
                getResourceString("PD_CONFIDENCE_VALUE"));
    }

}
