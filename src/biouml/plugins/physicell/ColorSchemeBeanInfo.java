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

        add( "outerColor" );
        add( "borderPen" );
        add( "innerColor" );
        add( "innerBorderPen" );
    }
}