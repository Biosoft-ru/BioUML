package biouml.plugins.brain.model.receptor;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class AmpaReceptorModelPropertiesBeanInfo extends BeanInfoEx2<AmpaReceptorModelProperties>
{
    public AmpaReceptorModelPropertiesBeanInfo()
    {
        super(AmpaReceptorModelProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
    	addWithTags("regimeType", AmpaReceptorModelProperties.availableRegimes);
    	
        add("lTotal");
        
        String[] numericParameters = new String[] 
        {
        	"alpha1",
        	"alpha2",
        	"beta1",
        	"beta2a",
        	"beta2b",
        	"c",
        	"delta1",
        	"gamma",
        	"h1",
        	"h2a",
        	"h2b",
        	"k1",
        	"k2",
        	"kappa1",
        	"kappa2",
        	"mu",
        	"nu",
        	"omega1",
        	"omega2",
        	"sigma1",
        	"sigma2"
        };
        
        for (String par : numericParameters)
        {
        	PropertyDescriptorEx pde = new PropertyDescriptorEx(par, beanClass);
            pde.setNumberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE);
            add(pde);
        }
        
        addWithTags("modelType", AmpaReceptorModelProperties.availableModelTypes);
        
        add("portsFlag");
    }
}