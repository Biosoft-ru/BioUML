package biouml.plugins.brain.model.cellular;

import ru.biosoft.util.bean.BeanInfoEx2;

public class MinimalCellularModelPropertiesBeanInfo extends BeanInfoEx2<MinimalCellularModelProperties>
{
    public MinimalCellularModelPropertiesBeanInfo()
    {
        super(MinimalCellularModelProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {        
    	add("kBath");
        add("cM");
        add("tauN");
        add("gCl");
        add("gK");
        add("gNa");
        add("gKL");
        add("gNaL");
        add("omegaI");
        add("omegaO");
        add("beta");
        add("gamma");
        add("eps");
        add("ro");
    }
}