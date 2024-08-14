package biouml.model.util._test;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ru.biosoft.graph.CompartmentCrossCostGridLayouter;
import ru.biosoft.graph.DiagonalPathLayouter;
import ru.biosoft.graph.FastGridLayouter;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.OrthogonalPathLayouter;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramFilter;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.expression.ExpressionFilter;
import biouml.plugins.expression.ExpressionFilterProperties;
import biouml.model.DiagramElementStyle;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.filter.BiopolimerDiagramFilter;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class DiagramXmlWriterTest extends TestCase
{
    /**
     * Test simple DPS without complex structure
     * @throws Exception
     */
    public void testSerializeDPS_Simple() throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        dps.add(new DynamicProperty("p1", Integer.class, 10));
        dps.add(new DynamicProperty("p2", String.class, "value 2"));

        DynamicProperty d = new DynamicProperty("p3", Double.class, 3.456);
        d.setShortDescription("double property");
        dps.add(d);

        DynamicPropertySet registry = new DynamicPropertySetSupport();

        Document doc = createDocument();
        Element root = doc.createElement("test");
        doc.appendChild(root);
        DiagramXmlWriter.serializeDPS(doc, root, dps, registry);
        String xml = getXML(doc).replaceAll(System.getProperty("line.separator"), "");

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<test>"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>"
                + "<property name=\"p2\" short-description=\"p2\" type=\"String\" value=\"value 2\"/>"
                + "<property name=\"p3\" short-description=\"double property\" type=\"double\" value=\"3.456\"/>" + "</test>";
        assertEquals(xml, result);
    }

    /**
     * Test simple DPS with registry - must create property references
     * @throws Exception
     */
    public void testSerializeDPS_SimpleRegistry() throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        dps.add(new DynamicProperty("p1", Integer.class, 10));
        dps.add(new DynamicProperty("p2", String.class, "value 2"));

        DynamicProperty d = new DynamicProperty("p3", Double.class, 3.456);
        d.setShortDescription("double property");
        dps.add(d);

        DynamicPropertySet registry = new DynamicPropertySetSupport();
        //registry.add(new DynamicProperty("p1", Integer.class, null));
        registry.add(new DynamicProperty("p2", String.class, null));
        registry.add(new DynamicProperty("p3", Double.class, null));

        Document doc = createDocument();
        Element root = doc.createElement("test");
        doc.appendChild(root);
        DiagramXmlWriter.serializeDPS(doc, root, dps, registry);
        String xml = getXML(doc).replaceAll(System.getProperty("line.separator"), "");

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<test>"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>"
                + "<propertyRef name=\"p2\" value=\"value 2\"/>" + "<propertyRef name=\"p3\" value=\"3.456\"/>" + "</test>";

        assertEquals(xml, result);
    }

    /**
     * Test complex DPS
     * @throws Exception
     */
    public void testSerializeDPS_Complex() throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        dps.add(new DynamicProperty("p1", Integer.class, 10));
        dps.add(new DynamicProperty("p2", String.class, "value 2"));

        DynamicProperty d = new DynamicProperty("p3", Double.class, 3.456);
        d.setShortDescription("double property");
        dps.add(d);

        // complex part
        DynamicProperty complexProperty = new DynamicProperty("p_complex", DynamicPropertySet.class, null);
        complexProperty.setShortDescription("complex_descr");
        dps.add(complexProperty);
        DynamicPropertySet nested = new DynamicPropertySetSupport();
        nested.add(new DynamicProperty("pp1", Integer.class, 20));
        nested.add(new DynamicProperty("pp2", String.class, "value 2"));
        dps.setValue("p_complex", nested);

        DynamicPropertySet registry = new DynamicPropertySetSupport();

        Document doc = createDocument();
        Element root = doc.createElement("test");
        doc.appendChild(root);
        DiagramXmlWriter.serializeDPS(doc, root, dps, registry);
        String xml = getXML(doc).replaceAll(System.getProperty("line.separator"), "");

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<test>"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>"
                + "<property name=\"p2\" short-description=\"p2\" type=\"String\" value=\"value 2\"/>"
                + "<property name=\"p3\" short-description=\"double property\" type=\"double\" value=\"3.456\"/>"
                + "<property name=\"p_complex\" short-description=\"complex_descr\" type=\"composite\">"
                + "<property name=\"pp1\" short-description=\"pp1\" type=\"int\" value=\"20\"/>"
                + "<property name=\"pp2\" short-description=\"pp2\" type=\"String\" value=\"value 2\"/>" + "</property>" + "</test>";

        assertEquals(xml, result);
    }

    /**
     * Test complex DPS
     * @throws Exception
     */
    public void testSerializeDPS_ComplexRegistry() throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        dps.add(new DynamicProperty("p1", Integer.class, 10));
        dps.add(new DynamicProperty("p2", String.class, "value 2"));
        dps.add(new DynamicProperty("p3", Double.class, 3.456));

        // complex part
        DynamicProperty complexProperty = new DynamicProperty("p_complex", DynamicPropertySet.class, null);
        complexProperty.setShortDescription("complex_descr");
        dps.add(complexProperty);
        DynamicPropertySet nested = new DynamicPropertySetSupport();
        nested.add(new DynamicProperty("pp1", Integer.class, 20));
        nested.add(new DynamicProperty("pp2", String.class, "value 2"));
        dps.setValue("p_complex", nested);


        // create registry
        DynamicPropertySet registry = new DynamicPropertySetSupport();
        //registry.add(new DynamicProperty("p1", Integer.class, null));
        registry.add(new DynamicProperty("p2", String.class, null));
        DynamicProperty d_reg = new DynamicProperty("p3", Double.class, null);
        d_reg.setShortDescription("double property");
        registry.add(new DynamicProperty("p3", Double.class, null));

        // complex part
        DynamicProperty complexPropertyReg = new DynamicProperty("p_complex", DynamicPropertySet.class, null);
        complexPropertyReg.setShortDescription("complex_descr");
        registry.add(complexPropertyReg);
        DynamicPropertySet nestedReg = new DynamicPropertySetSupport();
        nestedReg.add(new DynamicProperty("pp1", Integer.class, null));
        nestedReg.add(new DynamicProperty("pp2", String.class, null));
        registry.setValue("p_complex", nestedReg);

        Document doc = createDocument();
        Element root = doc.createElement("test");
        doc.appendChild(root);
        DiagramXmlWriter.serializeDPS(doc, root, dps, registry);
        String xml = getXML(doc).replaceAll(System.getProperty("line.separator"), "");

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<test>"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>"
                + "<propertyRef name=\"p2\" value=\"value 2\"/>" + "<propertyRef name=\"p3\" value=\"3.456\"/>"
                + "<propertyRef name=\"p_complex\">" + "<propertyRef name=\"pp1\" value=\"20\"/>"
                + "<propertyRef name=\"pp2\" value=\"value 2\"/>" + "</propertyRef>" + "</test>";

        assertEquals(xml, result);
    }


    /**
     * Test simple DPS with array
     * @throws Exception
     */
    public void testSerializeDPS_Array() throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        dps.add(new DynamicProperty("p1", Integer.class, 10));
        dps.add(new DynamicProperty("p2", String.class, "value 2"));
        dps.add(new DynamicProperty("p3", Double.class, new Double[] {123.22, 345.55}));

        DynamicPropertySet registry = new DynamicPropertySetSupport();

        Document doc = createDocument();
        Element root = doc.createElement("test");
        doc.appendChild(root);
        DiagramXmlWriter.serializeDPS(doc, root, dps, registry);
        String xml = getXML(doc).replaceAll(System.getProperty("line.separator"), "");

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<test>"
                + "<property name=\"p1\" short-description=\"p1\" type=\"int\" value=\"10\"/>"
                + "<property name=\"p2\" short-description=\"p2\" type=\"String\" value=\"value 2\"/>"
                + "<property elementType=\"double\" name=\"p3\" short-description=\"p3\" type=\"array\">" + "<item>123.22</item>"
                + "<item>345.55</item>" + "</property>" + "</test>";

        assertEquals(xml, result);
    }

    /**
     * Test DPS with array with non primitive values (as example array of
     * {@link biouml.plugins.sbgn.DiagramElementStyle DiagramElementStyle} was taken)
     * @throws Exception
     */
    public void testSerializeDPS_ArrayArrayOfNonPrimitive() throws Exception
    {
        DiagramElementStyle[] array = new DiagramElementStyle[2];
        DiagramElementStyle style = new DiagramElementStyle();
        style.setPen( new Pen( 4 ) );
        style.setBrush( new Brush( Color.yellow ) );
        style.setFont( new ColorFont( "Arial", 0, 14, Color.red ) );
        array[0] = style;
        style = new DiagramElementStyle();
        array[1] = style;

        DynamicPropertySet dps = new DynamicPropertySetSupport();
        dps.add( new DynamicProperty( "testArray", array.getClass(), array ) );

        DynamicPropertySet registry = new DynamicPropertySetSupport();

        Document doc = createDocument();
        Element root = doc.createElement( "test" );
        doc.appendChild( root );
        DiagramXmlWriter.serializeDPS( doc, root, dps, registry );
        String xml = getXML( doc ).replaceAll( System.getProperty( "line.separator" ), "" );

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                + "<test>"
                + "<property elementType=\"biouml.model.DiagramElementStyle\" name=\"testArray\" short-description=\"testArray\" type=\"array\">"
                + "<item>" + "<property name=\"brush\" short-description=\"brush\" type=\"brush\" value=\"yellow\"/>"
                + "<property name=\"font\" short-description=\"font\" type=\"font\" value=\"Arial;0;14;red\"/>"
                + "<property name=\"pen\" short-description=\"pen\" type=\"pen\" value=\"4.0;black;Solid\"/>" + "</item>" + "<item>"
                + "<property name=\"brush\" short-description=\"brush\" type=\"brush\" value=\"white\"/>"
                + "<property name=\"font\" short-description=\"font\" type=\"font\" value=\"Arial;0;12;black\"/>"
                + "<property name=\"pen\" short-description=\"pen\" type=\"pen\" value=\"1.0;black;Solid\"/>" + "</item>" + "</property>"
                + "</test>";

        assertEquals( expected, xml );
    }

    private Document createDocument() throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        return doc;
    }

    private String getXML(Node doc) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter stream = new StringWriter();
        StreamResult result = new StreamResult(stream);
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
        return stream.getBuffer().toString();
    }

    public void testSerializeDiagramWithDPSArray() throws Exception
    {
        Diagram diagram = new PathwaySimulationDiagramType().createDiagram(null, "test", null);
        biouml.model.Node node = new biouml.model.Node(diagram, "node", new Stub(null, "node", "test node"));
        DynamicPropertySet[] columns = new DynamicPropertySet[2];
        columns[0] = new DynamicPropertySetAsMap();
        columns[0].add(new DynamicProperty("name", String.class, "col1"));
        columns[0].add(new DynamicProperty("type", String.class, "VARCHAR"));
        columns[1] = new DynamicPropertySetAsMap();
        columns[1].add(new DynamicProperty("name", String.class, "col2"));
        columns[1].add(new DynamicProperty("type", String.class, "BIGINT"));
        node.getAttributes().add(new DynamicProperty("columns", DynamicPropertySet[].class, columns));
        diagram.put(node);

        Diagram diagram2 = getProcessedDiagram( diagram );
        DiagramElement node2 = diagram2.get( "node" );
        assertNotNull( node2 );
        DynamicPropertySet[] value = (DynamicPropertySet[])node2.getAttributes().getValue( "columns" );
        assertEquals( 2, value.length );
        assertEquals( "col1", value[0].getValue( "name" ) );
        assertEquals( "VARCHAR", value[0].getValue( "type" ) );
        assertEquals( "col2", value[1].getValue( "name" ) );
        assertEquals( "BIGINT", value[1].getValue( "type" ) );
    }

    public void testSerializeLayouter() throws Exception
    {
        Diagram diagram = new PathwaySimulationDiagramType().createDiagram( null, "test", null );
        Diagram diagram2; //processed diagram

        //HierarchicLayouter + OrthogonalPathLayouter
        HierarchicLayouter hl1 = new HierarchicLayouter();
        hl1.setVerticalOrientation( true );
        hl1.setSplineEdges( false );
        hl1.setVirtualNodesDistance( 200 );

        OrthogonalPathLayouter opl1 = new OrthogonalPathLayouter( 20, 30 );
        opl1.setSmoothEdges( false );
        hl1.getPathLayouterWrapper().setPathLayouter( opl1 );

        diagram.setPathLayouter( hl1 );
        diagram2 = getProcessedDiagram( diagram );

        Layouter l = diagram2.getPathLayouter();
        assertTrue( l instanceof HierarchicLayouter );
        HierarchicLayouter hl2 = (HierarchicLayouter)l;
        assertEquals( hl1.isVerticalOrientation(), hl2.isVerticalOrientation() );
        assertEquals( hl1.isSplineEdges(), hl2.isSplineEdges() );
        assertEquals( hl1.getVirtualNodesDistance(), hl2.getVirtualNodesDistance() );

        l = hl2.getPathLayouterWrapper().getPathLayouter();
        assertTrue( l instanceof OrthogonalPathLayouter );
        OrthogonalPathLayouter opl2 = (OrthogonalPathLayouter)l;
        assertEquals( opl1.isSmoothEdges(), opl2.isSmoothEdges() );
        assertEquals( opl1.getGridX(), opl2.getGridX() );
        assertEquals( opl1.getGridY(), opl2.getGridY() );

        //HierarchicLayouter + DiagonalPathLayouter (it has empty bean info so will be written without properties)
        hl1 = new HierarchicLayouter();
        hl1.setLayerOrderIterationNum( 10 );

        DiagonalPathLayouter dpl1 = new DiagonalPathLayouter();
        hl1.getPathLayouterWrapper().setPathLayouter( dpl1 );

        diagram.setPathLayouter( hl1 );
        diagram2 = getProcessedDiagram( diagram );

        l = diagram2.getPathLayouter();
        assertTrue( l instanceof HierarchicLayouter );
        hl2 = (HierarchicLayouter)l;
        assertEquals( hl1.isVerticalOrientation(), hl2.isVerticalOrientation() );
        assertEquals( hl1.getLayerOrderIterationNum(), hl2.getLayerOrderIterationNum() );
        assertTrue( hl2.getPathLayouterWrapper().getPathLayouter() instanceof DiagonalPathLayouter );

        //ForceDirectedLayouter
        ForceDirectedLayouter fdl1 = new ForceDirectedLayouter( 15 );
        fdl1.setHorisontalMovementAllowed( false );
        fdl1.setMagneticIntencity( 14.5f );

        opl1 = new OrthogonalPathLayouter( 40, 10 );
        fdl1.getPathLayouterWrapper().setPathLayouter( opl1 );

        diagram.setPathLayouter( fdl1 );
        diagram2 = getProcessedDiagram( diagram );

        l = diagram2.getPathLayouter();
        assertTrue( l instanceof ForceDirectedLayouter );
        ForceDirectedLayouter fdl2 = (ForceDirectedLayouter)l;
        assertEquals( fdl1.getEdgeLength(), fdl2.getEdgeLength() );
        assertEquals( fdl1.isHorisontalMovementAllowed(), fdl2.isHorisontalMovementAllowed() );
        assertEquals( fdl1.getMagneticIntencity(), fdl2.getMagneticIntencity() );

        l = fdl2.getPathLayouterWrapper().getPathLayouter();
        assertTrue( l instanceof OrthogonalPathLayouter );
        opl2 = (OrthogonalPathLayouter)l;
        assertEquals( opl1.getGridX(), opl2.getGridX() );
        assertEquals( opl1.getGridY(), opl2.getGridY() );

        //CompartmentCrossCostGridLayouter
        CompartmentCrossCostGridLayouter ccgl1 = new CompartmentCrossCostGridLayouter();
        ccgl1.setSaturationDist( 10 );
        ccgl1.setRc( 0 );
        ccgl1.setNe( 100 );

        diagram.setPathLayouter( ccgl1 );
        diagram2 = getProcessedDiagram( diagram );

        l = diagram2.getPathLayouter();
        assertTrue( l instanceof CompartmentCrossCostGridLayouter );
        CompartmentCrossCostGridLayouter ccgl2 = (CompartmentCrossCostGridLayouter)l;
        assertEquals( ccgl1.getSaturationDist(), ccgl2.getSaturationDist() );
        assertEquals( ccgl1.getRc(), ccgl2.getRc() );
        assertEquals( ccgl1.getNe(), ccgl2.getNe() );

        //FastGridLayouter
        FastGridLayouter fgl1 = new FastGridLayouter();
        fgl1.setThreadCount( 4 );
        fgl1.setIterations( 78 );
        fgl1.setStrongRepulsion( -10 );

        diagram.setPathLayouter( fgl1 );
        diagram2 = getProcessedDiagram( diagram );

        l = diagram2.getPathLayouter();
        assertTrue( l instanceof FastGridLayouter );
        FastGridLayouter fgl2 = (FastGridLayouter)l;
        assertEquals( fgl1.getThreadCount(), fgl2.getThreadCount() );
        assertEquals( fgl1.getIterations(), fgl2.getIterations() );
        assertEquals( fgl1.getStrongRepulsion(), fgl2.getStrongRepulsion() );
    }

    public void testSerializeFilters() throws Exception
    {
        Diagram diagram = new PathwaySimulationDiagramType().createDiagram( null, "test", null );
        DiagramFilter[] filters = new DiagramFilter[2];

        ExpressionFilter ef = new ExpressionFilter( "Test Expression Filter" );
        ef.setEnabled( true );
        ExpressionFilterProperties properties = ef.getProperties();
        properties.setUseFlux( false );
        properties.setUseInsideFill( false );
        properties.setUseOutsideFill( false );
        filters[0] = ef;

        BiopolimerDiagramFilter bdf = new BiopolimerDiagramFilter( diagram );
        filters[1] = bdf;

        diagram.setDiagramFilter( ef );
        diagram.setFilterList( filters );
        diagram = getProcessedDiagram( diagram );

        DiagramFilter[] filters2 = diagram.getFilterList();
        assertEquals( filters.length, filters2.length );
        assertEquals( filters[0].getClass(), filters2[0].getClass() );
        assertEquals( filters[0].getName(), filters2[0].getName() );
        assertEquals( filters[0].isEnabled(), filters2[0].isEnabled() );
        assertEquals( diagram.getFilter(), filters2[0] );
        assertEquals( filters[0].getProperties().getClass(), filters2[0].getProperties().getClass() );
        assertEquals( ( (ExpressionFilterProperties)filters[0].getProperties() ).isUseFlux(),
                ( (ExpressionFilterProperties)filters2[0].getProperties() ).isUseFlux() );
        assertEquals( ( (ExpressionFilterProperties)filters[0].getProperties() ).isUseInsideFill(),
                ( (ExpressionFilterProperties)filters2[0].getProperties() ).isUseInsideFill() );
        assertEquals( ( (ExpressionFilterProperties)filters[0].getProperties() ).isUseOutsideFill(),
                ( (ExpressionFilterProperties)filters2[0].getProperties() ).isUseOutsideFill() );
        assertEquals( ( (ExpressionFilterProperties)filters[0].getProperties() ).isUsePval(),
                ( (ExpressionFilterProperties)filters2[0].getProperties() ).isUsePval() );

        assertEquals( filters[1].getClass(), filters2[1].getClass() );
        assertEquals( filters[1].getName(), filters2[1].getName() );
        assertEquals( filters[1].isEnabled(), filters2[1].isEnabled() );
    }

    private Diagram getProcessedDiagram(Diagram diagram) throws Exception
    {
        try (TempFile file = TempFiles.file( ".xml" ))
        {
            try (OutputStream stream = new FileOutputStream( file ))
            {
                new DiagramXmlWriter( stream ).write( diagram );
            }
            Diagram diagram2;
            try (InputStream input = new FileInputStream(file))
            {
                diagram2 = DiagramXmlReader.readDiagram(diagram.getName(), input, null, null, null);
            }
            assertNotNull( diagram2 );
            return diagram2;
        }
    }
}
