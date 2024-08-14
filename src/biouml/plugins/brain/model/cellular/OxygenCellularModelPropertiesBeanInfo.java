package biouml.plugins.brain.model.cellular;

import ru.biosoft.util.bean.BeanInfoEx2;

public class OxygenCellularModelPropertiesBeanInfo extends BeanInfoEx2<OxygenCellularModelProperties>
{
    public OxygenCellularModelPropertiesBeanInfo()
    {
        super(OxygenCellularModelProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {        
        add("c");
        add("iExt");
        add("gNa");
        add("gK");
        add("gNaL");
        add("gKL");
        add("gClL");
        add("gamma");
        add("beta");
        add("roMax");
        add("gGlia");
        add("epsK");
        add("epsO");
        add("alpha");
        add("lambda");
    }
}