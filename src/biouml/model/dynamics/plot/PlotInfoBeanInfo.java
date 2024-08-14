package biouml.model.dynamics.plot;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PlotInfoBeanInfo extends BeanInfoEx2<PlotInfo>
{
    public PlotInfoBeanInfo()
    {
        super(PlotInfo.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("title");
        add("active");
        add( "xAxisInfo" );
        add( "yAxisInfo" );
        add("xVariable");
        add("yVariables");
        add("experiments");
    }
}