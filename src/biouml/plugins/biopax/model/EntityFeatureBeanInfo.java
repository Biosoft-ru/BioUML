package biouml.plugins.biopax.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class EntityFeatureBeanInfo extends ConceptBeanInfo
{
    public EntityFeatureBeanInfo()
    {
        this(SequenceFeature.class, "ENTITYFEATURE" );
    }

    protected EntityFeatureBeanInfo(Class beanClass, String key)
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
        pde = new PropertyDescriptorEx("featureLocation", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "LC");
        add(index, pde,
                getResourceString("PN_ENTITYFEATURE_LOCATION"),
                getResourceString("PD_ENTITYFEATURE_LOCATION"));
        
        pde = new PropertyDescriptorEx("featureType", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TP");
        add(index+1, pde,
                getResourceString("PN_ENTITYFEATURE_TYPE"),
                getResourceString("PD_ENTITYFEATURE_TYPE"));
        
    }

}
