package biouml.plugins.physicell.web;

import biouml.plugins.physicell.document.PhysicellSimulationResult;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;

public class PhysicellResultOptionsProvider implements BeanProvider, CacheableBeanProvider
{
    @Override
    public Object getBean(String path)
    {
        System.out.println("QUERY: "+path  );
        Object cachedObj = WebServicesServlet.getSessionCache().getObject( path );
        PhysicellSimulationResult result = (PhysicellSimulationResult)cachedObj;
//        DataElementPath fullPath = DataElementPath.create( path );
//        PhysicellSimulationResult result = fullPath.getDataElement( PhysicellSimulationResult.class );
        return result.getOptions();
    }

}