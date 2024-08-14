package biouml.plugins.gtrd;

import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.bsa.analysis.FrequencyMatrixBeanInfo;
import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class GTRDMatrixBeanInfo extends FrequencyMatrixBeanInfo
{
    public GTRDMatrixBeanInfo()
    {
        super(GTRDMatrix.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        PropertyDescriptorEx pde = new PropertyDescriptorEx("references", beanClass, "getReferencesString", null);
        pde.setValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(PeaksGTRDType.class).toString());
        add(pde, getResourceString("PN_REFERENCES"), getResourceString("PD_REFERENCES"));

        pde = new PropertyDescriptorEx("uniprot", beanClass, "getUniprotIDs", null);
        pde.setValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class).toString());
        add(pde, getResourceString("PN_UNIPROT"), getResourceString("PD_UNIPROT"));

        pde = new PropertyDescriptorEx("classReferences", beanClass, "getClassReferences", null);
        pde.setValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(ClassGTRDType.class).toString());
        add(pde, getResourceString("PN_CLASS_REFERENCES"), getResourceString("PD_CLASS_REFERENCES"));

        addHidden(new PropertyDescriptorEx("classReference", beanClass, "getClassReference", null));
    }
}
