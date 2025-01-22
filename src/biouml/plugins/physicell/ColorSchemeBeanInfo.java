package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ColorSchemeBeanInfo extends BeanInfoEx2<ColorScheme>
{
    public ColorSchemeBeanInfo()
    {
        super( ColorScheme.class );
    }

    @Override
    public void initProperties()
    {
        add( "name" );
        add( "color" );
        add("border");
        property( "borderColor" ).readOnly( "noBorder" ).add();
        add("core");
        property( "coreColor" ).readOnly( "noCore" ).add();
        property( "coreBorderColor" ).readOnly( "noCore" ).add();
    }
}