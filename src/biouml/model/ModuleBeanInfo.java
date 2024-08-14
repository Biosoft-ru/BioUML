package biouml.model;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ModuleBeanInfo extends BeanInfoEx
{

    public ModuleBeanInfo ( )
    {
        this ( Module.class, "DATABASE", MessageBundle.class.getName ( ) );
    }

    protected ModuleBeanInfo ( Class beanClass, String key, String messageBundle )
    {
        super ( beanClass, messageBundle );
        if ( key != null && messageBundle != null )
        {
            beanDescriptor.setDisplayName ( getResourceString ( "CN_" + key ) );
            beanDescriptor.setShortDescription ( getResourceString ( "CD_" + key ) );
        }
    }

    @Override
    public void initProperties ( ) throws Exception
    {
        super.initProperties ( );
        
        super.initResources ( MessageBundle.class.getName ( ) );
        
        add(new PropertyDescriptorEx( "name", beanClass, "getName", null),
            getResourceString("PN_DATABASE_NAME"),
            getResourceString("PD_DATABASE_NAME"));
        add(new PropertyDescriptorEx( "version", beanClass, "getVersion", null),
            getResourceString("PN_DATABASE_VERSION"),
            getResourceString("PD_DATABASE_VERSION"));
        add(new PropertyDescriptorEx( "size", beanClass, "getSize", null),
            getResourceString("PN_DATABASE_SIZE"),
            getResourceString("PD_DATABASE_SIZE"));
        addHidden(new PropertyDescriptorEx("descriptionHTML", beanClass, "getDescriptionHTML", "setDescription"));
    }
    
}
