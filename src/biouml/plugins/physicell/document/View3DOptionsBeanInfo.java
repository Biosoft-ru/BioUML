package biouml.plugins.physicell.document;

import java.lang.reflect.Method;

import ru.biosoft.util.bean.BeanInfoEx2;

public class View3DOptionsBeanInfo extends BeanInfoEx2<View3DOptions>
{
    public View3DOptionsBeanInfo()
    {
        super( View3DOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "head" ).editor( SliderEditor.class ).value( "max", 180 ).value( "min", -180 ).add();
        property( "pitch" ).editor( SliderEditor.class ).value( "max", 180 ).value( "min", -180 ).add();
        property( "xCutOff" ).editor( SliderEditor.class ).value( "max", getMethod( "getMaxX" ) ).value( "min", getMethod( "getMinX" ) )
                .add();
        property( "yCutOff" ).editor( SliderEditor.class ).value( "max", getMethod( "getMaxY" ) ).value( "min", getMethod( "getMinX" ) )
                .add();
        property( "zCutOff" ).editor( SliderEditor.class ).value( "max", getMethod( "getMaxZ" ) ).value( "min", getMethod( "getMinX" ) )
                .add();
        add("densityZ");
        add("densityY");
        add("densityX");
        property( "quality" ).tags( View3DOptions.QUALITIES ).add();
    }
    
    private Method getMethod(String name) throws NoSuchMethodException
    {
        return beanClass.getMethod( name, (Class<?>[] )null );
    }
}