package biouml.plugins.downloadext;

import ru.biosoft.access.ImporterFormat.DefaultImporterFormatEditor;
import ru.biosoft.access.repository.DataElementPathEditor;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class FTPUploadAnalysisParametersBeanInfo extends BeanInfoEx
{
    public FTPUploadAnalysisParametersBeanInfo()
    {
        super(FTPUploadAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("fileURL", beanClass), getResourceString("PN_URL"), getResourceString("PD_URL"));
        PropertyDescriptorEx pde = DataElementPathEditor.registerOutput("resultPath", beanClass, null);
        pde.setValue(DataElementPathEditor.ICON_ID, FTPUploadAnalysisParameters.class.getMethod("getOutputIcon"));
        add(pde, getResourceString("PN_IMPORT_RESULT_PATH"),
                getResourceString("PD_IMPORT_RESULT_PATH"));

        pde = new PropertyDescriptorEx( "importerFormat", beanClass );
        pde.setSimple( true );
        add( pde, DefaultImporterFormatEditor.class,
                getResourceString( "PN_IMPORT_FORMAT" ),
                getResourceString("PD_IMPORT_FORMAT"));
        pde = new PropertyDescriptorEx("importerProperties", beanClass);
        pde.setHidden(beanClass.getMethod("isPropertiesHidden"));
        add(pde, getResourceString("PN_IMPORT_PROPERTIES"), getResourceString("PD_IMPORT_PROPERTIES"));
    }
}
