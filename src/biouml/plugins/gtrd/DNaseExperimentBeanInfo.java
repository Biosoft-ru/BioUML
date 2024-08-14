package biouml.plugins.gtrd;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class DNaseExperimentBeanInfo extends BeanInfoEx
{
    public DNaseExperimentBeanInfo()
    {
        super(DNaseExperiment.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName("DNase-seq experiment");
        beanDescriptor.setShortDescription("DNase-seq experiment");
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pde, "Experiment ID", "Experiment ID");
        
        pde = new PropertyDescriptorEx("cell", beanClass, "getCell", null);
        add(pde, "Cell", "Cell");

        pde = new PropertyDescriptorEx("treatment", beanClass, "getTreatment", null);
        add(pde, "Treatment", "Treatment");
        
        pde = new PropertyDescriptorEx("specie", beanClass, "getSpecie", null);
        add(pde, "Specie", "Specie");
        
        pde = new PropertyDescriptorEx("externalReferences", beanClass, "getExternalReferences", null);
        add(pde, "External References", "References to external databases");
      
        pde = new PropertyDescriptorEx("macsPeaks", beanClass, "getMacsPeaks", null);
        add(pde, "MACS2 Peaks", "MACS2 Peaks");
        
        pde = new PropertyDescriptorEx("hotspot2", beanClass, "getHotspotPeaks", null);
        add(pde, "Hotspot2 Peaks", "Hotspot2");
        
        pde = new PropertyDescriptorEx("wellington_macs2", beanClass, "getMacsWelingtonPeaks", null);
        add(pde, "Wellington MACS2 footprints", "Wellington MACS2 Peaks");
        
        pde = new PropertyDescriptorEx("wellington_hotspot2", beanClass, "getHotspotWelingtonPeaks", null);
        add(pde, "Wellington Hotspot2 footprints", "Wellington Hotspot2 Peaks");
        
        pde = new PropertyDescriptorEx("alignment", beanClass);
        add(pde, "Alignment", "Alignment");
        
        pde = new PropertyDescriptorEx("reads", beanClass);
        add(pde, "Reads", "Reads");
    }
}
