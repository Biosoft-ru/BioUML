package biouml.plugins.sbol;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLReader;
import org.sbolstandard.core2.SBOLValidationException;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Import SBOL RDF graphs in one of the formats
 * RDF/XML (File extension: rdf)
 * Turtle (File extension: ttl)
 * N-Triples (File extension: nt)
 * JSON-LD (File extension: jsonld)
 * RDF/JSON (File extension: rj)
 **/

public class SbolImporter implements DataElementImporter
{
    private SbolImportProperties properties = null;
    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if ( parent.isAcceptable(Diagram.class) )
            if ( file == null )
                return ACCEPT_HIGHEST_PRIORITY;
            else
            {
                String lcname = file.getName().toLowerCase();
                if ( lcname.endsWith(".sbol") || lcname.endsWith(".rdf") || lcname.endsWith(".ttl") || lcname.endsWith(".nt") || lcname.endsWith(".jsonld") || lcname.endsWith(".rj") )
                    return ACCEPT_HIGHEST_PRIORITY;
                else if ( lcname.endsWith(".xml") )
                    return ACCEPT_MEDIUM_PRIORITY;
            }
        ;
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(DataCollection<?> parent, File file, String diagramName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        if ( jobControl != null )
            jobControl.functionStarted();
        try (FileInputStream in = new FileInputStream(file); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            if ( properties.getDiagramName() == null )
                throw new Exception("Please specify diagram name.");

            Diagram diagram = SbolDiagramReader.readDiagram(file, properties.getDiagramName(), parent);

            if ( jobControl != null )
                jobControl.functionFinished();
            CollectionFactoryUtils.save(diagram);
            return diagram;
        }
        catch (Exception e)
        {
            if ( jobControl != null )
                jobControl.functionTerminatedByError(e);
            throw e;
        }
    }

    private Diagram read(File file, DataCollection<?> parent, String diagramName) throws Exception
    {
        Diagram result = new SbolDiagramType().createDiagram(parent, diagramName, new DiagramInfo(diagramName));
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

            //doc.getTopLevels()
            Set<ComponentDefinition> components = doc.getComponentDefinitions();
            for ( ComponentDefinition cd : components )
            {
                Node compNode = new Node(result, new SbolBase(cd));
                result.put(compNode);
            }
        }
        result.setNotificationEnabled(true);
        return result;
    }

    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        if ( properties == null )
            properties = new SbolImportProperties();
        if ( elementName != null )
            properties.setDiagramName(elementName);
        else if ( file != null )
            properties.setDiagramName(file.getName());
        return properties;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Diagram.class;
    }

    @Override
    public boolean init(Properties arg0)
    {
        return true;
    }

}
