package biouml.plugins.physicell.document;

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
        property("time").editor( SliderEditor.class ).value( "max", beanClass.getMethod( "getMaxTime", (Class<?>[] )null ) ).value( "min", 0 ).add();
        property( "head" ).editor(SliderEditor.class).value( "max", 180 ).value( "min", -180 ).add();
        property( "pitch" ).editor(SliderEditor.class).value( "max", 180 ).value( "min", -180 ).add();
        property( "xCutOff" ).editor(SliderEditor.class).value( "max", beanClass.getMethod( "getMaxX", (Class<?>[] )null ) ).value( "min", 0 ).add();
        property( "yCutOff" ).editor(SliderEditor.class).value( "max", beanClass.getMethod( "getMaxY", (Class<?>[] )null ) ).value( "min", 0 ).add();
        property( "zCutOff" ).editor(SliderEditor.class).value( "max", beanClass.getMethod( "getMaxZ", (Class<?>[] )null ) ).value( "min", 0 ).add();
        addWithTags("quality", View3DOptions.QUALITIES);
        add("axes");
        add("statistics");
    }
}