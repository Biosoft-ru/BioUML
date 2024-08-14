package ru.biosoft.analysis;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import one.util.streamex.StreamEx;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class UpDownIdentificationParametersBeanInfo extends BeanInfoEx2<UpDownIdentificationParameters>
{
    public UpDownIdentificationParametersBeanInfo()
    {
        super(UpDownIdentificationParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    public UpDownIdentificationParametersBeanInfo(Class type, String name)
    {
        super(type, name);
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("experimentData", beanClass), getResourceString("PN_EXPERIMENT"), getResourceString("PD_EXPERIMENT"));
        add(new PropertyDescriptorEx("controlData", beanClass), getResourceString("PN_CONTROL"), getResourceString("PD_CONTROL"));
        property( "method" ).tags( bean -> StreamEx.of( bean.getMethodNames() ) ).add();
        addWithTags( "inputLogarithmBase", Util.getLogarithmBaseNames() );
        addWithTags( "outputType", UpDownIdentificationParameters.getOutputTypes() );
        add(new PropertyDescriptorEx("pvalue", beanClass), getResourceString("PN_PVALUE"), getResourceString("PD_PVALUE"));
        add(BeanUtil.createExpertDescriptor("threshold", beanClass), getResourceString("PN_THRESHOLD"), getResourceString("PD_THRESHOLD"));
        add(new PropertyDescriptorEx("fdr", beanClass), getResourceString("PN_CALCULATING_FDR"), getResourceString("PD_CALCULATING_FDR"));
        PropertyDescriptorEx pde = OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class), "$experimentData/tablePath$ up-down");
        pde.setValue(DataElementPathEditor.ICON_ID, MicroarrayAnalysisParameters.class.getMethod("getIcon"));
        add(pde, getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
    }
}
