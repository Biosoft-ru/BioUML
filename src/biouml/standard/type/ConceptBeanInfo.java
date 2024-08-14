package biouml.standard.type;

import com.developmentontheedge.beans.editors.StringTagEditorSupport;

//FIXME: rework hierarchy with parametrization (here we loose T but it is possible that subclasses will need it)
public class ConceptBeanInfo extends ReferrerBeanInfo<Concept>
{
    protected ConceptBeanInfo(Class<? extends Concept> c)
    {
        super(c);
    }

    public ConceptBeanInfo()
    {
        super(Concept.class);
        properties.remove(1);
        property("type").editor(ConceptTypeEditor.class).htmlDisplayName("TY").add(1);
    }
    
    protected ConceptBeanInfo(Class<? extends Concept> beanClass, String key)
    {
        super(beanClass, key);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property("completeName").htmlDisplayName("NM").add(4);
        property("synonyms").htmlDisplayName("SY").add(5);
    }

    public static class ConceptTypeEditor extends StringTagEditorSupport
    {
        public ConceptTypeEditor()
        {
            super(MessageBundle.class.getName(), MessageBundle.class, "CONCEPT_TYPES");
        }

        @Override
        public String getAsText()
        {
            String type = (String)getValue();

            if( type.endsWith("concept") )
                return "concept";

            if( type.length() > Type.TYPE_CONCEPT.length() + 1 )
                return type.substring(Type.TYPE_CONCEPT.length() + 1);

            return null;
        }

        @Override
        public void setAsText(String text)
        {
            if( text.equals("concept") )
                setValue("semantic-concept");
            else
                setValue(Type.TYPE_CONCEPT + "-" + text);
        }
    }
}
