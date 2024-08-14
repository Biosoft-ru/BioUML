package biouml.standard.simulation.plot;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

import biouml.standard.simulation.plot.Plot.AxisType;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * Definition of common properties for simulation engine
 */
public class PlotBeanInfo extends BeanInfoEx2<Plot>
{
    public PlotBeanInfo()
    {
        super(Plot.class);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        property(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null)).htmlDisplayName( "ID" ).add();        
        property("title").htmlDisplayName( "PT" ).add();
        property("description").htmlDisplayName( "DE" ).add();
        property("xTitle").htmlDisplayName( "XT" ).add();
        property("xTitleFont").htmlDisplayName( "XTF" ).editor( FontEditor.class ).add();
        property("xTickLabelFont").htmlDisplayName( "XTLF" ).editor( FontEditor.class ).add();
        property("xAxisType").htmlDisplayName( "XAT" ).editor(  AxisTypeEditor.class ).add();
        property("xAutoRange").htmlDisplayName( "XAR" ).readOnly( "isXAxisTypeLogarithmic" ).add();
        property("xFrom").htmlDisplayName( "XFR" ).readOnly( "isXAutoRange" ).add();
        property("xTo").htmlDisplayName( "XTO" ).readOnly( "isXAutoRange" ).add();
        
        property("yTitle").htmlDisplayName( "YT" ).add();
        property("yTitleFont").htmlDisplayName( "YTF" ).editor( FontEditor.class ).add();
        property("yTickLabelFont").htmlDisplayName( "YTLF" ).editor( FontEditor.class ).add();
        property("yAxisType").htmlDisplayName( "YAT" ).editor(  AxisTypeEditor.class ).add();
        property("yAutoRange").htmlDisplayName( "YAR" ).readOnly( "isYAxisTypeLogarithmic" ).add();
        property("yFrom").htmlDisplayName( "YFR" ).readOnly( "isYAutoRange" ).add();
        property("yTo").htmlDisplayName( "YTO" ).readOnly( "isYAutoRange" ).add();
        property( "defaultSource" ).hidden().htmlDisplayName( "DS" ).add();
    }


    public static class AxisTypeEditor extends StringTagEditorSupport
    {
        @Override
        public String[] getTags()
        {
            return AxisType.getAxisTypes().toArray( new String[0] );
        }

    }
}
