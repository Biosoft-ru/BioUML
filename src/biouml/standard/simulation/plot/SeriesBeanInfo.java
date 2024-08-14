package biouml.standard.simulation.plot;

import ru.biosoft.graphics.PenEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SeriesBeanInfo extends BeanInfoEx2<Series>
{
    public SeriesBeanInfo()
    {
        super( Series.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "source" );
        addReadOnly( "xPath" );
        addReadOnly( "xVar" );
        addReadOnly( "yPath" );
        addReadOnly( "yVar" );        
        add( "legend" );
        add( "spec", PenEditor.class );
    }
}