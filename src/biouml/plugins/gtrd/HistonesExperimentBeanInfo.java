package biouml.plugins.gtrd;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class HistonesExperimentBeanInfo extends BeanInfoEx 
{
   public HistonesExperimentBeanInfo()
   {
        super(HistonesExperiment.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName("ChIP-seq HM experiments");
        beanDescriptor.setShortDescription("Histone marks ChIP-seq experiment");
   }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pde, "Experiment ID", "Experiment ID");
        
        pde = new PropertyDescriptorEx("antibody", beanClass, "getAntibody", null);
        add(pde, "Antibody", "Antibody used in ChIP");

        pde = new PropertyDescriptorEx( "target", beanClass, "getTarget", null );
        add(pde, "Target", "Histone modification's name");

        pde = new PropertyDescriptorEx("cell", beanClass, "getCell", null);
        add(pde, "Cell", "Cell");

        pde = new PropertyDescriptorEx("treatment", beanClass, "getTreatment", null);
        add(pde, "Treatment", "Treatment");
        
        pde = new PropertyDescriptorEx("specie", beanClass, "getSpecie", null);
        add(pde, "Specie", "Specie");
        
        pde = new PropertyDescriptorEx("externalReferences", beanClass, "getExternalReferences", null);
        add(pde, "External References", "References to external databases");

        pde = new PropertyDescriptorEx("controlExperiment", beanClass, "isControlExperiment", null);
        add(pde, "Is control experiment", "Is control experiment");

        pde = new PropertyDescriptorEx("control", beanClass, "getControl", null);
        //pde.setHidden(beanClass.getMethod("isControlExperiment"));
        add(pde, "Control", "Control ChIP experiment");
        
        pde = new PropertyDescriptorEx("articles", beanClass, "getArticles", null);
        add(pde, "Articles", "Articles");
        
        pde = new PropertyDescriptorEx("macsPeaks", beanClass, "getMacsPeaks", null);
        add(pde, "MACS2 Peaks", "MACS2 Peaks");
        
        pde = new PropertyDescriptorEx("alignment", beanClass);
        add(pde, "Alignment", "Alignment");
        
        pde = new PropertyDescriptorEx("reads", beanClass);
        add(pde, "Reads", "Reads");
    }

}
