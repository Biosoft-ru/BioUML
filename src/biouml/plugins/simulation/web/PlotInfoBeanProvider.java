package biouml.plugins.simulation.web;

import biouml.model.Diagram;
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
        Diagram diagram = WebDiagramsProvider.getDiagram(path, true);
        if( diagram == null )
            return null;

        Object plotObject = diagram.getAttributes().getValue("Plots");
        if( plotObject instanceof PlotsInfo )
        {
            return (PlotsInfo)plotObject;
        }
        else
        {
            PlotsInfo plot = new PlotsInfo(diagram.getRole(EModel.class));
            diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient("Plots", PlotsInfo.class, plot));
            return plot;
        }
    }
}
