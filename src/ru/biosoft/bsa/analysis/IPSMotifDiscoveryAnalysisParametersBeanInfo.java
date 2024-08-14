package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.gui.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class IPSMotifDiscoveryAnalysisParametersBeanInfo extends BeanInfoEx
{
    public IPSMotifDiscoveryAnalysisParametersBeanInfo()
    {
        super(IPSMotifDiscoveryAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = DataElementPathEditor.registerInput("sequencesPath", beanClass, Track.class);
        pde.setValue(DataElementPathEditor.CHILD_CLASS, AnnotatedSequence.class);
        add(pde, getResourceString("PN_IPSMOTIFDISCOVERY_SEQCOLLECTION"), getResourceString("PD_IPSMOTIFDISCOVERY_SEQCOLLECTION"));

        add(DataElementPathEditor.registerInputMulti("initialMatrices", beanClass, FrequencyMatrix.class),
                getResourceString("PN_IPSMOTIFDISCOVERY_INITIAL_MATRICES"), getResourceString("PD_IPSMOTIFDISCOVERY_INITIAL_MATRICES"));
        add(new PropertyDescriptor("extendInitialMatrices", beanClass), getResourceString("PN_IPSMOTIFDISCOVERY_EXTEND"),
                getResourceString("PD_IPSMOTIFDISCOVERY_EXTEND"));

        add(new PropertyDescriptorEx("maxIterations", beanClass), getResourceString("PN_IPSMOTIFDISCOVERY_MAX_ITERATIONS"),
                getResourceString("PD_IPSMOTIFDISCOVERY_MAX_ITERATIONS"));
        add(new PropertyDescriptorEx("minClusterSize", beanClass), getResourceString("PN_IPSMOTIFDISCOVERY_MIN_CLUSTER_SIZE"),
                getResourceString("PD_IPSMOTIFDISCOVERY_MIN_CLUSTER_SIZE"));
        add(new PropertyDescriptorEx("windowSize", beanClass), getResourceString("PN_IPSMOTIFDISCOVERY_WINDOW_SIZE"),
                getResourceString("PD_IPSMOTIFDISCOVERY_WINDOW_SIZE"));
        add(new PropertyDescriptorEx("critIPS", beanClass), getResourceString("PN_IPSMOTIFDISCOVERY_CRIT_IPS"),
                getResourceString("PD_IPSMOTIFDISCOVERY_CRIT_IPS"));

        pde = DataElementPathEditor.registerOutput("outputPath", beanClass, WeightMatrixCollection.class);
        pde.setValue(DataElementPathEditor.CHILD_CLASS, FrequencyMatrix.class);
        add(pde, getResourceString("PN_IPSMOTIFDISCOVERY_OUTPUT_MATRIX_LIB"), getResourceString("PD_IPSMOTIFDISCOVERY_OUTPUT_MATRIX_LIB"));
    }

}
