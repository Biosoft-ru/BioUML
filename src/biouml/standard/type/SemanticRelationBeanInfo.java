package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class SemanticRelationBeanInfo extends ReferrerBeanInfo<SemanticRelation>
{
    public SemanticRelationBeanInfo()
    {
        super(SemanticRelation.class, "SEMANTIC_RELATION");
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("inputElementName", beanClass);
        pde.setReadOnly(true);
        HtmlPropertyInspector.setDisplayName(pde, "IN");
        add(2, pde,
            getResourceString("PN_RELATION_INPUT"),
            getResourceString("PD_RELATION_INPUT"));

        pde = new PropertyDescriptorEx("outputElementName", beanClass);
        pde.setReadOnly(true);
        HtmlPropertyInspector.setDisplayName(pde, "OUT");
        add(3, pde,
            getResourceString("PN_RELATION_OUTPUT"),
            getResourceString("PD_RELATION_OUTPUT"));

        pde = new PropertyDescriptorEx("relationType", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "RT");
        add(4, pde,
            getResourceString("PN_RELATION_TYPE"),
            getResourceString("PD_RELATION_TYPE"));

        pde = new PropertyDescriptorEx("participation", beanClass);
        pde.setPropertyEditorClass(ParticipationTypeEditor.class);
        HtmlPropertyInspector.setDisplayName(pde, "PT");
        add(5, pde,
            getResourceString("PN_PARTICIPATION_TYPE"),
            getResourceString("PD_PARTICIPATION_TYPE"));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Editors
    //

    public static class ParticipationTypeEditor extends StringTagEditorSupport
    {
        public ParticipationTypeEditor()
        {
            super(MessageBundle.class.getName(), MessageBundle.class, "PARTICIPATION_TYPES");
        }
    }

}
