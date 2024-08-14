package biouml.standard.type;

import ru.biosoft.util.bean.BeanInfoEx2;

public class KineticLawBeanInfo extends BeanInfoEx2<KineticLaw>
{
    public KineticLawBeanInfo()
    {
        super(KineticLaw.class);
        this.setSubstituteByChild(true);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property("formula").htmlDisplayName("FM").add();
        //        property("timeUnits").htmlDisplayName("TU").add();
        //        property("substanceUnits").htmlDisplayName("SU").add();
        //        property("comment").htmlDisplayName("CC").add();
    }
}
