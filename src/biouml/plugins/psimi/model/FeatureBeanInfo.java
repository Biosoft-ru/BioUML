package biouml.plugins.psimi.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class FeatureBeanInfo extends ConceptBeanInfo
{
    public FeatureBeanInfo()
    {
        this(Feature.class, "FEATURE");
    }

    protected FeatureBeanInfo(Class<? extends Feature> beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        initResources("biouml.plugins.psimi.MessageBundle");

        PropertyDescriptorEx pde;
        
        pde = new PropertyDescriptorEx("featureType", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "FT");
        add(pde,
                getResourceString("PN_FEATURE_FTYPE"),
                getResourceString("PD_FEATURE_FTYPE"));
        
        pde = new PropertyDescriptorEx("featureDetectionMethod", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "FDM");
        add(pde,
                getResourceString("PN_FEATURE_FDMETHOD"),
                getResourceString("PD_FEATURE_FDMETHOD"));
        
        pde = new PropertyDescriptorEx("experimentRefList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "ERL");
        add(pde,
                getResourceString("PN_FEATURE_ERLIST"),
                getResourceString("PD_FEATURE_ERLIST"));
        
        pde = new PropertyDescriptorEx("featureRangeList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "FRL");
        add(pde,
                getResourceString("PN_FEATURE_FRLIST"),
                getResourceString("PD_FEATURE_FRLIST"));
    }
}
