package ru.biosoft.bsa.analysis.createsitemodel;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CreateWeightMatrixModelParametersBeanInfo extends CreateSiteModelParametersBeanInfo
{
    public CreateWeightMatrixModelParametersBeanInfo()
    {
        super(CreateWeightMatrixModelParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("matrixPath", beanClass, FrequencyMatrix.class), getResourceString("PN_MATRIX_PATH"),
                getResourceString("PD_MATRIX_PATH"));
        add(new PropertyDescriptorEx("modelType", beanClass), CreateWeightMatrixModelParameters.ModelTypeEditor.class,
                getResourceString("PN_MODEL_TYPE"), getResourceString("PD_MODEL_TYPE"));
        add(new PropertyDescriptorEx("threshold", beanClass), getResourceString("PN_THRESHOLD"), getResourceString("PD_THRESHOLD"));
        
        PropertyDescriptorEx pde = new PropertyDescriptorEx("nucleotideFrequencies", beanClass);
        pde.setHidden(beanClass.getMethod("isNucleotideFrequenciesHidden"));
        add(pde, getResourceString("PN_NUCLEOTIDE_FREQUENCIES"), getResourceString("PD_NUCLEOTIDE_FREQUENCIES"));
        
        super.initProperties();
        OptionEx.makeAutoProperty(findPropertyDescriptor("modelName"), "$matrixPath/name$");
    }
}
