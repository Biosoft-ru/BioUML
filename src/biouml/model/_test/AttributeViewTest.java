package biouml.model._test;

import biouml.standard.type.Stub;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.swing.PropertyInspector;

public class AttributeViewTest extends ViewTestCase
{
    public AttributeViewTest(String name)
    {
        super(name);
        File configFile = new File( "./biouml/model/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public void testArrayView() throws Exception
    {
        Stub stub = new Stub(null, "the stub");
        stub.getAttributes().add(new DynamicProperty("str array", String[].class, new String[]{"str1", "str2"}));

        PropertyInspector pi = new PropertyInspector();
        pi.explore(stub);

        assertView(pi, "string array property");
    }

/*
    public void testStubNodeView() throws Exception
    {
        Stub stub = new Stub(null, "the stub");
        stub.getAttributes().add(new DynamicProperty("string", String.class));

        Node node = new Node(null, "the node", stub);
        node.getAttributes().add(new DynamicProperty("string", String.class));

        node.getAttributes().add(new DynamicProperty("str array", Brush[].class, new Brush[]{new Brush(new Color(0,0,0)), new Brush(new Color(100,100,100))}));

        PropertyInspector pi = new PropertyInspector();
        pi.explore(node);

        assertView(pi, "node with stub");
    }

    public void testConceptNodeView() throws Exception
    {
        Concept concept = new Concept(null, "the concept");
        concept.getAttributes().add(new DynamicProperty("string", String.class));

        Node node = new Node(null, "the node", concept);
        node.getAttributes().add(new DynamicProperty("string", String.class));


        PropertyInspector pi = new PropertyInspector();
        pi.explore(node);

        assertView(pi, "node with concept");
    }
*/
}
