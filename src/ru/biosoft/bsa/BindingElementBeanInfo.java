package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class BindingElementBeanInfo extends BeanInfoEx
{
    public BindingElementBeanInfo()
    {
        super( BindingElement.class, BSAMessageBundle.class.getName() );

        beanDescriptor.setDisplayName( getResourceString( "CN_BINDING" ) );
        beanDescriptor.setShortDescription( getResourceString( "CD_BINDING" ) );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "name", beanClass.getMethod( "getName", new Class<?>[]{} ), null ),
                getResourceString( "PN_BINDING_NAME" ), getResourceString( "PD_BINDING_NAME" ) );
        PropertyDescriptorEx pde = new PropertyDescriptorEx( "factors", beanClass.getMethod( "getFactors", new Class<?>[]{} ), null );
        pde.setHidden(true);
        add(pde, getResourceString("PN_BINDING_FACTORS"), getResourceString("PD_BINDING_FACTORS"));
        add( new PropertyDescriptorEx( "factorNames", beanClass.getMethod( "getFactorNames", new Class<?>[]{} ), null ),
                getResourceString( "PN_BINDING_FACTORS" ), getResourceString( "PD_BINDING_FACTORS" ) );
    }
}
