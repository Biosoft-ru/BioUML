package biouml.plugins.psimi.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

import biouml.standard.type.ConceptBeanInfo;

public class ParticipantBeanInfo extends ConceptBeanInfo
{
    public ParticipantBeanInfo()
    {
        this(Participant.class, "PARTICIPANT");
    }

    protected ParticipantBeanInfo(Class<? extends Participant> beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        initResources("biouml.plugins.psimi.MessageBundle");

        PropertyDescriptorEx pde;
        
        pde = new PropertyDescriptorEx("interactorRef", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "IRR");
        add(pde,
                getResourceString("PN_PARTICIPANT_INTERACTORREF"),
                getResourceString("PD_PARTICIPANT_INTERACTORREF"));
        
        pde = new PropertyDescriptorEx("interactionRef", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "INR");
        add(pde,
                getResourceString("PN_PARTICIPANT_INTERACTIONREF"),
                getResourceString("PD_PARTICIPANT_INTERACTIONREF"));
        
        pde = new PropertyDescriptorEx("biologicalRole", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "BR");
        add(pde,
                getResourceString("PN_PARTICIPANT_BIOLOGICALROLE"),
                getResourceString("PD_PARTICIPANT_BIOLOGICALROLE"));
        
        pde = new PropertyDescriptorEx("experimentalRoleList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "ERL");
        add(pde,
                getResourceString("PN_PARTICIPANT_EXROLE_LIST"),
                getResourceString("PD_PARTICIPANT_EXROLE_LIST"));
        
        pde = new PropertyDescriptorEx("experimentalPreparationList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "EPL");
        add(pde,
                getResourceString("PN_PARTICIPANT_EXPREPARATION_LIST"),
                getResourceString("PD_PARTICIPANT_EXPREPARATION_LIST"));
        
        pde = new PropertyDescriptorEx("featureList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "FL");
        add(pde,
                getResourceString("PN_PARTICIPANT_FEATURE_LIST"),
                getResourceString("PD_PARTICIPANT_FEATURE_LIST"));
        
        pde = new PropertyDescriptorEx("hostOrganismList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "HOL");
        add(pde,
                getResourceString("PN_PARTICIPANT_HOSTORGANISM_LIST"),
                getResourceString("PD_PARTICIPANT_HOSTORGANISM_LIST"));
        
        pde = new PropertyDescriptorEx("confidenceList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "COL");
        add(pde,
                getResourceString("PN_PARTICIPANT_CONFIDENCE_LIST"),
                getResourceString("PD_PARTICIPANT_CONFIDENCE_LIST"));
        
        pde = new PropertyDescriptorEx("parameterList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PL");
        add(pde,
                getResourceString("PN_PARTICIPANT_PARAMETER_LIST"),
                getResourceString("PD_PARTICIPANT_PARAMETER_LIST"));
        
        pde = new PropertyDescriptorEx("participantIdentificationMethodList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "PIML");
        add(pde,
                getResourceString("PN_PARTICIPANT_PIMETHOD_LIST"),
                getResourceString("PD_PARTICIPANT_PIMETHOD_LIST"));
        
        pde = new PropertyDescriptorEx("experimentalInteractorList", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "EIL");
        add(pde,
                getResourceString("PN_PARTICIPANT_EI_LIST"),
                getResourceString("PD_PARTICIPANT_EI_LIST"));
    }
}
