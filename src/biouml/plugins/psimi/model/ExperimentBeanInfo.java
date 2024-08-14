package biouml.plugins.psimi.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class ExperimentBeanInfo extends ConceptBeanInfo
{
    public ExperimentBeanInfo()
    {
        this(Experiment.class, "EXPERIMENT");
    }

    protected ExperimentBeanInfo(Class<? extends Experiment> beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        initResources("biouml.plugins.psimi.MessageBundle");

        PropertyDescriptorEx pde;
        
        pde = new PropertyDescriptorEx("hostOrganismList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "HOL");
        add(pde,
                getResourceString("PN_HOSTORGANISM_LIST"),
                getResourceString("PD_HOSTORGANISM_LIST"));
        
        pde = new PropertyDescriptorEx("interactionDetectionMethod", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IDM");
        add(pde,
                getResourceString("PN_ID_METHOD"),
                getResourceString("PD_ID_METHOD"));
        
        pde = new PropertyDescriptorEx("participantIdentificationMethod", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PIM");
        add(pde,
                getResourceString("PN_PI_METHOD"),
                getResourceString("PD_PI_METHOD"));
        
        pde = new PropertyDescriptorEx("featureDetectionMethod", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "FDM");
        add(pde,
                getResourceString("PN_FD_METHOD"),
                getResourceString("PD_FD_METHOD"));
        
        pde = new PropertyDescriptorEx("confidenceList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "COL");
        add(pde,
                getResourceString("PN_CONFIDENCE_LIST"),
                getResourceString("PD_CONFIDENCE_LIST"));
    }
}
