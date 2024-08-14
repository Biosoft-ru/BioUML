package ru.biosoft.analysiscore;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.workbench.editors.FileSelector;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ImportAnalysisParametersBeanInfo extends BeanInfoEx
{
    public ImportAnalysisParametersBeanInfo()
    {
        super(ImportAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("file", beanClass);
        pde.setValue(FileSelector.TITLE, "File to import");
        pde.setPropertyEditorClass(FileSelector.class);
        pde.setSimple(true);
        add(pde, getResourceString("PN_IMPORT_FILE"), getResourceString("PD_IMPORT_FILE"));
        
        pde = DataElementPathEditor.registerOutput("resultPath", beanClass, null);
        pde.setValue(DataElementPathEditor.ICON_ID, ImportAnalysisParameters.class.getMethod("getOutputIcon"));
        add(pde, getResourceString("PN_IMPORT_RESULT_PATH"), getResourceString("PD_IMPORT_RESULT_PATH"));
        
        pde = new PropertyDescriptorEx("properties", beanClass);
        pde.setHidden(beanClass.getMethod("isPropertiesHidden"));
        add(pde, getResourceString("PN_IMPORT_PROPERTIES"), getResourceString("PD_IMPORT_PROPERTIES"));
    }
}
