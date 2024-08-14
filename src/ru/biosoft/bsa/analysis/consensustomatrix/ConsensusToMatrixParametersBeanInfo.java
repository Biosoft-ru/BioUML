package ru.biosoft.bsa.analysis.consensustomatrix;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ConsensusToMatrixParametersBeanInfo extends BeanInfoEx
{

    public ConsensusToMatrixParametersBeanInfo()
    {
        super(ConsensusToMatrixParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_CONSENSUS_TO_MATRIX);
        beanDescriptor.setShortDescription(MessageBundle.CD_CONSENSUS_TO_MATRIX);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("consensus", beanClass), MessageBundle.PN_CONSENSUS, MessageBundle.PD_CONSENSUS);
        
        PropertyDescriptorEx pde = DataElementPathEditor.registerOutput("outputCollection", beanClass, WeightMatrixCollection.class);
        pde.setValue(DataElementPathEditor.CHILD_CLASS, FrequencyMatrix.class);
        add(pde, MessageBundle.PN_OUTPUT_COLLECTION, MessageBundle.PD_OUTPUT_COLLECTION);
        add(new PropertyDescriptor("matrixName", beanClass), MessageBundle.PN_MATRIX_NAME, MessageBundle.PD_MATRIX_NAME);
        
        addHidden(DataElementPathEditor.registerOutput("matrix", beanClass, FrequencyMatrix.class));
    }



}
