package biouml.plugins.brain.model.cellular;

import ru.biosoft.util.bean.BeanInfoEx2;

public class Epileptor2OxygenCellularModelPropertiesBeanInfo extends BeanInfoEx2<Epileptor2OxygenCellularModelProperties>
{
    public Epileptor2OxygenCellularModelPropertiesBeanInfo()
    {
        super(Epileptor2OxygenCellularModelProperties.class);
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
        add("roMax");
        add("vTh");
        add("FRMax");
        add("kFR");
        add("gKL");
        add("gSyn");
        add("sigma");
        add("alpha");
        add("lambda");
        add("epsO");

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