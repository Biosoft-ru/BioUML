package biouml.model.dynamics.plot;

import biouml.standard.simulation.plot.PlotBeanInfo.AxisTypeEditor;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class AxisInfoBeanInfo extends BeanInfoEx2<AxisInfo>
{
    public AxisInfoBeanInfo()
    {
        super( AxisInfo.class );
        //        setHideChildren( true );
        //        setCompositeEditor( "title;titleFont;axisType;autoRange;from;to", new java.awt.GridLayout( 1, 6 ) );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "axisType", AxisTypeEditor.class );
        add( "title" );
        add( "titleFont", FontEditor.class );
        add( "autoRange" );
        add( "from" );
        add( "to" );
        //            addReadOnly( "autoRange", "isAxisTypeLogarithmic" );
        //            addReadOnly( "from", "isAutoRange" );
        //            addReadOnly( "to", "isAutoRange" );
    }
}