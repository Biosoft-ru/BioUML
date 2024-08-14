package biouml.standard.state;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.InternalException;
import ru.biosoft.access.history.HistoryElement;
import ru.biosoft.access.history.DiffManager;

/**
 * History support for diagram objects
 */
public class DiagramDiffManager implements DiffManager
{
    @Override
    public int getPriority(Class elementType)
    {
        if( Diagram.class.isAssignableFrom(elementType) )
            return 10;
        return 0;
    }

    @Override
    public void fillDifference(HistoryElement historyElement, DataElement oldElement, DataElement newElement) throws Exception
    {
        State state = DiagramStateUtility.createState((Diagram)newElement, (Diagram)oldElement, historyElement.getName());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Diagram diagram = DiagramStateUtility.getNativeDiagram(state);
        if (diagram == null)
            throw new InternalException( "Diagram for state is missing" );
        
        DiagramXmlWriter diagramWriter = new DiagramXmlWriter(doc, diagram);
        Element stateElement = StateXmlSerializer.getStateXmlElement(state, doc, diagramWriter);
        doc.appendChild(stateElement);
        DOMSource source = new DOMSource(doc);
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
        historyElement.setData(stringWriter.toString());
    }

    @Override
    public Object parseDifference(DataElement de, HistoryElement historyElement) throws Exception
    {
        ByteArrayInputStream bs = new ByteArrayInputStream(historyElement.getData().getBytes());
        DiagramXmlReader reader = new DiagramXmlReader(de.getName(), bs, null);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(bs);
        Element stateElement = document.getDocumentElement();
        reader.setDiagram((Diagram)de);
        State state = StateXmlSerializer.readXmlElement(stateElement, (Diagram)de, reader);
//        ( (Diagram)de ).removeState(state);
        return state;
    }

    @Override
    public DataElement applyDifference(DataElement de, HistoryElement[] historyElements) throws Exception
    {
        Diagram result = ( (Diagram)de ).clone(de.getOrigin(), de.getName());
        for( int i=0; i < historyElements.length-1; i++ )
        {
            HistoryElement he = historyElements[i];
            DiagramStateUtility.redoEdits(result, ( (State)he.getDataObj(result, this) ).getStateUndoManager().getEdits());
        }
        return result;
    }

    @Override
    public DataElement getDifferenceElement(DataElement first, DataElement second) throws Exception
    {
        State state = DiagramStateUtility.createState((Diagram)first, (Diagram)second, "");
        Diagram result = ( (Diagram)first ).clone(first.getOrigin(), first.getName());
        DiagramStateUtility.applyState(result, state, state.getName());
        return result;
    }
}
