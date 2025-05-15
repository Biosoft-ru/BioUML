
package biouml.standard.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.exception.InternalException;
import biouml.model.Diagram;
import biouml.model.util.DiagramXmlConstants;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;

/**
 * @author anna
 *
 */
public class StateTransformer extends AbstractFileTransformer<State>
{
    /**
     * Return class of output data element. Output data element stored in
     * transformed data collection.
     * 
     * @return Class of output data element.
     */
    @Override
    public Class<State> getOutputType()
    {
        return State.class;
    }

    @Override
    public State load(File input, String name, DataCollection<State> origin) throws Exception
    {
        try (FileInputStream fis = new FileInputStream( input ))
        {
            DiagramXmlReader reader = new DiagramXmlReader( name, fis, null );

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse( fis );
            Element stateElement = document.getDocumentElement();
            if( stateElement == null )
                throw new InternalException( "Document element missing" );
            String diagramName = stateElement.getAttribute( DiagramXmlConstants.DIAGRAM_REF_ATTR );
            if( diagramName == null )
                throw new BiosoftCustomException( null, "Parse error: " + DiagramXmlConstants.DIAGRAM_REF_ATTR + " is missing" );
            Diagram de = DataElementPath.create( diagramName ).getDataElement( Diagram.class );
            reader.setDiagram( de );
            State state = StateXmlSerializer.readXmlElement( origin, stateElement, de, reader );
            return state;
        }
    }

    @Override
    public void save(File output, State state) throws Exception
    {
        Diagram diagram = DiagramStateUtility.getNativeDiagram(state);
        if (diagram == null)
            throw new InternalException( "Diagram for state is missing" );
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        try (OutputStream stream = new FileOutputStream( output ))
        {
            DiagramXmlWriter diagramWriter = new DiagramXmlWriter( doc, diagram );
            Element stateElement = StateXmlSerializer.getStateXmlElement( state, doc, diagramWriter );
            stateElement.setAttribute( DiagramXmlConstants.DIAGRAM_REF_ATTR, DataElementPath.create( diagram ).toString() );
            doc.appendChild( stateElement );
            DOMSource source = new DOMSource( doc );
            Result result = new StreamResult( stream );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty( OutputKeys.MEDIA_TYPE, "text/xml" );
            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            transformer.transform( source, result );
        }
    }
}
