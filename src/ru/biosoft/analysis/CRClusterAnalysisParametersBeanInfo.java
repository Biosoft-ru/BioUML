package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CRClusterAnalysisParametersBeanInfo extends BeanInfoEx
{
    public CRClusterAnalysisParametersBeanInfo()
    {
        super(CRClusterAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("experimentData", beanClass), getResourceString("PN_EXPERIMENT"), getResourceString("PD_EXPERIMENT"));
        add(BeanUtil.createExpertDescriptor("chainsCount", beanClass), getResourceString("PN_CRC_CHAIN_COUNT"),
                getResourceString("PD_CRC_CHAIN_COUNT"));
        add(BeanUtil.createExpertDescriptor("cycleCount", beanClass), getResourceString("PN_CRC_CYCLE_COUNT"),
                getResourceString("PD_CRC_CYCLE_COUNT"));
        add(BeanUtil.createExpertDescriptor("cutoff", beanClass), getResourceString("PN_CRC_CUTOFF"),
                getResourceString("PD_CRC_CUTOFF"));
        
//        add(new PropertyDescriptorEx("maxShift", beanClass), getResourceString("PN_CRC_MAXIMUM_SHIFT"),
//                getResourceString("PD_CRC_MAXIMUM_SHIFT"));
        add(BeanUtil.createExpertDescriptor("invert", beanClass), getResourceString("PN_CRC_ALLOW_INVERSION"),
                getResourceString("PD_CRC_ALLOW_INVERSION"));
        add(BeanUtil.createExpertDescriptor("threshold", beanClass), getResourceString("PN_THRESHOLD"), getResourceString("PD_THRESHOLD"));
        add(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class),
                getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
        
    }
}
