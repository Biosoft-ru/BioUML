package biouml.plugins.brain.model.cellular;

import ru.biosoft.util.bean.BeanInfoEx2;

public class Epileptor2CellularModelPropertiesBeanInfo extends BeanInfoEx2<Epileptor2CellularModelProperties>
{
    public Epileptor2CellularModelPropertiesBeanInfo()
    {
        super(Epileptor2CellularModelProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
    	add("sf");
    	
        add("tauK");
        add("tauNa");
        add("tauM");
        add("tauD");
        add("dKo");
        add("dNai");
        add("dSR");
        add("gamma");
        add("ro");
        add("vTh");
        add("FRMax");
        add("kFR");
        add("gKL");
        add("gSyn");
        add("sigma");
        
        addWithTags("neuronObserverType", Epileptor2CellularModelProperties.availableObservers);
        add("cU");
        add("gU");
        add("gL");
        add("URest");
        add("UTh");
        add("UPeak");
        add("UReset");
        
        add("portsFlag");
    }
}