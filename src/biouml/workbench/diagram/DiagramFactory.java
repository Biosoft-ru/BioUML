package biouml.workbench.diagram;

import ru.biosoft.access.core.DataElementPath;
import biouml.model.Diagram;
import biouml.standard.diagram.DiagramUtility;

import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.DocumentFactory;

public class DiagramFactory implements DocumentFactory
{
    @Override
    public ApplicationDocument createDocument()
    {
        return new DiagramDocument(null);
    }

    @Override
    public ApplicationDocument openDocument(String name)
    {
        Diagram dc = DataElementPath.create(name).getDataElement(Diagram.class);
        return DiagramUtility.isComposite(dc) ? new CompositeDiagramDocument(dc) : new DiagramDocument(dc);
    }
}
