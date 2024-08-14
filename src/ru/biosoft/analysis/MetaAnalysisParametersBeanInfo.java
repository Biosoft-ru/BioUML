package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MetaAnalysisParametersBeanInfo extends BeanInfoEx
{
    public MetaAnalysisParametersBeanInfo()
    {
        super(MetaAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInputMulti("tablePaths", beanClass, TableDataCollection.class),
                getResourceString("PN_META_INPUT_TABLES"), getResourceString("PN_META_INPUT_TABLES"));
        
        add(new PropertyDescriptorEx("outputType", beanClass),getResourceString("PN_UPDOWN_OUTPUT_TYPE"), getResourceString("PD_UPDOWN_OUTPUT_TYPE"));
        add(new PropertyDescriptorEx("pvalue", beanClass), getResourceString("PN_PVALUE"), getResourceString("PD_PVALUE"));
        
        add(new PropertyDescriptorEx("fdr", beanClass), getResourceString("PN_CALCULATING_FDR"),
                getResourceString("PD_CALCULATING_FDR"));
        
        add(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class),
                getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
    }

}
