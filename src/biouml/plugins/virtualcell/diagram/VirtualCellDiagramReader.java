package biouml.plugins.virtualcell.diagram;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.ModelXmlReader;
import ru.biosoft.access.core.DataCollection;
import org.w3c.dom.Element;

public class VirtualCellDiagramReader extends DiagramXmlReader
{
    @Override
    protected ModelXmlReader createModelReader(Diagram diagram)
    {
        return new VirtualCellModelReader( diagram );
    }

    @Override
    public Diagram readDiagram(Element diagramElement, DiagramType diagramType, DataCollection origin) throws Exception
    {
        Diagram result = super.readDiagram( diagramElement, diagramType, origin );
        return result;
    }
}