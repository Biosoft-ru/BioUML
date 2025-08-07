package biouml.plugins.physicell;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.ModelXmlReader;
import biouml.plugins.physicell.plot.PlotProperties;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.DPSUtils;

import java.util.Map;

import org.w3c.dom.Element;

public class PhysicellDiagramReader extends DiagramXmlReader
{
    @Override
    protected ModelXmlReader createModelReader(Diagram diagram)
    {
        return new PhysicellModelReader( diagram );
    }

    @Override
    public Diagram readDiagram(Element diagramElement, DiagramType diagramType, DataCollection origin) throws Exception
    {
        Diagram result = super.readDiagram( diagramElement, diagramType, origin );

        result.recursiveStream().map( de -> de.getRole() ).select( CellDefinitionProperties.class ).forEach( cdp -> {
            cdp.update();
        } );

        return result;
    }
    
    @Override
    public void readPlotsInfo(Element element, Diagram diagram, Map<String, String> newPaths)
    {
        PlotProperties properties;
        Object pbj = readElement( element, PlotProperties.class );
        if( pbj != null )
            properties = (PlotProperties)pbj;
        else
            properties = new PlotProperties();
        properties.setModel( diagram.getRole( MulticellEModel.class ) );
        diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( "Plots", PlotProperties.class, properties ) );
    }
}