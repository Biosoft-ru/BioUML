package ru.biosoft.analysis;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.OptionEx;

public class FoldChangeParametersBeanInfo extends BeanInfoEx
{
    public FoldChangeParametersBeanInfo()
    {
        super(FoldChangeParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    public FoldChangeParametersBeanInfo(Class type, String name)
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
        add(new PropertyDescriptorEx("type", beanClass), getResourceString("PN_FOLD_CHANGE_TYPE"), getResourceString("PD_FOLD_CHANGE_TYPE"));
        addWithTags( "type", FoldChangeParameters.getTypeNames() );
        addWithTags( "inputLogarithmBase", Util.getLogarithmBaseNames() );
        addWithTags( "outputLogarithmBase", Util.getLogarithmBaseNames() );
        add(BeanUtil.createExpertDescriptor("threshold", beanClass), getResourceString("PN_THRESHOLD"), getResourceString("PD_THRESHOLD"));
        PropertyDescriptorEx pde = OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTablePath", beanClass, TableDataCollection.class), "$experimentData/tablePath$ fc");
        pde.setValue(DataElementPathEditor.ICON_ID, MicroarrayAnalysisParameters.class.getMethod("getIcon"));
        add(pde, getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
        pde = DataElementPathEditor.registerOutput("histogramOutput", beanClass, ImageDataElement.class, true);
        pde.setHidden(beanClass.getMethod("isHistogramHidden"));
        add(pde, getResourceString("PN_OUTPUT_HISTOGRAM"), getResourceString("PD_OUTPUT_HISTOGRAM"));
    }
}
