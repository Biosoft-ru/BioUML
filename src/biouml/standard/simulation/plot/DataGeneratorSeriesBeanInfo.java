
package biouml.standard.simulation.plot;

import ru.biosoft.graphics.PenEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author anna
 */
public class DataGeneratorSeriesBeanInfo extends BeanInfoEx2<DataGeneratorSeries>
{
    public DataGeneratorSeriesBeanInfo()
    {
        super( DataGeneratorSeries.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "source" );
        addReadOnly( "xVar" );
        addReadOnly( "yVar" );
        add( "legend" );
        add( "spec", PenEditor.class );
    }
}