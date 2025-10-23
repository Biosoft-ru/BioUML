package biouml.plugins.physicell.document;

import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ViewOptionsBeanInfo extends BeanInfoEx2<ViewOptions>
{
    public ViewOptionsBeanInfo()
    {
        super( ViewOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "time" ).editor( SliderEditor.class ).value( "max", beanClass.getMethod( "getMaxTime", (Class<?>[])null ) )
                .value( "min", 0 ).add();
        add("timeStep");
        add("2D");
        addHidden("options2D", "is3D");
        addHidden("options3D", "is2D");
        add( "statistics" );
        add( "axes" );
        add("cells");
        add("drawNuclei");
        property( "substrate" ).tags(bean -> StreamEx.of(bean.getSubstrates()) ).add();
        add( "drawDensity" );
        add("densityColor");
        add("statisticsX");
        add("statisticsY");
        add("StatisticsFont", FontEditor.class );
        add("statisticsBackground");
        add("fps");
        add("result");
    }
}