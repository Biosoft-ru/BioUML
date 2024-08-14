
package biouml.plugins.bindingregions.analysis;


import biouml.plugins.bindingregions.resources.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author yura
 *
 */
public class BindingRegionsParametersBeanInfo extends BeanInfoEx
{
    public BindingRegionsParametersBeanInfo()
    {
        super(BindingRegionsParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_BINDING_REGIONS);
        beanDescriptor.setShortDescription(MessageBundle.CD_BINDING_REGIONS);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("mode", beanClass), BindingRegionsParameters.ModesEditor.class, getResourceString("PN_MODE"), getResourceString("PD_MODE"));

        PropertyDescriptorEx pde1 = new PropertyDescriptorEx("expParameters1", beanClass);
        pde1.setHidden(beanClass.getMethod("isExpParameters1Hidden"));
        add(pde1, "Exp parameters1", "Exp parameters1");
        
        PropertyDescriptorEx pde2 = new PropertyDescriptorEx("expParameters2", beanClass);
        pde2.setHidden(beanClass.getMethod("isExpParameters2Hidden"));
        add(pde2, "Exp parameters2", "Exp parameters2");

        PropertyDescriptorEx pde3 = new PropertyDescriptorEx("expParameters3", beanClass);
        pde3.setHidden(beanClass.getMethod("isExpParameters3Hidden"));
        add(pde3, "Exp parameters3", "Exp parameters3");
        
        PropertyDescriptorEx pde4 = new PropertyDescriptorEx("expParameters4", beanClass);
        pde4.setHidden(beanClass.getMethod("isExpParameters4Hidden"));
        add(pde4, "Exp parameters4", "Exp parameters4");

        PropertyDescriptorEx pde5 = new PropertyDescriptorEx("expParameters5", beanClass);
        pde5.setHidden(beanClass.getMethod("isExpParameters5Hidden"));
        add(pde5, "Exp parameters5", "Exp parameters5");
        
        PropertyDescriptorEx pde6 = new PropertyDescriptorEx("expParameters6", beanClass);
        pde6.setHidden(beanClass.getMethod("isExpParameters6Hidden"));
        add(pde6, "Exp parameters6", "Exp parameters6");

        PropertyDescriptorEx pde7 = new PropertyDescriptorEx("expParameters7", beanClass);
        pde7.setHidden(beanClass.getMethod("isExpParameters7Hidden"));
        add(pde7, "Exp parameters7", "Exp parameters7");
        
        PropertyDescriptorEx pde8 = new PropertyDescriptorEx("expParameters8", beanClass);
        pde8.setHidden(beanClass.getMethod("isExpParameters8Hidden"));
        add(pde8, "Exp parameters8", "Exp parameters8");

        PropertyDescriptorEx pde9 = new PropertyDescriptorEx("expParameters9", beanClass);
        pde9.setHidden(beanClass.getMethod("isExpParameters9Hidden"));
        add(pde9, "Exp parameters9", "Exp parameters9");
        
        PropertyDescriptorEx pde10 = new PropertyDescriptorEx("expParameters10", beanClass);
        pde10.setHidden(beanClass.getMethod("isExpParameters10Hidden"));
        add(pde10, "Exp parameters10", "Exp parameters10");
        
        PropertyDescriptorEx pde11 = new PropertyDescriptorEx("expParameters11", beanClass);
        pde11.setHidden(beanClass.getMethod("isExpParameters11Hidden"));
        add(pde11, "Exp parameters11", "Exp parameters11");

        PropertyDescriptorEx pde12 = new PropertyDescriptorEx("expParameters12", beanClass);
        pde12.setHidden(beanClass.getMethod("isExpParameters12Hidden"));
        add(pde12, "Exp parameters12", "Exp parameters12");
        
        PropertyDescriptorEx pde13 = new PropertyDescriptorEx("expParameters13", beanClass);
        pde13.setHidden(beanClass.getMethod("isExpParameters13Hidden"));
        add(pde13, "Exp parameters13", "Exp parameters13");
        
        PropertyDescriptorEx pde14 = new PropertyDescriptorEx("expParameters14", beanClass);
        pde14.setHidden(beanClass.getMethod("isExpParameters14Hidden"));
        add(pde14, "Exp parameters14", "Exp parameters14");
        
