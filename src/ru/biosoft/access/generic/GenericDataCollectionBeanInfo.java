package ru.biosoft.access.generic;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.application.Application;

public class GenericDataCollectionBeanInfo extends BeanInfoEx
{
    public GenericDataCollectionBeanInfo()
    {
        super(GenericDataCollection.class, MessageBundle.class.getName() );
        
        beanDescriptor.setShortDescription( getResourceString("CD_GENERIC_DC") );
        try
        {
            setDisplayNameMethod(GenericDataCollection.class.getMethod("getName"));
        }
        catch( Exception e )
        {
            beanDescriptor.setDisplayName     ( getResourceString("CN_GENERIC_DC") );
        }
    }
    
    @Override
    public void initProperties() throws Exception
    {
        if(Application.getApplicationFrame() != null)
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("preferedTableImplementation", beanClass);
            pde.setPropertyEditorClass(TableImplementationRecord.TableImplementationSelector.class);
            pde.setSimple(true);
            add(pde, getResourceString("PN_TABLE_IMPLEMENTATION"), getResourceString("PD_TABLE_IMPLEMENTATION"));
            
            add(new PropertyDescriptorEx("databaseURL", beanClass), getResourceString("PN_DATABASE_URL"), getResourceString("PD_DATABASE_URL"));
            add(new PropertyDescriptorEx("databaseUser", beanClass), getResourceString("PN_DATABASE_USER"), getResourceString("PD_DATABASE_USER"));
            add(new PropertyDescriptorEx("databasePassword", beanClass), getResourceString("PN_DATABASE_PASSWORD"), getResourceString("PD_DATABASE_PASSWORD"));
        }
        
        addHidden(new PropertyDescriptorEx("description", beanClass));
    }

}
