package biouml.standard.simulation.plot;

import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.access.core.DataElementPath;

public class PlotBeanProvider implements CacheableBeanProvider
{
    @Override
    public Object getBean(String path)
    {
        return new Plot( null, DataElementPath.create( path ).getName() );
    }
}
