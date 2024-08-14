package biouml.plugins.biopax.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class SequenceSiteBeanInfo extends ConceptBeanInfo
{
    public SequenceSiteBeanInfo()
    {
        this(SequenceSite.class, "SEQUENCESITE" );
    }

    protected SequenceSiteBeanInfo(Class beanClass, String key)
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
        pde = new PropertyDescriptorEx("positionStatus", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PS");
        add(index, pde,
                getResourceString("PN_SEQUENCESITE_POSITIONSTATUS"),
                getResourceString("PD_SEQUENCESITE_POSITIONSTATUS"));
        
        pde = new PropertyDescriptorEx("sequencePosition", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "SP");
        add(index+1, pde,
                getResourceString("PN_SEQUENCESITE_SEQUENCEPOSITION"),
                getResourceString("PD_SEQUENCESITE_SEQUENCEPOSITION"));
        
    }

}
