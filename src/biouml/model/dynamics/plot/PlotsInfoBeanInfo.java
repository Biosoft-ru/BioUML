package biouml.model.dynamics.plot;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PlotsInfoBeanInfo extends BeanInfoEx2<PlotsInfo>
{
    public PlotsInfoBeanInfo()
    {
        super(PlotsInfo.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("plots");
    }
}