package ru.biosoft.bsa;

import ru.biosoft.access.repository.DataElementPathEditor;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class BasicGenomeSelectorBeanInfo extends BeanInfoEx
{
    public BasicGenomeSelectorBeanInfo()
    {
        this(BasicGenomeSelector.class);
    }
    
    protected BasicGenomeSelectorBeanInfo(Class<? extends BasicGenomeSelector> beanClass)
    {
        super(beanClass, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("PN_TRACKIMPORT_PROPERTIES"));
        beanDescriptor.setShortDescription(getResourceString("PD_TRACKIMPORT_PROPERTIES"));
    }
    
    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("dbSelector", beanClass);
        add(pde, getResourceString("PN_SITESEARCH_SEQDATABASE"), getResourceString("PD_SITESEARCH_SEQDATABASE"));
        pde = DataElementPathEditor.registerInput("sequenceCollectionPath", beanClass, SequenceCollection.class);
        pde.setHidden(beanClass.getMethod("isSequenceCollectionPathHidden"));
        add(pde, getResourceString("PN_TRACKIMPORT_SEQCOLLECTION"), getResourceString("PD_TRACKIMPORT_SEQCOLLECTION"));
    }
}
