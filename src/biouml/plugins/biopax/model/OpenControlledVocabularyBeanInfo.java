package biouml.plugins.biopax.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class OpenControlledVocabularyBeanInfo extends ConceptBeanInfo
{
    public OpenControlledVocabularyBeanInfo()
    {
        this(OpenControlledVocabulary.class, "OPENCONTROLLEDVOCABULARY" );
    }

    protected OpenControlledVocabularyBeanInfo(Class beanClass, String key)
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
        
        pde = new PropertyDescriptorEx("term", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TM");
        add(index, pde,
                getResourceString("PN_OPENCONTROLLEDVOCABULARY_TERM"),
                getResourceString("PD_OPENCONTROLLEDVOCABULARY_TERM"));
        
        pde = new PropertyDescriptorEx("vocabularyType", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "VT");
        add(index+1, pde,
                getResourceString("PN_OPENCONTROLLEDVOCABULARY_TYPE"),
                getResourceString("PD_OPENCONTROLLEDVOCABULARY_TYPE"));
    }

}
