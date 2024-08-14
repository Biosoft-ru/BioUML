package biouml.plugins.biopax.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class EvidenceBeanInfo extends ConceptBeanInfo
{
    public EvidenceBeanInfo()
    {
        this(Evidence.class, "EVIDENCE" );
    }

    protected EvidenceBeanInfo(Class beanClass, String key)
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
        pde = new PropertyDescriptorEx("confidence", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "CD");
        add(index, pde,
                getResourceString("PN_EVIDENCE_CONFIDENCE"),
                getResourceString("PD_EVIDENCE_CONFIDENCE"));
        
        pde = new PropertyDescriptorEx("evidenceCode", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "EC");
        add(index+1, pde,
                getResourceString("PN_EVIDENCE_CODE"),
                getResourceString("PD_EVIDENCE_CODE"));
    }

}
