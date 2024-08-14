package biouml.plugins.simulation.ode.radau5;

import biouml.plugins.simulation.OdeSimulatorOptionsBeanInfo;

public class Radau5OptionsBeanInfo extends OdeSimulatorOptionsBeanInfo
{
    public Radau5OptionsBeanInfo()
    {
        super(Radau5Options.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add("hinit");
        add("mljac");
        add("mujac");
        add("mlmas");
        add("mumas");
        add("hmax");
        add("nmax");
        add("facl");
        add("nit");
        add("startn");
        add("predictGustafsson");
        add("hessenberg");
        add("fnewt");
        add("quot1");
        add("quot2");
        add("thet");
    }
}