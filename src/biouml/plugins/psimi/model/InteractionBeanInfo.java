package biouml.plugins.psimi.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class InteractionBeanInfo extends ConceptBeanInfo
{
    public InteractionBeanInfo()
    {
        this(Interaction.class, "INTERACTION");
    }

    protected InteractionBeanInfo(Class<? extends Interaction> beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        initResources("biouml.plugins.psimi.MessageBundle");

        PropertyDescriptorEx pde;
        
        pde = new PropertyDescriptorEx("availabilityRef", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "AR");
        add(pde,
                getResourceString("PN_INTERACTION_AR"),
                getResourceString("PD_INTERACTION_AR"));
        
        pde = new PropertyDescriptorEx("experimentList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "EXL");
        add(pde,
                getResourceString("PN_INTERACTION_EXLIST"),
                getResourceString("PD_INTERACTION_EXLIST"));
        
        pde = new PropertyDescriptorEx("interactionType", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "INT");
        add(pde,
                getResourceString("PN_INTERACTION_TYPE"),
                getResourceString("PD_INTERACTION_TYPE"));
        
        pde = new PropertyDescriptorEx("modelled", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "MD");
        add(pde,
                getResourceString("PN_INTERACTION_MODELLED"),
                getResourceString("PD_INTERACTION_MODELLED"));
        
        pde = new PropertyDescriptorEx("intraMolecular", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IM");
        add(pde,
                getResourceString("PN_INTERACTION_IM"),
                getResourceString("PD_INTERACTION_IM"));
        
        pde = new PropertyDescriptorEx("negative", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "NG");
        add(pde,
                getResourceString("PN_INTERACTION_NEGATIVE"),
                getResourceString("PD_INTERACTION_NEGATIVE"));
        
        pde = new PropertyDescriptorEx("confidenceList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "CFL");
        add(pde,
                getResourceString("PN_INTERACTION_CFLIST"),
                getResourceString("PD_INTERACTION_CFLIST"));
        
        pde = new PropertyDescriptorEx("participantList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PL");
        add(pde,
                getResourceString("PN_INTERACTION_PARTICIPANT_LIST"),
                getResourceString("PD_INTERACTION_PARTICIPANT_LIST"));
        
        pde = new PropertyDescriptorEx("parameterList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PL");
        add(pde,
                getResourceString("PN_INTERACTION_PARAMETER_LIST"),
                getResourceString("PD_INTERACTION_PARAMETER_LIST"));
        
        pde = new PropertyDescriptorEx("inferredInteractionList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IEL");
        add(pde,
                getResourceString("PN_INTERACTION_II_LIST"),
                getResourceString("PD_INTERACTION_II_LIST"));
    }
}
