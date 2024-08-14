package biouml.plugins.gtrd;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.biohub.ReferenceTypeRegistry;

public class ChIPexoExperimentBeanInfo extends BeanInfoEx
{
	public ChIPexoExperimentBeanInfo()
    {
        super(ChIPexoExperiment.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName("ChIP-exo experiment");
        beanDescriptor.setShortDescription("ChIP-exo experiment");
    }
	
	protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pde, "Experiment ID", "Experiment ID");
        
        pde = new PropertyDescriptorEx("antibody", beanClass, "getAntibody", null);
        add(pde, "Antibody", "Antibody used in ChIP");

        pde = new PropertyDescriptorEx("tfClassId", beanClass, "getTfClassId", null);
        pde.setValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(ProteinGTRDType.class)
                .toString());
        //pde.setHidden(beanClass.getMethod("isControlExperiment"));
        add(pde, "TF class", "Transcription factor class");
        
        pde = new PropertyDescriptorEx( "tfTitle", beanClass, "getTfTitle", null );
        //pde.setHidden(beanClass.getMethod("isControlExperiment"));
        add(pde, "TF title", "Transcription factor class");

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
        
        pde = new PropertyDescriptorEx("gem", beanClass, "getGemPeaks", null);
        add(pde, "GEM Peaks", "GEM Peaks");
        
        pde = new PropertyDescriptorEx("peakzilla", beanClass, "getPeakzillaPeaks", null);
        add(pde, "Peakzilla Peaks", "Peakzilla Peaks");
        
        pde = new PropertyDescriptorEx("alignment", beanClass);
        add(pde, "Alignment", "Alignment");
        
        pde = new PropertyDescriptorEx("reads", beanClass);
        add(pde, "Reads", "Reads");
    }

}
