package biouml.standard.type;

import biouml.standard.type.ReferrerBeanInfo;
import one.util.streamex.StreamEx;

public class SpecieBeanInfo extends ReferrerBeanInfo<Specie>
{
    public SpecieBeanInfo()
    {
        super(Specie.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property("type").tags( bean->StreamEx.of(bean.getAvailableTypes())).htmlDisplayName("TY").add();
        property("charge").htmlDisplayName("CH").add();
    }
}
