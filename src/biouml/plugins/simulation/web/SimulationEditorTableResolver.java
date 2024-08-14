package biouml.plugins.simulation.web;

import biouml.model.Diagram;
import biouml.plugins.simulation.document.InteractiveSimulation;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.table.access.TableResolver;

public class SimulationEditorTableResolver extends TableResolver
{
    private String simulationName = null;
    public SimulationEditorTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        simulationName = arguments.getString( "simulation" );
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        if( de instanceof Diagram )
        {
            Object cachedObj = WebServicesServlet.getSessionCache().getObject( simulationName );
            if( cachedObj instanceof InteractiveSimulation )
            {
                InteractiveSimulation simulation = (InteractiveSimulation)cachedObj;
                return simulation.getInputParameters();
            }
        }
        return null;
    }
}
