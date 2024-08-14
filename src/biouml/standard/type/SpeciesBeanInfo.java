package biouml.standard.type;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.support.SetAttributesCommand;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class SpeciesBeanInfo extends BeanInfoEx
{
    public SpeciesBeanInfo()
    {
        super(Species.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties ( ) throws Exception
    {
        super.initProperties ( );

        PropertyDescriptor pde;
        pde = new PropertyDescriptorEx ( "latinName", beanClass, "getLatinName", null );
        HtmlPropertyInspector.setDisplayName ( pde, "LN" );
        add ( pde, getResourceString ( "PN_SPECIES_LATINNAME" ),
                getResourceString ( "PD_SPECIES_LATINNAME" ) );

        pde = new PropertyDescriptorEx ( "commonName", beanClass );
        HtmlPropertyInspector.setDisplayName ( pde, "CN" );
        add ( pde, getResourceString ( "PN_SPECIES_COMMONNAME" ),
                getResourceString ( "PD_SPECIES_COMMONNAME" ) );

        pde = new PropertyDescriptorEx ( "abbreviation", beanClass );
        HtmlPropertyInspector.setDisplayName ( pde, "AB" );
        add ( pde, getResourceString ( "PN_SPECIES_ABBREVIATION" ),
                getResourceString ( "PD_SPECIES_ABBREVIATION" ) );

        pde = new PropertyDescriptorEx ( "description", beanClass );
        HtmlPropertyInspector.setDisplayName ( pde, "DE" );
        add ( pde, getResourceString ( "PN_SPECIES_DESCRIPTION" ),
                getResourceString ( "PD_SPECIES_DESCRIPTION" ) );

        pde = new PropertyDescriptorEx("attributes", beanClass.getMethod("getAttributes"), null);
        pde.setValue("commandClass", SetAttributesCommand.class);
        HtmlPropertyInspector.setDisplayName(pde, "AT");
        add(pde, getResourceString("PN_ATTRIBUTES"), getResourceString("PD_ATTRIBUTES"));
    }
}
