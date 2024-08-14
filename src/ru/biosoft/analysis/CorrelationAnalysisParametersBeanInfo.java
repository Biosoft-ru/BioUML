package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CorrelationAnalysisParametersBeanInfo extends BeanInfoEx
{
    public CorrelationAnalysisParametersBeanInfo()
    {
        super(CorrelationAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("experimentData", beanClass), getResourceString("PN_EXPERIMENT"), getResourceString("PD_EXPERIMENT"));
        add(new PropertyDescriptorEx("controlData", beanClass), getResourceString("PN_CONTROL"), getResourceString("PD_CONTROL"));

        add(new PropertyDescriptorEx("dataSource", beanClass), getResourceString("PN_CORR_DATA_SOURCE"),
                getResourceString("PD_CORR_DATA_SOURCE"));
        add(new PropertyDescriptorEx("resultType", beanClass), getResourceString("PN_CORR_RESULT_TYPE"),
                getResourceString("PD_CORR_RESULT_TYPE"));
        add(new PropertyDescriptorEx("correlationType", beanClass), getResourceString("PN_CORR_TYPE"), getResourceString("PD_CORR_TYPE"));

        add(new PropertyDescriptorEx("pvalue", beanClass), getResourceString("PN_PVALUE"),
                getResourceString("PD_PVALUE"));
        add(BeanUtil.createExpertDescriptor("threshold", beanClass), getResourceString("PN_THRESHOLD"), getResourceString("PD_THRESHOLD"));
        
        add(new PropertyDescriptorEx("fdr", beanClass), getResourceString("PN_CALCULATING_FDR"),
                getResourceString("PD_CALCULATING_FDR"));

        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class), "$experimentData/tablePath$ $correlationType$"),
                getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
    }

}
