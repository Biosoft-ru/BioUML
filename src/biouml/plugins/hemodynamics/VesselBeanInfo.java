package biouml.plugins.hemodynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VesselBeanInfo extends BeanInfoEx2<Vessel>
{
    public VesselBeanInfo()
    {
        super(Vessel.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("title");
        add("length");
        add("beta");
        add("initialArea");
        add("initialArea1");
        add("referencedPressure");
        add("plotPressure");
        add("plotFlow");
        add("plotArea");
        add("plotVelocity");
        add("plotPulseWaveVelocity");
        add("segment");
    }
}
