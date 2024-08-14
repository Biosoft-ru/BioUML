package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.model.util.ReactionEditor;
import biouml.standard.diagram.DatabaseReferencesPropertyEditor;
import biouml.standard.type.ReferrerBeanInfo.LiteratureReferencesEditor;
import ru.biosoft.access.support.SetAttributesCommand;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ReactionBeanInfo extends BeanInfoEx2<Reaction>
{
    public ReactionBeanInfo()
    {
        super(Reaction.class);
        this.setBeanEditor( ReactionEditor.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null)).htmlDisplayName("ID").add();
        property(new PropertyDescriptorEx("type", beanClass.getMethod("getType"), null)).htmlDisplayName("TY").expert().add();
        property("date").expert().htmlDisplayName("DT").add();
        property("title").expert().htmlDisplayName("TI").add();
        property("comment").expert().htmlDisplayName("CC").add();
        property(new PropertyDescriptorEx("attributes", beanClass.getMethod("getAttributes"), null)).expert()
                .value("commandClass", SetAttributesCommand.class).hidden("hasNoAttributes").htmlDisplayName("AT").add();        
        property("description").htmlDisplayName("DE").add(4);
        property("reversible").htmlDisplayName("RV").add();
        property("fast").htmlDisplayName("FS").add();
        property("kineticLaw").add();
        property("specieReferences").htmlDisplayName("SR")
                .childDisplayName( beanClass.getMethod( "getSpecieTitle", new Class[] {Integer.class, Object.class} ) ).add();
        property("databaseReferences").editor(DatabaseReferencesPropertyEditor.class).htmlDisplayName("DR").add();
        property("literatureReferences").htmlDisplayName("RF").editor(LiteratureReferencesEditor.class).add();
    }
}
