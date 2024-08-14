package ru.biosoft.bsa.filter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.IndexedPropertyDescriptorEx;

public class TranscriptionFactorFilterBeanInfo extends BeanInfoEx
{
    public TranscriptionFactorFilterBeanInfo()
    {
        super(TranscriptionFactorFilter.class, "ru.biosoft.bsa.filter.MessageBundle");
        beanDescriptor.setDisplayName     (getResourceString("PN_TRANSCRIPTION_FACTOR_FILTER"));
        beanDescriptor.setShortDescription(getResourceString("PD_TRANSCRIPTION_FACTOR_FILTER"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new IndexedPropertyDescriptorEx("filter", beanClass),
            getResourceString("PN_TRANSCRIPTION_FACTOR_FILTER"),
            getResourceString("PD_TRANSCRIPTION_FACTOR_FILTER"));
        setSubstituteByChild(true);
    }
}

