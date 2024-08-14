package biouml.standard.simulation.plot;

import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.DocumentFactory;

import biouml.plugins.simulation.plot.PlotDocument;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;

public class PlotFactory implements DocumentFactory
{
    @Override
    public ApplicationDocument createDocument()
    {
        return null;
    }

    @Override
    public ApplicationDocument openDocument(String name)
    {
        ApplicationDocument document = null;
        DataElement de = CollectionFactory.getDataElement(name);
        if( de instanceof Plot )
        {
            document = new PlotDocument((Plot)de);
        }
        else if( de instanceof SimulationResult )
        {
            String plotName = de.getName() + "Plot";
            Plot plot = new Plot(null, plotName);
            plot.setXAutoRange(true);
            plot.setYAutoRange(true);
            document = new PlotDocument(plot);
            ( (PlotDocument)document ).setDefaultSimulationResult((SimulationResult)de);
        }
        return document;
    }
}
