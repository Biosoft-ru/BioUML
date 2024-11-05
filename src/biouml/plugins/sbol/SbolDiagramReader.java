package biouml.plugins.sbol;

import java.io.File;
import java.util.Set;

import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftParseException;

public class SbolDiagramReader
{
    public static Diagram readDiagram(File file, String name, DataCollection<?> origin) throws Exception
    {
        Diagram result = new SbolDiagramType().createDiagram(origin, name, new DiagramInfo(name));
        result.setNotificationEnabled(false);
        SBOLDocument doc = null;
        try
        {
            doc = SBOLReader.read(file);
        }
        catch (SBOLValidationException e)
        {
            throw new BiosoftParseException(e, file.getName());
        }
        if ( doc != null )
        {
            fillDiagramByDocument(doc, result);
            result.getAttributes().add(new DynamicProperty(SbolUtil.SBOL_DOCUMENT_PROPERTY, SBOLDocument.class, doc));

        }
        result.setNotificationEnabled(true);
        return result;
    }

    private static void fillDiagramByDocument(SBOLDocument doc, Diagram diagram)
    {
        Set<ComponentDefinition> components = doc.getComponentDefinitions();
        for ( ComponentDefinition cd : components )
        {
            Node compNode = new Node(diagram, new SbolBase(cd));
            diagram.put(compNode);
        }
    }
}
