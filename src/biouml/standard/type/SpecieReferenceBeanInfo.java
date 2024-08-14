package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.support.SetAttributesCommand;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SpecieReferenceBeanInfo extends BeanInfoEx2<SpecieReference>
{
    public SpecieReferenceBeanInfo()
    {
        super(SpecieReference.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property(new PropertyDescriptorEx("name", beanClass, "getName", null)).htmlDisplayName("ID").add();
        property(new PropertyDescriptorEx("specieVariable", beanClass, "getSpecieVariable", null)).add();
        property("specie").hidden().htmlDisplayName("SP").add();
        property("role").readOnly("isInitialized").tags(SpecieReference.specieRoles).htmlDisplayName("RL").add();
        property("stoichiometry").htmlDisplayName("ST").add();
        property("modifierAction").readOnly("isReactantOrProduct").tags(SpecieReference.modifierActions).htmlDisplayName("MD").add();
        property("participation").tags(SpecieReference.participationTypes).htmlDisplayName("PT").add();
        property("title").htmlDisplayName("TI").add();
        property("comment").htmlDisplayName("CC").add();
        property(new PropertyDescriptorEx("attributes", beanClass.getMethod("getAttributes"), null))
                .value("commandClass", SetAttributesCommand.class).hidden("hasNoAttributes").htmlDisplayName("AT").add();
    }
}
