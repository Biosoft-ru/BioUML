package biouml.plugins.physicell.document;

import ru.biosoft.util.bean.BeanInfoEx2;

public class View2DOptionsBeanInfo extends BeanInfoEx2<View2DOptions>
{
    public View2DOptionsBeanInfo()
    {
        super( View2DOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "slice" ).editor( SliderEditor.class ).value( "max", beanClass.getMethod( "getMaxSlice", (Class<?>[])null ) ).add();
        addWithTags( "sectionString", View2DOptions.SECTION_VALUES );
        add( "substrate" );
        add( "drawAgents" );
        add( "drawGrid" );
        add( "axes" );
        add( "statistics" );
    }
}