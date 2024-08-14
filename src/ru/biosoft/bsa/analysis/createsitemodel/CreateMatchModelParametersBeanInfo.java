package ru.biosoft.bsa.analysis.createsitemodel;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CreateMatchModelParametersBeanInfo extends CreateSiteModelParametersBeanInfo
{
    public CreateMatchModelParametersBeanInfo()
    {
        super(CreateMatchModelParameters.class);

    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("matrixPath", beanClass, FrequencyMatrix.class), getResourceString("PN_MATRIX_PATH"), getResourceString("PD_MATRIX_PATH"));
        add(new PropertyDescriptorEx("cutoff", beanClass), getResourceString("PN_CUTOFF"), getResourceString("PD_CUTOFF"));
        add(new PropertyDescriptorEx("coreCutoff", beanClass), getResourceString("PN_CORE_CUTOFF"), getResourceString("PD_CORE_CUTOFF"));

        add(new PropertyDescriptorEx("defaultCore", beanClass), getResourceString("PN_DEFAULT_CORE"), getResourceString("PD_DEFAULT_CORE"));
        
        PropertyDescriptorEx pde = new PropertyDescriptorEx("coreStart", beanClass);
        pde.setHidden(beanClass.getMethod("isDefaultCore"));
        add(pde, getResourceString("PN_CORE_START"), getResourceString("PD_CORE_START"));
        
        pde = new PropertyDescriptorEx("coreLength", beanClass);
        pde.setHidden(beanClass.getMethod("isDefaultCore"));
        add(pde, getResourceString("PN_CORE_LENGTH"), getResourceString("PD_CORE_LENGTH"));
        
        super.initProperties();
        OptionEx.makeAutoProperty(findPropertyDescriptor("modelName"), "$matrixPath/name$");
    }
}
