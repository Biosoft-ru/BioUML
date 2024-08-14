package biouml.plugins.psimi.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class InteractorBeanInfo extends ConceptBeanInfo
{
    public InteractorBeanInfo()
    {
        this(Interactor.class, "INTERACTOR");
    }

    protected InteractorBeanInfo(Class<? extends Interactor> beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        initResources("biouml.plugins.psimi.MessageBundle");

        PropertyDescriptorEx pde;
        
        pde = new PropertyDescriptorEx("interactorType", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "INT");
        add(pde,
                getResourceString("PN_INTERACTOR_TYPE"),
                getResourceString("PD_INTERACTOR_TYPE"));
        
        pde = new PropertyDescriptorEx("organism", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "ORG");
        add(pde,
                getResourceString("PN_INTERACTOR_ORGANISM"),
                getResourceString("PD_INTERACTOR_ORGANISM"));
        
        pde = new PropertyDescriptorEx("sequence", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "INS");
        add(pde,
                getResourceString("PN_INTERACTOR_SEQUENCE"),
                getResourceString("PD_INTERACTOR_SEQUENCE"));
    }
}
