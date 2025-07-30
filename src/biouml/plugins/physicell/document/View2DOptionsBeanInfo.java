package biouml.plugins.physicell.document;

import java.lang.reflect.Method;

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
        property( "slice" ).editor( SliderEditor.class ).value( "max", getMethod( "getMaxSlice")).value( "min", getMethod( "getMinSlice")).add();
        addWithTags( "sectionString", View2DOptions.SECTION_VALUES );
//        add( "drawGrid" );
    }
    
    private Method getMethod(String name) throws NoSuchMethodException
    {
        return beanClass.getMethod( name, (Class<?>[] )null );
    }
}