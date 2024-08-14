package biouml.plugins.hemodynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

public class HemodynamicsOptionsBeanInfo extends BeanInfoEx2<HemodynamicsOptions>
{
    public HemodynamicsOptionsBeanInfo()
    {
        super(HemodynamicsOptions.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        addWithTags("inputCondition", HemodynamicsOptions.getInputConditions());
        addWithTags("outputCondition", HemodynamicsOptions.getOutputConditions());
        add("fromZero");
        add("oldLinearisation");
        add("useFullPressureConservation");
        add("modelArteriols");
    }
}