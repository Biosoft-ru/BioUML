package ru.biosoft.bsa.analysis.createsitemodel;


import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CreateIPSModelParametersBeanInfo extends CreateSiteModelParametersBeanInfo
{
    public CreateIPSModelParametersBeanInfo()
    {
        super(CreateIPSModelParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInputMulti("frequencyMatrices", beanClass, FrequencyMatrix.class), getResourceString("PN_FREQUENCY_MATRICES"), getResourceString("PD_FREQUENCY_MATRICES"));
        add(new PropertyDescriptorEx("critIPS", beanClass), getResourceString("PN_CRIT_IPS"), getResourceString("PN_CRIT_IPS"));
        add(new PropertyDescriptorEx("windowSize", beanClass), getResourceString("PN_WINDOW_SIZE"), getResourceString("PD_WINDOW_SIZE"));
        add(new PropertyDescriptorEx("distMin", beanClass), getResourceString("PN_DIST_MIN"), getResourceString("PD_DIST_MIN"));
        super.initProperties();
    }
}
