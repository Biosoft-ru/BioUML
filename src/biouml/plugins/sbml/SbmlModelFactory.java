package biouml.plugins.sbml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.Map;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ApplicationUtils;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.composite.SbmlCompositeDiagramType;

/**
 * Utility class to read/write diagrams in SBML format.
 *
 * Factory design pattern is used to process different
 * levels and versions of SBML format.
 */
public class SbmlModelFactory
{
    protected static final Logger log = Logger.getLogger( SbmlModelFactory.class.getName() );


    public static SbmlModelReader getReader(Document document)
    {
        Element root = document.getDocumentElement();
        String level = root.getAttribute( SbmlConstants.SBML_LEVEL_ATTR );
        String version = root.getAttribute( SbmlConstants.SBML_VERSION_ATTR );

        SbmlModelReader reader = null;

        if( "1".equals( level ) )
        {
            if( "1".equals( version ) )
            {
                reader = new SbmlModelReader_11();
            }
            else
            {
                //                MessageBundle.warn(log, "WARN_READ_LEVEL_1_UNKNOWN_VERSION", new String[] {name, version});
                reader = new SbmlModelReader_12();
            }
        }
        else if( "2".equals( level ) )
        {
            if( "1".equals( version ) )
            {
                reader = new SbmlModelReader_21();
            }
            else if( "2".equals( version ) )
            {
                reader = new SbmlModelReader_22();
            }
            else if( "3".equals( version ) )
            {
                reader = new SbmlModelReader_23();
            }
            else if( "4".equals( version ) )
            {
                reader = new SbmlModelReader_24();
            }
            else if( "5".equals( version ) )
            {
                reader = new SbmlModelReader_25();
            }
        }
        else if( "3".equals( level ) )
        {
            if( "1".equals( version ) )
            {
                reader = new SbmlModelReader_31();
            }
            else if ("2".equals(version))
            {
                reader = new SbmlModelReader_32();
            }
        }
        return reader;
    }

    private static void showError(Document doc, String name)
    {
        Element root = doc.getDocumentElement();
        String level = root.getAttribute( SbmlConstants.SBML_LEVEL_ATTR );
        String version = root.getAttribute( SbmlConstants.SBML_VERSION_ATTR );
        MessageBundle.error( log, "ERROR_READ_UNKNOWN_LEVEL_VERSION", new String[] {name, level, version} );
    }

    public static Diagram readDiagram(Document document, String name, DataCollection origin, String diagramName, Map<String, String> newPaths) throws Exception
    {
        SbmlModelReader reader = getReader( document);
        if( reader == null )
        {
            showError( document, name );
            return null;
        }
        String dname = diagramName != null ? diagramName : name;
        reader.setNewPaths( newPaths );
        return reader.read( document, dname, origin );
    }

    public static Diagram readDiagram(File file, DataCollection origin, String diagramName) throws Exception
    {
        String name = ApplicationUtils.getFileNameWithoutExtension( file.getName() );
        return readDiagram( file, name, origin, diagramName );
    }
    
    public static Diagram readDiagram(File file, DataCollection origin, String diagramName, Map<String, String> newPaths) throws Exception
    {
        String name = ApplicationUtils.getFileNameWithoutExtension( file.getName() );
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( file );
        return readDiagram( document, name, origin, diagramName, newPaths );
    }

    /**
     * Method intended to be used from scripts. It reads SBML Diagram from file, 
     * creates diagram with the same name as file and no data collection origin.
     */
    public static Diagram readDiagram(String filePath, boolean layout) throws Exception
    {
        File f = new File( filePath );
        String name = ApplicationUtils.getFileNameWithoutExtension( f.getName() );
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( f );
        SbmlModelReader reader = getReader( document );
        reader.setShouldLayout( layout );
        return reader.read( document, name, null );
    }
    
    public static Diagram readDiagram(InputStream stream, String name, DataCollection origin, String diagramName) throws Exception
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( stream );
        return readDiagram( document, name, origin, diagramName, null );
    }

    public static Diagram readDiagram(File file, String name, DataCollection origin, String diagramName) throws Exception
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( file );
        return readDiagram( document, name, origin, diagramName, null );
    }

    public static Document createDOM(Diagram diagram/*, String level, String version*/) throws Exception
    {
        return createDOM( diagram, null );
    }

    public static Document createDOM(Diagram diagram, SbmlModelWriter writer/*, String level, String version*/) throws Exception
    {
        if( diagram == null )
        {
            MessageBundle.error( log, "ERROR_DIAGRAM_NULL", new String[] {} );
            throw new NullPointerException( MessageBundle.resources.getResourceString( "ERROR_DIAGRAM_NULL" ) );
        }
        if( writer == null )
        {
            writer = getWriter( diagram );
        }
        return writer.createDOM( diagram );
    }

    public static void writeDiagram(File file, Diagram sourceDiagram/*, String level, String version*/) throws Exception
    {
        writeDiagram( file, sourceDiagram, null );
    }

    public static void writeDiagram(File file, Diagram sourceDiagram, SbmlModelWriter writer/*, String level, String version*/)
            throws Exception
    {
        Document document = createDOM( sourceDiagram, writer/*, level, version*/);

        if( document != null )
        {
            writeDiagram( file, document );
        }
    }

    public static void writeDiagram(File file, Document document) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // set because default indent amount is zero

        DOMSource source = new DOMSource( document );
        try (OutputStream os = new FileOutputStream( file ))
        {
            StreamResult result = new StreamResult( os );
            transformer.transform( source, result );
        }
    }

    public static @Nonnull SbmlModelWriter getWriter(Diagram diagram)
    {
        SbmlModelWriter writer = null;
        DiagramType type = diagram.getType();

        if( type instanceof SbmlDiagramType_L3v2 || type instanceof SbmlCompositeDiagramType || type instanceof SbgnDiagramType )
        {
            writer = new SbmlModelWriter_32();
        }
        else if( type instanceof SbmlDiagramType_L3v1 )
        {
            writer = new SbmlModelWriter_31();
        }
        else if( type instanceof SbmlDiagramType_L2v4 )
        {
            writer = new SbmlModelWriter_24();
        }
        else if( type instanceof SbmlDiagramType_L2v3 )
        {
            writer = new SbmlModelWriter_23();
        }
        else if( type instanceof SbmlDiagramType_L2v2 )
        {
            writer = new SbmlModelWriter_22();
        }
        else if( type instanceof SbmlDiagramType_L2 )
        {
            writer = new SbmlModelWriter_21();
        }
        else if( type instanceof SbmlDiagramType )
        {
            writer = new SbmlModelWriter_32();
        }
        else if( type instanceof SbmlDiagramType_L1 )
        {
            writer = new SbmlModelWriter_11();
        }
        if( writer == null )
        {
            throw new InternalException("Unsupported diagram type passed to SbmlModelFactory: " + diagram.getType() + " (diagram: "
                    + diagram.getCompletePath() + ")");
        }
        return writer;
    }
}
