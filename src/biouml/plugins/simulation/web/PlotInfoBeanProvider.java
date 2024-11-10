package biouml.plugins.simulation.web;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.util.DPSUtils;

public class PlotInfoBeanProvider implements BeanProvider
{
    @Override
    public PlotsInfo getBean(String path)
    {
        Diagram diagram = WebDiagramsProvider.getDiagram( path, true );
        if( diagram == null )
            return null;

        Object plotObject = diagram.getAttributes().getValue( "Plots" );
        if( plotObject instanceof PlotsInfo )
        {
            return (PlotsInfo)plotObject;
        }
        else
        {
            Role role = diagram.getRole();
            if( role instanceof EModel )
            {
                PlotsInfo plot = new PlotsInfo((EModel)role);
                diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( "Plots", PlotsInfo.class, plot ) );
                return plot;
            }
        }
        return null;
    }
}
