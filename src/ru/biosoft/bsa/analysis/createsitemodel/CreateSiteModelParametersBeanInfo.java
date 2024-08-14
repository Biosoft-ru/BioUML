package ru.biosoft.bsa.analysis.createsitemodel;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CreateSiteModelParametersBeanInfo extends BeanInfoEx
{
    public CreateSiteModelParametersBeanInfo(Class beanClass)
    {
        super(beanClass, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = DataElementPathEditor.registerOutput("outputCollection", beanClass, SiteModelCollection.class);
        add(pde, getResourceString("PN_OUTPUT_COLLECTION"), getResourceString("PD_OUTPUT_COLLECTION"));
        add(new PropertyDescriptor("modelName", beanClass), getResourceString("PN_MODEL_NAME"), getResourceString("PD_MODEL_NAME"));
        
        addHidden(DataElementPathEditor.registerOutput("modelPath", beanClass, SiteModel.class));
    }
}
