package biouml.plugins.research.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;

public class WorkflowParametersProvider implements BeanProvider, CacheableBeanProvider
{
    private static final Logger log = Logger.getLogger( WorkflowParametersProvider.class.getName() );

    @Override
    public Object getBean(String path)
    {
        Diagram diagram = WebDiagramsProvider.getDiagram(path, true);
        try
        {
            return WorkflowItemFactory.getWorkflowParameters(diagram);
        }
        catch( Exception e )
        {
            log.log( Level.WARNING, "Can not get workflow parameters for " + path, e );
            return null;
        }
    }

}
