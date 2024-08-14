package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SelectColumnsParametersBeanInfo extends BeanInfoEx
{
    public SelectColumnsParametersBeanInfo()
    {
        super(SelectColumnsParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    public SelectColumnsParametersBeanInfo(Class beanClass, String resourceBundleName)
    {
        super(beanClass, resourceBundleName);
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("columnGroup", beanClass), getResourceString("PN_SLICE_COLUMNS"),
                getResourceString("PD_SLICE_COLUMNS"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("output", beanClass, TableDataCollection.class),
                "$columnGroup/tablePath$ selected"), getResourceString("PN_OUTPUT_TABLE"), getResourceString("PD_OUTPUT_TABLE"));
    }
}
