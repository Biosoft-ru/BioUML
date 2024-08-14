package biouml.model.xml._test;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import junit.framework.TestCase;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeReader;
import biouml.model.xml.XmlDiagramTypeWriter;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class XmlDiagramTypeReadWriteTest extends TestCase
{
    private static final String TMP_FILE_NAME = "../data_resources/tmp.xml";

    public void test1() throws Exception
    {
        XmlDiagramType xmlDiagramType = new XmlDiagramType();
        DynamicPropertySet properties = xmlDiagramType.getProperties();
        properties.add(new DynamicProperty("p1", String.class));
        xmlDiagramType.setNodeTypes(new String[] { "node1", "node2" } );
        xmlDiagramType.setEdgeTypes(new String[] { "edge1", "edge2", "edge3" } );
        
        testDiagramTypeReadWrite(xmlDiagramType);
    }

    public void test2() throws Exception
    {
        XmlDiagramType xmlDiagramType = new XmlDiagramType();
        DynamicPropertySet properties = xmlDiagramType.getProperties();
        properties.add(new DynamicProperty("p1", String.class));
        xmlDiagramType.setNodeTypes(new String[] { "node1", "node2" } );
        xmlDiagramType.setEdgeTypes(new String[] { "edge1", "edge2", "edge3" } );
        
        DynamicPropertySet dps1 = new DynamicPropertySetSupport();
        dps1.add(new DynamicProperty("node1_prop1", Double.class));
        xmlDiagramType.addType("node1", dps1, null);
        
        DynamicPropertySet dps2 = new DynamicPropertySetSupport();
        dps2.add(new DynamicProperty("edge1_prop1", Double.class));
        xmlDiagramType.addType("edge1", dps2, null);

        testDiagramTypeReadWrite(xmlDiagramType);
    }
    
    
    private void testDiagramTypeReadWrite(XmlDiagramType xmlDiagramType) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( TMP_FILE_NAME ))
        {
            XmlDiagramTypeWriter writer = new XmlDiagramTypeWriter( fos );
            writer.write( xmlDiagramType );
            fos.flush();
        }

        FileInputStream fis = new FileInputStream(TMP_FILE_NAME);
        XmlDiagramTypeReader reader = new XmlDiagramTypeReader("test GN", fis);
        XmlDiagramType readXmlDiagramType = reader.read(null);

        assertEquals(readXmlDiagramType, xmlDiagramType);
    }
}
