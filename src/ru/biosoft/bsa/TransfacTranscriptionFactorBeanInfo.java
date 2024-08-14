package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class TransfacTranscriptionFactorBeanInfo extends BeanInfoEx
{
    public TransfacTranscriptionFactorBeanInfo()
    {
        super(TransfacTranscriptionFactor.class, BSAMessageBundle.class.getName() );
        beanDescriptor.setDisplayName     (getResourceString("CN_FACTOR"));
        beanDescriptor.setShortDescription(getResourceString("CD_FACTOR"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("name",
                beanClass,"getName",null );
        HtmlPropertyInspector.setDisplayName(pde, "ID");
        add(pde,
            getResourceString("PN_FACTOR_NAME"),
            getResourceString("PD_FACTOR_NAME"));
        pde = new PropertyDescriptorEx("title", beanClass, "getDisplayName", null);
        pde.setDisplayName(getResourceString("PN_FACTOR_DISPLAY_NAME"));
        addHidden(pde);    // For pretty search results
        pde = new PropertyDescriptorEx("displayName",
                beanClass,"getDisplayName",null );
        HtmlPropertyInspector.setDisplayName(pde, "AC");
        add(pde,
            getResourceString("PN_FACTOR_DISPLAY_NAME"),
            getResourceString("PD_FACTOR_DISPLAY_NAME"));
        pde = new PropertyDescriptorEx("taxon",
                beanClass,"getTaxon",null );
        HtmlPropertyInspector.setDisplayName(pde, "OS");
        add(pde,
            getResourceString("PN_FACTOR_TAXON"),
            getResourceString("PD_FACTOR_TAXON"));
        pde = new PropertyDescriptorEx("generalClass",
                beanClass,"getGeneralClass",null );
        HtmlPropertyInspector.setDisplayName(pde, "CH");
        add(pde,
            getResourceString("PN_FACTOR_GENERAL_CLASS"),
            getResourceString("PD_FACTOR_GENERAL_CLASS"));
        pde = new PropertyDescriptorEx("bindingDomain",
                beanClass,"getDNABindingDomain",null );
        HtmlPropertyInspector.setDisplayName(pde, "CL");
        add(pde,
            getResourceString("PN_FACTOR_BINDING_DOMAIN"),
            getResourceString("PD_FACTOR_BINDING_DOMAIN"));
        add(new PropertyDescriptorEx("negativeTissueSpecificity",
                beanClass,"getNegativeTissueSpecificity",null ),
            getResourceString("PN_FACTOR_NEGATIVE_TISSUE_SPECIFICITY"),
            getResourceString("PD_FACTOR_NEGATIVE_TISSUE_SPECIFICITY"));
        add(new PropertyDescriptorEx("positiveTissueSpecificity",
                beanClass,"getPositiveTissueSpecificity",null ),
            getResourceString("PN_FACTOR_POSITIVE_TISSUE_SPECIFICITY"),
            getResourceString("PD_FACTOR_POSITIVE_TISSUE_SPECIFICITY"));
        add(new PropertyDescriptorEx("references",
                beanClass,"getDatabaseReferences",null ),
            getResourceString("PN_FACTOR_REFERENCES"),
            getResourceString("PD_FACTOR_REFERENCES"));
    }
}
