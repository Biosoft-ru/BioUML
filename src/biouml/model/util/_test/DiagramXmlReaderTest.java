package biouml.model.util._test;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import biouml.model.Diagram;
import biouml.model.DiagramElementStyle;
import biouml.model.Module;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.XmlSerializationUtils;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Gene;
import biouml.standard.type.RNA;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;

public class DiagramXmlReaderTest extends TestCase
{
    Document parse(String xml) throws ParserConfigurationException, Exception, IOException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xml));
        Document doc = builder.parse(is);

        return doc;
    }

    public void testReadDPS_Simple() throws IntrospectionException, Exception
    {
        DynamicPropertySet registry = new DynamicPropertySetSupport();

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + "<test>\n"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>\n"
                + "<property name=\"p2\" short-description=\"p2\" type=\"String\" value=\"value 2\"/>\n"
                + "<property name=\"p3\" short-description=\"double property\" type=\"double\" value=\"3.456\"/>\n" + "</test>\n";

        Document doc = parse(xml);

        DynamicPropertySet dps = DiagramXmlReader.readDPS((Element)doc.getElementsByTagName("test").item(0), registry);

        assertNotNull(dps);
        assertNotNull(dps.getProperty("p1"));
        assertEquals(dps.getProperty("p1").getValue(), 10);

        assertNotNull(dps.getProperty("p2"));
        assertEquals(dps.getProperty("p2").getValue(), "value 2");

        assertNotNull(dps.getProperty("p3"));
        assertEquals(dps.getProperty("p3").getValue(), 3.456);
        assertEquals(dps.getProperty("p3").getShortDescription(), "double property");
    }

    public void testReadDPS_SimpleRegistry() throws IntrospectionException, Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + "<test>\n"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>\n"
                + "<propertyRef name=\"p2\" value=\"value 2\"/>\n" + "<propertyRef name=\"p3\" value=\"3.456\"/>\n" + "</test>\n";

        Document doc = parse(xml);

        DynamicPropertySet registry = new DynamicPropertySetSupport();
        //registry.add(new DynamicProperty("p1", Integer.class, null));
        registry.add(new DynamicProperty("p2", String.class, null));
        registry.add(new DynamicProperty("p3", Double.class, null));

        DynamicPropertySet dps = DiagramXmlReader.readDPS((Element)doc.getElementsByTagName("test").item(0), registry);

        assertNotNull(dps);
        assertNotNull(dps.getProperty("p1"));
        assertEquals(dps.getProperty("p1").getValue(), 10);
        assertEquals(dps.getProperty("p1").getShortDescription(), "p1");

        assertNotNull(dps.getProperty("p2"));
        assertEquals(dps.getProperty("p2").getValue(), "value 2");

        assertNotNull(dps.getProperty("p3"));
        assertEquals(dps.getProperty("p3").getValue(), 3.456);
    }

    public void testReadDPS_Complex() throws IntrospectionException, Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + "<test>\n"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>\n"
                + "<property name=\"p2\" short-description=\"p2\" type=\"String\" value=\"value 2\"/>\n"
                + "<property name=\"p3\" short-description=\"double property\" type=\"double\" value=\"3.456\"/>\n"
                + "<property name=\"p_composite\" short-description=\"composite_descr\" type=\"composite\">\n"
                + "<property name=\"pp1\" short-description=\"pp1\" type=\"int\" value=\"20\"/>\n"
                + "<property name=\"pp2\" short-description=\"pp2\" type=\"String\" value=\"value 2\"/>\n" + "</property>\n" + "</test>\n";

        Document doc = parse(xml);

        DynamicPropertySet registry = new DynamicPropertySetSupport();

        DynamicPropertySet dps = DiagramXmlReader.readDPS((Element)doc.getElementsByTagName("test").item(0), registry);

        assertNotNull(dps);
        assertNotNull(dps.getProperty("p1"));
        assertEquals(dps.getProperty("p1").getValue(), 10);
        assertEquals(dps.getProperty("p1").getShortDescription(), "p1");

        assertNotNull(dps.getProperty("p2"));
        assertEquals(dps.getProperty("p2").getValue(), "value 2");

        assertNotNull(dps.getProperty("p3"));
        assertEquals(dps.getProperty("p3").getValue(), 3.456);

        DynamicProperty compositeProperty = dps.getProperty("p_composite");
        assertNotNull(compositeProperty);

        assertNotNull( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp1"));
        assertNotNull( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp2"));

        assertEquals( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp1").getValue(), 20);
        assertEquals( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp2").getValue(), "value 2");
    }

    public void testReadDPS_compositeRegistry() throws IntrospectionException, Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + "<test>\n"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>\n"
                + "<propertyRef name=\"p2\" value=\"value 2\"/>\n" + "<propertyRef name=\"p3\" value=\"3.456\"/>\n"
                + "<propertyRef name=\"p_composite\">\n" + "<propertyRef name=\"pp1\" value=\"20\"/>\n"
                + "<propertyRef name=\"pp2\" value=\"value 2\"/>\n" + "</propertyRef>\n" + "</test>\n";

        Document doc = parse(xml);

        // create registry
        DynamicPropertySet registry = new DynamicPropertySetSupport();
        //registry.add(new DynamicProperty("p1", Integer.class, null));
        registry.add(new DynamicProperty("p2", String.class, null));
        DynamicProperty d_reg = new DynamicProperty("p3", Double.class, null);
        d_reg.setShortDescription("double property");
        registry.add(new DynamicProperty("p3", Double.class, null));

        // composite part
        DynamicProperty compositePropertyReg = new DynamicProperty("p_composite", DynamicPropertySet.class, null);
        compositePropertyReg.setShortDescription("composite_descr");
        registry.add(compositePropertyReg);
        DynamicPropertySet nestedReg = new DynamicPropertySetSupport();
        nestedReg.add(new DynamicProperty("pp1", Integer.class, null));
        nestedReg.add(new DynamicProperty("pp2", String.class, null));
        registry.setValue("p_composite", nestedReg);

        DynamicPropertySet dps = DiagramXmlReader.readDPS((Element)doc.getElementsByTagName("test").item(0), registry);

        assertNotNull(dps);
        assertNotNull(dps.getProperty("p1"));
        assertEquals(dps.getProperty("p1").getValue(), 10);
        assertEquals(dps.getProperty("p1").getShortDescription(), "p1");

        assertNotNull(dps.getProperty("p2"));
        assertEquals(dps.getProperty("p2").getValue(), "value 2");

        assertNotNull(dps.getProperty("p3"));
        assertEquals(dps.getProperty("p3").getValue(), 3.456);

        DynamicProperty compositeProperty = dps.getProperty("p_composite");
        assertNotNull(compositeProperty);
        assertTrue(compositeProperty.getValue() instanceof DynamicPropertySet);

        assertNotNull( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp1"));
        assertNotNull( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp2"));

        assertEquals( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp1").getValue(), 20);
        assertEquals( ( (DynamicPropertySet)compositeProperty.getValue() ).getProperty("pp2").getValue(), "value 2");
    }

    public void testReadDPS_Array() throws IntrospectionException, Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + "<test>\n"
                + "<property name=\"p2\" short-description=\"p2\" type=\"String\" value=\"value 2\"/>\n"
                + "<property name=\"p3\" short-description=\"p3\" type=\"array\" elementType=\"double\">\n" + "<item>123.22</item>\n"
                + "<item>345.55</item>\n" + "</property>\n" + "</test>\n";

        Document doc = parse(xml);

        DynamicPropertySet registry = new DynamicPropertySetSupport();
        DynamicPropertySet dps = DiagramXmlReader.readDPS((Element)doc.getElementsByTagName("test").item(0), registry);

        assertNotNull(dps);

        assertNotNull(dps.getProperty("p2"));
        assertEquals(dps.getProperty("p2").getValue(), "value 2");

        assertNotNull(dps.getProperty("p3"));
        Object value = dps.getProperty("p3").getValue();
        assertTrue(value.getClass().equals(Double[].class));
        Double[] array = (Double[])value;
        assertEquals(array[0], 123.22);
        assertEquals(array[1], 345.55);
    }

    public void testReadDPS_ArrayOfNonPrimitive() throws Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                + "<test>"
                + "<property elementType=\"biouml.model.DiagramElementStyle\" name=\"testArray\" short-description=\"testArray\" type=\"array\">"
                + "<item>" + "<property name=\"brush\" short-description=\"brush\" type=\"brush\" value=\"yellow\"/>"
                + "<property name=\"font\" short-description=\"font\" type=\"font\" value=\"Arial;0;14;red\"/>"
                + "<property name=\"pen\" short-description=\"pen\" type=\"pen\" value=\"4.0;black;Solid\"/>" + "</item>" + "<item>"
                + "<property name=\"brush\" short-description=\"brush\" type=\"brush\" value=\"white\"/>"
                + "<property name=\"font\" short-description=\"font\" type=\"font\" value=\"Arial;0;12;black\"/>"
                + "<property name=\"pen\" short-description=\"pen\" type=\"pen\" value=\"1.0;black;Solid\"/>" + "</item>" + "</property>"
                + "</test>";

        Document doc = parse( xml );

        DynamicPropertySet registry = new DynamicPropertySetSupport();
        DynamicPropertySet dps = DiagramXmlReader.readDPS( (Element)doc.getElementsByTagName( "test" ).item( 0 ), registry );

        assertNotNull( dps );

        assertNotNull( dps.getProperty( "testArray" ) );
        assertEquals( dps.getType( "testArray" ), DiagramElementStyle[].class );

        DiagramElementStyle[] value = (DiagramElementStyle[])dps.getValue( "testArray" );
        assertEquals( 2, value.length );
        DiagramElementStyle style = value[0];
        assertEquals( new Brush( Color.yellow ), style.getBrush() );
        assertEquals( new ColorFont( "Arial", 0, 14, Color.red ), style.getFont() );
        assertEquals( new Pen( 4 ), style.getPen() );
        style = value[1];
        DiagramElementStyle defaultStyle = new DiagramElementStyle();
        assertEquals( defaultStyle.getBrush(), style.getBrush() );
        assertEquals( defaultStyle.getFont(), style.getFont() );
        assertEquals( defaultStyle.getPen(), style.getPen() );
    }

    public void testReadDPS_CompositeArray() throws IntrospectionException, Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + "<test>\n"
                + "<property name=\"p3\" short-description=\"p3\" type=\"array\" elementType=\"composite\">\n"
                + "<property name=\"p3_0\" type=\"composite\">"
                + "<property name=\"p_ar1\" short-description=\"xxxxx\" type=\"double\" value=\"5.432\"/>"
                + "<property name=\"p_ar2\" short-description=\"rrrrr\" type=\"int\" value=\"5\"/>" + "</property>"
                + "<property name=\"p3_1\" short-description=\"yyyyy\" type=\"composite\">"
                + "<property name=\"pp_ar1\" short-description=\"zzzzz\" type=\"String\" value=\"QQQ\"/>" + "</property>\n"
                + "<property name=\"p3_2\" type=\"composite\">"
                + "<property name=\"p_ar3\" short-description=\"qqqq\" type=\"double\" value=\"9.88\"/>" + "</property>" + "</property>\n"
                + "</test>\n";

        Document doc = parse(xml);

        DynamicPropertySet registry = new DynamicPropertySetSupport();
        DynamicPropertySet dps = DiagramXmlReader.readDPS((Element)doc.getElementsByTagName("test").item(0), registry);

        assertNotNull(dps);

        assertNotNull(dps.getProperty("p3"));
        Object value = dps.getProperty("p3").getValue();
        Object[] array = (Object[])value;

        assertTrue(array[0] instanceof DynamicPropertySet);
        assertNotNull( ( (DynamicPropertySet)array[0] ).getProperty("p_ar1"));
        assertEquals( ( (DynamicPropertySet)array[0] ).getProperty("p_ar1").getValue(), 5.432);
        assertNotNull( ( (DynamicPropertySet)array[0] ).getProperty("p_ar2"));
        assertEquals( ( (DynamicPropertySet)array[0] ).getProperty("p_ar2").getValue(), 5);

        assertTrue(array[1] instanceof DynamicPropertySet);
        assertNotNull( ( (DynamicPropertySet)array[1] ).getProperty("pp_ar1"));
        assertEquals( ( (DynamicPropertySet)array[1] ).getProperty("pp_ar1").getValue(), "QQQ");

        assertTrue(array[2] instanceof DynamicPropertySet);
        assertNotNull( ( (DynamicPropertySet)array[2] ).getProperty("p_ar3"));
        assertEquals( ( (DynamicPropertySet)array[2] ).getProperty("p_ar3").getValue(), 9.88);
    }

    public void testReadDimension() throws Exception
    {
        Dimension d = XmlSerializationUtils.readDimension("20;60");
        assertEquals(20, d.width);
        assertEquals(60, d.height);
    }

    public void testReadBrush() throws Exception
    {
        Brush b = XmlSerializationUtils.readBrush("20;60;30");
        assertEquals(20,  ( (Color)b.getPaint() ).getRed());
        assertEquals(60,  ( (Color)b.getPaint() ).getGreen());
        assertEquals(30,  ( (Color)b.getPaint() ).getBlue());
        
        b = XmlSerializationUtils.readBrush( "#FFF:#000" );
        assertEquals(Color.WHITE, b.getColor());
        assertEquals(Color.BLACK, b.getColor2());
        
        b = XmlSerializationUtils.readBrush( "yellow:green:100" );
        assertEquals(Color.YELLOW, b.getColor());
        assertEquals(Color.GREEN, b.getColor2());
        assertEquals(100.0, b.getAngle(), 0.00001);
    }

    public void testReadPen() throws Exception
    {
        Pen pen = XmlSerializationUtils.readPen("2;20;60;30");
        assertEquals(2.0, pen.getWidth());
        assertEquals(new Color(20, 60, 30), pen.getColor());

        pen = XmlSerializationUtils.readPen("4;yellow");
        assertEquals(4.0, pen.getWidth());
        assertEquals(Color.YELLOW, pen.getColor());

        pen = XmlSerializationUtils.readPen("4;cyan");
        assertEquals(4.0, pen.getWidth());
        assertEquals(Color.CYAN, pen.getColor());

        pen = XmlSerializationUtils.readPen("4;some_color");
        assertEquals(4.0, pen.getWidth());
        assertEquals(Color.BLACK, pen.getColor());
        
        pen = XmlSerializationUtils.readPen("2;rgb(10,20,30);Dashed" );
        assertEquals(2.0, pen.getWidth());
        assertEquals(new Color(10,20,30), pen.getColor());
        assertEquals(9.0f, pen.getStroke().getDashArray()[0]);
        assertEquals(6.0f, pen.getStroke().getDashArray()[1]);
    }
    
    public void testReadFont() throws Exception
    {
        ColorFont font = XmlSerializationUtils.readFont( "Arial;1;14;black" );
        assertNotNull( "Cannot parse font", font );
        assertTrue( "Font family should Arial or Dialog", Arrays.asList( "Arial", "Dialog").contains( font.getFont().getFamily() ) );
        assertEquals(1, font.getFont().getStyle());
        assertEquals(14, font.getFont().getSize());
        assertEquals(Color.BLACK, font.getColor());
    }

    public void testReadingWithoutKernels() throws Exception
    {
        Environment.setClassLoading( new BiosoftClassLoading() );

        CollectionFactory.createRepository("../data/test/biouml/standard");
        String diagramCode = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                +"<dml appVersion=\"0.7.7\" version=\"0.7.7\">"
                +"<diagram diagramType=\"biouml.standard.diagram.PathwayDiagramType\">"
                +"<diagramInfo/>"
                +"<compartmentInfo color=\"255,255,255\" height=\"352\" kernel=\"stub/test\" kernel_type=\"info-diagram\" shape=\"0\" width=\"681\" x=\"0\" y=\"0\"/>"
                +"<nodes>"
                +"<node height=\"23\" kernel=\"Data/rna/E\" width=\"24\" x=\"136\" y=\"271\"/>"
                +"<node height=\"43\" kernel=\"Data/gene/G\" width=\"40\" x=\"20\" y=\"26\"/>"
                +"</nodes>"
                +"<edges>"
                +"<edge in=\"G\" kernel=\"Data/relation/gene\\sG-&gt;rna\\sE\" out=\"E\"/>"
                +"</edges>"
                +"</diagram>"
                +"</dml>";
        byte[] bytes = diagramCode.getBytes(StandardCharsets.ISO_8859_1);
        DataElementPath examplePath = DataElementPath.create("databases", "examples");
        List<String> kernelNames = new ArrayList<>();
        Diagram diagram = DiagramXmlReader.readDiagram("test", new ByteArrayInputStream(bytes), new DiagramInfo("test"),
                examplePath.getChildPath("Diagrams").getDataCollection(Diagram.class), examplePath.getDataElement(Module.class), kernelNames, null);
        Base kernel = diagram.get("E").getKernel();
        assertTrue(kernel instanceof Stub);
        kernel = diagram.get("G").getKernel();
        assertTrue(kernel instanceof Stub);
        kernel = diagram.get("gene/G->rna/E").getKernel();
        assertTrue(kernel instanceof Stub);
        assertEquals(3, kernelNames.size());
        
        diagram = DiagramXmlReader.readDiagram("test", new ByteArrayInputStream(bytes), new DiagramInfo("test"), examplePath.getChildPath("Diagrams").getDataCollection(Diagram.class),
                examplePath.getDataElement(Module.class));
        kernel = diagram.get("E").getKernel();
        assertTrue(kernel instanceof RNA);
        kernel = diagram.get("G").getKernel();
        assertTrue(kernel instanceof Gene);
        kernel = diagram.get("gene/G->rna/E").getKernel();
        assertTrue(kernel instanceof SemanticRelation);
    }
}
