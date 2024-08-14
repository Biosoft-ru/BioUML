package biouml.plugins.psimi.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class OrganismBeanInfo extends ConceptBeanInfo
{
    public OrganismBeanInfo()
    {
        this(Organism.class, "ORGANISM" );
    }

    protected OrganismBeanInfo(Class<? extends Organism> beanClass, String key)
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
        pde = new PropertyDescriptorEx("celltype", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "CT");
        add(index, pde,
                getResourceString("PN_ORGANISM_CELLTYPE"),
                getResourceString("PD_ORGANISM_CELLTYPE"));
        
        pde = new PropertyDescriptorEx("compartment", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "CM");
        add(index, pde,
                getResourceString("PN_ORGANISM_COMPARTMENT"),
                getResourceString("PD_ORGANISM_COMPARTMENT"));
        
        pde = new PropertyDescriptorEx("tissue", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TS");
        add(index+1, pde,
                getResourceString("PN_ORGANISM_TISSUE"),
                getResourceString("PD_ORGANISM_TISSUE"));
        
    }

}
