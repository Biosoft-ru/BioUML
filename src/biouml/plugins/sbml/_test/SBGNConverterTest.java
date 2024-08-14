package biouml.plugins.sbml._test;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.plugins.sbml.SbmlDiagramTransformer;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;

/**
 *  SBML/SBGN converter test
 */
public class SBGNConverterTest extends AbstractBioUMLTest
{
    static final String dataDirectory = "../data/";
    static final String resourcesDirectory = "../data_resources/";

    /** Standard JUnit constructor */
    public SBGNConverterTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(SBGNConverterTest.class.getName());
        suite.addTest(new SBGNConverterTest("testConvertor"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test case
    //

    public void testConvertor() throws Exception
    {
        CollectionFactory.createRepository(dataDirectory);
        CollectionFactory.createRepository(resourcesDirectory);
        DataElement targetDiagramObj = CollectionFactory.getDataElement("databases/Biomodels/Diagrams/BIOMD0000000001.xml");
        assertNotNull("Can not load diagram", targetDiagramObj);
        Diagram targetDiagram = (Diagram)targetDiagramObj;

        String type = (String)targetDiagram.getAttributes().getValue(SbmlDiagramTransformer.BASE_DIAGRAM_TYPE);
        DiagramType sbmlType = (DiagramType)Class.forName(type).newInstance();

        SBGNConverterNew converter = new SBGNConverterNew();
        Diagram sbmlDiagram = converter.restoreSBML(targetDiagram, sbmlType);
        assertNotNull("Can not restore diagram", sbmlDiagram);

        Document dom = SbmlModelFactory.createDOM(sbmlDiagram);
        if( dom != null )
        {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(dom);
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            transformer.transform(source, result);

            System.out.println("///////////////////////////////////////////////////////////////////////");
            System.out.println(sw.toString());
            System.out.println("///////////////////////////////////////////////////////////////////////");
        }
    }
}
