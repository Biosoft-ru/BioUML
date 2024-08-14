package biouml.plugins.gtrd;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MNaseExperimentBeanInfo extends BeanInfoEx 
{
	
	public MNaseExperimentBeanInfo()
	{
		super(MNaseExperiment.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName("MNase-seq experiment");
        beanDescriptor.setShortDescription("MNase-seq experiment");
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
      
        pde = new PropertyDescriptorEx("danpos2", beanClass, "getDanpos2Peaks", null);
        add(pde, "DANPOS2 Peaks", "DANPOS2 Peaks");
        
        pde = new PropertyDescriptorEx("alignment", beanClass);
        add(pde, "Alignment", "Alignment");
        
        pde = new PropertyDescriptorEx("reads", beanClass);
        add(pde, "Reads", "Reads");
    }
}
