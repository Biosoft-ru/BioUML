package biouml.plugins.physicell;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.ModelXmlReader;
import ru.biosoft.access.core.DataCollection;
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
}