        PropertyDescriptorEx pde15 = new PropertyDescriptorEx("expParameters15", beanClass);
        pde15.setHidden(beanClass.getMethod("isExpParameters15Hidden"));
        add(pde15, "Exp parameters15", "Exp parameters15");
        
        PropertyDescriptorEx pde16 = new PropertyDescriptorEx("expParameters16", beanClass);
        pde16.setHidden(beanClass.getMethod("isExpParameters16Hidden"));
        add(pde16, "Exp parameters16", "Exp parameters16");
        
        PropertyDescriptorEx pde17 = new PropertyDescriptorEx("expParameters17", beanClass);
        pde17.setHidden(beanClass.getMethod("isExpParameters17Hidden"));
        add(pde17, "Exp parameters17", "Exp parameters17");
        
        PropertyDescriptorEx pde18 = new PropertyDescriptorEx("expParameters18", beanClass);
        pde18.setHidden(beanClass.getMethod("isExpParameters18Hidden"));
        add(pde18, "Exp parameters18", "Exp parameters18");
        
        PropertyDescriptorEx pde19 = new PropertyDescriptorEx("expParameters19", beanClass);
        pde19.setHidden(beanClass.getMethod("isExpParameters19Hidden"));
        add(pde19, "Exp parameters19", "Exp parameters19");
        
        PropertyDescriptorEx pde20 = new PropertyDescriptorEx("expParameters20", beanClass);
        pde20.setHidden(beanClass.getMethod("isExpParameters20Hidden"));
        add(pde20, "Exp parameters20", "Exp parameters20");
        
        PropertyDescriptorEx pde21 = new PropertyDescriptorEx("expParameters21", beanClass);
        pde21.setHidden(beanClass.getMethod("isExpParameters21Hidden"));
        add(pde21, "Exp parameters21", "Exp parameters21");

        PropertyDescriptorEx pde22 = new PropertyDescriptorEx("expParameters22", beanClass);
        pde22.setHidden(beanClass.getMethod("isExpParameters22Hidden"));
        add(pde22, "Exp parameters22", "Exp parameters22");
        
        PropertyDescriptorEx pde23 = new PropertyDescriptorEx("expParameters23", beanClass);
        pde23.setHidden(beanClass.getMethod("isExpParameters23Hidden"));
        add(pde23, "Exp parameters23", "Exp parameters23");
        
        PropertyDescriptorEx pde24 = new PropertyDescriptorEx("expParameters24", beanClass);
        pde24.setHidden(beanClass.getMethod("isExpParameters24Hidden"));
        add(pde24, "Exp parameters24", "Exp parameters24");
        
        PropertyDescriptorEx pde25 = new PropertyDescriptorEx("expParameters25", beanClass);
        pde25.setHidden(beanClass.getMethod("isExpParameters25Hidden"));
        add(pde25, "Exp parameters25", "Exp parameters25");
        
        PropertyDescriptorEx pde27 = new PropertyDescriptorEx("expParameters27", beanClass);
        pde27.setHidden(beanClass.getMethod("isExpParameters27Hidden"));
        add(pde27, "Exp parameters27", "Exp parameters27");
        
        PropertyDescriptorEx pde28 = new PropertyDescriptorEx("expParameters28", beanClass);
        pde28.setHidden(beanClass.getMethod("isExpParameters28Hidden"));
        add(pde28, "Exp parameters28", "Exp parameters28");

        PropertyDescriptorEx pde29 = new PropertyDescriptorEx("expParameters29", beanClass);
        pde29.setHidden(beanClass.getMethod("isExpParameters29Hidden"));
        add(pde29, "Exp parameters29", "Exp parameters29");
        
        PropertyDescriptorEx pde30 = new PropertyDescriptorEx("expParameters30", beanClass);
        pde30.setHidden(beanClass.getMethod("isExpParameters30Hidden"));
        add(pde30, "Exp parameters30", "Exp parameters30");
        
//      add(new PropertyDescriptorEx("minimalNumberOfOverlaps", beanClass), getResourceString("PN_MINIMAL_NUMBER_OF_OVERLAPS"), getResourceString("PD_MINIMAL_NUMBER_OF_OVERLAPS"));
//      add(DataElementPathEditor.registerInput("sequencePath", beanClass, Map.class));   path to given sequence
    }
}