package biouml.plugins.gtrd;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ATACExperimentBeanInfo extends BeanInfoEx 
{
	
	public ATACExperimentBeanInfo()
	{
		super(MNaseExperiment.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName("ATAC-seq experiment");
        beanDescriptor.setShortDescription("ATAC-seq experiment");
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
      
        pde = new PropertyDescriptorEx("macs2", beanClass, "getMacs2Peaks", null);
        add(pde, "MACS2 Peaks", "MACS2 Peaks");
        
        pde = new PropertyDescriptorEx("alignmentId", beanClass);
        add(pde, "AlignmentId", "AlignmentId");
        
        pde = new PropertyDescriptorEx("reads", beanClass);
        add(pde, "Reads", "Reads");
    }
}
