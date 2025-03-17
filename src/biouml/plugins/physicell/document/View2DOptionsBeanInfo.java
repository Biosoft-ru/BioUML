package biouml.plugins.physicell.document;

import one.util.streamex.StreamEx;
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
        property( "substrate" ).tags(bean -> StreamEx.of(bean.getSubstrates()) ).add();
        add( "drawAgents" );
        add( "drawDensity" );
        add( "drawGrid" );
    }
}