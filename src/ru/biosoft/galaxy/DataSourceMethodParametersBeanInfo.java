package ru.biosoft.galaxy;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author lan
 *
 */
public class DataSourceMethodParametersBeanInfo extends BeanInfoEx
{
    public DataSourceMethodParametersBeanInfo()
    {
        super(DataSourceMethodParameters.class, MessageBundle.class.getName());
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("outputPath", beanClass, FolderCollection.class), MessageBundle.PN_OUTPUT_FOLDER, MessageBundle.PD_OUTPUT_FOLDER);
        PropertyDescriptorEx pde = new PropertyDescriptorEx("urlBuilder", beanClass, "getUrlBuilder", null);
        pde.setPropertyEditorClass(DataSourceURLRenderer.class);
        pde.setDisplayName(DataSourceMethodParameters.class.getMethod("getURLDisplayName"));
        pde.setShortDescription(MessageBundle.PD_URL);
        pde.setSimple(true);
        add(pde);
    }
}
