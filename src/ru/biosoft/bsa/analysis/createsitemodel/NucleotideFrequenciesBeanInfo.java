package ru.biosoft.bsa.analysis.createsitemodel;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class NucleotideFrequenciesBeanInfo extends BeanInfoEx
{
    public NucleotideFrequenciesBeanInfo()
    {
        super(NucleotideFrequencies.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("PN_NUCLEOTIDE_FREQUENCIES"));
        beanDescriptor.setShortDescription(getResourceString("PD_NUCLEOTIDE_FREQUENCIES"));
    }
    
    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("a", beanClass));
        add(new PropertyDescriptorEx("c", beanClass));
        add(new PropertyDescriptorEx("g", beanClass));
        add(new PropertyDescriptorEx("t", beanClass));
    }

}
