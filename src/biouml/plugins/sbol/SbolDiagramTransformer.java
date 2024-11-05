package biouml.plugins.sbol;

import java.io.File;

import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLWriter;

import biouml.model.Diagram;
import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.PriorityTransformer;

public class SbolDiagramTransformer extends AbstractFileTransformer<Diagram> implements PriorityTransformer
{

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement de)
    {
        if ( FileDataElement.class.isAssignableFrom(inputClass) && (de instanceof Diagram) )
        {
            if ( ((Diagram) de).getType() instanceof SbolDiagramType )
                return 12;
        }
        return 0;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if ( name.endsWith(".rdf") || name.endsWith(".ttl") || name.endsWith(".nt") || name.endsWith(".jsonld") || name.endsWith(".rj") )
            return 10;
        else if ( name.endsWith(".xml") )
            return 5;
        return 0;
    }

    @Override
    public Class<? extends Diagram> getOutputType()
    {
        return Diagram.class;
    }

    @Override
    public Diagram load(File input, String name, DataCollection<Diagram> origin) throws Exception
    {
        Diagram result = SbolDiagramReader.readDiagram(input, name, origin);
        return result;
    }

    @Override
    public void save(File output, Diagram diagram) throws Exception
    {
        if ( diagram.getType() instanceof SbolDiagramType )
        {
            Object doc = diagram.getAttributes().getValue(SbolUtil.SBOL_DOCUMENT_PROPERTY);
            if ( doc != null && doc instanceof SBOLDocument )
            {
                SBOLWriter.write((SBOLDocument) doc, output);
            }
        }

    }



}
