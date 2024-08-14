package biouml.plugins.cellml;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;

/**
 * Writes diagram in SBML format.
 *
 * SbmlModelWriter can correctly process following types of diagram:
 * <ul>
 *   <li>SbmlDiagramType</li>
 *   <li>PathwaySimulationDiagramType. This standard BioUML diagram type is most closely
 *       corresponds to SBML diagram type</li>
 *   <li>PathwayDiagramType</li>
 * </ul>
 *
 * Reactant and product are obligatory elements of SBML reaction,
 * but it BioUML allows user to create reaction without reactants or products.
 * So to respect SBML requirements REACTANT_STUB or PRODUCT_STUB can be added into
 * reaction and model.
 *
 * <p>SbmlModelReader can recognise such stubs and remove them from model automatically.
 * Thus "_in_empty_set" (REACTANT_STUB) and "_out_empty_set" are reserved key words that should not be used
 * as specie names.
 *
 * @pending SName validation
 *
 * @pending warn if specie list or reaction list is empty.
 */
public class CellMLModelWriter
{
    protected Document document;
    protected List compartmentList;
    protected Logger log;
    protected File file;
    protected Diagram diagram;

    ////////////////////////////////////////////////////////////////////////////
    // Constructor and public methods
    //

    public CellMLModelWriter(File outFile) throws Exception
    {
        file = outFile;
        log = Logger.getLogger(CellMLModelWriter.class.getName());
    }

    public void write(Diagram sourceDiagram) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        write(sourceDiagram, transformerFactory.newTransformer());
    }

    public void write(Diagram sourceDiagram, Transformer transformer) throws Exception
    {
        if( sourceDiagram == null )
        {
            CellMLUtils.error("ERROR_DIAGRAM_NULL", new String[]{});
            throw new NullPointerException();
        }

        diagram = sourceDiagram;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.newDocument();

/*        Element element = null;
        document.createElement(SBML_ELEMENT);
        element.setAttribute(XMLNS_ATTR, SBML_XMLNS_VALUE);
        element.setAttribute(SBML_LEVEL_ATTR, SBML_LEVEL_VALUE);
        element.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE);

        document.appendChild(element);
*/
        //writeDiagram(element);
        writeDiagram(document.getDocumentElement());

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file);
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

     protected void writeDiagram(Element sbml) throws Exception
    {
/*        Element model = null;
        document.createElement(MODEL_ELEMENT);
        sbml.appendChild(model);

        // while model name is optional, then we try to use diagram title
        model.setAttribute(NAME_ATTR, diagram.getTitle());

        if( diagram.getKernel() instanceof DiagramInfo )
        {
            DiagramInfo info = (DiagramInfo)diagram.getKernel();
            writeNotes(model, info.getDescription());
        }

        //writeUnitList(element);
        writeCompartmentList(model);
        writeSpecieList(model);
        writeParameterList(model);
        //writeRuleList(element);
        writeReactionList(model);
*/
    }
     
    public static Document createDOM(Diagram diagram) throws Exception
    {
        if( diagram == null )
        {
            CellMLUtils.error("ERROR_DIAGRAM_NULL", new String[] {});
            throw new NullPointerException();
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        writeDiagram(document.getDocumentElement(), diagram);
        return document;
    }

    private static void writeDiagram(Element documentElement, Diagram diagram2)
    {
        // TODO Support write diagram
    }

}