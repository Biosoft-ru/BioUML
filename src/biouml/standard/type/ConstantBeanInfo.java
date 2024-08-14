package biouml.standard.type;

public class ConstantBeanInfo extends ReferrerBeanInfo<Constant>
{
    public ConstantBeanInfo()
    {
        super(Constant.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property("value").htmlDisplayName("VL").add(2);
        property("unit").htmlDisplayName("UN").add(3);
    }
}
