package biouml.plugins.simulation.web;

import biouml.model.Diagram;
import biouml.plugins.simulation.SimulationEngineWrapper;
import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;

public class SimulationEngineBeanProvider implements CacheableBeanProvider
{
    @Override
    public SimulationEngineWrapper getBean(String path)
    {
        Diagram diagram = WebDiagramsProvider.getDiagram(path, true);
        return diagram == null ? null : new SimulationEngineWrapper(diagram);
    }
}
