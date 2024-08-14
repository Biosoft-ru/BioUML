package biouml.model._test;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;
import biouml.model.Node;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;

import ru.biosoft.graphics.Brush;

public class ArrayAttributeViewTest extends ViewTestCase
{
    public ArrayAttributeViewTest(String name)
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

    public void testNodeView() throws Exception
    {
        Node node = new Node(null, "the node", null);
        node.getAttributes().add(new DynamicProperty("string", String.class));

        node.getAttributes().add(new DynamicProperty("str array", Brush[].class, new Brush[]{new Brush(new Color(255, 0, 0)), new Brush(new Color(0, 255, 0))}));


        PropertyInspectorEx pi = new PropertyInspectorEx();
        pi.explore(node);

        assertView(pi, "node view");
    }
}
